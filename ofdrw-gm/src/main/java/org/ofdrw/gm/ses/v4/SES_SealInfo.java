package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.*;
import org.ofdrw.gm.ses.v1.ExtensionDatas;
import org.ofdrw.gm.ses.v1.SES_ESPictrueInfo;
import org.ofdrw.gm.ses.v1.SES_Header;

import java.util.Enumeration;

/**
 * 印章信息
 *
 * @author 权观宇
 * @since 2020-04-19 15:26:39
 */
public class SES_SealInfo extends ASN1Object {

    /**
     * 头信息
     */
    private SES_Header header;

    /**
     * 电子印章标识符
     * <p>
     * 电子印章数据唯一标识编码
     */
    private DERIA5String esID;

    /**
     * 印章属性信息
     */
    private SES_ESPropertyInfo property;

    /**
     * 电子印章图片数据
     */
    private SES_ESPictrueInfo picture;

    /**
     * 自定义数据
     */
    private ExtensionDatas extDatas;

    public SES_SealInfo(SES_Header header,
                        DERIA5String esID,
                       SES_ESPropertyInfo property,
                        SES_ESPictrueInfo picture,
                        ExtensionDatas extDatas) {
        this.header = header;
        this.esID = esID;
        this.property = property;
        this.picture = picture;
        this.extDatas = extDatas;
    }

    public SES_SealInfo(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        header = SES_Header.getInstance(e.nextElement());
        esID = DERIA5String.getInstance(e.nextElement());
        property = SES_ESPropertyInfo.getInstance(e.nextElement());
        picture = SES_ESPictrueInfo.getInstance(e.nextElement());
        if (e.hasMoreElements()) {
            extDatas = ExtensionDatas.getInstance(e.nextElement());
        }
    }

    public static SES_SealInfo getInstance(Object o) {
        if (o instanceof SES_SealInfo) {
            return (SES_SealInfo) o;
        } else if (o != null) {
            return new SES_SealInfo(ASN1Sequence.getInstance(o));
        }
        return null;
    }

    public SES_Header getHeader() {
        return header;
    }

    public SES_SealInfo setHeader(SES_Header header) {
        this.header = header;
        return this;
    }

    public DERIA5String getEsID() {
        return esID;
    }

    public SES_SealInfo setEsID(DERIA5String esID) {
        this.esID = esID;
        return this;
    }

    public SES_SealInfo setEsID(String esID) {
        this.esID = new DERIA5String(esID);
        return this;
    }

    public SES_ESPropertyInfo getProperty() {
        return property;
    }

    public void setProperty(SES_ESPropertyInfo property) {
        this.property = property;
    }

    public SES_ESPictrueInfo getPicture() {
        return picture;
    }

    public SES_SealInfo setPicture(SES_ESPictrueInfo picture) {
        this.picture = picture;
        return this;
    }

    public ExtensionDatas getExtDatas() {
        return extDatas;
    }

    public SES_SealInfo setExtDatas(ExtensionDatas extDatas) {
        this.extDatas = extDatas;
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(5);
        v.add(header);
        v.add(esID);
        v.add(property);
        v.add(picture);
        if (extDatas != null) {
            v.add(extDatas);
        }
        return new BERSequence(v);
    }
}
