package org.ofdrw.core.pageDescription.color.pattern;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
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
            this.removeAttr("Thumbnail");
            return this;
        }
        this.addAttribute("Thumbnail", thumbnail.toString());
        return this;
    }

    /**
     * 【可选】
     * 增加 页块
     * <p>
     * 一个页块中可以嵌套其他页块，可含有0到多个页块
     *
     * @param pageBlock 页块实例
     * @return this
     */
    @Override
    public CellContent addPageBlock(PageBlockType pageBlock) {
        this.add(pageBlock);
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
