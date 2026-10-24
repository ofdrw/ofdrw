package org.ofdrw.converter.style;

import org.ofdrw.core.basicType.ST_RefID;

/**
 * 不可变样式作用域，代表样式链上的一个节点。
 * <p>
 * 每个作用域记录：作用域类型、自身引用的绘制参数 ID、内联样式（已结算结果）以及父作用域。
 * 由于字段全部为 final 且没有修改接口，任何“追加作用域”的操作都只能通过
 * {@link #child(StyleScopeKind, ST_RefID, ResolvedStyle)} 返回新实例，
 * 兄弟节点之间不可能互相看到对方追加的参数，从结构上消除了共享可变列表造成的样式污染。
 * <p>
 * 语义与 ofdrw-viewer-engine 的 {@code StyleScope} 一致：子作用域可覆盖父作用域。
 */
public final class StyleScope {

    /**
     * 作用域类型
     */
    private final StyleScopeKind kind;

    /**
     * 该作用域引用的绘制参数 ID，可为 null
     */
    private final ST_RefID drawParamId;

    /**
     * 父作用域，根作用域的父作用域为 null
     */
    private final StyleScope parent;

    /**
     * 内联样式（已结算的块根样式），可为 null
     */
    private final ResolvedStyle inline;

    /**
     * 创建样式作用域。仅由本类内部与 {@link #child} 使用。
     *
     * @param kind        作用域类型
     * @param drawParamId 该作用域引用的绘制参数 ID，可为 null
     * @param parent      父作用域，根作用域为 null
     * @param inline      内联样式，可为 null
     */
    private StyleScope(StyleScopeKind kind, ST_RefID drawParamId, StyleScope parent, ResolvedStyle inline) {
        this.kind = kind == null ? StyleScopeKind.DOCUMENT : kind;
        this.drawParamId = drawParamId;
        this.parent = parent;
        this.inline = inline;
    }

    /**
     * 创建文档根作用域。
     *
     * @return 类型为 {@link StyleScopeKind#DOCUMENT} 的无父作用域节点
     */
    public static StyleScope document() {
        return new StyleScope(StyleScopeKind.DOCUMENT, null, null, null);
    }

    /**
     * 追加子作用域。
     * <p>
     * 始终返回新实例，当前作用域保持不变，因此兄弟作用域之间不会共享任何可变状态。
     *
     * @param kind        子作用域类型
     * @param drawParamId 子作用域引用的绘制参数 ID，可为 null
     * @param inline      子作用域的内联样式，可为 null
     * @return 新的子作用域，其父作用域为当前作用域
     */
    public StyleScope child(StyleScopeKind kind, ST_RefID drawParamId, ResolvedStyle inline) {
        return new StyleScope(kind, drawParamId, this, inline);
    }

    /**
     * 获取作用域类型。
     *
     * @return 作用域类型
     */
    public StyleScopeKind getKind() {
        return kind;
    }

    /**
     * 获取该作用域引用的绘制参数 ID。
     *
     * @return 绘制参数 ID，null 表示该作用域未引用绘制参数
     */
    public ST_RefID getDrawParamId() {
        return drawParamId;
    }

    /**
     * 获取父作用域。
     *
     * @return 父作用域，null 表示当前为根作用域
     */
    public StyleScope getParent() {
        return parent;
    }

    /**
     * 获取内联样式。
     *
     * @return 内联样式，null 表示该作用域没有内联样式
     */
    public ResolvedStyle getInline() {
        return inline;
    }

    /**
     * 作用域链文本表示，便于告警与调试。
     *
     * @return 形如 {@code OBJECT > LAYER > DOCUMENT} 的作用域链描述
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(kind.name());
        StyleScope scope = this;
        while (scope.parent != null) {
            scope = scope.parent;
            sb.append(" > ").append(scope.kind.name());
        }
        return sb.toString();
    }
}
