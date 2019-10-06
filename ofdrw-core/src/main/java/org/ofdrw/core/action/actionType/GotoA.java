package org.ofdrw.core.action.actionType;

import org.dom4j.Element;

/**
 * 附件动作
 * <p>
 * 附件动作表明打开当前文档内的一个附件
 * <p>
 * 图 76 附件动作结构
 *
 * @author 权观宇
 * @since 2019-10-05 09:33:45
 */
public class GotoA extends ActionEntity {
    public GotoA(Element proxy) {
        super(proxy);
    }

    public GotoA() {
        super("GotoA");
    }

    public GotoA(String attachId, boolean newWindow) {
        this();
        this.setAttachID(attachId)
                .setNewWindow(newWindow);
    }

    public GotoA(String attachId) {
        this();
        this.setAttachID(attachId);
    }

    /**
     * 【必选 属性】
     * 设置 附件的标识（xs:IDREF）
     *
     * @param attachId 附件的标识（xs:IDREF）
     * @return this
     */
    public GotoA setAttachID(String attachId) {
        this.addAttribute("AttachID", attachId);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 附件的标识（xs:IDREF）
     *
     * @return 附件的标识（xs:IDREF）
     */
    public String getAttachID() {
        return this.attributeValue("AttachID");
    }

    /**
     * 【可选 属性】
     * 设置 是否在新窗口中打开
     *
     * @param newWindow true - 新窗口中打开
     * @return this
     */
    public GotoA setNewWindow(boolean newWindow) {
        this.addAttribute("NewWindow", Boolean.toString(newWindow));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 是否在新窗口中打开
     *
     * @return true - 新窗口中打开
     */
    public Boolean getNewWindow() {
        String str = this.attributeValue("NewWindow");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        return Boolean.parseBoolean(str);
    }
}
