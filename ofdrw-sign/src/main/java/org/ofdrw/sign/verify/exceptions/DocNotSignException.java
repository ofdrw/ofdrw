package org.ofdrw.sign.verify.exceptions;



/**
 * 文件未签章异常
 *
 * @author 权观宇
 * @since 2020-04-22 11:01:18
 */
public class DocNotSignException extends OFDVerifyException {

    public DocNotSignException() {
    }

    public DocNotSignException(String msg) {
        super(msg);
    }

    public DocNotSignException(String message, Throwable cause) {
        super(message, cause);
    }
}
