package org.ofdrw.converter.canvas;

import org.ofdrw.converter.AWTMaker;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.converter.utils.CommonUtil;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * 将 OFD 页面绘制到 {@link CanvasSurface} 的图片绘制器。
 * <p>
 * 画布的分配、背景填充与释放由调用方负责，本类只负责把一页内容绘制到给定画布。
 * <p>
 * 注意：{@code AWTMaker.writePage} 内部已完成毫米到像素的缩放，
 * 因此 {@link #drawPage(CanvasSurface, int)} 不会再次设置缩放变换。
 */
public class CanvasImageMaker extends AWTMaker {

    /**
     * 创建画布图片绘制器。
     * <p>
     * OFD内部使用毫米作为基本单位
     *
     * @param reader OFD解析器
     * @param ppm    每毫米像素数量(Pixels per millimeter)，DPI与PPM转换可以使用{@link CommonUtil#dpiToPpm(int)}。
     */
    public CanvasImageMaker(OFDReader reader, double ppm) {
        super(reader, ppm);
    }

    /**
     * 获取当前每毫米像素数量。
     *
     * @return 每毫米像素数量，单位：像素/毫米
     */
    public double getPPM() {
        return ppm;
    }

    /**
     * 获取页面的物理尺寸。
     *
     * @param pageIndex 页码，从 0 起
     * @return 页面尺寸，单位：毫米
     * @throws GeneralConvertException 页码非法
     */
    public ST_Box pageSize(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new GeneralConvertException(String.format("%s 不是有效索引", pageIndex));
        }
        return pages.get(pageIndex).getSize();
    }

    /**
     * 将指定页面绘制到画布。
     * <p>
     * 不分配画布、不填充背景，画布生命周期由调用方管理；绘制内容以画布左上角为原点，按当前 ppm 输出。
     *
     * @param surface   目标画布
     * @param pageIndex 页码，从 0 起
     * @throws GeneralConvertException 画布为空或页码非法
     */
    public void drawPage(CanvasSurface surface, int pageIndex) {
        drawPage(surface, pageIndex, null);
    }

    /**
     * 使用画布级仿射变换将指定页面绘制到画布。
     * <p>
     * 不分配画布、不填充背景，画布生命周期由调用方管理。
     * <p>
     * {@code writePage} 内部已完成毫米到像素的缩放，因此 {@code transform} 作用在已缩放的像素坐标系上，
     * 只能包含旋转与平移，不得包含缩放（旋转导出请使用设计文档 4.2 节的画布变换）。
     * 绘制在独立的 Graphics2D 副本上进行，变换不会泄漏到后续绘制。
     *
     * @param surface   目标画布
     * @param pageIndex 页码，从 0 起
     * @param transform 画布级变换（旋转 + 平移，不含缩放），为 null 表示恒等变换
     * @throws GeneralConvertException 画布为空或页码非法
     */
    public void drawPage(CanvasSurface surface, int pageIndex, AffineTransform transform) {
        if (surface == null) {
            throw new GeneralConvertException("画布不能为空");
        }
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new GeneralConvertException(String.format("%s 不是有效索引", pageIndex));
        }
        PageInfo pageInfo = pages.get(pageIndex);
        Graphics2D graphics = surface.createGraphics();
        try {
            if (transform != null) {
                graphics.transform(transform);
            }
            writePage(graphics, pageInfo, null);
        } finally {
            graphics.dispose();
        }
    }

    /**
     * 将指定页面渲染为位图。
     * <p>
     * 画布尺寸按页面尺寸与 ppm 计算（向上取整）。印章渲染时透明背景会被保留。
     *
     * @param pageIndex 页码，从 0 起
     * @return 渲染完成的位图
     * @throws GeneralConvertException 页码非法
     */
    public BufferedImage makePage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new GeneralConvertException(String.format("%s 不是有效索引", pageIndex));
        }
        ST_Box pageBox = pages.get(pageIndex).getSize();
        // 毫米 转 像素，向上取整避免右侧/底部被裁掉
        int widthPixel = Math.max(1, (int) Math.ceil(ppm * pageBox.getWidth()));
        int heightPixel = Math.max(1, (int) Math.ceil(ppm * pageBox.getHeight()));
        CanvasSurface surface = CanvasSurface.create(widthPixel, heightPixel, null);
        try {
            drawPage(surface, pageIndex);
            // 复制一份像素数据，避免画布释放后位图失效
            return copyOf(surface);
        } finally {
            surface.flush();
        }
    }

    /**
     * 创建用于渲染嵌套 OFD 印章的绘制器，使印章同样走画布设备层。
     *
     * @param nestedReader 印章 OFD 解析器
     * @param ppm          每毫米像素数量，单位：像素/毫米
     * @return 印章绘制器
     */
    @Override
    protected AWTMaker createStampMaker(OFDReader nestedReader, double ppm) {
        return new CanvasImageMaker(nestedReader, ppm);
    }

    /**
     * 用画布设备层渲染印章首页。
     *
     * @param maker     印章绘制器
     * @param pageIndex 页码，从 0 起
     * @return 位图，或 null 表示无可用页面
     */
    @Override
    protected BufferedImage makeStampPage(AWTMaker maker, int pageIndex) {
        if (maker == null || pageIndex < 0 || pageIndex >= maker.pageSize()) {
            return null;
        }
        if (maker instanceof CanvasImageMaker) {
            return ((CanvasImageMaker) maker).makePage(pageIndex);
        }
        return super.makeStampPage(maker, pageIndex);
    }

    /**
     * 复制画布像素数据，返回独立的位图。
     *
     * @param surface 画布
     * @return 与画布同尺寸的位图
     * @throws GeneralConvertException 画布不支持像素读取
     */
    private static BufferedImage copyOf(CanvasSurface surface) {
        if (!(surface instanceof BufferedImageSurface)) {
            throw new GeneralConvertException(String.format("画布实现 %s 不支持像素读取", surface.getClass().getName()));
        }
        BufferedImage source = ((BufferedImageSurface) surface).getImage();
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        try {
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return copy;
    }
}
