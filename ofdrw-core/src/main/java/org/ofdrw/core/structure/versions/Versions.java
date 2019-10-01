package org.ofdrw.core.structure.versions;

import org.dom4j.Element;
import org.ofdrw.core.structure.OFDElement;

/**
 * 一个OFD文档可能有多个版本
 * <p>
 * 版本序列
 * <p>
 * 图 89 版本结构列表
 */
public class Versions extends OFDElement {

    public Versions(Element proxy) {
        super(proxy);
    }


    public Versions() {
        super("Versions");
    }

    public Versions(Version version) {
        this();
        this.add(version);
    }

    /**
     * 【必选】
     * 版本描述入口
     *
     * @param version 版本描述入口
     * @return this
     */
    public Versions addVersion(Version version) {
        this.add(version);
        return this;
    }

    @Override
    public String getQualifiedName() {
        return "ofd:Versions";
    }
}
