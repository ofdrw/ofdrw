package org.ofdrw.converter.pdf.render;

import java.util.ArrayList;
import java.util.List;

import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.Box;

/**
 * 图元几何换算：OFD 图元（对象空间，mm，左上角原点 Y 向下）→ PDF 页面坐标（pt，左下角原点 Y 向上）
 * <p>
 * 与 {@link PathRenderer#objectToPage} 采用完全相同的变换顺序（设计 3.2 节，决策 1 逐点换算）：
 * <ol>
 *     <li>图元 CTM 与容器 CTM 叠加：{@code M = CTM · 容器CTM}；</li>
 *     <li>平移外接矩形左上角与页面框偏移（决策 26）：{@code T = [1 0 0 1 (bx+x0) (by+y0)]}；</li>
 *     <li>mm→pt 与 Y 翻转：{@code F = [k 0 0 -k 0 pageH·k]}。</li>
 * </ol>
 * 缩放<b>只</b>由上述显式上下文（容器 CTM + 图元 CTM + 外接矩形）决定，不嗅探父节点类型（设计 3.2 节）。
 * 本类不持有任何渲染库类型，便于单测。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ImageGeometry {

    private ImageGeometry() {
    }

    /**
     * 计算图元的对象空间 → 页面坐标矩阵
     *
     * @param ctm     图元的 CTM（行向量），可为空表示单位矩阵
     * @param boundary 图元外接矩形（x、y、w、h），可为空表示原点 (0,0)
     * @param context 渲染上下文（提供容器 CTM、页面框偏移与页面高度）
     * @return 换算矩阵：对象空间 (mm) → 页面坐标 (pt)
     */
    public static AffineMatrix objectToPage(org.ofdrw.core.basicType.ST_Array ctm, org.ofdrw.core.basicType.ST_Box boundary,
                                            RenderContext context) {
        if (context == null) {
            throw new IllegalArgumentException("渲染上下文不能为空");
        }
        AffineMatrix combined = elementCtm(ctm).multiply(context.getCurrentCtm());
        // 外接矩形平移、页面框偏移、mm→pt 与 Y 翻转统一由 CoordSystem 完成
        return CoordSystem.objectToPage(
                CoordSystem.boundary(boundaryX(boundary), boundaryY(boundary),
                        boundaryWidth(boundary), boundaryHeight(boundary)),
                combined, context.getPageX0Mm(), context.getPageY0Mm(), context.getPageHeightMm());
    }

    /**
     * 取外接矩形宽度
     *
     * @param boundary 外接矩形，可为空
     * @return 宽度，单位 mm；缺失时返回 0
     */
    private static double boundaryWidth(org.ofdrw.core.basicType.ST_Box boundary) {
        return boundary == null || boundary.getWidth() == null ? 0 : boundary.getWidth();
    }

    /**
     * 取外接矩形高度
     *
     * @param boundary 外接矩形，可为空
     * @return 高度，单位 mm；缺失时返回 0
     */
    private static double boundaryHeight(org.ofdrw.core.basicType.ST_Box boundary) {
        return boundary == null || boundary.getHeight() == null ? 0 : boundary.getHeight();
    }

    /**
     * 计算图像图元的「单位正方形对象空间 → 页面坐标」矩阵
     * <p>
     * 与 {@link #objectToPage} 的区别：图像的对象空间是单位正方形而非 mm 坐标，
     * 其 {@code CTM}（如 {@code "w 0 0 h 0 0"}）给出从单位正方形到外接矩形尺寸（mm）的缩放，
     * 单位换算与 Y 翻转仍由 CoordSystem 的比例完成（设计约束 2）。
     * 外接矩形左上角的平移仍由 {@code Boundary} 提供（CTM 的平移部分对外接矩形定位而言为零）。
     *
     * @param ctm      图元 CTM（行向量），可为空表示单位矩阵
     * @param boundary 图元外接矩形，可为空
     * @param context  渲染上下文，不能为空
     * @return 单位正方形对象空间 → 页面坐标 (pt) 的矩阵
     */
    public static AffineMatrix unitSquareToPage(org.ofdrw.core.basicType.ST_Array ctm,
                                                org.ofdrw.core.basicType.ST_Box boundary,
                                                RenderContext context) {
        if (context == null) {
            throw new IllegalArgumentException("渲染上下文不能为空");
        }
        AffineMatrix elementCtm = elementCtm(ctm);
        double bx = boundary == null || boundary.getTopLeftX() == null ? 0 : boundary.getTopLeftX();
        double by = boundary == null || boundary.getTopLeftY() == null ? 0 : boundary.getTopLeftY();
        /*
         * 变换顺序（multiply 语义为“先本矩阵、再右侧矩阵”）：
         *   1) 单位正方形 -CTM→ 对象尺寸（宽高由 CTM 的 a/d 给出）；
         *   2) 平移到外接矩形左上角（该平移处于图元对象空间，不参与 CTM 缩放）+ 页面框偏移；
         *   3) 叠加父容器 CTM。
         * 若写成 translate.multiply(ctm)，平移量会先经过 CTM 的缩放，实测会偏移到页面外。
         */
        AffineMatrix translate = AffineMatrix.translate(bx + context.getPageX0Mm(), by + context.getPageY0Mm());
        AffineMatrix objectToOfd = elementCtm.multiply(translate).multiply(context.getCurrentCtm());
        // OFD 页面坐标（mm，Y 向下）→ PDF 页面坐标（pt，Y 向上）：需要乘 mm→pt 并翻转 Y
        AffineMatrix flip = new AffineMatrix(CoordSystem.CONTENT_MM_TO_PT, 0, 0, -CoordSystem.CONTENT_MM_TO_PT, 0,
                context.getPageHeightMm() * CoordSystem.CONTENT_MM_TO_PT);
        return objectToOfd.multiply(flip);
    }

    /**
     * 计算带旋转/斜切 CTM 的图像「单位正方形 → 页面坐标」矩阵
     * <p>
     * 图像对象空间是单位正方形，OFD 的 {@code CTM} 完成旋转、缩放与镜像；外接矩形只用于定位，
     * 其左上角仍是图像落点的锚点。变换按以下口径构造（与基准 PDF 的 {@code cm} 矩阵一致）：
     * <ol>
     *     <li>取 CTM 线性部分，并按 PDF 的 Y 轴向上约定翻转 {@code b}、{@code c}；</li>
     *     <li>把变换后的单位正方形外接框的左上角对齐到 {@code Boundary} 左上角；</li>
     *     <li>mm→pt 换算。</li>
     * </ol>
     * 轴对齐 CTM 不走本方法（直接按外接矩形 {@code drawImage} 即可），避免改变既有正确样本的落点。
     *
     * @param ctm      图元 CTM（行向量），可为空表示单位矩阵
     * @param boundary 图元外接矩形，可为空
     * @param context  渲染上下文，不能为空
     * @return 单位正方形对象空间 → 页面坐标 (pt) 的矩阵
     */
    public static AffineMatrix rotatedUnitSquareToPage(org.ofdrw.core.basicType.ST_Array ctm,
                                                       org.ofdrw.core.basicType.ST_Box boundary,
                                                       RenderContext context) {
        if (context == null) {
            throw new IllegalArgumentException("渲染上下文不能为空");
        }
        AffineMatrix element = elementCtm(ctm);
        double k = CoordSystem.CONTENT_MM_TO_PT;
        // PDF 坐标 Y 轴向上：CTM 的 b、c 分量取反后才是 PDF 用户空间中的方向
        double a = element.getA() * k;
        double b = -element.getB() * k;
        double c = -element.getC() * k;
        double d = element.getD() * k;
        // 外接框左上角对齐 Boundary 左上角（页面框偏移计入）
        double bx = boundaryX(boundary) + context.getPageX0Mm();
        double by = boundaryY(boundary) + context.getPageY0Mm();
        double e = bx * k - Math.min(0, Math.min(a, c));
        double f = (context.getPageHeightMm() - by) * k - Math.max(0, Math.max(b, d));
        return new AffineMatrix(a, b, c, d, e, f);
    }

    /**
     * 判断图元 CTM 是否为轴对齐（仅缩放，无旋转/斜切）
     *
     * @param ctm 图元 CTM，可为空
     * @return true 表示轴对齐；CTM 为空或非 6 个操作数时同样视为轴对齐
     */
    public static boolean isAxisAligned(org.ofdrw.core.basicType.ST_Array ctm) {
        if (ctm == null || ctm.size() != 6) {
            return true;
        }
        Double[] values = ctm.toDouble();
        double b = values[1] == null ? 0 : values[1];
        double c = values[2] == null ? 0 : values[2];
        return Math.abs(b) < 1e-9 && Math.abs(c) < 1e-9;
    }

    /**
     * 取外接矩形左上角横坐标
     *
     * @param boundary 外接矩形，可为空
     * @return 横坐标，单位 mm；为空返回 0
     */
    private static double boundaryX(org.ofdrw.core.basicType.ST_Box boundary) {
        return boundary == null || boundary.getTopLeftX() == null ? 0 : boundary.getTopLeftX();
    }

    /**
     * 取外接矩形左上角纵坐标
     *
     * @param boundary 外接矩形，可为空
     * @return 纵坐标，单位 mm；为空返回 0
     */
    private static double boundaryY(org.ofdrw.core.basicType.ST_Box boundary) {
        return boundary == null || boundary.getTopLeftY() == null ? 0 : boundary.getTopLeftY();
    }

    /**
     * 解析图元 CTM 属性
     *
     * @param ctm 图元 CTM（行向量），可为空
     * @return 仿射矩阵；为空或操作数不为 6 时返回单位矩阵
     */
    public static AffineMatrix elementCtm(org.ofdrw.core.basicType.ST_Array ctm) {
        if (ctm == null || ctm.size() != 6) {
            return AffineMatrix.IDENTITY;
        }
        Double[] values = ctm.toDouble();
        double[] raw = new double[6];
        for (int i = 0; i < 6; i++) {
            raw[i] = values[i] == null ? 0 : values[i];
        }
        return CoordSystem.fromCtm(raw);
    }

    /**
     * 图元外接矩形 → 页面矩形
     * <p>
     * <b>图像的对象空间是单位正方形</b>：GB/T 33190 8.2 的 {@code CTM} 把单位正方形 {@code (0,0)-(1,1)} 映射到对象空间，
     * 图像对象通常写作 {@code CTM="w 0 0 h 0 0"} 且 {@code w}/{@code h} 与外接矩形宽高一致（实测样本即如此）。
     * 因此这里把单位正方形的四个角经 {@code toPage} 变换后取外接矩形；
     * 若改用外接矩形的 mm 尺寸 {@code (0,0)-(w,h)} 参与变换，会与 CTM 中的缩放叠加成 {@code w×w} 的平方级放大。
     *
     * @param toPage 对象空间（单位正方形）→ 页面坐标矩阵，不能为空
     * @return 页面矩形，单位 pt；矩阵退化时返回 {@code null}
     */
    public static Box unitSquareBox(AffineMatrix toPage) {
        if (toPage == null) {
            return null;
        }
        double[] p1 = toPage.transform(0, 0);
        double[] p2 = toPage.transform(1, 0);
        double[] p3 = toPage.transform(0, 1);
        double[] p4 = toPage.transform(1, 1);
        double minX = Math.min(Math.min(p1[0], p2[0]), Math.min(p3[0], p4[0]));
        double maxX = Math.max(Math.max(p1[0], p2[0]), Math.max(p3[0], p4[0]));
        double minY = Math.min(Math.min(p1[1], p2[1]), Math.min(p3[1], p4[1]));
        double maxY = Math.max(Math.max(p1[1], p2[1]), Math.max(p3[1], p4[1]));
        if (!(maxX > minX) || !(maxY > minY)) {
            return null;
        }
        return new Box(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * 图元外接矩形 → 页面矩形
     * <p>
     * 图元对象空间以外接矩形左上角为原点（8.1.4）：页面矩形 = 对象空间矩形 {@code (0,0,w,h)} 经 {@code toPage} 变换后取外接矩形。
     * <p>
     * <b>仅适用于对象坐标以 mm 度量的图元</b>（路径等）。图像的对象空间是单位正方形，应改用 {@link #unitSquareBox(AffineMatrix)}。
     *
     * @param boundary 图元外接矩形，单位 mm，可为空
     * @param toPage   对象空间 → 页面坐标矩阵，不能为空
     * @return 页面矩形，单位 pt；外接矩形缺失或退化时返回 {@code null}
     */
    public static Box boundaryBox(org.ofdrw.core.basicType.ST_Box boundary, AffineMatrix toPage) {
        if (boundary == null || boundary.getWidth() == null || boundary.getHeight() == null
                || boundary.getWidth() <= 0 || boundary.getHeight() <= 0) {
            return null;
        }
        double w = boundary.getWidth();
        double h = boundary.getHeight();
        double[] p1 = toPage.transform(0, 0);
        double[] p2 = toPage.transform(w, 0);
        double[] p3 = toPage.transform(0, h);
        double[] p4 = toPage.transform(w, h);
        double minX = Math.min(Math.min(p1[0], p2[0]), Math.min(p3[0], p4[0]));
        double maxX = Math.max(Math.max(p1[0], p2[0]), Math.max(p3[0], p4[0]));
        double minY = Math.min(Math.min(p1[1], p2[1]), Math.min(p3[1], p4[1]));
        double maxY = Math.max(Math.max(p1[1], p2[1]), Math.max(p3[1], p4[1]));
        if (!(maxX > minX) || !(maxY > minY)) {
            return null;
        }
        return new Box(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * 把页面矩阵中的线性部分换算为毫米（用于把 px→pt 的系数换算回 mm 语境）
     *
     * @param toPage 对象空间 → 页面坐标矩阵，不能为空
     * @return 对象空间 1mm 对应的页面长度，单位 pt
     */
    public static double unitScale(AffineMatrix toPage) {
        if (toPage == null) {
            return 1;
        }
        // 图像对象空间是单位正方形，其“对象空间 1 单位”由矩阵线性部分直接给出，不依赖平移；
        // 容器 CTM 的 mm→pt 比例（CONTENT_MM_TO_PT）已经体现在矩阵里
        return Math.hypot(toPage.getA(), toPage.getB());
    }

    /**
     * 把 OFD 的 {@code ST_Array} 虚线样式解析为数值数组
     * <p>
     * 支持 {@code #} 十六进制与十进制；非法值按 0 处理；不含正数时返回空数组（由调用方退化为实线）。
     *
     * @param dash 虚线数组，可为空
     * @return 虚线长度序列，单位与来源一致（mm）
     */
    public static double[] dashArray(org.ofdrw.core.basicType.ST_Array dash) {
        if (dash == null || dash.size() == 0) {
            return new double[0];
        }
        List<String> items = dash.getArray();
        List<Double> values = new ArrayList<>(items.size());
        boolean positive = false;
        for (String item : items) {
            Double value = parseLength(item);
            double v = value == null || value < 0 ? 0 : value;
            if (v > 0) {
                positive = true;
            }
            values.add(v);
        }
        if (!positive) {
            return new double[0];
        }
        double[] result = new double[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    /**
     * 解析长度文本（支持 {@code #} 十六进制）
     *
     * @param text 原始文本
     * @return 数值；无法解析返回 {@code null}
     */
    static Double parseLength(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String value = text.trim();
        try {
            if (value.startsWith("#")) {
                return (double) Integer.parseInt(value.substring(1), 16);
            }
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
