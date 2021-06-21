package org.ofdrw.core.crypto.encryt;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.crypto.ProtectionCaseID;

/**
 * 密钥描述文件
 * <p>
 * 密钥描述文件采用XML格式描述，存储了方案、算法和
 * 多人、多角色、多密码或证书等关键解密信息，其数据结构见图C.1
 * <p>
 * GMT 0099-2020 C.2 密钥描述文件
 *
 * @author 权观宇
 * @since 2021-06-21 18:48:15
 */
public class DecyptSeed extends OFDElement {

    public DecyptSeed(Element proxy) {
        super(proxy);
    }

    public DecyptSeed() {
        super("DecyptSeed");
    }

    /**
     * 【必选 属性】
     * <p>
     * 设置 加密操作标识，应与解密入口描述中的一致
     *
     * @param id 加密操作标识
     * @return this
     */
    public DecyptSeed setID(@NotNull String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("加密操作标识(id)为空");
        }
        this.attributeValue("ID", id);
        return this;
    }

    /**
     * 【必选 属性】
     * <p>
     * 获取 加密操作标识，应与解密入口描述中的一致
     *
     * @return 加密操作标识
     */
    @NotNull
    public String getID() {
        return this.attributeValue("ID");
    }

    /**
     * 【必选 属性】
     * 设置 加密保护方案标识，参见附录A
     *
     * @param encryptCaseId 加密保护方案标识 {@link ProtectionCaseID}
     * @return this
     */
    public DecyptSeed setEncryptCaseId(@NotNull String encryptCaseId) {
        if (encryptCaseId == null) {
            throw new IllegalArgumentException("加密保护方案标识(id)为空");
        }
        this.attributeValue("EncryptCaseId", encryptCaseId);
        return this;
    }
    /**
     * 【必选 属性】
     * 设置 加密保护方案标识，参见附录A
     *
     * @param encryptCaseId 加密保护方案标识 {@link ProtectionCaseID}
     * @return this
     */
    public DecyptSeed setEncryptCaseId(@NotNull ProtectionCaseID encryptCaseId) {
        if (encryptCaseId == null) {
            throw new IllegalArgumentException("加密保护方案标识(id)为空");
        }
        this.attributeValue("EncryptCaseId", encryptCaseId.toString());
        return this;
    }
    /**
     * 【必选 属性】
     * 获取 加密保护方案标识，参见附录A {@link ProtectionCaseID}
     *
     * @return 加密保护方案标识
     */
    public String getEncryptCaseId() {
        return this.attributeValue("EncryptCaseId");
    }


}
