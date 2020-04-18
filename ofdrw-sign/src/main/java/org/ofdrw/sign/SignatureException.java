package org.ofdrw.sign;

/**
 * 电子签名通用异常
 *
 * @author 权观宇
 * @since 2020-04-18 20:45:47
 */
public class SignatureException extends SecurityException {
    public SignatureException() {
    }

    public SignatureException(String s) {
        super(s);
    }

    public SignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
