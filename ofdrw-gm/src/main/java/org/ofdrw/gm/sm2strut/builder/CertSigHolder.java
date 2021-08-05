package org.ofdrw.gm.sm2strut.builder;

import org.ofdrw.gm.cert.CertTools;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

/**
 * 证书和签名值封装
 * <p>
 * 更加便于理解和操作
 *
 * @author 权观宇
 * @since 2021-08-05 18:39:48
 */
public class CertSigHolder {
    /**
     * 签名值
     */
    byte[] signature;

    /**
     * 证书对象
     */
    Certificate certificate;

    /**
     * ASN1证书对象结构
     */
    private org.bouncycastle.asn1.x509.Certificate asn1Obj;

    public CertSigHolder(byte[] signature, Certificate certificate) {
        this.signature = signature;
        this.certificate = certificate;
        this.asn1Obj = null;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    /**
     * 转换证书对象为ASN1结构对象
     *
     * @return 证书 ASN1结构
     * @throws CertificateEncodingException 证书编码异常
     * @throws IOException                  IO读写异常
     */
    public org.bouncycastle.asn1.x509.Certificate getAsn1Cert() throws CertificateEncodingException, IOException {
        if (this.certificate == null) {
            throw new IllegalArgumentException("证书对象(certificate)为空");
        }
        if (asn1Obj != null) {
            return asn1Obj;
        }
        asn1Obj = CertTools.asn1(this.certificate);
        return asn1Obj;
    }
}
