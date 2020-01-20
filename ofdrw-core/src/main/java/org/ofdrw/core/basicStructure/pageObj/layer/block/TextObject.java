package org.ofdrw.core.basicStructure.pageObj.layer.block;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.text.text.CT_Text;

/**
 * 文字对象
 * <p>
 * 见 11.2
 * <p>
 * 7.7 表 16
 *
 * @author 权观宇
 * @since 2019-10-29 05:13:54
 */
public class TextObject extends CT_Text implements PageBlockType {
    public TextObject(Element proxy) {
        super(proxy);
    }

    private TextObject() {
        super("TextObject");
    }

    /**
     * @param id 对象ID
     */
    public TextObject(ST_ID id) {
        this();
        this.setObjID(id);
    }

    /**
     * @param id 对象ID
     */
    public TextObject(long id) {
        this();
        this.setObjID(new ST_ID(id));
    }


    /**
     * 【必选 属性】
     * 设置 对象ID
     *
     * @param id 对象ID
     * @return this
     */
    public TextObject setID(ST_ID id) {
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
