package org.ofdrw.gm.ses.parse;

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Sequence;

/**
 * 版本持有器
 *
 * @author 权观宇
 * @since 2020-04-20 09:17:22
 */
public class SESVersionHolder {

    /**
     * 版本号
     */
    private SESVersion version;

    /**
     * 没有构造的对象
     */
    private ASN1Sequence objSeq;

    public SESVersionHolder(SESVersion version, ASN1Sequence objSeq) {
        this.version = version;
        this.objSeq = objSeq;
    }

    public SESVersion getVersion() {
        return version;
    }

    public ASN1Sequence getObjSeq() {
        return objSeq;
    }

    /**
     * 获取版本对应的电子签章值对象
     * <p>
     * 可以通过版本号强制来判断强制类型转换
     *
     * @param <T> 期待的电子签章值类型
     * @return ASN1对象
     */
    public <T extends ASN1Object> T SESObject() {
        ASN1Object obj = null;
        switch (this.version) {
            case v1:
                if (this.objSeq.size() == 2) {
                    obj = new org.ofdrw.gm.ses.v1.SES_Signature(this.objSeq);
                }
                break;
            case v4:
                if (this.objSeq.size() >= 4) {
                    obj = new org.ofdrw.gm.ses.v4.SES_Signature(this.objSeq);
                }
        }
        return (T) obj;
    }

    /**
     * 获取版本对应的电子印章对象
     * <p>
     * 可以通过版本号强制来判断强制类型转换
     *
     * @param <T> 期待的电子印章对象类型
     * @return 电子印章对象
     */
    public <T extends ASN1Object> T SealObject() {
        ASN1Object obj = null;
        switch (this.version) {
            case v1:
                if (this.objSeq.size() == 2) {
                    obj = new org.ofdrw.gm.ses.v1.SESeal(this.objSeq);
                }
                break;
            case v4:
                if (this.objSeq.size() == 4) {
                    obj = new org.ofdrw.gm.ses.v4.SESeal(this.objSeq);
                }
                break;
        }
        return (T) obj;
    }
}
