package org.ofdrw.sign.verify;

import org.ofdrw.core.signatures.SigType;
import org.ofdrw.sign.verify.exceptions.SESInvalidException;

import java.io.IOException;

/**
 * 电子印章数据验证容器
 *
 * @author 权观宇
 * @since 2020-04-22 02:25:08
 */
public interface SESValidateContainer {

    /**
     * 验证电子签章数据
     * <p>
     * 如果验证不通过请抛出异常
     *
     * @param type         电子签名类型（Sign/Seal）
     * @param tbsContent   待签章内容
     * @param sesSignature 电子签章数据
     * @throws SESInvalidException 电子签章数据失效
     * @throws IOException         IO异常
     */
    void validate(SigType type, byte[] tbsContent, byte[] sesSignature) throws SESInvalidException, IOException;
}
