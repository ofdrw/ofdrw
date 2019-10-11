package org.ofdrw.core.graph;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

import java.util.ArrayList;
import java.util.List;

/**
 * 图形也可以使用 XML 负载类型的方式进行描述，这种方式主要用于
 * 区域（Region）。区域由一系列的分路径（Area）组成，每个路径都是闭合的.
 * <p>
 * 图 49 区域结构
 *
 * @author 权观宇
 * @since 2019-10-05 11:49:19
 */
public class CT_Region extends OFDElement {
    public CT_Region(Element proxy) {
        super(proxy);
    }

    public CT_Region() {
        super("Region");
    }

    /**
     * 【必选】
     * 为区域增加分路径
     *
     * @param area 路径
     * @return this
     */
    public CT_Region addArea(CT_Area area) {
        this.add(area);
        return this;
    }

    /**
     * 【必选】
     * 获取 区域中所有分路径
     *
     * @return 区域中所有分路径
     */
    public List<CT_Area> getAreas(){
        List<Element> es = this.elements();
        List<CT_Area> res = new ArrayList<>();
        es.forEach(item -> res.add(new CT_Area(item)));
        return res;
    }
}
