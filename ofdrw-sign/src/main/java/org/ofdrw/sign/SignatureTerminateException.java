package org.ofdrw.sign;

/**
 * 签名终止异常
 * <p>
 * 表示该文档不允许在进行签名
 *
 * @author 权观宇
 * @since 2020-04-17 03:10:39
 */
public class SignatureTerminateException extends SignatureException {


    public SignatureTerminateException() {
    }

    public SignatureTerminateException(String s) {
        super(s);
    }

    public SignatureTerminateException(String message, Throwable cause) {
        super(message, cause);
    }
}
