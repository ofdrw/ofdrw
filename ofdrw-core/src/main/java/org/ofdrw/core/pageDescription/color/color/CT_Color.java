package org.ofdrw.core.pageDescription.color.color;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_RefID;

import java.util.List;

/**
 * 基本颜色
 * <p>
 * 本标准中定义的颜色是一个广义的概念，包括基本颜色、底纹和渐变
 * <p>
 * 基本颜色支持两种指定方式：一种是通过设定颜色个通道值指定颜色空间的某个颜色，
 * 另一种是通过索引值取得颜色空间中的一个预定义颜色。
 * <p>
 * 由于不同颜色空间下，颜色通道的含义、数目各不相同，因此对颜色空间的类型、颜色值的
 * 描述格式等作出了详细的说明，见表 27。BitsPerComponent（简称 BPC）由效时，
 * 颜色通道值的取值下限是 0，上限由 BitsPerComponent 决定，取值区间 [0, 2^BPC - 1]
 * 内的整数，采用 10 进制或 16 进制的形式表示，采用 16 进制表示时，应以“#”加以标识。
 * 当颜色通道的值超出了相应区间，则按照默认颜色来处理。
 * <p>
 * 8.3.2 基本颜色 图 25 表 26
 *
 * @author 权观宇
 * @since 2019-10-11 09:01:56
 */
public class CT_Color extends OFDElement {

    public CT_Color(Element proxy) {
        super(proxy);
    }

    public CT_Color() {
        super("Color");
    }

    /**
     * @param color 颜色族中的颜色
     */
    public CT_Color(ColorClusterType color) {
        this();
        this.setColor(color);
    }


    protected CT_Color(String name) {
        super(name);
    }

    /**
     * RGB颜色值
     * <p>
     * 其中颜色空间（CT_ColorSpace）的通道使用位数（BitsPerComponent）为 8
     * <p>
     * 采用10进制表示方式
     *
     * @param r 红色 0~255
     * @param g 绿色 0~255
     * @param b 蓝色 0~255
     * @return RGB 颜色
     */
    public static CT_Color rgb(int r, int g, int b) {
        return new CT_Color()
                .setValue(new ST_Array(r, g, b));
    }

