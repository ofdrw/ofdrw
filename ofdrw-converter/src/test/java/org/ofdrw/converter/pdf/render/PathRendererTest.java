package org.ofdrw.converter.pdf.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.ClipCommand;
import org.ofdrw.converter.pdf.model.ClosePath;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.CurveTo;
import org.ofdrw.converter.pdf.model.FillRule;
import org.ofdrw.converter.pdf.model.LineTo;
import org.ofdrw.converter.pdf.model.MoveTo;
import org.ofdrw.converter.pdf.model.PathCommand;
import org.ofdrw.converter.pdf.model.PathSegment;
import org.ofdrw.converter.pdf.model.PdfPageContent;
import org.ofdrw.converter.pdf.model.RestoreState;
import org.ofdrw.converter.pdf.model.SaveState;
import org.ofdrw.converter.pdf.model.SetAlpha;
import org.ofdrw.converter.pdf.model.StrokeStyle;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.graph.pathObj.FillColor;
import org.ofdrw.core.graph.pathObj.Rule;
import org.ofdrw.core.graph.pathObj.StrokeColor;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.clips.CT_Clip;
import org.ofdrw.core.pageDescription.clips.Clips;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 路径渲染器测试（渲染器无关断言：只检查中间模型指令）
 * <p>
 * 锁定 P1 阶段的核心约定：
 * <ul>
 *     <li>逐点换算（决策 1）：外接矩形平移 + 页面框偏移 + Y 翻转，全部经 {@link CoordSystem}；</li>
 *     <li>{@code S/M/L/Q/B/A/C} 全部可用，{@code Q} 升阶为三次贝塞尔，{@code A} 拆分为贝塞尔段；</li>
 *     <li>填充规则 NonZero / Even-Odd；</li>
 *     <li>每个图元输出 {@code q … Q} 且成对（设计约束 1）；</li>
 *     <li>裁剪以 {@code q + ClipCommand + Q} 包裹内容。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
class PathRendererTest {

    /**
     * 断言容限，单位 pt
     */
    private static final double EPS = 1e-6;

    /**
     * 页面高 100mm，宽 100mm
     */
    private static final double PAGE_MM = 100;

    /**
     * 创建默认渲染上下文
     *
     * @return 渲染上下文
     */
    private static RenderContext context() {
        PdfPageContent content = new PdfPageContent(PAGE_MM * CoordSystem.MM_TO_PT, PAGE_MM * CoordSystem.MM_TO_PT);
        return RenderContexts.create(PAGE_MM, PAGE_MM, content, null);
    }

    /**
     * 创建只有路径、不做任何资源解析的渲染器
     *
     * @return 路径渲染器
     */
    private static PathRenderer renderer() {
        return new PathRenderer(new GraphicsStateResolver(id -> null), null);
    }

    @Test
    @DisplayName("直线与闭合：S/M/L/C 全为页面坐标，Y 轴向上")
    void moveLineClose() {
        CT_Path path = new CT_Path();
        path.setBoundary(10, 10, 20, 10);
        path.setAbbreviatedData(new AbbreviatedData().S(0, 0).L(20, 0).L(20, 10).C());
        path.setFill(true);
        path.setStroke(false);
        path.setFillColor(CT_Color.rgb(255, 0, 0));

        List<ContentCommand> commands = renderer().render(path, context());
        // q, StrokeStyle, SetAlpha, PathCommand, Q
        assertEquals(5, commands.size());
        assertEquals(SaveState.INSTANCE, commands.get(0));
        assertTrue(commands.get(1) instanceof StrokeStyle);
        assertTrue(commands.get(2) instanceof SetAlpha);
        PathCommand command = (PathCommand) commands.get(3);
        assertEquals(RestoreState.INSTANCE, commands.get(4));

        List<PathSegment> segments = command.getSegments();
        assertEquals(4, segments.size());
        // 对象空间 (0,0) + 外接矩形左上角 (10,10) → 页面 (10mm, 10mm) 翻转
        MoveTo move = (MoveTo) segments.get(0);
        assertEquals(CoordSystem.mmToPt(10), move.getX(), EPS);
        assertEquals(CoordSystem.yToPt(10, PAGE_MM), move.getY(), EPS);
        LineTo line = (LineTo) segments.get(1);
        assertEquals(CoordSystem.mmToPt(30), line.getX(), EPS);
        assertEquals(CoordSystem.yToPt(10, PAGE_MM), line.getY(), EPS);
        assertTrue(segments.get(3) instanceof ClosePath);
        assertTrue(command.isFill());
        assertEquals(FillRule.NON_ZERO, command.getFillRule());
        assertEquals(1f, command.getFillColor().getChannel(0), 1e-6);
        assertEquals(0f, command.getFillColor().getChannel(1), 1e-6);
    }

