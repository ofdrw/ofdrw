package org.ofdrw.reader.model;

import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.core.signatures.sig.SignedInfo;
import org.ofdrw.gm.ses.parse.SESVersion;
import org.ofdrw.gm.ses.parse.SESVersionHolder;
import org.ofdrw.gm.ses.v1.SES_ESPictrueInfo;
import org.ofdrw.gm.ses.v4.SES_Signature;
import org.ofdrw.gm.ses.v4.TBS_Sign;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * OFD中的签章信息
 *
 * @author dltech21
 * @since 2020/8/11
 */
public class StampAnnotEntity {

    /**
     * 签名要保护的原文及本次签名相关的信息
     */
    private SignedInfo signedInfo;


    /**
     * 印章图片
     * <p>
     * 可能是OFD、PNG、gif、svg等，根据类型判断
     */
    private byte[] imageByte;

    /**
     * 印章图片类型
     * <p>
     * OFD、PNG、gif、svg
     */
    private String imgType;

    /**
     * 多版本印章
     */
    private SESVersionHolder sesVersionHolder;

    private StampAnnotEntity() {
    }

    /**
     * 创建印章实体
     *
     * @param signedInfo       签名要保护的原文及本次签名相关的信息
     * @param sesVersionHolder 多版本电子签章数据
     */
    public StampAnnotEntity(SESVersionHolder sesVersionHolder, SignedInfo signedInfo) {
        this.signedInfo = signedInfo;
        this.sesVersionHolder = sesVersionHolder;
        init();
    }

    private void init() {
        if (sesVersionHolder.getVersion() == SESVersion.v4) {
            SES_Signature sesSignature = sesVersionHolder.getInstanceV4();
            TBS_Sign toSign = sesSignature.getToSign();
            final SES_ESPictrueInfo picture = toSign.getEseal().geteSealInfo().getPicture();
            this.imgType = picture.getType().getString();
            this.imageByte = picture.getData().getOctets();
        } else {
            org.ofdrw.gm.ses.v1.SES_Signature sesSignature = sesVersionHolder.getInstanceV1();
            org.ofdrw.gm.ses.v1.TBS_Sign toSign = sesSignature.getToSign();
            final SES_ESPictrueInfo picture = toSign.getEseal().getEsealInfo().getPicture();
            this.imgType = picture.getType().getString();
            this.imageByte = picture.getData().getOctets();
        }
    }

    /**
     * 签名的外观序列
     *
     * @return 签名的外观序列
     */
    public List<StampAnnot> getStampAnnots() {
        return signedInfo.getStampAnnots();
    }

    /**
     * 图片数据
     *
     * @return 图片数据
     */
    public byte[] getImageByte() {
        return imageByte;
    }

    /**
     * 印章图片类型
     *
     * @return 图片类型
     */
    public String getImgType() {
        return imgType;
    }

    /**
     * 获取图片字节流
     * @return 字节流
     */
    public ByteArrayInputStream getImageStream(){
        return new ByteArrayInputStream(this.imageByte);
    }

    /**
     * 获取多版本的电子签章数据信息
     *
     * @return 电子签章数据信息
     */
    public SESVersionHolder getHolder() {
        return sesVersionHolder;
    }

    /**
     * 获取签名要保护的原文及本次签名相关的信息
     *
     * @return 签名要保护的原文及本次签名相关的信息
     */
    public SignedInfo getSignedInfo() {
        return signedInfo;
    }
}
