package org.ofdrw.core.signatures.sig;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.dom4j.Element;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.signatures.appearance.Seal;
import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.core.signatures.range.References;

import java.util.List;

/**
 * 签名要保护的原文及本次签名相关的信息
 * <p>
 * 18.2.1 文件摘要 图 86 表 67
 * <p>
 * GMT0099 B.2 图 B.2
 *
 * @author 权观宇
 * @since 2019-11-20 07:23:38
 */
public class SignedInfo extends OFDElement {
    public SignedInfo(Element proxy) {
        super(proxy);
    }

    public SignedInfo() {
        super("SignedInfo");
    }


    /**
     * 【必选】
     * 设置 创建签名时所用的签章组件提供者信息
     *
     * @param provider 创建签名时所用的签章组件提供者信息
     * @return this
     */
    public SignedInfo setProvider(Provider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("创建签名时所用的签章组件提供者信息（Provider）为空");
        }
        this.set(provider);
        return this;
    }

    /**
     * 【必选】
     * 获取 创建签名时所用的签章组件提供者信息
     *
     * @return 创建签名时所用的签章组件提供者信息
     */
    public Provider getProvider() {
        Element e = this.getOFDElement("Provider");
        return e == null ? null : new Provider(e);
    }

    /**
     * 【可选】
     * 设置 签名方法
     * <p>
     * 记录安全模块返回的签名算法代码，以便验证时使用
     *
     * @param signatureMethod 签名方法
     * @return this
     */
    public SignedInfo setSignatureMethod(String signatureMethod) {
        this.setOFDEntity("SignatureMethod", signatureMethod);
        return this;
    }

    /**
     * 【可选】
     * 设置 签名方法
     * <p>
     * 记录安全模块返回的签名算法代码，以便验证时使用
     *
     * @param oid 签名方法ASN1对象标识符
     * @return this
     */
    public SignedInfo setSignatureMethod(ASN1ObjectIdentifier oid) {
        if (oid == null) {
            throw new IllegalArgumentException("签名方法ASN1对象标识符(oid)为空");
        }
        return setSignatureMethod(oid.toString());
    }

    /**
     * 【可选】
     * 设置 签名方法
     * <p>
     * 记录安全模块返回的签名算法代码，以便验证时使用
     *
     * @return 签名方法
     */
    public String getSignatureMethod() {
        return this.getOFDElementText("SignatureMethod");
    }


    /**
     * 【可选】
     * 设置 签名时间
     * <p>
     * 记录安全模块返回的签名时间，以便验证时使用
     *
     * @param signatureDateTime 签名时间
     * @return this
     */
    public SignedInfo setSignatureDateTime(String signatureDateTime) {
        this.setOFDEntity("SignatureDateTime", signatureDateTime);
        return this;
    }

    /**
     * 【可选】
     * 设置 签名时间
     * <p>
     * 记录安全模块返回的签名时间，以便验证时使用
     *
     * @return 签名时间
     */
    public String getSignatureDateTime() {
        return this.getOFDElementText("SignatureDateTime");
    }

    /**
     * 【可选 OFD 2.0】
     * 设置 扩展签名属性集
     *
     * @param parameters 扩展签名属性集，null表示删除
     * @return this
     */
    public SignedInfo setParameters(@Nullable Parameters parameters) {
        if (parameters == null) {
            this.removeOFDElemByNames("Parameters");
            return this;
        }
        this.set(parameters);
        return this;
    }

    /**
     * 【可选 OFD 2.0】
     * 获取 扩展签名属性集
     *
     * @return 扩展签名属性集，可能为null
     */
    @Nullable
    public Parameters getParameters() {
        Element e = this.getOFDElement("Parameters");
        return e == null ? null : new Parameters(e);
    }


    /**
     * 【必选】
     * 设置 包内文件计算所得的摘要记录列表
     * <p>
     * 一个受本次签名保护的包内文件对应一个 Reference节点
     *
     * @param references 包内文件计算所得的摘要记录列表
     * @return this
     */
    public SignedInfo setReferences(References references) {
        if (references == null) {
            throw new IllegalArgumentException("包内文件计算所得的摘要记录列表（References）为空");
        }
        this.set(references);
        return this;
    }

    /**
     * 【必选】
     * 设置 包内文件计算所得的摘要记录列表
     * <p>
     * 一个受本次签名保护的包内文件对应一个 Reference节点
     *
     * @return 包内文件计算所得的摘要记录列表
     */
    public References getReferences() {
        Element e = this.getOFDElement("References");
        if (e == null) {
            throw new IllegalArgumentException("包内文件计算所得的摘要记录列表（References）为空");
        }
        return new References(e);
    }

    /**
     * 【可选】
     * 增加 本签名关联的外观（用OFD中的注解表示）
     * <p>
     * 该节点可出现多次
     *
     * @param stampAnnot 本签名关联的外观
     * @return this
     */
    public SignedInfo addStampAnnot(StampAnnot stampAnnot) {
        if (stampAnnot == null) {
            return this;
        }
        this.add(stampAnnot);
        return this;
    }

    /**
     * 【可选】
     * 获取 本签名关联的外观（用OFD中的注解表示）序列
     * <p>
     * 该节点可出现多次
     *
     * @return 本签名关联的外观序列
     */
    public List<StampAnnot> getStampAnnots() {
        return this.getOFDElements("StampAnnot", StampAnnot::new);
    }


    /**
     * 【可选】
     * 设置 电子印章信息
     *
     * @param seal 电子印章信息
     * @return this
     */
    public SignedInfo setSeal(Seal seal) {
        if (seal == null) {
            this.removeOFDElemByNames("Seal");
            return this;
        }
        this.set(seal);
        return this;
    }

    /**
     * 【可选】
     * 设置 电子印章信息
     *
     * @return 电子印章信息
     */
    public Seal getSeal() {
        Element e = this.getOFDElement("Seal");
        return e == null ? null : new Seal(e);
    }
}
