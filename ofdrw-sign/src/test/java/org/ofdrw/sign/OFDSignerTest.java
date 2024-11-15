package org.ofdrw.sign;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.signatures.Signatures;
import org.ofdrw.gm.cert.PKCGenerate;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.gm.ses.v1.*;
import org.ofdrw.gm.ses.v4.SESeal;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.signContainer.SESV1Container;
import org.ofdrw.sign.signContainer.SESV4Container;
import org.ofdrw.sign.stamppos.NormalStampPos;
import org.ofdrw.sign.verify.OFDValidator;
import org.ofdrw.sign.verify.container.SESV4ValidateContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * OFD签名引擎测试
 *
 * @author 权观宇
 * @since 2020-04-18 11:10:43
 */
class OFDSignerTest {

    /**
     * 测试文档中没有Doc_0 只有Doc_1的情况签章
     *
     * @throws GeneralSecurityException _
     * @throws IOException              _
     */
    @Test
    void testOnlyDoc1Sign() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "doc1File.ofd");
        Path out = Paths.get("target/Doc1FileSigned.ofd");

        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out, new NumberFormatAtomicSignID())
        ) {
            SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new NormalStampPos(1, 50, 50, 40, 40));
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        // 验证
        try (OFDReader reader = new OFDReader(out);
             OFDValidator validator = new OFDValidator(reader)) {
            validator.setValidator(new SESV4ValidateContainer());
            validator.exeValidate();
            System.out.println(">> 验证通过");
        }

    }

    /**
     * 已经有印章的情况下再追加签章
     *
     * @throws GeneralSecurityException _
     * @throws IOException              _
     */
    @Test
    void reSignTest() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "signedFile.ofd");
        Path out = Paths.get("target/ReSign.ofd");

        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out, new NumberFormatAtomicSignID())
        ) {
            SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new NormalStampPos(1, 50, 50, 40, 40));
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }

        // 验证
        try (OFDReader reader = new OFDReader(out);
             OFDValidator validator = new OFDValidator(reader)) {
            validator.setValidator(new SESV4ValidateContainer());
            validator.exeValidate();
            System.out.println(">> 验证通过");
        }
    }

    /**
     * 文档全保护状态下签章测试，应该抛出异常
     *
     * @throws GeneralSecurityException _
     * @throws IOException              _
     */
    @Test
    void reSignExceptionTest() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "allprotected.ofd");
        Path out = Paths.get("target/ReSignException.ofd");
        Assertions.assertThrows(SignatureTerminateException.class, () -> {
            // 1. 构造签名引擎
            try (OFDReader reader = new OFDReader(src);
                 OFDSigner signer = new OFDSigner(reader, out, new NumberFormatAtomicSignID())
            ) {
                SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
                // 2. 设置签名模式
                signer.setSignMode(SignMode.WholeProtected);
                // 3. 设置签名使用的扩展签名容器
                signer.setSignContainer(signContainer);
                // 4. 设置显示位置
                signer.addApPos(new NormalStampPos(1, 50, 50, 40, 40));
                // 5. 执行签名
                signer.exeSign();
                // 6. 关闭签名引擎，生成文档。
            }
        });
    }

    /**
     * 过滤需要被保护的文件
     */
    @Test
    void setProtectFileFilter() throws IOException, GeneralSecurityException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "helloworld.ofd");
        Path out = Paths.get("target/filter_ed.ofd");
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
            signer.setProtectFileFilter(absPath -> {
                System.out.println(">> Filter protect file Path: " + absPath);
                if ("/OFD.xml".equals(absPath.toString())) {
                    return false;
                }
                return true;
            });
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());

    }

    /**
     * 设置本次签名的“基”签名，形成链式验证
     */
    @Test
    void setRelative() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "signedFile.ofd");
        Path out = Paths.get("target/linked_verify.ofd");

        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out, new NumberFormatAtomicSignID())
        ) {
            SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new NormalStampPos(1, 50, 50, 40, 40));

            // 借助Reader获取已经存在签名的信息
            final Signatures signatures = reader.getDefaultSignatures();
            // 获取第一个签名的ID
            final String relativeId = signatures.getSignatures().get(0).getID();
            // 设置 关联的ID，形成链式验证
            signer.setRelative(relativeId);
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }

    /**
     * 测试不标准的命名空间签名
     */
    @Test
    public void testNoStdNs() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "namespace_no_std.ofd");
        Path out = Paths.get("target/namespace_no_std_signed.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
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


    /**
     * 测试文档添加多个PNG章面的签章
     * <p>
     * 注意：该方法只用于生成测试数据，电子印章请使用符合国家规范的电子印章服务器生成管理！
     *
     * @author 张镇
     * @since 2024-11-15 16:26:33
     */
    @Test
    void sealtMorePng() throws Exception {
        Path src = Paths.get("src/test/resources", "doc1File.ofd");
        Path out = Paths.get("target/more_png_signed.ofd");

        Path userP12 = Paths.get("src/test/resources", "USER.p12");
        Path sealerP12 = Paths.get("src/test/resources", "SealBuilder.p12");
        Path picturePath1 = Paths.get("src/test/resources", "StampImg1.png");
        Path picturePath2 = Paths.get("src/test/resources", "StampImg2.png");


        SES_Header header = new SES_Header(new ASN1Integer(1), new DERIA5String("OFDR&WTest"));

        /*
         * 印章属性信息构造
         */
        // 获取用户证书
        Certificate userCert = PKCS12Tools.ReadUserCert(userP12, "private", "777777");
        ASN1EncodableVector v = new ASN1EncodableVector(1);
        v.add(new DEROctetString(userCert.getEncoded()));

        PrivateKey privateKey = PKCS12Tools.ReadPrvKey(sealerP12, "private", "777777");

        Calendar then = Calendar.getInstance();
        then.add(Calendar.YEAR, 2);
        // 印章属性信息
        SES_ESPropertyInfo property = new SES_ESPropertyInfo()
                .setType(SES_ESPropertyInfo.OrgType)
                .setName(new DERUTF8String("OFDRW测试用印章"))
                .setCertList(new DERSequence(v))
                .setCreateDate(new ASN1UTCTime(new Date()))
                .setValidStart(new ASN1UTCTime(new Date()))
                .setValidEnd(new ASN1UTCTime(then.getTime()));

        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out)) {

            // 设置签名模式
            signer.setSignMode(SignMode.ContinueSign);

            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                // 印章图片信息 构造
                Path pngPath;
                if(i%2 == 0){
                    pngPath = picturePath2;
                }else{
                    pngPath = picturePath1;
                }
                SES_ESPictrueInfo picture = new SES_ESPictrueInfo()
                        .setType("png")
                        .setData(Files.readAllBytes(pngPath))
                        .setWidth(100)
                        .setHeight(100);

                // 印章信息构造
                SES_SealInfo sealInfo = new SES_SealInfo()
                        .setHeader(header)
                        .setEsID(UUID.randomUUID().toString().replace("-", "").toUpperCase())
                        .setProperty(property)
                        .setPicture(picture);

                DEROctetString signCert = new DEROctetString(userCert.getEncoded());
                // 印章信息、制章人证书、签名算法标识符组成的信息作为签名原文
                ASN1EncodableVector v1 = new ASN1EncodableVector(3);
                v1.add(sealInfo);
                v1.add(signCert);
                v1.add(GMObjectIdentifiers.sm2sign_with_sm3);

                // 签名对象
                Signature signature = Signature.getInstance("SM3withSm2", new BouncyCastleProvider());
                signature.initSign(privateKey);
                signature.update(new DERSequence(v1).getEncoded(ASN1Encoding.DER));
                byte[] sign = signature.sign();
                // 印章签名信息
                SES_SignInfo signInfo = new SES_SignInfo()
                        .setCert(signCert)
                        .setSignatureAlgorithm(GMObjectIdentifiers.sm2sign_with_sm3)
                        .setSignData(sign);

                // 印章
                org.ofdrw.gm.ses.v1.SESeal seal = new org.ofdrw.gm.ses.v1.SESeal(sealInfo, signInfo);
                // 实现电子签章容器（这个签名容器是这个库实现的，生产肯定是自定义实现）
                ExtendSignatureContainer signContainer = new SESV1Container(privateKey, seal, userCert);
                // 设置签名使用的扩展签名容器
                signer.setSignContainer(signContainer);

                NormalStampPos normalStampPos = new NormalStampPos(i,
                        10, 10,
                        100, 100);
                signer.addApPos(normalStampPos);

                // 执行签名
                signer.exeSign();
                // 清除签名对象List
                signer.cleanApPos();
            }

            System.out.println(">> 多章面签章签署完毕。生成文件位置: " + out.toAbsolutePath().toAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}