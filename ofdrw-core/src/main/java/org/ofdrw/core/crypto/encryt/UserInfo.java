package org.ofdrw.core.crypto.encryt;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.OFDElement;

import java.util.Base64;

/**
 * 【OFD 2.0】
 * 可解密该次操作的用户
 * <p>
 * GMT0099 附录 C.1
 *
 * @author 权观宇
 * @since 2021-06-23 18:39:00
 */
public class UserInfo extends OFDElement {
    /**
     * 用户角色类型：文档管理员
     */
    public static final String UserTypeOwner = "Owner";
    /**
     * 用户角色类型：用户，默认值
     */
    public static final String UserTypeUser = "User";


    public UserInfo(Element proxy) {
        super(proxy);
    }

    public UserInfo() {
        super("UserInfo");
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 设置 用户名称
     *
     * @param userName 用户名称，如果为null则删除该属性。
     * @return this
     */
    public UserInfo setUserName(@Nullable String userName) {
        if (userName == null) {
            this.removeAttr("UserName");
            return this;
        }
        this.addAttribute("UserName", userName);
        return this;
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 获取 用户名称
     *
     * @return 用户名称，可能为null。
     */
    @Nullable
    public String getUserName() {
        return this.attributeValue("UserName");
    }


    /**
     * 【可选 属性 OFD 2.0】
     * 设置 用户角色类型
     * <p>
     * 当时文档管理员时取值为Owner，普通用户取值为User，默认为User。
     *
     * {@link #UserTypeOwner} {@link #UserTypeUser}
     *
     * @param userType 用户角色类型，如果为null则删除该属性。
     * @return this
     */
    public UserInfo setUserType(@Nullable String userType) {
        if (userType == null) {
            this.removeAttr("UserType");
            return this;
        }
        this.addAttribute("UserType", userType);
        return this;
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 获取 用户角色类型
     * <p>
     * 默认值： User（用户）
     *
     * @return 用户角色类型，默认值 用户（User）
     */
    @NotNull
    public String getUserType() {
        String userType = this.attributeValue("UserType");
        return userType == null ? UserTypeUser : userType;
    }

    /**
     * 【可选 OFD 2.0】
     * 设置 用户的加解密公钥证书
     * <p>
     * 加密方案标识符为 1.1.2时必选
     *
     * @param userCert 用户的加解密公钥证书，null时表示删除属性
     * @return this
     */
    public UserInfo setUserCert(@Nullable byte[] userCert) {
        if (userCert == null) {
            this.removeOFDElemByNames("UserCert");
            return this;
        }
        this.addOFDEntity("UserCert", Base64.getEncoder().encodeToString(userCert));
        return this;
    }

    /**
     * 【可选 OFD 2.0】
     * 获取 用户的加解密公钥证书
     *
     * @return 用户的加解密公钥证书，可能为null
     */
    @Nullable
    public byte[] getUserCert() {
        final Element e = this.getOFDElement("UserCert");
        if (e == null) {
            return null;
        }
        return Base64.getDecoder().decode(e.getTextTrim());
    }

    /**
     * 【必选  OFD 2.0】
     * 设置 文件对称加密的包装密钥
     *
     * @param encryptedWk 文件对称加密的包装密钥
     * @return this
     */
    public UserInfo setEncryptedWK(@NotNull byte[] encryptedWk) {
        if (encryptedWk == null) {
            throw new IllegalArgumentException("文件对称加密的包装密钥（EncrypteWk）为空");
        }
        this.setOFDEntity("EncryptedWK", Base64.getEncoder().encodeToString(encryptedWk));
        return this;
    }


    /**
     * 【必选 OFD 2.0】
     * 获取 文件对称加密的包装密钥
     *
     * @return 文件对称加密的包装密钥
     */
    @NotNull
    public byte[] getEncryptedWK() {
        final Element e = this.getOFDElement("EncryptedWK");
        if (e == null) {
            throw new IllegalArgumentException("文件对称加密的包装密钥(EncryptedWK)不存在");
        }
        return Base64.getDecoder().decode(e.getTextTrim());
    }


    /**
     * 【可选 OFD 2.0】
     * 设置 初始化向量值
     * <p>
     * 默认16个0
     *
     * @param ivValue 初始化向量值，如果为null表示删除参数。
     * @return this
     */
    public UserInfo setIVValue(byte[] ivValue) {
        if (ivValue == null) {
            this.removeOFDElemByNames("IVValue");
        }
        this.setOFDEntity("IVValue", Base64.getEncoder().encodeToString(ivValue));
        return this;
    }

    /**
     * 【可选 OFD 2.0】
     * 获取 初始化向量值
     * <p>
     * 默认16个0
     *
     * @return 初始化向量值，默认16个0
     */
    public byte[] getIVValue() {
        Element e = this.getOFDElement("IVValue");
        return e == null ? new byte[16] : Base64.getDecoder().decode(e.getTextTrim());
    }
}
