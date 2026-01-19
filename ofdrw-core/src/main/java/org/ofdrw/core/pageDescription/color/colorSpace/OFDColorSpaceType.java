package org.ofdrw.core.pageDescription.color.colorSpace;

/**
 * 颜色空间的类型
 * <p>
 * 8.3.1 表 25 颜色空间属性
 *
 * @author 权观宇
 * @since 2019-10-11 08:07:31
 */
public enum OFDColorSpaceType {
    /**
     * 灰度
     */
    GRAY,
    /**
     * 红绿蓝
     */
    RGB,
    /**
     * 印刷颜色
     */
    CMYK;

    /**
     * 获取实例
     * 
     * @param type 类型字符串
     * @return 实例
     * @throws IllegalArgumentException 未知的颜色空间类型
     */
    public static OFDColorSpaceType getInstance(String type) {
        type = (type == null) ? "" : type.trim();

        // switch (type) {
        // case "GRAY":
        // return GRAY;
        // case "RGB":
        // return RGB;
        // case "CMYK":
        // return CMYK;
        // default:
        // throw new IllegalArgumentException("未知的颜色空间类型：" + type);
        // }
        if (type.equalsIgnoreCase("GRAY")) {
            return GRAY;
        } else if (type.equalsIgnoreCase("RGB")) {
            return RGB;
        } else if (type.equalsIgnoreCase("CMYK")) {
            return CMYK;
        } else {
            throw new IllegalArgumentException("未知的颜色空间类型：" + type);
        }
    }
}
