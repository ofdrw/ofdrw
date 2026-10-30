package org.ofdrw.converter.pdf.render;

import java.util.List;

import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.pageDescription.clips.Area;
import org.ofdrw.core.pageDescription.clips.ClipAble;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.text.CT_Text;

/**
 * 区域与文字区域的几何换算：OFD 坐标（mm，左上角原点 Y 向下）→ PDF 页面坐标（pt，左下角原点 Y 向上）
 * <p>
 * 换算一律复用渲染器传入的 {@link AffineMatrix}（由 {@link org.ofdrw.converter.pdf.CoordSystem#objectToPage} 构造，
 * 设计约束 2），对矩形取四点变换后的外接矩形，因此同样适用于带旋转/缩放的容器 CTM。
 * <p>
 * 文字区域（裁剪 Area 为 Text 时）按“文字单元外接矩形合并 ∧ 文字对象外接矩形”近似：
 * 规范（表 33）要求按文字排版区域裁剪，但 PDF 裁剪操作符只接受路径，
 * 因此本实现取两者交集形成矩形带作为保守近似，由 {@link ClipApplier} 输出提示日志。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class RegionGeometry {

    /**
     * 文字单元纵向余量比例（字符高度按字号估算，取值偏保守以避免裁掉字形）
     */
    private static final double TEXT_HEIGHT_SLACK = 1.2;

    /**
     * 文字单元横向余量比例
     */
    private static final double TEXT_WIDTH_SLACK = 1.1;

    /**
     * 文字字号缺失时的兜底字号，单位 mm（OFD 默认字号 5mm 的三级标题字号）
     */
    private static final double FALLBACK_TEXT_SIZE_MM = 3.528;

    private RegionGeometry() {
    }

    /**
     * OFD 矩形 → 页面矩形（四点变换后取外接矩形）
     *
     * @param x      左上角横坐标，单位 mm
     * @param y      左上角纵坐标，单位 mm
     * @param width  宽度，单位 mm
     * @param height 高度，单位 mm
     * @param toPage 对象空间 → 页面坐标矩阵，不能为空
     * @return 页面矩形，单位 pt
     */
    static Box boxToPage(double x, double y, double width, double height, AffineMatrix toPage) {
        double[] p1 = toPage.transform(x, y);
        double[] p2 = toPage.transform(x + width, y);
        double[] p3 = toPage.transform(x, y + height);
        double[] p4 = toPage.transform(x + width, y + height);
        double minX = Math.min(Math.min(p1[0], p2[0]), Math.min(p3[0], p4[0]));
        double maxX = Math.max(Math.max(p1[0], p2[0]), Math.max(p3[0], p4[0]));
        double minY = Math.min(Math.min(p1[1], p2[1]), Math.min(p3[1], p4[1]));
        double maxY = Math.max(Math.max(p1[1], p2[1]), Math.max(p3[1], p4[1]));
        return new Box(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * 计算单个区域（Area）的页面矩形
     *
     * @param area   区域，可为空
     * @param toPage 对象空间 → 页面坐标矩阵，不能为空
     * @return 页面矩形；区域为空、裁剪对象类型不支持或缺少外接矩形时返回 {@code null}
     */
    static Box areaBox(Area area, AffineMatrix toPage) {
        if (area == null) {
            return null;
        }
        ClipAble clipObj = area.getClipObj();
        if (clipObj instanceof CT_Text) {
            return textBox((CT_Text) clipObj, toPage);
        }
        if (clipObj instanceof org.ofdrw.core.graph.pathObj.CT_Path) {
            return boundaryBox(((org.ofdrw.core.graph.pathObj.CT_Path) clipObj).getBoundary(), toPage);
        }
        return null;
    }

    /**
     * 文字对象 → 页面矩形（文字单元外接矩形合并后与对象外接矩形取交集）
     *
     * @param text   文字对象，不能为空
     * @param toPage 对象空间 → 页面坐标矩阵，不能为空
     * @return 页面矩形；无文字单元时退化为对象外接矩形，对象也没有外接矩形时返回 {@code null}
     */
    static Box textBox(CT_Text text, AffineMatrix toPage) {
        ST_Box boundary = text.getBoundary();
        List<TextCode> codes = text.getTextCodes();
        if (codes == null || codes.isEmpty()) {
            return boundaryBox(boundary, toPage);
        }
        double left = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        double size = text.getSize() == null || text.getSize() <= 0 ? FALLBACK_TEXT_SIZE_MM : text.getSize();
        for (TextCode code : codes) {
            if (code == null || code.getX() == null || code.getY() == null || code.getContent() == null) {
                continue;
            }
            int charCount = Math.max(1, code.getContent().codePointCount(0, code.getContent().length()));
            // 文字单元以基线定位：横向按字数 × 字号估算，纵向覆盖基线上方一行字高
            double width = charCount * size * TEXT_WIDTH_SLACK;
            double height = size * TEXT_HEIGHT_SLACK;
            left = Math.min(left, code.getX());
            right = Math.max(right, code.getX() + width);
            top = Math.min(top, code.getY() - height);
            bottom = Math.max(bottom, code.getY());
        }
        if (left > right || top > bottom) {
            return boundaryBox(boundary, toPage);
        }
        if (boundary != null) {
            // 与对象外接矩形取交集，避免估算法向超出对象范围
            left = Math.max(left, boundary.getTopLeftX());
            right = Math.min(right, boundary.getTopLeftX() + boundary.getWidth());
            top = Math.max(top, boundary.getTopLeftY());
            bottom = Math.min(bottom, boundary.getTopLeftY() + boundary.getHeight());
            if (left > right || top > bottom) {
                return boundaryBox(boundary, toPage);
            }
        }
        return boxToPage(left, top, right - left, bottom - top, toPage);
    }

    /**
     * OFD 外接矩形 → 页面矩形
     *
     * @param boundary 外接矩形，单位 mm，可为空
     * @param toPage   对象空间 → 页面坐标矩阵，不能为空
     * @return 页面矩形；外接矩形为空返回 {@code null}
     */
    static Box boundaryBox(ST_Box boundary, AffineMatrix toPage) {
        if (boundary == null || boundary.getTopLeftX() == null || boundary.getTopLeftY() == null
                || boundary.getWidth() == null || boundary.getHeight() == null) {
            return null;
        }
        return boxToPage(boundary.getTopLeftX(), boundary.getTopLeftY(),
                boundary.getWidth(), boundary.getHeight(), toPage);
    }

    /**
     * 判断裁剪对象是否为文字
     *
     * @param clipObj 裁剪对象
     * @return {@code true} 表示文字裁剪
     */
    static boolean isText(Object clipObj) {
        return clipObj instanceof CT_Text;
    }

    /**
     * 判断裁剪对象是否为路径
     *
     * @param clipObj 裁剪对象
     * @return {@code true} 表示路径裁剪
     */
    static boolean isPath(Object clipObj) {
        return clipObj instanceof org.ofdrw.core.graph.pathObj.CT_Path;
    }
}
