package org.ofdrw.gm.ses.parse;

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
}
