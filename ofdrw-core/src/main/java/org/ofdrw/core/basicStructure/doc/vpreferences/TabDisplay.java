package org.ofdrw.core.basicStructure.doc.vpreferences;

/**
 * 标题栏显示模式
 * <p>
 * 默认值为 FileName，当设置为 DocTitle但不存在 Title属性时，
 * 按照 FileName 处理
 * <p>
 * 7.5 表 9 视图首选项
 *
 * @author 权观宇
 * @since 2019-10-07 09:07:50
 */
public enum TabDisplay {
    /**
     * 文件名称
     */
    FileName,
    /**
     * 呈现元数据中的 Title 属性
     */
    DocTitle;

    public static TabDisplay getInstance(String tabDisplay) {
        if (tabDisplay == null || tabDisplay.trim().length() == 0) {
            return FileName;
        }
        // switch (tabDisplay) {
        //     case "FileName":
        //         return FileName;
        //     case "DocTitle":
        //         return DocTitle;
        //     default:
        //         throw new IllegalArgumentException("未知的标题栏显示模式： " + tabDisplay);
        // }
        if (tabDisplay.equalsIgnoreCase("FileName")) {
            return FileName;
        } else if (tabDisplay.equalsIgnoreCase("DocTitle")) {
            return DocTitle;
        } else {
            throw new IllegalArgumentException("未知的标题栏显示模式： " + tabDisplay);
        }
    }
}
