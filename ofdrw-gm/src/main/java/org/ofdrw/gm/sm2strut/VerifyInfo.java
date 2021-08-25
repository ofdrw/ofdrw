package org.ofdrw.gm.sm2strut;

/**
 * 签名验证信息
 *
 * @author 权观宇
 * @since 2021-08-25 19:21:24
 */
public class VerifyInfo {
    /**
     * 验证是否通过
     * true - 通过
     * false - 不通过
     */
    public boolean result;
    /**
     * 不通过时提供错误信息
     */
    public String hit;

    public VerifyInfo(boolean result, String hit) {
        this.result = result;
        this.hit = hit;
    }

    /**
     * 获取错误返还值
     *
     * @param errDesp 错误描述
     * @return 实例
     */
    public static VerifyInfo Err(String errDesp) {
        return new VerifyInfo(false, errDesp);
    }

    /**
     * 获取验证成功状态
     *
     * @return 状态信息
     */
    public static VerifyInfo OK() {
        return new VerifyInfo(true, "");
    }
}
