package org.ofdrw.crypto.integrity;

import java.nio.file.Path;
import java.security.GeneralSecurityException;

/**
 * 用于完整性保护协议的签名实现
 *
 * @author 权观宇
 * @since 2021-08-17 19:47:04
 */
@FunctionalInterface
public interface ProtectSigner {
    /**
     * 执行摘要和签名
     * @param tbs 待签名文件路径
     * @return 签名值应符合“GB/T 35275”标准
     * @throws GeneralSecurityException
     */
    byte[] sign(Path tbs) throws GeneralSecurityException;
}
