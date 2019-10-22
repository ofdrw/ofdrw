package org.ofdrw.core.text;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.STBase;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.pageDescription.clips.ClipAble;

/**
 * 文字定位
 * <p>
 * 文字对象使用严格的文字定位信息进行定位
 * <p>
 * 11.3 文字定位 图 61 表 46
 *
 * @author 权观宇
 * @since 2019-10-21 09:28:35
 */
public class TextCode extends OFDElement implements ClipAble {
    public TextCode(Element proxy) {
        super(proxy);
    }

    public TextCode() {
        super("TextCode");
    }

    /**
     * 设置文字内容
     *
     * @param content 内容
     * @return this
     */
    public TextCode setContent(String content) {
        this.setText(content);
        return this;
    }

    /**
     * 获取文字内容
     * @return 文字内容
     */
    public String getContent(){
        return this.getText();
    }

    /**
     * 【可选 属性】
     * 设置 第一个文字的字形在对象坐标系下的 X 坐标
     * <p>
     * 当 X 不出现，则采用上一个 TextCode 的 X 值，文字对象中的一个
     * TextCode 的属性必选
     *
     * @param x 第一个文字的字形在对象坐标系下的 X 坐标
     * @return this
     */
    public TextCode setX(Double x) {
        if (x == null) {
            this.removeAttr("X");
            return this;
        }
        this.addAttribute("X", STBase.fmt(x));
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 第一个文字的字形在对象坐标系下的 X 坐标
     * <p>
     * 当 X 不出现，则采用上一个 TextCode 的 X 值，文字对象中的一个
     * TextCode 的属性必选
     *
     * @return 第一个文字的字形在对象坐标系下的 X 坐标；null表示采用上一个 TextCode 的 X 值
     */
    public Double getX() {
        String str = this.attributeValue("X");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选 属性】
     * 设置 第一个文字的字形原点在对象坐标系下的 Y 坐标
     * <p>
     * 当 Y 不出现，则采用上一个 TextCode 的 Y 值，文字对象中的一个
     * TextCode 的属性必选
     *
     * @param y 第一个文字的字形原点在对象坐标系下的 Y 坐标
     * @return this
     */
    public TextCode setY(Double y) {
        if (y == null) {
            this.removeAttr("Y");
            return this;
        }
        this.addAttribute("Y", STBase.fmt(y));
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 第一个文字的字形在对象坐标系下的 Y 坐标
     * <p>
     * 当 X 不出现，则采用上一个 TextCode 的 Y 值，文字对象中的一个
     * TextCode 的属性必选
     *
     * @return 第一个文字的字形在对象坐标系下的 Y 坐标；null表示采用上一个 TextCode 的 Y 值
     */
    public Double getY() {
        String str = this.attributeValue("Y");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选 属性】
     * 设置 文字之间在 X 方向上的偏移值
     * <p>
     * double 型数值队列，列表中的每个值代表一个文字与前一个
     * 文字之间在 X 方向的偏移值
     * <p>
     * DeltaX 不出现时，表示文字的绘制点在 X 方向不做偏移。
     *
     * @param deltaX 文字之间在 X 方向上的偏移值
     * @return this
     */
    public TextCode setDeltaX(ST_Array deltaX) {
        if (deltaX == null) {
            this.removeAttr("DeltaX");
            return this;
        }
        this.addAttribute("DeltaX", deltaX.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 文字之间在 X 方向上的偏移值
     * <p>
     * double 型数值队列，列表中的每个值代表一个文字与前一个
     * 文字之间在 X 方向的偏移值
     * <p>
     * DeltaX 不出现时，表示文字的绘制点在 X 方向不做偏移。
     *
     * @return 文字之间在 X 方向上的偏移值；null表示不偏移
     */
    public ST_Array getDeltaX() {
        return ST_Array.getInstance(this.attributeValue("DeltaX"));
    }


    /**
     * 【可选 属性】
     * 设置 文字之间在 Y 方向上的偏移值
     * <p>
     * double 型数值队列，列表中的每个值代表一个文字与前一个
     * 文字之间在 Y 方向的偏移值
     * <p>
     * DeltaY 不出现时，表示文字的绘制点在 Y 方向不做偏移。
     *
     * @param deltaY 文字之间在 Y 方向上的偏移值；null表示不偏移
     * @return this
     */
    public TextCode setDeltaY(ST_Array deltaY) {
        if (deltaY == null) {
            this.removeAttr("DeltaY");
            return this;
        }
        this.addAttribute("DeltaY", deltaY.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 文字之间在 Y 方向上的偏移值
     * <p>
     * double 型数值队列，列表中的每个值代表一个文字与前一个
     * 文字之间在 Y 方向的偏移值
     * <p>
     * DeltaY 不出现时，表示文字的绘制点在 Y 方向不做偏移。
     *
     * @return 文字之间在 Y 方向上的偏移值；null表示不偏移
     */
    public ST_Array getDeltaY() {
        String str = this.attributeValue("DeltaY");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        return ST_Array.getInstance(str);
    }
}
