package org.ofdrw.core.signatures;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.doc.CT_CommonData;

import java.util.List;

/**
 * 签名列表根节点
 * <p>
 * 签名列表问价你的入口点在 7.4 主入口中定义。
 * 签名列表文件中可以包含多个签名（例如联合发文等情况），见图 85。
 * 当允许下次继续添加签名时，该文件不会被包含到本次签名的
 * 保护文件列表（References）中。
 * <p>
 * 18.1 签名列表 图 85 表 66
 *
 * @author 权观宇
 * @since 2019-11-20 06:45:45
 */
public class Signatures extends OFDElement {
    public Signatures(Element proxy) {
        super(proxy);
    }

    public Signatures() {
        super("Signatures");
    }

    /**
     * 【可选 属性】
     * 设置 安全标识的最大值
     * <p>
     * 作用与文档入口文件 Document.xml 中的 MaxID相同，
     * 为了避免在签名时影响文档入口文件，采用了与ST_ID不一样
     * 的ID编码方式。
     * <p>
     * 推荐使用“sNNN”的编码方式，NNN从1开始
     *
     * @param maxSignId 安全标识的最大值
     * @return this
     */
    public Signatures setMaxSignId(String maxSignId) {
        this.removeOFDElemByNames("MaxSignId");
        if (maxSignId != null) {
            this.addOFDEntity("MaxSignId", maxSignId);
        }
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 安全标识的最大值
     * <p>
     * 作用与文档入口文件 Document.xml 中的 MaxID相同，
     * 为了避免在签名时影响文档入口文件，采用了与ST_ID不一样
     * 的ID编码方式。
     * <p>
     * 推荐使用“sNNN”的编码方式，NNN从1开始
     *
     * @return 安全标识的最大值
     */
    public String getMaxSignId() {
        Element e = this.getOFDElement("MaxSignId");
        return e == null ? null :e.getStringValue();
    }

    /**
     * 【可选】
     * 增加 数字签名或安全签章在类表中的注册信息
     *
     * @param signature 数字签名或安全签章在类表中的注册信息
     * @return this
     */
    public Signatures addSignature(Signature signature) {
        if (signature == null) {
            return this;
        }
        this.add(signature);
        return this;
    }

    /**
     * 【可选】
     * 获取 数字签名或安全签章在类表中的注册信息序列
     *
     * @return 数字签名或安全签章在类表中的注册信息序列
     */
    public List<Signature> getSignatures() {
        return this.getOFDElements("Signature", Signature::new);
    }
}
