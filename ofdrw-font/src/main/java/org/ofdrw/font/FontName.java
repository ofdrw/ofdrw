package org.ofdrw.font;

/**
 * 字体名称
 *
 * @author 权观宇
 * @since 2020-03-18 20:42:45
 */
public enum FontName {
    FangSong,
    KaiTi,
    SimHei,
    SimSun,
    TimesNewRoman,
    YaHei;

    /**
     * 获取字字体文件名带后缀
     *
     * @return 字体文件名
     */
    public String getFilename() {
        return this.toString() + ".ttf";
    }
}
