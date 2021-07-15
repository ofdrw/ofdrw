package org.ofdrw.core.crypto.encryt;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * 明密文对应关系
 *
 * @author 权观宇
 * @since 2021-07-15 19:20:24
 */
public class EncryptEntry extends OFDElement {
    public EncryptEntry(Element proxy) {
        super(proxy);
    }

    public EncryptEntry() {
        super("EncryptEntry");
    }

    /**
     * 创建 明密文对应关系
     *
     * @param path           加密前包内文件的绝对路径
     * @param ePath          加密后形成的包内密文的绝对路径
     * @param decryptSeedLoc 此项密文独有的密钥数据文件路径
     */
    public EncryptEntry(ST_Loc path, ST_Loc ePath, ST_Loc decryptSeedLoc) {
        this();
        setPathA(path);
        setEPath(ePath);
        setDecryptSeedLoc(decryptSeedLoc);
    }

    /**
     * 创建 明密文对应关系
     *
     * @param path  加密前包内文件的绝对路径
     * @param ePath 加密后形成的包内密文的绝对路径
     */
    public EncryptEntry(ST_Loc path, ST_Loc ePath) {
        this();
        setPathA(path);
        setEPath(ePath);
    }

    /**
     * 创建 明密文对应关系
     *
     * @param path           加密前包内文件的绝对路径
     * @param ePath          加密后形成的包内密文的绝对路径
     * @param decryptSeedLoc 此项密文独有的密钥数据文件路径
     */
    public EncryptEntry(String path, String ePath, String decryptSeedLoc) {
        this();
        setPathA(path);
        setEPath(ePath);
        setDecryptSeedLoc(decryptSeedLoc);
    }

    /**
     * 创建 明密文对应关系
     *
     * @param path  加密前包内文件的绝对路径
     * @param ePath 加密后形成的包内密文的绝对路径
     */
    public EncryptEntry(String path, String ePath) {
        this();
        setPathA(path);
        setEPath(ePath);
    }


    /**
     * 【必选 属性】
     * 设置 加密前包内文件的绝对路径
     *
     * @param path 加密前包内文件的绝对路径
     * @return this
     */
    public EncryptEntry setPathA(@NotNull String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("加密前包内文件的绝对路径(path)为空");
        }
        this.addAttribute("Path", path);
        return this;
    }

    /**
     * 【必选 属性】
     * 设置 加密前包内文件的绝对路径
     * <p>
     * 区别于{@link #getPath()} 重名名为 PathA
     *
     * @param path 加密前包内文件的绝对路径
     * @return this
     */
    public EncryptEntry setPathA(@NotNull ST_Loc path) {
        return setPathA(path == null ? null : path.toString());
    }

    /**
     * 【必选 属性】
     * 获取 加密前包内文件的绝对路径
     *
     * @return 加密前包内文件的绝对路径
     */
    public ST_Loc getPathA() {
        return ST_Loc.getInstance(this.attributeValue("Path"));
    }

    /**
     * 【必选 属性】
     * 设置 加密后形成的包内密文的绝对路径
     *
     * @param ePath 加密后形成的包内密文的绝对路径
     * @return this
     */
    public EncryptEntry setEPath(@NotNull String ePath) {
        if (ePath == null || ePath.isEmpty()) {
            throw new IllegalArgumentException("加密后形成的包内密文的绝对路径(ePath)为空");
        }
        this.addAttribute("EPath", ePath);
        return this;
    }

    /**
     * 【必选 属性】
     * 设置 加密后形成的包内密文的绝对路径
     *
     * @param ePath 加密后形成的包内密文的绝对路径
     * @return this
     */
    public EncryptEntry setEPath(@NotNull ST_Loc ePath) {
        return setEPath(ePath == null ? null : ePath.toString());
    }

    /**
     * 【必选 属性】
     * 获取 加密后形成的包内密文的绝对路径
     *
     * @return 加密后形成的包内密文的绝对路径
     */
    public ST_Loc getEPath() {
        return ST_Loc.getInstance(this.attributeValue("EPath"));
    }


    /**
     * 【可选 属性】
     * 设置 此项密文独有的密钥数据文件路径
     * <p>
     * 该属性不出现时，使用所属加密操作信息中定义的通用密钥数据
     *
     * @param decryptSeedLoc 此项密文独有的密钥数据文件路径，null时表示删除
     * @return this
     */
    public EncryptEntry setDecryptSeedLoc(String decryptSeedLoc) {
        if (decryptSeedLoc == null) {
            this.removeAttr("DecryptSeedLoc");
            return this;
        }
        this.addAttribute("DecryptSeedLoc", decryptSeedLoc);
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 此项密文独有的密钥数据文件路径
     * <p>
     * 该属性不出现时，使用所属加密操作信息中定义的通用密钥数据
     *
     * @param decryptSeedLoc 此项密文独有的密钥数据文件路径，null时表示删除
     * @return this
     */
    public EncryptEntry setDecryptSeedLoc(ST_Loc decryptSeedLoc) {
        return setDecryptSeedLoc(decryptSeedLoc == null ? null : decryptSeedLoc.toString());
    }

    /**
     * 【可选 属性】
     * 获取 此项密文独有的密钥数据文件路径
     * <p>
     * 该属性不出现时，使用所属加密操作信息中定义的通用密钥数据
     *
     * @return 此项密文独有的密钥数据文件路径，null请使用 加密操作信息中定义的通用密钥数据
     */
    @Nullable
    public ST_Loc getDecryptSeedLoc() {
        return ST_Loc.getInstance(this.attributeValue("DecryptSeedLoc"));
    }


}
