package org.ofdrw.core.action.actionType.actionGoto;

/**
 * 申明目标区域的描述方法
 * <p>
 * 表 54 目标区域属性
 *
 * @author 权观宇
 * @since 2019-10-05 08:39:59
 */
public enum DestType {
    /**
     * 目标区域由左上角位置（Left，Top）
     * 以及页面缩放比例（Zoom）确定
     */
    XYZ,
    /**
     * 适合整个窗口区域
     */
    Fit,
    /**
     * 适合窗口宽度，目标区域由Top确定
     */
    FitH,
    /**
     * 适合窗口高度，目标区域由Left确定
     */
    FitV,
    /**
     * 适合窗口内的目标区域，目标区域为
     * （Left，Top，Right，Bottom）所确定的矩形区域
     */
    FitR;

    /**
     * 获取目标区域实例
     *
     * @param type 类型字符串
     * @return 实例
     */
    public static DestType getInstance(String type) {
        type = type == null ? "" : type.trim();
        if (type.equalsIgnoreCase("XYZ")){
            return XYZ;
        } else if (type.equalsIgnoreCase("Fit")){
            return Fit;
        } else if (type.equalsIgnoreCase("FitH")){
            return FitH;
        } else if (type.equalsIgnoreCase("FitV")){
            return FitV;
        } else if (type.equalsIgnoreCase("FitR")){
            return FitR;
        } else {
            throw new IllegalArgumentException("未知目标区域类型：" + type);
        }
    }
}
