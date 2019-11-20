package org.ofdrw.core.signatures.sig;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

/**
 * 签名要保护的原文及本次签名相关的信息
 * <p>
 * 18.2.1 文件摘要 图 86 表 67
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

    // TODO 2019-11-20 19:49:31 References
    // TODO 2019-11-20 19:49:42 StampAnnot
    // TODO 2019-11-20 19:49:49 Seal
}
