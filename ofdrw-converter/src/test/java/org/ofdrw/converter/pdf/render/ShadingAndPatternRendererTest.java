package org.ofdrw.converter.pdf.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.PdfPageContent;
import org.ofdrw.converter.pdf.model.PatternCommand;
import org.ofdrw.converter.pdf.model.ShadingCommand;
import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.graph.pathObj.FillColor;
import org.ofdrw.core.pageDescription.color.color.CT_AxialShd;
import org.ofdrw.core.pageDescription.color.color.CT_RadialShd;
import org.ofdrw.core.pageDescription.color.color.Extend;
import org.ofdrw.core.pageDescription.color.color.MapType;
import org.ofdrw.core.pageDescription.color.color.Segment;
import org.ofdrw.core.pageDescription.color.pattern.CT_Pattern;
import org.ofdrw.core.pageDescription.color.pattern.CellContent;
import org.ofdrw.core.pageDescription.color.pattern.ReflectMethod;
import org.ofdrw.core.pageDescription.color.pattern.RelativeTo;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 渐变与底纹渲染测试（P3，渲染器无关：只断言中间模型指令）
 * <p>
 * 覆盖设计第 2 节表格中的高风险项：
 * <ul>
 *     <li>轴向渐变：MapType（Direct/Repeat/Reflect）、Extend 0~3 → PDF Extend 数组、MapUnit、Segment 位置缺省均分；</li>
 *     <li>径向渐变：Eccentricity / Angle 的椭圆化映射；</li>
 *     <li>底纹：决策 17 的 Normal 近似、RelativeTo=Page、XStep 小于单元尺寸的告警与近似标记。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
class ShadingAndPatternRendererTest {

    /**
     * 断言容限
     */
    private static final double EPS = 1e-6;

    /**
     * 页面尺寸，单位 mm
     */
    private static final double PAGE_MM = 100;

    /**
     * 创建渲染上下文
     *
     * @return 渲染上下文
     */
    private static RenderContext context() {
        PdfPageContent content = new PdfPageContent(PAGE_MM * CoordSystem.MM_TO_PT, PAGE_MM * CoordSystem.MM_TO_PT);
        return RenderContexts.create(PAGE_MM, PAGE_MM, content, null);
    }

    /**
     * 构造颜色段
     *
     * @param position 位置，{@code null} 表示缺省
     * @param rgb      通道值
     * @return 颜色段
     */
    private static Segment segment(Double position, String... rgb) {
        FillColor color = new FillColor();
        color.setValue(new org.ofdrw.core.basicType.ST_Array((java.io.Serializable[]) rgb));
        return position == null ? new Segment(color) : new Segment(position, color);
    }

    @Test
    @DisplayName("轴向渐变：坐标经 CoordSystem 换算，停止点位置缺省均分")
    void axialGeometry() {
        CT_AxialShd axial = new CT_AxialShd();
        axial.setStartPoint(new ST_Pos(10, 20));
        axial.setEndPoint(new ST_Pos(40, 20));
        axial.addSegment(segment(null, "255", "0", "0"));
        axial.addSegment(segment(null, "0", "0", "255"));
        ShadingCommand command = new ShadingRenderer(new ColorResolver(null)).axial(axial, context());
        assertNotNull(command);
        assertEquals(ShadingCommand.ShadingType.AXIAL, command.getShadingType());
        double[] start = CoordSystem.pointToPage(10, 20, PAGE_MM);
        double[] end = CoordSystem.pointToPage(40, 20, PAGE_MM);
        assertEquals(start[0], command.getStartX(), 1e-6);
        assertEquals(start[1], command.getStartY(), 1e-6);
        assertEquals(end[0], command.getEndX(), 1e-6);
        assertEquals(end[1], command.getEndY(), 1e-6);
        assertEquals(0.0, command.getStops().get(0).getFraction(), EPS);
        assertEquals(1.0, command.getStops().get(1).getFraction(), EPS);
    }

