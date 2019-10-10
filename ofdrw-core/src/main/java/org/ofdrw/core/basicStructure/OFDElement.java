package org.ofdrw.core.basicStructure;

import org.dom4j.Element;
import org.dom4j.QName;
import org.ofdrw.core.Const;
import org.ofdrw.core.basicType.ST_ID;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;


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
public abstract class OFDElement extends DefaultElementProxy {

    public OFDElement(Element proxy) {
        super(proxy);
        /*
         * 判断 被代理的对象和子类的对象是否为同一个类型
         */
        String subObjsQName = this.getQualifiedName();
        String proxyObjQName = this.proxy.getQualifiedName();
        if (!subObjsQName.equals(proxyObjQName)) {
            throw new IllegalArgumentException("wrong proxy type: "
                    + this.proxy.getQualifiedName()
                    + " require: " + this.getQualifiedName());
        }
    }

    public OFDElement(String name) {
        // 设置 xmlns:ofd=http://www.ofdspec.org/2016 , 并增加 ofd
        super(name, Const.OFD_NAMESPACE);
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
     * 获取OFD的元素
     *
     * @param name OFD元素名称
     * @return OFD元素或null
     */
    public Element getOFDElement(String name) {
        return this.element(new QName(name, Const.OFD_NAMESPACE));
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
     *
     * @param name OFD元素名称
     * @return 指定名称OFD元素集合
     */
    public List<Element> getOFDElements(String name) {
        return this.elements(new QName(name, Const.OFD_NAMESPACE));
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
            for (Element toBeDelete : this.getOFDElements(name)) {
                if (toBeDelete == null) {
                    continue;
                }
                this.remove(toBeDelete);
                deleteElements.add(toBeDelete);
            }
        }
        return deleteElements;
    }


//    /**
//     * 【可选】
//     * <p>
//     * 设置 OFD对象标识，无符号整数，应在文档内唯一。
//     * <p>
//     * 0标识无效标识符
//     *
//     * @param objId OFD对象标识
//     * @return this
//     */
//    public OFDElement setObjID(Long objId) {
//        this.addAttribute("ID", objId.toString());
//        return this;
//    }
//
//    /**
//     * 【可选】
//     * <p>
//     * 设置 OFD对象标识，无符号整数，应在文档内唯一。
//     * <p>
//     * 0标识无效标识符
//     *
//     * @return OFD对象标识，null表示对象标识不存在
//     */
//    public Long getObjID() {
//        String str = this.attributeValue("ID");
//        if (str == null || str.trim().length() == 0) {
//            return null;
//        }
//        return Long.parseLong(str);
//    }

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
