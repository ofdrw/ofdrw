package org.ofdrw.core.text.text;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.graph.pathObj.FillColor;
import org.ofdrw.core.graph.pathObj.StrokeColor;
import org.ofdrw.core.pageDescription.CT_GraphicUnit;
import org.ofdrw.core.pageDescription.clips.ClipAble;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.text.CT_CGTransform;
import org.ofdrw.core.text.TextCode;

import java.util.List;

/**
 * 文字对象
 * <p>
 * 11.2 文字对象 图 59 表 45
 *
 * @author 权观宇
 * @since 2019-10-18 09:27:41
 */
public class CT_Text extends CT_GraphicUnit<CT_Text> implements ClipAble {
    public CT_Text(Element proxy) {
        super(proxy);
    }

    public CT_Text(String name) {
        super(name);
    }

    public CT_Text() {
        super("Text");
    }

    /**
     * 获取文字对象
     *
     * @param id 文字对象ID
     * @return 文字对象 TextObject
     */
    public static CT_Text textObject(ST_ID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        CT_Text res = new CT_Text("TextObject");
        res.setObjID(id);
        return res;
    }

    /**
     * 构造文字对象
     *
     * @param id 对象ID
     * @return 对象
     */
    public TextObject toObj(ST_ID id) {
        this.setOFDName("TextObject");
        this.setObjID(id);
        return new TextObject(this);
    }

