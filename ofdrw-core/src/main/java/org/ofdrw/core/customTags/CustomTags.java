package org.ofdrw.core.customTags;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

import java.util.List;

/**
 * 自定义标引
 * <p>
 * 外部系统或用户可以添加自定义标记和信息，从而达到与其他系统、数据
 * 进行交互的目的并扩展应用。一个文档可以带有多个自定义标引。
 * <p>
 * 自定义标引列表的入口点在 7.5 文档根节点中定义。
 * <p>
 * 标引索引文件，标引文件中通过ID引用于被引用标引对象
 * 发生“非接触式（分离式）”关联。标引内容可任意扩展，
 * 但建议给出扩展内容的规范约束文件（schema）或命名空间。
 *
 * 16 图 82 表 63
 *
 * @author 权观宇
 * @since 2019-11-19 06:11:42
 */
public class CustomTags extends OFDElement {
    public CustomTags(Element proxy) {
        super(proxy);
    }

    protected CustomTags() {
        super("CustomTags");
    }

    /**
     * 【可选】
     * 增加 自定义标引入口
     *
     * @param customTag  自定义标引入口
     * @return this
     */
    public CustomTags addCustomTag(CustomTag customTag) {
        if (customTag == null) {
            return this;
        }
        this.add(customTag);
        return this;
    }
    /**
     * 【可选】
     * 获取 自定义标引入口列表
     *
     * @return  自定义标引入口列表
     */
    public List<CustomTag> getCustomTags(){
        return this.getOFDElements("CustomTag", CustomTag::new);
    }
}
