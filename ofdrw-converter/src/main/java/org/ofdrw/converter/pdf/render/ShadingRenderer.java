package org.ofdrw.converter.pdf.render;

import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.ClosePath;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.FillRule;
import org.ofdrw.converter.pdf.model.LineTo;
import org.ofdrw.converter.pdf.model.MoveTo;
import org.ofdrw.converter.pdf.model.PathSegment;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.RestoreState;
import org.ofdrw.converter.pdf.model.SaveState;
import org.ofdrw.converter.pdf.model.SetAlpha;
import org.ofdrw.converter.pdf.model.ShadingCommand;
import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.pageDescription.color.color.CT_AxialShd;
import org.ofdrw.core.pageDescription.color.color.CT_RadialShd;
import org.ofdrw.core.pageDescription.color.color.Extend;
import org.ofdrw.core.pageDescription.color.color.MapType;
import org.ofdrw.core.pageDescription.color.color.Segment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 渐变渲染器（P3）：OFD AxialShd / RadialShd → 中间模型 {@link ShadingCommand}
 * <p>
 * <b>坐标</b>：OFD 渐变几何（起点/终点/半径）位于图元<b>对象空间</b>（mm，左上角原点、Y 轴向下），
 * 因此按设计 3.2 节固定顺序换算：先平移图元外接矩形 → 再叠加图元与父容器 CTM → 最后 mm→pt 与 Y 翻转
 * （全部经 {@link CoordSystem#objectToPage(org.ofdrw.converter.pdf.model.Box, AffineMatrix, double, double, double)}）。
 * <p>
 * <b>Extend（表 29）与 PDF 的对应</b>：OFD 的 Extend 是<b>单一位标志</b>，控制渐变两个延长方向是否继续绘制；
 * PDF 的 axial/radial shading 用 {@code /Extend [起始侧, 结束侧]} 两端独立的布尔值表达。OFD 的“起始点方向”
 * 指结束点至起始点的延长线（渐变开始之前的一侧），“结束点方向”指起始点至结束点的延长线（渐变结束之后的一侧），因此：
 * <pre>
 *   OFD Extend 0（不延长） → PDF [false, false]
 *   OFD Extend 1（起始侧） → PDF [true,  false]
 *   OFD Extend 2（结束侧） → PDF [false, true ]
 *   OFD Extend 3（两侧）   → PDF [true,  true ]
 * </pre>
 * 见 {@link #pdfExtendArray(Extend)}；延长区使用边界颜色平铺，对应 {@link ShadingCommand.Extend#PAD}。
 * <p>
 * <b>MapType</b>：{@link MapType#Direct}（缺省）颜色只映射一次，延长区按 Extend 处理；
 * {@link MapType#Repeat} / {@link MapType#Reflect} 需要循环/镜像映射，PDF 渐变无法表达，
 * 按决策 7/8 以 Direct + Extend 近似并输出 <b>warning</b>（{@link #mapTypeNotice}）。
 * <p>
 * <b>MapUnit</b>：颜色映射重复的区间长度（mm），仅对 Repeat/Reflect 有意义；PDF 侧无对应字段，
 * 同样在警告中说明（{@link #mapUnitMm} 供需要自行分片的调用方使用）。
 * <p>
 * <b>Segment Position</b>：范围 0~1；缺失时按“首段 0、末段 1、其余均分”补全（表 29 备注），
 * 并保证位置单调不减（PDF 对停止点顺序有要求）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ShadingRenderer implements ContentRenderer {

    private static final Logger logger = LoggerFactory.getLogger(ShadingRenderer.class);

    /**
     * 颜色解析器
     */
    private final ColorResolver colorResolver;

    /**
     * 已告警原因（同一原因只告警一次）
     */
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    /**
     * 构造渐变渲染器
     *
     * @param colorResolver 颜色解析器，不能为空
     */
    public ShadingRenderer(ColorResolver colorResolver) {
        if (colorResolver == null) {
            throw new IllegalArgumentException("颜色解析器不能为空");
        }
        this.colorResolver = colorResolver;
    }

    @Override
    public String getName() {
        return "ShadingRenderer";
    }

    /**
     * 渲染渐变
     * <p>
     * 输入为 OFD 的轴向或径向渐变；输出单条 {@link ShadingCommand}，
     * 其填充区域为占位区域，由调用方（路径渲染器）替换为图元的真实填充路径。
     *
     * @param input   渐变对象
     * @param context 渲染上下文
     * @return 指令序列；类型不支持或数据非法时返回空列表
     */
    @Override
    public List<ContentCommand> render(Object input, RenderContext context) {
        if (context == null) {
            return new ArrayList<>();
        }
        try {
            ShadingCommand command = null;
            if (input instanceof CT_AxialShd) {
                command = axial((CT_AxialShd) input, context);
            } else if (input instanceof CT_RadialShd) {
                command = radial((CT_RadialShd) input, context);
            }
            return command == null ? new ArrayList<>() : new ArrayList<>(Collections.singletonList(command));
        } catch (RuntimeException e) {
            warnOnce("shading-error", "渐变渲染失败，已跳过：" + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 轴向渐变
     *
     * @param axial   OFD 轴向渐变
     * @param context 渲染上下文
     * @return 渐变指令；数据非法时返回 {@code null}
     */
    public ShadingCommand axial(CT_AxialShd axial, RenderContext context) {
        ST_Pos start = axial.getStartPoint();
        ST_Pos end = axial.getEndPoint();
        if (start == null || end == null) {
            warnOnce("axial-points", "轴向渐变缺少起点或终点，已跳过");
            return null;
        }
        List<ShadingCommand.ColorStop> stops = stops(axial.getSegments(), "轴向渐变");
        if (stops == null) {
            return null;
        }
        AffineMatrix toPage = effectMatrix(context);
        double[] p0 = toPage.transform(start.getX(), start.getY());
        double[] p1 = toPage.transform(end.getX(), end.getY());
        mapTypeNotice(axial.getMapType(), axial.getMapUnit(), "轴向渐变");
        return new ShadingCommand(ShadingCommand.ShadingType.AXIAL, stops, extend(axial.getExtend()), area(),
                FillRule.NON_ZERO, p0[0], p0[1], p1[0], p1[1], 1.0, 0.0);
    }

    /**
     * 径向渐变（含 Eccentricity / Angle）
     * <p>
     * <b>映射推导</b>：OFD 径向渐变是以起点为圆心的同心椭圆族，椭圆半轴为
     * {@code (r, r / Eccentricity)}，并绕起点旋转 {@code Angle} 度（顺时针）；起点与终点不同时，
     * 渐变中心由起点线性移动到终点。PDF 的 radial shading（Type 3）只能表达两个<b>圆</b>之间的锥形渐变。
     * <p>
     * 因此把“椭圆”拆成对填充区域的线性变换：先纵向压扁 {@code 1/Eccentricity}（椭圆 → 圆），
     * 再旋转 {@code -Angle}（OFD 顺时针 → 页面坐标取负角），见 {@link #ellipticalTransform(double, double)}。
     * 写入器对该变换下的圆形径向渐变光栅化后，在页面上呈现的就是原椭圆；
     * 圆心坐标由本方法在“页面坐标”空间给出，变换由写入器以同一矩阵应用。
     * <p>
     * 近似误差：需要写入器应用上述变换才能严格复现椭圆；若写入器只按圆形绘制，
     * 误差为椭圆长短轴之比 {@code Eccentricity}，因此这里输出 <b>warning</b> 说明前置条件。
     *
     * @param radial  OFD 径向渐变
     * @param context 渲染上下文
     * @return 渐变指令；数据非法时返回 {@code null}
     */
    public ShadingCommand radial(CT_RadialShd radial, RenderContext context) {
        ST_Pos start = radial.getStartPoint();
        ST_Pos end = radial.getEndPoint();
        if (start == null || end == null) {
            warnOnce("radial-points", "径向渐变缺少起点或终点，已跳过");
            return null;
        }
        List<ShadingCommand.ColorStop> stops = stops(radial.getSegments(), "径向渐变");
        if (stops == null) {
            return null;
        }
        double eccentricity = radial.getEccentricity() == null ? 1.0 : radial.getEccentricity();
        double angle = radial.getAngle() == null ? 0.0 : radial.getAngle();
        if (eccentricity != 1.0 || angle != 0.0) {
            warnOnce("radial-ellipse", "径向渐变为椭圆（Eccentricity=" + eccentricity + ", Angle=" + angle
                    + "）：写入器须按 ShadingRenderer.ellipticalTransform 对填充区域做同一线性变换，"
                    + "否则退化为圆形近似（长短轴偏差 " + eccentricity + " 倍）");
        }
        /*
         * 椭圆 → 圆：OFD 的径向等值线是椭圆（半轴 r 与 r/Eccentricity，绕起点旋转 Angle），
         * PDF 的 radial shading 只能是圆。这里把“椭圆化”留在中间模型里（eccentricity/angleDeg），
         * 写入器在绘制该指令时先用 {@link #ellipticalTransform} 对填充区域与着色做同一线性变换，
         * 圆形径向渐变即呈现为原椭圆；变换的推导与近似误差见 ellipticalTransform 的注释。
         */
        AffineMatrix toPage = effectMatrix(context);
        double[] p0 = toPage.transform(start.getX(), start.getY());
        double[] p1 = toPage.transform(end.getX(), end.getY());
        mapTypeNotice(radial.getMapType(), radial.getMapUnit(), "径向渐变");
        return new ShadingCommand(ShadingCommand.ShadingType.RADIAL, stops, extend(radial.getExtend()), area(),
                FillRule.NON_ZERO, p0[0], p0[1], p1[0], p1[1], eccentricity, angle);
    }

    /**
     * 径向渐变的椭圆化线性变换（写入器与测试共用）
     *
     * @param eccentricity 离心率，1 为正圆
     * @param angleDeg     旋转角度，单位度（顺时针）
     * @return 仅含线性部分的变换矩阵
     */
    public static AffineMatrix ellipticalTransform(double eccentricity, double angleDeg) {
        double e = eccentricity <= 0 ? 1.0 : eccentricity;
        AffineMatrix squash = AffineMatrix.scale(1.0, 1.0 / e);
        double rad = Math.toRadians(-angleDeg);
        AffineMatrix rotate = new AffineMatrix(Math.cos(rad), Math.sin(rad), -Math.sin(rad), Math.cos(rad), 0, 0);
        return squash.multiply(rotate);
    }

    /**
     * 构造颜色停止点：位置缺省时按均分补全（表 29）
     *
     * @param segments OFD 颜色段列表
     * @param usage    用途文案
     * @return 停止点列表；少于 2 段或颜色无法解析时返回 {@code null}
     */
    List<ShadingCommand.ColorStop> stops(List<Segment> segments, String usage) {
        if (segments == null || segments.size() < 2) {
            warnOnce("stops:" + usage, usage + " 的颜色段少于 2 个，已跳过");
            return null;
        }
        int size = segments.size();
        List<ShadingCommand.ColorStop> stops = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Segment segment = segments.get(i);
            /* 颜色可能是“基本颜色”（Value 属性 + 不透明度）或“颜色族”（Pattern/渐变）：
               CT_Color#getColor() 只解析 <Color> 子元素，无法覆盖这两种形态，统一经
               {@link ColorResolver#resolveSegmentColor} 读取段内颜色元素。 */
            PdfColor resolved = colorResolver.resolveSegmentColor(segment, usage + "颜色段");
            if (resolved == null) {
                warnOnce("stop-color:" + usage, usage + " 的颜色段颜色无法解析，已跳过该渐变");
                return null;
            }
            Double position = segment.getPosition();
            // 位置缺省：首段 0、末段 1、其余均分
            double fraction = position == null ? (double) i / (size - 1) : position;
            stops.add(new ShadingCommand.ColorStop(fraction, resolved));
        }
        return ensureMonotonic(stops);
    }

    /**
     * 保证停止点位置单调不减（PDF 要求）
     *
     * @param stops 停止点
     * @return 规范化后的新列表
     */
    private static List<ShadingCommand.ColorStop> ensureMonotonic(List<ShadingCommand.ColorStop> stops) {
        List<ShadingCommand.ColorStop> result = new ArrayList<>(stops.size());
        double last = 0;
        for (ShadingCommand.ColorStop stop : stops) {
            double fraction = Math.max(last, stop.getFraction());
            result.add(new ShadingCommand.ColorStop(fraction, stop.getColor()));
            last = fraction;
        }
        return result;
    }

    /**
     * OFD Extend（位标志）→ 中间模型扩展方式
     * <p>
     * 0 表示两侧都不延长（{@link ShadingCommand.Extend#NONE}），1/2/3 至少一侧延长，
     * 延长区以边界颜色填充（{@link ShadingCommand.Extend#PAD}）；具体哪一侧由 {@link #pdfExtendArray} 给出。
     *
     * @param extend OFD 扩展方式，可为空
     * @return 扩展方式
     */
    public static ShadingCommand.Extend extend(Extend extend) {
        if (extend == null || extend == Extend._0) {
            return ShadingCommand.Extend.NONE;
        }
        return ShadingCommand.Extend.PAD;
    }

    /**
     * OFD Extend → PDF Extend 布尔数组
     *
     * @param extend OFD 扩展方式，可为空
     * @return 长度 2 的数组：{@code [0]} 起始侧、{@code [1]} 结束侧
     */
    public static boolean[] pdfExtendArray(Extend extend) {
        if (extend == null) {
            return new boolean[]{false, false};
        }
        switch (extend) {
            case _1:
                return new boolean[]{true, false};
            case _2:
                return new boolean[]{false, true};
            case _3:
                return new boolean[]{true, true};
            case _0:
            default:
                return new boolean[]{false, false};
        }
    }

    /**
     * MapType 非 Direct 时输出“近似”提示（决策 7/8：降级不静默）
     *
     * @param mapType OFD 映射方式，可为空
     * @param mapUnit MapUnit，单位 mm，可为空
     * @param usage   用途文案
     */
    private void mapTypeNotice(MapType mapType, Double mapUnit, String usage) {
        if (mapType == null || mapType == MapType.Direct) {
            return;
        }
        warnOnce("maptype:" + usage, usage + " 的 MapType=" + mapType + "（MapUnit=" + mapUnit
                + "mm）：PDF 渐变仅支持单次颜色映射，已按 Direct + Extend 近似绘制，"
                + "循环/镜像段缺失（决策 7：降级不阻断）");
    }

    /**
     * MapUnit 换算为 pt
     *
     * @param mapUnit OFD MapUnit，单位 mm；{@code null} 表示缺省
     * @return 区间长度，单位 pt；缺省时返回 -1 表示“取渐变区间长度”
     */
    public static double mapUnitMm(Double mapUnit) {
        return mapUnit == null || mapUnit <= 0 ? -1 : CoordSystem.mmToPt(mapUnit);
    }

    /**
     * 图元对象空间 → PDF 页面坐标矩阵（含页面框偏移，决策 26）
     *
     * @param context 渲染上下文
     * @return 变换矩阵
     */
    private static AffineMatrix effectMatrix(RenderContext context) {
        return CoordSystem.objectToPage(null, context.getCurrentCtm(),
                context.getPageX0Mm(), context.getPageY0Mm(), context.getPageHeightMm());
    }

    /**
     * 占位填充区域（1pt 方形）：由调用方（路径渲染器）替换为图元的真实填充路径
     *
     * @return 闭合的方形路径
     */
    private static List<PathSegment> area() {
        List<PathSegment> segments = new ArrayList<>(5);
        segments.add(new MoveTo(0, 0));
        segments.add(new LineTo(1, 0));
        segments.add(new LineTo(1, 1));
        segments.add(new LineTo(0, 1));
        segments.add(ClosePath.INSTANCE);
        return segments;
    }

    /**
     * 渐变填充的状态包装：{@code q + SetAlpha + ShadingCommand + Q}（设计约束 1：q/Q 成对）
     *
     * @param command 渐变指令，不能为空
     * @param alpha   不透明度，0~1
     * @return 指令序列
     */
    public static List<ContentCommand> withState(ShadingCommand command, float alpha) {
        List<ContentCommand> commands = new ArrayList<>(4);
        commands.add(SaveState.INSTANCE);
        commands.add(SetAlpha.of(alpha));
        commands.add(command);
        commands.add(RestoreState.INSTANCE);
        return commands;
    }

    /**
     * 输出一次告警（相同原因只告警一次）
     *
     * @param key     告警分类键
     * @param message 告警内容
     */
    private void warnOnce(String key, String message) {
        if (reported.add(key)) {
            logger.warn("[{}] {}", RenderContext.DIAGNOSTIC_TAG, message);
        }
    }
}
