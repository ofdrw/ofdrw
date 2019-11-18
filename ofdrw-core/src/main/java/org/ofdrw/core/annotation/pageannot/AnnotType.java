package org.ofdrw.core.annotation.pageannot;

/**
 * 注释类型取值
 *
 * 15.2 表 62
 * @author 权观宇
 * @since 2019-11-16 02:26:03
 */
public enum AnnotType {
    /**
     * 连接注释
     */
    Link,
    /**
     * 路径注释，一般为图形对象，比如矩形、多边形、贝塞尔曲线等
     */
    Path,
    /**
     * 高亮注释
     */
    Highlight,
    /**
     * 签章注释
     */
    Stamp,
    /**
     * 水印注释
     */
    Watermark;

    public static AnnotType getInstance(String str) {
        switch (str) {
            case "Link":
                return Link;
            case "Path":
                return Path;
            case "Highlight":
                return Highlight;
            case "Stamp":
                return Stamp;
            case "Watermark":
                return Watermark;
            default:
                throw new IllegalArgumentException("未知的注释类型取值：" + str);
        }

    }
}
