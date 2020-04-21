package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.*;

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
            timeStamp = DERBitString.getInstance(e.nextElement());
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

    public void setToSign(TBS_Sign toSign) {
        this.toSign = toSign;
    }

    public ASN1OctetString getCert() {
        return cert;
    }

    public void setCert(ASN1OctetString cert) {
        this.cert = cert;
    }

    public ASN1ObjectIdentifier getSignatureAlgID() {
        return signatureAlgID;
    }

    public void setSignatureAlgID(ASN1ObjectIdentifier signatureAlgID) {
        this.signatureAlgID = signatureAlgID;
    }

    public ASN1BitString getSignature() {
        return signature;
    }

    public void setSignature(ASN1BitString signature) {
        this.signature = signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = new DERBitString(signature);
    }

    public ASN1BitString getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(ASN1BitString timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(5);
        v.add(toSign);
        v.add(cert);
        v.add(signatureAlgID);
        v.add(signature);
        if (timeStamp != null) {
            v.add(timeStamp);
        }
        return new BERSequence(v);
    }
}
