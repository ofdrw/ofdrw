package org.ofdrw.gm.ses.v1;

import org.bouncycastle.asn1.*;

import java.io.IOException;
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

    public SESeal() {
        super();
    }

    public SESeal(SES_SealInfo esealInfo, SES_SignInfo signInfo) {
        this.esealInfo = esealInfo;
        this.signInfo = signInfo;
    }

    public SESeal(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        esealInfo = SES_SealInfo.getInstance(e.nextElement());
        /*
         * 兼容非标签章 非标签章的签名信息格式与标准不同无法解析
         * */
        try {
            signInfo = SES_SignInfo.getInstance(e.nextElement());
        } catch (Exception exception) {

        }
    }

    public static SESeal getInstance(Object o) {
        if (o instanceof SESeal) {
            return (SESeal) o;
        } else if (o instanceof byte[]) {
            ASN1InputStream aIn = new ASN1InputStream((byte[]) o);
            try {
                ASN1Primitive obj = aIn.readObject();
                return new SESeal(ASN1Sequence.getInstance(obj));
            } catch (IOException e) {
                throw new IllegalArgumentException("电子印章数据 无法解析", e);
            }
        } else if (o instanceof DEROctetString) {
            DEROctetString string = (DEROctetString) o;
            return new SESeal(ASN1Sequence.getInstance(string.getOctets()));
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
        return new DERSequence(v);
    }
}
