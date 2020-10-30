package org.ofdrw.sign.timestamp;


import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * 时间戳Hook
 *
 * @author minghu-zhang
 * @since 2020-10-30 12:31:43
 */
@FunctionalInterface
public interface TimeStampHook {

    /**
     * 执行方法获取时间戳
     *
     * @param identifier 签名算法identifier
     * @param signature  签章签名值
     * @return 返回结果
     */
    ASN1BitString apply(AlgorithmIdentifier identifier, byte[] signature);
}
