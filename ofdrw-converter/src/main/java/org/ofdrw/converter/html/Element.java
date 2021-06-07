package org.ofdrw.converter.html;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yuanfang
 * date: 2021-05-24
 */

public class Element {

    private int _index;

    private String innerHTML;

    private Map<String, String> attribute;

    private List<Element> children;

    private String tagName;

    private String style;

    private String version;

    private String namespaceURI;

    private double x;

    private double y;


    public void setAttribute(String name, String value) {
        if (attribute == null)
            attribute = new HashMap<>();
        attribute.put(name, value);
    }

    public void appendChild(Element element) {
        if (children == null)
            children = new ArrayList<>();
        element._index = _index++;
        children.add(element);
    }

    public int get_index() {
        return _index;
    }

    public void set_index(int _index) {
        this._index = _index;
    }

    public String getInnerHTML() {
        return innerHTML;
    }

    public void setInnerHTML(String innerHTML) {
        this.innerHTML = innerHTML;
    }

    public Map<String, String> getAttribute() {
        return attribute;
    }

    public void setAttribute(Map<String, String> attribute) {
        this.attribute = attribute;
    }

    public List<Element> getChildren() {
        return children;
    }

    public void setChildren(List<Element> children) {
        this.children = children;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getNamespaceURI() {
        return namespaceURI;
    }

    public void setNamespaceURI(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        setAttribute("x", String.valueOf(x));
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        setAttribute("y", String.valueOf(y));
    }

}
