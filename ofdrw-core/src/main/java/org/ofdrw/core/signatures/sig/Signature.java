package org.ofdrw.core.signatures.sig;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.OFDSimpleTypeElement;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * 签名接口
 * <p>
 * OFD的数字签名通过对描述文件的保护间接实现对OFD原文的保护。
 * 签名结构中的签名信息（SignedInfo）是这一过程中的关键点，
 * 其中记录了当次数字签名保护的所有文件的二进制摘要信息，同时
 * 将安全算法提供者、签名算法、签名时间、和所应用的安全印章等
 * 信息也包含在此节点内。签名描述文件同时包含了签名值将要存放的
 * 包内位置，一旦对该文件实施签名保护，则其对应的包内文件原文
 * 以及本次签名对应的附加信息都将不可改动，从而实现依次数字签名
 * 对整个原文内容的保护。签名描述文件的主要结构描述见图 86。
 * <p>
 * 文件摘要文件根节点为 Signature，其子节点 SignedInfo 对应元素说明见表 67。
 * <p>
 * 18.2.1 文件摘要 图 86 表 67
 *
 * @author 权观宇
 * @since 2019-11-20 07:12:16
 */
public class Signature extends OFDElement {
    public Signature(Element proxy) {
        super(proxy);
    }

    public Signature() {
        super("Signature");
    }

    // TODO 2019-11-20 19:21:51 SignedInfo

    /**
     * 【必选】
     * 设置 指向安全签名提供者所返还的针对签名描述文件计算所得的签名值文件
     *
     * @param signedValue 指向安全签名提供者所返还的针对签名描述文件计算所得的签名值文件
     * @return this
     */
    public Signature setSignedValue(ST_Loc signedValue) {
        if (signedValue == null) {
            throw new IllegalArgumentException("");
        }
        this.set(new OFDSimpleTypeElement("SignedValue", signedValue));
        return this;
    }

    /**
     * 【必选】
     * 获取 指向安全签名提供者所返还的针对签名描述文件计算所得的签名值文件
     *
     * @return 指向安全签名提供者所返还的针对签名描述文件计算所得的签名值文件
     */
    public ST_Loc getSignedValue() {
        Element e = this.getOFDElement("SignedValue");
        return e == null ? null : ST_Loc.getInstance(e);
    }
}
