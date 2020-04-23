package org.ofdrw.sign.verify;

import org.ofdrw.core.signatures.SigType;
import org.ofdrw.sign.verify.exceptions.InvalidSignedValueException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

/**
 * 签名数据验证容器
 *
 * @author 权观宇
 * @since 2020-04-22 02:25:08
 */
public interface SignedDataValidateContainer {

    /**
     * 签名数据验证
     * <p>
     * 如果验证不通过请抛出异常
     *
     * @param type        电子签名类型（Sign/Seal）
     * @param signAlgName 签名算法名称或OID
     * @param tbsContent  待签章内容
     * @param signedValue 电子签章数据或签名值（SignedValue.xml文件内容）
     * @throws InvalidSignedValueException 电子签章数据失效
     * @throws IOException                 IO异常
     * @throws GeneralSecurityException    运算过程中异常
     */
    void validate(SigType type, String signAlgName, byte[] tbsContent, byte[] signedValue) throws InvalidSignedValueException, IOException, GeneralSecurityException;
}
