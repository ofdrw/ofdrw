package org.ofdrw.gm.cert;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

/**
 * 证书转换工具
 *
 * @author 权观宇
 * @since 2021-08-05 19:03:14
 */
public final class CertTools {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 转换证书对象为 ASN1结构对象
     *
     * @param certificate JCE证书对象
     * @return ASN1证书结构
     * @throws CertificateEncodingException 证书编码异常
     * @throws IOException                  IO读写异常
     */
    public static Certificate asn1(java.security.cert.Certificate certificate) throws CertificateEncodingException, IOException {
        ASN1Primitive p = ASN1Primitive.fromByteArray(certificate.getEncoded());
        if (p == null) {
            throw new IllegalArgumentException("无法解析证书(certificate)");
        }
        return org.bouncycastle.asn1.x509.Certificate.getInstance(p);
    }

    /**
     * 转换 ASN1结构对象 为 证书对象
     *
     * @param certificate JCE证书对象
     * @return ASN1证书结构
     * @throws CertificateException 证书解析异常
     */
    public static java.security.cert.Certificate obj(Certificate certificate) throws CertificateException {
        return new JcaX509CertificateConverter().setProvider("BC")
                .getCertificate(new X509CertificateHolder(certificate));
    }
}
