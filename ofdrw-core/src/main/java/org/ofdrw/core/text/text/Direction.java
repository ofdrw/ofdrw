package org.ofdrw.core.text.text;

/**
 * 方向角度
 * <p>
 * 11.3 文字定位 表 47
 *
 * @author 权观宇
 * @since 2019-10-18 09:45:46
 */
public enum Direction {
    /**
     * 可选旋转角度 0、90、180、270
     */
    Angle_0(0),
    Angle_90(90),
    Angle_180(180),
    Angle_270(270);

    /**
     * 旋转角度
     */
    private Integer angle;
    Direction(int angle) {
        this.angle = angle;
    }

    public static Direction getInstance(String angleStr) {
        if (angleStr == null || angleStr.trim().length() == 0) {
            angleStr = "0";
        }
        switch (angleStr) {
            case "":
            case "0":
                return Angle_0;
            case "90":
                return Angle_90;
            case "180":
                return Angle_180;
            case "270":
                return Angle_270;
            default:
                throw new NumberFormatException("未知旋转角度：" + angleStr);
        }
    }

    @Override
    public String toString() {
        return angle.toString();
    }
}
