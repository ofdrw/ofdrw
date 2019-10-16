package org.ofdrw.core.pageDescription.clips;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

/**
 * 图元对象的裁剪区域序列
 * <p>
 * 采用对象空间坐标系
 * <p>
 * 当存在多个 Clip对象时，最终裁剪区为所有 Clip区域交集。
 * <p>
 * 8.5 图元对象 图 45 表 34
 *
 * @author 权观宇
 * @since 2019-10-14 08:00:02
 */
public class Clips extends OFDElement {
    // TODO 2019-10-15 20:33:39 Clip

    public Clips(Element proxy) {
        super(proxy);
    }

    public Clips() {
        super("Clips");
    }
}
