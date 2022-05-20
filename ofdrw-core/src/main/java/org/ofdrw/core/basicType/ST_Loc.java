package org.ofdrw.core.basicType;


import org.dom4j.Element;

/**
 * 包结构内文件的路径，“.”表示当前路径，“..”表示符路径
 * <p>
 * 约定：
 * 1. “、”代表根节点；
 * 2. 未显示指定代表当前路径；
 * 3. 路径区分大小写
 * <p>
 * 示例：
 * <code>
 * /Pages/P1/Content.xml
 * ./Res/Book1.jpg
 * ../Pages/P1/Res.xml
 * Pages/P1/Rcs.xml
 * </code>
 * ————《GB/T 33190-2016》 表 2 基本数据类型
 *
 * @author 权观宇
 * @since 2019-09-28 10:40:33
 */
public class ST_Loc extends STBase {
    /**
     * 路径
     */
    private String loc;

    public ST_Loc(String path) {
        this.loc = path;
    }

    /**
     * 获取路径实例
     *
     * @param loc 路径字符串
     * @return 实例或null
     */
    public static ST_Loc getInstance(String loc) {
        if (loc == null || loc.trim().length() == 0) {
            return null;
        }
        return new ST_Loc(loc.trim());
    }

    /**
     * 从实例中获取 路径
     *
     * @param e 元素
     * @return 路径对象
     */
    public static ST_Loc getInstance(Element e) {
        if (e == null) {
            return null;
        }
        return getInstance(e.getTextTrim());
    }

    public String getLoc() {
        return loc;
    }

    public ST_Loc setLoc(String loc) {
        this.loc = loc;
        return this;
    }

    /**
     * 路径分割
     *
     * @return 各个子路径
     */
    public String[] split() {
        return this.loc.split("/");
    }

    /**
     * 获取父母路径
     *
     * @return 父母路径字符串
     */
    public String parent() {
        int indexOf = loc.lastIndexOf('/');
        if (indexOf == -1) {
            return "";
        }
        return loc.substring(0, indexOf);
    }

    /**
     * 获取父母路径
     *
     * @return 父母路径
     */
    public ST_Loc parentLoc() {
        return new ST_Loc(this.parent());
    }

    /**
     * 获取路径的文件名称
     *
     * @return 文件名称
     */
    public String getFileName() {
        int indexOf = loc.lastIndexOf('/');
        if (indexOf == -1) {
            return loc;
        } else if (indexOf == loc.length() - 1) {
            return "";
        }
        return loc.substring(indexOf + 1);
    }

    /**
     * 路径拼接
     *
     * @param p2 路径对象
     * @return 拼接后路径
     */
    public ST_Loc cat(String p2) {
        if (p2 == null) {
            return this;
        }

        String part1 = this.loc;
        if (part1.endsWith("/")) {
            part1 = part1.substring(0, part1.length() - 1);
        }
        if (p2.startsWith("/")) {
            p2 = p2.substring(1);
        }
        return new ST_Loc(part1 + "/" + p2);
    }

    /**
     * 路径拼接
     *
     * @param loc 路径对象
     * @return 拼接后路径
     */
    public ST_Loc cat(ST_Loc loc) {
        if (loc == null) {
            return this;
        }
        return cat(loc.getLoc());
    }

    /**
     * 是否以指定字符结尾
     *
     * @param suffix 指定字符
     * @return true 指定字符结尾；false 不以指定字符结尾
     */
    public boolean endWith(String suffix) {
        return loc.endsWith(suffix);
    }

    /**
     * 是否以指定字符开头
     *
     * @param prefix 前缀
     * @return true 指定字符开头；false 不以指定字符开头
     */
    public boolean startWith(String prefix) {
        return loc.startsWith(prefix);
    }

    /**
     * 是否为根路径（以"/"开头）
     *
     * @return true - 根路径
     */
    public boolean isRootPath() {
        return loc.startsWith("/");
    }

    @Override
    public String toString() {
        return loc;
    }

}
