package org.ofdrw.core.structure;

import org.dom4j.tree.DefaultElement;
import org.ofdrw.core.Const;


/**
 * 文件根节点
 * <p>
 * XML文档使用的命名空间为 http://www.ofdspec.org/2016，其表示符应为 ofd；
 * 应在包内各XML文档的根节点申明 defaults:ofd。
 * 元素节点应使用命名空间标识符，元素属性不使用命名空间标识符。
 * ————《GB/T 33190-2016》 7.1 命名空间
 *
 * @author 权观宇
 * @since 2019-09-28 12:05:55
 */
public class RootBasicElement extends DefaultElement {

    public RootBasicElement(String name) {
        // 设置 xmlns:ofd=http://www.ofdspec.org/2016 , 并增加 ofd
        super(name, Const.OFD_NAMESPACE);
    }
}
