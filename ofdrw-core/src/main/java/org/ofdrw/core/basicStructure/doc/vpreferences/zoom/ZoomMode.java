package org.ofdrw.core.basicStructure.doc.vpreferences.zoom;

import org.dom4j.Element;

/**
 * 自动缩放模式
 * <p>
 * 默认值为 Default
 * <p>
 * 7.5 表 9 视图首选项
 *
 * @author 权观宇
 * @since 2019-10-07 09:18:57
 */
public class ZoomMode extends ZoomScale {
    public ZoomMode(Element proxy) {
        super(proxy);
    }

    private ZoomMode() {
        super("ZoomMode");
    }

    private ZoomMode(String type) {
        this();
        this.addText(type);
    }

    public enum Type {
        /**
         * 默认缩放
         */
        Default,
        /**
         * 合适高度
         */
        FitHeight,
        /**
         * 合适宽度
         */
        FitWidth,
        /**
         * 合适区域
         */
        FitRect;
    }

    /**
     * 获取 自动缩放模式类型
     *
     * 类型参考{@link Type}
     *
     * @return 自动缩放模式类型
     */
    public Type getType() {
        String str = this.getText();
        switch (str) {
            case "Default":
                return Type.Default;
            case "FitHeight":
                return Type.FitHeight;
            case "FitWidth":
                return Type.FitWidth;
            case "FitRect":
                return Type.FitRect;
            default:
                throw new IllegalArgumentException("未知的自动缩放模式： " + str);
        }
    }

    /**
     * 获取工厂方式枚举的实例
     *
     * @param type 自动缩放模式类型
     * @return 自动缩放模式
     */
    public static ZoomMode getInstance(Type type) {
        return new ZoomMode(type.toString());
    }
}
