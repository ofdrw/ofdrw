package org.ofdrw.reader.tools;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.VisitorSupport;
import org.dom4j.tree.DefaultElement;
import org.ofdrw.core.Const;

/**
 * 命名空间变更
 *
 * @author 权观宇
 * @since 2020-10-15 20:01:20
 */
public class NameSpaceModifier extends VisitorSupport {
    /**
     * 期望变更的命名空间
     */
    private Namespace expectNs;

    /**
     * 指定变更的命名空间
     *
     * @param namespace 期望变更的新的命名空间
     */
    public NameSpaceModifier(Namespace namespace) {
        if (namespace == null) {
            namespace = Const.OFD_NAMESPACE;
        }
        expectNs = namespace;
    }

    /**
     * 使用默认的命名空间变更元素的命名空间
     * <p>
     * 默认命名空间为: xmlns:ofd="http://www.ofdspec.org/2016
     */
    public NameSpaceModifier() {
        this(Const.OFD_NAMESPACE);
    }

    /**
     * 根节点遍历
     * @param document 根节点对象
     */
    public void visit(Document document) {
        ((DefaultElement) document.getRootElement()).setNamespace(this.expectNs);
        document.getRootElement().additionalNamespaces().clear();
    }

    /**
     * 命名空间遍历
     * @param namespace 命名空间
     */
    public void visit(Namespace namespace) {
        // 删除命名空间
        namespace.detach();
    }


//    public void visit(Attribute node) {
//        if (node.toString().contains("xmlns") || node.toString().contains("ofd:")) {
//            node.detach();
//        }
//    }

    /**
     * 根节点下的子节点遍历
     * @param node 子节点
     */
    public void visit(Element node) {
        if (node instanceof DefaultElement) {
            ((DefaultElement) node).setNamespace(this.expectNs);
        }
    }

    /**
     * 设置期望变更到的命名空间
     * @param expectNs 命名空间
     * @return this
     */
    public NameSpaceModifier setExpectNs(Namespace expectNs) {
        this.expectNs = expectNs;
        return this;
    }

    public Namespace getExpectNs() {
        return expectNs;
    }
}
