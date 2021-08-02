package org.ofdrw.gm.sm2strut;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

/**
 * GMT 35275-2017 5. OID 定义
 *
 * 表 1 对象标识符
 *
 * @author 权观宇
 * @since 2021-08-02 19:26:18
 */
public final class OIDs {
    /**
     * SM2密码算法加密签名消息语法规范
     */
    public static final ASN1ObjectIdentifier gmt35275_sm2 = new ASN1ObjectIdentifier("1.2.156.10197.6.1.4.2");

    /**
     * 数据类型
     */
     public static final ASN1ObjectIdentifier data = gmt35275_sm2.branch("1").intern();
    /**
     * 签名数据类型
     */
    public static final ASN1ObjectIdentifier signedData =gmt35275_sm2.branch("2").intern();

    /**
     * 数字信封类型
     */
    public static final ASN1ObjectIdentifier envelopedData = gmt35275_sm2.branch("3").intern();

    /**
     * 签名及数字信封类型
     */
    public static final ASN1ObjectIdentifier signedAndEnvelopedData = gmt35275_sm2.branch("4").intern();

    /**
     * 加密数据类型
     */
    public static final ASN1ObjectIdentifier encryptedData = gmt35275_sm2.branch("5").intern();

    /**
     * 密钥协商类型
     */
    public static final ASN1ObjectIdentifier keyAgreementInfo = gmt35275_sm2.branch("6").intern();
}
