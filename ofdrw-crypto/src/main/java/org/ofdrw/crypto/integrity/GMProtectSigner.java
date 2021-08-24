package org.ofdrw.crypto.integrity;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.gm.sm2strut.SignedData;
import org.ofdrw.gm.sm2strut.builder.SignedDataBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;

/**
 * 以国密 SM2 和 SM3 算法实现的OFD完整性保护签名实现
 * <p>
 * 签名值符合 《GB/T 35275》标准
 *
 * @author 权观宇
 * @since 2021-08-23 19:07:54
 */
public class GMProtectSigner implements ProtectSigner {

    /**
     * 版式文件合成者的签名私钥
     */
    private PrivateKey privateKey;

    /**
     * 版式文件合成者的公钥证书
     */
    private Certificate signCert;

    /**
     * 完整性保护签名实现
     *
     * @param privateKey 版式文件合成者的签名私钥
     * @param signCert   版式文件合成者的公钥证书
     */
    public GMProtectSigner(@NotNull PrivateKey privateKey, @NotNull Certificate signCert) {
        if (privateKey == null) {
            throw new IllegalArgumentException("版式文件合成者的签名私钥(privateKey)为空");
        }
        if (signCert == null) {
            throw new IllegalArgumentException("版式文件合成者的公钥证书(signCert)为空");
        }
        this.privateKey = privateKey;
        this.signCert = signCert;
    }

    /**
     * 根据 GM/T 0099  OFD完整性保护协议 执行完整性保护 7.4.3 中的 c) d) 步骤
     * <p>
     * c) 根据签名方案，计算完整性保护文件的杂凑值；
     * d) 根据签名方案，使用版式文件合成者的签名私钥对杂凑值进行数字签名；
     *
     * @param tbs 待签名文件路径
     * @return 签名值应符合“GB/T 35275”标准
     * @throws GeneralSecurityException 安全计算异常
     */
    @Override
    public byte[] digestThenSign(Path tbs) throws GeneralSecurityException, IOException {
        // 杂凑算法采用SM3时，遵循《GB/32905》
        MessageDigest md = new SM3.Digest();
        final byte[] raw = Files.readAllBytes(tbs);
        md.update(raw);
        // 计算完整性保护文件的杂凑值,作为待签名原文。
        final byte[] plaintext = md.digest();
        // 签名算法采用SM2时，遵循 《GB/T32918》和《GB/T 35275》
        Signature sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initSign(this.privateKey);
        sg.update(plaintext);
        // 签名算法采用SM2时，遵循 《GB/T32918》
        final byte[] signature = sg.sign();
        // 构造 《GB/T 35275》数据格式
        final SignedData signedData = SignedDataBuilder.signedData(plaintext, signature, this.signCert);
        // DER编码ASN1结构
        final byte[] encoded = signedData.getEncoded();
        System.out.println(        Base64.toBase64String(encoded));
        return encoded;
    }
}
