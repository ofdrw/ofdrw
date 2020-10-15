package org.ofdrw.reader;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.VisitorSupport;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;

public class NameSpaceCreater extends VisitorSupport {
  Namespace fmt_ns = new Namespace("ofd", "http://www.ofdspec.org/2016");
	public void visit(Document document) {
		((DefaultElement) document.getRootElement()).setNamespace(fmt_ns);
		document.getRootElement().additionalNamespaces().clear();
	}
	public void visit(Namespace namespace) {
		namespace.detach();
	}
	public void visit(Attribute node) {
		if (node.toString().contains("xmlns") || node.toString().contains("ofd:")) {
			node.detach();
		}
	}
	public void visit(Element node) {
		if (node instanceof DefaultElement) {
			((DefaultElement) node).setNamespace(fmt_ns);
		}
	}
}
