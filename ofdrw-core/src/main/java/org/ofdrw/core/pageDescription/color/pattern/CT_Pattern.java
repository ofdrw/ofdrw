package org.ofdrw.core.pageDescription.color.pattern;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.STBase;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.pageDescription.color.color.ColorClusterType;

/**
 * 底纹
 * <p>
 * 底纹是复杂颜色的一种，用于图形和文字的填充以及沟边处理。
 * <p>
 * 8.3.3 底纹 图 26  表 28
 *
 * @author 权观宇
 * @since 2019-10-12 08:14:58
 */
public class CT_Pattern extends OFDElement implements ColorClusterType {

    public CT_Pattern(Element proxy) {
        super(proxy);
    }

    public CT_Pattern() {
        super("Pattern");
    }

    /**
     * 【必选 属性】
     * 设置 底纹单元宽度
     *
     * @param width 底纹单元宽度
     * @return this
     */
    public CT_Pattern setWidth(Double width) {
        if (width == null) {
            throw new IllegalArgumentException("底纹单元宽度不能为空");
        }
        this.addAttribute("Width", width.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 底纹单元宽度
     *
     * @return 底纹单元宽度
     */
    public Double getWidth() {
        String str = this.attributeValue("Width");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("底纹格式非法，底纹单元宽度不能为空");
        }
        return Double.parseDouble(str);
    }

    /**
     * 【必选 属性】
     * 获取 底纹单元高度
     *
     * @param height 底纹单元高度
     * @return this
     */
    public CT_Pattern setHeight(Double height) {
        if (height == null) {
            throw new IllegalArgumentException("底纹单元高度不能为空");
        }
        this.addAttribute("Height", STBase.fmt(height));
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 底纹单元高度
     *
     * @return 底纹单元高度
     */
    public Double getHeight() {
        String str = this.attributeValue("Height");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("底纹格式非法，底纹单元高度不能为空");
        }
        return Double.parseDouble(str);
    }


    /**
     * 【可选 属性】
     * 设置 X 方向底纹单元间距
     * <p>
     * 默认值为底纹单元的宽度。
     * <p>
     * 若设定值小于底纹单元的宽度时，应按默认值处理
     *
     * @param xStep X 方向底纹单元间距
     * @return this
     */
    public CT_Pattern setXStep(Double xStep) {
        if (xStep == null) {
            this.removeAttr("XStep");
            return this;
        }
        Double width = getWidth();
        if (xStep < width) {
            xStep = width;
        }
        this.addAttribute("XStep", STBase.fmt(xStep));
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 X 方向底纹单元间距
     * <p>
     * 默认值为底纹单元的宽度。
     * <p>
     * 若设定值小于底纹单元的宽度时，应按默认值处理
     *
     * @return X 方向底纹单元间距
     */
    public Double getXStep() {
        String str = this.attributeValue("XStep");
        if (str == null || str.trim().length() == 0) {
            return getWidth();
        }
        Double var = Double.parseDouble(str);
        Double width = getWidth();
        return (var < width) ? width : var;
    }

    /**
     * 【可选 属性】
     * 设置 Y 方向底纹单元间距
     * <p>
     * 默认值为底纹单元的高度。
     * <p>
     * 若设定值小于底纹单元的高度时，应按默认值处理
     *
     * @param yStep Y 方向底纹单元间距
     * @return this
     */
    public CT_Pattern setYStep(Double yStep) {
        if (yStep == null) {
            this.removeAttr("YStep");
            return this;
        }
        Double height = getHeight();
        if (yStep < height) {
            yStep = height;
        }
        this.addAttribute("YStep", STBase.fmt(yStep));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 Y 方向底纹单元间距
     * <p>
     * 默认值为底纹单元的高度。
     * <p>
     * 若设定值小于底纹单元的高度时，应按默认值处理
     *
     * @return Y 方向底纹单元间距
     */
    public Double getYStep() {
        String str = this.attributeValue("YStep");
        if (str == null || str.trim().length() == 0) {
            return getHeight();
        }
        Double var = Double.parseDouble(str);
        Double height = getHeight();
        return (var < height) ? height : var;
    }

    /**
     * 【可选 属性】
     * 设置 底纹单元的翻转方式
     * <p>
     * 默认值为 Normal
     * <p>
     * 参考{@link ReflectMethod}
     *
     * @param reflectMethod 底纹单元的翻转方式
     * @return this
     */
    public CT_Pattern setReflectMethod(ReflectMethod reflectMethod) {
        if (reflectMethod == null) {
            this.removeAttr("ReflectMethod");
            return this;
        }
        this.addAttribute("ReflectMethod", reflectMethod.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 底纹单元的翻转方式
     * <p>
     * 默认值为 Normal
     * <p>
     * 参考{@link ReflectMethod}
     *
     * @return 底纹单元的翻转方式
     */
    public ReflectMethod getReflectMethod() {
        return ReflectMethod.getInstance(this.attributeValue("ReflectMethod"));
    }

    /**
     * 【可选 属性】
     * 设置 底纹单元起始位置
     * <p>
     * 默认值为 Object：相对于对象坐标原点
     *
     * @param relativeTo 底纹单元起始位置
     * @return this
     */
    public CT_Pattern setRelativeTo(RelativeTo relativeTo) {
        if (relativeTo == null) {
            this.removeAttr("RelativeTo");
            return this;
        }
        this.addAttribute("RelativeTo", relativeTo.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 底纹单元起始位置
     * <p>
     * 默认值为 Object：相对于对象坐标原点
     *
     * @return 底纹单元起始位置
     */
    public RelativeTo getRelativeTo() {
        return RelativeTo.getInstance(this.attributeValue("RelativeTo"));
    }

    /**
     * 【可选 属性】
     * 设置 底纹单元的变换矩阵
     * <p>
     * 用于某些需要对底纹单元进行平移旋转变换的场合，
     * 默认为单位矩阵；底纹呈现时先做 XStep、YStep 排列，
     * 然后一起做 CTM 处理
     *
     * @param ctm 底纹单元的变换矩阵
     * @return this
     */
    public CT_Pattern setCTM(ST_Array ctm) {
        if (ctm == null) {
            this.removeAttr("CTM");
            return this;
        }

        this.addAttribute("CTM", ctm.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 底纹单元的变换矩阵
     * <p>
     * 用于某些需要对底纹单元进行平移旋转变换的场合，
     * 默认为单位矩阵；底纹呈现时先做 XStep、YStep 排列，
     * 然后一起做 CTM 处理
     *
     * @return 底纹单元的变换矩阵
     */
    public ST_Array getCTM() {
        return ST_Array.getInstance(this.attributeValue("CTM"));
    }


    /**
     * 【必选】
     * 设置 底纹单元
     * <p>
     * 用底纹填充目标区域时，所使用的单元对象
     *
     * @param cellContent 底纹单元
     * @return this
     */
    public CT_Pattern setCellContent(CellContent cellContent) {
        this.set(cellContent);
        return this;
    }

    /**
     * 【必选】
     * 获取 底纹单元
     * <p>
     * 用底纹填充目标区域时，所使用的单元对象
     *
     * @return 底纹单元
     */
    public CellContent getCellContent() {
        Element e = this.getOFDElement("CellContent");
        if (e == null) {
            throw new IllegalArgumentException("底纹格式异常，未找到任何可用底纹单元（CellContent）");
        }
        return new CellContent(e);
    }
}
