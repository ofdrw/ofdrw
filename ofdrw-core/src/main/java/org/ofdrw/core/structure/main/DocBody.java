package org.ofdrw.core.structure.main;

import org.ofdrw.core.basicType.LocST;
import org.ofdrw.core.structure.BasicObjectElement;

/**
 * 文件对象入口，可以存在多个，以便在一个文档中包含多个版式文档
 */
public class DocBody extends BasicObjectElement {

    /**
     * 文档根节点文档名称
     */
    public static final String DOC_ROOT = "DocRoot";

    public DocBody() {
        super("DocBody");
    }

    /**
     * 【必选】
     * 设置文档元数据信息描述
     *
     * @param docInfo 文档元数据信息描述
     * @return this
     */
    public DocBody setDocInfo(DocInfo docInfo) {
        this.add(docInfo);
        return this;
    }

    /**
     * 【可选】
     * 设置指向文档根节点文档
     *
     * @param locST 指向根节点文档路径
     * @return this
     */
    public DocBody setDocRoot(LocST locST) {
        this.add(new BasicObjectElement(DOC_ROOT, locST));
        return this;
    }

    /**
     * 获取指向文档根节点文档路径
     *
     * @return 指向文档根节点文档路径
     */
    public LocST getDocRoot() {
        return new LocST(this.element("ofd:DocRoot").getText());
    }

    // TODO 2019-9-29 23:23:48
}
