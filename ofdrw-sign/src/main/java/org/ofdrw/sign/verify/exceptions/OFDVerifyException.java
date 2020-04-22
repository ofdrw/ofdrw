package org.ofdrw.sign.verify.exceptions;

import java.security.GeneralSecurityException;

/**
 * OFD 验证异常
 *
 * @author 权观宇
 * @since 2020-04-22 01:11:58
 */
public class OFDVerifyException extends GeneralSecurityException {
    public OFDVerifyException() {
    }

    public OFDVerifyException(String msg) {
        super(msg);
    }

    public OFDVerifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
