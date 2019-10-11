package org.ofdrw.core.graph.method;

import org.dom4j.Element;
import org.ofdrw.core.basicType.ST_Pos;

/**
 * 二次贝塞尔曲线结构
 * <p>
 * 图 52 二次贝塞尔曲线结构
 *
 * 二次贝塞尔曲线公式
 * <code>
 *     B(t) = (1 - t)^2 + 2t(1 - t)(P1) + t^2(P2)
 *     t ∈ [0,1]
 * </code>
 *
 * @author 权观宇
 * @since 2019-10-05 05:22:28
 */
public class QuadraticBezier extends Command {
    public QuadraticBezier(Element proxy) {
        super(proxy);
    }

    public QuadraticBezier() {
        super("QuadraticBezier");
    }

    public QuadraticBezier(ST_Pos point1, ST_Pos point2) {
        this();
        this.setPoint1(point1)
                .setPoint2(point2);
    }

    /**
     * 【必选 属性】
     * 设置 二次贝塞尔曲线的控制点
     *
     * @param point1 二次贝塞尔曲线的控制点
     * @return this
     */
    public QuadraticBezier setPoint1(ST_Pos point1) {
        this.addAttribute("Point1", point1.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 二次贝塞尔曲线的控制点
     *
     * @return 二次贝塞尔曲线的控制点
     */
    public ST_Pos getPoint1() {
        return ST_Pos.getInstance(this.attributeValue("Point1"));
    }

    /**
     * 【必选 属性】
     * 设置 二次贝塞尔曲线的结束点
     *
     * @param point2 二次贝塞尔曲线的结束点
     * @return this
     */
    public QuadraticBezier setPoint2(ST_Pos point2) {
        this.addAttribute("Point2", point2.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 二次贝塞尔曲线的结束点
     *
     * @return 二次贝塞尔曲线的结束点
     */
    public ST_Pos getPoint2() {
        return ST_Pos.getInstance(this.attributeValue("Point2"));
    }

}
