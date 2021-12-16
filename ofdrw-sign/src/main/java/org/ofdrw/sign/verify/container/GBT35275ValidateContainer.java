package org.ofdrw.sign.verify.container;

import org.ofdrw.core.signatures.SigType;
import org.ofdrw.gm.sm2strut.*;
import org.ofdrw.sign.verify.SignedDataValidateContainer;
import org.ofdrw.sign.verify.exceptions.InvalidSignedValueException;

import java.security.GeneralSecurityException;

/**
 * 根据 GM/T 0099-2020 7.2.2 数据格式要求
 * <p>
 * b) 签名类型为数字签名且签名算法使用SM2时，签名值数据应遵循 GB/T 35275
 * <p>
 * 数字签名验证容器
 *
 * @author 权观宇
 * @since 2021-8-9 16:15:11
 */
public class GBT35275ValidateContainer implements SignedDataValidateContainer {

    public GBT35275ValidateContainer() {
    }

    @Override
    public void validate(SigType type, String alg, byte[] tbsContent, byte[] signedValue)
            throws InvalidSignedValueException, GeneralSecurityException {
        if (type != SigType.Sign) {
            throw new IllegalArgumentException("签名类型(type)必须是 Sign，不支持电子印章验证");
        }
        final VerifyInfo verifyInfo = GBT35275Validate.validate(alg, tbsContent, signedValue);
        if (!verifyInfo.result) {
            throw new InvalidSignedValueException(verifyInfo.hit);
        }
    }
}
