package org.ofdrw.gm.ses.v1;

import org.bouncycastle.asn1.*;

import java.util.Enumeration;

/**
 * 印章图片信息
 *
 * @author 权观宇
 * @since 2020-04-19 14:30:22
 */
public class SES_ESPictrueInfo extends ASN1Object {

    /**
     * 图片类型
     * <p>
     * 代表印章图片类型，如 GIF、BMP、JPG、SVG等
     */
    private DERIA5String type;

    /**
     * 印章图片数据
     */
    private ASN1OctetString data;

    /**
     * 图片显示宽度，单位为毫米（mm）
     */
    private ASN1Integer width;

    /**
     * 图片显示高度，单位为毫米（mm）
     */
    private ASN1Integer height;

    public SES_ESPictrueInfo(
            ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        type = DERIA5String.getInstance(e.nextElement());
        data = ASN1OctetString.getInstance(e.nextElement());
        width = ASN1Integer.getInstance(e.nextElement());
        height = ASN1Integer.getInstance(e.nextElement());
    }


    public SES_ESPictrueInfo(DERIA5String type,
                             ASN1OctetString data,
                             ASN1Integer width,
                             ASN1Integer height) {
        this.type = type;
        this.data = data;
        this.width = width;
        this.height = height;
    }

    public static SES_ESPictrueInfo getInstance(Object o) {
        if (o instanceof SES_ESPictrueInfo) {
            return (SES_ESPictrueInfo) o;
        } else if (o != null) {
            return new SES_ESPictrueInfo(ASN1Sequence.getInstance(o));
        }
        return null;
    }


    public ASN1OctetString getData() {
        return data;
    }

    public SES_ESPictrueInfo setData(ASN1OctetString data) {
        this.data = data;
        return this;
    }

    public SES_ESPictrueInfo setData(byte[] data) {
        this.data = new DEROctetString(data);
        return this;
    }

    public ASN1Integer getWidth() {
        return width;
    }

    public SES_ESPictrueInfo setWidth(ASN1Integer width) {
        this.width = width;
        return this;
    }

    public SES_ESPictrueInfo setWidth(long width) {
        this.width = new ASN1Integer(width);
        return this;
    }

    public ASN1Integer getHeight() {
        return height;
    }

    public SES_ESPictrueInfo setHeight(ASN1Integer height) {
        this.height = height;
        return this;
    }

    public SES_ESPictrueInfo setHeight(long height) {
        this.height = new ASN1Integer(height);
        return this;
    }

    public DERIA5String getType() {
        return type;
    }

    public SES_ESPictrueInfo setType(String type) {
        this.type = new DERIA5String(type);
        return this;
    }

    public SES_ESPictrueInfo setType(DERIA5String type) {
        this.type = type;
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(4);
        v.add(type);
        v.add(data);
        v.add(width);
        v.add(height);
        return new BERSequence(v);
    }
}
