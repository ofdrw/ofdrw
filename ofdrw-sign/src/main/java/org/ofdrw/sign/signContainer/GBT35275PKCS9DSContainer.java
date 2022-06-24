package org.ofdrw.sign.signContainer;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.gm.sm2strut.ContentInfo;
import org.ofdrw.gm.sm2strut.OIDs;
import org.ofdrw.gm.sm2strut.SignedData;
import org.ofdrw.gm.sm2strut.builder.PKCS9SignedDataBuilder;
import org.ofdrw.sign.ExtendSignatureContainer;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;

/**
 * 根据 GM/T 0099-2020 及 PKCS#7 CMS 签名
 * <p>
 * 注该方法与{@link GBT35275DSContainer} 不同，签名对象为SignInfo中的 authenticatedAttributes字段，
 * authenticatedAttributes为一个CMS PKCS#9表示含签名时间、原文杂凑值的键值对结构。
 * <p>
 * 该格式为数科验证兼容格式。
 *
 * @author 权观宇
 * @since 2022-6-24 21:42:59
 */
public class GBT35275PKCS9DSContainer implements ExtendSignatureContainer {

    /**
     * 签名私钥
     */
    private final PrivateKey prvKey;

    /**
     * 私钥对应公钥的证书
     */
    private final Certificate cert;

    /**
     * 创建数字签名容器
     * <p>
     * 签名值数据应遵循 GB/T 35275
     *
     * @param cert   SM2签名证书，应符合GB/T 20518
     * @param prvKey 私钥
     */
    public GBT35275PKCS9DSContainer(@NotNull Certificate cert, @NotNull PrivateKey prvKey) {
        if (cert == null) {
            throw new IllegalArgumentException("签名使用证书（cert）不能为空");
        }
        if (prvKey == null) {
            throw new IllegalArgumentException("签名使用私钥（prvKey）不能为空");
        }
        this.cert = cert;
        this.prvKey = prvKey;
    }

    /**
     * SM3摘要算法功能
     *
     * @return SM3摘要算法功能
     */
    @Override
    public MessageDigest getDigestFnc() {
        return new SM3.Digest();
    }

    /**
     * SM2WithSM3
     *
     * @return 签名方法OID
     */
    @Override
    public ASN1ObjectIdentifier getSignAlgOID() {
        return GMObjectIdentifiers.sm2sign_with_sm3;
    }

    /**
     * 对待签名数据签名
     *
     * @param inData       待签名数据流
     * @param propertyInfo 忽略
     * @return 签名结果值
     * @throws IOException       IO流读取异常
     * @throws SecurityException 签名计算异常
     */
    @Override
    public byte[] sign(InputStream inData, String propertyInfo) throws GeneralSecurityException, IOException {
        // e) 根据签名方案，使用操作人签名的私钥对杂凑值进行数字签名
        Signature signatureFnc = Signature.getInstance(
                GMObjectIdentifiers.sm2sign_with_sm3.toString(),
                new BouncyCastleProvider());
        signatureFnc.initSign(prvKey);
        byte[] plaintext = IOUtils.toByteArray(inData);
        // 执行签名产生签名值
        final SignedData signedData = PKCS9SignedDataBuilder.signedData(plaintext, signatureFnc, this.cert, null);
        ContentInfo contentInfo = new ContentInfo(OIDs.signedData, signedData);
        return contentInfo.getEncoded();
    }

    /**
     * 电子签名不提供印章
     *
     * @return null
     * @throws IOException 获取印章IO异常
     */
    @Override
    public byte[] getSeal() throws IOException {
        return null;
    }

    /**
     * 获取签名节点类型
     *
     * @return 签名节点类型
     */
    @Override
    public SigType getSignType() {
        return SigType.Sign;
    }
}
