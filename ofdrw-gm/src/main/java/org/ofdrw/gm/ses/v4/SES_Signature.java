package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.*;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Enumeration;

/**
 * 电子签章数据
 *
 * @author 权观宇
 * @since 2020-04-19 22:04:04
 */
public class SES_Signature extends ASN1Object {

    /**
     * 签章信息
     */
    private TBS_Sign toSign;

    /**
     * 签章者证书
     */
    private ASN1OctetString cert;

    /**
     * 签名算法标识
     */
    private ASN1ObjectIdentifier signatureAlgID;

    /**
     * 签名值
     */
    private ASN1BitString signature;

    /**
     * 对签名值的时间戳【可选】
     */
    private ASN1BitString timeStamp;

    public SES_Signature() {
        super();
    }

    public SES_Signature(TBS_Sign toSign,
                         ASN1OctetString cert,
                         ASN1ObjectIdentifier signatureAlgID,
                         ASN1BitString signature,
                         ASN1BitString timeStamp) {
        this.toSign = toSign;
        this.cert = cert;
        this.signatureAlgID = signatureAlgID;
        this.signature = signature;
        this.timeStamp = timeStamp;
    }

    public SES_Signature(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        toSign = TBS_Sign.getInstance(e.nextElement());
        cert = ASN1OctetString.getInstance(e.nextElement());
        signatureAlgID = ASN1ObjectIdentifier.getInstance(e.nextElement());
        signature = DERBitString.getInstance(e.nextElement());
        if (e.hasMoreElements()) {
            Object obj = e.nextElement();
            if (obj instanceof ASN1TaggedObject) {
                timeStamp = DERBitString.getInstance(((ASN1TaggedObject) obj).getObject());
            }
        }
    }

    public static SES_Signature getInstance(Object o) {
        if (o instanceof SES_Signature) {
            return (SES_Signature) o;
        } else if (o != null) {
            return new SES_Signature(ASN1Sequence.getInstance(o));
        }
        return null;
    }

    public TBS_Sign getToSign() {
        return toSign;
    }

    public SES_Signature setToSign(TBS_Sign toSign) {
        this.toSign = toSign;
        return this;
    }

    public ASN1OctetString getCert() {
        return cert;
    }

    public SES_Signature setCert(ASN1OctetString cert) {
        this.cert = cert;
        return this;
    }
    public SES_Signature setCert(Certificate cert) throws CertificateEncodingException {
        this.cert = new DEROctetString(cert.getEncoded());
        return this;
    }

    public ASN1ObjectIdentifier getSignatureAlgID() {
        return signatureAlgID;
    }

    public SES_Signature setSignatureAlgID(ASN1ObjectIdentifier signatureAlgID) {
        this.signatureAlgID = signatureAlgID;
        return this;
    }

    public ASN1BitString getSignature() {
        return signature;
    }

    public SES_Signature setSignature(ASN1BitString signature) {
        this.signature = signature;
        return this;
    }

    public SES_Signature setSignature(byte[] signature) {
        this.signature = new DERBitString(signature);
        return this;
    }

    public ASN1BitString getTimeStamp() {
        return timeStamp;
    }

    public SES_Signature setTimeStamp(ASN1BitString timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(5);
        v.add(toSign);
        v.add(cert);
        v.add(signatureAlgID);
        v.add(signature);
        if (timeStamp != null) {
            v.add(new DERTaggedObject(true, 0, timeStamp));
        }
        return new DERSequence(v);
    }
}
