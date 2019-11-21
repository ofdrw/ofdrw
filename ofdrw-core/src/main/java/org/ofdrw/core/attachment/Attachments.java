package org.ofdrw.core.attachment;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

import java.util.List;

/**
 * 附件列表
 * <p>
 * 附件列表文件的入口点在 7.5 文档根节点中定义。
 * 一个OFD文件可以定义多个附件，附件列表结构如图 91 所示。
 * <p>
 * 20.1 附件列表 图 91 表 72
 *
 * @author 权观宇
 * @since 2019-11-21 19:21:29
 */
public class Attachments extends OFDElement {
    public Attachments(Element proxy) {
        super(proxy);
    }

    public Attachments() {
        super("Attachments");
    }

    /**
     * 【可选】
     * 增加 附件
     *
     * @param attachment 附件
     * @return this
     */
    public Attachments addAttachment(CT_Attachment attachment) {
        if (attachment == null) {
            return this;
        }
        this.add(attachment);
        return this;
    }

    /**
     * 【可选】
     * 增加 附件列表
     *
     * @return 附件列表
     */
    public List<CT_Attachment> getAttachments() {
        return this.getOFDElements("Attachment", CT_Attachment::new);
    }
}
