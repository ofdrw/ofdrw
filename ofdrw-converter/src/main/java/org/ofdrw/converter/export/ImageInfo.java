package org.ofdrw.converter.export;

import java.nio.file.Path;

/**
 * 单页导出图片的信息快照。
 * <p>
 * 由 {@link CanvasImageExporter#getImageInfos()} 返回，用于校验导出结果的实际尺寸、方向与输出位置。
 *
 * @author 权观宇
 * @since 2024-10-24
 */
public class ImageInfo {

    /**
     * 页码，从 0 起
     */
    private final int pageIndex;
    /**
     * 输出图片宽度，单位：像素
     */
    private final int width;
    /**
     * 输出图片高度，单位：像素
     */
    private final int height;
    /**
     * 顺时针旋转角度，单位：度
     */
    private final int rotation;
    /**
     * 每毫米像素数量，单位：像素/毫米
     */
    private final double ppm;
    /**
     * 输出文件路径
     */
    private final Path path;

    /**
     * 构造图片信息。
     *
     * @param pageIndex 页码，从 0 起
     * @param width     输出图片宽度，单位：像素
     * @param height    输出图片高度，单位：像素
     * @param rotation  顺时针旋转角度，单位：度
     * @param ppm       每毫米像素数量，单位：像素/毫米
     * @param path      输出文件路径
     */
    public ImageInfo(int pageIndex, int width, int height, int rotation, double ppm, Path path) {
        this.pageIndex = pageIndex;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.ppm = ppm;
        this.path = path;
    }

    /**
     * 获取页码。
     *
     * @return 页码，从 0 起
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * 获取输出图片宽度。
     *
     * @return 输出图片宽度，单位：像素
     */
    public int getWidth() {
        return width;
    }

    /**
     * 获取输出图片高度。
     *
     * @return 输出图片高度，单位：像素
     */
    public int getHeight() {
        return height;
    }

    /**
     * 获取顺时针旋转角度。
     *
     * @return 顺时针旋转角度，单位：度
     */
    public int getRotation() {
        return rotation;
    }

    /**
     * 获取每毫米像素数量。
     *
     * @return 每毫米像素数量，单位：像素/毫米
     */
    public double getPpm() {
        return ppm;
    }

    /**
     * 获取输出文件路径。
     *
     * @return 输出文件路径
     */
    public Path getPath() {
        return path;
    }

    @Override
    public String toString() {
        return String.format("ImageInfo{page=%d, %dx%d, rotation=%d, ppm=%.3f, path=%s}",
                pageIndex, width, height, rotation, ppm, path);
    }
}
