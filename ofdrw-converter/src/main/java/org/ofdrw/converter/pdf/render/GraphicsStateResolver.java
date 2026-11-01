package org.ofdrw.converter.pdf.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.DashPattern;
import org.ofdrw.converter.pdf.model.LineCap;
import org.ofdrw.converter.pdf.model.LineJoin;
import org.ofdrw.converter.pdf.model.StrokeStyle;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 绘制参数解析器：实现 OFD 的绘制参数就近原则与 DrawParam 继承链
 * <p>
 * 设计 3.3 节（OFD 8.2.1）：图元属性 &gt; 图元 DrawParam &gt; 图层 DrawParam &gt; 基础 DrawParam（{@code Relative} 递归）&gt; 默认值。
 * 实现方式：
 * <ol>
 *     <li>从起始绘制参数沿 {@code Relative} 链展开，链尾（最基础参数）先取值，链头最后覆盖；</li>
 *     <li>再用图元（或图层）自身 DrawParam 中<b>已定义</b>的字段覆盖；</li>
 *     <li>最后用图元同名属性（如 {@code LineWidth}、{@code Alpha}）覆盖；</li>
 *     <li>始终未定义的字段取 OFD 规范默认值（线宽 0.353mm、平头、尖角、斜接 3.528、实线、Alpha 255）。</li>
 * </ol>
 * <p>
 * 安全约束：{@code Relative} 链最大深度为 {@link #MAX_RELATIVE_DEPTH}（32），
 * 超出深度或出现环引用时截断链路并输出 warning，防止栈溢出（设计 3.3 节）。
 * <p>
 * 单位：线宽与虚线长度保留 OFD 原始单位 mm；换算为 pt 统一通过 {@link CoordSystem} 完成（设计约束 2）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class GraphicsStateResolver {

    /**
     * {@code Relative} 继承链的最大深度
     */
    public static final int MAX_RELATIVE_DEPTH = 32;

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(GraphicsStateResolver.class);

    /**
     * 绘制参数 ID → OFD 绘制参数对象的查找函数
     */
    @FunctionalInterface
    public interface DrawParamLookup {

        /**
         * 按 ID 查找绘制参数
         *
         * @param id 绘制参数 ID（{@link ST_ID} 的字符串形式）
         * @return 绘制参数；{@code null} 表示不存在
         */
        CT_DrawParam getDrawParam(String id);
    }

    /**
     * 绘制参数查找函数
     */
    private final DrawParamLookup lookup;

    /**
     * 按 ID 查找绘制参数资源
     *
     * @param id 绘制参数 ID（{@link ST_ID} 的字符串形式）
     * @return 绘制参数；不存在时返回 {@code null}
     */
    public CT_DrawParam getDrawParam(String id) {
        return lookup == null ? null : lookup.getDrawParam(id);
    }

    /**
     * 完整展开的继承链缓存（键为起始绘制参数 ID）
     */
    private final ConcurrentHashMap<String, EffectiveDrawParam> chainCache = new ConcurrentHashMap<>();

    /**
     * 已经告警过的原因，避免整页重复刷屏
     */
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    /**
     * 构造解析器
     *
     * @param lookup 绘制参数查找函数，不能为空
     */
    public GraphicsStateResolver(DrawParamLookup lookup) {
        if (lookup == null) {
            throw new IllegalArgumentException("绘制参数查找函数不能为空");
        }
        this.lookup = lookup;
    }

    /**
     * 获取解析器名称
     *
     * @return 名称
     */
    public String getName() {
        return "GraphicsStateResolver";
    }

    /**
     * 解析绘制参数引用为有效参数（含 {@code Relative} 链展开与默认值填充）
     *
     * @param ref 绘制参数引用，可为空
     * @return 有效绘制参数；{@code ref} 为空或对象不存在时返回 {@link EffectiveDrawParam#defaults()}
     */
    public EffectiveDrawParam resolve(ST_RefID ref) {
        String id = idOf(ref);
        if (id == null) {
            return EffectiveDrawParam.defaults();
        }
        return chainCache.computeIfAbsent(id, this::resolveChain);
    }

    /**
     * 解析单个绘制参数对象（不含 {@code Relative} 链）为有效参数
     *
     * @param param 绘制参数，可为空
     * @return 有效绘制参数；{@code param} 为空时返回默认值
     */
    public EffectiveDrawParam resolve(CT_DrawParam param) {
        if (param == null) {
            return EffectiveDrawParam.defaults();
        }
        return apply(param, EffectiveDrawParam.defaults());
    }

    /**
     * 在基准参数上叠加图元自身的绘制属性（属性优先于 DrawParam）
     *
     * @param base      基准参数，可为空（按默认值处理）
     * @param lineWidth 图元 {@code LineWidth} 属性，单位 mm，可为空
     * @param cap       图元 {@code Cap} 属性，可为空
     * @param join      图元 {@code Join} 属性，可为空
     * @param miter     图元 {@code MiterLimit} 属性，可为空
     * @param dash      图元 {@code DashPattern} 属性，单位 mm，可为空
     * @param alpha     图元 {@code Alpha} 属性（0~255），可为空（决策 2：缺省不透明）
     * @return 叠加后的新参数
     */
    public EffectiveDrawParam applyUnitAttributes(EffectiveDrawParam base, Double lineWidth, LineCapType cap,
                                                  LineJoinType join, Double miter, ST_Array dash, Integer alpha) {
        EffectiveDrawParam from = base == null ? EffectiveDrawParam.defaults() : base;
        return from.override(
                legalLength(lineWidth),
                capOf(cap),
                joinOf(join),
                legalMiter(miter),
                dashOf(dash),
                clampAlpha(alpha),
                null,
                null);
    }

    /**
     * 生成描边样式指令（无图元 CTM 缩放，按 mm→pt 换算线宽）
     *
     * @param param 有效绘制参数，不能为空
     * @return 描边样式指令（线宽与虚线已换算为 pt）
     */
    public static StrokeStyle toStrokeStyle(EffectiveDrawParam param) {
        // 无图元 CTM：缩放倍数即内容坐标的 mm→pt 比例
        return toStrokeStyle(param, CoordSystem.CONTENT_MM_TO_PT);
    }

    /**
     * 生成描边样式指令
     *
     * <p>线宽是“对象空间长度”，必须与坐标一样经过图元 CTM 缩放：参考实现把 {@code LineWidth}
     * 原值写在 mm 空间中（外层 {@code 2.8346 cm}），生效线宽由 {S} 命令时的总 CTM 决定，
     * 例如 0.5mm 边框写作 {@code 1.4173 w}（CTM 合计 1.0，有效 1.4173pt），而无图元 CTM 的
     * {@code .12 w}（CTM 合计 2.8346）有效线宽是 0.34pt。因此这里按图元 CTM 的线性缩放倍数
     * （已含 mm→pt）换算，避免整页统一乘或统一乘 2.8346 造成的三倍粗细偏差。</p>
     *
     * @param param    有效绘制参数，不能为空
     * @param ctmScale 图元 CTM 到页面 pt 的线性缩放倍数（mm→pt 已包含在内），正数
     * @return 描边样式指令（线宽与虚线已换算为 pt）
     */
    public static StrokeStyle toStrokeStyle(EffectiveDrawParam param, double ctmScale) {
        if (param == null) {
            throw new IllegalArgumentException("绘制参数不能为空");
        }
        double scale = ctmScale > 0d && !Double.isNaN(ctmScale) ? ctmScale : 1d;
        // scale 已含 mm→pt（由 CoordSystem.objectToPage 的 CONTENT_MM_TO_PT 提供），
        // 线宽与虚线均为 mm 空间的长度，统一只乘 scale，避免虚线被重复换算
        double[] dashes = param.getDashPattern().getDashArray();
        double[] inPt = new double[dashes.length];
        for (int i = 0; i < dashes.length; i++) {
            inPt[i] = dashes[i] * scale;
        }
        DashPattern dashInPt = new DashPattern(inPt,
                param.getDashPattern().getDashPhase() * scale);
        return new StrokeStyle(param.getLineWidthMm() * scale,
                param.getLineCap(), param.getLineJoin(), param.getMiterLimit(), dashInPt);
    }

    /**
     * 沿 {@code Relative} 链展开绘制参数
     * <p>
     * 父参数中<b>已定义</b>的字段作为基础值，被更近一层的参数覆盖；
     * 深度超过 {@link #MAX_RELATIVE_DEPTH} 或出现环引用时截断并告警。
     *
     * @param id 起始绘制参数 ID
     * @return 展开后的参数（已填充默认值）
     */
    private EffectiveDrawParam resolveChain(String id) {
        List<CT_DrawParam> chain = chainOf(id);
        EffectiveDrawParam result = EffectiveDrawParam.defaults();
        // 链尾是最基础参数，链头（起始参数）最贴近图元，因此从链尾向链头依次覆盖
        for (int i = chain.size() - 1; i >= 0; i--) {
            result = apply(chain.get(i), result);
        }
        return result;
    }

    /**
     * 解析图元的绘制参数，并把图层（容器）绘制参数作为继承基准
     * <p>
     * OFD 8.2.1 就近原则：图元属性 &gt; 图元 DrawParam &gt; 图层 DrawParam &gt; {@code Relative} 链 &gt; 默认值。
     * 与 {@link #resolve(ST_RefID)} 的差别在于基准不是默认值而是图层参数：图元未定义的字段
     * 会继续沿用图层声明的值（例如图层统一声明的描边色、线宽）。
     *
     * @param parent 图层有效绘制参数（已展开 Relative 链），可为空（为空等价于 {@link #resolve(ST_RefID)}）
     * @param ref    图元绘制参数引用，可为空
     * @return 有效绘制参数
     */
    public EffectiveDrawParam resolveInherited(EffectiveDrawParam parent, ST_RefID ref) {
        EffectiveDrawParam base = parent == null ? EffectiveDrawParam.defaults() : parent;
        String id = idOf(ref);
        if (id == null) {
            return base;
        }
        List<CT_DrawParam> chain = chainOf(id);
        for (int i = chain.size() - 1; i >= 0; i--) {
            base = apply(chain.get(i), base);
        }
        return base;
    }

    /**
     * 展开绘制参数 {@code Relative} 链
     *
     * @param id 起始绘制参数 ID
     * @return 从起始参数到链尾的参数列表
     */
    private List<CT_DrawParam> chainOf(String id) {
        List<CT_DrawParam> chain = new ArrayList<>(4);
        Set<String> visited = new HashSet<>();
        String current = id;
        while (current != null) {
            if (!visited.add(current)) {
                warn("drawparam-cycle", "绘制参数 Relative 链存在环引用，已截断：" + visited + " -> " + current);
                break;
            }
            if (visited.size() > MAX_RELATIVE_DEPTH) {
                warn("drawparam-depth", "绘制参数 Relative 链深度超过 " + MAX_RELATIVE_DEPTH + "，已截断：" + id);
                break;
            }
            CT_DrawParam param = lookup.getDrawParam(current);
            if (param == null) {
                warn("drawparam-missing-" + current, "绘制参数引用不存在：ID=" + current);
                break;
            }
            chain.add(param);
            String next = idOf(param.getRelative());
            current = next;
        }
        return chain;
    }

    /**
     * 用绘制参数中已定义的字段覆盖基准参数
     *
     * @param param 绘制参数，不能为空
     * @param base  基准参数，不能为空
     * @return 覆盖后的新参数
     */
    private EffectiveDrawParam apply(CT_DrawParam param, EffectiveDrawParam base) {
        org.ofdrw.core.pageDescription.color.color.CT_Color fill = param.getFillColor();
        return base.override(
                legalLength(attributeLength(param, "LineWidth")),
                capOf(capValue(param.attributeValue("Cap"))),
                joinOf(joinValue(param.attributeValue("Join"))),
                legalMiter(attributeLength(param, "MiterLimit")),
                dashOf(param.getDashPattern()),
                null,
                fill,
                param.getStrokeColor());
    }

    /**
     * 读取绘制参数的可选数值属性
     * <p>
     * OFD 核心模型中 {@code getLineWidth()} 在属性缺失时返回默认值 0.353，无法区分“未定义”与“显式 0.353”；
     * 就近原则要求“未定义字段不覆盖继承值”，因此这里直接读取属性本身。
     *
     * @param param    绘制参数，不能为空
     * @param attrName 属性名
     * @return 属性值；属性缺失或无法解析为有限数值时返回 {@code null}
     */
    private static Double attributeLength(CT_DrawParam param, String attrName) {
        String text = param.attributeValue(attrName);
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            double value = Double.parseDouble(text.trim());
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析线端样式文本
     * <p>
     * 属性缺失（{@code text} 为 null 或空白）时返回 {@code null}，表示“未定义”。
     * 不能直接交给 {@link LineCapType#getInstance(String)}：该方法按规范把空串映射为默认值 {@code Butt}，
     * 会把“未定义”误判为“显式 Butt”，进而打断就近原则的继承链。
     *
     * @param text 属性文本
     * @return 线端类型；未定义或无法解析返回 {@code null}
     */
    private static LineCapType capValue(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LineCapType.getInstance(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 解析线连接样式文本
     * <p>
     * 属性缺失（{@code text} 为 null 或空白）时返回 {@code null}，表示“未定义”；
     * 理由同 {@link #capValue(String)}：{@link LineJoinType#getInstance(String)} 会把空串映射为默认值 {@code Miter}。
     *
     * @param text 属性文本
     * @return 线连接类型；未定义或无法解析返回 {@code null}
     */
    private static LineJoinType joinValue(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LineJoinType.getInstance(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 线宽合法性：非负有限数值，否则视为未定义
     *
     * @param value 线宽，单位 mm
     * @return 合法线宽，或 {@code null}
     */
    private static Double legalLength(Double value) {
        if (value == null || !Double.isFinite(value) || value < 0) {
            return null;
        }
        return value;
    }

    /**
     * 斜接限制合法性：正有限数值，否则视为未定义
     *
     * @param value 斜接限制
     * @return 合法值，或 {@code null}
     */
    private static Double legalMiter(Double value) {
        if (value == null || !Double.isFinite(value) || value <= 0) {
            return null;
        }
        return value;
    }

    /**
     * 解析线端样式
     *
     * @param cap OFD 线端类型
     * @return 线端样式；未定义返回 {@code null}
     */
    private static LineCap capOf(LineCapType cap) {
        if (cap == null) {
            return null;
        }
        switch (cap) {
            case Round:
                return LineCap.ROUND;
            case Square:
                return LineCap.SQUARE;
            case Butt:
            default:
                return LineCap.BUTT;
        }
    }

    /**
     * 解析线连接样式
     *
     * @param join OFD 线连接类型
     * @return 线连接样式；未定义返回 {@code null}
     */
    private static LineJoin joinOf(LineJoinType join) {
        if (join == null) {
            return null;
        }
        switch (join) {
            case Round:
                return LineJoin.ROUND;
            case Bevel:
                return LineJoin.BEVEL;
            case Miter:
            default:
                return LineJoin.MITER;
        }
    }

    /**
     * 解析虚线样式
     * <p>
     * OFD {@code DashPattern} 数组为“线段长度、间隔长度”交替序列，单位 mm；
     * 非法值按 0 处理，全 0（不含正数）时由 {@link DashPattern} 退化为实线。
     *
     * @param dash 虚线数组
     * @return 虚线样式；未定义返回 {@code null}
     */
    private static DashPattern dashOf(ST_Array dash) {
        if (dash == null || dash.size() == 0) {
            return null;
        }
        List<String> items = dash.getArray();
        double[] values = new double[items.size()];
        for (int i = 0; i < items.size(); i++) {
            Double value = parseLength(items.get(i));
            values[i] = value == null || value < 0 ? 0 : value;
        }
        return new DashPattern(values, 0);
    }

    /**
     * 解析长度字符串（支持 {@code #} 十六进制与十进制）
     *
     * @param text 原始文本，单位 mm
     * @return 长度；无法解析返回 {@code null}
     */
    private static Double parseLength(String text) {
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

    /**
     * Alpha 收敛到 0~255
     *
     * @param alpha OFD Alpha 属性
     * @return 合法 Alpha；未定义返回 {@code null}
     */
    private static Integer clampAlpha(Integer alpha) {
        if (alpha == null) {
            return null;
        }
        return Math.max(0, Math.min(255, alpha));
    }

    /**
     * 提取引用 ID 的字符串形式
     *
     * @param ref 引用 ID
     * @return ID 字符串，{@code null} 表示无法取得
     */
    static String idOf(ST_RefID ref) {
        if (ref == null) {
            return null;
        }
        try {
            ST_ID id = ref.getRefId();
            return id == null ? null : id.toString();
        } catch (RuntimeException e) {
            return ref.toString();
        }
    }

    /**
     * 输出一次告警（相同原因只告警一次）
     *
     * @param key     告警分类键
     * @param message 告警内容
     */
    private void warn(String key, String message) {
        if (reported.add(key)) {
            logger.warn("[{}] {}", RenderContext.DIAGNOSTIC_TAG, message);
        }
    }
}
