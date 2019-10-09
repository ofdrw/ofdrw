package org.ofdrw.core.basicStructure.pageObj.layer;

/**
 * 图层类型
 * <p>
 * 统称类型分为前景层、正文层、背景层，这些层按照出现的
 * 先后顺序依次进行渲染，每一层的默认颜色采用透明。
 * <p>
 * 层的渲染顺序如下图 （图 16 图层渲染顺序）
 * <code>
 * ---------- 最上层
 * 前景层
 * ----------
 * [前景模板]
 * ----------
 * 正文层
 * ----------
 * [正文层]
 * ----------
 * 背景层
 * ----------
 * [背景层]
 * ---------- 最下层
 * </code>
 * <p>
 * 7.7 页对象
 *
 * @author 权观宇
 * @since 2019-10-09 10:01:01
 */
public enum Type {
    /**
     * 正文层
     */
    Body,
    /**
     * 前景层
     */
    Foreground,
    /**
     * 背景层
     */
    Background;

    /**
     * 获取图层类型实例
     *
     * @param type 图层类型字符串
     * @return 图层类型
     */
    public static Type getInstance(String type) {
        switch (type) {
            case "Body":
                return Body;
            case "Foreground":
                return Foreground;
            case "Background":
                return Background;
            default:
                throw new IllegalArgumentException("未知的图层类型：" + type);
        }
    }
}
