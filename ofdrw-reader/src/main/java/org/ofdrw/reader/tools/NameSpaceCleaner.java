package org.ofdrw.reader.tools;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.VisitorSupport;
import org.dom4j.tree.DefaultElement;

/**
 * 命名空间清理类
 * <p>
 * 用于清理已经存在的命名空间
 *
 * @author libra19911018
 * @since 2020-10-15 19:38:15
 */
public class NameSpaceCleaner extends VisitorSupport {
    public void visit(Document document) {
        ((DefaultElement) document.getRootElement()).setNamespace(Namespace.NO_NAMESPACE);
        document.getRootElement().additionalNamespaces().clear();
    }

    public void visit(Namespace namespace) {
        namespace.detach();
    }

//    public void visit(Attribute node) {
//        if (node.toString().contains("xmlns") || node.toString().contains("ofd:")) {
//            node.detach();
//        }
//    }

    public void visit(Element node) {
        if (node instanceof DefaultElement) {
            ((DefaultElement) node).setNamespace(Namespace.NO_NAMESPACE);
        }
    }
}
