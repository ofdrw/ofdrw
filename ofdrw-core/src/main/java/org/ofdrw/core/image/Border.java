package org.ofdrw.core.image;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.pageDescription.color.color.CT_Color;

/**
 * 图像边框
 * <p>
 * 10 表 43
 *
 * @author 权观宇
 * @since 2019-10-27 02:45:46
 */
public class Border extends OFDElement {
    public Border(Element proxy) {
        super(proxy);
    }

    public Border() {
        super("Border");
    }

    /**
     * 【可选 属性】
     * 设置 边框线宽
     * <p>
     * 如果为 0 则表示边框不进行绘制
     * <p>
     * 默认值为 0.353 mm
     *
     * @param lineWidth 边框线宽
     * @return this
     */
    public Border setLineWidth(Double lineWidth) {
        if (lineWidth == null) {
            this.removeAttr("LineWidth");
            return this;
        }
        this.addAttribute("LineWidth", lineWidth.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 边框线宽
     * <p>
     * 如果为 0 则表示边框不进行绘制
     * <p>
     * 默认值为 0.353 mm
     *
     * @return 边框线宽
     */
    public Double getLineWidth() {
        String str = this.attributeValue("LineWidth");
        if (str == null || str.trim().length() == 0) {
            return 0.353d;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选 属性】
     * 设置 边框水平角半径
     * <p>
     * 默认值为 0
     *
     * @param horizonalCornerRadius 边框水平角半径
     * @return this
     */
    public Border setHorizonalCornerRadius(Double horizonalCornerRadius) {
        if (horizonalCornerRadius == null) {
            this.removeAttr("HorizonalCornerRadius");
            return this;
        }
        this.addAttribute("HorizonalCornerRadius", horizonalCornerRadius.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 边框水平角半径
     * <p>
     * 默认值为 0
     *
     * @return 边框水平角半径
     */
    public Double getHorizonalCornerRadius() {
        String str = this.attributeValue("HorizonalCornerRadius");
        if (str == null || str.trim().length() == 0) {
            return 0d;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选 属性】
     * 设置 边框垂直角半径
     * <p>
     * 默认值为 0
     *
     * @param verticalCornerRadius 边框垂直角半径
     * @return this
     */
    public Border setVerticalCornerRadius(Double verticalCornerRadius) {
        if (verticalCornerRadius == null) {
            this.removeAttr("VerticalCornerRadius");
            return this;
        }
        this.addAttribute("VerticalCornerRadius", verticalCornerRadius.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 边框垂直角半径
     * <p>
     * 默认值为 0
     *
     * @return 边框垂直角半径
     */
    public Double getVerticalCornerRadius() {
        String str = this.attributeValue("VerticalCornerRadius");
        if (str == null || str.trim().length() == 0) {
            return 0d;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选 属性】
     * 设置 边框虚线重复样式开始的位置
     * <p>
     * 边框的起点位置为左上角，绕行方向为顺时针
     * <p>
     * 默认值为 0
     *
     * @param dashOffset 边框虚线重复样式开始的位置
     * @return this
     */
    public Border setDashOffset(Double dashOffset) {
        if (dashOffset == null) {
            this.removeAttr("DashOffset");
            return this;
        }
        this.addAttribute("DashOffset", dashOffset.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 边框虚线重复样式开始的位置
     * <p>
     * 边框的起点位置为左上角，绕行方向为顺时针
     * <p>
     * 默认值为 0
     *
     * @return 边框虚线重复样式开始的位置
     */
    public Double getDashOffset() {
        String str = this.attributeValue("DashOffset");
        if (str == null || str.trim().length() == 0) {
            return 0d;
        }
        return Double.parseDouble(str);
    }


    /**
     * 【属性 可选】
     * 设置 边框虚线重复样式
     * <p>
     * 边框的起点位置为左上角，绕行方向为顺时针
     *
     * @param dashPattern 边框虚线重复样式
     * @return this
     */
    public Border setDashPattern(ST_Array dashPattern) {
        if (dashPattern == null) {
            this.removeAttr("DashPattern");
            return this;
        }
        this.addAttribute("DashPattern", dashPattern.toString());
        return this;
    }

    /**
     * 【属性 可选】
     * 获取 边框虚线重复样式
     * <p>
     * 边框的起点位置为左上角，绕行方向为顺时针
     *
     * @return 边框虚线重复样式
     */
    public ST_Array getDashPattern() {
        return ST_Array.getInstance(this.attributeValue("DashPattern"));
    }


    /**
     * 【可选】
     * 设置 边框颜色
     * <p>
     * 有关边框颜色描述见 8.3.2 基本颜色
     * <p>
     * 默认为黑色
     *
     * @param borderColor 边框颜色
     * @return this
     */
    public Border setBorderColor(CT_Color borderColor) {
        if (borderColor == null) {
            return this;
        }
        if (!(borderColor instanceof BorderColor)) {
            this.setOFDName("BorderColor");
        }
        this.add(borderColor);
        return this;
    }

    /**
     * 【可选】
     * 获取 边框颜色
     * <p>
     * 有关边框颜色描述见 8.3.2 基本颜色
     * <p>
     * 默认为黑色
     *
     * @return 边框颜色，null表示为黑色
     */
    public BorderColor getBorderColor() {
        Element e = this.getOFDElement("BorderColor");
        return e == null ? null : new BorderColor(e);
    }
}
