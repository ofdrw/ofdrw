package org.ofdrw.sign.signContainer;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.gm.ses.v5.SES_Header;
import org.ofdrw.gm.ses.v5.SES_Signature;
import org.ofdrw.gm.ses.v5.SESeal;
import org.ofdrw.gm.ses.v5.TBS_Sign;
import org.ofdrw.sign.ExtendSignatureContainer;
import org.ofdrw.sign.timestamp.TimeStampHook;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.Locale;

/**
 * V5电子签章数据生成扩展容器
 * <p>
 * 签名计算范围：印章签名=Sign(eSealInfo)，签章签名=Sign(TBS_Sign)
 * <p>
 * 注意：该容器仅用于测试，电子签章请使用符合国家规范具有国家型号证书的设备进行！
 *
 * @since 2026-04-24
 * @author minghu.zhang
 */
public class SESV5Container implements ExtendSignatureContainer {

    private final PrivateKey privateKey;
    private final SESeal seal;
    private final Certificate certificate;
    private TimeStampHook timeStampHook;

    public SESV5Container(PrivateKey privateKey, SESeal seal, Certificate signCert) {
        this.privateKey = privateKey;
        this.seal = seal;
        this.certificate = signCert;
    }

    public SESV5Container(PrivateKey privateKey, SESeal seal, Certificate signCert, TimeStampHook timeStampHook) {
        this.privateKey = privateKey;
        this.seal = seal;
        this.certificate = signCert;
        this.timeStampHook = timeStampHook;
    }

    @Override
    public MessageDigest getDigestFnc() {
        return new SM3.Digest();
    }

    @Override
    public ASN1ObjectIdentifier getSignAlgOID() {
        return GMObjectIdentifiers.sm2sign_with_sm3;
    }

    public void setTimeStampHook(TimeStampHook timeStampHook) {
        this.timeStampHook = timeStampHook;
    }

    @Override
    public byte[] sign(InputStream inData, String propertyInfo) throws IOException, GeneralSecurityException {
        MessageDigest md = getDigestFnc();
        byte[] dataHash = md.digest(IOUtils.toByteArray(inData));

        TBS_Sign toSign = new TBS_Sign()
                .setVersion(SES_Header.V5)
                .setEseal(seal)
                .setTimeInfo(new ASN1GeneralizedTime(new Date(), Locale.CHINA))
                .setDataHash(dataHash)
                .setPropertyInfo(propertyInfo);

        // 签章签名 = Sign(TBS_Sign)
        Signature sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initSign(privateKey);
        sg.update(toSign.getEncoded("DER"));
        final byte[] sigVal = sg.sign();

        SES_Signature signature = new SES_Signature()
                .setToSign(toSign)
                .setCert(certificate)
                .setSignatureAlgID(GMObjectIdentifiers.sm2sign_with_sm3)
                .setSignatureValue(sigVal);

        if (timeStampHook != null) {
            byte[] timeStamp = timeStampHook.apply(sigVal);
            if (timeStamp != null) {
                signature.setTimeStamp(new DERBitString(timeStamp));
            }
        }

        return signature.getEncoded("DER");
    }

    @Override
    public byte[] getSeal() throws IOException {
        return seal.getEncoded("DER");
    }

    @Override
    public SigType getSignType() {
        return SigType.Seal;
    }
}
