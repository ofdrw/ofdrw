package org.ofdrw.core.graph.tight.method;

import org.dom4j.Element;
import org.ofdrw.core.basicType.ST_Pos;

/**
 * 移动节点
 * <p>
 * 用于表示到新的绘制点指令
 *
 * @author 权观宇
 * @since 2019-10-05 12:44:18
 */
public class Move extends Command {
    public Move(Element proxy) {
        super(proxy);
    }

    public Move() {
        super("Move");
    }

    public Move(ST_Pos point1) {
        this();
        this.setPoint1(point1);
    }

    public Move(double x, double y) {
        this();
        this.setPoint1(new ST_Pos(x, y));
    }

    /**
     * 【必选 属性】
     * 设置 移动后新的当前绘制点
     *
     * @param pint1 移动后新的当前绘制点
     * @return this
     */
    public Move setPoint1(ST_Pos pint1) {
        this.addAttribute("Point1", pint1.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 移动后新的当前绘制点
     *
     * @return 移动后新的当前绘制点
     */
    public ST_Pos getPoint1() {
        return ST_Pos.getInstance(this.attributeValue("Point1"));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append("M ");
        final ST_Pos point1 = getPoint1();
        if (point1 != null) {
            sb.append(point1.toString());
        }else{
            sb.append("0 0");
        }
        return sb.toString();
    }
}
