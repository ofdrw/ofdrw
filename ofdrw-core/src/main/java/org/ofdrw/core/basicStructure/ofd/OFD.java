package org.ofdrw.core.basicStructure.ofd;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

import java.util.List;

/**
 * 主入口
 * <p>
 * OFD.xml
 * ————《GB/T 33190-2016》 图 3
 */
public class OFD extends OFDElement {
    /**
     * 【必选】
     * 文件格式的版本号
     * <p>
     * 固定值： 1.2
     * <p>
     * 参照表 3
     */
    public static final String VERSION = "1.2";

    /**
     * 【必选】
     * 文件格式子集类型，取值为"OFD"，表明此文件符合本标准。
     */
    public static final String DOC_TYPE = "OFD";

    public OFD(Element proxy) {
        super(proxy);

    }

    public OFD() {
        super("OFD");
        this.addAttribute("Version", VERSION);
        this.addAttribute("DocType", DOC_TYPE);
    }


    /**
     * 文件对象入口列表创建文档对象
     *
     * @param docBodies 文件对象入口序列
     */
    public OFD(List<DocBody> docBodies) {
        this();
        for (DocBody item : docBodies) {
            if (item != null) {
                this.add(item);
            }
        }
    }


    /**
     * 文件对象入口创建文档对象
     *
     * @param docBody 文件对象入口
     */
    public OFD(DocBody docBody) {
        this();
        this.add(docBody);
    }

    /**
     * 【必选 属性】文件格式版本号
     *
     * @return 文件格式版本号
     */
    public String getVersion() {
        return this.attributeValue("Version");
    }

    /**
     * 【必选 属性】设置 文件版本号
     *
     * @param version 版本好
     * @return this
     */
    public OFD setVersion(String version) {
        this.addAttribute("Version", version);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 文件格式子集类型
     * <p>
     * 返回文档中 DocType 属性的实际值：
     * <ul>
     *   <li>标准 OFD 文件返回 "OFD"</li>
     *   <li>符合 GB/T 42133-2022 的长期保存文件返回 "OFD-A"</li>
     *   <li>属性缺失时返回 null</li>
     * </ul>
     *
     * @return DocType 属性值，缺省时为 null
     */
    public String getDocType() {
        return this.attributeValue("DocType");
    }

    /**
     * 【必选 属性】
     * 设置 文件格式子集类型
     * <p>
     * 标准 OFD 文件取值为 "OFD"（默认），
     * 符合 GB/T 42133-2022 的长期保存文件取值为 "OFD-A"。
     *
     * @param docType 文件格式子集类型，不能为空
     * @return this
     * @throws IllegalArgumentException docType 为 null 或空字符串
     */
    public OFD setDocType(String docType) {
        if (docType == null || docType.isEmpty()) {
            throw new IllegalArgumentException("DocType 不能为空");
        }
        this.addAttribute("DocType", docType);
        return this;
    }

    /**
     * 【必选】增加文件对象入口。
     * 文件对象入口，可以存在多个，以便在一个文档中包含多个版式文档
     *
     * @param docBody 文件对象入口
     * @return this
     */
    public OFD addDocBody(DocBody docBody) {
        this.add(docBody);
        return this;
    }

    /**
     * 【必选】 获取第一个文档入口
     *
     * @return 文件对象入口（如果有多个则获取第一个）
     */
    public DocBody getDocBody() {
        Element e = this.getOFDElement("DocBody");
        return e == null ? null : new DocBody(e);
    }

    /**
     * 获取指定序号的文档
     *
     * @param num 文档序号，从0起
     * @return 文件对象入口（如果有多个则获取第一个）
     */
    public DocBody getDocBody(int num) {
        return getDocBodies().get(num);
    }


    /**
     * 获取所有文档入口
     *
     * @return 所有文档入口
     */
    public List<DocBody> getDocBodies() {
        return this.getOFDElements("DocBody", DocBody::new);
    }

    @Override
    public String getQualifiedName() {
        return "ofd:OFD";
    }
}
