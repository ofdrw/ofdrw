import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

public class KeyPairGenerate {


    @Test
    void gen() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        // 获取SM2椭圆曲线的参数
        final ECGenParameterSpec sm2Spec = new ECGenParameterSpec("sm2p256v1");
        // 获取一个椭圆曲线类型的密钥对生成器
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
        // 使用SM2参数初始化生成器
        kpg.initialize(sm2Spec);

        // 使用SM2的算法区域初始化密钥生成器
        kpg.initialize(sm2Spec, new SecureRandom());
        // 获取密钥对
        KeyPair keyPair = kpg.generateKeyPair();

        PublicKey pubKey = keyPair.getPublic();
        String pubKEnc = Base64.toBase64String(pubKey.getEncoded());
        System.out.println(">> Pub Key: " + pubKEnc);

        PrivateKey priKey = keyPair.getPrivate();
        String priKEnc = Base64.toBase64String(priKey.getEncoded());
        System.out.println(">> Pri Key: " + priKEnc);
    }
}
