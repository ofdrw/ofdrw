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
        str = str == null ? "" : str.trim();
        if (str.equalsIgnoreCase("Image")) {
            return Image;
        } else if (str.equalsIgnoreCase("Audio")) {
            return Audio;
        } else if (str.equalsIgnoreCase("Video")) {
            return Video;
        } else {
            throw new IllegalArgumentException("多媒体格式不支持：" + str);
        }
        // switch (str.toLowerCase()) {
        //     case "image":
        //         return Image;
        //     case "audio":
        //         return Audio;
        //     case "video":
        //         return Video;
        //     default:
        //         throw new IllegalArgumentException("多媒体格式不支持：" + str);
        // }
    }


}
