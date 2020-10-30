package org.ofdrw.sign.signContainer;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.gm.ses.v1.SES_Header;
import org.ofdrw.gm.ses.v4.SES_Signature;
import org.ofdrw.gm.ses.v4.SESeal;
import org.ofdrw.gm.ses.v4.TBS_Sign;
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
 * 《GB/T 38540-2020 信息安全技术 安全电子签章密码技术规范》 电子签章数据生成扩展容器
 * <p>
 * 注意：该容器仅用于测试，电子签章请使用符合国家规范具有国家型号证书的设备进行！
 *
 * @author 权观宇
 * @since 2020-04-21 01:22:47
 */
public class SESV4Container implements ExtendSignatureContainer {
    /**
     * 签名使用的私钥
     */
    private final PrivateKey privateKey;

    /**
     * 电子印章
     */
    private final SESeal seal;

    /**
     * 签章使用的证书
     */
    private final Certificate certificate;

    /**
     * 时间戳hook对象
     */
    private TimeStampHook timeStampHook;

    /**
     * V1版本的电子签章容器构造
     *
     * @param privateKey 签名使用的私钥
     * @param seal       电子印章
     * @param signCert   签章用户证书
     */
    public SESV4Container(PrivateKey privateKey, SESeal seal, Certificate signCert) {
        this.privateKey = privateKey;
        this.seal = seal;
        this.certificate = signCert;
    }

    /**
     * V1版本的电子签章容器构造
     *
     * @param privateKey    签名使用的私钥
     * @param seal          电子印章
     * @param signCert      签章用户证书
     * @param timeStampHook 时间戳Hook
     */
    public SESV4Container(PrivateKey privateKey, SESeal seal, Certificate signCert, TimeStampHook timeStampHook) {
        this.privateKey = privateKey;
        this.seal = seal;
        this.certificate = signCert;
        this.timeStampHook = timeStampHook;
    }

    /**
     * 提供文件的摘要算法功能
     *
     * @return 摘要算法功能
     */
    @Override
    public MessageDigest getDigestFnc() {
        return new SM3.Digest();
    }

    /**
     * 签名方法OID
     *
     * @return 签名方法OID
     */
    @Override
    public ASN1ObjectIdentifier getSignAlgOID() {
        return GMObjectIdentifiers.sm2sign_with_sm3;
    }

    /**
     * 设置TimeStampHook
     *
     * @param timeStampHook hook对象
     */
    public void setTimeStampHook(TimeStampHook timeStampHook) {
        this.timeStampHook = timeStampHook;
    }

    /**
     * 对待签名数据进行电子签章
     * <p>
     * 注意：该方法不符合《GM/T 0031-2014 安全电子签章密码技术规范》 流程规范，生成的电子签章
     * 不具有效力，请使用符合国家标准具有型号证书的设备产生电子签章数据。
     * <p>
     * 该方法只用于测试调试。
     *
     * @param inData       待签名数据流
     * @param propertyInfo 签章属性信息
     * @return 签名或签章结果值
     * @throws IOException              流操作异常
     * @throws GeneralSecurityException 签名计算异常
     */
    @Override
    public byte[] sign(InputStream inData, String propertyInfo) throws IOException, GeneralSecurityException {

        MessageDigest md = getDigestFnc();
        // 签名原文杂凑值，也就是Signature.xml 文件的杂凑值
        byte[] dataHash = md.digest(IOUtils.toByteArray(inData));

        TBS_Sign toSign = new TBS_Sign()
                .setVersion(SES_Header.V4)
                .setEseal(seal)
                .setTimeInfo(new ASN1GeneralizedTime(new Date(), Locale.CHINA))
                .setDataHash(dataHash)
                .setPropertyInfo(propertyInfo);

        Signature sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initSign(privateKey);
        sg.update(toSign.getEncoded("DER"));
        final byte[] sigVal = sg.sign();
        SES_Signature signature = new SES_Signature()
                .setToSign(toSign)
                .setCert(certificate)
                .setSignatureAlgID(GMObjectIdentifiers.sm2sign_with_sm3)
                .setSignature(sigVal);

        if (timeStampHook != null) {
            byte[] timeStamp = timeStampHook.apply(sigVal);
            if (timeStamp != null) {
                signature.setTimeStamp(new DERBitString(timeStamp));
            }
        }

        return signature.getEncoded("DER");
    }

    /**
     * 设置时间戳hook
     *
     * @param timeStampHook 传入得时间戳hook
     */
    public void setTimestampHook(TimeStampHook timeStampHook) {
        this.timeStampHook = timeStampHook;
    }

    /**
     * 获取电子印章二进制编码
     * <p>
     * 如果{@link #getSignType()} 返还类型为{@link SigType#Sign}那么请返回null
     *
     * @return 电子印章二进制编码
     * @throws IOException 获取印章IO异常
     */
    @Override
    public byte[] getSeal() throws IOException {
        return seal.getEncoded("DER");
    }

    /**
     * 获取签名节点类型
     *
     * @return 签名节点类型
     */
    @Override
    public SigType getSignType() {
        return SigType.Seal;
    }
}
