package org.ofdrw.converter;

import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;
import org.ofdrw.reader.tools.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 图片转换类
 *
 * @author qaqtutu
 * @since 2021-03-13 10:00:01
 */
public class ImageMaker extends AWTMaker {


    /**
     * 创建图片转换对象实例
     * <p>
     * OFD内部使用毫米作为基本单位
     *
     * @param reader OFD解析器
     * @param ppm    每毫米像素数量(Pixels per millimeter)
     */

    public ImageMaker(OFDReader reader, int ppm) {
        super(reader, ppm);
    }

    /**
     * 渲染OFD页面为图片
     *
     * @param pageIndex 页码，从0起
     * @return 渲染完成的图片
     */
    public BufferedImage makePage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new GeneralConvertException(String.format("%s 不是有效索引", pageIndex));
        }
        PageInfo pageInfo = pages.get(pageIndex);
        ST_Box pageBox = pageInfo.getSize();
        double pageWidthPixel = ppm * pageBox.getWidth();
        double pageHeightPixel = ppm * pageBox.getHeight();

        BufferedImage image = createImage((int) pageWidthPixel, (int) pageHeightPixel);
        Graphics2D graphics = (Graphics2D) image.getGraphics();

        writePage(graphics, pageInfo, null);

        return image;
    }

    /**
     * 创建图片
     *
     * @param pageWidthPixel  图形宽度
     * @param pageHeightPixel 图像高度
     */
    private BufferedImage createImage(int pageWidthPixel, int pageHeightPixel) {
        return ImageUtils.createImage(pageWidthPixel, pageHeightPixel, isStamp);
    }
}
