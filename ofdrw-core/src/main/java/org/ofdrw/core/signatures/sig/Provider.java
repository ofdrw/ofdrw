package org.ofdrw.core.signatures.sig;

import org.dom4j.Element;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.OFDElement;

import java.util.Base64;

/**
 * 创建签名时所用的签章组件提供者信息
 * <p>
 * 18.2.1 文件摘要 图 86 表 67
 *
 * @author 权观宇
 * @since 2019-11-20 19:26:15
 */
public class Provider extends OFDElement {
    public Provider(Element proxy) {
        super(proxy);
    }

    public Provider() {
        super("Provider");
    }

    /**
     * 【必选 属性】
     * 设置 创建签名时所用的签章组件提供者信息
     *
     * @param providerName 创建签名时所用的签章组件提供者信息
     * @return this
     */
    public Provider setProviderName(String providerName) {
        if (providerName == null || providerName.trim().length() == 0) {
            throw new IllegalArgumentException("创建签名时所用的签章组件提供者信息（ProviderName）为空");
        }
        this.addAttribute("ProviderName", providerName);
        return this;
    }

    /**
     * 【必选 属性】
     * 设置 创建签名时所用的签章组件提供者信息
     *
     * @return 创建签名时所用的签章组件提供者信息
     */
    public String getProviderName() {
        String str = this.attributeValue("ProviderName");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("创建签名时所用的签章组件提供者信息（ProviderName）为空");
        }
        return str;
    }

    /**
     * 【可选 属性】
     * 设置 创建签名时所使用的签章组件的版本
     *
     * @param version 创建签名时所使用的签章组件的版本
     * @return this
     */
    public Provider setVersion(String version) {
        if (version == null || version.trim().length() == 0) {
            this.removeAttr("Version");
            return this;
        }
        this.addAttribute("Version", version);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 创建签名时所使用的签章组件的版本
     *
     * @return 创建签名时所使用的签章组件的版本
     */
    public String getVersion() {
        return this.attributeValue("Version");
    }

    /**
     * 【可选 属性】
     * 设置 创建签名时所使用的签章组件的制造商
     *
     * @param company 创建签名时所使用的签章组件的制造商
     * @return this
     */
    public Provider setCompany(String company) {
        if (company == null || company.trim().length() == 0) {
            this.removeAttr("Company");
            return this;
        }
        this.addAttribute("Company", company);
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 创建签名时所使用的签章组件的制造商
     *
     * @return 创建签名时所使用的签章组件的制造商
     */
    public String getCompany() {
        return this.attributeValue("Company");
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 设置 创建签名时所用的签章组件的接口协议版本
     * <p>
     * GMT0099 表 B.2
     *
     * @param protocolVer 创建签名时所用的签章组件的接口协议版，如果为空则删除该属性
     * @return this
     */
    public Provider setProtocolVer(@Nullable String protocolVer) {
        if (protocolVer == null) {
            this.removeAttr("ProtocolVer");
            return this;
        }
        this.addAttribute("ProtocolVer", protocolVer);
        return this;
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 获取 创建签名时所用的签章组件的接口协议版本
     * <p>
     * GMT0099 表 B.2
     *
     * @return 创建签名时所用的签章组件的接口协议版，可能为空
     */
    @Nullable
    public String getProtocolVer() {
        return this.attributeValue("ProtocolVer");
    }

    /**
     * 【可选 OFD 2.0】
     * 设置 创建签名时所用的签章组件的扩展信息
     * <p>
     * GMT0099 表 B.2
     *
     * @param extendData 创建签名时所用的签章组件的扩展信息
     * @return this
     */
    public Provider setExtendData(@Nullable byte[] extendData) {
        if (extendData == null) {
            extendData = new byte[0];
        }
        this.setOFDEntity("ExtendData", Base64.getEncoder().encodeToString(extendData));
        return this;
    }

    /**
     * 【可选 OFD 2.0】
     * 获取 创建签名时所用的签章组件的扩展信息
     * <p>
     * GMT0099 表 B.2
     *
     * @return 创建签名时所用的签章组件的扩展信息，可能为空字节数组
     */
    public byte[] getExtendData() {
        Element e = this.getOFDElement("ExtendData");
        if (e == null) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(e.getTextTrim());
    }
}
