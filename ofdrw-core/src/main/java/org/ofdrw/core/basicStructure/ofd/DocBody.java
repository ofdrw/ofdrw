package org.ofdrw.core.basicStructure.ofd;

import org.dom4j.Element;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicStructure.OFDElement;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.versions.Versions;

/**
 * 文件对象入口，可以存在多个，以便在一个文档中包含多个版式文档
 */
public class DocBody extends OFDElement {

    /**
     * 文档根节点文档名称
     */
    public static final String DOC_ROOT = "DocRoot";

    public DocBody(Element proxy) {
        super(proxy);
    }

    public DocBody() {
        super("DocBody");
    }


    /**
     * 【必选】
     * 设置文档元数据信息描述
     *
     * @param CTDocInfo 文档元数据信息描述
     * @return this
     */
    public DocBody setDocInfo(CT_DocInfo CTDocInfo) {
        this.add(CTDocInfo);
        return this;
    }

    /**
     * 【必选】
     * 获取文档元数据信息描述
     *
     * @return 文档元数据信息描述 或null
     */
    public CT_DocInfo getDocInfo() {
        Element element = this.getOFDElement("DocInfo");
        return element == null ? null : new CT_DocInfo(element);
    }

    /**
     * 【可选】
     * 设置指向文档根节点文档
     *
     * @param docRoot 指向根节点文档路径
     * @return this
     */
    public DocBody setDocRoot(ST_Loc docRoot) {
        this.add(docRoot.getElement(DOC_ROOT));
        return this;
    }

    /**
     * 【可选】
     * 获取指向文档根节点文档路径
     *
     * @return 指向文档根节点文档路径
     */
    public ST_Loc getDocRoot() {
        String locStr = this.getOFDElementText("DocRoot");
        if (locStr == null || locStr.trim().length() == 0) {
            return null;
        }
        return new ST_Loc(locStr);
    }

    /**
     * 【可选】
     * 获取 包含多个版本描述节点，用于定义文件因注释和其他改动产生的版本信息，见第19章
     *
     * @param versions 版本序列
     * @return this
     */
    public DocBody setVersions(Versions versions) {
        this.add(versions);
        return this;
    }

    /**
     * 【可选】
     * 获取 包含多个版本描述序列
     *
     * @return 包含多个版本描述序列
     */
    public Versions getVersions() {
        Element element = this.getOFDElement("Versions");
        return element == null ? null : new Versions(element);
    }

    /**
     * 【可选】
     * 设置 指向该文档中签名和签章结构的路径 （见18章）
     *
     * @param signatures 指向该文档中签名和签章结构的路径
     * @return this
     */
    public DocBody setSignatures(ST_Loc signatures) {
        this.addOFDEntity("Signatures", signatures.toString());
        return this;
    }

    /**
     * 【可选】
     * 获取 指向该文档中签名和签章结构的路径
     *
     * @return 指向该文档中签名和签章结构的路径
     */
    public ST_Loc getSignatures() {
        String locStr = this.getOFDElementText("Signatures");
        if (locStr == null || locStr.trim().length() == 0) {
            return null;
        }
        return new ST_Loc(locStr);
    }


    public String getQualifiedName() {
        return "ofd:DocBody";
    }
}
