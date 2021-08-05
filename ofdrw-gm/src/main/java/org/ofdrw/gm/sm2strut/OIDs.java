package org.ofdrw.gm.sm2strut;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

/**
 * GMT 35275-2017 5. OID 定义
 * <p>
 * GMT 33560-2017 附录A 商用密码领域中的相关OID定义
 * <p>
 * 表 1 对象标识符
 *
 * @author 权观宇
 * @since 2021-08-02 19:26:18
 */
public final class OIDs {
    /*
                    GMT 35275-2017 5. OID 定义
     */

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
    public static final ASN1ObjectIdentifier signedData = gmt35275_sm2.branch("2").intern();

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

    /*
                GMT 33560-2017 附录 A 部分
     */

    /**
     * SM4分组密码算法
     */
    public static final ASN1ObjectIdentifier sm4 = new ASN1ObjectIdentifier("1.2.156.10197.1.100");

    /**
     * SM2椭圆曲线公钥密码算法
     */
    public static final ASN1ObjectIdentifier sm2 = new ASN1ObjectIdentifier("1.2.156.10197.1.301");
    /**
     * SM2-1 数字签名算法
     */
    public static final ASN1ObjectIdentifier sm2Sign = sm2.branch("1").intern();
    /**
     * SM2-2 密钥交换协议
     */
    public static final ASN1ObjectIdentifier sm2KeyExchange = sm2.branch("2").intern();
    /**
     * SM2-3 公钥加密算法
     */
    public static final ASN1ObjectIdentifier sm2Encrypt = sm2.branch("3").intern();


    /**
     * SM3 密码杂凑算法
     */
    public static final ASN1ObjectIdentifier sm3 = new ASN1ObjectIdentifier("1.2.156.10197.1.401");

    /**
     * SM3 密码杂凑算法，无密钥使用
     */
    public static final ASN1ObjectIdentifier sm3_1 = sm3.branch("1");

    /**
     * SM3 密码杂凑算法，有密钥使用
     */
    public static final ASN1ObjectIdentifier sm3_2 = sm3.branch("1");


    /**
     * 基于SM2算法和SM3算法的签名
     */
    public static final ASN1ObjectIdentifier SM3withSM2 = new ASN1ObjectIdentifier("1.2.156.10197.501");
}
