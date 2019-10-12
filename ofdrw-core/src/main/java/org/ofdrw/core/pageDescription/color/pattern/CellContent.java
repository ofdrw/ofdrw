package org.ofdrw.core.pageDescription.color.pattern;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_PageBlock;
import org.ofdrw.core.basicType.ST_RefID;

/**
 * 底纹单元
 * <p>
 * 用底纹填充目标区域时，所使用的单元对象
 * <p>
 * CellContent 作为底纹对象的绘制单元，使用一种和外界没有
 * 任何关联的独立的坐标空间：坐上角（0,0）为原点，X 轴向右增长，
 * Y 轴向下增长，单位为毫米。
 *
 * @author 权观宇
 * @since 2019-10-12 09:07:00
 */
public class CellContent extends CT_PageBlock {
    // TODO 2019-10-12 21:36:51 case test

    public CellContent(Element proxy) {
        super(proxy);
    }

    public CellContent() {
        super("CellContent");
    }

    /**
     * 【可选 属性】
     * 设置 引用资源文件中缩略图图像的标识符
     *
     * @param thumbnail 引用资源文件中缩略图图像的标识符
     * @return this
     */
    public CellContent setThumbnail(ST_RefID thumbnail) {
        if (thumbnail == null) {
            return this;
        }
        this.addAttribute("Thumbnail", thumbnail.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 引用资源文件中缩略图图像的标识符
     *
     * @return 引用资源文件中缩略图图像的标识符
     */
    public ST_RefID getThumbnail() {
        return ST_RefID.getInstance(this.attributeValue("Thumbnail"));
    }
}
