package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.*;
import org.ofdrw.gm.ses.v1.ExtensionDatas;

import java.util.Enumeration;

/**
 * 签章信息
 *
 * @author 权观宇
 * @since 2020-04-19 21:29:45
 */
public class TBS_Sign extends ASN1Object {

    /**
     * 电子印章版本号，与电子印章版本号保持一致
     */
    private ASN1Integer version;

    /**
     * 电子印章
     */
    private SESeal eseal;

    /**
     * 签章时间
     */
    private ASN1GeneralizedTime timeInfo;

    /**
     * 原文杂凑值
     */
    private ASN1BitString dataHash;

    /**
     * 原文数据的属性
     */
    private DERIA5String propertyInfo;

    /**
     * 自定义数据 【可选】
     */
    private ExtensionDatas extDatas;

    public TBS_Sign() {
        super();
    }

    public TBS_Sign(ASN1Integer version,
                    SESeal eseal,
                    ASN1GeneralizedTime timeInfo,
                    ASN1BitString dataHash,
                    DERIA5String propertyInfo,
                    ExtensionDatas extDatas) {
        this.version = version;
        this.eseal = eseal;
        this.timeInfo = timeInfo;
        this.dataHash = dataHash;
        this.propertyInfo = propertyInfo;
        this.extDatas = extDatas;
    }

    public TBS_Sign(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        version = ASN1Integer.getInstance(e.nextElement());
        eseal = SESeal.getInstance(e.nextElement());
        timeInfo = ASN1GeneralizedTime.getInstance(e.nextElement());
        dataHash = DERBitString.getInstance(e.nextElement());
        propertyInfo = DERIA5String.getInstance(e.nextElement());
        if (e.hasMoreElements()) {
            Object obj = e.nextElement();
            if (obj instanceof ASN1TaggedObject) {
                extDatas = ExtensionDatas.getInstance(((ASN1TaggedObject) obj).getObject());
            }
        }
    }

    public static TBS_Sign getInstance(Object o) {
        if (o instanceof TBS_Sign) {
            return (TBS_Sign) o;
        } else if (o != null) {
            return new TBS_Sign(ASN1Sequence.getInstance(o));
        }
        return null;
    }

    public ASN1Integer getVersion() {
        return version;
    }

    public TBS_Sign setVersion(ASN1Integer version) {
        this.version = version;
        return this;
    }

    public SESeal getEseal() {
        return eseal;
    }

    public TBS_Sign setEseal(SESeal eseal) {
        this.eseal = eseal;
        return this;
    }

    public ASN1GeneralizedTime getTimeInfo() {
        return timeInfo;
    }

    public TBS_Sign setTimeInfo(ASN1GeneralizedTime timeInfo) {
        this.timeInfo = timeInfo;
        return this;
    }

    public ASN1BitString getDataHash() {
        return dataHash;
    }

    public TBS_Sign setDataHash(ASN1BitString dataHash) {
        this.dataHash = dataHash;
        return this;
    }

    public TBS_Sign setDataHash(byte[] dataHash) {
        this.dataHash = new DERBitString(dataHash);
        return this;
    }

    public DERIA5String getPropertyInfo() {
        return propertyInfo;
    }

    public TBS_Sign setPropertyInfo(DERIA5String propertyInfo) {
        this.propertyInfo = propertyInfo;
        return this;
    }

    public TBS_Sign setPropertyInfo(String propertyInfo) {
        this.propertyInfo = new DERIA5String(propertyInfo);
        return this;
    }

    public ExtensionDatas getExtDatas() {
        return extDatas;
    }

    public TBS_Sign setExtDatas(ExtensionDatas extDatas) {
        this.extDatas = extDatas;
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(6);
        v.add(version);
        v.add(eseal);
        v.add(timeInfo);
        v.add(dataHash);
        v.add(propertyInfo);
        if (extDatas != null) {
            v.add(new DERTaggedObject(true, 0, extDatas));
        }
        return new DERSequence(v);
    }
}
