package org.ofdrw.core.signatures;

/**
 * 签名节点的类型
 * <p>
 * 目前规定了两个可选值
 *
 * 18.1 签名列表 图 85 表 66
 *
 * @author 权观宇
 * @since 2019-11-20 06:59:41
 */
public enum SigType {
    /**
     * 安全签章
     * <p>
     * 默认值
     */
    Seal,
    /**
     * 纯数字签名
     */
    Sign;

    public static SigType getInstance(String str) {
        str = (str == null) ? "" : str.trim();
        switch (str) {
            case "":
            case "Seal":
                return Seal;
            case "Sign":
                return Sign;
            default:
                throw new IllegalArgumentException("未知的签名节点的类型：" + str);
        }
    }
}
