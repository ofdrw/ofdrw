package org.ofdrw.converter.pdf.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.PdfPageContent;
import org.ofdrw.converter.pdf.model.ShadingCommand;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.color.colorSpace.BitsPerComponent;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.color.colorSpace.CV;
import org.ofdrw.core.pageDescription.color.colorSpace.OFDColorSpaceType;
import org.ofdrw.core.pageDescription.color.colorSpace.Palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 颜色解析测试（P3，设计 3.4 节）
 * <p>
 * 锁定：GRAY/RGB/CMYK 三色空间、<b>CMYK 不转 RGB</b>、{@code BitsPerComponent} 位宽归一化、
 * 调色板索引、ICC 降级告警、颜色 Alpha 与图元 Alpha 相乘（决策 2：缺省不透明）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class ColorResolverTest {

    /**
     * 断言容限
     */
    private static final double EPS = 1e-6;

    /**
     * 构造颜色
     *
     * @param value 通道值
     * @return 颜色
     */
    private static CT_Color fillColor(String... value) {
        // FillColor 是带 OFD 命名空间的 FillColor 元素：CT_Color 的 getColorByType() 依赖元素限定名，
        // 直接 new CT_Color() 会得到无命名空间的匿名元素
        org.ofdrw.core.graph.pathObj.FillColor color = new org.ofdrw.core.graph.pathObj.FillColor();
        color.setValue(new org.ofdrw.core.basicType.ST_Array((java.io.Serializable[]) value));
        return color;
    }

    /**
     * 构造颜色空间
     *
     * @param type      类型
     * @param bitsPerComponent 位宽，可为空
     * @param palette   调色板，可为空
     * @return 颜色空间
     */
    private static CT_ColorSpace newSpace(OFDColorSpaceType type, BitsPerComponent bitsPerComponent, Palette palette) {
        CT_ColorSpace colorSpace = new CT_ColorSpace();
        colorSpace.setType(type);
        if (bitsPerComponent != null) {
            colorSpace.setBitsPerComponent(bitsPerComponent);
        }
        if (palette != null) {
            colorSpace.setPalette(palette);
        }
        return colorSpace;
    }

    @Test
    @DisplayName("RGB：十进制与 #RRGGBB 十六进制通道")
    void rgb() {
        ColorResolver resolver = new ColorResolver(null);
        PdfColor decimal = resolver.resolve(fillColor("255", "128", "0"), "填充");
        assertNotNull(decimal);
        assertEquals(PdfColor.ColorSpace.RGB, decimal.getColorSpace());
        assertEquals(1.0, decimal.getChannel(0), EPS);
        assertEquals(128.0 / 255.0, decimal.getChannel(1), EPS);
        assertEquals(0.0, decimal.getChannel(2), EPS);

        // 归一化值（0~1 小数）原样使用，不按位宽归一化
        PdfColor normalized = resolver.resolve(fillColor("0.2", "0.4", "0.6"), "填充");
        assertEquals(0.2, normalized.getChannel(0), EPS);
        assertEquals(0.4, normalized.getChannel(1), EPS);
        assertEquals(0.6, normalized.getChannel(2), EPS);

        // 十六进制单通道：按位宽归一化（#FF → 1.0，#80 → 0.5）
        PdfColor hexGray = resolver.resolve(fillColor("#80"), "填充");
        assertEquals(PdfColor.ColorSpace.GRAY, hexGray.getColorSpace());
        assertEquals(128.0 / 255.0, hexGray.getChannel(0), 1e-3);
    }

    @Test
    @DisplayName("CMYK：保持原颜色空间，绝不转 RGB")
    void cmyk() {
        ColorResolver resolver = new ColorResolver(null);
        PdfColor cmyk = resolver.resolve(fillColor("0", "100", "100", "0"), "填充");
        assertNotNull(cmyk);
        assertEquals(PdfColor.ColorSpace.CMYK, cmyk.getColorSpace(), "CMYK 必须保持原颜色空间（设计 3.4）");
        assertEquals(4, cmyk.getComponentCount());
        // 通道值按 8 位量化值归一化（与 decimal/normalized/palette 等用例同一口径）：100/255
        assertEquals(100.0 / 255.0, cmyk.getChannel(1), EPS);
        assertEquals(0.0, cmyk.getChannel(3), EPS);
    }

    @Test
    @DisplayName("GRAY：单通道")
    void gray() {
        ColorResolver resolver = new ColorResolver(null);
        PdfColor gray = resolver.resolve(fillColor("64"), "填充");
        assertEquals(PdfColor.ColorSpace.GRAY, gray.getColorSpace());
        assertEquals(64.0 / 255.0, gray.getChannel(0), EPS);
    }

    @Test
    @DisplayName("颜色空间引用：类型与 BitsPerComponent 位宽归一化")
    void bitsPerComponent() {
        // 4 位颜色空间：通道上限 15
        CT_ColorSpace gray4 = newSpace(OFDColorSpaceType.GRAY, BitsPerComponent.BIT_4, null);
        ColorResolver resolver = new ColorResolver(id -> gray4);
        CT_Color c = fillColor("15");
        c.setColorSpace(new ST_RefID(1L));
        PdfColor resolved = resolver.resolve(c, "填充");
        assertEquals(1.0, resolved.getChannel(0), EPS, "4 位颜色的 15 应归一化为 1.0");
        assertEquals(PdfColor.ColorSpace.GRAY, resolved.getColorSpace());

        // 8 位颜色空间：通道上限 255
        CT_ColorSpace rgb8 = newSpace(OFDColorSpaceType.RGB, BitsPerComponent.BIT_8, null);
        ColorResolver resolver8 = new ColorResolver(id -> rgb8);
        CT_Color c8 = fillColor("255", "0", "0");
        c8.setColorSpace(new ST_RefID(2L));
        assertEquals(1.0, resolver8.resolve(c8, "填充").getChannel(0), EPS);
    }

    @Test
    @DisplayName("调色板：Index 查表取通道值")
    void palette() {
        Palette palette = new Palette();
        CV first = new CV(new org.ofdrw.core.basicType.ST_Array((java.io.Serializable[]) new String[]{"10", "20", "30"}));
        CV second = new CV(new org.ofdrw.core.basicType.ST_Array((java.io.Serializable[]) new String[]{"200", "100", "50"}));
        palette.addCV(first);
        palette.addCV(second);
        CT_ColorSpace rgbSpace = newSpace(OFDColorSpaceType.RGB, null, palette);
        ColorResolver resolver = new ColorResolver(id -> rgbSpace);

        org.ofdrw.core.graph.pathObj.FillColor indexed = new org.ofdrw.core.graph.pathObj.FillColor();
        indexed.setIndex(1);
        indexed.setColorSpace(new ST_RefID(3L));
        PdfColor resolved = resolver.resolve(indexed, "填充");
        assertNotNull(resolved);
        // 调色板取出的是“颜色空间通道值”：整数语义为量化值，按位宽（8 位 255）归一化
        assertEquals(PdfColor.ColorSpace.RGB, resolved.getColorSpace());
        assertEquals(200.0 / 255.0, resolved.getChannel(0), EPS);
        assertEquals(100.0 / 255.0, resolved.getChannel(1), EPS);
    }

    @Test
    @DisplayName("Alpha：缺省不透明（决策 2），颜色 Alpha × 图元 Alpha")
    void opacity() {
        assertEquals(1.0f, ColorResolver.opacity(null), EPS, "属性缺失视为不透明（决策 2）");
        assertEquals(0.0f, ColorResolver.opacity(0), EPS, "显式 0 为完全透明");
        assertEquals(1.0f, ColorResolver.opacity(255), EPS);
        assertEquals(0.5f, ColorResolver.opacity(128), 1e-2);

        // 颜色 Alpha=128、图元 Alpha=128 → 约 0.25
        ColorResolver resolver = new ColorResolver(null);
        CT_Color c = fillColor("0", "0", "0");
        c.setAlpha(128);
        PdfColor resolved = resolver.resolve(c, 128, "填充");
        assertEquals(ColorResolver.opacity(128) * ColorResolver.opacity(128), resolved.getOpacity(), 1e-6);

        // 缺省：颜色 alpha 缺失 + 图元 alpha 缺失 → 1.0
        assertEquals(1.0f, resolver.resolve(fillColor("0", "0", "0"), null, "填充").getOpacity(), EPS);
    }

    @Test
    @DisplayName("渐变/底纹颜色不返回单色；负通道按 0 处理")
    void nonSolid() {
        ColorResolver resolver = new ColorResolver(null);
        org.ofdrw.core.graph.pathObj.FillColor gradient = new org.ofdrw.core.graph.pathObj.FillColor();
        gradient.setColor(new org.ofdrw.core.pageDescription.color.color.CT_AxialShd());
        assertNull(resolver.resolve(gradient, "填充"), "渐变颜色由 ShadingRenderer 处理");

        PdfColor negative = resolver.resolve(fillColor("-5", "0", "0"), "填充");
        assertEquals(0.0, negative.getChannel(0), EPS);
    }

    @Test
    @DisplayName("ShadingRenderer：Extend 位标志 → PDF Extend 数组")
    void extendMapping() {
        assertFalse(ShadingRenderer.pdfExtendArray(org.ofdrw.core.pageDescription.color.color.Extend._0)[0]);
        assertFalse(ShadingRenderer.pdfExtendArray(org.ofdrw.core.pageDescription.color.color.Extend._0)[1]);
        boolean[] one = ShadingRenderer.pdfExtendArray(org.ofdrw.core.pageDescription.color.color.Extend._1);
        assertTrue(one[0]);
        assertFalse(one[1]);
        boolean[] two = ShadingRenderer.pdfExtendArray(org.ofdrw.core.pageDescription.color.color.Extend._2);
        assertFalse(two[0]);
        assertTrue(two[1]);
        boolean[] three = ShadingRenderer.pdfExtendArray(org.ofdrw.core.pageDescription.color.color.Extend._3);
        assertTrue(three[0]);
        assertTrue(three[1]);

        assertEquals(ShadingCommand.Extend.NONE,
                ShadingRenderer.extend(org.ofdrw.core.pageDescription.color.color.Extend._0));
        assertEquals(ShadingCommand.Extend.PAD,
                ShadingRenderer.extend(org.ofdrw.core.pageDescription.color.color.Extend._3));
    }

    @Test
    @DisplayName("ShadingRenderer：径向椭圆变换的推导")
    void ellipticalTransform() {
        // 正圆、无旋转 → 单位矩阵
        assertEquals(org.ofdrw.converter.pdf.model.AffineMatrix.IDENTITY,
                ShadingRenderer.ellipticalTransform(1.0, 0));
        // 偏心 2 → 纵向压扁 0.5（把椭圆还原成圆）
        org.ofdrw.converter.pdf.model.AffineMatrix squash = ShadingRenderer.ellipticalTransform(2.0, 0);
        assertEquals(1.0, squash.getA(), EPS);
        assertEquals(0.5, squash.getD(), EPS);
        // 旋转 90°（OFD 顺时针）→ 页面上为逆时针 90°
        org.ofdrw.converter.pdf.model.AffineMatrix rotate = ShadingRenderer.ellipticalTransform(1.0, 90);
        assertEquals(0.0, rotate.getA(), 1e-9);
        assertEquals(-1.0, rotate.getB(), 1e-9);
        assertEquals(1.0, rotate.getC(), 1e-9);
        assertEquals(0.0, rotate.getD(), 1e-9);
    }

    @Test
    @DisplayName("渐变渲染：停止点位置缺省均分、坐标经 CoordSystem 换算")
    void axialShading() {
        PdfPageContent content = new PdfPageContent(100 * CoordSystem.MM_TO_PT, 100 * CoordSystem.MM_TO_PT);
        RenderContext context = RenderContexts.create(100, 100, content, null);
        ShadingRenderer renderer = new ShadingRenderer(new ColorResolver(null));

        org.ofdrw.core.pageDescription.color.color.CT_AxialShd axial =
                new org.ofdrw.core.pageDescription.color.color.CT_AxialShd();
        axial.setStartPoint(new org.ofdrw.core.basicType.ST_Pos(10, 20));
        axial.setEndPoint(new org.ofdrw.core.basicType.ST_Pos(30, 20));
        axial.setExtend(org.ofdrw.core.pageDescription.color.color.Extend._3);
        axial.addSegment(new org.ofdrw.core.pageDescription.color.color.Segment(fillColor("255", "0", "0")));
        axial.addSegment(new org.ofdrw.core.pageDescription.color.color.Segment(fillColor("0", "0", "255")));
        // 中间段未给位置
        axial.addSegment(new org.ofdrw.core.pageDescription.color.color.Segment(fillColor("0", "255", "0")));

        java.util.List<ContentCommand> commands = renderer.render(axial, context);
        assertEquals(1, commands.size());
        ShadingCommand command = (ShadingCommand) commands.get(0);
        assertEquals(ShadingCommand.ShadingType.AXIAL, command.getShadingType());
        assertEquals(3, command.getStops().size());
        // 位置缺省均分：0、0.5、1
        assertEquals(0.0, command.getStops().get(0).getFraction(), EPS);
        assertEquals(0.5, command.getStops().get(1).getFraction(), EPS);
        assertEquals(1.0, command.getStops().get(2).getFraction(), EPS);
        // 坐标：对象空间 (10,20) → 页面 (10mm, 100-20mm)
        double[] expectedStart = CoordSystem.pointToPage(10, 20, 100);
        assertEquals(expectedStart[0], command.getStartX(), 1e-6);
        assertEquals(expectedStart[1], command.getStartY(), 1e-6);
        // Extend 3 → 两侧延长
        assertEquals(ShadingCommand.Extend.PAD, command.getExtend());
    }
}
