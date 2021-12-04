package org.ofdrw.gm.ses.v1;

import org.bouncycastle.asn1.*;

import java.util.Enumeration;

/**
 * 印章签名信息
 *
 * @author 权观宇
 * @since 2020-04-19 14:43:39
 */
public class SES_SignInfo extends ASN1Object {

    /**
     * 代表对电子印章数据进行签名的制章人证书
     */
    private ASN1OctetString cert;

    /**
     * 代表签名算法OID标识
     * <p>
     * 遵循 GM/T 006
     */
    private ASN1ObjectIdentifier signatureAlgorithm;

    /**
     * 制章人的签名值
     * <p>
     * 制章人对电子印章格式中印章信息SES_SealInfo、制章人证书、签名算法标识符按 SEQUENCE方式组成的信息内容的数字签名
     */
    private ASN1BitString signData;

    public SES_SignInfo() {
        super();
    }

    public static SES_SignInfo getInstance(Object o) {
        if (o instanceof SES_SignInfo) {
            return (SES_SignInfo) o;
        } else if (o instanceof DERBitString) {
            return new SES_SignInfo(ASN1Sequence.getInstance(((DERBitString) o).getOctets()));
        } else if (o != null) {
            return new SES_SignInfo(ASN1Sequence.getInstance(o));
        }
        return null;
    }

    public SES_SignInfo(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        cert = ASN1OctetString.getInstance(e.nextElement());
        signatureAlgorithm = ASN1ObjectIdentifier.getInstance(e.nextElement());
        signData = DERBitString.getInstance(e.nextElement());
    }


    public SES_SignInfo(ASN1OctetString cert, ASN1ObjectIdentifier signatureAlgorithm, ASN1BitString signData) {
        this.cert = cert;
        this.signatureAlgorithm = signatureAlgorithm;
        this.signData = signData;
    }

    public ASN1OctetString getCert() {
        return cert;
    }

    public SES_SignInfo setCert(ASN1OctetString cert) {
        this.cert = cert;
        return this;
    }

    public ASN1ObjectIdentifier getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public SES_SignInfo setSignatureAlgorithm(ASN1ObjectIdentifier signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
        return this;
    }

    public ASN1BitString getSignData() {
        return signData;
    }

    public SES_SignInfo setSignData(ASN1BitString signData) {
        this.signData = signData;
        return this;
    }

    public SES_SignInfo setSignData(byte[] signData) {
        this.signData = new DERBitString(signData);
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(3);
        v.add(cert);
        v.add(signatureAlgorithm);
        v.add(signData);
        return new DERSequence(v);
    }
}
