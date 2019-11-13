package org.ofdrw.core.basicStructure.res;

/**
 * 多媒体格式。
 * <p>
 * 7.9 资源 图 21 表 19
 *
 * @author 权观宇
 * @since 2019-11-13 07:58:38
 */
public enum MediaType {
    /**
     * 位图图像
     */
    Image,
    /**
     * 音频
     */
    Audio,
    /**
     * 视频
     */
    Video;

    public static MediaType getInstance(String str) {
        switch (str) {
            case "Image":
                return Image;
            case "Audio":
                return Audio;
            case "Video":
                return Video;
            default:
                throw new IllegalArgumentException("多媒体格式不支持：" + str);
        }
    }


}
