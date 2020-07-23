package org.ofdrw.core.annotation.pageannot;

import org.dom4j.Element;
import org.ofdrw.core.Const;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.OFDSimpleTypeElement;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicType.ST_ID;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 注释
 * <p>
 * 15.2 图 81 表 61
 *
 * @author 权观宇
 * @since 2019-11-16 02:22:34
 */
public class Annot extends OFDElement {

    public Annot(Element proxy) {
        super(proxy);
    }

    public Annot() {
        super("Annot");
    }

    /**
     * 【必选 属性】
     * 设置 注释的标识
     *
     * @param id 注释的标识
     * @return this
     */
    public Annot setID(ST_ID id) {
        if (id == null) {
            throw new IllegalArgumentException("注解对象（ID）不能为空");
        }
        this.setObjID(id);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 注释的标识
     *
     * @return 注释的标识
     */
    public ST_ID getID() {
        return this.getObjID();
    }

    /**
     * 【必选 属性】
     * 设置 注释类型
     * <p>
     * 具体取值见{@link AnnotType}
     *
     * @param type 注释类型
     * @return this
     */
    public Annot setType(AnnotType type) {
        if (type == null) {
            throw new IllegalArgumentException("注释类型（Type）为空");
        }
        this.addAttribute("Type", type.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 注释类型
     * <p>
     * 具体取值见{@link AnnotType}
     *
     * @return 注释类型
     */
    public AnnotType getType() {
        return AnnotType.getInstance(this.attributeValue("Type"));
    }

    /**
     * 【必选 属性】
     * 设置 注释创建者
     *
     * @param creator 注释创建者
     * @return this
     */
    public Annot setCreator(String creator) {
        if (creator == null) {
            throw new IllegalArgumentException("注释创建者（Creator）为空");
        }
        this.addAttribute("Creator", creator);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 注释创建者
     *
     * @return 注释创建者
     */
    public String getCreator() {
        String str = this.attributeValue("Creator");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("注释创建者（Creator）为空");
        }
        return str;
    }

    /**
     * 【必选 属性】
     * 设置 最近一次修改的时间
     *
     * @param lastModDate 最近一次修改的时间
     * @return this
     */
    public Annot setLastModDate(LocalDate lastModDate) {
        if (lastModDate == null) {
            throw new IllegalArgumentException("最近一次修改的时间（LastModDate）为空");
        }
        this.addAttribute("LastModDate", Const.DATE_FORMATTER.format(lastModDate));
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 最近一次修改的时间
     *
     * @return 最近一次修改的时间
     */
    public LocalDate getLastModDate() {
        String date = this.attributeValue("LastModDate");
        if (date == null || date.trim().length() == 0) {
            throw new IllegalArgumentException("最近一次修改的时间（LastModDate）为空");
        }
        return LocalDate.parse(date, Const.DATE_FORMATTER);
    }

    /**
     * 【可选 属性】
     * 设置 注释子类型
     *
     * @param subtype 注释子类型
     * @return this
     */
    public Annot setSubtype(String subtype) {
        if (subtype == null) {
            this.removeAttr("Subtype");
            return this;
        }
        this.addAttribute("Subtype", subtype);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 注释子类型
     *
     * @return 注释子类型
     */
    public String getSubtype() {
        return this.attributeValue("Subtype");
    }

    /**
     * 【可选 属性】
     * 设置 表示该注释对象是否显示
     * <p>
     * 默认值为 true
     *
     * @param visible 表示该注释对象是否显示，默认值为 true
     * @return this
     */
    public Annot setVisible(boolean visible) {
        this.addAttribute("Visible", Boolean.toString(visible));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 表示该注释对象是否显示
     * <p>
     * 默认值为 true
     *
     * @return 表示该注释对象是否显示，默认值为 true
     */
    public boolean getVisible() {
        String str = this.attributeValue("Visible");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选 属性】
     * 设置 对象的Remark 信息是否随页面一起打印
     * <p>
     * 默认值为 true
     *
     * @param print 对象的Remark 信息是否随页面一起打印
     * @return this
     */
    public Annot setPrint(Boolean print) {
        if (print == null) {
            this.removeAttr("Print");
            return this;
        }
        this.addAttribute("Print", Boolean.toString(print));
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 对象的Remark 信息是否随页面一起打印
     * <p>
     * 默认值为 true
     *
     * @return 对象的Remark 信息是否随页面一起打印
     */
    public Boolean getPrint() {
        String str = this.attributeValue("Print");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选 属性】
     * 设置 对象的 Remark 信息是否不随页面缩放而同步缩放
     * <p>
     * 默认值为 false
     *
     * @param noZoom 对象的 Remark 信息是否不随页面缩放而同步缩放
     * @return this
     */
    public Annot setNoZoom(Boolean noZoom) {
        if (noZoom == null) {
            this.removeAttr("NoZoom");
            return this;
        }
        this.addAttribute("NoZoom", noZoom.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 对象的 Remark 信息是否不随页面缩放而同步缩放
     * <p>
     * 默认值为 false
     *
     * @return 对象的 Remark 信息是否不随页面缩放而同步缩放
     */
    public Boolean getNoZoom() {
        String str = this.attributeValue("NoZoom");
        if (str == null || str.trim().length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选 属性】
     * 设置 对象的 Remark 信息是否不随页面旋转而旋转
     * <p>
     * 默认值为 false
     *
     * @param noRotate 对象的 Remark 信息是否不随页面旋转而旋转
     * @return this
     */
    public Annot setNoRotate(Boolean noRotate) {
        if (noRotate == null) {
            this.removeAttr("NoRotate");
            return this;
        }
        this.addAttribute("NoRotate", noRotate.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 对象的 Remark 信息是否不随页面旋转而旋转
     * <p>
     * 默认值为 false
     *
     * @return 对象的 Remark 信息是否不随页面旋转而旋转
     */
    public Boolean getNoRotate() {
        String str = this.attributeValue("NoRotate");
        if (str == null || str.trim().length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选 属性】
     * 设置 对象的 Remark 信息是否不能被用户更改
     * <p>
     * 默认值为 true
     *
     * @param readOnly 对象的 Remark 信息是否不能被用户更改
     * @return this
     */
    public Annot setReadOnly(Boolean readOnly) {
        if (readOnly == null) {
            this.removeAttr("ReadOnly");
            return this;
        }
        this.addAttribute("ReadOnly", readOnly.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 对象的 Remark 信息是否不能被用户更改
     * <p>
     * 默认值为 true
     *
     * @return 对象的 Remark 信息是否不能被用户更改
     */
    public Boolean getReadOnly() {
        String str = this.attributeValue("ReadOnly");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }


    /**
     * 【可选】
     * 设置 注释说明内容
     *
     * @param remark 注释说明内容
     * @return this
     */
    public Annot setRemark(String remark) {
        if (remark == null) {
            this.removeOFDElemByNames("Remark");
            return this;
        }
        this.addOFDEntity("Remark", remark);
        return this;
    }

    /**
     * 【可选】
     * 获取 注释说明内容
     *
     * @return 注释说明内容
     */
    public String getRemark() {
        Element e = this.getOFDElement("Remark");
        return e == null ? null : e.getText();
    }

    /**
     * 【可选】
     * 增加 注释参数
     *
     * @param name      键名
     * @param parameter 值
     * @return this
     */
    public Annot addParameter(String name, String parameter) {
        if (name == null || name.trim().length() == 0) {
            return this;
        }

        Element parameters = this.getOFDElement("Parameters");
        if (parameters == null) {
            parameters = OFDElement.getInstance("Parameters");
            this.add(parameters);
        }

        Iterator<Element> iterator = parameters.elementIterator();
        boolean foundKey = false;
        while (iterator.hasNext()) {
            Element element = iterator.next();
            if (name.equals(element.attribute("Name").getValue())) {
                element.setText(parameter);
                foundKey = true;
                break;
            }
        }
        if (!foundKey) {
            OFDElement parameterEl = new OFDSimpleTypeElement("Parameter", parameter);
            parameterEl.addAttribute("Name", name);
            parameters.add(parameterEl);
        }
        return this;
    }

    /**
     * 【可选】
     * 获取 一组注释参数
     *
     * @return 注解参数映射表
     */
    public Map<String, String> getParameters() {
        Element e = this.getOFDElement("Parameters");
        if (e == null) {
            return Collections.emptyMap();
        }
        OFDElement parameters = new OFDElement(e);

        HashMap<String, String> res = new HashMap<>(5);
        parameters.getOFDElements("Parameter", OFDElement::new).forEach(p -> {
            String name = p.attributeValue("Name");
            String value = p.getTextTrim();
            res.put(name, value);
        });
        return res;
    }

    /**
     * 【必选】
     * 设置 注释的静态显示效果
     * <p>
     * 使用页面块定义来描述
     *
     * @param appearance 注释的静态显示效果
     * @return this
     */
    public Annot setAppearance(Appearance appearance) {
        if (appearance == null) {
            throw new IllegalArgumentException("注释的静态显示效果（Appearance）为空");
        }
        this.set(appearance);
        return this;
    }

    /**
     * 【必选】
     * 获取 注释的静态显示效果
     * <p>
     * 使用页面块定义来描述
     *
     * @return  注释的静态显示效果
     */
    public Appearance getAppearance() {
        return this.getOFDElement("Appearance", Appearance::new);
    }
}
