package org.ofdrw.core.versions;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.OFDElement;

import java.util.ArrayList;
import java.util.List;

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
     * 增加 版本描述入口
     *
     * @param version 版本描述入口
     * @return this
     */
    public Versions addVersion(Version version) {
        this.add(version);
        return this;
    }

    /**
     * 【必选】
     * 获取 版本描述入口列表
     *
     * @return 版本描述入口列表
     */
    public List<Version> getVersions() {
        List<Element> elements = this.getOFDElements("Version");
        List<Version> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(new Version(item)));
        return res;
    }

    @Override
    public String getQualifiedName() {
        return "ofd:Versions";
    }
}
