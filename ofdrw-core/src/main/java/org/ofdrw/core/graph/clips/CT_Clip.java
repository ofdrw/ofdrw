package org.ofdrw.core.graph.clips;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

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

    // TODO 2019-10-15 20:16:30 Area
}
