package org.ofdrw.sign;

import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.signContainer.GMDigestSignatureContainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OFD签名引擎测试
 *
 * @author 权观宇
 * @since 2020-04-18 11:10:43
 */
class OFDSignerTest {

    /**
     * 从P12中获取私钥
     *
     * @param userP12 PKCS12文件路径
     * @param pwd     解密密钥
     * @return 私钥
     */
    public static PrivateKey ReadPrvKey(Path userP12, String pwd) throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance("PKCS12", new BouncyCastleProvider());
        try (InputStream rootKsIn = Files.newInputStream(userP12)) {
            ks.load(rootKsIn, pwd.toCharArray());
            return (PrivateKey) ks.getKey("private", pwd.toCharArray());
        }
    }

    /**
     * OFD电子签名演示
     */
    @Test
    void testDigestSign() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        PrivateKey prvKey = ReadPrvKey(userP12Path, "777777");

        Path src = Paths.get("src/test/resources", "helloworld.ofd");
        Path out = Paths.get("target/DigestSign.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out)) {
            GMDigestSignatureContainer signContainer = new GMDigestSignatureContainer(prvKey);
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