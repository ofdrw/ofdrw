package org.ofdrw.converter.style;

import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.color.color.CT_Color;

import java.awt.Color;

/**
 * 样式结算器：按作用域链与图元内联属性结算最终绘制样式。
 * <p>
 * 结算优先级（由低到高）：
 * <ol>
 *     <li>对象类型默认值；</li>
 *     <li>作用域链继承：父作用域先结算，子作用域覆盖父作用域，继承内容只有线型与绘制参数中的颜色；</li>
 *     <li>图元自身的内联属性（Fill/Stroke 开关、颜色、透明度）。</li>
 * </ol>
 * 其中 Fill/Stroke 开关与 Alpha 不从父作用域继承，只取图元自身的属性；
 * 绘制参数中的颜色优先于类型默认色，图元自身的颜色优先级最高。
 */
public class StyleResolver {

    /**
     * 默认线宽，单位：毫米（mm）
     */
    public static final double DEFAULT_LINE_WIDTH = 0.353;

    /**
     * 默认尖角截断值（Join 为 Miter 时有效），无单位
     */
    public static final double DEFAULT_MITER_LIMIT = 3.528;

    /**
     * 默认描边颜色，sRGB 黑色
     */
    public static final int DEFAULT_STROKE_COLOR = 0x000000;

    /**
     * 绘制参数结算器
     */
    private final DrawParamResolver drawParamResolver;

    /**
     * 颜色解析器，用于把图元自身颜色换算为 sRGB
     */
    private final DrawParamResolver.DrawParamLookup colorLookup;

    /**
     * 创建样式结算器。
     *
     * @param drawParamResolver 绘制参数结算器，不得为 null
     * @param colorLookup       颜色解析器，与绘制参数结算器使用同一查找器
     */
    public StyleResolver(DrawParamResolver drawParamResolver, DrawParamResolver.DrawParamLookup colorLookup) {
        this.drawParamResolver = drawParamResolver;
        this.colorLookup = colorLookup;
    }

    /**
     * 结算作用域链自身的样式（不包含图元内联属性）。
     * <p>
     * 继承内容只有线型与绘制参数颜色；Fill/Stroke 开关与 Alpha 保持默认值（false/false/1）。
     * 每次调用都从根作用域重新展开并生成新实例，不缓存，避免任何跨对象的共享可变状态。
     *
     * @param scope 当前作用域
     * @return 作用域链结算出的样式
     */
    public ResolvedStyle resolveScope(StyleScope scope) {
        if (scope == null) {
            return documentDefault();
        }
        ResolvedStyle parentStyle = resolveScope(scope.getParent());
        ResolvedStyle style = resolveWithDrawParam(parentStyle, scope.getDrawParamId(), scope.getKind());
        if (scope.getInline() != null) {
            style = applyTo(style, scope.getInline());
        }
        return style;
    }

    /**
     * 把绘制参数合并到继承样式上。
     * <p>
     * 图层与图元（就近原则）中的绘制参数除线型外，还会贡献填充色与描边色，
     * 供其下未声明颜色的图元继承；页面块/组合等容器作用域只继承线型，
     * 避免容器层的颜色意外覆盖图元的类型默认色。
     *
     * @param base        下层样式
     * @param drawParamId 绘制参数 ID，null 表示无
     * @param kind        作用域类型
     * @return 合并后的新样式
     */
    private ResolvedStyle resolveWithDrawParam(ResolvedStyle base, ST_RefID drawParamId, StyleScopeKind kind) {
        ResolvedStyle patch = drawParamResolver.resolve(drawParamId);
        if (patch == null) {
            return base;
        }
        if (kind == StyleScopeKind.LAYER || kind == StyleScopeKind.OBJECT) {
            return applyTo(base, patch);
        }
        return base.derive(patch.getLineWidth(), patch.getLineCap(), patch.getLineJoin(), patch.getMiterLimit(),
                patch.getDashOffset(), patch.getDashPattern(), null, null, null, null, null);
    }

    /**
     * 按图元类型取默认样式。
     *
     * @param type 图元类型
     * @return 默认样式
     */
    public ResolvedStyle kindDefault(GraphicUnitType type) {
        if (type == GraphicUnitType.TEXT) {
            // 文字：默认填充黑色，不描边
            return new ResolvedStyle(DEFAULT_LINE_WIDTH, LineCapStyle.BUTT, LineJoinStyle.MITER,
                    DEFAULT_MITER_LIMIT, 0, new double[0], 0x000000, null, true, false, 1);
        }
        if (type == GraphicUnitType.PATH) {
            // 路径：默认不填充，描边黑色
            return new ResolvedStyle(DEFAULT_LINE_WIDTH, LineCapStyle.BUTT, LineJoinStyle.MITER,
                    DEFAULT_MITER_LIMIT, 0, new double[0], null, DEFAULT_STROKE_COLOR, false, true, 1);
        }
        // 其它图元：不填充不描边
        return new ResolvedStyle(DEFAULT_LINE_WIDTH, LineCapStyle.BUTT, LineJoinStyle.MITER,
                DEFAULT_MITER_LIMIT, 0, new double[0], null, null, false, false, 1);
    }

