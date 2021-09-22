package org.ofdrw.core.graph.tight;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.graph.tight.method.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 区域由一系列的分路径（Area）组成，每个路径都是闭合的
 * <p>
 * 图 49 区域结构
 *
 * @author 权观宇
 * @since 2019-10-05 11:51:23
 */
public class CT_Area extends OFDElement {
    public CT_Area(Element proxy) {
        super(proxy);
    }

    public CT_Area() {
        super("Area");
    }

    /**
     * 【必选 属性】
     * 设置 定义字图形的起始点坐标
     *
     * @param start 定义字图形的起始点坐标
     * @return this
     */
    public CT_Area setStart(ST_Pos start) {
        this.addAttribute("Start", start.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 定义字图形的起始点坐标
     *
     * @return 定义字图形的起始点坐标
     */
    public ST_Pos getStart() {
        return ST_Pos.getInstance(this.attributeValue("Start"));
    }

    /**
     * 【必选】
     * 增加 绘制指令
     * <p>
     * 移动点、画线、画圆弧等
     *
     * @param command 绘制指令
     * @return this
     */
    public CT_Area addCommand(Command command) {
        this.add(command);
        return this;
    }

    /**
     * 连续绘制
     *
     * @param command 绘制指令
     * @return this
     */
    public CT_Area next(Command command){
        return addCommand(command);
    }

    /**
     * 【必选】
     * 获取 绘制指令序列（顺序决定了绘制图形）
     * <p>
     * 移动点、画线、画圆弧等
     *
     * @return 绘制指令序列
     */
    public List<Command> getCommands() {
        List<Element> elementList = this.elements();
        List<Command> res = new ArrayList<>(elementList.size());
        elementList.forEach(item -> {
            // 获取全名
            String qName = item.getQualifiedName();
            switch (qName) {
                case "ofd:Move":
                case "Move":
                    elementList.add(new Move(item));
                    break;
                case "ofd:Line":
                case "Line":
                    elementList.add(new Line(item));
                    break;
                case "ofd:QuadraticBezier":
                case "QuadraticBezier":
                    elementList.add(new QuadraticBezier(item));
                    break;
                case "ofd:CubicBezier":
                case "CubicBezier":
                    elementList.add(new CubicBezier(item));
                    break;
                case "ofd:Arc":
                case "Arc":
                    elementList.add(new Arc(item));
                    break;
                case "ofd:Close":
                case "Close":
                    elementList.add(new Close(item));
                    break;
                default:
                    throw new IllegalArgumentException("未知类型：" + qName);
            }
        });
        return res;
    }
}
