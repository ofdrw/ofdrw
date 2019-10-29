package org.ofdrw.core.basicStructure.pageObj.layer.block;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.graph.pathObj.CT_Path;

/**
 * 图形对象
 * <p>
 * 见 9.1
 * <p>
 * 7.7 表 16
 *
 * @author 权观宇
 * @since 2019-10-29 05:14:52
 */
public class PathObject extends CT_Path implements PageBlockType {

    public PathObject(Element proxy) {
        super(proxy);
    }

    private PathObject() {
        super("PathObject");
    }

    public PathObject(ST_ID id){
        this();
        this.setObjID(id);
    }


    /**
     * 【必选 属性】
     * 设置 对象ID
     *
     * @param id 对象ID
     * @return this
     */
    public PathObject setID(ST_ID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        this.setObjID(id);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 对象ID
     *
     * @return 对象ID
     */
    public ST_ID getID() {
        return this.getObjID();
    }
}
