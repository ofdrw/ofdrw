package org.ofdrw.core.pageDescription.drawParam;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.STBase;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.graph.pathObj.FillColor;
import org.ofdrw.core.graph.pathObj.StrokeColor;
import org.ofdrw.core.pageDescription.color.color.CT_Color;

/**
 * 绘制参数
 * <p>
 * 绘制参数是一组用于控制绘制渲染效果的修饰参数的集合。
 * 绘制参数可以被不同的图元对象所共享。
 * <p>
 * 绘制参数可以继承已有的绘制参数，被继承的绘制参数称为
 * 该参数的“基础绘制参数”。
 * <p>
 * 图元对象通过绘制参数的标识符引用绘制参数。图元对象在引用
 * 绘制参数的同时，还可以定义自己的绘制属性，图元自有的绘制属性
 * 将覆盖引用的绘制参数中的同名属性。
 * <p>
 * 绘制参数可通过引用基础绘制参数的方式形成嵌套，对单个绘制参数而言，
 * 它继承了其基础绘制参数中的所有属性，并且可以重定义其基础绘制参数中的属性。
 * <p>
 * 绘制参数的作用顺序采用就近原则，即当多个绘制参数作用于同一个对象并且这些绘制参数
 * 中具有相同的要素时，采用与被作用对象关系最为密切的绘制参数的要素对其进行渲染。
 * 例如，当图元已经定义绘制参数时，则按定义属性进行渲染；当图元未定义绘制参数时，
 * 应首先按照图元定义的绘制参数进行渲染；图元未定义绘制参数时应采用所在图层的默认绘制参数
 * 渲染；当图元和所在图层都没有定义绘制参数时，按照各绘制属性的默认值进行渲染。
 * <p>
 * 8.2 绘制参数结构 图 22
 *
 * @author 权观宇
 * @since 2019-10-11 06:47:29
 */
public class CT_DrawParam extends OFDElement {
    public CT_DrawParam(Element proxy) {
        super(proxy);
    }

    public CT_DrawParam() {
        super("DrawParam");
    }

    public ST_ID getID(){
        return this.getObjID();
    }

    public CT_DrawParam setID(ST_ID id) {
        this.setObjID(id);
        return this;
    }


