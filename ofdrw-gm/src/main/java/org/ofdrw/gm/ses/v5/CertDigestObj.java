package org.ofdrw.gm.ses.v5;

import org.bouncycastle.asn1.*;

import java.util.Enumeration;

/**
 * 签章者证书杂凑值
 *
 * @since 2026-04-24
 * @author minghu.zhang
 */
public class CertDigestObj extends ASN1Object {

    private DERPrintableString type;
    private ASN1OctetString value;

    public CertDigestObj() {
        super();
    }

    public CertDigestObj(DERPrintableString type, ASN1OctetString value) {
        this.type = type;
        this.value = value;
    }

    public CertDigestObj(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        type = (DERPrintableString) DERPrintableString.getInstance(e.nextElement());
        value = ASN1OctetString.getInstance(e.nextElement());
    }

    public static CertDigestObj getInstance(Object o) {
        if (o instanceof CertDigestObj) {
            return (CertDigestObj) o;
        } else if (o != null) {
            return new CertDigestObj(ASN1Sequence.getInstance(o));
        }
        return null;
    }

    public DERPrintableString getType() { return type; }

    public CertDigestObj setType(DERPrintableString type) {
        this.type = type;
        return this;
    }

    public CertDigestObj setType(String type) {
        this.type = new DERPrintableString(type);
        return this;
    }

    public ASN1OctetString getValue() { return value; }

    public CertDigestObj setValue(ASN1OctetString value) {
        this.value = value;
        return this;
    }

    public CertDigestObj setValue(byte[] value) {
        this.value = new DEROctetString(value);
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(2);
        v.add(type);
        v.add(value);
        return new DERSequence(v);
    }
}
