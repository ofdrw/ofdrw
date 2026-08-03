package org.ofdrw.crypto.decryptor;

/**
 * 解密结果：包含从密钥描述文件中解密出的文件加密密钥(FEK)和初始化向量(IV)
 * <p>
 * FEK 和 IV 均为 16 字节，用于 SM4-CBC 模式解密包内文件。
 *
 * @author 权观宇
 * @since 2.4.0
 */
public class DecryptResult {

    /**
     * 文件加密密钥（File Encrypt Key）
     * <p>
     * 长度 16 字节，用于 SM4-CBC 模式解密包内加密文件
     */
    private final byte[] fek;

    /**
     * 初始化向量（Initialization Vector）
     * <p>
     * 长度 16 字节，用于 SM4-CBC 模式解密
     */
    private final byte[] iv;

    /**
     * 创建解密结果
     *
     * @param fek 文件加密密钥，16字节
     * @param iv  初始化向量，16字节
     * @throws IllegalArgumentException 若参数为 null 或长度不为 16
     */
    public DecryptResult(byte[] fek, byte[] iv) {
        if (fek == null || fek.length != 16) {
            throw new IllegalArgumentException("文件加密密钥(fek)必须为16字节");
        }
        if (iv == null || iv.length != 16) {
            throw new IllegalArgumentException("初始化向量(iv)必须为16字节");
        }
        this.fek = fek.clone();
        this.iv = iv.clone();
    }

    /**
     * 获取文件加密密钥
     *
     * @return FEK 副本（16字节）
     */
    public byte[] getFek() {
        return fek.clone();
    }

    /**
     * 获取初始化向量
     *
     * @return IV 副本（16字节）
     */
    public byte[] getIv() {
        return iv.clone();
    }
}
