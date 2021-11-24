package org.ofdrw.gm.ses.v1;

import org.bouncycastle.asn1.*;

import java.io.IOException;
import java.util.Enumeration;

/**
 * 电子签章数据
 *
 * @author 权观宇
 * @since 2020-04-19 15:49:19
 */
public class SES_Signature extends ASN1Object {

    /**
     * 待电子签章数据
     */
    private TBS_Sign toSign;

    /**
     * 电子签章中签名值
     */
    private ASN1BitString signature;

    public SES_Signature() {
        super();
    }

    public SES_Signature(TBS_Sign toSign, ASN1BitString signature) {
        this.toSign = toSign;
        this.signature = signature;
    }


    public SES_Signature(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        toSign = TBS_Sign.getInstance(e.nextElement());
        signature = DERBitString.getInstance(e.nextElement());
    }


    public static SES_Signature getInstance(Object o) {
        if (o instanceof SES_Signature) {
            return (SES_Signature) o;
        } else if (o instanceof byte[]) {
            ASN1InputStream aIn = new ASN1InputStream((byte[]) o);
            try {
                ASN1Primitive obj = aIn.readObject();
                return new SES_Signature(ASN1Sequence.getInstance(obj));
            } catch (IOException e) {
                throw new IllegalArgumentException("电子签章数据 格式错误", e);
            }
        } else if (o != null) {
            return new SES_Signature(ASN1Sequence.getInstance(o));
        }
        return null;
    }

    public TBS_Sign getToSign() {
        return toSign;
    }

    public SES_Signature setToSign(TBS_Sign toSign) {
        this.toSign = toSign;
        return this;
    }

    public ASN1BitString getSignature() {
        return signature;
    }

    public SES_Signature setSignature(ASN1BitString signature) {
        this.signature = signature;
        return this;
    }

    public SES_Signature setSignature(byte[] signature) {
        this.signature = new DERBitString(signature);
        return this;
    }


    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(3);
        v.add(toSign);
        v.add(signature);
        return new DERSequence(v);
    }
}
