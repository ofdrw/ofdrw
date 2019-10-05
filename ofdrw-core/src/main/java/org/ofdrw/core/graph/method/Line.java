package org.ofdrw.core.graph.method;

import org.dom4j.Element;
import org.ofdrw.core.basicType.ST_Pos;

/**
 * 线段
 * <p>
 * 图 51 线段结构
 *
 * @author 权观宇
 * @since 2019-10-05 05:14:27
 */
public class Line extends Command {
    public Line(Element proxy) {
        super(proxy);
    }

    public Line() {
        super("Line");
    }


    public Line(ST_Pos point1) {
        this();
        setPoint1(point1);
    }

    public Line(double x, double y) {
        this();
        setPoint1(new ST_Pos(x, y));
    }

    /**
     * 【必选 属性】
     * 设置 线段的结束点
     *
     * @param point1 线段的结束点
     * @return this
     */
    public Line setPoint1(ST_Pos point1) {
        this.addAttribute("Point1", point1.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 线段的结束点
     *
     * @return 线段的结束点
     */
    public ST_Pos getPoint1() {
        return ST_Pos.getInstance(this.attributeValue("Point1"));
    }
}