    @Test
    @DisplayName("二次贝塞尔 Q 升阶为三次贝塞尔")
    void quadraticRaisedToCubic() {
        CT_Path path = new CT_Path();
        path.setBoundary(0, 0, 10, 10);
        path.setAbbreviatedData(new AbbreviatedData().M(0, 0).Q(10, 0, 10, 10));
        path.setStroke(true);
        path.setStrokeColor(CT_Color.rgb(0, 0, 0));

        PathCommand command = (PathCommand) renderer().render(path, context()).get(3);
        CurveTo curve = (CurveTo) command.getSegments().get(1);
        // p0=(0,100)mm→pt, c=(10,0), p1=(10,10) 在对象空间；控制点按 2/3 插值
        double y0 = CoordSystem.yToPt(0, PAGE_MM);
        double y1 = CoordSystem.yToPt(10, PAGE_MM);
        double yc = CoordSystem.yToPt(0, PAGE_MM);
        assertEquals(CoordSystem.mmToPt(10), curve.getX3(), EPS);
        assertEquals(y1, curve.getY3(), EPS);
        assertEquals(0 + 2.0 / 3.0 * 10, CoordSystem.ptToMm(curve.getX1()), EPS);
        assertEquals(y0 + 2.0 / 3.0 * (yc - y0), curve.getY1(), EPS);
        assertEquals(10 + 2.0 / 3.0 * 0, CoordSystem.ptToMm(curve.getX2()), EPS);
        assertEquals(y1 + 2.0 / 3.0 * (yc - y1), curve.getY2(), EPS);
    }

    @Test
    @DisplayName("圆弧 A：拆分为贝塞尔段，终点为 A 操作符终点")
    void arcSegments() {
        CT_Path path = new CT_Path();
        path.setBoundary(0, 0, 10, 10);
        // 半径 5 的圆：起点 (5,0) → 终点 (-5,1)，large=true + 方向相反 → 270° 圆弧，拆成 3 段
        AbbreviatedData arcData = new AbbreviatedData();
        arcData.getRawOptVal().add(new org.ofdrw.core.graph.pathObj.OptVal("M", new double[]{5, 0}));
        arcData.getRawOptVal().add(new org.ofdrw.core.graph.pathObj.OptVal("A", new double[]{5, 5, 0, 1, 0, -5, 1}));
        path.setAbbreviatedData(arcData);
        path.setStroke(true);

        PathCommand command = (PathCommand) renderer().render(path, context()).get(3);
        List<PathSegment> segments = command.getSegments();
        // 起始 S/M 不计入路径段；270° 圆弧拆成 4 段贝塞尔（4 × C），路径未闭合因此额外补 C 关闭
        assertEquals(5, segments.size());
        int curveCount = 0;
        for (PathSegment segment : segments) {
            if (segment instanceof CurveTo) {
                curveCount++;
            }
        }
        assertTrue(curveCount >= 2);
        assertTrue(segments.size() == 5, "实际路径：" + segments);
        // 圆弧终点等于 A 操作符终点
        PathSegment last = segments.get(segments.size() - 1);
        assertEquals(CoordSystem.mmToPt(-5), last.getEndX(), 1e-6);
        assertEquals(CoordSystem.yToPt(1, PAGE_MM), last.getEndY(), 1e-6);
    }

    @Test
    @DisplayName("填充规则：Rule=Even-Odd")
    void evenOddRule() {
        CT_Path path = new CT_Path();
        path.setBoundary(0, 0, 10, 10);
        path.setRule(Rule.Even_Odd);
        path.setAbbreviatedData(new AbbreviatedData().S(0, 0).L(10, 0).L(10, 10).C());
        path.setFill(true);

        PathCommand command = (PathCommand) renderer().render(path, context()).get(3);
        assertEquals(FillRule.EVEN_ODD, command.getFillRule());
    }

