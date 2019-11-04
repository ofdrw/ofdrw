package org.ofdrw.core.action;

import org.dom4j.Element;
import org.ofdrw.core.action.actionType.OFDAction;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.graph.tight.CT_Region;

import java.util.List;

/**
 * 动作类型结构
 * <p>
 * 图 73 动作类型结构
 *
 * @author 权观宇
 * @since 2019-10-05 11:34:07
 */
public class CT_Action extends OFDElement {
    public CT_Action(Element proxy) {
        super(proxy);
    }

    public CT_Action() {
        super("Action");
    }

    public CT_Action(EventType type, OFDAction action) {
        this();
        this.setEvent(type)
                .setAction(action);
    }

    /**
     * 【必选 属性】
     * 设置 事件类型
     * <p>
     * 触发动作的条件，事件的具体类型见表 52 {@link EventType}
     *
     * @param event 事件类型
     * @return this
     */
    public CT_Action setEvent(EventType event) {
        this.addAttribute("Event", event.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 事件类型
     * <p>
     * 触发动作的条件，事件的具体类型见表 52  {@link EventType}
     *
     * @return 事件类型
     */
    public EventType getEvent() {
        return EventType.getInstance(this.attributeValue("Event"));
    }

    /**
     * 【可选】
     * 设置 多个复杂区域为该链接对象的启动区域
     * <p>
     * 该参数不出现时以所在图元或页面的外接矩形作为启动区域，见 9.3
     *
     * @param region 多个复杂区域为该链接对象的启动区域
     * @return this
     */
    public CT_Action setRegion(CT_Region region) {
        this.set(region);
        return this;
    }

    /**
     * 【可选】
     * 获取 多个复杂区域为该链接对象的启动区域
     * <p>
     * 该参数不出现时以所在图元或页面的外接矩形作为启动区域，见 9.3
     *
     * @return 多个复杂区域为该链接对象的启动区域 或 null
     */
    public CT_Region getRegion() {
        Element e = this.getOFDElement("Region");
        return e == null ? null : new CT_Region(e);
    }

    /**
     * 【必选】
     * 设置 动作
     *
     * @param action 动作
     * @return this
     */
    public CT_Action setAction(OFDAction action) {
        this.removeOFDElemByNames("URI", "Sound", "GotoA", "Movie", "Goto");
        this.add(action);
        return this;
    }


    /**
     * 【必选】
     * 获取 动作
     * <p>
     * 可通过 instanceof 判断动作的具体类型
     *
     * @return 动作实体
     */
    public OFDAction getAction() {
        List<Element> elements = this.elements();
        if (elements == null || elements.isEmpty()) {
            throw new IllegalArgumentException("错误的Action结构，没有任何子节点");
        }
        Element e = elements.get(0);
        return OFDAction.getInstance(e);
    }
}
