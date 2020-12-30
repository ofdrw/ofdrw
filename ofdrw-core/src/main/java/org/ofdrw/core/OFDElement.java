package org.ofdrw.core;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.QName;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicType.ST_ID;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


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
public class OFDElement extends DefaultElementProxy {

    public OFDElement(Element proxy) {
        super(proxy);
//        /*
//         * 判断 被代理的对象和子类的对象是否为同一个类型
//         */
//        String subObjsQName = this.getQualifiedName();
//        String proxyObjQName = this.proxy.getQualifiedName();
//        if (!subObjsQName.equals(proxyObjQName)) {
//            throw new IllegalArgumentException("wrong proxy type: "
//                    + this.proxy.getQualifiedName()
//                    + " require: " + this.getQualifiedName());
//        }
    }

    protected OFDElement(String name) {
        // 设置 xmlns:ofd=http://www.ofdspec.org/2016 , 并增加 ofd
        super(name, Const.OFD_NAMESPACE);
    }

    /**
     * @param name 元素名称
     * @return 获取OFD类型元素实例
     */
    public static OFDElement getInstance(String name) {
        return new OFDElement(name);
    }

    /**
     * 向元素中增加OFD元素
     *
     * @param name  元素名称
     * @param value 元素文本
     * @return this
     */
    public OFDElement addOFDEntity(String name, Serializable value) {
        this.add(new OFDSimpleTypeElement(name, value));
        return this;
    }

    /**
     * 设置OFD参数
     * <p>
     * 如果参数已经存在则修改参数
     * <p>
     * 如果属性值value为null，表示删除该类元素
     *
     * @param name  元素名称
     * @param value 元素文本
     * @return this
     */
    public OFDElement setOFDEntity(String name, Serializable value) {
        if (value == null) {
            this.removeOFDElemByNames(name);
            return this;
        }

        Element e = this.getOFDElement(name);
        if (e == null) {
            return addOFDEntity(name, value);
        } else {
            e.setText(value.toString());
            return this;
        }
    }

    /**
     * 设置 元素名称
     *
     * @param name 元素名称
     * @return this
     */
    public OFDElement setOFDName(String name) {
        this.setQName(new QName(name, Const.OFD_NAMESPACE));
        return this;
    }

    /**
     * 获取OFD的元素
     *
     * @param name OFD元素名称
     * @return OFD元素或null
     */
    public Element getOFDElement(String name) {
//        return this.element(new QName(name, Const.OFD_NAMESPACE));
        return this.element(new OFDCommonQName(name));
    }



    /**
     * 代理对象创建
     *
     * @param name   元素名称
     * @param mapper 代理对象构造器
     * @param <R>    元素类型
     * @return 代理对象
     */
    public <R extends OFDElement> R getOFDElement(String name, Function<? super Element, ? extends R> mapper) {
        Element e = this.getOFDElement(name);
        if (e == null) {
            return null;
        }
        return mapper.apply(e);
    }

    /**
     * 如果属性存在则删除
     *
     * @param name 属性名
     * @return true 删除成功；false 删除失败，可能是由于属性不存在
     */
    public boolean removeAttr(String name) {
        Attribute a = this.attribute(name);
        if (a != null) {
            return this.remove(a);
        }
        return false;
    }

    /**
     * 获取OFD元素中的文本
     *
     * @param name 元素名称
     * @return 文本
     */
    public String getOFDElementText(String name) {
        Element element = getOFDElement(name);
        return element == null ? null : element.getText();
    }

    /**
     * 获取 指定名称OFD元素集合
     * <p>
     * 集合将会保持原有次序
     *
     * @param name   OFD元素名称
     * @param mapper 转换对象构造器引用
     * @param <R>    指定元素对象
     * @return 指定名称OFD元素集合
     */
    public <R> List<R> getOFDElements(String name, Function<? super Element, ? extends R> mapper) {
        List<Element> elements = this.elements(new OFDCommonQName(name));
        if (elements == null || elements.isEmpty()) {
            return Collections.emptyList();
        }
        return elements.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }


    /**
     * 获取 指定名称OFD元素集合
     * <p>
     * 集合将会保持原有次序
     * qname匹配的时候不再验证namespace，兼容namespace为空的情况。
     * author daiwf
     * @param name   OFD元素名称
     * @param mapper 转换对象构造器引用
     * @param <R>    指定元素对象
     * @return 指定名称OFD元素集合
     */
    public <R> List<R> getElements(String name, Function<? super Element, ? extends R> mapper) {
        List<Element> elements = this.elements(new QName(name));
        if (elements == null || elements.isEmpty()) {
            return Collections.emptyList();
        }
        return elements.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }
    /**
     * 设置元素
     * <p>
     * 如果同类型元素已经存在，那么删除原有元素
     *
     * @param element 需要设置的元素
     * @return this
     */
    public OFDElement set(Element element) {
        if (element == null) {
            return this;
        }
        // 删除原有内容
        List<Element> old = this.elements(element.getQName());
        if (old != null && !old.isEmpty()) {
            for (Element toBeReplace : old) {
                this.remove(toBeReplace);
            }
        }
        this.add(element);
        return this;
    }

    /**
     * 根据 OFD元素的名称删除节点内所有匹配的OFD元素
     *
     * @param names 需要被删除元素名称序列
     * @return 被删除的所有OFD元素
     */
    public List<Element> removeOFDElemByNames(String... names) {
        List<Element> deleteElements = new LinkedList<>();
        if (names == null) {
            return null;
        }
        // 遍历所有需要被删除的名称
        for (String name : names) {
            if (name == null || name.trim().length() == 0) {
                continue;
            }
            // 根据名字获取指定类型的元素
            for (Element toBeDelete : this.getOFDElements(name, OFDElement::new)) {
                if (toBeDelete == null) {
                    continue;
                }
                this.remove(toBeDelete);
                deleteElements.add(toBeDelete);
            }
        }
        return deleteElements;
    }


    /**
     * 【可选】
     * <p>
     * 设置 OFD对象标识，无符号整数，应在文档内唯一。
     * <p>
     * 0标识无效标识符
     *
     * @param objId OFD对象标识
     * @return this
     */
    public OFDElement setObjID(ST_ID objId) {
        this.addAttribute("ID", objId.toString());
        return this;
    }

    public OFDElement setObjID(long objId) {
        return this.setObjID(new ST_ID(objId));
    }


    /**
     * 移除元素中所有内容
     *
     * @return this
     */
    public OFDElement removeAll() {
        this.elements().forEach(this::remove);
        return this;
    }

    /**
     * 【可选】
     * <p>
     * 设置 OFD对象标识，无符号整数，应在文档内唯一。
     * <p>
     * 0标识无效标识符
     *
     * @return OFD对象标识，null表示对象标识不存在
     */
    public ST_ID getObjID() {
        return ST_ID.getInstance(this.attributeValue("ID"));
    }


    /**
     * OFD元素采用OFD的命名空间，所以直接调用代理对象
     *
     * @return 元素全名（含有前缀）
     */
    @Override
    public String getQualifiedName() {
        return this.proxy.getQualifiedName();
    }
}
