package org.ofdrw.sign.verify.container;

import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.sign.verify.SignedDataValidateContainer;
import org.ofdrw.sign.verify.exceptions.InvalidSignedValueException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * 数字签名验证容器
 *
 * @author 权观宇
 * @since 2020-04-22 03:22:22
 */
public class DigitalSignContainer implements SignedDataValidateContainer {
    /**
     * 验证使用的公钥
     */
    public PublicKey pk;

    public DigitalSignContainer(PublicKey pk) {
        if (pk == null) {
            throw new IllegalArgumentException("验证使用的公钥参数(pk)不能为空");
        }
        this.pk = pk;
    }

    public DigitalSignContainer(Certificate certificate) {
        this(certificate.getPublicKey());
    }

    @Override
    public void validate(SigType type, String alg, byte[] tbsContent, byte[] signedValue)
            throws InvalidSignedValueException, GeneralSecurityException {
        if (type != SigType.Sign) {
            throw new IllegalArgumentException("签名类型(type)必须是 Sign，不支持电子印章验证");
        }
        Signature sg = Signature.getInstance(alg, new BouncyCastleProvider());
        sg.initVerify(pk);
        if (!sg.verify(signedValue)) {
            throw new InvalidSignedValueException("签名值不一致");
        }
    }
}