    /**
     * 【必选 属性】
     * 设置 引用资源文件中定义的字形标识
     *
     * @param font 引用字形资源文件路径
     * @return this
     */
    public CT_Text setFont(ST_RefID font) {
        if (font == null) {
            throw new IllegalArgumentException("字形资源文件（Font）不能为空");
        }
        this.addAttribute("Font", font.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 设置 引用资源文件中定义的字形标识
     *
     * @param refId ID
     * @return this
     */
    public CT_Text setFont(long refId) {
        return setFont(new ST_RefID(refId));
    }

    /**
     * 【必选 属性】
     * 获取 引用资源文件路径
     *
     * @return 引用字形资源文件路径
     */
    public ST_RefID getFont() {
        return ST_RefID.getInstance(this.attributeValue("Font"));
    }

    /**
     * 【必选 属性】
     * 设置 字号，单位为毫米
     *
     * @param size 字号，单位为毫米
     * @return this
     */
    public CT_Text setSize(Double size) {
        if (size == null) {
            throw new IllegalArgumentException("字号（Size）不能为空");
        }
        this.addAttribute("Size", size.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 字号，单位为毫米
     *
     * @return 字号，单位为毫米
     */
    public Double getSize() {
        String str = this.attributeValue("Size");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("字号（Size）不能为空");
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选 属性】
     * 设置 是否勾边
     * <p>
     * 默认值为 false
     *
     * @param stroke true - 勾边；false - 不勾边
     * @return this
     */
    public CT_Text setStroke(Boolean stroke) {
        if (stroke == null) {
            this.removeAttr("Stroke");
            return this;
        }
        this.addAttribute("Stroke", stroke.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 是否勾边
     * <p>
     * 默认值为 false
     *
     * @return true - 勾边；false - 不勾边
     */
    public Boolean getStroke() {
        String str = this.attributeValue("Stroke");
        if (str == null || str.trim().length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选 属性】
     * 设置 是否填充
     * <p>
     * 默认值为 true
     *
     * @param fill true - 填充；false - 不填充
     * @return this
     */
    public CT_Text setFill(Boolean fill) {
        if (fill == null) {
            this.removeAttr("Fill");
            return this;
        }
        this.addAttribute("Fill", fill.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 是否填充
     * <p>
     * 默认值为 true
     *
     * @return true - 填充；false - 不填充
     */
    public Boolean getFill() {
        String str = this.attributeValue("Fill");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选 属性】
     * 设置 字形在水平方向的缩放比
     * <p>
     * 默认值为 1.0
     * <p>
     * 例如：当 HScale 值为 0.5 时表示实际显示的字宽为原来字宽的一半。
     *
     * @param hScale 字形在水平方向的缩放比
     * @return this
     */
    public CT_Text setHScale(Double hScale) {
        if (hScale == null) {
            this.removeAttr("HScale");
            return this;
        }
        this.addAttribute("HScale", hScale.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 字形在水平方向的缩放比
     * <p>
     * 默认值为 1.0
     * <p>
     * 例如：当 HScale 值为 0.5 时表示实际显示的字宽为原来字宽的一半。
     *
     * @return 字形在水平方向的缩放比
     */
    public Double getHScale() {
        String str = this.attributeValue("HScale");
        if (str == null || str.trim().length() == 0) {
            return 1.0d;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选 属性】
     * 指定 阅读方向
     * <p>
     * 指定了文字排列的方向，描述见 11.3 文字定位
     * <p>
     * 默认值为 0
     *
     * @param readDirection 阅读方向，可选值为{@link Direction}
     * @return this
     */
    public CT_Text setReadDirection(Direction readDirection) {
        if (readDirection == null) {
            this.removeAttr("ReadDirection");
            return this;
        }
        this.addAttribute("ReadDirection", readDirection.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 阅读方向
     * <p>
     * 指定了文字排列的方向，描述见 11.3 文字定位
     * <p>
     * 默认值为 0
     *
     * @return 阅读方向，可选值为{@link Direction}
     */
    public Direction getReadDirection() {
        return Direction.getInstance(this.attributeValue("ReadDirection"));
    }

    /**
     * 【可选 属性】
     * 指定 字符方向
     * <p>
     * 指定了文字放置的方向，描述见 11.3 文字定位
     * <p>
     * 默认值为 0
     *
     * @param charDirection 字符方向，可选值为{@link Direction}
     * @return this
     */
    public CT_Text setCharDirection(Direction charDirection) {
        if (charDirection == null) {
            this.removeAttr("CharDirection");
            return this;
        }
        this.addAttribute("CharDirection", charDirection.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 字符方向
     * <p>
     * 指定了文字放置的方向，描述见 11.3 文字定位
     * <p>
     * 默认值为 0
     *
     * @return 字符方向，可选值为{@link Direction}
     */
    public Direction getCharDirection() {
        return Direction.getInstance(this.attributeValue("CharDirection"));
    }

    /**
     * 【可选 属性】
     * 设置 文字对象的粗细值
     * <p>
     * 默认值为 400
     *
     * @param weight 文字对象的粗细值，可选值{@link Weight}
     * @return this
     */
    public CT_Text setWeight(Weight weight) {
        if (weight == null) {
            this.removeAttr("Weight");
            return this;
        }
        this.addAttribute("Weight", weight.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 文字对象的粗细值
     * <p>
     * 默认值为 400
     *
     * @return 文字对象的粗细值，可选值{@link Weight}
     */
    public Weight getWeight() {
        return Weight.getInstance(this.attributeValue("Weight"));
    }

    /**
     * 【可选 属性】
     * 设置 是否是斜体样式
     * <p>
     * 默认值为 false
     *
     * @param italic true - 斜体样式； false - 正常
     * @return this
     */
    public CT_Text setItalic(Boolean italic) {
        if (italic == null) {
            this.removeAttr("Italic");
            return this;
        }
        this.addAttribute("Italic", italic.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 是否是斜体样式
     * <p>
     * 默认值为 false
     *
     * @return true - 斜体样式； false - 正常
     */
    public Boolean getItalic() {
        String str = this.attributeValue("Italic");
        if (str == null || str.trim().length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }


    /**
     * 【可选】
     * 设置 填充颜色
     * <p>
     * 默认为黑色
     *
     * @param fillColor 填充颜色
     * @return this
     */
    public CT_Text setFillColor(CT_Color fillColor) {
        if (fillColor == null) {
            return this;
        }
        fillColor.setOFDName("FillColor");
        this.add(fillColor);
        return this;
    }

    /**
     * 【可选】
     * 获取 填充颜色
     * <p>
     * 默认为黑色
     *
     * @return 填充颜色，null表示黑色
     */
    public FillColor getFillColor() {
        Element e = this.getOFDElement("FillColor");
        return e == null ? null : new FillColor(e);
    }


    /**
     * 【可选】
     * 设置 勾边颜色
     * <p>
     * 默认为透明色
     *
     * @param strokeColor 勾边颜色
     * @return this
     */
    public CT_Text setStrokeColor(CT_Color strokeColor) {
        if (strokeColor == null) {
            return this;
        }
        strokeColor.setOFDName("StrokeColor");
        this.add(strokeColor);
        return this;
    }

    /**
     * 【可选】
     * 获取 勾边颜色
     * <p>
     * 默认为透明色
     *
     * @return 勾边颜色，null为透明色
     */
    public StrokeColor getStrokeColor() {
        Element e = this.getOFDElement("StrokeColor");
        return e == null ? null : new StrokeColor(e);
    }

    /**
     * 【可选】
     * 增加  指定字符编码到字符索引之间的变换关系
     * <p>
     * 描述见 11.4 字符变换
     *
     * @param cgTransform 字符编码到字符索引之间的变换关系
     * @return this
     */
    public CT_Text addCGTransform(CT_CGTransform cgTransform) {
        if (cgTransform == null) {
            return this;
        }
        this.add(cgTransform);
        return this;
    }

    /**
     * 【可选】
     * 获取  指定字符编码到字符索引之间的变换关系序列
     * <p>
     * 描述见 11.4 字符变换
     *
     * @return 字符编码到字符索引之间的变换关系序列
     */
    public List<CT_CGTransform> getCGTransforms() {
        return this.getOFDElements("CGTransform", CT_CGTransform::new);
    }

    /**
     * 【必选】
     * 增加 文字内容
     * <p>
     * 也就是一段字符编码串
     * <p>
     * 如果字符编码不在XML编码方式的字符范围之内，应采用“\”加四位
     * 十六进制数的格式转义；文字内容中出现的空格也需要转义
     * 若 TextCode 作为占位符使用时一律采用  ¤ （\u00A4）占位
     *
     * @param textCode 文字内容
     * @return this
     */
    public CT_Text addTextCode(TextCode textCode) {
        if (textCode == null) {
            return this;
        }
        this.add(textCode);
        return this;
    }

    /**
     * 【必选】
     * 获取 文字内容序列
     * <p>
     * 也就是一段字符编码串
     * <p>
     * 如果字符编码不在XML编码方式的字符范围之内，应采用“\”加四位
     * 十六进制数的格式转义；文字内容中出现的空格也需要转义
     * 若 TextCode 作为占位符使用时一律采用  ¤ （\u00A4）占位
     *
     * @return 文字内容序列
     */
    public List<TextCode> getTextCodes() {
        return this.getOFDElements("TextCode", TextCode::new);
    }

}
