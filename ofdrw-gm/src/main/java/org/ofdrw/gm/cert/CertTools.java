package org.ofdrw.gm.cert;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x509.Certificate;
import org.ofdrw.gm.sm2strut.IssuerAndSerialNumber;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;

/**
 * 证书转换工具
 *
 * @author 权观宇
 * @since 2021-08-05 19:03:14
 */
public final class CertTools {

    /**
     * 转换证书对象为 ASN1结构对象
     *
     * @param certificate JCE证书对象
     * @return ASN1证书结构
     */
    public static Certificate asn1(java.security.cert.Certificate certificate) throws CertificateEncodingException, IOException {
        ASN1Primitive p = ASN1Primitive.fromByteArray(certificate.getEncoded());
        if (p == null) {
            throw new IllegalArgumentException("无法解析证书(certificate)");
        }
        return org.bouncycastle.asn1.x509.Certificate.getInstance(p);
    }
}
