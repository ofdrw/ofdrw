package org.ofdrw.sign.signContainer;

import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.OFDSigner;
import org.ofdrw.sign.SignMode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

/**
 * 数据签名演示
 *
 * @author 权观宇
 * @since 2020-04-22 03:16:01
 */
class DigitalSignContainerTest {

    /**
     * OFD电子签名演示
     */
    @Test
    void testDigestSign() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");

        Path src = Paths.get("src/test/resources", "helloworld.ofd");
        Path out = Paths.get("target/DigitalSign.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out)) {
            DigitalSignContainer signContainer = new DigitalSignContainer(prvKey);
            // 2. 设置签名模式
//            signer.setSignMode(SignMode.WholeProtected);
            signer.setSignMode(SignMode.ContinueSign);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 执行签名
            signer.exeSign();
            // 5. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }

}