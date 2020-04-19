package org.ofdrw.gm.ses.v1;

import org.bouncycastle.asn1.*;

import java.util.Enumeration;

/**
 * 电子印章数据
 *
 * @author 权观宇
 * @since 2020-04-19 15:33:55
 */
public class SESeal extends ASN1Object {
    /**
     * 印章信息
     */
    private SES_SealInfo esealInfo;

    /**
     * 制章人对印章签名的信息
     */
    private SES_SignInfo signInfo;

    public SESeal(SES_SealInfo esealInfo, SES_SignInfo signInfo) {
        this.esealInfo = esealInfo;
        this.signInfo = signInfo;
    }

    public SESeal(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        esealInfo = SES_SealInfo.getInstance(e.nextElement());
        signInfo = SES_SignInfo.getInstance(e.nextElement());
    }

    public static SESeal getInstance(Object o) {
        if (o instanceof SESeal) {
            return (SESeal) o;
        } else if (o != null) {
            return new SESeal(ASN1Sequence.getInstance(o));
        }
        return null;
    }

    public SES_SealInfo getEsealInfo() {
        return esealInfo;
    }

    public SESeal setEsealInfo(SES_SealInfo esealInfo) {
        this.esealInfo = esealInfo;
        return this;
    }

    public SES_SignInfo getSignInfo() {
        return signInfo;
    }

    public SESeal setSignInfo(SES_SignInfo signInfo) {
        this.signInfo = signInfo;
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(3);
        v.add(esealInfo);
        v.add(signInfo);
        return new BERSequence(v);
    }
}
