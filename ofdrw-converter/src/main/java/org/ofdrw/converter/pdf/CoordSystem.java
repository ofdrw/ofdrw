package org.ofdrw.converter.pdf;

import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.Box;

/**
 * 坐标换算工具（设计约束 2）
 * <p>
 * <b>本类是 OFD 与 PDF 之间全部坐标换算的唯一入口</b>：代码中禁止再出现零散的 {@code ×72/25.4} 或手写 Y 翻转。
 * <p>
 * 两类坐标系（设计 3.1 节）：
 * <table border="1">
 *     <tr><th>维度</th><th>OFD</th><th>PDF</th></tr>
 *     <tr><td>页面原点</td><td>左上角，Y 轴向下</td><td>左下角，Y 轴向上</td></tr>
 *     <tr><td>页面单位</td><td>mm</td><td>pt</td></tr>
 *     <tr><td>图元局部坐标</td><td>对象空间：外接矩形左上角为原点，Y 轴向下，单位 mm</td><td>页面坐标（本实现逐点换算，见决策 1）</td></tr>
 * </table>
 * <p>
 * 决策 1（逐点换算）：所有图元坐标在写入指令前换算为 PDF 页面坐标，页面 CTM 保持恒等，
 * 因此本类提供的是“对象空间 → 页面坐标”这一个复合矩阵 {@link #objectToPage(Box, AffineMatrix)}，
 * 而不是让渲染器在 PDF 中写页面级翻转矩阵。
 * <p>
 * 单精度或类型转换时一律使用 {@code double}，全部方法均为纯函数，无状态、线程安全。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class CoordSystem {

    /**
     * 1 英寸包含的毫米数
     */
    public static final double MM_PER_INCH = 25.4d;

    /**
     * 1 英寸包含的 PDF 点（pt）数
     */
    public static final double PT_PER_INCH = 72d;

    /**
     * 毫米到点（pt）的换算比例：{@code 72 / 25.4}
     */
    public static final double MM_TO_PT = PT_PER_INCH / MM_PER_INCH;

    /**
     * 点（pt）到毫米的换算比例：{@code 25.4 / 72}
     */
    public static final double PT_TO_MM = MM_PER_INCH / PT_PER_INCH;

    /**
     * 内容坐标（图元、文字、图像、注释）的毫米到点换算比例：{@code 2.8346}
     * <p>
     * 与页面框不同，参考实现把内容坐标按 1mm = 2.8346pt 换算后写入内容流（页面框仍按 72/25.4 精确换算）。
     * 两者相差约 1.6e-5，但该差异会改变图像重采样与字形栅格化的相位，逐像素对比时影响显著。
     */
    public static final double CONTENT_MM_TO_PT = 2.8346d;

    /**
     * 内容坐标的点到毫米换算比例：{@code 1 / 2.8346}
     */
    public static final double CONTENT_PT_TO_MM = 1d / CONTENT_MM_TO_PT;

    private CoordSystem() {
    }

    /**
     * 毫米转 PDF 点
     *
     * @param mm 长度，单位 mm
     * @return 长度，单位 pt
     */
    public static double mmToPt(double mm) {
        return mm * CONTENT_MM_TO_PT;
    }

    /**
     * PDF 点转毫米
     *
     * @param pt 长度，单位 pt
     * @return 长度，单位 mm
     */
    public static double ptToMm(double pt) {
        return pt * CONTENT_PT_TO_MM;
    }

    /**
     * OFD 纵坐标（页面左上角原点、Y 轴向下、单位 mm）转 PDF 纵坐标（左下角原点、Y 轴向上、单位 pt）
     *
     * @param yMm       OFD 纵坐标，单位 mm
     * @param pageHMm   页面高度，单位 mm
     * @return PDF 纵坐标，单位 pt
     */
    public static double yToPt(double yMm, double pageHMm) {
        return (pageHMm - yMm) * CONTENT_MM_TO_PT;
    }

    /**
     * PDF 纵坐标转 OFD 纵坐标（逆变换）
     *
     * @param yPt     PDF 纵坐标，单位 pt
     * @param pageHMm 页面高度，单位 mm
     * @return OFD 纵坐标，单位 mm
     */
    public static double yFromPt(double yPt, double pageHMm) {
        return pageHMm - yPt * CONTENT_PT_TO_MM;
    }

    /**
     * OFD 页面坐标点（左上角原点、Y 轴向下、单位 mm）转 PDF 页面坐标点（左下角原点、Y 轴向上、单位 pt）
     *
     * @param xMm     OFD 横坐标，单位 mm
     * @param yMm     OFD 纵坐标，单位 mm
     * @param pageHMm 页面高度，单位 mm
     * @return 新的坐标数组 {@code [x, y]}，单位 pt
     */
    public static double[] pointToPage(double xMm, double yMm, double pageHMm) {
        return new double[]{mmToPt(xMm), yToPt(yMm, pageHMm)};
    }

    /**
     * OFD 矩形（左上角原点、Y 轴向下、单位 mm）转 PDF 页面矩形（左下角原点、Y 轴向上、单位 pt）
     * <p>
     * 返回矩形的 {@code x}/{@code y} 为左下角，{@code width}/{@code height} 保持正数。
     *
     * @param box      OFD 矩形，单位 mm
     * @param pageHMm  页面高度，单位 mm
     * @return PDF 页面矩形，单位 pt
     */
    public static Box boxToPage(Box box, double pageHMm) {
        if (box == null) {
            throw new IllegalArgumentException("矩形不能为空");
        }
        double x = mmToPt(box.getX());
        double w = mmToPt(box.getWidth());
        double h = mmToPt(box.getHeight());
        // OFD 下边界换算为 PDF 纵坐标后即为矩形左下角
        double y = yToPt(box.getY() + box.getHeight(), pageHMm);
        return new Box(x, y, w, h);
    }

    /**
     * OFD 物理页面框转 PDF 页面矩形，对应决策 26（{@code MediaBox = CropBox = PhysicalBox}）
     * <p>
     * 允许 PhysicalBox 带有非零左上角偏移 {@code (x0, y0)}：返回矩形的左下角保留该偏移，
     * 使页面内容与页面框偏移在 PDF 中与 OFD 一致。
     *
     * @param x0Mm     页面框左上角横坐标，单位 mm
     * @param y0Mm     页面框左上角纵坐标，单位 mm
     * @param widthMm  页面宽度，单位 mm
     * @param heightMm 页面高度，单位 mm
     * @return PDF 页面矩形，单位 pt（x/y 为左下角）
     */
    public static Box pageBoxToPt(double x0Mm, double y0Mm, double widthMm, double heightMm) {
        // 页面框自身即为其坐标系，下边界 y0Mm + heightMm 翻转后左下角纵向值恒为 y0Mm
        // 页面框按 72/25.4 精确换算（内容坐标使用 CONTENT_MM_TO_PT，见该常量说明）
        final double k = PT_PER_INCH / MM_PER_INCH;
        double y = y0Mm * k;
        return new Box(x0Mm * k, y, widthMm * k, heightMm * k);
    }

    /**
     * 对象空间 → PDF 页面坐标矩阵
     * <p>
     * OFD 图元局部坐标以外接矩形左上角为原点、Y 轴向下、单位 mm（8.1.4），且 CTM 在对象空间内叠加（8.1.5）：
     * <ol>
     *     <li>先平移外接矩形左上角：{@code T = [1 0 0 1 bx by]}；</li>
     *     <li>再叠加图元（含父容器）CTM：{@code M = CTM · T}；</li>
     *     <li>最后换算到 PDF 页面：单位 mm→pt，Y 轴翻转，页面框偏移。</li>
     * </ol>
     * 结果是<b>单点即可换算</b>的仿射矩阵，渲染器对每条路径点调用 {@link AffineMatrix#transform(double, double)} 即可。
     *
     * @param localBox 图元外接矩形（Boundary），单位 mm，左上角原点语义；为空表示无平移（等价于 {@code (0, 0)}）
     * @param ctm      图元及其父容器叠加后的 CTM，可为空表示单位矩阵
     * @return 对象空间 (mm) → PDF 页面坐标 (pt) 的完整矩阵
     */
    public static AffineMatrix objectToPage(Box localBox, AffineMatrix ctm) {
        return objectToPage(localBox, ctm, 0, 0, 0);
    }

    /**
     * 对象空间 → PDF 页面坐标矩阵（含页面框偏移与页面高度）
     * <p>
     * 适用于模板页、复合对象等容器：容器自身带有页面框偏移时，通过 {@code pageX0Mm}/{@code pageY0Mm} 传入。
     *
     * @param localBox 图元外接矩形，单位 mm，可为空
     * @param ctm      图元及其父容器叠加后的 CTM，可为空表示单位矩阵
     * @param pageX0Mm 页面框左上角横坐标偏移，单位 mm
     * @param pageY0Mm 页面框左上角纵坐标偏移，单位 mm
     * @param pageHMm  页面高度，单位 mm
     * @return 对象空间 (mm) → PDF 页面坐标 (pt) 的完整矩阵
     */
    public static AffineMatrix objectToPage(Box localBox, AffineMatrix ctm, double pageX0Mm, double pageY0Mm, double pageHMm) {
        double bx = localBox == null ? 0 : localBox.getX();
        double by = localBox == null ? 0 : localBox.getY();
        // 1) 外接矩形定位：先平移（外接矩形左上角 + 页面框偏移），再叠加 CTM（设计 3.2 节）
        AffineMatrix translate = AffineMatrix.translate(bx + pageX0Mm, by + pageY0Mm);
        AffineMatrix objectToOfd = (ctm == null ? AffineMatrix.IDENTITY : ctm).multiply(translate);
        // 2) OFD 页面坐标 (mm, Y 向下) → PDF 页面坐标 (pt, Y 向上)：y_pdf = (pageH - y) × k
        AffineMatrix flip = new AffineMatrix(CONTENT_MM_TO_PT, 0, 0, -CONTENT_MM_TO_PT, 0, pageHMm * CONTENT_MM_TO_PT);
        return objectToOfd.multiply(flip);
    }

    /**
     * 页内坐标系 → PDF 页面坐标矩阵
     * <p>
     * 与 {@link #objectToPage(Box, AffineMatrix, double, double, double)} 的区别在于<b>不叠加外接矩形平移</b>：
     * 文字的 {@code TextCode/X/Y} 是相对外接矩形角点的偏移，布局阶段（{@code TextLayoutEngine}）
     * 已把角点与偏移合成为页内坐标，因此文字渲染只叠加容器 CTM 与页面换算，避免重复平移。
     *
     * @param ctm      图元及其父容器叠加后的 CTM，可为空表示单位矩阵
     * @param pageX0Mm 页面框左上角横坐标偏移，单位 mm
     * @param pageY0Mm 页面框左上角纵坐标偏移，单位 mm
     * @param pageHMm  页面高度，单位 mm
     * @return 页内坐标 (mm) → PDF 页面坐标 (pt) 的完整矩阵
     */
    public static AffineMatrix absoluteToPage(AffineMatrix ctm, double pageX0Mm, double pageY0Mm, double pageHMm) {
        return objectToPage(null, ctm, pageX0Mm, pageY0Mm, pageHMm);
    }

    /**
     * 对单个对象空间坐标点做完整换算
     *
     * @param xMm      对象空间横坐标，单位 mm
     * @param yMm      对象空间纵坐标，单位 mm
     * @param localBox 图元外接矩形，单位 mm，可为空
     * @param ctm      图元及其父容器叠加后的 CTM，可为空
     * @param pageHMm  页面高度，单位 mm
     * @return PDF 页面坐标 {@code [x, y]}，单位 pt
     */
    public static double[] objectPointToPage(double xMm, double yMm, Box localBox, AffineMatrix ctm, double pageHMm) {
        return objectToPage(localBox, ctm, 0, 0, pageHMm).transform(xMm, yMm);
    }

    /**
     * 解析 OFD 的 CTM 数组（行向量写法 {@code [a b c d e f]}）
     *
     * @param ctm 长度为 6 的数组，单位无量纲，可为空
     * @return 仿射矩阵，为空或长度不为 6 时返回单位矩阵
     */
    public static AffineMatrix fromCtm(double[] ctm) {
        if (ctm == null || ctm.length != 6) {
            return AffineMatrix.IDENTITY;
        }
        return new AffineMatrix(ctm[0], ctm[1], ctm[2], ctm[3], ctm[4], ctm[5]);
    }

    /**
     * 把 OFD 外接矩形（Boundary，单位 mm，左上角原点语义）取整为不可变矩形对象
     *
     * @param boundary topLeftX/topLeftY/width/height 的四元组，单位 mm
     * @return 矩形
     */
    public static Box boundary(double topLeftX, double topLeftY, double width, double height) {
        return new Box(topLeftX, topLeftY, width, height);
    }
}