    @Test
    @DisplayName("轴向渐变：Segment Position 显式值保留并保证单调")
    void axialPositions() {
        CT_AxialShd axial = new CT_AxialShd();
        axial.setStartPoint(new ST_Pos(0, 0));
        axial.setEndPoint(new ST_Pos(10, 0));
        axial.addSegment(segment(0.0, "255", "0", "0"));
        axial.addSegment(segment(0.3, "0", "255", "0"));
        // 位置回退（非法）→ 保证单调不减
        axial.addSegment(segment(0.2, "0", "0", "255"));
        ShadingCommand command = new ShadingRenderer(new ColorResolver(null)).axial(axial, context());
        assertEquals(3, command.getStops().size());
        assertEquals(0.0, command.getStops().get(0).getFraction(), EPS);
        assertEquals(0.3, command.getStops().get(1).getFraction(), EPS);
        assertEquals(0.3, command.getStops().get(2).getFraction(), EPS);
    }

    @Test
    @DisplayName("Extend 0~3 → PDF Extend 数组：[起始侧, 结束侧]")
    void extendArray() {
        assertArrayEquals(new boolean[]{false, false}, ShadingRenderer.pdfExtendArray(Extend._0));
        assertArrayEquals(new boolean[]{true, false}, ShadingRenderer.pdfExtendArray(Extend._1));
        assertArrayEquals(new boolean[]{false, true}, ShadingRenderer.pdfExtendArray(Extend._2));
        assertArrayEquals(new boolean[]{true, true}, ShadingRenderer.pdfExtendArray(Extend._3));
        // 缺省（null）等价于 Extend 0
        assertArrayEquals(new boolean[]{false, false}, ShadingRenderer.pdfExtendArray(null));
    }

