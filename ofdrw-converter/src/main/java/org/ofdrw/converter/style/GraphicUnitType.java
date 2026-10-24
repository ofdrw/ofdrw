package org.ofdrw.converter.style;

/**
 * 参与样式结算的图元类型，用于选择类型默认值。
 */
public enum GraphicUnitType {

    /**
     * 文字对象：默认填充黑色，不描边
     */
    TEXT,

    /**
     * 路径对象：默认不填充，描边黑色
     */
    PATH,

    /**
     * 图像对象：不受线型影响
     */
    IMAGE,

    /**
     * 组合对象：内容由其矢量图内部的图元决定
     */
    COMPOSITE
}