    /**
     * 结算单个图元的最终样式。
     *
     * @param scope       图元所在作用域
     * @param type        图元类型，用于取类型默认值
     * @param fill        图元的 Fill 属性，null 表示未设置（沿用类型默认值）
     * @param stroke      图元的 Stroke 属性，null 表示未设置（沿用类型默认值）
     * @param fillColor   图元自身的填充颜色，可为 null
     * @param strokeColor 图元自身的描边颜色，可为 null
     * @param alpha       图元自身的透明度，取值 0..1；null 表示未设置
     * @return 结算后的不可变样式
     */
    public ResolvedStyle resolve(StyleScope scope, GraphicUnitType type, Boolean fill, Boolean stroke,
                                 CT_Color fillColor, CT_Color strokeColor, Double alpha) {
        ResolvedStyle kindDefault = kindDefault(type);
        ResolvedStyle inherited = resolveScope(scope);

        Boolean resolvedFill = fill == null ? Boolean.valueOf(kindDefault.isFill()) : fill;
        Boolean resolvedStroke = stroke == null ? Boolean.valueOf(kindDefault.isStroke()) : stroke;
        // 图元自身颜色 > 继承（图层/对象绘制参数）颜色 > 类型默认颜色
        Integer ownFill = toRgb(fillColor);
        Integer ownStroke = toRgb(strokeColor);
        Integer resolvedFillColor = ownFill != null ? ownFill : inherited.getFillColor();
        if (resolvedFillColor == null) {
            resolvedFillColor = kindDefault.getFillColor();
        }
        // 路径的填充色默认为透明：既无绘制参数也无图元颜色时不产生填充色
        if (resolvedFillColor == null && type == GraphicUnitType.TEXT) {
            resolvedFillColor = Integer.valueOf(DEFAULT_STROKE_COLOR);
        }
        Integer resolvedStrokeColor = ownStroke != null ? ownStroke : inherited.getStrokeColor();
        if (resolvedStrokeColor == null) {
            resolvedStrokeColor = kindDefault.getStrokeColor() != null
                    ? kindDefault.getStrokeColor()
                    : Integer.valueOf(DEFAULT_STROKE_COLOR);
        }
        Double resolvedAlpha = alpha == null ? Double.valueOf(1) : alpha;

        return inherited.derive(inherited.getLineWidth(), inherited.getLineCap(), inherited.getLineJoin(),
                inherited.getMiterLimit(), inherited.getDashOffset(), inherited.getDashPattern(),
                resolvedFillColor, resolvedStrokeColor, resolvedFill, resolvedStroke, resolvedAlpha);
    }

    /**
     * 文档默认样式。
     *
     * @return 文档根作用域的默认样式（线宽 0.353mm、描边黑色、不填充、透明度 1）
     */
    public ResolvedStyle documentDefault() {
        return new ResolvedStyle(DEFAULT_LINE_WIDTH, LineCapStyle.BUTT, LineJoinStyle.MITER,
                DEFAULT_MITER_LIMIT, 0, new double[0], null, DEFAULT_STROKE_COLOR, false, false, 1);
    }

    /**
     * 把上层样式合并进来；Fill/Stroke 开关与 Alpha 不从上层继承。
     *
     * @param base  下层样式
     * @param upper 上层（更近）样式，可为 null
     * @return 合并后的新样式
     */
    private ResolvedStyle applyTo(ResolvedStyle base, ResolvedStyle upper) {
        if (upper == null) {
            return base;
        }
        return base.derive(upper.getLineWidth(), upper.getLineCap(), upper.getLineJoin(), upper.getMiterLimit(),
                upper.getDashOffset(), upper.getDashPattern(), upper.getFillColor(), upper.getStrokeColor(),
                null, null, null);
    }

    /**
     * 把 OFD 颜色换算为 sRGB 整数。
     *
     * @param color OFD 颜色
     * @return sRGB 0xRRGGBB，null 表示未设置或无法解析
     */
    private Integer toRgb(CT_Color color) {
        if (color == null) {
            return null;
        }
        Color awtColor = colorLookup.toColor(color);
        return awtColor == null ? null : Integer.valueOf(awtColor.getRGB() & 0xFFFFFF);
    }
}
