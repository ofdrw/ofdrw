package org.ofdrw.converter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;
import org.ofdrw.reader.tools.ImageUtils;

/**
 * 图片转换类
 *
 * @author qaqtutu
 * @author iandjava
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
	@Deprecated
    public ImageMaker(OFDReader reader, int ppm) {
        super(reader, ppm);
    }
    
    /**
     * 创建图片转换对象实例
     * <p>
     * OFD内部使用毫米作为基本单位
     *
     * @param reader OFD解析器
     * @param ppm    每毫米像素数量(Pixels per millimeter) 调用CommonUtil.dpiToPpm(200) 给定DPI下的像素数量
     */

    public ImageMaker(OFDReader reader,double ppm) {
        super(reader,ppm);
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
        
        //按照标准 ImageIO.write之后还需要对图片 exif dpi设置生成的dpi值 这样第三方通过像素和DPI换算出纸质尺寸
        int pageWidthPixel = (int) Math.round(ppm * pageBox.getWidth());
        int pageHeightPixel = (int) Math.round(ppm * pageBox.getHeight());

        BufferedImage image = createImage(pageWidthPixel,pageHeightPixel);
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
