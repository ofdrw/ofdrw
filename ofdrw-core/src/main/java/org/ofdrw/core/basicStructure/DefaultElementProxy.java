package org.ofdrw.core.basicStructure;

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


    public QName getQName() {
        return this.proxy.getQName();
    }

    public void setQName(QName qname) {
        this.proxy.setQName(qname);
    }

    public Namespace getNamespace() {
        return this.proxy.getNamespace();
    }

    public QName getQName(String qualifiedName) {
        return this.proxy.getQName(qualifiedName);
    }

    public Namespace getNamespaceForPrefix(String prefix) {
        return this.proxy.getNamespaceForPrefix(prefix);
    }

    public Namespace getNamespaceForURI(String uri) {
        return this.proxy.getNamespaceForURI(uri);
    }

    public List<Namespace> getNamespacesForURI(String uri) {
        return this.proxy.getNamespacesForURI(uri);
    }

    public String getNamespacePrefix() {
        return this.proxy.getNamespacePrefix();
    }

    public String getNamespaceURI() {
        return this.proxy.getNamespaceURI();
    }

    /**
     * 需要继承的子类实现该方法，用于在代理对象是做类型检查
     *
     * @return 元素全名（含有前缀）
     */
    public abstract String getQualifiedName();

    public List<Namespace> additionalNamespaces() {
        return this.proxy.additionalNamespaces();
    }

    public List<Namespace> declaredNamespaces() {
        return this.proxy.declaredNamespaces();
    }

    public Element addAttribute(String name, String value) {
        return this.proxy.addAttribute(name, value);
    }

    public Element addAttribute(QName qName, String value) {
        return this.proxy.addAttribute(qName, value);
    }

    public Element addComment(String comment) {
        return this.proxy.addComment(comment);
    }

    public Element addCDATA(String cdata) {
        return this.proxy.addCDATA(cdata);
    }

    public Element addEntity(String name, String text) {
        return this.proxy.addEntity(name, text);
    }

    public Element addNamespace(String prefix, String uri) {
        return this.proxy.addNamespace(prefix, uri);
    }

    public Element addProcessingInstruction(String target, String text) {
        return this.proxy.addProcessingInstruction(target, text);
    }

    public Element addProcessingInstruction(String target, Map<String, String> data) {
        return this.proxy.addProcessingInstruction(target, data);
    }

    public Element addText(String text) {
        return this.proxy.addText(text);
    }

    public void add(Attribute attribute) {
        this.proxy.add(attribute);
    }

    public void add(CDATA cdata) {
        this.proxy.add(cdata);
    }

    public void add(Entity entity) {
        this.proxy.add(entity);
    }

    public void add(Text text) {
        this.proxy.add(text);
    }

    public void add(Namespace namespace) {
        this.proxy.add(namespace);
    }

    public boolean remove(Attribute attribute) {
        return this.proxy.remove(attribute);
    }

    public boolean remove(CDATA cdata) {
        return this.proxy.remove(cdata);
    }

    public boolean remove(Entity entity) {
        return this.proxy.remove(entity);
    }

    public boolean remove(Namespace namespace) {
        return this.proxy.remove(namespace);
    }

    public boolean remove(Text text) {
        return this.proxy.remove(text);
    }

    public boolean supportsParent() {
        return this.proxy.supportsParent();
    }

    public Element getParent() {
        return this.proxy.getParent();
    }

    public void setParent(Element parent) {
        this.proxy.setParent(parent);
    }

    public Document getDocument() {
        return this.proxy.getDocument();
    }

    public void setDocument(Document document) {
        this.proxy.setDocument(document);
    }

    public boolean isReadOnly() {
        return this.proxy.isReadOnly();
    }

    public boolean hasContent() {
        return this.proxy.hasContent();
    }

    public String getName() {
        return this.proxy.getName();
    }

    public void setName(String name) {
        this.proxy.setName(name);
    }

    public String getText() {
        return this.proxy.getText();
    }

    public void setText(String text) {
        this.proxy.setText(text);
    }

    public String getTextTrim() {
        return this.proxy.getTextTrim();
    }

    public String getStringValue() {
        return this.proxy.getStringValue();
    }

    public String getPath() {
        return this.proxy.getPath();
    }

    public String getPath(Element context) {
        return this.proxy.getPath(context);
    }

    public String getUniquePath() {
        return this.proxy.getUniquePath();
    }

    public String getUniquePath(Element context) {
        return this.proxy.getUniquePath(context);
    }

    public String asXML() {
        return this.proxy.asXML();
    }

    public void write(Writer writer) throws IOException {
        this.proxy.write(writer);
    }

    public short getNodeType() {
        return this.proxy.getNodeType();
    }

    public String getNodeTypeName() {
        return this.proxy.getNodeTypeName();
    }

    public Node detach() {
        return this.proxy.detach();
    }

    public List<Node> selectNodes(String xpathExpression) {
        return this.proxy.selectNodes(xpathExpression);
    }

    public Object selectObject(String xpathExpression) {
        return this.proxy.selectObject(xpathExpression);
    }

    public List<Node> selectNodes(String xpathExpression, String comparisonXPathExpression) {
        return this.proxy.selectNodes(xpathExpression, comparisonXPathExpression);
    }

    public List<Node> selectNodes(String xpathExpression, String comparisonXPathExpression, boolean removeDuplicates) {
        return this.proxy.selectNodes(xpathExpression, comparisonXPathExpression, removeDuplicates);
    }

    public Node selectSingleNode(String xpathExpression) {
        return this.proxy.selectSingleNode(xpathExpression);
    }

    public String valueOf(String xpathExpression) {
        return this.proxy.valueOf(xpathExpression);
    }

    public Number numberValueOf(String xpathExpression) {
        return this.proxy.numberValueOf(xpathExpression);
    }

    public boolean matches(String xpathExpression) {
        return this.proxy.matches(xpathExpression);
    }

    public XPath createXPath(String xpathExpression) throws InvalidXPathException {
        return this.proxy.createXPath(xpathExpression);
    }

    public Node asXPathResult(Element parent) {
        return this.proxy.asXPathResult(parent);
    }

    public void accept(Visitor visitor) {
        this.proxy.accept(visitor);
    }

    public Object clone() {
        return this.proxy.clone();
    }

    public Object getData() {
        return this.proxy.getData();
    }

    public void setData(Object data) {
        this.proxy.setData(data);
    }

    public List<Attribute> attributes() {
        return this.proxy.attributes();
    }

    public void setAttributes(List<Attribute> attributes) {
        this.proxy.setAttributes(attributes);
    }

    public int attributeCount() {
        return this.proxy.attributeCount();
    }

    public Iterator<Attribute> attributeIterator() {
        return this.proxy.attributeIterator();
    }

    public Attribute attribute(int index) {
        return this.proxy.attribute(index);
    }

    public Attribute attribute(String name) {
        return this.proxy.attribute(name);
    }

    public Attribute attribute(QName qName) {
        return this.proxy.attribute(qName);
    }

    public String attributeValue(String name) {
        return this.proxy.attributeValue(name);
    }

    public String attributeValue(String name, String defaultValue) {
        return this.proxy.attributeValue(name, defaultValue);
    }

    public String attributeValue(QName qName) {
        return this.proxy.attributeValue(qName);
    }

    public String attributeValue(QName qName, String defaultValue) {
        return this.proxy.attributeValue(qName, defaultValue);
    }

    @Deprecated
    public void setAttributeValue(String name, String value) {
        this.proxy.setAttributeValue(name, value);
    }

    @Deprecated
    public void setAttributeValue(QName qName, String value) {
        this.proxy.setAttributeValue(qName, value);
    }

    public Element element(String name) {
        return this.proxy.element(name);
    }

    public Element element(QName qName) {
        return this.proxy.element(qName);
    }

    public List<Element> elements() {
        return this.proxy.elements();
    }

    public List<Element> elements(String name) {
        return this.proxy.elements(name);
    }

    public List<Element> elements(QName qName) {
        return this.proxy.elements(qName);
    }

    public Iterator<Element> elementIterator() {
        return this.proxy.elementIterator();
    }

    public Iterator<Element> elementIterator(String name) {
        return this.proxy.elementIterator(name);
    }

    public Iterator<Element> elementIterator(QName qName) {
        return this.proxy.elementIterator(qName);
    }

    public boolean isRootElement() {
        return this.proxy.isRootElement();
    }

    public boolean hasMixedContent() {
        return this.proxy.hasMixedContent();
    }

    public boolean isTextOnly() {
        return this.proxy.isTextOnly();
    }

    public void appendAttributes(Element element) {
        this.proxy.appendAttributes(element);
    }

    public Element createCopy() {
        return this.proxy.createCopy();
    }

    public Element createCopy(String name) {
        return this.proxy.createCopy(name);
    }

    public Element createCopy(QName qName) {
        return this.proxy.createCopy(qName);
    }

    public String elementText(String name) {
        return this.proxy.elementText(name);
    }

    public String elementText(QName qname) {
        return this.proxy.elementText(qname);
    }

    public String elementTextTrim(String name) {
        return this.proxy.elementTextTrim(name);
    }

    public String elementTextTrim(QName qname) {
        return this.proxy.elementTextTrim(qname);
    }

    public Node getXPathResult(int index) {
        return this.proxy.getXPathResult(index);
    }

    public Node node(int index) throws IndexOutOfBoundsException {
        return this.proxy.node(index);
    }

    public int indexOf(Node node) {
        return this.proxy.indexOf(node);
    }

    public int nodeCount() {
        return this.proxy.nodeCount();
    }

    public Element elementByID(String elementID) {
        return this.proxy.elementByID(elementID);
    }

    public List<Node> content() {
        return this.proxy.content();
    }

    public Iterator<Node> nodeIterator() {
        return this.proxy.nodeIterator();
    }

    public void setContent(List<Node> content) {
        this.proxy.setContent(content);
    }

    public void appendContent(Branch branch) {
        this.proxy.appendContent(branch);
    }

    public void clearContent() {
        this.proxy.clearContent();
    }

    public List<ProcessingInstruction> processingInstructions() {
        return this.proxy.processingInstructions();
    }

    public List<ProcessingInstruction> processingInstructions(String target) {
        return this.proxy.processingInstructions(target);
    }

    public ProcessingInstruction processingInstruction(String target) {
        return this.proxy.processingInstruction(target);
    }

    public void setProcessingInstructions(List<ProcessingInstruction> listOfPIs) {
        this.proxy.setProcessingInstructions(listOfPIs);
    }

    public Element addElement(String name) {
        return this.proxy.addElement(name);
    }

    public Element addElement(QName qname) {
        return this.proxy.addElement(qname);
    }

    public Element addElement(String qualifiedName, String namespaceURI) {
        return this.proxy.addElement(qualifiedName, namespaceURI);
    }

    public boolean removeProcessingInstruction(String target) {
        return this.proxy.removeProcessingInstruction(target);
    }

    public void add(Node node) {
        this.proxy.add(node);
    }

    public void add(Comment comment) {
        this.proxy.add(comment);
    }

    public void add(Element element) {
        this.proxy.add(element);
    }

    public void add(ProcessingInstruction pi) {
        this.proxy.add(pi);
    }

    public boolean remove(Node node) {
        return this.proxy.remove(node);
    }

    public boolean remove(Comment comment) {
        return this.proxy.remove(comment);
    }

    public boolean remove(Element element) {
        return this.proxy.remove(element);
    }

    public boolean remove(ProcessingInstruction pi) {
        return this.proxy.remove(pi);
    }

    public void normalize() {
        this.proxy.normalize();
    }

    @Override
    public boolean equals(Object o) {
        return proxy.equals(o);
    }
}
