package org.ofdrw.crypto.enryptor;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.engines.SM2Engine;

import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.core.crypto.ProtectionCaseID;
import org.ofdrw.core.crypto.encryt.UserInfo;
import org.ofdrw.gm.sm2strut.SM2Cipher;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.Certificate;

/**
 * OFD用户证书加密器
 *
 * @author 权观宇
 * @since 2021-7-29 18:36:07
 */
public class UserCertEncryptor implements UserFEKEncryptor {
    /**
     * 用户证书
     * <p>
     * 证书中的公钥用于加密 文件加密密钥
     */
    private final Certificate certificate;

    /**
     * 用户名称
     */
    private String username;
    /**
     * 用户角色类型
     * <p>
     * 当时文档管理员时取值为Owner  {@link UserInfo#UserTypeOwner}
     * 普通用户取值为User  {@link UserInfo#UserTypeUser}，默认为User。
     */
    private String userType;


    /**
     * OFD用户数字证书加密器
     *
     * @param username    用户名称
     * @param userType    用户角色类型 {@link UserInfo#UserTypeOwner}  {@link UserInfo#UserTypeUser}
     * @param certificate 用户数字证书
     */
    public UserCertEncryptor(@NotNull String username, String userType, @NotNull Certificate certificate) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("用户名称(username)为空");
        }
        if (certificate == null) {
            throw new IllegalArgumentException("用户数字证书(certificate)为空");
        }
        this.username = username;
        this.userType = userType;
        this.certificate = certificate;
    }

    /**
     * OFD用户数字证书加密器
     * <p>
     * 用户类型：User(用户)  {@link UserInfo#UserTypeUser}
     *
     * @param username    用户名称
     * @param certificate 用户数字证书
     */
    public UserCertEncryptor(@NotNull String username, @NotNull Certificate certificate) {
        this(username, null, certificate);
    }


    /**
     * 加密 文件加密密钥 并封装为
     *
     * @param fek 文件加密密钥（File Encrypt Key ）
     * @param iv  加密向量IV
     * @return 用户信息（包含加密的文件加密密钥）
     * @throws CryptoException 加密过程运行异常
     */
    @Override
    public UserInfo encrypt(byte[] fek, byte[] iv) throws CryptoException, IOException, GeneralSecurityException {
        PublicKey publicKey = this.certificate.getPublicKey();
        final ECPublicKeyParameters publicKeyParameters = (ECPublicKeyParameters) ECUtil.generatePublicKeyParameter(publicKey);
        final SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
        engine.init(true, new ParametersWithRandom(publicKeyParameters));
        byte[] c1c3c2 = engine.processBlock(fek, 0, fek.length);
        final SM2Cipher sm2Cipher = SM2Cipher.fromC1C3C2(c1c3c2);
        // 创建 可解密该次操作的用户
        UserInfo userInfo = new UserInfo().setUserName(this.username);
        if (this.userType != null) {
            userInfo.setUserType(this.userType);
        }
        userInfo.setIVValue(iv);
        userInfo.setEncryptedWK(sm2Cipher.getEncoded());
        return userInfo;
    }

    /**
     * 用户加密时使用的证书，仅在使用证书加密的加密器中需要实现
     *
     * @return 证书文件字节内容（DER编码），在使用口令加密时可返还null
     */
    @Override
    public byte[] userCert() throws GeneralSecurityException {
        return this.certificate.getEncoded();
    }

    /**
     * 加密保护方案标识，见附录 A.1 {@link ProtectionCaseID}
     *
     * @return 加密保护方案标识
     */
    @Override
    public @NotNull String encryptCaseId() {
        return ProtectionCaseID.EncryptGMCert.getId();
    }
}
