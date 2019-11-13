package org.ofdrw.core.compositeObj;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CompositeObject;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.CT_GraphicUnit;

/**
 * 复合对象
 * <p>
 * 复合对象是一种特殊的图元对象，拥有图元对象的一切特性，
 * 但其内容在ResourceID指向的矢量图像资源中进行描述，
 * 一个资源可以被多个复合对象所引用。通过这种方式可实现对
 * 文档内矢量图文内容的服用。
 * <p>
 * 13 复合对象 图 71 表 49
 *
 * @author 权观宇
 * @since 2019-10-27 04:08:12
 */
public class CT_Composite extends CT_GraphicUnit<CT_Composite> {

    public CT_Composite(Element proxy) {
        super(proxy);
    }

    public CT_Composite() {
        super("Composite");
    }

    protected CT_Composite(String name) {
        super(name);
    }

    /**
     * 构造复合对象
     *
     * @param id 对象ID
     * @return 对象
     */
    public CompositeObject toObj(ST_ID id) {
        this.setOFDName("CompositeObject");
        this.setObjID(id);
        return new CompositeObject(this);
    }

    /**
     * 【必选 属性】
     * 设置 引用资源文件中定义的矢量图像的标识
     * <p>
     * 复合对象引用的资源时 Res 中的矢量图像（CompositeGraphUnit）
     * ，其类型为 CT_VectorG，其结构如 72 所示
     *
     * @param resourceId 引用资源文件中定义的矢量图像的标识ID
     * @return this
     */
    public CT_Composite setResourceID(ST_RefID resourceId) {
        if (resourceId == null) {
            throw new IllegalArgumentException("矢量图像的标识（ResourceID）不能为空");
        }
        this.addAttribute("ResourceID", resourceId.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 引用资源文件中定义的矢量图像的标识
     * <p>
     * <p>
     * 复合对象引用的资源时 Res 中的矢量图像（CompositeGraphUnit）
     * ，其类型为 CT_VectorG，其结构如 72 所示
     *
     * @return 引用资源文件中定义的矢量图像的标识ID
     */
    public ST_RefID getResourceID() {
        return ST_RefID.getInstance(this.attributeValue("ResourceID"));
    }
}
