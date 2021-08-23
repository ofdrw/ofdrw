package org.ofdrw.crypto.integrity;

import java.io.IOException;
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
     * 根据 GM/T 0099  OFD完整性保护协议 执行完整性保护 7.4.3 中的 c) d) 步骤
     * <p>
     * c) 根据签名方案，计算完整性保护文件的杂凑值；
     * d) 根据签名方案，使用版式文件合成者的签名私钥对杂凑值进行数字签名；
     *
     * @param tbs 待签名文件路径
     * @return 签名值应符合“GB/T 35275”标准
     * @throws GeneralSecurityException 安全计算异常
     * @throws IOException IO读写异常
     */
    byte[] digestThenSign(Path tbs) throws GeneralSecurityException, IOException;
}
