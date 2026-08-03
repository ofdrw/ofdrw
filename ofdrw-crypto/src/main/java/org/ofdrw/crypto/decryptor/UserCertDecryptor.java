package org.ofdrw.crypto.decryptor;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.ofdrw.core.crypto.ProtectionCaseID;
import org.ofdrw.core.crypto.encryt.UserInfo;
import org.ofdrw.gm.sm2strut.SM2Cipher;

import java.io.IOException;

/**
 * OFD 用户证书解密器
 * <p>
 * 使用 SM2 私钥解密 C1C3C2 格式的密文，恢复文件加密密钥(FEK)。
 * 对应加密器：{@link org.ofdrw.crypto.enryptor.UserCertEncryptor}
 * <p>
 * 加密时使用公钥加密 FEK，解密时使用对应的私钥解密。
 *
 * @author 权观宇
 * @since 2.4.0
 */
public class UserCertDecryptor implements UserFEKDecryptor {

    /**
     * 目标用户名
     * <p>
     * null 时匹配第一个 UserInfo（不按用户名过滤）
     */
    private final String username;

    /**
     * SM2 私钥参数
     */
    private final ECPrivateKeyParameters privateKeyParams;

    /**
     * 创建证书解密器
     *
     * @param username   目标用户名，null 时匹配第一个 UserInfo
     * @param privateKey SM2 私钥（用于解密 FEK）
     * @throws IllegalArgumentException 私钥为空
     */
    public UserCertDecryptor(String username, ECPrivateKeyParameters privateKey) {
        if (privateKey == null) {
            throw new IllegalArgumentException("SM2私钥(privateKey)不能为空");
        }
        this.username = username;
        this.privateKeyParams = privateKey;
    }

    /**
     * 创建证书解密器（不指定用户名，匹配第一个 UserInfo）
     *
     * @param privateKey SM2 私钥
     */
    public UserCertDecryptor(ECPrivateKeyParameters privateKey) {
        this(null, privateKey);
    }

    /**
     * 从 UserInfo 中解密出 FEK 和 IV
     * <p>
     * 使用 SM2 私钥解密 ASN.1 编码的 EncryptedWK(C1C3C2格式) → FEK。
     *
     * @param userInfo 加密的用户信息
     * @return 解密结果，包含 FEK(16字节) 和 IV(16字节)
     * @throws CryptoException 解密失败（密钥不匹配或数据损坏）
     */
    @Override
    public DecryptResult decrypt(UserInfo userInfo) throws CryptoException {
        byte[] encryptedWK = userInfo.getEncryptedWK();
        byte[] iv = userInfo.getIVValue();

        try {
            // EncryptedWK 以 ASN.1 DER 编码存储（SM2Cipher 格式）
            SM2Cipher sm2Cipher = SM2Cipher.getInstance(
                    org.bouncycastle.asn1.ASN1Primitive.fromByteArray(encryptedWK));
            // 转换为 C1C3C2 格式用于 SM2Engine 解密
            byte[] c1c3c2 = sm2Cipher.convertC1C3C2();

            // SM2 解密
            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(false, privateKeyParams);
            byte[] fek = engine.processBlock(c1c3c2, 0, c1c3c2.length);

            return new DecryptResult(fek, iv);
        } catch (IOException e) {
            throw new CryptoException("证书解密失败：无法解析加密数据格式", e);
        } catch (Exception e) {
            throw new CryptoException("证书解密失败：密钥不匹配或数据损坏", e);
        }
    }

    @Override
    public String encryptCaseId() {
        return ProtectionCaseID.EncryptGMCert.getId();
    }

    @Override
    public String getUsername() {
        return username;
    }
}
