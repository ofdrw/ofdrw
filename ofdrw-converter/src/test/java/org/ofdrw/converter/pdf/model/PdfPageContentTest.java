package org.ofdrw.converter.pdf.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 中间模型契约测试
 * <p>
 * 验证中间模型与 PDFBox 彻底解耦、坐标语义明确、并可作为渲染器无关断言的载体（设计 4.1 节）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class PdfPageContentTest {

    /**
     * 构造一条最小可用路径（矩形）
     *
     * @return 矩形路径指令
     */
    private static PathCommand rectPath() {
        List<PathSegment> segments = new ArrayList<>();
        segments.add(new MoveTo(0, 0));
        segments.add(new LineTo(10, 0));
        segments.add(new LineTo(10, 10));
        segments.add(new LineTo(0, 10));
        segments.add(ClosePath.INSTANCE);
        return new PathCommand(segments, true, false, FillRule.NON_ZERO, PdfColor.BLACK, null);
    }

    /**
     * float 数组转 double 数组，便于使用带容差的数组断言
     *
     * @param values float 数组
     * @return double 数组
     */
    private static double[] toDouble(float[] values) {
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }
        return result;
    }

    @Test
    @DisplayName("页面内容：尺寸校验与 q/Q 成对维护")
    void pageContentState() {
        assertThrows(IllegalArgumentException.class, () -> new PdfPageContent(0, 100));
        assertThrows(IllegalArgumentException.class, () -> new PdfPageContent(100, -1));

        PdfPageContent content = new PdfPageContent(595.28, 841.89);
        assertTrue(content.isBalanced());
        // 无 q 时追加 Q 不产生指令
        content.restoreState();
        assertEquals(0, content.size());
        content.saveState().add(rectPath()).saveState().restoreState();
        assertFalse(content.isBalanced());
        assertEquals(1, content.stateDepth());
        content.restoreState();
        assertTrue(content.isBalanced());
        assertEquals(5, content.size());
        assertEquals(595.28, content.getWidthPt(), 1e-9);
        assertEquals(841.89, content.getHeightPt(), 1e-9);
        // 指令列表只读
        assertThrows(UnsupportedOperationException.class, () -> content.getCommands().clear());
    }

    @Test
    @DisplayName("页面内容：空指令拒绝")
    void rejectsNullCommand() {
        PdfPageContent content = new PdfPageContent(100, 100);
        assertThrows(IllegalArgumentException.class, () -> content.add(null));
        // null 列表与含 null 元素的列表：null 列表直接忽略
        assertSame(content, content.addAll(null));
    }

    @Test
    @DisplayName("路径指令：首元素必须是 MoveTo，且必须填充或描边")
    void pathCommandValidation() {
        List<PathSegment> noMove = Collections.singletonList(new LineTo(1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new PathCommand(noMove, true, false, FillRule.NON_ZERO, PdfColor.BLACK, null));
        List<PathSegment> ok = Collections.singletonList(new MoveTo(0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PathCommand(ok, false, false, FillRule.NON_ZERO, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new PathCommand(ok, true, false, FillRule.NON_ZERO, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new PathCommand(Collections.<PathSegment>emptyList(), true, true, FillRule.NON_ZERO, PdfColor.BLACK, PdfColor.BLACK));

        PathCommand both = new PathCommand(ok, true, true, FillRule.EVEN_ODD, PdfColor.BLACK, PdfColor.WHITE);
        assertEquals(FillRule.EVEN_ODD, both.getFillRule());
        assertEquals(CoordSpace.PAGE, both.getCoordSpace());
        assertTrue(both.isFill() && both.isStroke());
        assertEquals(1, both.getSegments().size());
    }

    @Test
    @DisplayName("颜色：CMYK 与原颜色空间保留（决策：CMYK 不得转 RGB）")
    void pdfColor() {
        PdfColor cmyk = PdfColor.cmyk(0, 0, 0, 1);
        assertEquals(PdfColor.ColorSpace.CMYK, cmyk.getColorSpace());
        assertEquals(4, cmyk.getComponentCount());
        assertEquals(1f, cmyk.getChannel(3), 1e-6);
        assertEquals(1f, cmyk.getOpacity(), 1e-6);
        // 通道越界截断
        PdfColor clamped = PdfColor.rgb(-1f, 2f, 0.5f);
        assertArrayEquals(new double[]{0d, 1d, 0.5d}, toDouble(clamped.getChannels()), 1e-6);
        // 不透明度截断与替换
        assertEquals(0f, PdfColor.rgb(0, 0, 0).withOpacity(-3f).getOpacity(), 1e-6);
        assertEquals(0.5f, PdfColor.rgb(0, 0, 0).withOpacity(0.5f).getOpacity(), 1e-6);
        // 通道数校验
        assertThrows(IllegalArgumentException.class, () -> new PdfColor(PdfColor.ColorSpace.CMYK, 1f, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PdfColor(null, 1f, 0, 0, 0));
        // 防御性拷贝
        float[] channels = clamped.getChannels();
        channels[0] = 9f;
        assertEquals(0f, clamped.getChannel(0), 1e-6);
        // 相等性
        assertEquals(PdfColor.rgb(0.1f, 0.2f, 0.3f), PdfColor.rgb(0.1f, 0.2f, 0.3f));
        assertNotNull(PdfColor.BLACK.toString());
    }

    @Test
    @DisplayName("透明度：OFD Alpha 缺省按不透明处理（设计决策 2）")
    void alphaDefault() {
        assertEquals(1f, SetAlpha.fromOfd(null).getFillAlpha(), 1e-6);
        assertEquals(1f, SetAlpha.fromOfd(255).getFillAlpha(), 1e-6);
        SetAlpha half = SetAlpha.fromOfd(128);
        assertEquals(128 / 255f, half.getFillAlpha(), 1e-6);
        assertEquals(half.getFillAlpha(), half.getStrokeAlpha(), 1e-6);
        assertEquals(0f, SetAlpha.of(-1f).getFillAlpha(), 1e-6);
    }

    @Test
    @DisplayName("仿射矩阵：乘法顺序与逆矩阵")
    void affineMatrix() {
        AffineMatrix translate = AffineMatrix.translate(10, 20);
        AffineMatrix scale = AffineMatrix.scale(2, 3);
        // 先平移再缩放：点 (1,1) → (11,21) → (22,63)
        double[] p = translate.multiply(scale).transform(1, 1);
        assertArrayEquals(new double[]{22, 63}, p, 1e-9);
        // 先缩放再平移：(1,1) → (2,3) → (12,23)
        double[] q = scale.multiply(translate).transform(1, 1);
        assertArrayEquals(new double[]{12, 23}, q, 1e-9);
        // 逆矩阵自洽
        AffineMatrix m = new AffineMatrix(2, 0.5, -0.5, 3, 5, 7);
        double[] raw = m.transform(4, 9);
        double[] original = m.inverse().transform(raw[0], raw[1]);
        assertArrayEquals(new double[]{4, 9}, original, 1e-9);
        assertEquals(AffineMatrix.IDENTITY, AffineMatrix.IDENTITY.multiply(AffineMatrix.IDENTITY));
    }

    @Test
    @DisplayName("虚线：全 0 退化为实线，负值按 0 处理")
    void dashPattern() {
        assertTrue(DashPattern.SOLID.isSolid());
        assertTrue(new DashPattern(new double[]{0, 0}, 0).isSolid());
        assertTrue(new DashPattern(null, 1).isSolid());
        DashPattern dash = new DashPattern(new double[]{3, -1, 2}, 1.5);
        assertFalse(dash.isSolid());
        assertArrayEquals(new double[]{3, 0, 2}, dash.getDashArray(), 1e-9);
        assertEquals(1.5, dash.getDashPhase(), 1e-9);
    }

    @Test
    @DisplayName("文字指令：字形序列与可提取文本")
    void textCommand() {
        List<GlyphRun> glyphs = new ArrayList<>();
        glyphs.add(new GlyphRun(3, "a", 0, 0, 5));
        glyphs.add(new GlyphRun(7, "b", 5, 0, 5));
        TextCommand text = new TextCommand("F1", 12, AffineMatrix.IDENTITY, glyphs, PdfColor.BLACK);
        assertEquals("ab", text.getUnicodeText());
        assertEquals(2, text.getGlyphs().size());
        assertTrue(text.getGlyphs().get(0).hasGlyphId());
        assertFalse(new GlyphRun(-1, "x", 0, 0, 0).hasGlyphId());
        assertEquals(CoordSpace.PAGE, text.getCoordSpace());
        assertThrows(IllegalArgumentException.class,
                () -> new TextCommand("", 12, AffineMatrix.IDENTITY, glyphs, PdfColor.BLACK));
        assertThrows(IllegalArgumentException.class,
                () -> new TextCommand("F1", 0, AffineMatrix.IDENTITY, glyphs, PdfColor.BLACK));
        assertThrows(IllegalArgumentException.class,
                () -> new TextCommand("F1", 12, AffineMatrix.IDENTITY, Collections.<GlyphRun>emptyList(), PdfColor.BLACK));
    }

    @Test
    @DisplayName("图像指令：放置矩阵与数据防御性拷贝")
    void imageCommand() {
        byte[] data = new byte[]{1, 2, 3};
        Box placement = new Box(100, 200, 50, 25);
        ImageCommand image = new ImageCommand("Im1", org.ofdrw.converter.pdf.font.ImageEncoding.JPEG,
                640, 480, data, placement, false);
        data[0] = 9;
        assertEquals(1, image.getImageData()[0]);
        double[] m = image.getPlacementMatrix().toArray();
        assertArrayEquals(new double[]{50, 0, 0, 25, 100, 200}, m, 1e-9);
        assertTrue(image.getEncoding().isLossy());
        assertFalse(org.ofdrw.converter.pdf.font.ImageEncoding.PNG.isLossy());
        assertEquals(org.ofdrw.converter.pdf.font.ImageEncoding.JPEG,
                org.ofdrw.converter.pdf.font.ImageEncoding.ofFormat("jpg"));
        assertEquals(org.ofdrw.converter.pdf.font.ImageEncoding.RAW,
                org.ofdrw.converter.pdf.font.ImageEncoding.ofFormat("BMP"));
        assertThrows(IllegalArgumentException.class,
                () -> new ImageCommand("Im1", org.ofdrw.converter.pdf.font.ImageEncoding.JPEG, 0, 1,
                        new byte[]{1}, placement, false));
    }

    @Test
    @DisplayName("渐变指令：至少两个停止点，坐标停止截断")
    void shadingCommand() {
        List<PathSegment> area = Collections.<PathSegment>singletonList(new MoveTo(0, 0));
        List<ShadingCommand.ColorStop> stops = new ArrayList<>();
        stops.add(new ShadingCommand.ColorStop(-1, PdfColor.BLACK));
        stops.add(new ShadingCommand.ColorStop(2, PdfColor.WHITE));
        ShadingCommand shading = new ShadingCommand(ShadingCommand.ShadingType.AXIAL, stops,
                ShadingCommand.Extend.PAD, area, FillRule.NON_ZERO, 0, 0, 100, 0, 1, 0);
        assertEquals(0, shading.getStops().get(0).getFraction(), 1e-9);
        assertEquals(1, shading.getStops().get(1).getFraction(), 1e-9);
        assertEquals(ShadingCommand.Extend.PAD, shading.getExtend());
        assertThrows(IllegalArgumentException.class,
                () -> new ShadingCommand(ShadingCommand.ShadingType.AXIAL,
                        Collections.singletonList(new ShadingCommand.ColorStop(0, PdfColor.BLACK)),
                        ShadingCommand.Extend.NONE, area, FillRule.NON_ZERO, 0, 0, 1, 1, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ShadingCommand(ShadingCommand.ShadingType.RADIAL, stops,
                        ShadingCommand.Extend.NONE, Collections.<PathSegment>emptyList(), FillRule.NON_ZERO, 0, 0, 1, 1, 1, 0));
    }

    @Test
    @DisplayName("裁剪指令：并集标记与填充规则")
    void clipCommand() {
        List<PathSegment> segments = Collections.<PathSegment>singletonList(new MoveTo(0, 0));
        ClipCommand clip = new ClipCommand(segments, FillRule.EVEN_ODD, true);
        assertTrue(clip.isUnionArea());
        assertEquals(FillRule.EVEN_ODD, clip.getFillRule());
        assertThrows(IllegalArgumentException.class,
                () -> new ClipCommand(Collections.<PathSegment>emptyList(), FillRule.NON_ZERO, false));
    }

    @Test
    @DisplayName("渲染器无关：指令导出文本可用于断言绘制顺序")
    void dumpText() {
        PdfPageContent content = new PdfPageContent(100, 100);
        content.saveState()
                .add(new StrokeStyle(2, LineCap.ROUND, LineJoin.BEVEL, 4, new DashPattern(new double[]{3, 2}, 0)))
                .add(rectPath())
                .restoreState();
        String dump = PdfContentWriter.dump(content);
        assertTrue(dump.contains("q"));
        assertTrue(dump.contains("w 2.0 J ROUND j BEVEL M 4.0 d Dash[3.0, 2.0] phase=0.0"));
        assertTrue(dump.contains("paint=f"));
        assertTrue(dump.contains("Q"));
        assertEquals(4, dump.trim().split("\n").length - 1);
        assertTrue(PdfContentWriter.contains(content, StrokeStyle.class));
        assertFalse(PdfContentWriter.contains(content, TextCommand.class));
        assertEquals("B", DumpFormatter.paintOperator(FillRule.NON_ZERO, true, true));
        assertEquals("B*", DumpFormatter.paintOperator(FillRule.EVEN_ODD, true, true));
        assertEquals("S", DumpFormatter.paintOperator(FillRule.NON_ZERO, false, true));
    }

    @Test
    @DisplayName("枚举解析：填充规则、线端、线连接与 OFD 文本对齐")
    void enumParsing() {
        assertEquals(FillRule.EVEN_ODD, FillRule.of("Even-Odd"));
        assertEquals(FillRule.NON_ZERO, FillRule.of("NonZero"));
        assertEquals(FillRule.NON_ZERO, FillRule.of(null));
        assertEquals(LineCap.ROUND, LineCap.of("Round"));
        assertEquals(LineCap.BUTT, LineCap.of("unknown"));
        assertEquals(LineJoin.BEVEL, LineJoin.of("Bevel"));
        assertEquals(LineJoin.MITER, LineJoin.of(null));
        // 默认坐标作用域为页面坐标，需要对象空间时必须显式指定
        PathCommand objectSpace = new PathCommand(CoordSpace.OBJECT, Collections.singletonList(new MoveTo(0, 0)),
                true, false, FillRule.NON_ZERO, PdfColor.BLACK, null);
        assertEquals(CoordSpace.PAGE, rectPath().getCoordSpace());
        assertEquals(CoordSpace.OBJECT, objectSpace.getCoordSpace());
    }

    @Test
    @DisplayName("边框：圆角半径不超过短边一半")
    void imageBorder() {
        ImageBorderCommand border = new ImageBorderCommand(new Box(0, 0, 20, 10), 1, 100,
                PdfColor.BLACK, DashPattern.SOLID, null);
        assertEquals(5, border.getEffectiveCornerRadiusPt(), 1e-9);
        assertThrows(IllegalArgumentException.class,
                () -> new ImageBorderCommand(null, 1, 0, PdfColor.BLACK, null, null));
    }

    @Test
    @DisplayName("表单与底纹指令：标识校验")
    void formAndPattern() {
        FormCommand form = new FormCommand("Fm1", null);
        assertEquals(AffineMatrix.IDENTITY, form.getPlacementMatrix());
        assertThrows(IllegalArgumentException.class, () -> new FormCommand("", null));

        List<ContentCommand> cell = Collections.<ContentCommand>singletonList(new RectCommand(new Box(0, 0, 5, 5), PdfColor.BLACK));
        PatternCommand pattern = new PatternCommand("Pattern1", new Box(0, 0, 10, 10), cell, true);
        assertTrue(pattern.isApproximate());
        assertThrows(IllegalArgumentException.class,
                () -> new PatternCommand("p", new Box(0, 0, 0, 10), cell, false));
        assertThrows(IllegalArgumentException.class,
                () -> new PatternCommand("p", new Box(0, 0, 10, 10), Collections.<ContentCommand>emptyList(), false));
        assertEquals("% ofd", new MarkerCommand("ofd").toString());
    }
}
