package org.ofdrw.gm.sm2strut.builder;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.gm.cert.CertTools;
import org.ofdrw.gm.sm2strut.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * 签名数据类型构造器
 * <p>
 * 符合 GBT35275 8 签名数据类型 signedData
 * <p>
 * 签名对象为 signerInfos中的authenticatedAttributes字段（内部结构为 PKCS#9）
 *
 * @author 权观宇
 * @since 2022-6-24 22:37:33
 */
public final class PKCS9SignedDataBuilder {

    /**
     * 组装 签名数据类型
     *
     * @param plaintext  需要保护原文
     * @param signFnc    签名实现，已经初始化。
     * @param signCert   签名使用的证书
     * @param extCertArr 扩展证书序列，如根证书等。
     * @return SignedData
     * @throws GeneralSecurityException 证书解析异常
     * @throws IOException              IO操作异常
     */
    public static SignedData signedData(@NotNull byte[] plaintext,
                                        @NotNull Signature signFnc,
                                        @NotNull Certificate signCert,
                                        @Nullable List<Certificate> extCertArr) throws GeneralSecurityException, IOException {
        if (plaintext == null || plaintext.length == 0) {
            throw new IllegalArgumentException("签名原文(plaintext)为空");
        }
        if (signCert == null) {
            throw new IllegalArgumentException("证书(signCert)为空");
        }
        if (signFnc == null) {
            throw new IllegalArgumentException("签名函数(signFnc)为空");
        }

        // 消息摘要算法标识符的集合,固定值 SM3算法
        ASN1Set digestAlgorithms = new DERSet(new AlgorithmIdentifier(OIDs.sm3));
        // 待签名的 数据内容
        ContentInfo contentInfo = new ContentInfo(OIDs.data, null);
        MessageDigest md = new SM3.Digest();
        // 计算待保护内容的摘要
        byte[] digest = md.digest(plaintext);


        int len = 1;
        int i = 0;
        if (extCertArr != null) {
            len += extCertArr.size();
        }
        ASN1Encodable[] certArr = new ASN1Encodable[len];
        final org.bouncycastle.asn1.x509.Certificate signerCert = CertTools.asn1(signCert);
        certArr[0] = signerCert;
        if (extCertArr != null) {
            // 如果附加的证书不为空，那么追加到证书列表
            for (Certificate c : extCertArr) {
                certArr[i] = CertTools.asn1(c);
                i++;
            }
        }
        ASN1Set certificates = new DERSet(certArr);
        // 签名证书ID
        IssuerAndSerialNumber isn = new IssuerAndSerialNumber(signerCert.getIssuer(), signerCert.getSerialNumber());
        // 签名生成签名者信息
        SignerInfo signerInfo = sign(digest, signFnc, isn);

        ASN1Set signerInfos = new DERSet(new ASN1Encodable[]{signerInfo});
        // 组装对象
        return new SignedData(digestAlgorithms, contentInfo, certificates, signerInfos);
    }

    /**
     * 构造签名者信息
     *
     * @param digest  需要保护的原文
     * @param signFnc 签名实现
     * @param isn     签名者公钥证书序列号
     * @return 签名者信息
     * @throws GeneralSecurityException 安全计算异常
     */
    public static SignerInfo sign(byte[] digest, Signature signFnc, IssuerAndSerialNumber isn) throws GeneralSecurityException {
        /*
            authenticatedAttributes属性为 Attributes （键值对）
            每对键值对使用一个Sequence描述，该Sequence的键为OID，值为Set

            authenticatedAttributes ::= Attributes
            Attributes ::= SET OF Attribute
            Attribute ::= SEQUENCE {
                attrType OBJECT IDENTIFIER,
                attrValues SET OF AttributeValue
            }

            AttributeValue ::= ANY

            PKCS#9 OID 见 {@link org.bouncycastle.asn1.cms.CMSAttributes}
         */
        Attribute contentType = new Attribute(CMSAttributes.contentType, new DERSet(OIDs.data));
        Attribute signingTime = new Attribute(CMSAttributes.signingTime, new DERSet(new ASN1UTCTime(new Date(), Locale.CHINA)));
        Attribute messageDigest = new Attribute(CMSAttributes.messageDigest, new DERSet(new DEROctetString(digest)));

        // 构造 authenticatedAttributes
        ASN1Set authenticatedAttributes = new DERSet(new ASN1Encodable[]{
                contentType,
                signingTime,
                messageDigest
        });

        try {
            signFnc.update(authenticatedAttributes.getEncoded());
        } catch (IOException e) {
            // ignore
        }
       byte[] signature =  signFnc.sign();

        SignerInfo res =  new SignerInfo(
                isn,
                new AlgorithmIdentifier(OIDs.sm3), // 固定SM3算法
                new AlgorithmIdentifier(OIDs.sm2Sign), // 固定 SM2椭圆曲线数字签名算法
                new DEROctetString(signature)
        );
        res.setAuthenticatedAttributes(authenticatedAttributes);

        return res;
    }
}
