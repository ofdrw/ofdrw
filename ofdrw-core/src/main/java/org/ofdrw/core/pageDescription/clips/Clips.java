package org.ofdrw.core.pageDescription.clips;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

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
//        this.setTransFlag(false);
    }

    /**
     * 使用一个裁剪对象初始化裁剪序列
     *
     * @param clip 裁剪对象
     */
    public Clips(CT_Clip clip) {
        this();
        addClip(clip);
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
        return this.getOFDElements("Clip", CT_Clip::new);
    }

    /**
     * 【必选】
     * 设置变换标志
     * <p>
     * 在1.2 版本后通过TransFlag参数来控制，
     * 裁剪区域是否受外部CTM的影响，默认为false不受外部CTM影响。
     *
     * @param transFlag 是否受外部CTM影响
     * @return this
     */
    public Clips setTransFlag(Boolean transFlag) {
        if (transFlag == null) {
            this.removeAttr("TransFlag");
            return this;
        }
        this.addAttribute("TransFlag", Boolean.toString(transFlag));
        return this;
    }

    /**
     * 【必选】
     * 获取裁剪区域变换标志，默认值为false 不受外部CTM影响
     *
     * @return 是否受外部CTM影响，false 不受
     */
    public boolean getTransFlag() {
        String str = this.attributeValue("TransFlag");
        return Boolean.parseBoolean(str);
    }
}
