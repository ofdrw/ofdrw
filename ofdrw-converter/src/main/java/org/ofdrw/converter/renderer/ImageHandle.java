package org.ofdrw.converter.renderer;

import java.awt.image.BufferedImage;

/**
 * 已注册到渲染器的图片句柄。
 * <p>
 * 渲染器通过 {@link Renderer#createImage(BufferedImage)} 创建句柄，
 * 绘制阶段只引用句柄，避免每次绘制都传递像素数据。
 * 句柄不可跨渲染器使用。
 */
public final class ImageHandle {

    /**
     * 序号，同一渲染器内唯一
     */
    private final int id;

    /**
     * 图片像素数据
     */
    private final BufferedImage image;

    /**
     * 构造图片句柄。
     *
     * @param id    序号，同一渲染器内唯一
     * @param image 图片像素数据，不能为空
     */
    public ImageHandle(int id, BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("图片像素数据不能为空");
        }
        this.id = id;
        this.image = image;
    }

    /**
     * 获取序号。
     *
     * @return 序号，同一渲染器内唯一
     */
    public int getId() {
        return id;
    }

    /**
     * 获取图片像素数据。
     *
     * @return 图片像素数据
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * 获取图片宽度。
     *
     * @return 图片宽度，单位：像素
     */
    public int getWidth() {
        return image.getWidth();
    }

    /**
     * 获取图片高度。
     *
     * @return 图片高度，单位：像素
     */
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public String toString() {
        return "ImageHandle{id=" + id + ", " + image.getWidth() + "x" + image.getHeight() + "}";
    }
}
