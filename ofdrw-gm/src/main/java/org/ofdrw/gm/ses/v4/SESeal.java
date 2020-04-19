package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.*;

/**
 * 电子印章数据
 *
 * @author 权观宇
 * @since 2020-04-19 17:47:55
 */
public class SESeal extends ASN1Object {

    /**
     * 印章信息
     */
    private SES_SealInfo eSealInfo;

    /**
     * 制章人证书
     */
    private ASN1OctetString cert;

    /**
     * 签名算法标识符
     */
    private ASN1ObjectIdentifier signAlgID;

    /**
     * 签名值
     */
    private ASN1BitString signedValue;
    
    @Override
    public ASN1Primitive toASN1Primitive() {
        return null;
    }
}
