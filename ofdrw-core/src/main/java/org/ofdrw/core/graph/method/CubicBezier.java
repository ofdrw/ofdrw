package org.ofdrw.core.graph.method;

import org.dom4j.Element;
import org.ofdrw.core.basicType.ST_Pos;

/**
 * 三次贝塞尔曲线
 * <p>
 * 图 53 三次贝塞尔曲线结构
 * <p>
 * 三次贝塞尔曲线公式
 * <code>
 *    B(t) = (1-t)^3(P0) + 3t(1-t)^2(P1) + 3t^2(1-t)(P2) + t^3(P3) t∈[0,1]
 * </code>
 *
 * @author 权观宇
 * @since 2019-10-05 05:27:50
 */
public class CubicBezier extends Command {
    public CubicBezier(Element proxy) {
        super(proxy);
    }

    public CubicBezier() {
        super("CubicBezier");
    }

    public CubicBezier(ST_Pos point1, ST_Pos point2, ST_Pos point3) {
        this();
        this.setPoint1(point1)
                .setPoint2(point2)
                .setPoint3(point3);
    }

    /**
     * 【必选 属性】
     * 设置 三次贝塞尔曲线的第一个控制点
     *
     * @param point1 三次贝塞尔曲线的第一个控制点
     * @return this
     */
    public CubicBezier setPoint1(ST_Pos point1) {
        this.addAttribute("Point1", point1.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 三次贝塞尔曲线的第以个控制点
     *
     * @return 三次贝塞尔曲线的第一个控制点
     */
    public ST_Pos getPoint1() {
        return ST_Pos.getInstance(this.attributeValue("Point1"));
    }

    /**
     * 【必选 属性】
     * 设置 三次贝塞尔曲线的第二个控制点
     *
     * @param point2 三次贝塞尔曲线的第二个控制点
     * @return this
     */
    public CubicBezier setPoint2(ST_Pos point2) {
        this.addAttribute("Point2", point2.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 三次贝塞尔曲线的第二个控制点
     *
     * @return 三次贝塞尔曲线的第二个控制点
     */
    public ST_Pos getPoint2() {
        return ST_Pos.getInstance(this.attributeValue("Point2"));
    }

    /**
     * 【必选 属性】
     * 设置 三次贝塞尔曲线的结束点，下一路径的起始点
     *
     * @param point3 三次贝塞尔曲线的结束点，下一路径的起始点
     * @return this
     */
    public CubicBezier setPoint3(ST_Pos point3) {
        this.addAttribute("Point3", point3.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 三次贝塞尔曲线的结束点，下一路径的起始点
     *
     * @return 三次贝塞尔曲线的结束点，下一路径的起始点
     */
    public ST_Pos getPoint3() {
        return ST_Pos.getInstance(this.attributeValue("Point3"));
    }
}
