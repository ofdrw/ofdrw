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
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.Locale;

/**
 * 没有提供电子印章文件的容器
 * @author 权观宇
 * @since 2020-05-18 19:48:29
 */
public class SESV1ContainerNoSeal extends SESV1Container {


    public SESV1ContainerNoSeal(PrivateKey privateKey, SESeal seal, Certificate signCert) {
       super(privateKey, seal, signCert);
    }

    /**
     * 返还空
     *
     * @return 电子印章二进制编码
     * @throws IOException 获取印章IO异常
     */
    @Override
    public byte[] getSeal() throws IOException {
        return  null;
    }

}
