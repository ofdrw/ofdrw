package org.ofdrw.sign.signContainer;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.gm.sm2strut.ContentInfo;
import org.ofdrw.gm.sm2strut.OIDs;
import org.ofdrw.gm.sm2strut.SignedData;
import org.ofdrw.gm.sm2strut.builder.SignedDataBuilder;
import org.ofdrw.sign.ExtendSignatureContainer;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;

/**
 * 根据 GM/T 0099-2020 7.2.2 数据格式要求
 * <p>
 * b) 签名类型为数字签名且签名算法使用SM2时，签名值数据应遵循 GB/T 35275
 *
 * @author 权观宇
 * @since 2021-8-9 16:15:16
 */
public class GBT35275DSContainer implements ExtendSignatureContainer {

    /**
     * 签名私钥
     */
    private final PrivateKey prvKey;

    /**
     * 私钥对应公钥的证书
     */
    private final Certificate cert;

    /**
     * 是否需要将文件Hash值进行Base64编码
     * <p>
     * 该参数用于兼容非规范的签名原文被Base64编码的待签名原文
     */
    private boolean enableFileHashBase64;

    /**
     * 创建数字签名容器
     * <p>
     * 签名值数据应遵循 GB/T 35275
     *
     * @param cert   SM2签名证书，应符合GB/T 20518
     * @param prvKey 私钥
     */
    public GBT35275DSContainer(@NotNull Certificate cert, @NotNull PrivateKey prvKey) {
        if (cert == null) {
            throw new IllegalArgumentException("签名使用证书（cert）不能为空");
        }
        if (prvKey == null) {
            throw new IllegalArgumentException("签名使用私钥（prvKey）不能为空");
        }
        this.cert = cert;
        this.prvKey = prvKey;
        this.enableFileHashBase64 = false;
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
        // 计算原文摘要
        MessageDigest md = new SM3.Digest();
        // d) 调用杂凑算法计算签名文件的杂凑值
        byte[] plaintext = md.digest(IOUtils.toByteArray(inData));
        if (this.enableFileHashBase64) {
            plaintext = Base64.encode(plaintext);
        }

        // e) 根据签名方案，使用操作人签名的私钥对杂凑值进行数字签名
        Signature signatureFnc = Signature.getInstance(
                GMObjectIdentifiers.sm2sign_with_sm3.toString(),
                new BouncyCastleProvider());
        signatureFnc.initSign(prvKey);
        signatureFnc.update(plaintext);
        // 执行签名产生签名值
        final byte[] signature = signatureFnc.sign();
        final SignedData signedData = SignedDataBuilder.signedData(plaintext, signature, this.cert);
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

    /**
     * 是否对文件摘要值进行Base64编码
     * <p>
     * Base64编码后的内容将会为待签名原文被签名
     * <p>
     * 该开关用于兼容部分阅读器只支持签名原文的文件Hash的Base64的情况。
     *
     * @param state 开关，true - 开启、false - 关闭
     */
    public void setEnableFileHashBase64(boolean state) {
        this.enableFileHashBase64 = state;
    }
}
