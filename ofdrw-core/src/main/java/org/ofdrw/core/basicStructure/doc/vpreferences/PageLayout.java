package org.ofdrw.core.basicStructure.doc.vpreferences;

/**
 * 页面布局
 * <p>
 * 7.5 表 9 视图首选项
 *
 * @author 权观宇
 * @since 2019-10-07 06:54:01
 */
public enum PageLayout {

    /**
     * 单页模式
     */
    OnePage,
    /**
     * 单列模式
     */
    OneColumn,
    /**
     * 对开模式
     */
    TwoPageL,
    /**
     * 对开连续模式
     */
    TwoColumnL,
    /**
     * 对开靠右模式
     */
    TwoPageR,
    /**
     * 对开连续靠右模式
     */
    TwoColumnR;

    public static PageLayout getInstance(String pageLayout) {
        pageLayout = pageLayout == null ? "" : pageLayout.trim();

        switch (pageLayout) {
            case "":
            case "OnePage":
                return OnePage;
            case "OneColumn":
                return OneColumn;
            case "TwoPageL":
                return TwoPageL;
            case "TwoColumnL":
                return TwoColumnL;
            case "TwoPageR":
                return TwoPageR;
            case "TwoColumnR":
                return TwoColumnR;
            default:
                throw new IllegalArgumentException("未知页面布局类型： " + pageLayout);
        }
    }
}
