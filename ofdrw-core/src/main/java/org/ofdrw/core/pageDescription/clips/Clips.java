package org.ofdrw.core.pageDescription.clips;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public Clips(Element proxy) {
        super(proxy);
    }

    public Clips() {
        super("Clips");
    }

    /**
     * 【必选】
     * 增加 图元对象的裁剪区域
     * <p>
     * 采用对象空间坐标系
     *
     * @param clip 图元对象的裁剪区域
     * @return this
     */
    public Clips addClip(CT_Clip clip) {
        if (clip == null) {
            return this;
        }
        this.add(clip);
        return this;
    }

    /**
     * 【必选】
     * 获取 图元对象的裁剪区域序列
     * <p>
     * 采用对象空间坐标系
     * <p>
     * 当存在多个 Clip 对象时，最终裁剪区为所有 Clip 区域的并集
     *
     * @return 图元对象的裁剪区域序列
     */
    public List<CT_Clip> getClips() {
        List<Element> elements = this.getOFDElements("Clip");
        if(elements == null || elements.size() == 0){
            return Collections.emptyList();
        }
        List<CT_Clip> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(new CT_Clip(item)));
        return res;
    }
}
