package org.ofdrw.core.action;

/**
 * 事件类型
 * <p>
 * 参照 52 事件类型
 *
 * @author 权观宇
 * @since 2019-10-05 11:38:04
 */
public enum EventType {
    /**
     * 文档打开
     */
    DO,
    /**
     * 页面打开
     */
    PO,
    /**
     * 单击区域
     */
    CLICK;

    /**
     * 根据字符串获取匹配类型实例
     *
     * @param event 事件名称，只能是 DO，PO，CLICK
     * @return 实例
     * @throws IllegalArgumentException 未知类型事件
     */
    public static EventType getInstance(String event) {
        switch (event) {
            case "DO":
                return DO;
            case "PO":
                return PO;
            case "CLICK":
                return CLICK;
            default:
                throw new IllegalArgumentException("未知类型事件： " + event);
        }
    }
}