    public static CT_Color rgb(int[] rgb) {
        return rgb(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * 【可选 属性】
     * 设置 颜色值
     * <p>
     * 指定了当前颜色空间下各通道的取值。Value 的取值应
     * 符合"通道 1 通道 2 通道 3 ..."格式。此属性不出现时，
     * 应采用 Index 属性从颜色空间的调色板中的取值。二者都不
     * 出现时，改颜色各通道的值全部为 0
     * <p>
     * 颜色表示：
     * <p>
     * Gray - 通过一个通道来表明灰度值；例如 "#FF 255"
     * <p>
     * RGB - 包含3个通道，一次是红、绿、蓝；例如 "#11 #22 #33"、"17 34 51"
     * <p>
     * CMYK - 包含4个通道，依次是青、黄、品红、黑；例如 "#11 #22 #33 # 44"、"17 34 51 68"
     *
     * @param value 颜色值
     * @return this
     */
    public CT_Color setValue(ST_Array value) {
        if (value == null) {
            this.removeAttr("Value");
            return this;
        }
        this.addAttribute("Value", value.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 颜色值
     * <p>
     * 指定了当前颜色空间下各通道的取值。Value 的取值应
     * 符合"通道 1 通道 2 通道 3 ..."格式。此属性不出现时，
     * 应采用 Index 属性从颜色空间的调色板中的取值。二者都不
     * 出现时，改颜色各通道的值全部为 0
     * <p>
     * 颜色表示：
     * <p>
     * Gray - 通过一个通道来表明灰度值；例如 "#FF 255"
     * <p>
     * RGB - 包含3个通道，一次是红、绿、蓝；例如 "#11 #22 #33"、"17 34 51"
     * <p>
     * CMYK - 包含4个通道，依次是青、黄、品红、黑；例如 "#11 #22 #33 # 44"、"17 34 51 68"
     *
     * @return 颜色值
     */
    public ST_Array getValue() {
        return ST_Array.getInstance(this.attributeValue("Value"));
    }

    /**
     * 【可选 属性】
     * 设置 调色板中颜色的编号，非负整数
     * <p>
     * 将从当前颜色空间的调色板中取出相应索引的预定义颜色用来描绘。
     * 索引从 0 开始
     *
     * @param index 调色板中颜色的编号
     * @return this
     */
    public CT_Color setIndex(Integer index) {
        if (index == null) {
            this.removeAttr("Index");
            return this;
        }

        if (index < 0) {
            throw new NumberFormatException("调色板中颜色的编号，必须为非负整数");
        }
        this.addAttribute("Index", index.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 调色板中颜色的编号，非负整数
     * <p>
     * 将从当前颜色空间的调色板中取出相应索引的预定义颜色用来描绘。
     * 索引从 0 开始
     *
     * @return 调色板中颜色的编号，null表示不存在
     */
    public Integer getIndex() {
        String str = this.attributeValue("Index");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        Integer index = Integer.parseInt(str);
        if (index < 0) {
            throw new NumberFormatException("调色板中颜色的编号，必须为非负整数");
        }
        return index;
    }

    /**
     * 【可选 属性】
     * 设置 引用资源文件中颜色空间的标识
     * <p>
     * 默认值为文档设定的颜色空间
     *
     * @param colorSpace 颜色空间的标识
     * @return this
     */
    public CT_Color setColorSpace(ST_RefID colorSpace) {
        if (colorSpace == null) {
            this.removeAttr("ColorSpace");
            return this;
        }
        this.addAttribute("ColorSpace", colorSpace.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 引用资源文件中颜色空间的标识
     * <p>
     * 默认值为文档设定的颜色空间
     *
     * @return 颜色空间的标识，为null是请从文档中获取设定的颜色空间，参照表 6 DefaultCS
     */
    public ST_RefID getColorSpace() {
        return ST_RefID.getInstance(this.attributeValue("ColorSpace"));
    }

    /**
     * 【可选 属性】
     * 设置 颜色透明度
     * <p>
     * 范围在 0~255 之间取值。
     * <p>
     * 默认为 255，完全不透明。
     *
     * @param alpha 颜色透明度
     * @return this
     */
    public CT_Color setAlpha(Integer alpha) {
        if (alpha == null) {
            this.removeAttr("Alpha");
            return this;
        } else if (alpha < 0) {
            alpha = 0;
        } else if (alpha > 255) {
            alpha = 255;
        }

        this.addAttribute("Alpha", alpha.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 颜色透明度
     * <p>
     * 范围在 0~255 之间取值。
     * <p>
     * 默认为 255，完全不透明。
     *
     * @return 颜色透明度 0~255
     */
    public Integer getAlpha() {
        String str = this.attributeValue("Alpha");
        if (str == null || str.trim().length() == 0) {
            return 255;
        }
        Integer value = Integer.parseInt(str);
        if (value < 0 || value > 255) {
            throw new NumberFormatException("颜色透明度范围在 0~255 之间取值：" + value);
        }
        return value;
    }

    /**
     * 【可选】
     * 设置 颜色
     *
     * @param color 颜色族
     * @return this
     */
    public CT_Color setColor(ColorClusterType color) {
        if (color == null) {
            return this;
        }
        this.removeOFDElemByNames("Pattern", "AxialShd", "RadialShd", "GouraudShd", "LaGouraudShd");
        this.add(color);
        return this;
    }

    /**
     * 【可选】
     * 获取 颜色
     *
     * @return 颜色族, null表示不存在
     */
    public ColorClusterType getColor() {
        List<Element> elements = this.elements();
        if (elements.size() == 0) {
            return null;
        }
        return ColorClusterType.getInstance(elements.get(0));
    }

    /**
     * 【可选】
     * 获取 指定类型的颜色
     *
     * @param <T> 颜色类型
     * @return 指定类型色对象， null表示不存指定类型颜色
     */
    @SuppressWarnings("unchecked")
    public <T extends ColorClusterType> T getColorByType() {
        List<Element> elements = this.elements();
        if (elements.size() == 0) {
            return null;
        }
        try {
            return (T) ColorClusterType.getInstance(elements.get(0));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }
    }
}
