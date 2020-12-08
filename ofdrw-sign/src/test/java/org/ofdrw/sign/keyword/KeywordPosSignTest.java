package org.ofdrw.sign.keyword;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.gm.ses.v4.SESeal;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.keyword.KeywordExtractor;
import org.ofdrw.reader.keyword.KeywordPosition;
import org.ofdrw.sign.NumberFormatAtomicSignID;
import org.ofdrw.sign.OFDSigner;
import org.ofdrw.sign.SignMode;
import org.ofdrw.sign.signContainer.SESV4Container;
import org.ofdrw.sign.stamppos.NormalStampPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.List;

/**
 * 关键字签章测试
 *
 * @author minghu-zhang
 * @since 2020-09-26 12:31:43
 */
class KeywordPosSignTest {

    /**
     * 多关键字，跨TextObject，后缀匹配测试
     *
     * @throws IOException 文件解析异常
     */
    @Test
    void multiKeywordSign() throws IOException, GeneralSecurityException, DocumentException {
        //签署文档路径
        Path src = Paths.get("src/test/resources/signedFile.ofd");
        // 签署输出路径
        Path out = Paths.get("target/MultiKeywordV4SignDoc.ofd");

        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        String[] keyword = {"备注", "销售方", "价金", "项目名称"};
        try (OFDReader reader = new OFDReader(src)) {
            List<KeywordPosition> positionList = KeywordExtractor.getKeyWordPositionList(reader, keyword);
            // 保证有且只有四个关键字返还
            Assertions.assertEquals(4, positionList.size());
            if (positionList.size() > 0) {
                try (OFDSigner signer = new OFDSigner(reader, out, new NumberFormatAtomicSignID())) {
                    SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
                    // 2. 设置签名模式
                    // signer.setSignMode(SignMode.WholeProtected);
                    signer.setSignMode(SignMode.ContinueSign);
                    // 3. 设置签名使用的扩展签名容器
                    signer.setSignContainer(signContainer);
                    for (KeywordPosition position : positionList) {
                        // 4. 中心点对齐签署
                        ST_Box box = position.getBox();
                        signer.addApPos(new NormalStampPos(position.getPage(), box.getTopLeftX() + box.getWidth() / 2 - 20,
                                box.getTopLeftY() + box.getHeight() / 2 - 20, 40, 40));
                    }

                    // 5. 执行签名
                    signer.exeSign();
                    // 6. 关闭签名引擎，生成文档。
                    System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
                }
            }

        }
    }


    /**
     * 获取ofd文本节点
     *
     * @throws IOException 文件解析异常
     */
    @Test
    void keywordSign() throws IOException, GeneralSecurityException, DocumentException {
        //签署文档路径
        Path src = Paths.get("src/test/resources/helloworld.ofd");
        // 签署输出路径
        Path out = Paths.get("target/KeywordV4SignDoc.ofd");

        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        String keyword = "好呀";

        try (OFDReader reader = new OFDReader(src)) {
            List<KeywordPosition> positionList = KeywordExtractor.getKeyWordPositionList(reader, keyword);
            // 保证有且只有一个关键字返还
            Assertions.assertEquals(1, positionList.size());
            if (positionList.size() > 0) {
                try (OFDSigner signer = new OFDSigner(reader, out, new NumberFormatAtomicSignID())) {
                    SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
                    // 2. 设置签名模式
                    // signer.setSignMode(SignMode.WholeProtected);
                    signer.setSignMode(SignMode.ContinueSign);
                    // 3. 设置签名使用的扩展签名容器
                    signer.setSignContainer(signContainer);
                    for (KeywordPosition position : positionList) {
                        // 4. 中心点对齐签署
                        ST_Box box = position.getBox();
                        signer.addApPos(new NormalStampPos(position.getPage(), box.getTopLeftX() + box.getWidth() / 2 - 20,
                                box.getTopLeftY() + box.getHeight() / 2 - 20, 40, 40));
                    }

                    // 5. 执行签名
                    signer.exeSign();
                    // 6. 关闭签名引擎，生成文档。
                    System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
                }
            }

        }
    }
}


