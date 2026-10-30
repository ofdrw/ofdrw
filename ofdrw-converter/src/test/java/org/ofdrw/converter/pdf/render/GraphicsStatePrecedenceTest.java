package org.ofdrw.converter.pdf.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.LineCap;
import org.ofdrw.converter.pdf.model.LineJoin;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 绘制参数优先级回归测试（GB/T 33190 第 8.2、8.5 节就近原则）
 * <p>
 * 锁定优先级链：
 * <b>图元自身显式属性 &gt; 图元 DrawParam &gt; Relative 继承链（最大深度 32） &gt; 规范默认值</b>。
 * <p>
 * 重点覆盖易错组合：图元没写 Cap、DrawParam 没写 Cap、Relative 链里写了 Cap=Round
 * —— 结果必须是 Round，而不是 {@link LineCap#BUTT}。
 * 根因是 {@code CT_GraphicUnit#getCap()} 在属性缺失时返回规范默认值 Butt，
 * 若渲染侧直接使用它，会把“未定义”误判为“已显式声明”，从而打断继承链。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class GraphicsStatePrecedenceTest {

    /**
     * 断言容限
     */
    private static final double EPS = 1e-9;

    /**
     * 构造绘制参数（ID、Relative、Cap、Join、LineWidth 全部可选）
     *
     * @param id        ID
     * @param relative  父绘制参数 ID，可为空
     * @param cap       线端点样式，可为空
     * @param join      线连接样式，可为空
     * @param lineWidth 线宽，单位 mm，可为空
     * @return 绘制参数
     */
    private static CT_DrawParam drawParam(String id, String relative, LineCapType cap, LineJoinType join,
                                          Double lineWidth) {
        CT_DrawParam param = new CT_DrawParam();
        // 设置顺序会影响相对顺序：LineWidth/Join/Cap/Relative 固定按此顺序写入
        if (lineWidth != null) {
            param.setLineWidth(lineWidth);
        }
        if (join != null) {
            param.setJoin(join);
        }
        if (cap != null) {
            param.setCap(cap);
        }
        param.setID(ST_ID.getInstance(id));
        if (relative != null) {
            param.setRelative(new ST_RefID(Long.parseLong(relative)));
        }
        return param;
    }

    /**
     * 创建以给定绘制参数表为后端的解析器
     *
     * @param params 绘制参数表
     * @return 解析器
     */
    private static GraphicsStateResolver resolver(CT_DrawParam... params) {
        Map<String, CT_DrawParam> map = new HashMap<>();
        for (CT_DrawParam param : params) {
            map.put(param.getID().toString(), param);
        }
        return new GraphicsStateResolver(id -> map.get(id));
    }

    /**
     * 创建路径图元
     *
     * @param drawParamId 图元 DrawParam 引用，可为空
     * @param cap         图元显式 Cap，可为空（不书写）
     * @return 路径图元
     */
    private static CT_Path path(String drawParamId, LineCapType cap) {
        CT_Path path = new CT_Path();
        path.setBoundary(0, 0, 10, 10);
        path.setAbbreviatedData(new AbbreviatedData().S(0, 0).L(10, 0));
        if (drawParamId != null) {
            path.setDrawParam(new ST_RefID(Long.parseLong(drawParamId)));
        }
        if (cap != null) {
            path.setCap(cap);
        }
        return path;
    }

    @Test
    @DisplayName("绘制参数序列化：ID/Relative/Cap/Join/LineWidth 均可读回")
    void drawParamRoundTrip() {
        CT_DrawParam param = drawParam("30", "31", LineCapType.Round, LineJoinType.Bevel, 2.0);
        assertEquals("31", param.getRelative().toString());
        assertEquals(LineCapType.Round, param.getCap());
        assertEquals(LineJoinType.Bevel, param.getJoin());
    }

    @Test
    @DisplayName("图元未写 Cap、DrawParam 未写 Cap、Relative 链写了 Cap=Round → Round（不是 Butt）")
    void relativeChainCapSurvivesUndefinedUnitCap() {
        // 链：图元 → DrawParam 30（只写线宽）→ Relative 31（只写 Cap=Round）
        GraphicsStateResolver resolver = resolver(
                drawParam("30", "31", null, null, 2.0),
                drawParam("31", null, LineCapType.Round, null, null));
        PathRenderer renderer = new PathRenderer(resolver, null);

        EffectiveDrawParam effective = renderer.effectiveDrawParam(path("30", null));
        assertEquals(LineCap.ROUND, effective.getLineCap());
        // 线宽取自 DrawParam，未被 Relative 链打断
        assertEquals(2.0, effective.getLineWidthMm(), EPS);
    }

    @Test
    @DisplayName("图元显式 Cap=Square 覆盖 Relative 链中的 Cap=Round")
    void explicitUnitCapWins() {
        GraphicsStateResolver resolver = resolver(
                drawParam("30", "31", null, null, 2.0),
                drawParam("31", null, LineCapType.Round, null, null));
        PathRenderer renderer = new PathRenderer(resolver, null);

        assertEquals(LineCap.SQUARE, renderer.effectiveDrawParam(path("30", LineCapType.Square)).getLineCap());
    }

    @Test
    @DisplayName("图元显式写成规范默认值 Butt 仍识别为“已声明”，覆盖链上的 Round")
    void explicitDefaultValueStillCountsAsDeclared() {
        GraphicsStateResolver resolver = resolver(
                drawParam("30", "31", null, null, 2.0),
                drawParam("31", null, LineCapType.Round, LineJoinType.Round, null));
        PathRenderer renderer = new PathRenderer(resolver, null);

        CT_Path explicitButt = path("30", LineCapType.Butt);
        EffectiveDrawParam effective = renderer.effectiveDrawParam(explicitButt);
        assertEquals(LineCap.BUTT, effective.getLineCap());
        // Join 未在别处声明 → 沿链取 Round
        assertEquals(LineJoin.ROUND, effective.getLineJoin());
    }

    @Test
    @DisplayName("全链未声明 Cap/Join → 规范默认值 Butt/Miter")
    void specDefaults() {
        GraphicsStateResolver resolver = resolver(drawParam("30", null, null, null, null));
        EffectiveDrawParam effective = new PathRenderer(resolver, null).effectiveDrawParam(path("30", null));
        assertEquals(LineCap.BUTT, effective.getLineCap());
        assertEquals(LineJoin.MITER, effective.getLineJoin());
        assertEquals(EffectiveDrawParam.DEFAULT_LINE_WIDTH_MM, effective.getLineWidthMm(), EPS);
    }

    @Test
    @DisplayName("Relative 链就近覆盖：子级 {Cap=Round} 覆盖父级 {Cap=Square, Join=Bevel}")
    void childOverridesRelativeParent() {
        GraphicsStateResolver resolver = resolver(
                drawParam("31", "32", LineCapType.Round, null, null),
                drawParam("32", null, LineCapType.Square, LineJoinType.Bevel, 3.0));
        PathRenderer renderer = new PathRenderer(resolver, null);

        EffectiveDrawParam effective = renderer.effectiveDrawParam(path("31", null));
        assertEquals(LineCap.ROUND, effective.getLineCap());
        assertEquals(LineJoin.BEVEL, effective.getLineJoin());
        assertEquals(3.0, effective.getLineWidthMm(), EPS);
        // 指令侧同样落到 Round
        assertEquals(LineCap.ROUND, GraphicsStateResolver.toStrokeStyle(effective).getLineCap());
        assertEquals(CoordSystem.mmToPt(3.0), GraphicsStateResolver.toStrokeStyle(effective).getLineWidthPt(), 1e-9);
    }

    @Test
    @DisplayName("Relative 链深度超过 32 时截断：只取前 32 层，不抛异常")
    void chainDepthLimit() {
        Map<String, CT_DrawParam> map = new HashMap<>();
        // 1 → 2 → … → 40；第 33 层起不可达（深度上限 32）
        for (int i = 1; i <= 40; i++) {
            CT_DrawParam param = drawParam(String.valueOf(i), i < 40 ? String.valueOf(i + 1) : null,
                    null, null, (double) i);
            map.put(param.getID().toString(), param);
        }
        GraphicsStateResolver resolver = new GraphicsStateResolver(map::get);
        EffectiveDrawParam effective = new PathRenderer(resolver, null).effectiveDrawParam(path("1", null));
        assertEquals(1.0, effective.getLineWidthMm(), EPS);
    }
}
