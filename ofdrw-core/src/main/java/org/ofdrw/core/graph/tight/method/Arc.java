package org.ofdrw.core.graph.tight.method;

import org.dom4j.Element;
import org.ofdrw.core.basicType.STBase;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Pos;

/**
 * 圆弧
 * <p>
 * 图 56圆弧的结构
 *
 * @author 权观宇
 * @since 2019-10-05 05:38:53
 */
public class Arc extends Command {
    public Arc(Element proxy) {
        super(proxy);
    }

    public Arc() {
        super("Arc");
    }

    /**
     * 【必选 属性】
     * 设置 弧线方向是否顺时针
     * <p>
     * true 表示由圆弧起始点到结束点是顺时针，false 表示由圆弧起始点到结束点是逆时针
     * <p>
     * 对于经过坐标系上指定两点，给定旋转角度和长短轴长度的椭圆，满足条件的可能有 2 个，
     * 对应的圆弧有 4 条，通过 LargeArc 属性可以排除 2 条，次属性从剩余的 2 条圆弧中确定
     * 一条
     *
     * @param sweepDirection true - 由圆弧起始点到结束点是顺时针;false - 由圆弧起始点到结束点是逆时针
     * @return this
     */
    public Arc setSweepDirection(boolean sweepDirection) {
        this.addAttribute("SweepDirection", Boolean.toString(sweepDirection));
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 弧线方向是否顺时针
     * <p>
     * true 表示由圆弧起始点到结束点是顺时针，false 表示由圆弧起始点到结束点是逆时针
     * <p>
     * 对于经过坐标系上指定两点，给定旋转角度和长短轴长度的椭圆，满足条件的可能有 2 个，
     * 对应的圆弧有 4 条，通过 LargeArc 属性可以排除 2 条，次属性从剩余的 2 条圆弧中确定
     * 一条
     *
     * @return true - 由圆弧起始点到结束点是顺时针;false - 由圆弧起始点到结束点是逆时针
     */
    public Boolean getSweepDirection() {
        return Boolean.parseBoolean(this.attributeValue("SweepDirection"));
    }

    /**
     * 【必选 属性】
     * 设置 是否是大圆弧
     * <p>
     * true 表示此线型对应的位角度大于 180°的弧，false 表示对应度数小于 180°的弧
     * <p>
     * 对于一个给定长、短轴的椭圆以及起始点和结束点，有一大一小两条圆弧，
     * 如果所描述线型恰好为 180°的弧，此属性的值不被参考，可由 SweepDirection 属性确定圆弧形状
     *
     * @param largeArc true - 此线型对应的位角度大于 180°的弧；false - 对应度数小于 180°的弧
     * @return this
     */
    public Arc setLargeArc(boolean largeArc) {
        this.addAttribute("LargeArc", Boolean.toString(largeArc));
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 是否是大圆弧
     * <p>
     * true 表示此线型对应的位角度大于 180°的弧，false 表示对应度数小于 180°的弧
     * <p>
     * 对于一个给定长、短轴的椭圆以及起始点和结束点，有一大一小两条圆弧，
     * 如果所描述线型恰好为 180°的弧，此属性的值不被参考，可由 SweepDirection 属性确定圆弧形状
     *
     * @return true - 此线型对应的位角度大于 180°的弧；false - 对应度数小于 180°的弧
     */
    public Boolean getLargeArc() {
        return Boolean.parseBoolean(this.attributeValue("LargeArc"));
    }


    /**
     * 【必选 属性】
     * 设置 按 EllipseSize 绘制的椭圆在当前坐标系下旋转的角度，
     * 正值为顺时针，负值为逆时针
     * <p>
     * [异常处理] 如果角度大于 360°，则以 360°取模
     *
     * @param rotationAngle 绘制的椭圆在当前坐标系下旋转的角度，正值为顺时针，负值为逆时针
     * @return this
     */
    public Arc setRotationAngle(double rotationAngle) {
        rotationAngle %= 360.0d;
        this.addAttribute("RotationAngle", STBase.fmt(rotationAngle));
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 按 EllipseSize 绘制的椭圆在当前坐标系下旋转的角度，
     * 正值为顺时针，负值为逆时针
     * <p>
     * [异常处理] 如果角度大于 360°，则以 360°取模
     *
     * @return 绘制的椭圆在当前坐标系下旋转的角度，正值为顺时针，负值为逆时针
     */
    public Double getRotationAngle() {
        double res = Double.parseDouble(this.attributeValue("RotationAngle"));
        return res % 360.0d;
    }

    /**
     * 【必选 属性】
     * 设置 长短轴
     * <p>
     * 形如[200 100]的数组，2个浮点数值一次对应椭圆的长、短轴长度，较大的一个为长轴
     * <p>
     * [异常处理]如果数组长度超过 2，则只取前两个数值
     * <p>
     * [异常处理]如果数组长度为 1，则认为这是一个园，该数值为圆的半径
     * <p>
     * [异常处理]如果数组前两个数值中有一个为 0，或者数组为空，则圆弧退化为一条从当前点
     * 到 EndPoint的线段
     * <p>
     * [异常处理]
     *
     * @param ellipseSize 形如[200 100]的数组，2个浮点数值一次对应椭圆的长、短轴长度，较大的一个为长轴
     * @return this
     */
    public Arc setEllipseSize(ST_Array ellipseSize) {
        if (ellipseSize == null) {
            throw new IllegalArgumentException("ellipseSize 不能为null");
        }
        this.addAttribute("EllipseSize", ellipseSize.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 设置 长短轴
     * <p>
     * 形如[200 100]的数组，2个浮点数值一次对应椭圆的长、短轴长度，较大的一个为长轴
     * <p>
     * [异常处理]如果数组长度超过 2，则只取前两个数值
     * <p>
     * [异常处理]如果数组长度为 1，则认为这是一个园，该数值为圆的半径
     * <p>
     * [异常处理]如果数组前两个数值中有一个为 0，或者数组为空，则圆弧退化为一条从当前点
     * 到 EndPoint的线段
     * <p>
     * [异常处理]
     *
     * @param sizes 长短轴参数
     * @return this
     */
    public Arc setEllipseSize(double... sizes) {
        return setEllipseSize(new ST_Array(sizes));
    }


    /**
     * 【必选 属性】
     * 获取 长短轴
     * <p>
     * 形如[200 100]的数组，2个浮点数值一次对应椭圆的长、短轴长度，较大的一个为长轴
     * <p>
     * [异常处理]如果数组长度超过 2，则只取前两个数值
     * <p>
     * [异常处理]如果数组长度为 1，则认为这是一个园，该数值为圆的半径
     * <p>
     * [异常处理]如果数组前两个数值中有一个为 0，或者数组为空，则圆弧退化为一条从当前点
     * 到 EndPoint的线段
     * <p>
     * [异常处理]
     *
     * @return 形如[200 100]的数组，2个浮点数值一次对应椭圆的长、短轴长度，较大的一个为长轴
     */
    public ST_Array getEllipseSize() {
        return ST_Array.getInstance(this.attributeValue("EllipseSize"));
    }

    /**
     * 【必选 属性】
     * 设置 圆弧结束点，下一个路径起点
     * <p>
     * 不能与当前的绘制点为同一位置
     *
     * @param endPoint 圆弧结束点，下一个路径起点
     * @return this
     */
    public Arc setEndPoint(ST_Pos endPoint) {
        this.addAttribute("EndPoint", endPoint.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 设置 圆弧结束点，下一个路径起点
     * <p>
     * 不能与当前的绘制点为同一位置
     *
     * @param x X坐标
     * @param y Y坐标
     * @return this
     */
    public Arc setEndPoint(double x, double y) {
        return setEndPoint(new ST_Pos(x, y));
    }

    /**
     * 【必选 属性】
     * 设置 圆弧结束点，下一个路径起点
     * <p>
     * 不能与当前的绘制点为同一位置
     *
     * @return 圆弧结束点，下一个路径起点
     */
    public ST_Pos getEndPoint() {
        return ST_Pos.getInstance(this.attributeValue("EndPoint"));
    }
}
