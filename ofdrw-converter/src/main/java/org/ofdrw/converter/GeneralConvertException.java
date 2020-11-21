package org.ofdrw.converter;

/**
 * 转换通用错误
 *
 * @author 权观宇
 * @since 2020-11-20 19:18:28
 */
public class GeneralConvertException extends RuntimeException {

    public GeneralConvertException() {
    }

    public GeneralConvertException(String message) {
        super(message);
    }

    public GeneralConvertException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeneralConvertException(Throwable cause) {
        super(cause);
    }
}
