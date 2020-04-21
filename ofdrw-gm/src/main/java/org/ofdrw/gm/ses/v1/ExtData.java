package org.ofdrw.gm.ses.v1;

import org.bouncycastle.asn1.*;

import java.util.Enumeration;

/**
 * 厂商自定义数据
 *
 * @author 权观宇
 * @since 2020-04-19 15:22:12
 */
public class ExtData extends ASN1Object {

    /**
     * 自定义扩展字段标识
     */
    private ASN1ObjectIdentifier extnID;

    /**
     * 自定义扩展字段是否关键
     * <p>
     * 默认值FALSE
     */
    private ASN1Boolean critical = ASN1Boolean.FALSE;

    /**
     * 自定义扩展字段数据值
     */
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

    public ASN1ObjectIdentifier getExtnID() {
        return extnID;
    }

    public ExtData setExtnID(ASN1ObjectIdentifier extnID) {
        this.extnID = extnID;
        return this;
    }

    public ASN1Boolean getCritical() {
        return critical;
    }

    public ExtData setCritical(ASN1Boolean critical) {
        this.critical = critical;
        return this;
    }

    public ASN1OctetString getExtnValue() {
        return extnValue;
    }

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
        return new BERSequence(v);
    }
}
