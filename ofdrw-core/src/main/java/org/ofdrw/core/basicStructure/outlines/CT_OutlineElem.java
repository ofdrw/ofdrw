package org.ofdrw.core.basicStructure.outlines;

import org.dom4j.Element;
import org.ofdrw.core.action.Actions;
import org.ofdrw.core.OFDElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 大纲节点
 * <p>
 * 图 19 大纲节点结构
 *
 * @author 权观宇
 * @since 2019-10-05 11:13:47
 */
public class CT_OutlineElem extends OFDElement {
    public CT_OutlineElem(Element proxy) {
        super(proxy);
    }

    public CT_OutlineElem() {
        super("OutlineElem");
    }

    public CT_OutlineElem(String title) {
        this();
        this.setTitle(title);
    }

    /**
     * 【必选 属性】
     * 设置 大纲节点标题
     *
     * @param title 大纲节点标题
     * @return this
     */
    public CT_OutlineElem setTitle(String title) {
        this.addAttribute("Title", title);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 大纲节点标题
     *
     * @return 大纲节点标题
     */
    public String getTitle() {
        return this.attributeValue("Title");
    }

    /**
     * 【可选 属性】
     * 设置 该节点下所有叶节点的数目参考值
     * <p>
     * 应该根据该节点下实际出现的子节点数为准
     * <p>
     * 默认值为 0
     *
     * @param count 该节点下所有叶节点的数目参考值
     * @return this
     */
    public CT_OutlineElem setCount(int count) {
        this.addAttribute("Count", Integer.toString(count));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 该节点下所有叶节点的数目参考值
     * <p>
     * 应该根据该节点下实际出现的子节点数为准
     * <p>
     * 默认值为 0
     *
     * @return 该节点下所有叶节点的数目参考值
     */
    public Integer getCount() {
        String str = this.attributeValue("Count");
        if (str == null || str.trim().length() == 0) {
            return 0;
        }
        return Integer.parseInt(str);
    }

    /**
     * 【可选 属性】
     * 在有子节点存在时有效，如果为 true，
     * 表示该大纲在初始状态下展开子节点；
     * 如果为 false，则表示不展开
     * <p>
     * 默认值为 true
     *
     * @param expanded true - 展开； false - 不展开
     * @return this
     */
    public CT_OutlineElem setExpanded(boolean expanded) {
        this.addAttribute("Expanded", Boolean.toString(expanded));
        return this;
    }

    /**
     * 【可选 属性】
     * 在有子节点存在时有效，如果为 true，
     * 表示该大纲在初始状态下展开子节点；
     * 如果为 false，则表示不展开
     * <p>
     * 默认值为 true
     *
     * @return true - 展开； false - 不展开
     */
    public Boolean getExpanded() {
        String str = this.attributeValue("Expanded");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选】
     * 设置 当此大纲节点被激活时执行的动作序列
     *
     * @param actions 动作序列
     * @return this
     */
    public CT_OutlineElem setActions(Actions actions) {
        this.set(actions);
        return this;
    }

    /**
     * 【可选】
     * 获取 当此大纲节点被激活时执行的动作序列
     *
     * @return 动作序列，可能为null
     */
    public Actions getActions() {
        Element e = this.getOFDElement("Actions");
        return e == null ? null : new Actions(e);
    }

    /**
     * 【可选】
     * 增加 大纲子节点
     * <p>
     * 该节点的子大纲节点。层层嵌套，形成树状结构
     *
     * @param outlineElem 大纲子节点
     * @return this
     */
    public CT_OutlineElem addOutlineElem(CT_OutlineElem outlineElem) {
        this.add(outlineElem);
        return this;
    }

    /**
     * 【可选】
     * 获取 该大纲下所有子节点
     * <p>
     * 该节点的子大纲节点。层层嵌套，形成树状结构
     *
     * @return 该大纲下所有子节点
     */
    public List<CT_OutlineElem> getOutlineElems() {
        List<Element> elements = this.getOFDElements("OutlineElem");
        if(elements == null || elements.size() == 0){
            return Collections.emptyList();
        }
        List<CT_OutlineElem> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(new CT_OutlineElem(item)));
        return res;
    }
}
