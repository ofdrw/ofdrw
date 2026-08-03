package org.ofdrw.crypto.decryptor;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.ofdrw.core.crypto.ProtectionCaseID;
import org.ofdrw.core.crypto.encryt.UserInfo;
import org.ofdrw.gm.support.KDF;

import java.nio.charset.StandardCharsets;

/**
 * OFD 用户口令解密器
 * <p>
 * 使用口令通过密钥派生函数(KDF)生成密钥加密密钥(KEK)，
 * 然后用 SM4-CBC 解密文件加密密钥(FEK)的包装密钥。
 * <p>
 * KDF 遵循 GB/T 32918.3-2016 5.4.3 密钥派生函数。
 * 对应加密器：{@link org.ofdrw.crypto.enryptor.UserPasswordEncryptor}
 *
 * @author 权观宇
 * @since 2.4.0
 */
public class UserPasswordDecryptor implements UserFEKDecryptor {

    /**
     * 目标用户名
     * <p>
     * null 时匹配第一个 UserInfo（不按用户名过滤）
     */
    private final String username;

    /**
     * 由 KDF 从口令派生出的密钥加密密钥(KEK)
     * <p>
     * 长度 16 字节，用于 SM4-CBC 解密 EncryptedWK 得到 FEK
     */
    private final byte[] kek;

    /**
     * 创建口令解密器
     *
     * @param username 目标用户名，null 时匹配第一个 UserInfo
     * @param password 加密时使用的口令（与加密时一致）
     * @throws IllegalArgumentException 口令为空
     */
    public UserPasswordDecryptor(String username, String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("口令(password)不能为空");
        }
        this.username = username;
        this.kek = extendKey(password);
    }

    /**
     * 创建口令解密器（不指定用户名，匹配第一个 UserInfo）
     *
     * @param password 加密时使用的口令
     */
    public UserPasswordDecryptor(String password) {
        this(null, password);
    }

    /**
     * 将口令通过密钥派生函数生成 KEK
     * <p>
     * 与 {@link org.ofdrw.crypto.enryptor.UserPasswordEncryptor#extendKey} 完全一致，
     * 使用 SM3 + KDF，派生 16 字节密钥。
     *
     * @param password 用户口令
     * @return 16字节 KEK
     */
    private byte[] extendKey(String password) {
        SM3.Digest h = new SM3.Digest();
        return KDF.extend(h, password.getBytes(StandardCharsets.UTF_8), 16);
    }

    /**
     * 从 UserInfo 中解密出 FEK 和 IV
     * <p>
     * 使用 KDF 派生的 KEK + UserInfo 中的 IV，SM4-CBC 解密 EncryptedWK → FEK。
     *
     * @param userInfo 加密的用户信息
     * @return 解密结果，包含 FEK(16字节) 和 IV(16字节)
     * @throws CryptoException 解密失败（口令错误或数据损坏）
     */
    @Override
    public DecryptResult decrypt(UserInfo userInfo) throws CryptoException {
        // 获取加密的文件加密密钥（包装密钥）
        byte[] encryptedWK = userInfo.getEncryptedWK();
        // 获取 IV
        byte[] iv = userInfo.getIVValue();

        // SM4-CBC 解密 EncryptedWK → FEK
        PaddedBufferedBlockCipher blockCipher =
                new PaddedBufferedBlockCipher(new CBCBlockCipher(new SM4Engine()), new PKCS7Padding());
        blockCipher.init(false, new ParametersWithIV(new KeyParameter(kek), iv));

        byte[] buffOut = new byte[encryptedWK.length + blockCipher.getBlockSize()];
        int n = blockCipher.processBytes(encryptedWK, 0, encryptedWK.length, buffOut, 0);
        try {
            n += blockCipher.doFinal(buffOut, n);
        } catch (org.bouncycastle.crypto.InvalidCipherTextException e) {
            throw new CryptoException("口令解密失败：口令错误或数据损坏", e);
        }

        byte[] fek = new byte[n];
        System.arraycopy(buffOut, 0, fek, 0, n);

        return new DecryptResult(fek, iv);
    }

    @Override
    public String encryptCaseId() {
        return ProtectionCaseID.EncryptGMPassword.getId();
    }

    @Override
    public String getUsername() {
        return username;
    }
}
