package org.ofdrw.converter.canvas;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 基于 {@link BufferedImage} 的像素画布实现。
 * <p>
 * 底层像素格式固定为 {@link BufferedImage#TYPE_INT_ARGB}，支持透明通道；
 * 未指定背景色时画布保持全透明。
 */
public class BufferedImageSurface extends CanvasSurface {

    /**
     * 底层像素图像
     */
    private final BufferedImage image;

    /**
     * 创建基于 BufferedImage 的画布。
     *
     * @param width      宽度，单位：像素，必须大于 0
     * @param height     高度，单位：像素，必须大于 0
     * @param background 背景色，为 null 表示保持全透明（不填充）
     * @throws IllegalArgumentException 宽度或高度不大于 0
     */
    public BufferedImageSurface(int width, int height, Color background) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(String.format("画布尺寸非法：%d x %d，宽高必须大于 0", width, height));
        }
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if (background != null) {
            // 使用 SRC 模式覆盖底色，保证背景色（含 alpha）替换初始透明像素
            Graphics2D g = this.image.createGraphics();
            try {
                g.setComposite(AlphaComposite.Src);
                g.setColor(background);
                g.fillRect(0, 0, width, height);
            } finally {
                g.dispose();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return 画布宽度，单位：像素
     */
    @Override
    public int getWidth() {
        return image.getWidth();
    }

    /**
     * {@inheritDoc}
     *
     * @return 画布高度，单位：像素
     */
    @Override
    public int getHeight() {
        return image.getHeight();
    }

    /**
     * {@inheritDoc}
     *
     * @return 新建的绘制上下文，调用方负责 dispose
     */
    @Override
    public Graphics2D createGraphics() {
        return image.createGraphics();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
        image.flush();
    }

    /**
     * 获取底层像素图像，用于编码输出。
     *
     * @return 底层像素图像
     */
    public BufferedImage getImage() {
        return image;
    }
}
