package org.ofdrw.core.pageDescription.color.color;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

/**
 * 渐变控制点，至少出现三个
 * <p>
 * 8.6.4.4 表 31 附录 A.13 P125
 *
 * @author 权观宇
 * @since 2019-11-09 12:28:07
 */
public class Point extends OFDElement {
    public Point(Element proxy) {
        super(proxy);
    }

    public Point() {
        super("Point");
    }

    /**
     * 【必选 属性】
     * 设置  控制点水平位置
     *
     * @param x 控制点水平位置
     * @return this
     */
    public Point setX(Double x) {
        if (x == null) {
            throw new IllegalArgumentException("控制点水平位置（X）为空");
        }
        this.addAttribute("X", x.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取  控制点水平位置
     *
     * @return 控制点水平位置
     */
    public Double getX() {
        String str = this.attributeValue("X");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("控制点水平位置（X）为空");
        }
        return Double.parseDouble(str);
    }

    /**
     * 【必选 属性】
     * 设置  控制点垂直位置
     *
     * @param y 控制点垂直位置
     * @return this
     */
    public Point setY(Double y) {
        if (y == null) {
            throw new IllegalArgumentException("控制点垂直位置（Y）为空");
        }
        this.addAttribute("Y", y.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取  控制点垂直位置
     *
     * @return 控制点垂直位置
     */
    public Double getY() {
        String str = this.attributeValue("Y");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("控制点垂直位置（Y）为空");
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选 属性】
     * 设置 三角单元切换的方向标志
     *
     * @param edgeFlag 三角单元切换的方向标志
     * @return this
     */
    public Point setEdgeFlag(EdgeFlag edgeFlag) {
        if (edgeFlag == null) {
            this.removeAttr("EdgeFlag");
            return this;
        }
        this.addAttribute("EdgeFlag", edgeFlag.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 三角单元切换的方向标志
     *
     * @return 三角单元切换的方向标志
     */
    public EdgeFlag getEdgeFlag() {
        return EdgeFlag.getInstance(this.attributeValue("EdgeFlag"));
    }


    /**
     * 【必选】
     * 设置 控制点对应的颜色
     * <p>
     * 应使用基本颜色
     *
     * @param color 控制点对应的颜色，应使用基本颜色
     * @return this
     */
    public Point setColor(CT_Color color) {
        if (color == null) {
            throw new IllegalArgumentException("控制点对应的颜色（color）不能为空");
        }
        this.set(color);
        return this;
    }

    /**
     * 【必选】
     * 获取 控制点对应的颜色
     * <p>
     * 应使用基本颜色
     *
     * @return 控制点对应的颜色，应使用基本颜色
     */
    public CT_Color getColor() {
        Element e = this.getOFDElement("Color");
        return e == null ? null : new CT_Color(e);
    }
}
