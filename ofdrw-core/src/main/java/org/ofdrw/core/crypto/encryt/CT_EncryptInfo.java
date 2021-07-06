package org.ofdrw.core.crypto.encryt;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.Const;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.signatures.sig.Parameters;

import java.time.LocalDateTime;

/**
 * 加密描述信息
 * <p>
 * 多重加密形成多个加密操作记录
 * 多重加密时，图C.2中的EncryptInfo节点可出现多次，且前后两次加密
 * 可通过可选的Relative属性串联起来。
 * <p>
 * GMT0099 C.3 表C.2
 *
 * @author 权观宇
 * @since 2021-06-23 18:36:33
 */
public class CT_EncryptInfo extends OFDElement {
    public CT_EncryptInfo(Element proxy) {
        super(proxy);
    }

    public CT_EncryptInfo() {
        super("EncryptInfo");
    }

    /**
     * 【必选 属性 OFD 2.0】
     * <p>
     * 设置 加密操作标识
     *
     * @param id 加密操作标识
     * @return this
     */
    public CT_EncryptInfo setID(@NotNull String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("加密操作标识(id)为空");
        }
        this.addAttribute("ID", id);
        return this;
    }

    /**
     * 【必选 属性 OFD 2.0】
     * <p>
     * 获取 加密操作标识
     *
     * @return 加密操作标识
     */
    @NotNull
    public String getID() {
        return this.attributeValue("ID");
    }


    /**
     * 【可选 属性 OFD 2.0】
     * 设置 上一次加密操作标识
     *
     * @param id 上一次加密操作标识，如果为null表示移除属性
     * @return this
     */
    public CT_EncryptInfo setRelative(String id) {
        if (id == null) {
            this.removeAttr("Relative");
            return this;
        }
        this.addAttribute("Relative", id);
        return this;
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 获取 上一次加密操作标识
     *
     * @return 上一次加密操作标识，可能为null
     */
    @Nullable
    public String getRelative() {
        return this.attributeValue("Relative");
    }

    /**
     * 【必选】
     * 设置 加密组件的相关信息
     *
     * @param provider 加密组件的相关信息
     * @return this
     */
    public CT_EncryptInfo setProvider(@NotNull Provider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("加密组件的相关信息（Provider）为空");
        }
        this.set(provider);
        return this;
    }

    /**
     * 【必选】
     * 获取 加密组件的相关信息
     *
     * @return 加密组件的相关信息
     */
    @NotNull
    public Provider getProvider() {
        Element e = this.getOFDElement("Provider");
        if (e == null) {
            throw new IllegalArgumentException("加密组件的相关信息（Provider）为空");
        }
        return new Provider(e);
    }


    /**
     * 【必选】
     * 设置 文档加密相关内容的描述
     * <p>
     * 文档加密类型或范围
     *
     * @param encryptScope 文档加密相关内容的描述（文档加密类型或范围）
     * @return this
     */
    public CT_EncryptInfo setEncryptScope(@NotNull String encryptScope) {
        if (encryptScope == null) {
            throw new IllegalArgumentException("文档加密相关内容的描述（EncryptScope）为空");
        }
        this.setOFDEntity("EncryptScope", encryptScope);
        return this;
    }

    /**
     * 【必选】
     * 获取 文档加密相关内容的描述
     * <p>
     * 文档加密类型或范围
     *
     * @return 文档加密相关内容的描述（文档加密类型或范围）
     */
    @NotNull
    public String getEncryptScope() {
        Element e = this.getOFDElement("EncryptScope");
        if (e == null) {
            throw new IllegalArgumentException("文档加密相关内容的描述（EncryptScope）为空");
        }
        return e.getTextTrim();
    }

    /**
     * 【可选 OFD 2.0】
     * 设置 加密操作的附加描述集合
     *
     * @param parameters 加密操作的附加描述集合，null表示删除
     * @return this
     */
    public CT_EncryptInfo setParameters(@Nullable Parameters parameters) {
        if (parameters == null) {
            this.removeOFDElemByNames("Parameters");
            return this;
        }
        this.set(parameters);
        return this;
    }

    /**
     * 【可选 OFD 2.0】
     * 获取 加密操作的附加描述集合
     *
     * @return 加密操作的附加描述集合，可能为null
     */
    @Nullable
    public Parameters getParameters() {
        Element e = this.getOFDElement("Parameters");
        return e == null ? null : new Parameters(e);
    }

    /**
     * 【可选】
     * 设置 加密时间
     *
     * @param encryptDate 加密时间，如果为null表示删除
     * @return this
     */
    public CT_EncryptInfo setEncryptDate(LocalDateTime encryptDate) {
        if (encryptDate == null) {
            this.removeOFDElemByNames("EncryptDate");
            return this;
        }
        this.setOFDEntity("EncryptDate", encryptDate.format(Const.DATETIME_FORMATTER));
        return this;
    }

    /**
     * 【可选】
     * 获取 加密时间
     *
     * @return 加密时间，可能为null
     */
    public LocalDateTime getEncryptDate() {
        String str = this.getOFDElementText("EncryptDate");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        return LocalDateTime.parse(str, Const.DATETIME_FORMATTER);
    }

    /**
     * 【可选】
     * 获取 加密时间
     *
     * @return 加密时间，可能为null
     */
    public String getEncryptDateStr() {
        return this.getOFDElementText("EncryptDate");
    }


    /**
     * 【必选】
     * 设置 指向包内的二进制密钥描述文件
     * <p>
     * 其中记录里解密所需的参数，例如密码算法标识、方案标识和其他参数等。
     *
     * @param decryptSeedLoc 指向包内的二进制密钥描述文件，路径
     * @return this
     */
    public CT_EncryptInfo setDecryptSeedLoc(@NotNull ST_Loc decryptSeedLoc) {
        if (decryptSeedLoc == null) {
            throw new IllegalArgumentException("指向包内的二进制密钥描述文件(decryptSeedLoc)为空");
        }
        this.setOFDEntity("DecryptSeedLoc", decryptSeedLoc.toString());
        return this;
    }

    /**
     * 【必选】
     * 设置 指向包内的二进制密钥描述文件
     * <p>
     * 其中记录里解密所需的参数，例如密码算法标识、方案标识和其他参数等。
     *
     * @param decryptSeedLoc 指向包内的二进制密钥描述文件，路径
     * @return this
     */
    public CT_EncryptInfo setDecryptSeedLoc(@NotNull String decryptSeedLoc) {
        if (decryptSeedLoc == null) {
            throw new IllegalArgumentException("指向包内的二进制密钥描述文件(decryptSeedLoc)为空");
        }
        this.setOFDEntity("DecryptSeedLoc", decryptSeedLoc);
        return this;
    }

    /**
     * 【必选】
     * 获取 指向包内的二进制密钥描述文件
     * <p>
     * 其中记录里解密所需的参数，例如密码算法标识、方案标识和其他参数等。
     *
     * @return 指向包内的二进制密钥描述文件，路径
     */
    @NotNull
    public ST_Loc getDecryptSeedLoc() {
        return ST_Loc.getInstance(this.getOFDElementText("DecryptSeedLoc"));
    }

    /**
     * 【必选】
     * 设置 明密文映射表或其加密后的文件存储的路径
     *
     * @param entriesMapLoc 明密文映射表或其加密后的文件存储的路径
     * @return this
     */
    public CT_EncryptInfo setEntriesMapLoc(ST_Loc entriesMapLoc) {
        if (entriesMapLoc == null) {
            throw new IllegalArgumentException("明密文映射表或其加密后的文件存储的路径（entriesMapLoc）为空");
        }
        this.setOFDEntity("EntriesMapLoc", entriesMapLoc.toString());
        return this;
    }

    /**
     * 【必选】
     * 设置 明密文映射表或其加密后的文件存储的路径
     *
     * @param entriesMapLoc 明密文映射表或其加密后的文件存储的路径
     * @return this
     */
    public CT_EncryptInfo setEntriesMapLoc(String entriesMapLoc) {
        if (entriesMapLoc == null) {
            throw new IllegalArgumentException("明密文映射表或其加密后的文件存储的路径（entriesMapLoc）为空");
        }
        this.setOFDEntity("EntriesMapLoc", entriesMapLoc);
        return this;
    }

    /**
     * 【必选】
     * 获取 明密文映射表或其加密后的文件存储的路径
     *
     * @return 明密文映射表或其加密后的文件存储的路径
     */
    public ST_Loc getEntriesMapLoc() {
        String entriesMapLoc = this.getOFDElementText("EntriesMapLoc");
        if (entriesMapLoc == null) {
            throw new IllegalArgumentException("明密文映射表或其加密后的文件存储的路径（entriesMapLoc）为空");
        }
        return ST_Loc.getInstance(entriesMapLoc);
    }
}
