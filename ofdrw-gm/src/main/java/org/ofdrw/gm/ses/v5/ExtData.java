package org.ofdrw.gm.ses.v5;

import org.bouncycastle.asn1.*;

import java.util.Enumeration;

/**
 * 厂商自定义数据
 *
 * @since 2026-04-24
 * @author minghu.zhang
 */
public class ExtData extends ASN1Object {

    private ASN1ObjectIdentifier extnID;
    private ASN1Boolean critical = ASN1Boolean.FALSE;
    private ASN1OctetString extnValue;

    public ExtData() {
        super();
    }

    public ExtData(ASN1ObjectIdentifier extnID, ASN1Boolean critical, ASN1OctetString extnValue) {
        this.extnID = extnID;
        this.critical = critical;
        this.extnValue = extnValue;
    }

    public ExtData(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        extnID = ASN1ObjectIdentifier.getInstance(e.nextElement());
        critical = ASN1Boolean.getInstance(e.nextElement());
        extnValue = ASN1OctetString.getInstance(e.nextElement());
    }

    public static ExtData getInstance(Object o) {
        if (o instanceof ExtData) {
            return (ExtData) o;
        } else if (o != null) {
            return new ExtData(ASN1Sequence.getInstance(o));
        }
        return null;
    }

    public ASN1ObjectIdentifier getExtnID() { return extnID; }

    public ExtData setExtnID(ASN1ObjectIdentifier extnID) {
        this.extnID = extnID;
        return this;
    }

    public ASN1Boolean getCritical() { return critical; }

    public ExtData setCritical(ASN1Boolean critical) {
        this.critical = critical;
        return this;
    }

    public ASN1OctetString getExtnValue() { return extnValue; }

    public ExtData setExtnValue(ASN1OctetString extnValue) {
        this.extnValue = extnValue;
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(3);
        v.add(extnID);
        v.add(critical);
        v.add(extnValue);
        return new DERSequence(v);
    }
}
