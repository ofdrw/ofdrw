package org.ofdrw.gm.ses.v5;

import org.bouncycastle.asn1.*;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Enumeration;

/**
 * 电子印章数据（V5）
 * <p>
 * 五字段平铺结构：eSealInfo + cert + signatureAlgID + signatureValue + timeStamp[0] OPTIONAL
 *
 * @since 2026-04-24
 * @author minghu.zhang
 */
public class SESeal extends ASN1Object {

    private SES_SealInfo eSealInfo;
    private ASN1OctetString cert;
    private ASN1ObjectIdentifier signatureAlgID;
    private ASN1BitString signatureValue;
    private ASN1BitString timeStamp;

    public SESeal() {
        super();
    }

    public SESeal(SES_SealInfo eSealInfo, ASN1OctetString cert,
                  ASN1ObjectIdentifier signatureAlgID, ASN1BitString signatureValue,
                  ASN1BitString timeStamp) {
        this.eSealInfo = eSealInfo;
        this.cert = cert;
        this.signatureAlgID = signatureAlgID;
        this.signatureValue = signatureValue;
        this.timeStamp = timeStamp;
    }

    public SESeal(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        eSealInfo = SES_SealInfo.getInstance(e.nextElement());
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

    public static SESeal getInstance(Object o) {
        if (o instanceof SESeal) {
            return (SESeal) o;
        } else if (o != null) {
            return new SESeal(ASN1Sequence.getInstance(o));
        }
        return null;
    }

    public SES_SealInfo geteSealInfo() { return eSealInfo; }

    public SESeal seteSealInfo(SES_SealInfo eSealInfo) {
        this.eSealInfo = eSealInfo;
        return this;
    }

    public ASN1OctetString getCert() { return cert; }

    public SESeal setCert(ASN1OctetString cert) {
        this.cert = cert;
        return this;
    }

    public SESeal setCert(Certificate cert) throws CertificateEncodingException {
        this.cert = new DEROctetString(cert.getEncoded());
        return this;
    }

    public ASN1ObjectIdentifier getSignatureAlgID() { return signatureAlgID; }

    public SESeal setSignatureAlgID(ASN1ObjectIdentifier signatureAlgID) {
        this.signatureAlgID = signatureAlgID;
        return this;
    }

    public ASN1BitString getSignatureValue() { return signatureValue; }

    public SESeal setSignatureValue(ASN1BitString signatureValue) {
        this.signatureValue = signatureValue;
        return this;
    }

    public SESeal setSignatureValue(byte[] signatureValue) {
        this.signatureValue = new DERBitString(signatureValue);
        return this;
    }

    public ASN1BitString getTimeStamp() { return timeStamp; }

    public SESeal setTimeStamp(ASN1BitString timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(5);
        v.add(eSealInfo);
        v.add(cert);
        v.add(signatureAlgID);
        v.add(signatureValue);
        if (timeStamp != null) {
            v.add(new DERTaggedObject(true, 0, timeStamp));
        }
        return new DERSequence(v);
    }
}
