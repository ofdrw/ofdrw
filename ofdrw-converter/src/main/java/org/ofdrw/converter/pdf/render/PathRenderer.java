package org.ofdrw.converter.pdf.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.ClosePath;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.FillRule;
import org.ofdrw.converter.pdf.model.PathCommand;
import org.ofdrw.converter.pdf.model.PathSegment;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.RestoreState;
import org.ofdrw.converter.pdf.model.SaveState;
import org.ofdrw.converter.pdf.model.SetAlpha;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.graph.pathObj.Rule;
import org.ofdrw.converter.pdf.model.ShadingCommand;
import org.ofdrw.core.pageDescription.color.color.CT_AxialShd;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.color.CT_RadialShd;
import org.ofdrw.core.pageDescription.color.pattern.CT_Pattern;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.color.colorSpace.OFDColorSpaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 路径渲染器：OFD {@code Path}（AbbreviatedData {@code S/M/L/Q/B/A/C}）→ 中间模型指令
 * <p>
 * 覆盖要求（设计 4.1 节、第 2 节表格）：
 * <ul>
 *     <li>圆弧 {@code A}：LargeArc / SweepDirection / 旋转角，由 {@link ArcConverter} 拆分为三次贝塞尔；</li>
 *     <li>二次贝塞尔 {@code Q} 升阶为三次贝塞尔；</li>
 *     <li>关闭子路径 {@code C} → {@link ClosePath}；</li>
 *     <li>填充规则 {@code Rule}：NonZero → {@link FillRule#NON_ZERO}，Even-Odd → {@link FillRule#EVEN_ODD}。</li>
 * </ul>
 * <p>
 * 输出约定（设计约束 1：不依赖内容流的隐式状态）：每个图元输出
 * {@code q, StrokeStyle, SetAlpha, PathCommand, Q}；有效绘制参数由 {@link GraphicsStateResolver}
 * 按“就近原则”一次性算出，坐标一律经 {@link CoordSystem} 逐点换算为 PDF 页面坐标（决策 1）。
 * <p>
 * 容错（设计约束 3、决策 8）：单图元异常只跳过该图元并输出 warning，不中断整页。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PathRenderer implements ContentRenderer {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(PathRenderer.class);

    /**
     * 颜色缺失时的默认色：黑色（OFD 8.3 默认填充色/勾边色均为黑色）
     */
    private static final PdfColor DEFAULT_COLOR = PdfColor.BLACK;

    /**
     * 绘制参数解析器
     */
    private final GraphicsStateResolver stateResolver;

    /**
     * 颜色空间查找函数（解析调色板颜色），可为空
     */
    private final ColorResolver.ColorSpaceLookup colorSpaceLookup;

    /**
     * 裁剪应用器
     */
    private final ClipApplier clipApplier;

    /**
     * 颜色解析器（P3：颜色空间、调色板、位宽、ICC 与颜色 Alpha）
     */
    private final ColorResolver colorResolver;

    /**
     * 渐变渲染器（P3），可为空（此时渐变填充按默认黑色处理并告警）
     */
    private final ShadingRenderer shadingRenderer;

    /**
     * 底纹渲染器（P3），可为空（此时底纹填充按默认黑色处理并告警）
     */
    private final PatternRenderer patternRenderer;

    /**
     * 紧缩图形数据转换器（按实例隔离告警去重）
     */
    private final AbbreviatedDataConverter pathConverter = new AbbreviatedDataConverter();

    /**
     * 已告警原因
     */
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    /**
     * 构造路径渲染器
     *
     * @param stateResolver    绘制参数解析器，不能为空
     * @param colorSpaceLookup 颜色空间查找函数，可为空（此时按通道数量推断颜色空间）
     */
    public PathRenderer(GraphicsStateResolver stateResolver, ColorResolver.ColorSpaceLookup colorSpaceLookup) {
        this(stateResolver, colorSpaceLookup, new ClipApplier());
    }

    /**
     * 构造路径渲染器
     *
     * @param stateResolver    绘制参数解析器，不能为空
     * @param colorSpaceLookup 颜色空间查找函数，可为空
     * @param clipApplier      裁剪应用器，不能为空
     */
    public PathRenderer(GraphicsStateResolver stateResolver, ColorResolver.ColorSpaceLookup colorSpaceLookup, ClipApplier clipApplier) {
        if (stateResolver == null || clipApplier == null) {
            throw new IllegalArgumentException("绘制参数解析器与裁剪应用器不能为空");
        }
        this.stateResolver = stateResolver;
        this.colorSpaceLookup = colorSpaceLookup;
        this.clipApplier = clipApplier;
        this.colorResolver = new ColorResolver(colorSpaceLookup == null ? null : colorSpaceLookup::getColorSpace);
        this.shadingRenderer = new ShadingRenderer(colorResolver);
        this.patternRenderer = new PatternRenderer(null);
    }

    @Override
    public String getName() {
        return "PathRenderer";
    }

    /**
     * 把 OFD 路径对象渲染为中间模型指令
     * <p>
     * 图层级基准绘制参数由调用方通过 {@link RenderContext#withCtm(AffineMatrix)} 之外的调度逻辑提供；
     * 本节（P1）按“图元 DrawParam（含 Relative 链）→ 图元属性 → 默认值”解析，图元属性优先。
     *
     * @param input   OFD 路径对象（{@link CT_Path} 及其子类，如 {@code PathObject}）
     * @param context 渲染上下文
     * @return 指令序列；输入类型不支持、不可见或路径为空时返回空列表
     */
    @Override
    public List<ContentCommand> render(Object input, RenderContext context) {
        if (!(input instanceof CT_Path) || context == null) {
            return Collections.emptyList();
        }
        CT_Path path = (CT_Path) input;
        try {
            if (Boolean.FALSE.equals(path.getVisible())) {
                return Collections.emptyList();
            }
            AffineMatrix toPage = objectToPage(path, context);
            List<PathSegment> segments = pathConverter.convert(path.getAbbreviatedDataEle(), toPage);
            if (segments.isEmpty()) {
                warnOnce("empty-path", "路径对象没有可用的紧缩图形数据，已跳过：" + path.getGraphicName());
                return Collections.emptyList();
            }
            PathCommand command = buildCommand(path, segments);
            if (command == null) {
                return Collections.emptyList();
            }
            List<ContentCommand> content = new ArrayList<>(8);
            content.add(SaveState.INSTANCE);
            EffectiveDrawParam params = effectiveDrawParam(path);
            content.add(GraphicsStateResolver.toStrokeStyle(params));
            /*
             * 不透明度 = 颜色 Alpha × 图元 Alpha（决策 2）。
             * 纯色填充/描边的颜色 Alpha 已由 ColorResolver 解析进 PdfColor.opacity，
             * 这里把它与图元 Alpha 合成后写入 SetAlpha，避免纯色的颜色 Alpha 被丢弃。
             */
            content.add(SetAlpha.of(compositeAlpha(path, params)));
            // 复杂填充色（渐变/底纹）由 P3 渲染器接管：先填充单色路径（描边或默认色），再叠加渐变/底纹
            List<ContentCommand> complexFill = complexFill(path, segments, params, context);
            content.add(command);
            content.addAll(complexFill);
            content.add(RestoreState.INSTANCE);
            return clipApplier.render(path.getClips(), context, content);
        } catch (RuntimeException e) {
            warnOnce("path-error", "路径图元渲染失败，已跳过：" + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 解析路径对象的有效绘制参数
     * <p>
     * 就近原则：图元 DrawParam（已展开 {@code Relative} 链）作为基准，图元同名属性覆盖，
     * 始终未定义的字段取 OFD 规范默认值。
     *
     * @param path 路径对象，不能为空
     * @return 有效绘制参数
     */
    public EffectiveDrawParam effectiveDrawParam(CT_Path path) {
        EffectiveDrawParam base = stateResolver.resolve(path.getDrawParam());
        // Cap/Join 使用“显式书写”访问器：getCap()/getJoin() 在属性缺失时返回规范默认值，
        // 会把默认值当成“图元已声明”，从而覆盖 DrawParam 与 Relative 链（GB/T 33190 8.2/8.5 就近原则）
        // Alpha 使用“仅取显式书写”的访问器：getAlpha() 在缺失时返回 255，
        // 会把“未定义”误判为“显式不透明”并覆盖 DrawParam/Relative 链上的透明度（决策 2 + 就近原则）
        return stateResolver.applyUnitAttributes(base, path.getLineWidth(), path.getCapExplicit(),
                path.getJoinExplicit(), path.getMiterLimit(), path.getDashPattern(), path.getAlphaExplicit());
    }

    /**
     * 计算路径对象的对象空间 → 页面坐标矩阵（含图元 CTM 与页面框偏移）
     * <p>
     * 对象空间以外接矩形左上角为原点（8.1.4），变换顺序固定为：平移外接矩形 → 叠加容器 CTM 与图元 CTM →
     * mm→pt 与 Y 翻转（设计 3.2 节）。
     *
     * @param path    路径对象，不能为空
     * @param context 渲染上下文，不能为空
     * @return 换算矩阵
     */
    public static AffineMatrix objectToPage(CT_Path path, RenderContext context) {
        AffineMatrix combined = elementCtm(path.getCTM()).multiply(context.getCurrentCtm());
        // 外接矩形平移、页面框偏移、mm→pt 与 Y 翻转统一由 CoordSystem 完成，避免各渲染器手写翻转
        return CoordSystem.objectToPage(boundaryOf(path), combined,
                context.getPageX0Mm(), context.getPageY0Mm(), context.getPageHeightMm());
    }

    /**
     * 取图元外接矩形
     *
     * @param path 路径图元
     * @return 外接矩形，单位 mm；缺失时返回 {@code null}（按原点处理）
     */
    private static org.ofdrw.converter.pdf.model.Box boundaryOf(CT_Path path) {
        if (path.getBoundary() == null) {
            return null;
        }
        return CoordSystem.boundary(
                path.getBoundary().getTopLeftX() == null ? 0 : path.getBoundary().getTopLeftX(),
                path.getBoundary().getTopLeftY() == null ? 0 : path.getBoundary().getTopLeftY(),
                path.getBoundary().getWidth() == null ? 0 : path.getBoundary().getWidth(),
                path.getBoundary().getHeight() == null ? 0 : path.getBoundary().getHeight());
    }

    /**
     * 解析图元 CTM 属性（行向量写法）
     * <p>
     * 只有 6 个操作数时才视为矩阵；其它长度（含属性缺失导致的空数组）按单位矩阵处理并输出提示，
     * 避免 OFD 中“空 CTM 属性”被误解为零矩阵而把图元缩成一点。
     *
     * @param ctm 图元 CTM，可为空
     * @return 仿射矩阵；为空或长度不为 6 时返回单位矩阵
     */
    public static AffineMatrix elementCtm(ST_Array ctm) {
        if (ctm == null) {
            return AffineMatrix.IDENTITY;
        }
        if (ctm.size() != 6) {
            logger.warn("[{}] 图元 CTM 操作数数量为 {}（应为 6），已按单位矩阵处理：{}",
                    RenderContext.DIAGNOSTIC_TAG, ctm.size(), ctm);
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
     * 根据路径对象的填充/描边属性与颜色生成 {@link PathCommand}
     *
     * @param path     路径对象，不能为空
     * @param segments 已换算为页面坐标的路径段，不能为空
     * @return 路径指令；既不填充也不描边时返回 {@code null}
     */
    private PathCommand buildCommand(CT_Path path, List<PathSegment> segments) {
        // OFD 表 41：Fill 缺省 false，Stroke 缺省 true（getStroke() 在缺省时返回 true）
        boolean fill = Boolean.TRUE.equals(path.getFill());
        boolean stroke = !Boolean.FALSE.equals(path.getStroke());
        if (!fill && !stroke) {
            warnOnce("invisible-path", "路径对象既未填充也未描边，已跳过：" + path.getGraphicName());
            return null;
        }
        PdfColor fillColor = fill ? defaultIfNull(resolveColor(path.getFillColor(), "填充")) : null;
        PdfColor strokeColor = stroke ? defaultIfNull(resolveColor(path.getStrokeColor(), "描边")) : null;
        return new PathCommand(segments, fill, stroke, fillRule(path.getRule()), fillColor, strokeColor);
    }

    /**
     * 复杂填充（渐变 / 底纹）指令
     * <p>
     * OFD 的填充颜色可以是纯色、轴向/径向渐变或底纹（表 26）。P3 起：
     * <ul>
     *     <li>轴向/径向渐变：{@link ShadingRenderer} 生成 {@link org.ofdrw.converter.pdf.model.ShadingCommand}，
     *     填充区域替换为当前图元的路径；椭圆径向渐变按 {@link ShadingRenderer#ellipticalTransform} 说明处理；</li>
     *     <li>底纹：{@link PatternRenderer} 生成 {@link org.ofdrw.converter.pdf.model.PatternCommand}（决策 17 近似）；</li>
     *     <li>其它复杂色（高洛德渐变等）按默认黑色处理并告警。</li>
     * </ul>
     * 不透明度取“颜色 Alpha × 图元 Alpha”（决策 2），通过 {@code SetAlpha} 写入。
     *
     * @param path     路径对象，不能为空
     * @param segments 已换算为页面坐标的路径段，不能为空
     * @param params   有效绘制参数（提供图元 Alpha）
     * @param context  渲染上下文
     * @return 附加指令序列；没有复杂填充时返回空列表
     */
    private List<ContentCommand> complexFill(CT_Path path, List<PathSegment> segments,
                                             EffectiveDrawParam params, RenderContext context) {
        CT_Color fillColor = path.getFillColor();
        if (fillColor == null || fillColor.getColorByType() == null) {
            return Collections.emptyList();
        }
        float alpha = ColorResolver.opacity(params.getAlpha());
        if (alpha <= 0f) {
            return Collections.emptyList();
        }
        Object cluster = fillColor.getColorByType();
        if (cluster instanceof CT_AxialShd || cluster instanceof CT_RadialShd) {
            ShadingCommand shading = cluster instanceof CT_AxialShd
                    ? shadingRenderer.axial((CT_AxialShd) cluster, context)
                    : shadingRenderer.radial((CT_RadialShd) cluster, context);
            if (shading == null) {
                warnOnce("shading-failed", "渐变填充解析失败，已按无填充处理：" + path.getGraphicName());
                return Collections.emptyList();
            }
            return ShadingRenderer.withState(
                    new ShadingCommand(shading.getShadingType(), shading.getStops(), shading.getExtend(),
                            segments, fillRule(path.getRule()),
                            shading.getStartX(), shading.getStartY(), shading.getEndX(), shading.getEndY(),
                            shading.getEccentricity(), shading.getAngleDeg()), alpha);
        }
        if (cluster instanceof CT_Pattern) {
            List<ContentCommand> patternCommands = patternRenderer.render(cluster, context);
            if (patternCommands.isEmpty()) {
                warnOnce("pattern-failed", "底纹填充解析失败，已按无填充处理：" + path.getGraphicName());
                return Collections.emptyList();
            }
            return patternCommands;
        }
        warnOnce("fill-unsupported", "不支持的填充色对象（" + cluster.getClass().getSimpleName()
                + "），已按默认黑色处理：" + path.getGraphicName());
        return Collections.emptyList();
    }

    /**
     * 颜色缺失时补默认黑色
     *
     * @param color 颜色，可为空
     * @return 非空颜色
     */
    private static PdfColor defaultIfNull(PdfColor color) {
        return color == null ? DEFAULT_COLOR : color;
    }

    /**
     * 解析 OFD 颜色为中间模型颜色
     * <p>
     * 颜色空间取自颜色的 {@code ColorSpace} 引用；无引用时按通道数量推断（1 → GRAY、3 → RGB、4 → CMYK）。
     * 通道值支持 {@code #RRGGBB} 十六进制与十进制：绝对值 ≤ 1 视为 0~1 归一化值，否则按 0~255 归一化。
     * 颜色自身 Alpha（表 26）写入 {@link PdfColor#getOpacity()}。
     *
     * @param color 颜色元素，可为空
     * @param usage 颜色用途，仅用于告警文案
     * @return 中间模型颜色；为空或颜色为渐变/底纹时返回 {@code null}（由 P3 阶段处理）
     */
    public PdfColor resolveColor(CT_Color color, String usage) {
        if (color == null) {
            return null;
        }
        Object cluster = color.getColorByType();
        if (cluster != null) {
            // 渐变/底纹由 ShadingRenderer / PatternRenderer 处理（P3）：纯色解析在此返回 null，
            // 由 render() 的复杂颜色分支接管
            return null;
        }
        return colorResolver.resolve(color, usage);
    }

    /**
     * 计算图元的最终不透明度：颜色 Alpha × 图元 Alpha（决策 2）
     * <p>
     * 填充与描边的颜色 Alpha 可能不同，取二者与图元 Alpha 合成后的最小值，保证不会比任一颜色更不透明；
     * 颜色缺失（渐变/底纹或无颜色）时只使用图元 Alpha。
     *
     * @param path   路径对象
     * @param params 有效绘制参数（提供图元 Alpha）
     * @return 不透明度，0~1
     */
    private float compositeAlpha(CT_Path path, EffectiveDrawParam params) {
        float unit = ColorResolver.opacity(params.getAlpha());
        float fill = path.getFillColor() == null ? 1f : ColorResolver.colorOpacity(resolveColor(path.getFillColor(), "填充"));
        float stroke = path.getStrokeColor() == null ? 1f : ColorResolver.colorOpacity(resolveColor(path.getStrokeColor(), "描边"));
        return unit * Math.min(fill, stroke);
    }

    /**
     * 解析颜色空间类型（引用 → 颜色空间定义；无引用返回 {@code null} 表示按通道推断）
     *
     * @param color 颜色元素
     * @return 颜色空间类型，可为空
     */
    private OFDColorSpaceType colorSpaceType(CT_Color color) {
        ST_RefID ref = color.getColorSpace();
        if (ref == null || colorSpaceLookup == null) {
            return null;
        }
        try {
            String id = GraphicsStateResolver.idOf(ref);
            CT_ColorSpace space = id == null ? null : colorSpaceLookup.getColorSpace(id);
            if (space == null) {
                return null;
            }
            if (space.getType() == null) {
                warnOnce("colorspace-type", "颜色空间缺少类型声明，按通道数量推断");
            }
            return space.getType();
        } catch (RuntimeException e) {
            warnOnce("colorspace-error", "颜色空间解析失败：" + e.getMessage());
            return null;
        }
    }

    /**
     * 读取单个通道并归一化到 0~1
     *
     * @param text 通道原始文本，可为空
     * @return 归一化通道值，0~1
     */
    private float channel(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0f;
        }
        String value = text.trim();
        try {
            double raw;
            if (value.startsWith("#")) {
                raw = Integer.parseInt(value.substring(1), 16);
                return (float) Math.max(0, Math.min(1, raw / 255.0));
            }
            raw = Double.parseDouble(value);
            if (Math.abs(raw) <= 1) {
                return (float) Math.max(0, Math.min(1, raw));
            }
            return (float) Math.max(0, Math.min(1, raw / 255.0));
        } catch (NumberFormatException e) {
            warnOnce("color-channel", "颜色通道非法（" + value + "），已按 0 处理");
            return 0f;
        }
    }

    /**
     * OFD 填充规则 → 中间模型填充规则
     *
     * @param rule OFD 规则，可为空
     * @return 填充规则；为空或未知时按 NonZero
     */
    public static FillRule fillRule(Rule rule) {
        return rule == Rule.Even_Odd ? FillRule.EVEN_ODD : FillRule.NON_ZERO;
    }

    /**
     * 确保每个子路径闭合（供只需要轮廓的调用方使用）
     *
     * @param segments 路径段，不能为空
     * @return 末尾补闭合指令的新列表
     */
    public static List<PathSegment> ensureClosed(List<PathSegment> segments) {
        List<PathSegment> result = new ArrayList<>(segments.size() + 1);
        result.addAll(segments);
        if (!segments.isEmpty() && !(segments.get(segments.size() - 1) instanceof ClosePath)) {
            result.add(ClosePath.INSTANCE);
        }
        return result;
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
