package org.ofdrw.converter.style;

/**
 * 样式作用域类型。
 * <p>
 * 作用域从文档根逐层嵌套到具体图元，用于区分不同层级对样式的贡献。
 */
public enum StyleScopeKind {

    /**
     * 文档根作用域
     */
    DOCUMENT,

    /**
     * 图层作用域（CT_Layer）
     */
    LAYER,

    /**
     * 页块作用域（CT_PageBlock）
     */
    PAGEBLOCK,

    /**
     * 组合对象作用域（CompositeObject 引用的矢量图内容）
     */
    COMPOSITE,

    /**
     * 注释外观作用域（Annot.Appearance）
     */
    ANNOTATION,

    /**
     * 图元对象作用域
     */
    OBJECT
}