    @Test
    @DisplayName("绘制参数就近原则：图元属性覆盖 DrawParam，DrawParam 覆盖 Relative 基础参数")
    void drawParamPrecedence() {
        CT_DrawParam base = new CT_DrawParam();
        base.setID(org.ofdrw.core.basicType.ST_ID.getInstance("20"));
        base.setLineWidth(2.0);

        CT_DrawParam child = new CT_DrawParam();
        child.setID(org.ofdrw.core.basicType.ST_ID.getInstance("21"));
        child.setRelative(new org.ofdrw.core.basicType.ST_RefID(20L));
        child.setLineWidth(1.0);
        child.setCap(org.ofdrw.core.pageDescription.drawParam.LineCapType.Round);

        GraphicsStateResolver resolver = new GraphicsStateResolver(id -> {
            if ("21".equals(id)) {
                return child;
            }
            return "20".equals(id) ? base : null;
        });

        // 子参数 1.0mm / Round 覆盖 Relative 基础参数的 2.0mm；未定义字段回落默认值
        EffectiveDrawParam fromDrawParam = resolver.resolve(new org.ofdrw.core.basicType.ST_RefID(21L));
        assertEquals(1.0, fromDrawParam.getLineWidthMm(), EPS);
        assertEquals(org.ofdrw.converter.pdf.model.LineCap.ROUND, fromDrawParam.getLineCap());
        assertEquals(EffectiveDrawParam.DEFAULT_MITER_LIMIT, fromDrawParam.getMiterLimit(), EPS);
        assertEquals(EffectiveDrawParam.DEFAULT_ALPHA, fromDrawParam.getAlpha());

        // 基础参数单独解析：只有 2.0mm，其余取默认值（未定义字段不被基础参数“默认 0.353”污染）
        EffectiveDrawParam fromBase = resolver.resolve(new org.ofdrw.core.basicType.ST_RefID(20L));
        assertEquals(2.0, fromBase.getLineWidthMm(), EPS);
        assertEquals(org.ofdrw.converter.pdf.model.LineCap.BUTT, fromBase.getLineCap());

        // 图元属性优先：LineWidth=3 覆盖 DrawParam 的 1.0，Alpha 取图元属性
        PathRenderer renderer = new PathRenderer(resolver, null);
        EffectiveDrawParam effective = renderer.effectiveDrawParam(
                pathWith(0, 0, 10, 10, 21, "S 0 0 L 10 0", 3.0, 128));
        assertEquals(3.0, effective.getLineWidthMm(), EPS);
        assertEquals(128, effective.getAlpha());
        // 图元未声明 Cap，但 DrawParam 21 自身写了 Cap=Round：按就近原则沿继承链取 Round，而非规范默认值 Butt
        // （CT_GraphicUnit#getCapExplicit 在属性缺失时返回 null，从而不会用默认值打断继承链）

        StrokeStyle style = GraphicsStateResolver.toStrokeStyle(effective);
        assertEquals(CoordSystem.mmToPt(3.0), style.getLineWidthPt(), EPS);
        assertEquals(org.ofdrw.converter.pdf.model.LineCap.ROUND, style.getLineCap());

        // 图元属性 Cap 覆盖 DrawParam
        CT_Path capped = pathWith(0, 0, 10, 10, 21, "S 0 0 L 10 0", 1.0, 255);
        capped.setCap(org.ofdrw.core.pageDescription.drawParam.LineCapType.Square);
        assertEquals(org.ofdrw.converter.pdf.model.LineCap.SQUARE,
                renderer.effectiveDrawParam(capped).getLineCap());
    }

    /**
     * 构造带绘制参数与图元属性的路径
     *
     * @param x         外接矩形左上角横坐标，单位 mm
     * @param y         外接矩形左上角纵坐标，单位 mm
     * @param width     外接矩形宽度，单位 mm
     * @param height    外接矩形高度，单位 mm
     * @param drawParam 绘制参数引用 ID，0 表示不引用
     * @param data      紧缩图形数据文本
     * @param lineWidth 图元线宽属性，单位 mm
     * @param alpha     图元透明度属性，0~255
     * @return 路径对象
     */
    private static CT_Path pathWith(double x, double y, double width, double height, long drawParam,
                                    String data, double lineWidth, int alpha) {
        CT_Path path = new CT_Path();
        path.setBoundary(x, y, width, height);
        path.setAbbreviatedData(new AbbreviatedData(AbbreviatedData.parse(data)));
        if (drawParam > 0) {
            path.setDrawParam(new org.ofdrw.core.basicType.ST_RefID(drawParam));
        }
        path.setLineWidth(lineWidth);
        path.setAlpha(alpha);
        return path;
    }

