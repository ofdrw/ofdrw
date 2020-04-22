package org.ofdrw.sign.verify.exceptions;

/**
 * 电子签章数据失效异常
 *
 * @author 权观宇
 * @since 2020-04-22 02:17:28
 */
public class SESInvalidException extends OFDVerifyException {
    /**
     * 失效原因
     */
    private String reason;

    /**
     * 状态码
     */
    private Integer code;

    public SESInvalidException(String reason) {
        super("电子签章数据失效：" + reason);
        this.reason = reason;
    }

    public SESInvalidException(String reason, Throwable cause) {
        super("电子签章数据失效：" + reason, cause);
        this.reason = reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getReason() {
        return reason;
    }
}
