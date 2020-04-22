package org.ofdrw.sign.signContainer;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.sign.ExtendSignatureContainer;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;

/**
 * 国密SM2withSM3数字签名实现容器
 *
 * @author 权观宇
 * @since 2020-04-20 12:26:33
 */
public class DigitalSignContainer implements ExtendSignatureContainer {

    /**
     * 签名私钥
     */
    private final PrivateKey prvKey;

    public DigitalSignContainer(PrivateKey prvKey) {
        if (prvKey == null) {
            throw new IllegalArgumentException("签名使用私钥（prvKey）不能为空");
        }
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
        Signature signatureFnc = Signature.getInstance(
                GMObjectIdentifiers.sm2sign_with_sm3.toString(),
                new BouncyCastleProvider());
        signatureFnc.initSign(prvKey);
        signatureFnc.update(IOUtils.toByteArray(inData));
        return signatureFnc.sign();
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
