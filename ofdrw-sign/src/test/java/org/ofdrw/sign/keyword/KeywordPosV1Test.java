package org.ofdrw.sign.keyword;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.gm.ses.v1.SESeal;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.OFDSigner;
import org.ofdrw.sign.SignMode;
import org.ofdrw.sign.signContainer.SESV1Container;
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
class KeywordPosV1Test {

    /**
     * 签署文档路径
     */
    private Path src = Paths.get("src/test/resources/keyword.ofd");
    /**
     * 签署输出路径
     */
    private Path out = Paths.get("target/KeywordV1SignDoc.ofd");

    /**
     * 获取ofd文本节点
     *
     * @throws IOException 文件解析异常
     */
    @Test
    void keywordSign() throws IOException, GeneralSecurityException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV1.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        String keyword = "办理";

        try (OFDReader reader = new OFDReader(src)) {
            List<KeywordPosition> positionList = KeywordExtractor.getKeyWordPositionList(reader, keyword);

            if (!positionList.isEmpty()) {
                OFDSigner signer = new OFDSigner(reader, out);

                SESV1Container signContainer = new SESV1Container(prvKey, seal, signCert);
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
                signer.close();
                // 6. 关闭签名引擎，生成文档。
                System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
            } else {
                System.out.println(">> 没有定位到关键字: " + keyword);
            }
        } catch (DocumentException e) {
            System.out.println(">> 文档解析异常: " + e.getMessage());
        }
    }


}