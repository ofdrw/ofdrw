package org.ofdrw.core.action.actionType.actionMovie;

/**
 * 放映参数属性
 * <p>
 * 表 59 放映参数属性
 *
 * @author 权观宇
 * @since 2019-10-06 05:56:41
 */
public enum PlayType {
    /**
     * 播放
     */
    Play,
    /**
     * 停止
     */
    Stop,
    /**
     * 暂停
     */
    Pause,
    /**
     * 继续
     */
    Resume;

    /**
     * 根据字符串类型获取 实例
     *
     * @param type 放映参数字符串
     * @return 实例
     */
    public static PlayType getInstance(String type) {
        type = type == null ? "" : type.trim();
        switch (type) {
            case "Play":
                return Play;
            case "Stop":
                return Stop;
            case "Pause":
                return Pause;
            case "Resume":
                return Resume;
            default:
                throw new IllegalArgumentException("未知的放映参数： " + type);
        }
    }
}
