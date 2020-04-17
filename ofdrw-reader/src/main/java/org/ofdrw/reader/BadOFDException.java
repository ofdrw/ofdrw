package org.ofdrw.reader;

/**
 * 错误OFD文件结构和文档格式异常
 *
 * @author 权观宇
 * @since 2020-04-17 03:29:28
 */
public class BadOFDException extends RuntimeException {
    public BadOFDException() {
    }

    public BadOFDException(String message) {
        super(message);
    }

    public BadOFDException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadOFDException(Throwable cause) {
        super(cause);
    }
}
