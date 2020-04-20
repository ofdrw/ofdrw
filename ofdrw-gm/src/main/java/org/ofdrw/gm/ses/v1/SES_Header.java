package org.ofdrw.gm.ses.v1;

import org.bouncycastle.asn1.*;

import java.util.Enumeration;

/**
 * 头信息
 *
 * @author 权观宇
 * @since 2020-04-19 15:13:29
 */
public class SES_Header extends ASN1Object {

    /**
     * 电子印章数据标识符
     * 固定值“ES”
     */
    public static final DERIA5String ID = new DERIA5String("ES");
    /**
     * 电子印章数据标识符
     * <p>
     * 固定值“ES”
     */
    private DERIA5String id;

    /**
     * 电子印章数据版本号标识
     */
    private ASN1Integer version;

    /**
     * 电子印章厂商ID
     * <p>
     * 在互联互通时，用于识别不同的软件厂商实现
     */
    private DERIA5String vid;

    public SES_Header(ASN1Integer version, DERIA5String vid) {
        this.id = ID;
        this.version = version;
        this.vid = vid;
    }


    public SES_Header(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        id = DERIA5String.getInstance(e.nextElement());
        version = ASN1Integer.getInstance(e.nextElement());
        vid = DERIA5String.getInstance(e.nextElement());
    }

    public static SES_Header getInstance(Object o) {
        if (o instanceof SES_Header) {
            return (SES_Header) o;
        } else if (o != null) {
            return new SES_Header(ASN1Sequence.getInstance(o));
        }
        return null;
    }

    public DERIA5String getId() {
        return id;
    }

    public ASN1Integer getVersion() {
        return version;
    }

    public SES_Header setVersion(ASN1Integer version) {
        this.version = version;
        return this;
    }

    public SES_Header setVersion(int version) {
        this.version = new ASN1Integer(version);
        return this;
    }

    public DERIA5String getVid() {
        return vid;
    }

    public SES_Header setVid(DERIA5String vid) {
        this.vid = vid;
        return this;
    }

    public SES_Header setVid(String vid) {
        this.vid = new DERIA5String(vid);
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(3);
        v.add(ID);
        v.add(version);
        v.add(vid);
        return new BERSequence(v);
    }
}
