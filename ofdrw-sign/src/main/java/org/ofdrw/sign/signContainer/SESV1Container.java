package org.ofdrw.sign.signContainer;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.gm.ses.v1.SES_Signature;
import org.ofdrw.gm.ses.v1.SESeal;
import org.ofdrw.gm.ses.v1.TBS_Sign;
import org.ofdrw.sign.ExtendSignatureContainer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * 《GM/T 0031-2014 安全电子签章密码技术规范》 电子签章数据生成扩展容器
 * <p>
 * 注意：该容器仅用于测试，电子签章请使用符合国家规范具有国家型号证书的设备进行！
 *
 * @author 权观宇
 * @since 2020-04-21 01:22:47
 */
public class SESV1Container implements ExtendSignatureContainer {
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
     * V1版本的电子签章容器构造
     *
     * @param privateKey 签名使用的私钥
     * @param seal       电子印章
     * @param signCert   签章用户证书
     */
    public SESV1Container(PrivateKey privateKey, SESeal seal, Certificate signCert) {
        this.privateKey = privateKey;
        this.seal = seal;
        this.certificate = signCert;
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
        byte[] digest = md.digest(IOUtils.toByteArray(inData));

        // 签名时间
        byte[] signUTCTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .getBytes(StandardCharsets.UTF_8);
//        ASN1UTCTime signUTCTime = new ASN1UTCTime(new Date(), Locale.CHINA);
        TBS_Sign tbsSign = new TBS_Sign()
                .setVersion(new ASN1Integer(1))
                .setEseal(seal)
                .setTimeInfo(new DERBitString(signUTCTime))
                .setDataHash(new DERBitString(digest))
                .setPropertyInfo(new DERIA5String(propertyInfo))
                .setCert(new DEROctetString(certificate.getEncoded()))
                .setSignatureAlgorithm(GMObjectIdentifiers.sm2sign_with_sm3);
        Signature signature = Signature.getInstance("SM3withSm2", new BouncyCastleProvider());
        signature.initSign(privateKey);
        signature.update(tbsSign.getEncoded("DER"));
        byte[] sign = signature.sign();
        SES_Signature sesSignature = new SES_Signature(tbsSign, new DERBitString(sign));
        return sesSignature.getEncoded("DER");
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
