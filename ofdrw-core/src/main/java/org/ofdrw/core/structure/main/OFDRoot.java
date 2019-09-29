package org.ofdrw.core.structure.main;

import org.ofdrw.core.structure.RootBasicElement;

import java.util.List;

/**
 * 主入口
 * <p>
 * OFD.xml
 * ————《GB/T 33190-2016》 图 3
 */
public class OFDRoot extends RootBasicElement {
    /**
     * 文件格式的版本号
     * <p>
     * 固定值： 1.0
     * <p>
     * 参照表 3
     */
    public static final String VERSION = "1.0";

    /**
     * 文件格式子集类型，取值为“OFD”，表明此文件符合本标准。
     */
    public static final String DOC_TYPE = "OFD";

    public OFDRoot() {
        super("OFD");
    }


    /**
     * 文件对象入口列表创建文档对象
     *
     * @param docBodies 文件对象入口序列
     */
    public OFDRoot(List<DocBody> docBodies) {
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
    public OFDRoot(DocBody docBody) {
        this();
        this.add(docBody);
    }

    /**
     * 【必选】文件格式版本号，取值为“1.0”
     *
     * @return 文件格式版本号
     */
    public String getVersion() {
        return VERSION;
    }

    /**
     * 【必选】文件格式子集类型，取值为“OFD”，表明此文件符合本标准。
     *
     * @return OFD
     */
    public String getDocType() {
        return DOC_TYPE;
    }

    /**
     * 【必选】增加文件对象入口。
     * 文件对象入口，可以存在多个，以便在一个文档中包含多个版式文档
     *
     * @param docBody 文件对象入口
     * @return this
     */
    public OFDRoot addDocBody(DocBody docBody) {
        this.add(docBody);
        return this;
    }
}
