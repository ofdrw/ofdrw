package org.ofdrw.crypto.integrity;

import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.gm.sm2strut.SignedData;
import org.ofdrw.gm.sm2strut.builder.SignedDataBuilder;
import org.ofdrw.sign.verify.SignedDataValidateContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Signature;

/**
 * 基于国密算法SM2 和 SM3算法的完整性保护签名值验证
 * <p>
 * 待签名值应符合 《GB/T 35275》标准
 *
 * @author 权观宇
 * @since 2021-08-24 20:12:27
 */
public class GMProtectVerifier implements ProtectVerifier {

    public SignedDataValidateContainer verifyContainer;

    /**
     * 创建 国密算法SM2 和 SM3算法的完整性保护签名值验证者
     *
     * @param verifyContainer 能够验证 GM/T 0099-2020 signedData的容器，推荐使用 {@link org.ofdrw.sign.verify.container.GBT35275ValidateContainer}
     */
    public GMProtectVerifier(@NotNull SignedDataValidateContainer verifyContainer) {
        if (verifyContainer == null) {
            throw new IllegalArgumentException("签名验证容器(verifyContainer)为空");
        }
        this.verifyContainer = verifyContainer;
    }

    /**
     * 根据 GM/T 0099  OFD完整性保护协议 执行完整性保护 7.4.6
     * <p>
     * b) 根据签名方案，调用杂凑算法计算完整性保护文件得到杂凑值
     * c) 读取签名值文件，进行签名验证
     *
     * @param ofdEntriesXmlPath 防夹带文件路径
     * @param signedValue       待验证的签名值，数据结构参照 《GM/T 0099 2020》 7.4.3 密码算法要求
     * @return 签名值验证结果
     * @throws GeneralSecurityException 安全计算异常
     * @throws IOException              IO读写异常
     */
    @Override
    public boolean digestThenVerify(@NotNull Path ofdEntriesXmlPath, @NotNull byte[] signedValue) throws GeneralSecurityException, IOException {
        final byte[] tbs = Files.readAllBytes(ofdEntriesXmlPath);
        // 验证签名
        verifyContainer.validate(SigType.Sign, "SM3withSm2", tbs, signedValue);
        // 如果没有抛出异常表示验证成功
        return true;
    }
}
