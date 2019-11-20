package org.ofdrw.core.extensions;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

import java.util.List;

/**
 * 扩展信息
 * <p>
 * 扩展信息列表的入口文件在 7.5 文档根节点中定义。
 * 扩展信息列表文件的根节点名为 Extensions，其下
 * 由 0 到多个扩展信息节点（Extension）组成，扩展
 * 信息列表的根节点结构如图 83 所示。
 *
 * <p>
 * 17 扩展信息 图 83 表 64
 *
 * @author 权观宇
 * @since 2019-11-20 05:40:27
 */
public class Extensions extends OFDElement {
    public Extensions(Element proxy) {
        super(proxy);
    }

    public Extensions() {
        super("Extensions");
    }

    /**
     * 【可选】
     * 增加 扩展信息节点
     *
     * @param extension 扩展信息节点
     * @return this
     */
    public Extensions addExtension(CT_Extension extension) {
        if (extension == null) {
            return this;
        }
        this.add(extension);
        return this;
    }

    /**
     * 【可选】
     * 获取 扩展信息节点序列
     *
     * @return 扩展信息节点
     */
    public List<CT_Extension> getExtensions() {
        return this.getOFDElements("Extension", CT_Extension::new);
    }
}
