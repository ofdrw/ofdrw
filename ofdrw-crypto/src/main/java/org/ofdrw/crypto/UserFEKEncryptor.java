package org.ofdrw.crypto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.crypto.ProtectionCaseID;
import org.ofdrw.core.crypto.encryt.UserInfo;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * 用户 文件加密密钥 加密器 User File Encrypt Key Encryptor
 * <p>
 * 用于加密 文件加密密钥 生成 {@link org.ofdrw.core.crypto.encryt.UserInfo}
 *
 * @author 权观宇
 * @since 2021-07-13 19:07:11
 */
public interface UserFEKEncryptor {

    /**
     * 加密 文件加密密钥 并封装为
     *
     * @param username 用户名
     * @param userType 用户类型，默认为 User（用户）
     * @param fek      文件加密密钥（File Encrypt Key ）
     * @return 用户信息（包含加密的文件加密密钥）
     * @throws GeneralSecurityException 加密过程运行异常
     * @throws IOException              IO操作异常
     */
    UserInfo encrypt(@NotNull String username, @Nullable String userType, @NotNull String fek) throws GeneralSecurityException, IOException;

    /**
     * 用户加密时使用的证书，仅在使用证书加密的加密器中需要实现
     *
     * @return 证书文件字节内容（DER编码），在使用口令加密时可返还null
     */
    byte[] userCert();

    /**
     * 加密保护方案标识，见附录 A.1 {@link ProtectionCaseID}
     *
     * @return 加密保护方案标识
     */
    @NotNull
    String encryptCaseId();
}
