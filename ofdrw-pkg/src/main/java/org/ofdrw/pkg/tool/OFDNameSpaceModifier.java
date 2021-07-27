package org.ofdrw.pkg.tool;

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
public class OFDNameSpaceModifier extends VisitorSupport {
    /**
     * 期望变更的命名空间
     */
    private Namespace expectNs;

    /**
     * 指定变更的命名空间
     *
     * @param namespace 期望变更的新的命名空间
     */
    public OFDNameSpaceModifier(Namespace namespace) {
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
    public OFDNameSpaceModifier() {
        this(Const.OFD_NAMESPACE);
    }

    /**
     * 根节点遍历
     *
     * @param document 根节点对象
     */
    public void visit(Document document) {
        final DefaultElement rootElement = (DefaultElement) document.getRootElement();
        if (rootElement == null) {
            return;
        }
        // 如果命名空间不同，那么更新命名空间
        if(!nsEqual(rootElement)){
            rootElement.setNamespace(this.expectNs);
            rootElement.additionalNamespaces().clear();
        }
    }

    /**
     * 命名空间遍历
     *
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
     *
     * @param node 子节点
     */
    public void visit(Element node) {
        if (node instanceof DefaultElement) {
            final DefaultElement element = (DefaultElement) node;
            if (!nsEqual(element)){
                element.setNamespace(this.expectNs);
            }
        }
    }

    public Namespace getExpectNs() {
        return expectNs;
    }

    /**
     * 命名空间是否一致
     *
     * @param e 需要比较的元素
     * @return true - 一致；false - 不一致
     */
    private boolean nsEqual(Element e) {
        final Namespace n = e.getNamespace();
        return expectNs.getPrefix().equals(n.getPrefix()) && expectNs.getText().equals(n.getText());
    }
}