    @Test
    @DisplayName("Relative 继承链：深度上限 32，超限截断后仍返回可用参数")
    void relativeDepthLimit() {
        GraphicsStateResolver resolver = new GraphicsStateResolver(id -> {
            int value = Integer.parseInt(id);
            CT_DrawParam param = new CT_DrawParam();
            param.setID(org.ofdrw.core.basicType.ST_ID.getInstance(String.valueOf(value)));
            param.setRelative(new org.ofdrw.core.basicType.ST_RefID((long) (value + 1)));
            param.setLineWidth((double) value);
            return param;
        });
        // 链长无限：解析必须终止
        EffectiveDrawParam result = resolver.resolve(new org.ofdrw.core.basicType.ST_RefID(1L));
        assertNotNull(result);
        // 链头（ID=1）最贴近图元，其线宽应生效
        assertEquals(1.0, result.getLineWidthMm(), EPS);
    }

    @Test
    @DisplayName("Relative 环引用：解析终止且不抛异常")
    void relativeCycle() {
        GraphicsStateResolver resolver = new GraphicsStateResolver(id -> {
            CT_DrawParam param = new CT_DrawParam();
            param.setID(org.ofdrw.core.basicType.ST_ID.getInstance(id));
            param.setRelative(new org.ofdrw.core.basicType.ST_RefID("A".equals(id) ? 2L : 1L));
            param.setLineWidth("A".equals(id) ? 5.0 : 7.0);
            return param;
        });
        EffectiveDrawParam result = resolver.resolve(new org.ofdrw.core.basicType.ST_RefID(1L));
        assertEquals(7.0, result.getLineWidthMm(), EPS);
    }

    @Test
    @DisplayName("裁剪：q + ClipCommand + 内容 + Q，多个 Clip 为多条指令（交集）")
    void clipIntersection() {
        Clips clips = twoClips();
        CT_Path path = new CT_Path();
        path.setBoundary(0, 0, 10, 10);
        path.setAbbreviatedData(new AbbreviatedData().S(0, 0).L(10, 0));
        path.setClips(clips);

        List<ContentCommand> commands = renderer().render(path, context());
        assertEquals(SaveState.INSTANCE, commands.get(0));
        ClipCommand first = (ClipCommand) commands.get(1);
        ClipCommand second = (ClipCommand) commands.get(2);
        assertTrue(first.isUnionArea());
        assertTrue(second.isUnionArea());
        assertEquals(FillRule.EVEN_ODD, first.getFillRule());
        // 外框 q + 2 条裁剪 + 内层 q + 样式/alpha/路径 + Q + 外框 Q
        assertEquals(SaveState.INSTANCE, commands.get(3));
        assertTrue(commands.get(6) instanceof PathCommand);
        assertEquals(RestoreState.INSTANCE, commands.get(7));
        assertEquals(RestoreState.INSTANCE, commands.get(commands.size() - 1));
    }

    @Test
    @DisplayName("裁剪：同一 Clip 内两个 Area 合并为并集（一条指令）")
    void clipUnion() {
        CT_Path clipPath = new CT_Path();
        clipPath.setBoundary(0, 0, 10, 10);
        clipPath.setAbbreviatedData(new AbbreviatedData().S(0, 0).L(5, 0).L(5, 5).C());
        org.ofdrw.core.pageDescription.clips.Area area1 = new org.ofdrw.core.pageDescription.clips.Area();
        area1.add((org.dom4j.Element) clipPath);
        CT_Path clipPath2 = new CT_Path();
        clipPath2.setBoundary(0, 0, 10, 10);
        clipPath2.setAbbreviatedData(new AbbreviatedData().S(0, 0).L(5, 0).L(5, 5).C());
        org.ofdrw.core.pageDescription.clips.Area area2 = new org.ofdrw.core.pageDescription.clips.Area();
        area2.add((org.dom4j.Element) clipPath2);
        org.ofdrw.core.pageDescription.clips.CT_Clip clip = new org.ofdrw.core.pageDescription.clips.CT_Clip();
        clip.addArea(area1);
        clip.addArea(area2);

        List<ClipCommand> commands = new ClipApplier().apply(
                new org.ofdrw.core.pageDescription.clips.Clips(clip), context());
        assertEquals(1, commands.size());
        // 两个子路径：2 × (M + 2L + C)
        assertEquals(8, commands.get(0).getSegments().size());
        assertTrue(commands.get(0).isUnionArea());
    }

