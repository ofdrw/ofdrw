package org.ofdrw.crypto.decryptor;

import org.bouncycastle.crypto.CryptoException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.crypto.encryt.UserInfo;

/**
 * 用户 文件加密密钥 解密器 User File Encrypt Key Decryptor
 * <p>
 * 用于从 {@link UserInfo} 中解密出文件加密密钥(FEK)和初始化向量(IV)。
 * 与 {@link org.ofdrw.crypto.enryptor.UserFEKEncryptor} 互为逆操作。
 * <p>
 * OFD 解密流程中，遍历密钥描述文件中的 UserInfo 列表，
 * 通过 {@link #encryptCaseId()} 和 {@link #getUsername()} 匹配对应的解密器，
 * 调用 {@link #decrypt(UserInfo)} 恢复 FEK 和 IV。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public interface UserFEKDecryptor {

    /**
     * 从 UserInfo 中解密出文件加密密钥和初始化向量
     * <p>
     * 根据加密方案的不同，解密方式也不同：
     * <ul>
     *   <li>口令加密（EncryptGMPassword）：KDF派生KEK → SM4-CBC解密 EncryptedWK → FEK</li>
     *   <li>证书加密（EncryptGMCert）：SM2私钥解密 C1C3C2密文 → FEK</li>
     * </ul>
     *
     * @param userInfo 加密的用户信息（含被包装的 FEK 和 IV）
     * @return 解密结果，包含 FEK 和 IV（均为16字节）
     * @throws CryptoException 解密过程异常（如密钥不匹配、口令错误）
     */
    DecryptResult decrypt(@NotNull UserInfo userInfo) throws CryptoException;

    /**
     * 加密保护方案标识
     * <p>
     * 必须与加密时使用的方案标识一致，参见 {@link org.ofdrw.core.crypto.ProtectionCaseID}：
     * <ul>
     *   <li>"1.1.1" — 口令加密（EncryptGMPassword）</li>
     *   <li>"1.1.2" — 证书加密（EncryptGMCert）</li>
     * </ul>
     * <p>
     * 解密时用于与密钥描述文件（DecyptSeed）中的 EncryptCaseId 进行匹配。
     *
     * @return 加密保护方案标识
     */
    @NotNull String encryptCaseId();

    /**
     * 目标用户名
     * <p>
     * 用于与 UserInfo 中的 UserName 进行匹配，定位该用户对应的加密信息。
     * 返回 null 时表示匹配第一个 UserInfo（不按用户名过滤）。
     *
     * @return 目标用户名，可为 null
     */
    @Nullable String getUsername();
}
