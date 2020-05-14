package org.ofdrw.core.annotation.pageannot;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicType.ST_Box;

/**
 * 注释的静态呈现效果
 * <p>
 * 使用页面块定义来描述
 * <p>
 * 15.2 图 81 表 61
 *
 * @author 权观宇
 * @since 2019-11-19 05:48:28
 */
public class Appearance extends CT_PageBlock {

    public Appearance(Element proxy) {
        super(proxy);
    }

    public Appearance(ST_Box boundary) {
        super("Appearance");
        setBoundary(boundary);
    }

    /**
     * 【必选】
     * 设置 边界
     * <p>
     * 附录 A.4
     *
     * @param boundary 边界
     * @return this
     */
    public Appearance setBoundary(ST_Box boundary) {
        if (boundary == null) {
            throw new IllegalArgumentException("Boundary 不能为空");
        }
        this.addAttribute("Boundary", boundary.toString());
        return this;
    }

    /**
     * 【必选】
     *
     * @return 边界
     */
    public ST_Box getBoundary() {
        return ST_Box.getInstance(this.attributeValue("Boundary"));
    }

    /**
     * 【可选】
     * 增加 页块
     * <p>
     * 一个页块中可以嵌套其他页块，可含有0到多个页块
     *
     * @param pageBlock 页块实例
     * @return this
     */
    @Override
    public Appearance addPageBlock(PageBlockType pageBlock) {
        super.addPageBlock(pageBlock);
        return this;
    }
}
