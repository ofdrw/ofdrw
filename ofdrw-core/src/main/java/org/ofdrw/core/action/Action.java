package org.ofdrw.core.action;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.OFDElement;

/**
 * 动作类型结构
 * <p>
 * 图 73 动作类型结构
 *
 * @author 权观宇
 * @since 2019-10-05 11:34:07
 */
public class Action extends OFDElement {
    public Action(Element proxy) {
        super(proxy);
    }

    public Action() {
        super("Action");
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
    public Action setEvent(EventType event) {
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
        return EventType.getInstance( this.attributeValue("Event"));
    }

    // TODO 2019-10-5 11:44:41 Region
}
