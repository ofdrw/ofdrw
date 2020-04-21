package org.ofdrw.sign;


import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.ofdrw.core.signatures.SigType;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

/**
 * 扩展数字签名容器
 *
 * @author 权观宇
 * @since 2020-04-18 12:09:57
 */
public interface ExtendSignatureContainer {

    /**
     * 提供文件的摘要算法功能
     *
     * @return 摘要算法功能
     */
    MessageDigest getDigestFnc();

    /**
     * 签名方法OID
     *
     * @return 签名方法OID
     */
    ASN1ObjectIdentifier getSignAlgOID();

    /**
     * 对待签名数据签名
     * <p>
     * 在操作过程中请勿对流进行关闭
     *
     * @param inData       待签名数据流
     * @param propertyInfo 签章属性信息
     * @return 签名或签章结果值
     * @throws IOException              流操作异常
     * @throws GeneralSecurityException 签名计算异常
     */
    byte[] sign(InputStream inData, String propertyInfo) throws IOException, GeneralSecurityException;

    /**
     * 获取电子印章二进制编码
     * <p>
     * 如果{@link #getSignType()} 返还类型为{@link SigType#Sign}那么请返回null
     *
     * @return 电子印章二进制编码
     * @throws IOException 获取印章IO异常
     */
    byte[] getSeal() throws IOException;

    /**
     * 获取签名节点类型
     *
     * @return 签名节点类型
     */
    SigType getSignType();
}
