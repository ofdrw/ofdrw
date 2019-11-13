package org.ofdrw.core.basicStructure.res.resources;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.res.OFDResource;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 包含了一组颜色空间的描述
 * <p>
 * 7.9 图 20 表 18
 *
 * @author 权观宇
 * @since 2019-11-13 07:27:47
 */
public class ColorSpaces extends OFDElement implements OFDResource {
    public ColorSpaces(Element proxy) {
        super(proxy);
    }

    public ColorSpaces() {
        super("ColorSpaces");
    }

    /**
     * 【必选】
     * 增加  颜色空间描述
     * <p>
     * 必须要有ID属性
     *
     * @param colorSpace 颜色空间描述，必须要有ID属性
     * @return this
     */
    public ColorSpaces addColorSpace(CT_ColorSpace colorSpace) {
        if (colorSpace == null) {
            return this;
        }
        if (colorSpace.getID() == null) {
            throw new IllegalArgumentException("颜色空间ID不能为空");
        }
        this.add(colorSpace);
        return this;
    }

    /**
     * 【必选】
     * 获取  颜色空间描述列表
     *
     * @return 颜色空间描述列表
     */
    public List<CT_ColorSpace> getColorSpaces() {
        List<Element> elements = this.getOFDElements("ColorSpace");
        if (elements == null || elements.size() == 0) {
            return Collections.emptyList();
        }
        List<CT_ColorSpace> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(new CT_ColorSpace(item)));
        return res;
    }
}
