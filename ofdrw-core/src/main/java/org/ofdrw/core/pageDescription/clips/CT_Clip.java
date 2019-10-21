package org.ofdrw.core.pageDescription.clips;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 裁剪区
 * <p>
 * 裁剪区由一组路径或文字构成，用以指定页面上的一个有效绘制区域，落在裁剪区
 * 意外的部分不受绘制指令的影响。
 * <p>
 * 一个裁剪区可由多个分路径（Area）组成，最终的裁剪范围是各个部分路径的并集。
 * 裁剪区中的数据均相对于所修饰图元对象的外界矩形。
 * <p>
 * 8.4 裁剪区 图 44 表 33
 *
 * @author 权观宇
 * @since 2019-10-15 08:15:58
 */
public class CT_Clip extends OFDElement {
    public CT_Clip(Element proxy) {
        super(proxy);
    }

    public CT_Clip() {
        super("Clip");
    }

    /**
     * 【必选】
     * 增加 裁剪区域
     * <p>
     * 用一个图形对象或文字对象来描述裁剪区的一个组成部分，
     * 最终裁剪区是这些区域的并集。
     *
     * @param area 裁剪区域
     * @return this
     */
    public CT_Clip addArea(Area area) {
        if (area == null) {
            return this;
        }
        this.add(area);
        return this;
    }

    /**
     * 【必选】
     * 设置 裁剪区域
     * <p>
     * 用一个图形对象或文字对象来描述裁剪区的一个组成部分，
     * 最终裁剪区是这些区域的并集。
     *
     * @return 裁剪区域
     */
    public List<Area> getAreas() {
        List<Element> elements = this.getOFDElements("Area");
        if(elements == null || elements.size() == 0){
            return Collections.emptyList();
        }
        List<Area> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(new Area(item)));
        return res;
    }
}
