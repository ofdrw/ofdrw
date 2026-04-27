package org.ofdrw.gm.ses.v5;

import org.bouncycastle.asn1.*;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Enumeration;

/**
 * 电子签章数据（V5）
 * <p>
 * 五字段：toSign + cert + signatureAlgID + signatureValue + timeStamp[0] OPTIONAL
 *
 * @since 2026-04-24
 * @author minghu.zhang
 */
public class SES_Signature extends ASN1Object {

    private TBS_Sign toSign;
    private ASN1OctetString cert;
    private ASN1ObjectIdentifier signatureAlgID;
    private ASN1BitString signatureValue;
    private ASN1BitString timeStamp;

    public SES_Signature() {
        super();
    }

    public SES_Signature(TBS_Sign toSign, ASN1OctetString cert,
                         ASN1ObjectIdentifier signatureAlgID, ASN1BitString signatureValue,
                         ASN1BitString timeStamp) {
        this.toSign = toSign;
        this.cert = cert;
        this.signatureAlgID = signatureAlgID;
        this.signatureValue = signatureValue;
        this.timeStamp = timeStamp;
    }

    public SES_Signature(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        toSign = TBS_Sign.getInstance(e.nextElement());
        cert = ASN1OctetString.getInstance(e.nextElement());
        signatureAlgID = ASN1ObjectIdentifier.getInstance(e.nextElement());
        signatureValue = DERBitString.getInstance(e.nextElement());
        if (e.hasMoreElements()) {
            Object obj = e.nextElement();
            if (obj instanceof ASN1TaggedObject) {
                timeStamp = DERBitString.getInstance(((ASN1TaggedObject) obj).getBaseObject());
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

    public TBS_Sign getToSign() { return toSign; }

    public SES_Signature setToSign(TBS_Sign toSign) {
        this.toSign = toSign;
        return this;
    }

    public ASN1OctetString getCert() { return cert; }

    public SES_Signature setCert(ASN1OctetString cert) {
        this.cert = cert;
        return this;
    }

    public SES_Signature setCert(Certificate cert) throws CertificateEncodingException {
        this.cert = new DEROctetString(cert.getEncoded());
        return this;
    }

    public ASN1ObjectIdentifier getSignatureAlgID() { return signatureAlgID; }

    public SES_Signature setSignatureAlgID(ASN1ObjectIdentifier signatureAlgID) {
        this.signatureAlgID = signatureAlgID;
        return this;
    }

    public ASN1BitString getSignatureValue() { return signatureValue; }

    public SES_Signature setSignatureValue(ASN1BitString signatureValue) {
        this.signatureValue = signatureValue;
        return this;
    }

    public SES_Signature setSignatureValue(byte[] signatureValue) {
        this.signatureValue = new DERBitString(signatureValue);
        return this;
    }

    public ASN1BitString getTimeStamp() { return timeStamp; }

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
        v.add(signatureValue);
        if (timeStamp != null) {
            v.add(new DERTaggedObject(true, 0, timeStamp));
        }
        return new DERSequence(v);
    }
}
