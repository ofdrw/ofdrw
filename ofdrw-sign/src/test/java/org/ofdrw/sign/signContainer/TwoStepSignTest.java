package org.ofdrw.sign.signContainer;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.gm.ses.v1.SES_Header;
import org.ofdrw.gm.ses.v4.SES_Signature;
import org.ofdrw.gm.ses.v4.SESeal;
import org.ofdrw.gm.ses.v4.TBS_Sign;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.NumberFormatAtomicSignID;
import org.ofdrw.sign.SignMode;
import org.ofdrw.sign.stamppos.NormalStampPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Date;

public class TwoStepSignTest {

    @Test
    public void twoStepSign() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "helloworld.ofd");
        Path out = Paths.get("target/sign.ofd");

        PreSignData preSignData;
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDPreSigner signer = new OFDPreSigner(reader, new NumberFormatAtomicSignID())
        ) {
            PreSignContainer signContainer = new PreSignContainer(seal);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.ContinueSign);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new NormalStampPos(1, 50, 50, 40, 40));
            // 5. 执行签名
            preSignData = signer.exePreSign();
            // 6. 关闭签名引擎，生成文档。
//            FileUtils.writeByteArrayToFile(out.toFile(), preSignData.getXmlBytes());
        }

        //前端做的
        TBS_Sign toSign = new TBS_Sign()
                .setVersion(SES_Header.V4)
                .setEseal(seal)
                .setTimeInfo(new ASN1GeneralizedTime(new Date()))
                .setDataHash(preSignData.getDataHash())
                .setPropertyInfo(preSignData.getPropertyInfo());

        Signature sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initSign(prvKey);
        sg.update(toSign.getEncoded("DER"));
        final byte[] sigVal = sg.sign();
        SES_Signature signature = new SES_Signature()
                .setToSign(toSign)
                .setCert(signCert)
                .setSignatureAlgID(GMObjectIdentifiers.sm2sign_with_sm3)
                .setSignature(sigVal);
        byte[] signedValue = signature.getEncoded("DER");


        try (OFDReader reader = new OFDReader(src);
             OFDComSigner signer = new OFDComSigner(reader, out, new NumberFormatAtomicSignID())
        ) {
            SignComContainer signContainer = new SignComContainer(preSignData.getXmlBytes(), preSignData.getSeal(), signedValue);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.ContinueSign);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 执行签名合成
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());

    }

}
