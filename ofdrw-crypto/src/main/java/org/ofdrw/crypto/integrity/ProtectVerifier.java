package org.ofdrw.crypto.integrity;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

/**
 * 实现该接口 用于实现 GM/T 0099 7.4.6 校验流程
 * <p>
 * 中签名方案验证内容
 *
 * @author 权观宇
 * @since 2021-08-24 19:34:22
 */
@FunctionalInterface
public interface ProtectVerifier {

    /**
     * 根据 GM/T 0099  OFD完整性保护协议 执行完整性保护 7.4.6
     * <p>
     * b) 根据签名方案，调用杂凑算法计算完整性保护文件得到杂凑值
     * c) 读取签名值文件，进行签名验证
     *
     * @param ofdEntriesXmlPath 防夹带文件路径
     * @param signedValue 待验证的签名值，数据结构参照 《GM/T 0099 2020》 7.4.3 密码算法要求
     * @return 签名值验证结果
     * @throws GeneralSecurityException 安全计算异常
     * @throws IOException              IO读写异常
     */
    boolean digestThenVerify(Path ofdEntriesXmlPath,byte[] signedValue) throws GeneralSecurityException, IOException;
}