    @Test
    @DisplayName("不可见图元跳过，异常输入返回空列表")
    void invisibleAndInvalid() {
        CT_Path path = new CT_Path();
        path.setVisible(false);
        path.setAbbreviatedData(new AbbreviatedData().S(0, 0).L(1, 1));
        assertTrue(renderer().render(path, context()).isEmpty());
        assertTrue(renderer().render("not-a-path", context()).isEmpty());
    }

    @Test
    @DisplayName("空路径数据输出告警并跳过，不抛异常")
    void emptyPathData() {
        CT_Path path = new CT_Path();
        path.setBoundary(0, 0, 10, 10);
        path.setStroke(true);
        assertTrue(renderer().render(path, context()).isEmpty());
    }

    @Test
    @DisplayName("填充色缺失时使用默认黑色")
    void defaultFillColor() {
        CT_Path path = new CT_Path();
        path.setBoundary(0, 0, 10, 10);
        path.setAbbreviatedData(new AbbreviatedData().S(0, 0).L(10, 0).L(10, 10).C());
        path.setFill(true);
        path.setStroke(true);
        PathCommand command = (PathCommand) renderer().render(path, context()).get(3);
        assertEquals(org.ofdrw.converter.pdf.model.PdfColor.BLACK, command.getFillColor());
        assertEquals(org.ofdrw.converter.pdf.model.PdfColor.BLACK, command.getStrokeColor());
    }

    /**
     * 构造包含两个 Clip（交集）的裁剪对象
     * <p>
     * dom4j 节点只能挂在一个父节点下，因此每个 Area 都使用独立的路径对象。
     *
     * @return 裁剪目标（两个 Clip，各含一个矩形区域）
     */
    private static Clips twoClips() {
        Clips clips = new Clips(new CT_Clip().addArea(rectangleArea()));
        clips.addClip(new CT_Clip().addArea(rectangleArea()));
        return clips;
    }

    /**
     * 构造一个矩形裁剪区域
     *
     * @return 区域
     */
    private static org.ofdrw.core.pageDescription.clips.Area rectangleArea() {
        CT_Path clipPath = new CT_Path();
        clipPath.setBoundary(0, 0, 10, 10);
        clipPath.setAbbreviatedData(new AbbreviatedData().S(0, 0).L(10, 0).L(10, 10).C());
        org.ofdrw.core.pageDescription.clips.Area area = new org.ofdrw.core.pageDescription.clips.Area();
        // 直接挂载元素：setClipObj 依赖 dom4j 父节点迁移，在本测试的构造顺序下不可用
        area.add((org.dom4j.Element) clipPath);
        return area;
    }

    @Test
    @DisplayName("FillColor/StrokeColor 元素解析为颜色空间与通道值")
    void colorElements() {
        PathRenderer renderer = renderer();
        FillColor cmyk = new FillColor();
        cmyk.setValue(new ST_Array(0, 0, 0, 255));
        org.ofdrw.converter.pdf.model.PdfColor color = renderer.resolveColor(cmyk, "填充");
        assertNotNull(color);
        assertEquals(org.ofdrw.converter.pdf.model.PdfColor.ColorSpace.CMYK, color.getColorSpace());
        assertEquals(4, color.getComponentCount());
        assertEquals(1f, color.getChannel(3), 1e-6);

        StrokeColor gray = new StrokeColor();
        gray.setValue(new ST_Array(0.5));
        org.ofdrw.converter.pdf.model.PdfColor grayColor = renderer.resolveColor(gray, "描边");
        assertEquals(org.ofdrw.converter.pdf.model.PdfColor.ColorSpace.GRAY, grayColor.getColorSpace());
        assertEquals(0.5f, grayColor.getChannel(0), 1e-6);

        FillColor hex = new FillColor();
        hex.setValue(new ST_Array(new java.io.Serializable[]{"#FF", "#00", "#80"}));
        org.ofdrw.converter.pdf.model.PdfColor hexColor = renderer.resolveColor(hex, "填充");
        assertEquals(1f, hexColor.getChannel(0), 1e-6);
        assertEquals(0f, hexColor.getChannel(1), 1e-6);
    }
}
