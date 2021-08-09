package org.ofdrw.sign.verify.container;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.gm.cert.CertTools;
import org.ofdrw.gm.sm2strut.IssuerAndSerialNumber;
import org.ofdrw.gm.sm2strut.SignedData;
import org.ofdrw.gm.sm2strut.SignerInfo;
import org.ofdrw.sign.verify.SignedDataValidateContainer;
import org.ofdrw.sign.verify.exceptions.InvalidSignedValueException;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * 根据 GM/T 0099-2020 7.2.2 数据格式要求
 * <p>
 * b) 签名类型为数字签名且签名算法使用SM2时，签名值数据应遵循 GB/T 35275
 * <p>
 * 数字签名验证容器
 *
 * @author 权观宇
 * @since 2021-8-9 16:15:11
 */
public class GBT35275ValidateContainer implements SignedDataValidateContainer {

    public GBT35275ValidateContainer() {
    }

    @Override
    public void validate(SigType type, String alg, byte[] tbsContent, byte[] signedValue)
            throws InvalidSignedValueException, GeneralSecurityException {
        if (type != SigType.Sign) {
            throw new IllegalArgumentException("签名类型(type)必须是 Sign，不支持电子印章验证");
        }
        SignedData signedData = null;
        try {
            signedData = SignedData.getInstance(signedValue);
        } catch (Exception ignored) {
        }
        if (signedData == null) {
            throw new IllegalArgumentException("无法解析签名值格式，不符 GBT35275");
        }
        // 计算原文摘要
        MessageDigest md = new SM3.Digest();
        // a) 根据签名文件中的签名方案，调用杂凑算法计算签名文件的杂凑值。
        byte[] plaintextAct = md.digest(tbsContent);
        byte[] plaintext = DEROctetString.getInstance(signedData.getContentInfo().getContent()).getOctets();

        if (!Arrays.equals(plaintextAct, plaintext)) {
            throw new InvalidSignedValueException("待签名原文不符");
        }
        // b) 根据签名文件的签名方案，结合步骤 a) 所得的杂凑值进行签名验证。
        for (ASN1Encodable item : signedData.getSignerInfos()) {
            final SignerInfo signerInfo = SignerInfo.getInstance(item);
            IssuerAndSerialNumber iaSn = signerInfo.getIssuerAngSerialNumber();
            // 根据提供者信息找到证书
            final Certificate c = signedData.getSignCert(iaSn);
            if (c == null) {
                throw new InvalidSignedValueException("没有找到匹配的证书无法验证签名");
            }
            final java.security.cert.Certificate cert = CertTools.obj(c);
            Signature sg = Signature.getInstance(alg, new BouncyCastleProvider());
            sg.initVerify(cert.getPublicKey());
            sg.update(plaintext);
            byte[] signature = signerInfo.getEncryptedDigest().getOctets();
            if (!sg.verify(signature)) {
                throw new InvalidSignedValueException("签名值不一致");
            }
        }
    }
}
