package org.ofdrw.sign.verify.container;

import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ofdrw.core.signatures.SigType;

import org.ofdrw.gm.ses.v1.SES_Signature;
import org.ofdrw.gm.ses.v1.TBS_Sign;
import org.ofdrw.sign.verify.SignedDataValidateContainer;
import org.ofdrw.sign.verify.exceptions.InvalidSignedValueException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Arrays;

/**
 * 《《GM/T 0031-2014 安全电子签章密码技术规范》 电子印章数据验证
 * <p>
 * 注意：仅用于测试，电子签章验证请使用符合国家规范的流程进行！
 *
 * @author 权观宇
 * @since 2020-04-22 22:56:23
 */
public class SESV1ValidateContainer implements SignedDataValidateContainer {

    @Override
    public void validate(SigType type,
                         String signAlgName,
                         byte[] tbsContent,
                         byte[] signedValue)
            throws InvalidSignedValueException, IOException, GeneralSecurityException {
        if (type == SigType.Sign) {
            throw new IllegalArgumentException("签名类型(type)必须是 Seal，不支持电子印章验证");
        }

        // 计算原文摘要
        MessageDigest md = new SM3.Digest();
        byte[] actualDataHash = md.digest(tbsContent);

        SES_Signature sesSignature = SES_Signature.getInstance(signedValue);
        TBS_Sign toSign = sesSignature.getToSign();
        byte[] expectDataHash = toSign.getDataHash().getOctets();


        // 比较原文摘要
        if (!Arrays.equals(actualDataHash, expectDataHash)) {
            throw new InvalidSignedValueException("Signature.xml 文件被篡改，电子签章失效。("
                    + toSign.getPropertyInfo().getString() + ")");
        }

        // 预期的电子签章数据，签章值
        byte[] expSigVal = sesSignature.getSignature().getOctets();

        Signature sg = Signature.getInstance( toSign.getSignatureAlgorithm().getId(),
                new BouncyCastleProvider());
        byte[] certDER =  toSign.getCert().getOctets();
        // 构造证书对象
        Certificate signCert = new CertificateFactory()
                .engineGenerateCertificate(new ByteArrayInputStream(certDER));
        sg.initVerify(signCert);
        sg.update(toSign.getEncoded("DER"));
        if (!sg.verify(expSigVal)) {
            throw new InvalidSignedValueException("电子签章数据签名值不匹配，电子签章数据失效。");
        }
    }
}
