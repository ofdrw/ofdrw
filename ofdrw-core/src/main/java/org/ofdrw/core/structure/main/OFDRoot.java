package org.ofdrw.core.structure.main;

import org.ofdrw.core.structure.RootBasicElement;

/**
 * 主入口
 * <p>
 * OFD.xml
 * ————《GB/T 33190-2016》 图 3
 */
public class OFDRoot extends RootBasicElement {
    /**
     * 文件格式的版本号
     *
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
}
