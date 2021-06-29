package org.ofdrw.gm.support;

import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-06-29 19:50:06
 */
class KDFTest {

    @Test
    void extend() {
        byte[] z, exp, actual;
        int klen;
        SM3.Digest h = new SM3.Digest();


        z = Hex.decode("0B8CE0B16A4583A8643FF486DB0A9D7B1B95ED5444DE6E2A7E67E3924C2FA9");
        klen = 64;
        exp = Hex.decode("5F868E0706F2D4F72B7C10D9937A50D146BC843C07E970DD6B9E7724CFE6224BC65817353192B29672392F806DCA491929630E0254DCF91656524131E6E20BF8");
        actual = KDF.extend(h, z, klen);
        assertArrayEquals(exp, actual);


        z = Hex.decode("74C993D3D9BAF368CAB6248177CE8EB3A9457C3629D7B163A3010FB4037F6873B6");
        klen = 16;
        exp = Hex.decode("B737DBAA4B9BBAB19EE2EE423B9C3520");
        actual = KDF.extend(h, z, klen);
        assertArrayEquals(exp, actual);

        z = Hex.decode("BD719E7161906CC4");
        klen = 64;
        exp = Hex.decode("B9170304308D1D2AACF1B69F33588F1601EB3070CC76CB8A4B741A1DB6FCE222B40D1914C126E503A89974E2BFC1DE842ECD67419BDCBA3D656B8E45E1F881B8");
        actual = KDF.extend(h, z, klen);
        assertArrayEquals(exp, actual);
    }
}