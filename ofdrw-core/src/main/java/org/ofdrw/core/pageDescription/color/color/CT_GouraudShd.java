package org.ofdrw.core.pageDescription.color.color;

import org.dom4j.Element;
import org.dom4j.QName;
import org.ofdrw.core.Const;
import org.ofdrw.core.OFDElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 高洛德渐变
 * <p>
 * 高洛德渐变的基本原理是指定三个带有可选颜色的顶点，在其构成的三角形区域内
 * 采用高洛德算法绘制渐变图形。
 * <p>
 * 8.3.4.4 高洛德渐变  图 41 表 31
 *
 * @author 权观宇
 * @since 2019-11-09 12:15:19
 */
public class CT_GouraudShd extends OFDElement implements ColorClusterType {
    public CT_GouraudShd(Element proxy) {
        super(proxy);
    }

    protected CT_GouraudShd(String name) {
        super(name);
    }

    public CT_GouraudShd() {
        super("GouraudShd");
    }

    /**
     * 【可选 属性】
     * 设置 在渐变控制点所确定的部分是否填充
     * <p>
     * 默认值为 false(0)
     *
     * @param extend false - 不填充； true - 填充
     * @return this
     */
    public CT_GouraudShd setExtend(Boolean extend) {
        if (extend == null) {
            this.removeAttr("Extend");
            return this;
        }

        this.addAttribute("Extend", String.valueOf(extend ? 1 : 0));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 在渐变控制点所确定的部分是否填充
     * <p>
     * 默认值为 false (0)
     *
     * @return false - 不填充； true - 填充
     */
    public Boolean getExtend() {
        String str = this.attributeValue("Extend");
        if (str == null || str.trim().length() == 0) {
            return false;
        }
        int extend = Integer.parseInt(str);
        return extend == 1;
    }

    /**
     * 【必选】
     * 增加  渐变控制点
     * <p>
     * 至少出现三个
     *
     * @param point 渐变控制点
     * @return this
     */
    public CT_GouraudShd addPoint(Point point) {
        if (point == null) {
            return this;
        }
        this.add(point);
        return this;
    }

    /**
     * 【必选】
     * 获取  渐变控制点列表
     * <p>
     * 至少出现三个
     *
     * @return 渐变控制点列表
     */
    public List<Point> getPoints() {
        List<Element> elements = this.getOFDElements("Point");
        if (elements == null || elements.size() == 0) {
            return Collections.emptyList();
        }
        List<Point> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(new Point(item)));
        return res;
    }


    /**
     * 【可选】
     * 设置 渐变范围外的填充颜色
     * <p>
     * 应使用基本颜色
     *
     * @param backColor 渐变范围外的填充颜色，应使用基本颜色
     * @return this
     */
    public CT_GouraudShd setBackColor(CT_Color backColor) {
        if (backColor == null) {
            this.removeAttr("BackColor");
            return this;
        }
        backColor.setOFDName("BackColor");
        this.set(backColor);
        return this;
    }

    /**
     * 【可选】
     * 获取 渐变范围外的填充颜色
     * <p>
     * 应使用基本颜色
     *
     * @return 渐变范围外的填充颜色，应使用基本颜色
     */
    public CT_Color getBackColor() {
        Element e = this.getOFDElement("BackColor");
        if (e != null) {
            e.setQName(new QName("Color", Const.OFD_NAMESPACE));
        }
        return e == null ? null : new CT_Color(e);
    }
}
