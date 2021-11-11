package org.ofdrw.core.basicStructure.res.resources;


import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.res.OFDResource;
import org.ofdrw.core.compositeObj.CT_VectorG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 包含了一组矢量图像
 * <p>
 * 7.9 图 20 表 18
 *
 * @author 权观宇
 * @since 2019-11-13 07:36:05
 */
public class CompositeGraphicUnits extends OFDElement implements OFDResource {
    public CompositeGraphicUnits(Element proxy) {
        super(proxy);
    }

    public CompositeGraphicUnits() {
        super("CompositeGraphicUnits");
    }


    /**
     * 【必选】
     * 增加 矢量图像资源描述
     * <p>
     * 必须要有ID属性
     *
     * @param compositeGraphicUnit 矢量图像资源描述
     * @return this
     */
    public CompositeGraphicUnits addCompositeGraphicUnit(CT_VectorG compositeGraphicUnit) {
        if (compositeGraphicUnit == null) {
            return this;
        }
        if (compositeGraphicUnit.getID() == null) {
            throw new IllegalArgumentException("矢量图像资源描述ID不能为空");
        }
        compositeGraphicUnit.setOFDName("CompositeGraphicUnit");
        this.add(compositeGraphicUnit);
        return this;
    }

    /**
     * 【必选】
     * 获取 矢量图像资源描述序列
     * <p>
     * 必须要有ID属性
     *
     * @return 矢量图像资源描述
     */
    public List<CT_VectorG> getCompositeGraphicUnits() {
        return this.getOFDElements("CompositeGraphicUnit",CT_VectorG::new);
    }
}
