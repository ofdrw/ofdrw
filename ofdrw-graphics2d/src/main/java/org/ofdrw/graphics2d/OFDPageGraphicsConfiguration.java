package org.ofdrw.graphics2d;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.VolatileImage;

/**
 * OFD页面图形配置
 *
 * @author 权观宇
 * @since 2023-2-23 20:10:03
 */
public final class OFDPageGraphicsConfiguration extends GraphicsConfiguration {

    /**
     * 页面宽度，单位：毫米
     */
    double width;
    /**
     * 页面高度，单位：毫米
     */
    double height;

    /**
     * 设备信息
     */
    private OFDPageGraphicsDevice device;

    /**
     * OFD页面图形配置对象
     *
     * @param width  页面宽度，单位：毫米
     * @param height 页面高度，单位：毫米
     */
    public OFDPageGraphicsConfiguration(double width, double height) {
        super();
        this.width = width;
        this.height = height;
    }

    /**
     * 获取该配置信息所关联的图形设备对象
     *
     * @return 关联的图形设备对象
     */
    @Override
    public GraphicsDevice getDevice() {
        if (this.device == null) {
            this.device = new OFDPageGraphicsDevice(this);
        }
        return this.device;
    }

    /**
     * 获取颜色模式
     *
     * @return 固定位透明模式
     */
    @Override
    public ColorModel getColorModel() {
        return getColorModel(Transparency.TRANSLUCENT);
    }

    /**
     * 获取指定透明类型的颜色模式
     *
     * @param transparency 透明类型
     * @return 颜色模式（可能返回null）
     */
    @Override
    public ColorModel getColorModel(int transparency) {
        switch (transparency) {
            case Transparency.TRANSLUCENT: // 透明
                return ColorModel.getRGBdefault();
            case Transparency.OPAQUE: // 不透明
                return new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff);
            default:
                return null;
        }
    }

    /**
     * 获取默认的变换矩阵
     *
     * @return 默认的变换矩阵
     */
    @Override
    public AffineTransform getDefaultTransform() {
        return new AffineTransform();
    }

    /**
     * 获取归一化变换矩阵
     *
     * @return 归一化变换矩阵
     */
    @Override
    public AffineTransform getNormalizingTransform() {
        return new AffineTransform();
    }

    /**
     * 获取绘制区域边界大小
     *
     * @return 绘制区域大小
     */
    @Override
    public Rectangle getBounds() {
        return new Rectangle2D.Double(0d, 0d, this.width, this.height).getBounds();
    }

    private BufferedImage img;
    private GraphicsConfiguration gc;

    /**
     * 创建图片合成方式
     *
     * @param width        图片宽度
     * @param height       图片高度
     * @param caps         图片功能？
     * @param transparency 透明方式.
     * @return VolatileImage
     * @throws AWTException 图片创建异常
     */
    @Override
    public VolatileImage createCompatibleVolatileImage(int width, int height,
                                                       ImageCapabilities caps, int transparency) throws AWTException {
        if (img == null) {
            img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            gc = img.createGraphics().getDeviceConfiguration();
        }
        return gc.createCompatibleVolatileImage(width, height, caps,
                transparency);
    }

}
