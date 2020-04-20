package org.ofdrw.core.signatures.range;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Loc;

import java.util.List;

/**
 * 签名的范围
 * <p>
 * 18.2.2 签名的范围 图 87 表 68
 *
 * @author 权观宇
 * @since 2019-11-21 17:57:19
 */
public class References extends OFDElement {
    public References(Element proxy) {
        super(proxy);
    }

    public References() {
        super("References");
    }

    /**
     * 【可选 属性】
     * 设置 摘要方法
     * <p>
     * 视应用场景的不同使用不同的摘要方法。
     * 用于各行业应用时，应使用符合行业安全贵方的算法。
     *
     * @param checkMethod 摘要方法
     * @return this
     */
    public References setCheckMethod(String checkMethod) {
        if (checkMethod == null || checkMethod.trim().length() == 0) {
            this.removeAttr("CheckMethod");
            return this;
        }
        this.addAttribute("CheckMethod", checkMethod);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 摘要方法
     * <p>
     * 视应用场景的不同使用不同的摘要方法。
     * 用于各行业应用时，应使用符合行业安全贵方的算法。
     *
     * @return 摘要方法
     */
    public String getCheckMethod() {
        return this.attributeValue("CheckMethod");
    }

    /**
     * 【必选】
     * 增加 针对一个文件的摘要节点
     *
     * @param reference 针对一个文件的摘要节点
     * @return this
     */
    public References addReference(Reference reference) {
        if (reference == null) {
            return this;
        }
        this.add(reference);
        return this;
    }

    /**
     * 检查是否包含文件
     *
     * @param absLoc 文件绝对路径
     * @return true - 含有文件；false - 不含
     */
    public boolean hasFile(String absLoc) {
        if (absLoc == null || absLoc.trim().isEmpty()) {
            return false;
        }
        for (Reference refItem : getReferences()) {
            if (absLoc.equals(refItem.getFileRef().getLoc())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 【必选】
     * 获取 针对一个文件的摘要节点列表
     *
     * @return 针对一个文件的摘要节点列表
     */
    public List<Reference> getReferences() {
        return this.getOFDElements("Reference", Reference::new);
    }
}
