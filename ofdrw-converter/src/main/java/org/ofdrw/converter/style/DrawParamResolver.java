package org.ofdrw.converter.style;

import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 绘制参数（DrawParam）结算器。
 * <p>
 * 负责解析单个绘制参数及其 Relative（基础绘制参数）链，把结果合并成一份不可变的 {@link ResolvedStyle}。
 * 结算顺序为：基础绘制参数的字段先出现，当前绘制参数的字段后覆盖（就近原则）。
 * <p>
 * 为避免环引用导致无限递归，Relative 链的最大深度默认为 {@value #DEFAULT_MAX_DEPTH}，
 * 超出深度或在链上重复遇到同一 ID 时截断并记录 warn 日志。
 */
public class DrawParamResolver {

    /**
     * Relative 链默认最大深度
     */
    public static final int DEFAULT_MAX_DEPTH = 16;

    /**
     * 日志
     */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 绘制参数查找器
     */
    private final DrawParamLookup lookup;

    /**
     * 已结算的绘制参数缓存，KEY：绘制参数 ID
     */
    private final Map<String, ResolvedStyle> cache = new HashMap<>();

    /**
     * 已告警过的缺失绘制参数 ID，避免同一文档重复输出相同告警
     */
    private final Set<String> reportedMissing = new HashSet<>();

    /**
     * 创建绘制参数结算器。
     *
     * @param lookup 绘制参数查找器，不得为 null
     */
    public DrawParamResolver(DrawParamLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * 结算指定绘制参数及其 Relative 链。
     *
     * @param drawParamId 绘制参数 ID，可为 null
     * @return 结算后的样式；ID 为 null 或无法找到资源时返回 null
     */
    public ResolvedStyle resolve(ST_RefID drawParamId) {
        if (drawParamId == null) {
            return null;
        }
        String key = idOf(drawParamId);
        if (key == null) {
            return null;
        }
        ResolvedStyle cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        List<CT_DrawParam> chain = new ArrayList<>(4);
        Set<String> visited = new HashSet<>();
        String current = key;
        boolean complete = true;
        while (current != null) {
            if (visited.contains(current)) {
                logger.warn("绘制参数 Relative 链存在环引用，已截断：{} -> {}", visited, current);
                complete = false;
                break;
            }
            if (visited.size() >= DEFAULT_MAX_DEPTH) {
                logger.warn("绘制参数 Relative 链深度超过 {}，已截断：{}", DEFAULT_MAX_DEPTH, visited);
                complete = false;
                break;
            }
            CT_DrawParam param = lookup.getDrawParam(current);
            if (param == null) {
                if (!reportedMissing.add(current)) {
                    // 同一缺失引用只告警一次，避免整页重复刷屏
                    complete = false;
                    break;
                }
                logger.warn("绘制参数引用不存在：ID={}", current);
                complete = false;
                break;
            }
            visited.add(current);
            chain.add(param);
            current = param.getRelative() == null ? null : idOf(param.getRelative());
        }
        if (chain.isEmpty()) {
            return null;
        }
        ResolvedStyle style = null;
        // 基础绘制参数在前、当前绘制参数在后，后者覆盖前者
        for (int i = chain.size() - 1; i >= 0; i--) {
            style = apply(chain.get(i), style);
        }
        // 只缓存完整展开的结果：链上存在缺失引用时（例如资源尚未加载完成）不缓存，避免后续命中不完整结果
        if (complete) {
            cache.put(key, style);
        }
        return style;
    }

    /**
     * 把单个绘制参数的字段合并到样式上。
     *
     * @param param    绘制参数
     * @param inherited 下层（基础）绘制参数结算出的样式，可为 null
     * @return 合并后的新样式
     */
    private ResolvedStyle apply(CT_DrawParam param, ResolvedStyle inherited) {
        ResolvedStyle style = inherited == null
                ? new ResolvedStyle(0.353, LineCapStyle.BUTT, LineJoinStyle.MITER, 3.528, 0, new double[0], null, null, false, false, 1)
                : inherited;
        return style.derive(
                numeric(param.attributeValue("LineWidth")),
                capOf(param.attributeValue("Cap")),
                joinOf(param.attributeValue("Join")),
                numeric(param.attributeValue("MiterLimit")),
                numeric(param.attributeValue("DashOffset")),
                dashPatternOf(param.attributeValue("DashPattern")),
                colorOf(param.getFillColor()),
                colorOf(param.getStrokeColor()),
                null,
                null,
                alphaOf(param.attributeValue("Alpha")));
    }

    /**
     * 解析数值属性，缺省或非法时返回 null。
     *
     * @param value 属性值
     * @return 数值，null 表示未设置
     */
    private Double numeric(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("绘制参数数值非法：{}", value);
            return null;
        }
    }

    /**
     * 解析透明属性，把 0..255 换算为 0..1。
     *
     * @param value 属性值
     * @return 透明度，null 表示未设置
     */
    private Double alphaOf(String value) {
        Double a = numeric(value);
        if (a == null) {
            return null;
        }
        return Math.max(0, Math.min(1, a / 255));
    }

    /**
     * 解析线端点样式。
     *
     * @param value 属性值
     * @return 线端点样式，null 表示未设置或非法
     */
    private LineCapStyle capOf(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            LineCapType type = LineCapType.getInstance(value);
            return LineCapStyle.valueOf(type.name().toUpperCase());
        } catch (Exception e) {
            logger.warn("绘制参数线端点样式非法：{}", value);
            return null;
        }
    }

    /**
     * 解析线条连接样式。
     *
     * @param value 属性值
     * @return 线条连接样式，null 表示未设置或非法
     */
    private LineJoinStyle joinOf(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            LineJoinType type = LineJoinType.getInstance(value);
            return LineJoinStyle.valueOf(type.name().toUpperCase());
        } catch (Exception e) {
            logger.warn("绘制参数线条连接样式非法：{}", value);
            return null;
        }
    }

    /**
     * 解析虚线重复样式。
     *
     * @param value 属性值，形如 {@code [3 2]}
     * @return 虚线样式，单位：毫米（mm）；null 表示未设置
     */
    private double[] dashPatternOf(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            List<String> items = org.ofdrw.core.basicType.ST_Array.getInstance(value).getArray();
            double[] pattern = new double[items.size()];
            for (int i = 0; i < items.size(); i++) {
                pattern[i] = Double.parseDouble(items.get(i).trim());
            }
            return pattern;
        } catch (Exception e) {
            logger.warn("绘制参数虚线样式非法：{}", value);
            return null;
        }
    }

    /**
     * 结算颜色（含色彩空间与调色板）。
     *
     * @param ctColor OFD 颜色
     * @return 颜色，sRGB 0xRRGGBB；null 表示未设置或无法解析
     */
    private Integer colorOf(CT_Color ctColor) {
        if (ctColor == null) {
            return null;
        }
        Color color = lookup.toColor(ctColor);
        if (color == null) {
            return null;
        }
        return color.getRGB() & 0xFFFFFF;
    }

    /**
     * 提取引用 ID 的字符串形式。
     *
     * @param refID 引用 ID
     * @return ID 字符串，null 表示无法取得
     */
    private String idOf(ST_RefID refID) {
        if (refID == null) {
            return null;
        }
        try {
            ST_ID id = refID.getRefId();
            return id == null ? null : id.toString();
        } catch (Exception e) {
            return refID.toString();
        }
    }

    /**
     * 绘制参数与颜色的查找抽象，便于在不依赖完整文档的情况下进行单元测试。
     */
    public interface DrawParamLookup {

        /**
         * 按 ID 查找绘制参数。
         *
         * @param id 绘制参数 ID
         * @return 绘制参数，null 表示不存在
         */
        CT_DrawParam getDrawParam(String id);

        /**
         * 把 OFD 颜色解析为 AWT 颜色。
         *
         * @param color OFD 颜色
         * @return AWT 颜色，null 表示无法解析
         */
        Color toColor(CT_Color color);
    }
}
