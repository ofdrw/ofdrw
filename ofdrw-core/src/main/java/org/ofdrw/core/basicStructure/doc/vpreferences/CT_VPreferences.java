package org.ofdrw.core.basicStructure.doc.vpreferences;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.doc.vpreferences.zoom.Zoom;
import org.ofdrw.core.basicStructure.doc.vpreferences.zoom.ZoomMode;
import org.ofdrw.core.basicStructure.doc.vpreferences.zoom.ZoomScale;

/**
 * 视图首选项
 * <p>
 * 本标准支持设置文档视图首选项（VPreferences）节点，以达到限定文档初始化视图
 * 便于阅读的目的。
 * <p>
 * 7.5 图 10 视图首选项结构
 *
 * @author 权观宇
 * @since 2019-10-07 05:55:03
 */
public class CT_VPreferences extends OFDElement {
    public CT_VPreferences(Element proxy) {
        super(proxy);
    }

    public CT_VPreferences() {
        super("VPreferences");
    }

    /**
     * 【可选】
     * 设置 窗口模式
     * <p>
     * 可选的模式列表，请参考 {@link PageMode}
     * <p>
     * 默认值为 None
     *
     * @param pageMode 窗口模式
     * @return this
     */
    public CT_VPreferences setPageMode(PageMode pageMode) {
        if (pageMode == null) {
            pageMode = PageMode.None;
        }
        this.addOFDEntity("PageMode", pageMode.toString());
        return this;
    }

    /**
     * 【可选】
     * 获取 窗口模式
     * <p>
     * 可选的模式列表，请参考 {@link PageMode}
     * <p>
     * 默认值为 None
     *
     * @return 窗口模式
     */
    public PageMode getPageMode() {
        String mode = this.getOFDElementText("PageMode");
        if (mode == null || mode.trim().length() == 0) {
            return PageMode.None;
        }
        return PageMode.getInstance(mode);
    }

    /**
     * 【可选】
     * 设置 页面布局模式
     * <p>
     * 可选的模式请参考{@link PageLayout}
     * <p>
     * 默认值为 OneColumn
     *
     * @param pageLayout 页面布局模式
     * @return this
     */
    public CT_VPreferences setPageLayout(PageLayout pageLayout) {
        pageLayout = pageLayout == null ? PageLayout.OneColumn : pageLayout;
        this.addOFDEntity("PageLayout", pageLayout.toString());
        return this;
    }

    /**
     * 【可选】
     * 获取 页面布局模式
     * <p>
     * 可选的模式请参考{@link PageLayout}
     * <p>
     * 默认值为 OneColumn
     *
     * @return 页面布局模式
     */
    public PageLayout getPageLayout() {
        String str = this.getOFDElementText("PageLayout");
        if (str == null || str.trim().length() == 0) {
            return PageLayout.OneColumn;
        }
        return PageLayout.getInstance(str);
    }

    /**
     * 【可选】
     * 设置 标题栏显示模式
     * <p>
     * 默认值为 FileName，当设置为 DocTitle但不存在 Title属性时，
     * 按照 FileName 处理
     *
     * @param tabDisplay 标题栏显示模式
     * @return this
     */
    public CT_VPreferences setTabDisplay(TabDisplay tabDisplay) {
        this.addOFDEntity("TabDisplay", tabDisplay.toString());
        return this;
    }

    /**
     * 【可选】
     * 获取 标题栏显示模式
     * <p>
     * 默认值为 FileName，当设置为 DocTitle但不存在 Title属性时，
     * 按照 FileName 处理
     *
     * @return 标题栏显示模式
     */
    public TabDisplay getTabDisplay() {
        String str = this.getOFDElementText("TabDisplay");
        if (str == null || str.trim().length() == 0) {
            return TabDisplay.FileName;
        }
        return TabDisplay.getInstance(str);
    }

    /**
     * 【可选】
     * 设置 是否隐藏工具栏
     * <p>
     * 默认值：false
     *
     * @param hideToolbar true - 隐藏； false - 不隐藏
     * @return this
     */
    public CT_VPreferences setHideToolbar(boolean hideToolbar) {
        this.addOFDEntity("HideToolbar", Boolean.toString(hideToolbar));
        return this;
    }

    /**
     * 【可选】
     * 获取 是否隐藏工具栏
     * <p>
     * 默认值：false
     *
     * @return true - 隐藏； false - 不隐藏
     */
    public Boolean getHideToolbar() {
        String str = this.getOFDElementText("HideToolbar");
        if (str == null || str.trim().length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }


    /**
     * 【可选】
     * 设置 是否隐藏菜单栏
     * <p>
     * 默认值：false
     *
     * @param hideMenubar true - 隐藏； false - 不隐藏
     * @return this
     */
    public CT_VPreferences setHideMenubar(boolean hideMenubar) {
        this.addOFDEntity("HideMenubar", Boolean.toString(hideMenubar));
        return this;
    }

    /**
     * 【可选】
     * 获取 是否隐藏菜单栏
     * <p>
     * 默认值：false
     *
     * @return true - 隐藏； false - 不隐藏
     */
    public Boolean getHideMenubar() {
        String str = this.getOFDElementText("HideMenubar");
        if (str == null || str.trim().length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }


    /**
     * 【可选】
     * 设置 是否隐藏主窗口之外的其他窗口组件
     * <p>
     * 默认值：false
     *
     * @param hideMenubar true - 隐藏； false - 不隐藏
     * @return this
     */
    public CT_VPreferences setHideWindowUI(boolean hideMenubar) {
        this.addOFDEntity("HideWindowUI", Boolean.toString(hideMenubar));
        return this;
    }

    /**
     * 【可选】
     * 获取 是否隐藏主窗口之外的其他窗口组件
     * <p>
     * 默认值：false
     *
     * @return true - 隐藏； false - 不隐藏
     */
    public Boolean getHideWindowUI() {
        String str = this.getOFDElementText("HideWindowUI");
        if (str == null || str.trim().length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }


    /**
     * 【可选】
     * 设置 文档自动缩放模式
     * <p>
     * 参考值{@link ZoomMode.Type}
     *
     * @param zoomMode 文档自动缩放模式
     * @return this
     */
    public CT_VPreferences setZoomMode(ZoomMode zoomMode) {
        // 从节点中删除所有可以选择的类型
        this.removeOFDElemByNames("Zoom", "ZoomMode");
        this.add(zoomMode);
        return this;
    }

    /**
     * 【可选】
     * 设置 文档的缩放率
     *
     * @param zoom 文档的缩放率
     * @return this
     */
    public CT_VPreferences setZoom(double zoom) {
        // 从节点中删除所有可以选择的类型
        this.removeOFDElemByNames("Zoom", "ZoomMode");
        this.add(new Zoom(zoom));
        return this;
    }

    /**
     * 【可选】
     * 获取 具体的缩放处理方式和值
     *
     * @return 具体的缩放处理方式和值 {@link Zoom} 或 {@link ZoomMode}
     */
    public ZoomScale getZoomScale(){
        Element zoomMode = this.getOFDElement("ZoomMode");
        if (zoomMode != null) {
            return new ZoomMode(zoomMode);
        }
        Element zoom = this.getOFDElement("Zoom");
        if (zoom != null) {
            return new Zoom(zoom);
        }
        return null;
    }
}
