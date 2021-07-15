package org.ofdrw.core.crypto.encryt;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.OFDElement;

import java.util.List;

/**
 * 未加密的名密文映射表文件数据结构
 * <p>
 * GMT0099 附录C 图C.3
 *
 * @author 权观宇
 * @since 2021-07-15 19:14:42
 */
public class EncryptEntries extends OFDElement {
    public EncryptEntries(Element proxy) {
        super(proxy);
    }

    public EncryptEntries() {
        super("EncryptEntries");
    }

    /**
     * 【必选 属性】
     * 设置 加密操作标识
     * <p>
     * 应与解密入口描述中的一致
     *
     * @param id 加密操作标识，应与解密入口描述中的一致
     * @return this
     */
    public EncryptEntries setID(@NotNull String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("加密操作标识(id)为空");
        }
        this.addAttribute("ID", id);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 加密操作标识
     *
     * @return 加密操作标识
     */
    public String getID() {
        return this.attributeValue("ID");
    }

    /**
     * 【必选】
     * 增加 明密文对应关系
     *
     * @param encryptEntry 明密文对应关系
     * @return this
     */
    public EncryptEntries addEncryptEntry(EncryptEntry encryptEntry) {
        if (encryptEntry == null) {
            return this;
        }
        this.add(encryptEntry);
        return this;
    }

    /**
     * 【必选】
     * 增加 明密文对应关系
     *
     * @param path           加密前包内文件的绝对路径
     * @param ePath          加密后形成的包内密文的绝对路径
     * @param decryptSeedLoc 此项密文独有的密钥数据文件路径
     * @return this
     */
    public EncryptEntries addEncryptEntry(@NotNull String path, @NotNull String ePath, @Nullable String decryptSeedLoc) {
        this.add(new EncryptEntry(path, ePath, decryptSeedLoc));
        return this;
    }

    /**
     * 【必选】
     * 增加 明密文对应关系
     *
     * @param path  加密前包内文件的绝对路径
     * @param ePath 加密后形成的包内密文的绝对路径
     * @return this
     */
    public EncryptEntries addEncryptEntry(@NotNull String path, @NotNull String ePath) {
        this.add(new EncryptEntry(path, ePath));
        return this;
    }

    /**
     * 【必选】
     * 获取 明密文对应关系列表
     *
     * @return 明密文对应关系列表
     */
    public List<EncryptEntry> getEncryptEntries() {
        return this.getOFDElements("EncryptEntry", EncryptEntry::new);
    }
}
