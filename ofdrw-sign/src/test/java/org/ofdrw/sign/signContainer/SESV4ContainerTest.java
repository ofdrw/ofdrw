package org.ofdrw.sign.signContainer;

import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.gm.ses.v4.SESeal;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.NumberFormatAtomicSignID;
import org.ofdrw.sign.OFDSigner;
import org.ofdrw.sign.SignMode;
import org.ofdrw.sign.stamppos.NormalStampPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.*;

class SESV4ContainerTest {
    @Test
    public void sign() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "helloworld.ofd");
        Path out = Paths.get("target/SESV4SignDoc.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
//             OFDSigner signer = new OFDSigner(reader, out)
             OFDSigner signer = new OFDSigner(reader, out, new NumberFormatAtomicSignID())
        ) {
            SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
            // 2. 设置签名模式
//            signer.setSignMode(SignMode.WholeProtected);
            signer.setSignMode(SignMode.ContinueSign);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new NormalStampPos(1, 50, 50, 40, 40));
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }

}