    /**
     * 【可选 属性】
     * 设置 基础绘制参数，引用资源文件中的绘制参数的标识符
     *
     * @param relative 引用资源文件中的绘制参数的标识符
     * @return this
     */
    public CT_DrawParam setRelative(ST_RefID relative) {
        this.addAttribute("Relative", relative.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 基础绘制参数，引用资源文件中的绘制参数的标识符
     *
     * @return 引用资源文件中的绘制参数的标识符
     */
    public ST_RefID getRelative() {
        return ST_RefID.getInstance(this.attributeValue("Relative"));
    }

    /**
     * 【可选 属性】
     * 设置 线宽
     * <p>
     * 非负浮点数，指定了绘制路径绘制时线的宽度。由于
     * 某些设备不能输出一个像素宽度的线，因此强制规定
     * 当线宽大于 0 时，无论多小都至少要绘制两个像素的宽度；
     * 当线宽为 0 时，绘制一个像素的宽度。由于线宽为 0 定义与
     * 设备相关，所以不推荐使用线宽为 0。
     * <p>
     * 默认值为 0.353 mm
     *
     * @param lineWidth 线宽
     * @return this
     * @throws NumberFormatException 线宽必须是非负浮点数
     */
    public CT_DrawParam setLineWidth(Double lineWidth) {
        if (lineWidth == null) {
            this.removeAttr("LineWidth");
            return this;
        }
        if (lineWidth < 0) {
            throw new NumberFormatException("线宽必须是非负浮点数");
        }
        this.addAttribute("LineWidth", STBase.fmt(lineWidth));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 线宽
     * <p>
     * 非负浮点数，指定了绘制路径绘制时线的宽度。由于
     * 某些设备不能输出一个像素宽度的线，因此强制规定
     * 当线宽大于 0 时，无论多小都至少要绘制两个像素的宽度；
     * 当线宽为 0 时，绘制一个像素的宽度。由于线宽为 0 定义与
     * 设备相关，所以不推荐使用线宽为 0。
     * <p>
     * 默认值为 0.353 mm
     *
     * @return 线宽
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
     * 设置 线条连接样式
     * <p>
     * 可选样式参照{@link LineJoinType}，线条连接样式的取值和显示效果之间的关系见表
     *
     * @param join 线条连接样式
     * @return this
     */
    public CT_DrawParam setJoin(LineJoinType join) {
        if (join == null) {
            this.removeAttr("Join");
            return this;
        }
        this.addAttribute("Join", join.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 线条连接样式
     * <p>
     * 可选样式参照{@link LineJoinType}，线条连接样式的取值和显示效果之间的关系见表
     *
     * @return 线条连接样式
     */
    public LineJoinType getJoin() {
        return LineJoinType.getInstance(this.attributeValue("Join"));
    }

    /**
     * 【可选 属性】
     * 设置  线端点样式
     * <p>
     * 可选样式参照{@link LineCapType}，线条端点样式取值与效果之间关系见表 24
     *
     * @param cap 线端点样式
     * @return this
     */
    public CT_DrawParam setCap(LineCapType cap) {
        if (cap == null) {
            this.removeAttr("Cap");
            return this;
        }
        this.addAttribute("Cap", cap.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 设置  线端点样式
     * <p>
     * 可选样式参照{@link LineCapType}，线条端点样式取值与效果之间关系见表 24
     * <p>
     * 默认值为 Butt
     *
     * @return 线端点样式
     */
    public LineCapType getCap() {
        return LineCapType.getInstance(this.attributeValue("Cap"));
    }

    /**
     * 【可选 属性】
     * 设置 线条虚线开始位置
     * <p>
     * 默认值为 0
     * <p>
     * 当 DashPattern 不出现时，该参数无效
     *
     * @param dashOffset 线条虚线开始位置
     * @return this
     */
    public CT_DrawParam setDashOffset(Double dashOffset) {
        if (dashOffset == null) {
            this.removeAttr("DashOffset");
            return this;
        }
        this.addAttribute("DashOffset", dashOffset.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 线条虚线开始位置
     * <p>
     * 默认值为 0
     * <p>
     * 当 DashPattern 不出现时，该参数无效
     *
     * @return 线条虚线开始位置
     */
    public Double getDashOffset() {
        String str = this.attributeValue("DashOffset");
        if (str == null || str.trim().length() == 0) {
            return 0d;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选 属性】
     * 设置 线条虚线的重复样式
     * <p>
     * 数组中共含两个值，第一个值代表虚线的线段的长度，
     * 第二个值代表虚线间隔的长度。
     * <p>
     * 默认值为空。
     * <p>
     * 线条样式的控制效果见表 23
     *
     * @param dashPattern 线条虚线的重复样式的数组中共含两个值，第一个值代表虚线的线段的长度，第二个值代表虚线间隔的长度。
     * @return this
     */
    public CT_DrawParam setDashPattern(ST_Array dashPattern) {
        if (dashPattern == null) {
            this.removeAttr("DashPattern");
            return this;
        }
        this.addAttribute("DashPattern", dashPattern.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 线条虚线的重复样式
     * <p>
     * 数组中共含两个值，第一个值代表虚线的线段的长度，
     * 第二个值代表虚线间隔的长度。
     * <p>
     * 默认值为空。
     * <p>
     * 线条样式的控制效果见表 23
     *
     * @return 线条虚线的重复样式的数组中共含两个值，第一个值代表虚线的线段的长度，第二个值代表虚线间隔的长度。
     */
    public ST_Array getDashPattern() {
        return ST_Array.getInstance(this.attributeValue("DashPattern"));
    }

    /**
     * 【可选 属性】
     * 设置 Join的截断值
     * <p>
     * Join为 Miter 时小角度结合点长度的截断值，默认值为 3.528。
     * 当 Join  不等于 Miter 时该参数无效。
     *
     * @param miterLimit Join的截断值长度
     * @return this
     */
    public CT_DrawParam setMiterLimit(Double miterLimit) {
        if (miterLimit == null) {
            this.removeAttr("MiterLimit");
            return this;
        }
        this.addAttribute("MiterLimit", miterLimit.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 Join的截断值
     * <p>
     * Join为 Miter 时小角度结合点长度的截断值，默认值为 3.528。
     * 当 Join  不等于 Miter 时该参数无效。
     *
     * @return Join的截断值长度
     */
    public Double getMiterLimit() {
        String str = this.attributeValue("MiterLimit");
        if (str == null || str.trim().length() == 0) {
            return 3.528d;
        }
        return Double.parseDouble(str);
    }


    /**
     * 【可选】
     * 设置 填充颜色
     * <p>
     * 用以填充路径形成的区域以及文字轮廓内的区域，
     * 默认值为透明色。关于颜色的描述见 8.3
     *
     * @param fillColor 填充颜色
     * @return this
     */
    public CT_DrawParam setFillColor(CT_Color fillColor) {
        if (fillColor == null) {
            this.removeOFDElemByNames("FillColor");
            return this;
        }
        FillColor color = new FillColor((Element) fillColor.clone());
        color.setOFDName("FillColor");
        this.set(color);
        return this;
    }

    /**
     * 【可选】
     * 获取 填充颜色
     * <p>
     * 用以填充路径形成的区域以及文字轮廓内的区域，
     * 默认值为透明色。关于颜色的描述见 8.3
     *
     * @return 填充颜色
     */
    public FillColor getFillColor() {
        Element e = this.getOFDElement("FillColor");
        return e == null ? null : new FillColor(e);
    }


    /**
     * 【可选】
     * 设置 勾边颜色
     * <p>
     * 用以填充路径形成的区域以及文字轮廓内的区域，
     * 默认值为黑色。关于颜色的描述见 8.3
     *
     * @param strokeColor 勾边颜色
     * @return this
     */
    public CT_DrawParam setStrokeColor(CT_Color strokeColor) {
        if (strokeColor == null) {
            this.removeOFDElemByNames("StrokeColor");
            return this;
        }
        StrokeColor c = new StrokeColor((Element) strokeColor.clone());
        c.setOFDName("StrokeColor");
        this.set(c);
        return this;
    }

    /**
     * 【可选】
     * 获取 勾边颜色
     * <p>
     * 用以填充路径形成的区域以及文字轮廓内的区域，
     * 默认值为黑色。关于颜色的描述见 8.3
     *
     * @return 勾边颜色
     */
    public CT_Color getStrokeColor() {
        Element e = this.getOFDElement("StrokeColor");
        return e == null ? null : new StrokeColor(e);
    }

    @Override
    public CT_DrawParam clone() {
        return new CT_DrawParam((Element) super.clone());
    }
}
