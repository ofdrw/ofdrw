package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.text.CT_CGTransform;
import org.ofdrw.core.text.TextCode;

import java.io.IOException;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 文字 run 构造器：把连续的同字体、同绘制属性的字形组织为一个 OFD 文字对象
 * <p>
 * 几何约定（均以页面 mm 为单位，与 {@code Pg} 的值域一致）：
 * <ul>
 *     <li>{@code Pg}：页面画布的当前变换 {@code Graphics2D#getTransform()}，把 PDF 用户空间（pt、y 向上）映射到 OFD 页面 mm（y 向下）；</li>
 *     <li>{@code TRM}：PDFBox 文字渲染矩阵，把文字空间（em 为单位）映射到 PDF 用户空间；</li>
 *     <li>{@code N = Pg × TRM}，对每个字形：{@code origin = N·(0,0)}、{@code em = |N·(0,1)|}；
 *     {@code linear} 为 {@code N} 的线性部分除以 {@code em}（y 列长度为 1，x 列长度即水平缩放）；</li>
 *     <li>Delta 为相邻字形 origin 之差经 {@code linear} 逆变换到对象坐标系的结果；</li>
 *     <li>首版不写 HScale：非等比缩放已包含在 {@code linear} 的 x 列中。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class TextRunBuilder {

    /**
     * 单个文字对象包含的最大字形数，超过后分片
     */
    public static final int MAX_GLYPHS_PER_RUN = Integer.getInteger("ofdrw.textobj.maxGlyphs", 2000);

    /**
     * 缺 ToUnicode 且无法反查时使用的占位符（GB/T 33190-2016 规定）
     */
    static final String PLACEHOLDER = "\u00A4";

    /**
     * 包围盒外扩比例（1%），避免查看器因浮点误差裁掉边缘
     */
    private static final double BOUNDARY_PADDING = 0.01;

    /**
     * 文字所在的 PDF 字体
     */
    private final PDFont font;

    /**
     * 该 run 的绘制属性快照
     */
    private final TextRunStyle style;

    /**
     * 字形集合
     */
    private final List<Glyph> glyphs = new ArrayList<>();

    /**
     * 字形轮廓在页面毫米下的包围盒并集（minX, minY, maxX, maxY）
     */
    private double minX = Double.MAX_VALUE;
    private double minY = Double.MAX_VALUE;
    private double maxX = -Double.MAX_VALUE;
    private double maxY = -Double.MAX_VALUE;

    /**
     * 字形原点在页面毫米下的包围盒并集（minOriginX, minOriginY, maxOriginX, maxOriginY）
     */
    private double minOriginX = Double.MAX_VALUE;
    private double minOriginY = Double.MAX_VALUE;
    private double maxOriginX = -Double.MAX_VALUE;
    private double maxOriginY = -Double.MAX_VALUE;

    /**
     * run 的首个字形
     */
    private Glyph first;

    /**
     * 字体上伸部（页面 mm，自基线向上），取 run 内最大值
     */
    private double ascent = 0d;

    /**
     * 字体下伸部（页面 mm，自基线向下），取 run 内最大值
     */
    private double descent = 0d;

    /**
     * 创建文字 run
     *
     * @param font  字体
     * @param style 绘制属性快照
     */
    TextRunBuilder(PDFont font, TextRunStyle style) {
        this.font = font;
        this.style = style;
    }

    /**
     * 判断当前 run 能否接纳新字形
     * <p>
     * 服务端字形逐字发出，任意相邻字形都可以处在同一 run 中（位置由 Delta 表达），
     * 只限制单 run 的字形数量以控制单个对象体积。
     *
     * @return true - 可以接纳
     */
    boolean accepts() {
        return glyphs.size() < MAX_GLYPHS_PER_RUN;
    }

    /**
     * 获取已收录的字形数
     *
     * @return 字形数
     */
    int size() {
        return glyphs.size();
    }

    /**
     * 加入一个字形
     *
     * @param glyph 字形（已计算好页面毫米下的 origin 与 em）
     */
    void add(Glyph glyph) {
        if (first == null) {
            first = glyph;
        }
        ascent = Math.max(ascent, glyph.ascent);
        descent = Math.max(descent, glyph.descent);
        glyphs.add(glyph);
        // 累加包围盒：轮廓盒
        if (glyph.box != null) {
            minX = Math.min(minX, glyph.box.getMinX());
            minY = Math.min(minY, glyph.box.getMinY());
            maxX = Math.max(maxX, glyph.box.getMaxX());
            maxY = Math.max(maxY, glyph.box.getMaxY());
        }
        // 累加基线盒：为空格等无轮廓字形提供兜底，保证 origin 一定落在盒内
        minOriginX = Math.min(minOriginX, Math.min(glyph.originX, glyph.originX + glyph.em));
        minOriginY = Math.min(minOriginY, glyph.originY);
        maxOriginX = Math.max(maxOriginX, Math.max(glyph.originX, glyph.originX + glyph.em));
        maxOriginY = Math.max(maxOriginY, glyph.originY);
    }

    /**
     * 构造 OFD 文字对象
     * <p>
     * TextObject 不写入文档资源，字体引用、GID 与字体子集化由调用方在写入阶段补齐。
     *
     * @param id               文字对象 ID
     * @param forceCgTransform 是否强制写 CGTransform（CID/Identity 字体总是 true）
     * @return 文字对象
     */
    TextObject build(ST_ID id, boolean forceCgTransform) {
        TextObject text = new TextObject(id);
        Glyph g0 = first;
        double size = g0.em;

        // 1. 字号
        text.setSize(size);
        /*
         * 包围盒：
         * 1) 顶端取“首个字形基线 − 字体 ascent”，使 TextCode.Y ≈ ascent（与查看器按字体基线定位的行为一致，
         *    避免 Boundary 贴着字形顶端导致基线系统性下移）；
         * 2) 底端取 min(字形轮廓底, 基线 + 字体 descent)，保证下伸部不被裁掉；
         * 3) 左右覆盖字形轮廓与全部字形 origin（末位空格）；
         * 4) 四边外扩 1%，吸收浮点误差与查看器的取整差异。
         */
        double topY = g0.originY - ascent;
        double bottomY = Math.max(maxY, maxOriginY + descent);
        double boxMinX = Math.min(minX, minOriginX);
        double boxMinY = Math.min(topY, bottomY);
        double boxMaxX = Math.max(maxX, maxOriginX);
        double boxMaxY = Math.max(bottomY, topY);
        double w = boxMaxX - boxMinX;
        double h = boxMaxY - boxMinY;
        double padX = Math.abs(w) * BOUNDARY_PADDING + 1e-4;
        double padY = Math.abs(h) * BOUNDARY_PADDING + 1e-4;
        text.setBoundary(new ST_Box(nz(boxMinX - padX), nz(boxMinY - padY), nz(w + 2 * padX), nz(h + 2 * padY)));

        double bx = nz(boxMinX - padX);
        double by = nz(boxMinY - padY);
        if (!isIdentityLinear(g0.linear)) {
            // 旋转/镜像/剪切/缩放：线性部分写入 CTM，平移量取首个字形原点
            text.setCTM(new ST_Array(nz(g0.linear.getScaleX()), nz(g0.linear.getShearY()),
                    nz(g0.linear.getShearX()), nz(g0.linear.getScaleY()),
                    nz(g0.originX), nz(g0.originY)));
        }
        // 文字定位：TextCode 的 X/Y 是首个字形原点相对 Boundary 左上角的偏移（GB/T 33190-2016 表 46）
        double stampX = nz(g0.originX - bx);
        double stampY = nz(g0.originY - by);

        // 2. TextCode：Unicode 文本 + Delta（相邻字形 origin 之差反变换到对象坐标系）
        StringBuilder content = new StringBuilder();
        List<double[]> positions = new ArrayList<>(glyphs.size());
        for (Glyph g : glyphs) {
            content.append(g.unicode);
            positions.add(new double[]{g.originX, g.originY});
        }
        TextCode code = new TextCode();
        code.setContent(content.toString());
        code.setCoordinate(stampX, stampY);
        if (glyphs.size() > 1) {
            double[] dx = new double[glyphs.size() - 1];
            double[] dy = new double[glyphs.size() - 1];
            for (int i = 1; i < glyphs.size(); i++) {
                double cx = positions.get(i)[0] - positions.get(i - 1)[0];
                double cy = positions.get(i)[1] - positions.get(i - 1)[1];
                dx[i - 1] = linearCoord(g0.linear, cx, cy, true);
                dy[i - 1] = linearCoord(g0.linear, cx, cy, false);
            }
            code.setDeltaX(toArray(dx));
            code.setDeltaY(toArray(dy));
        }
        text.addTextCode(code);

        // 3. CGTransform：按字体给出的最终 GID 写，字符位置为 UTF-16 偏移
        int[] codePositions = new int[glyphs.size()];
        int[] codeCounts = new int[glyphs.size()];
        int pos = 0;
        for (int i = 0; i < glyphs.size(); i++) {
            Glyph g = glyphs.get(i);
            codePositions[i] = pos;
            codeCounts[i] = g.unicode.length();
            pos += g.unicode.length();
        }
        if (forceCgTransform || needsCgTransform(codeCounts)) {
            for (CT_CGTransform transform : makeTransforms(codePositions, codeCounts, glyphs)) {
                text.addCGTransform(transform);
            }
        }
        // 简单字体且映射一对一、无占位符时不写 CGTransform，由查看器按内嵌字体 cmap 定位

        // 4. 绘制属性
        text.setFill(style.fill);
        text.setStroke(style.stroke);
        if (style.fillColor != null) {
            text.setFillColor(style.fillColor());
        }
        if (style.strokeColor != null) {
            text.setStrokeColor(style.strokeColor());
        }
        if (style.stroke) {
            text.setLineWidth(style.lineWidth);
        }
        if (style.alpha < 255) {
            text.setAlpha(style.alpha);
        }

        return text;
    }

    /**
     * 判断是否需要写 CGTransform：任意字形对应多个字符或占位符时
     *
     * @param codeCounts 各字形对应的字符数
     * @return true - 需要写 CGTransform
     */
    private static boolean needsCgTransform(int[] codeCounts) {
        for (int count : codeCounts) {
            if (count != 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把一组一对一映射拆成多个 CGTransform（每个字形一条）
     *
     * @param codePositions 各字形在 TextCode 中的 UTF-16 起始偏移
     * @param codeCounts    各字形对应的字符数
     * @param glyphs        字形集合
     * @return CGTransform 列表
     */
    private static List<CT_CGTransform> makeTransforms(int[] codePositions, int[] codeCounts, List<Glyph> glyphs) {
        // 合并连续的一对一映射：CodePosition 取区间起点，CodeCount 为区间字符数，Glyphs 依次给出对应 GID
        List<CT_CGTransform> list = new ArrayList<>();
        int i = 0;
        while (i < glyphs.size()) {
            int start = i;
            List<Integer> gids = new ArrayList<>();
            int codes = 0;
            while (i < glyphs.size() && codeCounts[i] == 1) {
                gids.add(glyphs.get(i).gid);
                codes++;
                i++;
            }
            if (codes == 0) {
                // 非一对一映射（连字、多字符），单独成段
                gids.add(glyphs.get(i).gid);
                list.add(new CT_CGTransform()
                        .setCodePosition(codePositions[i])
                        .setCodeCount(codeCounts[i])
                        .setGlyphCount(1)
                        .setGlyphs(new ST_Array(gids.get(0))));
                i++;
                continue;
            }
            list.add(new CT_CGTransform()
                    .setCodePosition(codePositions[start])
                    .setCodeCount(codes)
                    .setGlyphCount(gids.size())
                    .setGlyphs(toArray(gids)));
        }
        return list;
    }

    /**
     * 计算对象坐标系下与页面位移对应的坐标分量：{@code inverse(linear) · (dx, dy)}
     *
     * @param linear 对象坐标系到页面坐标系的线性部分
     * @param dx     页面 x 方向位移，单位 mm
     * @param dy     页面 y 方向位移，单位 mm
     * @param x      为 true 取 x 分量，否则取 y 分量
     * @return 对象坐标系下的分量
     */
    static double linearCoord(AffineTransform linear, double dx, double dy, boolean x) {
        try {
            Point2D p = linear.createInverse().deltaTransform(new Point2D.Double(dx, dy), null);
            return x ? p.getX() : p.getY();
        } catch (NoninvertibleTransformException e) {
            // 线性部分不可逆（退化文字矩阵）时退化为页面位移
            return x ? dx : dy;
        }
    }

    /**
     * 判断线性部分是否近似单位矩阵
     *
     * @param linear 线性部分
     * @return true - 近似单位矩阵
     */
    static boolean isIdentityLinear(AffineTransform linear) {
        return Math.abs(linear.getScaleX() - 1) < 1e-6
                && Math.abs(linear.getScaleY() - 1) < 1e-6
                && Math.abs(linear.getShearX()) < 1e-6
                && Math.abs(linear.getShearY()) < 1e-6;
    }

    /**
     * 数值归一化：避免 -0.0 与过长尾数
     *
     * @param v 数值
     * @return 归一化后的数值
     */
    static double nz(double v) {
        double r = Math.round(v * 1000d) / 1000d;
        return r == 0d ? 0d : r;
    }

    /**
     * double 数组转 OFD 数组
     *
     * @param arr 数值数组
     * @return OFD 数组
     */
    private static ST_Array toArray(double[] arr) {
        Double[] num = new Double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            num[i] = nz(arr[i]);
        }
        return new ST_Array(num);
    }

    /**
     * int 列表转 OFD 数组
     *
     * @param arr 数值列表
     * @return OFD 数组
     */
    private static ST_Array toArray(List<Integer> arr) {
        Integer[] num = new Integer[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            num[i] = arr.get(i);
        }
        return new ST_Array(num);
    }

    /**
     * 单个字形在页面毫米下的几何与内容信息
     */
    static final class Glyph {
        /**
         * 字形在 TextCode 中的 Unicode 文本（至少 1 个字符）
         */
        final String unicode;
        /**
         * 字体中的原始 GID
         */
        final int gid;
        /**
         * 字形原点在页面坐标系的 x，单位 mm
         */
        final double originX;
        /**
         * 字形原点在页面坐标系的 y，单位 mm
         */
        final double originY;
        /**
         * 1 em 在页面上的长度，单位 mm
         */
        final double em;
        /**
         * 对象坐标系到页面坐标系的线性部分
         */
        final AffineTransform linear;
        /**
         * 字形轮廓在页面坐标系下的包围盒，无轮廓时为空
         */
        final Rectangle2D box;

        /**
         * 字体上伸部在页面上的长度（自基线向上），单位 mm
         */
        final double ascent;

        /**
         * 字体下伸部在页面上的长度（自基线向下，正值），单位 mm
         */
        final double descent;

        Glyph(String unicode, int gid, double originX, double originY, double em,
              AffineTransform linear, Rectangle2D box, double ascent, double descent) {
            this.unicode = unicode;
            this.gid = gid;
            this.originX = originX;
            this.originY = originY;
            this.em = em;
            this.linear = linear;
            this.box = box;
            this.ascent = ascent;
            this.descent = descent;
        }
    }

    /**
     * run 的绘制属性快照
     */
    static final class TextRunStyle {
        /**
         * 是否填充
         */
        final boolean fill;
        /**
         * 是否描边
         */
        final boolean stroke;
        /**
         * 填充色的 RGB 值（0~255）；null 表示非 Device 颜色空间，使用 {@link #fillAwt}
         */
        final float[] fillColor;
        /**
         * 描边色的 RGB 值（0~255）；null 表示非 Device 颜色空间，使用 {@link #strokeAwt}
         */
        final float[] strokeColor;
        /**
         * 填充色的 AWT 近似颜色（仅当 {@link #fillColor} 为 null 时使用）
         */
        final java.awt.Color fillAwt;
        /**
         * 描边色的 AWT 近似颜色（仅当 {@link #strokeColor} 为 null 时使用）
         */
        final java.awt.Color strokeAwt;
        /**
         * 描边线的线宽，单位 mm
         */
        final double lineWidth;
        /**
         * 透明度 0~255
         */
        final int alpha;

        /**
         * 创建绘制属性快照
         *
         * @param fill        是否填充
         * @param stroke      是否描边
         * @param fillColor   填充色
         * @param strokeColor 描边色
         * @param lineWidth   描边线宽，单位 mm
         * @param alpha       透明度 0~255
         */
        TextRunStyle(boolean fill, boolean stroke, PDColor fillColor, PDColor strokeColor,
                     double lineWidth, int alpha) {
            this.fill = fill;
            this.stroke = stroke;
            this.fillColor = rgbOf(fillColor);
            this.strokeColor = rgbOf(strokeColor);
            this.fillAwt = awtApprox(fillColor);
            this.strokeAwt = awtApprox(strokeColor);
            this.lineWidth = lineWidth;
            this.alpha = alpha;
        }

        /**
         * 构造 OFD 填充色
         *
         * @return 填充色
         */
        CT_Color fillColor() {
            return toOfdColor(fillColor, fillAwt);
        }

        /**
         * 构造 OFD 描边色
         *
         * @return 描边色
         */
        CT_Color strokeColor() {
            return toOfdColor(strokeColor, strokeAwt);
        }

        /**
         * 生成 OFD 颜色：Device 颜色空间取直接映射值，其它颜色空间取 AWT 近似值
         *
         * @param rgb     RGB 值（0~255），可为 null
         * @param approx  AWT 近似颜色
         * @return OFD 颜色
         */
        private static CT_Color toOfdColor(float[] rgb, java.awt.Color approx) {
            if (rgb != null) {
                return new CT_Color().setValue(new ST_Array(Math.round(rgb[0]), Math.round(rgb[1]), Math.round(rgb[2])));
            }
            return new CT_Color().setValue(new ST_Array(approx.getRed(), approx.getGreen(), approx.getBlue()));
        }

        /**
         * 取颜色的 RGB 值（0~255）
         * <p>
         * DeviceGray/RGB/CMYK 直接映射，其它颜色空间返回 null（由调用方用 AWT 近似）
         *
         * @param color PD 颜色
         * @return {r, g, b}，无法直接映射时返回 null
         */
        private static float[] rgbOf(PDColor color) {
            if (color == null) {
                return null;
            }
            PDColorSpace cs = color.getColorSpace();
            if (cs == null) {
                return null;
            }
            String name = cs.getName();
            float[] comps = color.getComponents();
            if ("DeviceGray".equals(name) && comps.length >= 1) {
                float v = clamp01(comps[0]) * 255f;
                return new float[]{v, v, v};
            }
            if ("DeviceRGB".equals(name) && comps.length >= 3) {
                return new float[]{clamp01(comps[0]) * 255f, clamp01(comps[1]) * 255f, clamp01(comps[2]) * 255f};
            }
            if ("DeviceCMYK".equals(name) && comps.length >= 4) {
                // CMYK -> RGB（与 PDFBox 渲染时的近似公式一致）
                float k = clamp01(comps[3]);
                return new float[]{(1 - clamp01(comps[0])) * (1 - k) * 255f,
                        (1 - clamp01(comps[1])) * (1 - k) * 255f,
                        (1 - clamp01(comps[2])) * (1 - k) * 255f};
            }
            return null;
        }

        /**
         * 用 PDColor 的 toRGB 近似得到 AWT 颜色
         *
         * @param color PD 颜色，可为 null
         * @return AWT 颜色，无法转换时返回黑色
         */
        private static java.awt.Color awtApprox(PDColor color) {
            if (color == null) {
                return java.awt.Color.BLACK;
            }
            try {
                return new java.awt.Color(color.toRGB());
            } catch (IOException | RuntimeException e) {
                float[] rgb = rgbOf(color);
                if (rgb != null) {
                    return new java.awt.Color((int) rgb[0], (int) rgb[1], (int) rgb[2]);
                }
                return java.awt.Color.BLACK;
            }
        }

        /**
         * 把通道值限制在 0~1
         *
         * @param v 通道值
         * @return 限制后的通道值
         */
        private static float clamp01(float v) {
            return v < 0 ? 0 : (v > 1 ? 1 : v);
        }
    }
}
