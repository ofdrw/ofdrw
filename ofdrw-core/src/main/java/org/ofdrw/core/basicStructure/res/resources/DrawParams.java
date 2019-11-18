package org.ofdrw.core.basicStructure.res.resources;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.res.OFDResource;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 包含了一组绘制参数的描述
 * <p>
 * 7.9 图 20 表 18
 *
 * @author 权观宇
 * @since 2019-11-13 07:36:05
 */
public class DrawParams extends OFDElement implements OFDResource {
    public DrawParams(Element proxy) {
        super(proxy);
    }

    public DrawParams() {
        super("DrawParams");
    }

    /**
     * 【必选】
     * 增加 绘制参数描述
     * <p>
     * 必须要有ID属性
     *
     * @param drawParam 绘制参数描述
     * @return this
     */
    public DrawParams addDrawParam(CT_DrawParam drawParam) {
        if (drawParam == null) {
            return this;
        }
        if (drawParam.getID() == null) {
            throw new IllegalArgumentException("绘制参数描述ID不能为空");
        }
        this.add(drawParam);
        return this;
    }

    /**
     * 【必选】
     * 获取 绘制参数描述序列
     *
     * @return 绘制参数描述
     */
    public List<CT_DrawParam> getDrawParams() {
        return this.getOFDElements("DrawParam", CT_DrawParam::new);
    }
}
