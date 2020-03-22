package org.ofdrw.core.basicStructure.pageObj.layer.block;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.image.CT_Image;

/**
 * 图像对象
 * <p>
 * 见 10
 * <p>
 * 带有播放视频动作时，见第 12 章
 * <p>
 * 7.7 表 16
 *
 * @author 权观宇
 * @since 2019-10-29 17:20:01
 */
public class ImageObject extends CT_Image implements PageBlockType {

    public ImageObject(Element proxy) {
        super(proxy);
    }


    private ImageObject() {
        super("ImageObject");
    }

    public ImageObject(ST_ID id) {
        this();
        this.setObjID(id);
    }

    public ImageObject(long id){
        this(new ST_ID(id));
    }

    /**
     * 【必选 属性】
     * 设置 对象ID
     *
     * @param id 对象ID
     * @return this
     */
    public ImageObject setID(ST_ID id) {
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
