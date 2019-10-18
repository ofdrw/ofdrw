package org.ofdrw.core.pageDescription.clips;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_RefID;

import java.util.List;

/**
 * 裁剪区域
 * <p>
 * 用一个图形或文字对象来描述裁剪区的一个组成部分，
 * 最终裁剪区是这些区域的并集。
 * <p>
 * 8.4 裁剪区 表 33
 */
public class Area extends OFDElement {

    public Area(Element proxy) {
        super(proxy);
    }

    public Area() {
        super("Area");
    }

    /**
     * 【可选 属性】
     * 设置 引用资源文件中的绘制参数的标识
     * <p>
     * 线宽、结合点和端点样式等绘制特性对裁剪效果会产生影响，
     * 有关绘制参数的描述见 8.2
     *
     * @param drawParam 引用资源文件中的绘制参数的标识
     * @return this
     */
    public Area setDrawParam(ST_RefID drawParam) {
        this.addAttribute("DrawParam", drawParam.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 引用资源文件中的绘制参数的标识
     * <p>
     * 线宽、结合点和端点样式等绘制特性对裁剪效果会产生影响，
     * 有关绘制参数的描述见 8.2
     *
     * @return 引用资源文件中的绘制参数的标识
     */
    public ST_RefID getDrawParam() {
        return ST_RefID.getInstance(this.attributeValue("DrawParam"));
    }

    /**
     * 【可选 属性】
     * 设置 变换矩阵
     * <p>
     * 针对对象坐标系，对Area下包含的 Path 和 Text 进行进一步的变换
     *
     * @param ctm 变换矩阵
     * @return this
     */
    public Area setCTM(ST_Array ctm) {
        this.addAttribute("CTM", ctm.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 变换矩阵
     * <p>
     * 针对对象坐标系，对Area下包含的 Path 和 Text 进行进一步的变换
     *
     * @return 变换矩阵
     */
    public ST_Array getCTM() {
        return ST_Array.getInstance(this.attributeValue("CTM"));
    }


    /**
     * 【必选】
     * 设置 裁剪对象
     * <p>
     * 裁剪对象可以是 CT_Text、CT_Path
     *
     * @param clipObj 裁剪对象
     * @return this
     */
    public Area setClipObj(ClipAble clipObj) {
        this.add(clipObj);
        return this;
    }

    /**
     * 【必选】
     * 获取 裁剪对象
     * <p>
     * 裁剪对象可以是 CT_Text、CT_Path
     *
     * @return 裁剪对象
     */
    public ClipAble getClipObj() {
        List<Element> elements = this.elements();
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        Element e = elements.get(0);
        return e == null ? null : ClipAble.getInstance(e);
    }
}
