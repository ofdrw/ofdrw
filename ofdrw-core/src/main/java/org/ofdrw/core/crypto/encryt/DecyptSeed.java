package org.ofdrw.core.crypto.encryt;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.crypto.ProtectionCaseID;

import java.util.List;

/**
 * 密钥描述文件
 * <p>
 * 密钥描述文件采用XML格式描述，存储了方案、算法和
 * 多人、多角色、多密码或证书等关键解密信息，其数据结构见图C.1
 * <p>
 * 根据加密类型的不同，文件对称加密的包装密钥的生成方式也不同。
 * 口令加密是，使用用户输入的口令作为基础，通过密钥派生函数派生出密钥，
 * 然后用该密钥对文件对称加密密钥进行加密，生成文件对称加密的包装密钥。
 * 使用密钥派生函数是，应遵循GB/T 32918。
 * 证书加密时，使用用户的公钥证书对文件对称加密密钥进行非对称加密，
 * 生成文件对称加密的包装密钥。
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
        this.addAttribute("ID", id);
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
        this.addAttribute("EncryptCaseId", encryptCaseId.toString());
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

    /**
     * 【必选 OFD 2.0】
     * 增加 可解密该次操作的用户
     *
     * @param userInfo 可解密该次操作的用户
     * @return this
     */
    public DecyptSeed addUserInfo(@NotNull UserInfo userInfo) {
        if (userInfo == null) {
            throw new IllegalArgumentException("可解密该次操作的用户(userInfo)为空");
        }
        this.add(userInfo);
        return this;
    }

    /**
     * 【必选 OFD 2.0】
     * 获取 可解密该次操作的用户列表
     *
     * @return 可解密该次操作的用户列表
     */
    public List<UserInfo> getUserInfos() {
        return this.getOFDElements("UserInfo", UserInfo::new);
    }


    /**
     * 【必选 OFD 2.0】
     * 设置 扩展参数节点
     *
     * @param extendParams 扩展参数节点
     * @return this
     */
    public DecyptSeed setExtendParams(@NotNull ExtendParams extendParams) {
        if (extendParams == null) {
            throw new IllegalArgumentException("扩展参数节点(ExtendParams)为空");
        }
        this.set(extendParams);
        return this;
    }

    /**
     * 【必选 OFD 2.0】
     * 获取 扩展参数节点
     *
     * @return 扩展参数节点
     */
    @NotNull
    public ExtendParams getExtendParams() {
        Element e = this.getOFDElement("ExtendParams");
        if (e == null) {
            ExtendParams emptyParams = new ExtendParams();
            this.setExtendParams(emptyParams);
            return emptyParams;
        }
        return new ExtendParams(e);
    }
}
