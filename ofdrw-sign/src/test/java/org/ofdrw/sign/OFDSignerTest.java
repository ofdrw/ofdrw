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
     * 测试SM2公钥
     */
    public static final String PubKeyEnc = "MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAEpB5/ghLxG4mIuhwieT9LR4ytC40mepFKR83qAgC42q7bk71JwTbuHyWQtHDZsK3LXhMUWvZHJUxO7ZzgJEz2+g==";
    /**
     * 测试用SM2私钥
     */
    public static final String PriKeyEnc = "MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgmGm+OaFgYTVmCoGKBfWgdcBICj/QB+9ReUOu1TsJi4ugCgYIKoEcz1UBgi2hRANCAASkHn+CEvEbiYi6HCJ5P0tHjK0LjSZ6kUpHzeoCALjartuTvUnBNu4fJZC0cNmwrcteExRa9kclTE7tnOAkTPb6";

    /**
     * 获取测试用SM2密钥对
     *
     * @return 密钥对
     */
    public static KeyPair keyPair() throws GeneralSecurityException {
        KeyFactory keyFact = KeyFactory.getInstance("EC", new BouncyCastleProvider());
        // 根据采用的编码结构反序列化公私钥
        PublicKey pub = keyFact.generatePublic(new X509EncodedKeySpec(Base64.decode(PubKeyEnc)));
        PrivateKey priv = keyFact.generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(PriKeyEnc)));
        return new KeyPair(pub, priv);
    }

    @Test
    void testDigestSign() throws GeneralSecurityException, IOException {
        KeyPair keyPair = keyPair();

        Path src = Paths.get("src/test/resources", "helloworld.ofd");
        Path out = Paths.get("target/DigestSign.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out)) {
            GMDigestSignatureContainer signContainer = new GMDigestSignatureContainer(keyPair.getPrivate());
            // 2. 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 执行签名
            signer.exeSign();
            // 5. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }


    @Test
    void df() {
        System.out.println(OFDSigner.DF.format(LocalDateTime.now()));
    }

    @Test
    void fileWalk() throws IOException {
        Path path = Paths.get("src/test/resources", "helloworld.ofd");
        try (OFDReader reader = new OFDReader(path)) {
            OFDDir ofdDir = reader.getOFDDir();
            Path containerPath = ofdDir.getContainerPath();
            String cAbsP = FilenameUtils.separatorsToUnix(containerPath.toAbsolutePath().toString());
            List<ToDigestFileInfo> res = new LinkedList<>();
            Files.walkFileTree(containerPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String p = FilenameUtils.separatorsToUnix(file.toAbsolutePath().toString());
                    p = p.replace(cAbsP, "");
                    res.add(new ToDigestFileInfo(p, file));
                    System.out.println(">> " + p);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}