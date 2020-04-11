package org.ofdrw.core;

import org.dom4j.*;
import org.dom4j.tree.DefaultElement;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 元素代理对象
 *
 * @author 权观宇
 * @since 2019-10-01 01:48:13
 */
public abstract class DefaultElementProxy implements Element {
    /**
     * 代理对象
     * <p>
     * 当从容器中获取到Element会失去类型，对于失去类型的对象统一采用代理的方式
     * 获取属性或者对象内容
     */
    protected Element proxy;

    private DefaultElementProxy() {
    }

    public DefaultElementProxy(String name) {
        this.proxy = new DefaultElement(name);
    }

    public DefaultElementProxy(QName qname) {
        this.proxy = new DefaultElement(qname);
    }

    public DefaultElementProxy(QName qname, int attributeCount) {
        this.proxy = new DefaultElement(qname, attributeCount);
    }

    public DefaultElementProxy(String name, Namespace namespace) {
        this.proxy = new DefaultElement(name, namespace);
    }


    public DefaultElementProxy(Element proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException("被代理对象(proxy)不能为空");
        }
        this.proxy = proxy;
    }

    /**
     * 获取被代理对象本身
     *
     * @return 被代理的对象
     */
    public Element getProxy() {
        return proxy;
    }

    /**
     * 设置代理对象
     *
     * @param proxy 代理对象
     */
    public void setProxy(Element proxy) {
        this.proxy = proxy;
    }


    @Override
    public QName getQName() {
        return this.proxy.getQName();
    }

    @Override

    public void setQName(QName qname) {
        this.proxy.setQName(qname);
    }

    @Override
    public Namespace getNamespace() {
        return this.proxy.getNamespace();
    }

    @Override
    public QName getQName(String qualifiedName) {
        return this.proxy.getQName(qualifiedName);
    }

    @Override
    public Namespace getNamespaceForPrefix(String prefix) {
        return this.proxy.getNamespaceForPrefix(prefix);
    }

    @Override
    public Namespace getNamespaceForURI(String uri) {
        return this.proxy.getNamespaceForURI(uri);
    }

    @Override
    public List<Namespace> getNamespacesForURI(String uri) {
        return this.proxy.getNamespacesForURI(uri);
    }

    @Override
    public String getNamespacePrefix() {
        return this.proxy.getNamespacePrefix();
    }

    @Override
    public String getNamespaceURI() {
        return this.proxy.getNamespaceURI();
    }

    /**
     * 需要继承的子类实现该方法，用于在代理对象是做类型检查
     *
     * @return 元素全名（含有前缀）
     */
    @Override
    public abstract String getQualifiedName();

    @Override
    public List<Namespace> additionalNamespaces() {
        return this.proxy.additionalNamespaces();
    }

    @Override
    public List<Namespace> declaredNamespaces() {
        return this.proxy.declaredNamespaces();
    }

    @Override
    public Element addAttribute(String name, String value) {
        return this.proxy.addAttribute(name, value);
    }

    @Override
    public Element addAttribute(QName qName, String value) {
        return this.proxy.addAttribute(qName, value);
    }

    @Override
    public Element addComment(String comment) {
        return this.proxy.addComment(comment);
    }

    @Override
    public Element addCDATA(String cdata) {
        return this.proxy.addCDATA(cdata);
    }

    @Override
    public Element addEntity(String name, String text) {
        return this.proxy.addEntity(name, text);
    }

    @Override
    public Element addNamespace(String prefix, String uri) {
        return this.proxy.addNamespace(prefix, uri);
    }

    @Override
    public Element addProcessingInstruction(String target, String text) {
        return this.proxy.addProcessingInstruction(target, text);
    }

    @Override
    public Element addProcessingInstruction(String target, Map<String, String> data) {
        return this.proxy.addProcessingInstruction(target, data);
    }

    @Override
    public Element addText(String text) {
        return this.proxy.addText(text);
    }

    @Override
    public void add(Attribute attribute) {
        this.proxy.add(attribute);
    }

    @Override
    public void add(CDATA cdata) {
        this.proxy.add(cdata);
    }

    @Override
    public void add(Entity entity) {
        this.proxy.add(entity);
    }

    @Override
    public void add(Text text) {
        this.proxy.add(text);
    }

    @Override
    public void add(Namespace namespace) {
        this.proxy.add(namespace);
    }

    @Override
    public boolean remove(Attribute attribute) {
        return this.proxy.remove(attribute);
    }

    @Override
    public boolean remove(CDATA cdata) {
        return this.proxy.remove(cdata);
    }

    @Override
    public boolean remove(Entity entity) {
        return this.proxy.remove(entity);
    }

    @Override
    public boolean remove(Namespace namespace) {
        return this.proxy.remove(namespace);
    }

    @Override
    public boolean remove(Text text) {
        return this.proxy.remove(text);
    }

    @Override
    public boolean supportsParent() {
        return this.proxy.supportsParent();
    }

    @Override
    public Element getParent() {
        return this.proxy.getParent();
    }

    @Override
    public void setParent(Element parent) {
        this.proxy.setParent(parent);
    }

    @Override
    public Document getDocument() {
        return this.proxy.getDocument();
    }

    @Override
    public void setDocument(Document document) {
        this.proxy.setDocument(document);
    }

    @Override
    public boolean isReadOnly() {
        return this.proxy.isReadOnly();
    }

    @Override
    public boolean hasContent() {
        return this.proxy.hasContent();
    }

    @Override
    public String getName() {
        return this.proxy.getName();
    }

    @Override
    public void setName(String name) {
        this.proxy.setName(name);
    }

    @Override
    public String getText() {
        return this.proxy.getText();
    }

    @Override
    public void setText(String text) {
        this.proxy.setText(text);
    }

    @Override
    public String getTextTrim() {
        return this.proxy.getTextTrim();
    }

    @Override
    public String getStringValue() {
        return this.proxy.getStringValue();
    }

    @Override
    public String getPath() {
        return this.proxy.getPath();
    }

    @Override
    public String getPath(Element context) {
        return this.proxy.getPath(context);
    }

    @Override
    public String getUniquePath() {
        return this.proxy.getUniquePath();
    }

    @Override
    public String getUniquePath(Element context) {
        return this.proxy.getUniquePath(context);
    }

    @Override
    public String asXML() {
        return this.proxy.asXML();
    }

    @Override
    public void write(Writer writer) throws IOException {
        this.proxy.write(writer);
    }

    @Override
    public short getNodeType() {
        return this.proxy.getNodeType();
    }

    @Override
    public String getNodeTypeName() {
        return this.proxy.getNodeTypeName();
    }

    @Override
    public Node detach() {
        return this.proxy.detach();
    }

    @Override
    public List<Node> selectNodes(String xpathExpression) {
        return this.proxy.selectNodes(xpathExpression);
    }

    @Override
    public Object selectObject(String xpathExpression) {
        return this.proxy.selectObject(xpathExpression);
    }

    @Override
    public List<Node> selectNodes(String xpathExpression, String comparisonXPathExpression) {
        return this.proxy.selectNodes(xpathExpression, comparisonXPathExpression);
    }

    @Override
    public List<Node> selectNodes(String xpathExpression, String comparisonXPathExpression, boolean removeDuplicates) {
        return this.proxy.selectNodes(xpathExpression, comparisonXPathExpression, removeDuplicates);
    }

    @Override
    public Node selectSingleNode(String xpathExpression) {
        return this.proxy.selectSingleNode(xpathExpression);
    }

    @Override
    public String valueOf(String xpathExpression) {
        return this.proxy.valueOf(xpathExpression);
    }

    @Override
    public Number numberValueOf(String xpathExpression) {
        return this.proxy.numberValueOf(xpathExpression);
    }

    @Override
    public boolean matches(String xpathExpression) {
        return this.proxy.matches(xpathExpression);
    }

    @Override
    public XPath createXPath(String xpathExpression) throws InvalidXPathException {
        return this.proxy.createXPath(xpathExpression);
    }

    @Override
    public Node asXPathResult(Element parent) {
        return this.proxy.asXPathResult(parent);
    }

    @Override
    public void accept(Visitor visitor) {
        this.proxy.accept(visitor);
    }

    @Override
    public Object clone() {
        return this.proxy.clone();
    }

    @Override
    public Object getData() {
        return this.proxy.getData();
    }

    @Override
    public void setData(Object data) {
        this.proxy.setData(data);
    }

    @Override
    public List<Attribute> attributes() {
        return this.proxy.attributes();
    }

    @Override
    public void setAttributes(List<Attribute> attributes) {
        this.proxy.setAttributes(attributes);
    }

    @Override
    public int attributeCount() {
        return this.proxy.attributeCount();
    }

    @Override
    public Iterator<Attribute> attributeIterator() {
        return this.proxy.attributeIterator();
    }

    @Override
    public Attribute attribute(int index) {
        return this.proxy.attribute(index);
    }

    @Override
    public Attribute attribute(String name) {
        return this.proxy.attribute(name);
    }

    @Override
    public Attribute attribute(QName qName) {
        return this.proxy.attribute(qName);
    }

    @Override
    public String attributeValue(String name) {
        return this.proxy.attributeValue(name);
    }

    @Override
    public String attributeValue(String name, String defaultValue) {
        return this.proxy.attributeValue(name, defaultValue);
    }

    @Override
    public String attributeValue(QName qName) {
        return this.proxy.attributeValue(qName);
    }

    @Override
    public String attributeValue(QName qName, String defaultValue) {
        return this.proxy.attributeValue(qName, defaultValue);
    }

    @Override
    @Deprecated
    public void setAttributeValue(String name, String value) {
        this.proxy.setAttributeValue(name, value);
    }

    @Override
    @Deprecated
    public void setAttributeValue(QName qName, String value) {
        this.proxy.setAttributeValue(qName, value);
    }

    @Override
    public Element element(String name) {
        return this.proxy.element(name);
    }

    @Override
    public Element element(QName qName) {
        return this.proxy.element(qName);
    }

    @Override
    public List<Element> elements() {
        return this.proxy.elements();
    }

    @Override
    public List<Element> elements(String name) {
        return this.proxy.elements(name);
    }

    @Override
    public List<Element> elements(QName qName) {
        return this.proxy.elements(qName);
    }

    @Override
    public Iterator<Element> elementIterator() {
        return this.proxy.elementIterator();
    }

    @Override
    public Iterator<Element> elementIterator(String name) {
        return this.proxy.elementIterator(name);
    }

    @Override
    public Iterator<Element> elementIterator(QName qName) {
        return this.proxy.elementIterator(qName);
    }

    @Override
    public boolean isRootElement() {
        return this.proxy.isRootElement();
    }

    @Override
    public boolean hasMixedContent() {
        return this.proxy.hasMixedContent();
    }

    @Override
    public boolean isTextOnly() {
        return this.proxy.isTextOnly();
    }

    @Override
    public void appendAttributes(Element element) {
        this.proxy.appendAttributes(element);
    }

    @Override
    public Element createCopy() {
        return this.proxy.createCopy();
    }

    @Override
    public Element createCopy(String name) {
        return this.proxy.createCopy(name);
    }

    @Override
    public Element createCopy(QName qName) {
        return this.proxy.createCopy(qName);
    }

    @Override
    public String elementText(String name) {
        return this.proxy.elementText(name);
    }

    @Override
    public String elementText(QName qname) {
        return this.proxy.elementText(qname);
    }

    @Override
    public String elementTextTrim(String name) {
        return this.proxy.elementTextTrim(name);
    }

    @Override
    public String elementTextTrim(QName qname) {
        return this.proxy.elementTextTrim(qname);
    }

    @Override
    public Node getXPathResult(int index) {
        return this.proxy.getXPathResult(index);
    }

    @Override
    public Node node(int index) throws IndexOutOfBoundsException {
        return this.proxy.node(index);
    }

    @Override
    public int indexOf(Node node) {
        return this.proxy.indexOf(node);
    }

    @Override
    public int nodeCount() {
        return this.proxy.nodeCount();
    }

    @Override
    public Element elementByID(String elementID) {
        return this.proxy.elementByID(elementID);
    }

    @Override
    public List<Node> content() {
        return this.proxy.content();
    }

    @Override
    public Iterator<Node> nodeIterator() {
        return this.proxy.nodeIterator();
    }

    @Override
    public void setContent(List<Node> content) {
        this.proxy.setContent(content);
    }

    @Override
    public void appendContent(Branch branch) {
        this.proxy.appendContent(branch);
    }

    @Override
    public void clearContent() {
        this.proxy.clearContent();
    }

    @Override
    public List<ProcessingInstruction> processingInstructions() {
        return this.proxy.processingInstructions();
    }

    @Override
    public List<ProcessingInstruction> processingInstructions(String target) {
        return this.proxy.processingInstructions(target);
    }

    @Override
    public ProcessingInstruction processingInstruction(String target) {
        return this.proxy.processingInstruction(target);
    }

    @Override
    public void setProcessingInstructions(List<ProcessingInstruction> listOfPIs) {
        this.proxy.setProcessingInstructions(listOfPIs);
    }

    @Override
    public Element addElement(String name) {
        return this.proxy.addElement(name);
    }

    @Override
    public Element addElement(QName qname) {
        return this.proxy.addElement(qname);
    }

    @Override
    public Element addElement(String qualifiedName, String namespaceURI) {
        return this.proxy.addElement(qualifiedName, namespaceURI);
    }

    @Override
    public boolean removeProcessingInstruction(String target) {
        return this.proxy.removeProcessingInstruction(target);
    }

    @Override
    public void add(Node node) {
        this.proxy.add(node);
    }

    @Override
    public void add(Comment comment) {
        this.proxy.add(comment);
    }

    @Override
    public void add(Element element) {
        this.proxy.add(element);
    }

    @Override
    public void add(ProcessingInstruction pi) {
        this.proxy.add(pi);
    }

    @Override
    public boolean remove(Node node) {
        return this.proxy.remove(node);
    }

    @Override
    public boolean remove(Comment comment) {
        return this.proxy.remove(comment);
    }

    @Override
    public boolean remove(Element element) {
        return this.proxy.remove(element);
    }

    @Override
    public boolean remove(ProcessingInstruction pi) {
        return this.proxy.remove(pi);
    }

    @Override
    public void normalize() {
        this.proxy.normalize();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof DefaultElementProxy) {
            o = ((DefaultElementProxy) o).proxy;
        }
        return proxy.equals(o);
    }
}