    /**
     * 断言布尔数组
     *
     * @param expected 期望值
     * @param actual   实际值
     */
    private static void assertArrayEquals(boolean[] expected, boolean[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "下标 " + i);
        }
    }

    @Test
    @DisplayName("MapType：Direct/Repeat/Reflect 均可用，非 Direct 输出近似告警")
    void mapType() {
        // MapUnit 缺省返回 -1，表示“取渐变区间长度”
        assertEquals(-1, ShadingRenderer.mapUnitMm(null), 1e-9);
        assertEquals(CoordSystem.mmToPt(5), ShadingRenderer.mapUnitMm(5.0), 1e-9);

        CT_AxialShd axial = new CT_AxialShd();
        axial.setStartPoint(new ST_Pos(0, 0));
        axial.setEndPoint(new ST_Pos(10, 0));
        axial.setMapType(MapType.Reflect);
        axial.setMapUnit(5.0);
        axial.addSegment(segment(null, "255", "255", "255"));
        axial.addSegment(segment(null, "0", "0", "0"));
        // 不抛异常，仍产出一条渐变指令（近似 + warning）
        assertNotNull(new ShadingRenderer(new ColorResolver(null)).axial(axial, context()));
    }

    @Test
    @DisplayName("径向渐变：椭圆化映射（Eccentricity 压扁 + Angle 旋转）")
    void radialEllipse() {
        // 离心率 2 → 纵轴压扁一半
        org.ofdrw.converter.pdf.model.AffineMatrix squash = ShadingRenderer.ellipticalTransform(2.0, 0);
        assertEquals(1.0, squash.getA(), EPS);
        assertEquals(0.5, squash.getD(), EPS);
        // 角度 90°（OFD 顺时针）→ 页面上等价于逆时针 90°
        org.ofdrw.converter.pdf.model.AffineMatrix rotate = ShadingRenderer.ellipticalTransform(1.0, 90);
        assertEquals(0.0, rotate.getA(), 1e-9);
        assertEquals(-1.0, rotate.getB(), 1e-9);
        assertEquals(1.0, rotate.getC(), 1e-9);
        assertEquals(0.0, rotate.getD(), 1e-9);
        // 合成：先压扁再旋转
        org.ofdrw.converter.pdf.model.AffineMatrix combined = ShadingRenderer.ellipticalTransform(2.0, 90);
        // 矩阵乘法语义：(P·S)·R —— 先压扁（y 变为 0.5）再把结果逆时针旋转 90°：(0,0.5) → (0.5,0)
        double[] moved = combined.transform(0, 1);
        assertEquals(0.5, moved[0], 1e-9, "(0,1) 压扁为 (0,0.5)，逆时针旋转 90° 后为 (0.5,0)");
        assertEquals(0.0, moved[1], 1e-9);

        CT_RadialShd radial = new CT_RadialShd();
        radial.setStartPoint(new ST_Pos(50, 50));
        radial.setEndPoint(new ST_Pos(50, 50));
        radial.setStartRadius(0.0);
        radial.setEndRadius(30.0);
        radial.setEccentricity(2.0);
        radial.setAngle(30.0);
        radial.addSegment(segment(null, "255", "0", "0"));
        radial.addSegment(segment(null, "0", "0", "255"));
        ShadingCommand command = new ShadingRenderer(new ColorResolver(null)).radial(radial, context());
        assertNotNull(command);
        assertEquals(ShadingCommand.ShadingType.RADIAL, command.getShadingType());
        assertEquals(2.0, command.getEccentricity(), EPS);
        assertEquals(30.0, command.getAngleDeg(), EPS);
        // 同心：起止圆心相同
        assertEquals(command.getStartX(), command.getEndX(), EPS);
        assertEquals(command.getStartY(), command.getEndY(), EPS);
    }

    @Test
    @DisplayName("渐变：颜色段少于 2 个时不输出指令")
    void shadingNeedsTwoStops() {
        CT_AxialShd axial = new CT_AxialShd();
        axial.setStartPoint(new ST_Pos(0, 0));
        axial.setEndPoint(new ST_Pos(10, 0));
        axial.addSegment(segment(null, "255", "0", "0"));
        assertTrue(new ShadingRenderer(new ColorResolver(null)).render(axial, context()).isEmpty());
    }

    @Test
    @DisplayName("底纹：Normal 平铺，单元尺寸换算为 pt，近假标记为 false")
    void patternNormal() {
        CT_Pattern pattern = new CT_Pattern();
        pattern.setWidth(10.0);
        pattern.setHeight(5.0);
        pattern.setReflectMethod(ReflectMethod.Normal);
        pattern.setRelativeTo(RelativeTo.Object);
        pattern.setCellContent(new CellContent());
        PatternRenderer renderer = new PatternRenderer((content, ctx) ->
                Collections.singletonList(new org.ofdrw.converter.pdf.model.RectCommand(
                        new org.ofdrw.converter.pdf.model.Box(0, 0, 1, 1), PdfColor.BLACK)));
        List<ContentCommand> commands = renderer.render(pattern, context());
        assertEquals(1, commands.size());
        PatternCommand command = (PatternCommand) commands.get(0);
        assertFalse(command.isApproximate(), "Normal + Object 不属于近似实现");
        assertEquals(CoordSystem.mmToPt(10.0), command.getCell().getWidth(), 1e-6);
        assertEquals(CoordSystem.mmToPt(5.0), command.getCell().getHeight(), 1e-6);
    }

    @Test
    @DisplayName("底纹：ReflectMethod=Column / RelativeTo=Page 标记近似（决策 17）")
    void patternApproximate() {
        CT_Pattern pattern = new CT_Pattern();
        pattern.setWidth(8.0);
        pattern.setHeight(8.0);
        pattern.setReflectMethod(ReflectMethod.RowAndColumn);
        pattern.setRelativeTo(RelativeTo.Page);
        pattern.setCellContent(new CellContent());
        PatternRenderer renderer = new PatternRenderer((content, ctx) ->
                Collections.singletonList(new org.ofdrw.converter.pdf.model.RectCommand(
                        new org.ofdrw.converter.pdf.model.Box(0, 0, 1, 1), PdfColor.BLACK)));
        PatternCommand command = (PatternCommand) renderer.render(pattern, context()).get(0);
        assertTrue(command.isApproximate(), "非 Normal 翻转或页面相对定位必须标记为近似");
    }

    @Test
    @DisplayName("底纹：缺少单元内容或单元尺寸非法时跳过")
    void patternInvalid() {
        PatternRenderer renderer = new PatternRenderer((content, ctx) ->
                Collections.singletonList(new org.ofdrw.converter.pdf.model.RectCommand(
                        new org.ofdrw.converter.pdf.model.Box(0, 0, 1, 1), PdfColor.BLACK)));
        CT_Pattern noCell = new CT_Pattern();
        noCell.setWidth(5.0);
        noCell.setHeight(5.0);
        assertTrue(renderer.render(noCell, context()).isEmpty());

        CT_Pattern badSize = new CT_Pattern();
        badSize.setWidth(0.0);
        badSize.setHeight(5.0);
        badSize.setCellContent(new CellContent());
        assertTrue(renderer.render(badSize, context()).isEmpty());
    }
}
