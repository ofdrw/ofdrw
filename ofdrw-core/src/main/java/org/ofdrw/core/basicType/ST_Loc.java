package org.ofdrw.core.basicType;

/**
 * 包结构内文件的路径，“.”表示当前路径，“..”表示符路径
 * <p>
 * 约定：
 * 1. “、”代表根节点；
 * 2. 未显示指定代表当前路径；
 * 3. 路径区分大小写
 *
 * 示例：
 * <code>
 *     /Pages/P1/Content.xml
 *     ./Res/Book1.jpg
 *     ../Pages/P1/Res.xml
 *     Pages/P1/Rcs.xml
 * </code>
 * ————《GB/T 33190-2016》 表 2 基本数据类型
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

    public String getLoc() {
        return loc;
    }

    public ST_Loc setLoc(String loc) {
        this.loc = loc;return this;
    }

    @Override
    public String toString() {
        return loc;
    }


}
