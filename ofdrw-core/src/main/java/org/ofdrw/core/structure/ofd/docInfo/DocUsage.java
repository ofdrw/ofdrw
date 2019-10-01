package org.ofdrw.core.structure.ofd.docInfo;

/**
 * 文档分类
 *
 * @author 权观宇
 * @since 2019-10-01 05:22:41
 */
public enum DocUsage {
    /**
     * 普通文档
     */
    Normal,
    /**
     * 电子书
     */
    EBook,
    /**
     * 电子报纸
     */
    ENewsPaper,
    /**
     * 电子期刊
     */
    EMagzine;

    /***
     * 获取文档分类示例
     *
     * 默认值： Normal
     * @param usage 文档分类值
     * @return 实例
     */
    public static DocUsage getInstance(String usage) {
        switch (usage) {
            case "Normal":
                return Normal;
            case "EBook":
                return EBook;
            case "ENewsPaper":
                return ENewsPaper;
            case "EMagzine":
                return EMagzine;
            default:
                return Normal;
        }
    }
}
