package org.ofdrw.font;

/**
 * 字体名称
 *
 * @author 权观宇
 * @since 2020-03-18 20:42:45
 */
public enum FontName {

    /**
     * 思源黑体
     */
    NotoSans("NotoSansCJKsc-Medium"),
    /**
     * 思源黑体-粗体
     */
    NotoSansBold("NotoSansCJKsc-Bold"),

    /**
     * 思源宋体
     */
    NotoSerif("NotoSerifCJKsc-Medium"),
    /**
     * 思源宋体-粗体
     */
    NotoSerifBold("NotoSerifCJKsc-Bold");

    private String fileName;

    FontName(String fileName) {
        this.fileName = fileName + ".otf";
    }

    /**
     * 获取字字体文件名带后缀
     *
     * @return 字体文件名
     */
    public String getFilename() {
        // TODO 修改为思源名称
        return fileName;
    }
}
