package org.ofdrw.sign.stamppos;

import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.gm.ses.v4.SESeal;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.NumberFormatAtomicSignID;
import org.ofdrw.sign.OFDSigner;
import org.ofdrw.sign.SignMode;
import org.ofdrw.sign.signContainer.SESV4Container;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 骑缝章测试
 *
 * @author 权观宇
 * @since 2020-04-25 12:31:43
 */
class RidingStampPosTest {

    @Test
    public void rideRight() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "VPage7.ofd");
        Path out = Paths.get("target/V4RideRight.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out)) {
            SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new RidingStampPos(Side.Right, 40.0, 40, 40));
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }

    @Test
    public void rideLeft() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "VPage7.ofd");
        Path out = Paths.get("target/V4RideLeft.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out)) {
            SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new RidingStampPos(Side.Left, 40.0, 40, 40));
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }

    @Test
    public void rideBottom() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "VPage7.ofd");
        Path out = Paths.get("target/V4RideBottom.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out)) {
            SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new RidingStampPos(Side.Bottom, 40.0, 40, 40));
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }

    @Test
    public void rideTop() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "VPage7.ofd");
        Path out = Paths.get("target/V4RideTop.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out)) {
            SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new RidingStampPos(Side.Top, 40.0, 40, 40));
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }

    @Test
    public void rideRightOffset() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "VPage7.ofd");
        Path out = Paths.get("target/V4RideRightOffset.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out, new NumberFormatAtomicSignID())) {
            SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new RidingStampPos(Side.Right, 10d, 40, 40));
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }

    @Test
    public void rideTopOffset() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "VPage7.ofd");
        Path out = Paths.get("target/V4RideTopOffset.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out,new NumberFormatAtomicSignID())) {
            SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new RidingStampPos(Side.Top, 10d, 40, 40));
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }

    @Test
    public void rideRightOffsetMaginClipNumber() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "VPage7.ofd");
        Path out = Paths.get("target/V4RideRightOffsetMaginClipNumber.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out, new NumberFormatAtomicSignID())) {
            SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new RidingStampPos(Side.Right, 10d, 3, 40, 40, 1));
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }


    @Test
    public void rideCuttingOnRight() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV4.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "VPage7.ofd");
        Path out = Paths.get("target/V4RideRightOffsetMaginClipNumber.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out, new NumberFormatAtomicSignID())) {
            SESV4Container signContainer = new SESV4Container(prvKey, seal, signCert);
            // 2. 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 设置显示位置
            signer.addApPos(new CuttingRideStampPos(Side.Right, null, 40, 40, 0, null));
            // 5. 执行签名
            signer.exeSign();
            // 6. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }
}