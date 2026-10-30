package org.ofdrw.converter.pdf.model;

import org.ofdrw.converter.pdf.font.ImageEncoding;

/**
 * 图像绘制指令：对应 PDF 中的 {@code cm} + {@code Do}（Image XObject）
 * <p>
 * 设计 3.7 节与决策 23（保真优先）：图像数据由 {@link #getImageData()} 原样或无损携带，
 * 编码选择由 {@link #getEncoding()} 声明，写入器不得对有损编码的原始数据做二次编码。
 * <p>
 * 坐标系约定：{@link #getPlacement()} 是图像在 PDF 页面坐标（原点左下、单位 pt）中的外接矩形，
 * 图像内容按 PDF 约定以“单位正方形 + 变换矩阵”放置，即 {@code cm width 0 0 height x y cm}。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ImageCommand extends AbstractCommand {

    /**
     * 图像资源标识（同一页面内复用同一图像对象时使用）
     */
    private final String resourceKey;

    /**
     * 图像编码
     */
    private final ImageEncoding encoding;

    /**
     * 图像像素宽度
     */
    private final int pixelWidth;

    /**
     * 图像像素高度
     */
    private final int pixelHeight;

    /**
     * 图像数据（原始编码字节）
     */
    private final byte[] imageData;

    /**
     * 图像在页面中的放置区域，单位 pt（PDF 页面坐标）
     */
    private final Box placement;

    /**
     * 是否启用图像蒙版（PDF {@code /Mask} 或 {@code /SMask}）
     */
    private final boolean masked;

    /**
     * 显式放置矩阵（单位正方形 → 页面坐标），可为空
     */
    private final AffineMatrix explicitMatrix;

    /**
     * 构造图像指令
     *
     * @param resourceKey 图像资源标识，不能为空
     * @param encoding    图像编码，不能为空
     * @param pixelWidth  像素宽度，必须大于 0
     * @param pixelHeight 像素高度，必须大于 0
     * @param imageData   图像数据，不能为空
     * @param placement   放置区域，单位 pt（PDF 页面坐标），不能为空
     * @param masked      是否启用图像蒙版
     */
    public ImageCommand(String resourceKey, ImageEncoding encoding, int pixelWidth, int pixelHeight,
                        byte[] imageData, Box placement, boolean masked) {
        this(resourceKey, encoding, pixelWidth, pixelHeight, imageData, placement, masked, null);
    }

    /**
     * 构造带完整放置矩阵的图像指令
     * <p>
     * 放置矩阵为单位正方形对象空间 → 页面坐标的完整变换（含旋转、镜像）。
     * 传入后写入侧以 {@code cm} + {@code Do} 绘制，旋转/镜像图像不会被退化为外接矩形。
     *
     * @param resourceKey 图像资源标识，不能为空
     * @param encoding    图像编码，不能为空
     * @param pixelWidth  像素宽度，必须大于 0
     * @param pixelHeight 像素高度，必须大于 0
     * @param imageData   图像数据，不能为空
     * @param placement   放置区域，单位 pt（PDF 页面坐标），不能为空
     * @param masked      是否启用图像蒙版
     * @param placementMatrix 单位正方形 → 页面坐标的放置矩阵，可为空（为空时按轴对齐矩形绘制）
     */
    public ImageCommand(String resourceKey, ImageEncoding encoding, int pixelWidth, int pixelHeight,
                        byte[] imageData, Box placement, boolean masked, AffineMatrix placementMatrix) {
        super(CoordSpace.PAGE);
        if (resourceKey == null || resourceKey.isEmpty()) {
            throw new IllegalArgumentException("图像资源标识不能为空");
        }
        if (encoding == null) {
            throw new IllegalArgumentException("图像编码不能为空");
        }
        if (pixelWidth <= 0 || pixelHeight <= 0) {
            throw new IllegalArgumentException("图像尺寸必须大于 0: " + pixelWidth + " x " + pixelHeight);
        }
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("图像数据不能为空");
        }
        if (placement == null) {
            throw new IllegalArgumentException("图像放置区域不能为空");
        }
        this.resourceKey = resourceKey;
        this.encoding = encoding;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
        this.imageData = imageData.clone();
        this.placement = placement;
        this.masked = masked;
        this.explicitMatrix = placementMatrix;
    }

    /**
     * @return 图像资源标识
     */
    public String getResourceKey() {
        return resourceKey;
    }

    /**
     * @return 图像编码
     */
    public ImageEncoding getEncoding() {
        return encoding;
    }

    /**
     * @return 像素宽度
     */
    public int getPixelWidth() {
        return pixelWidth;
    }

    /**
     * @return 像素高度
     */
    public int getPixelHeight() {
        return pixelHeight;
    }

    /**
     * @return 图像数据副本
     */
    public byte[] getImageData() {
        return imageData.clone();
    }

    /**
     * @return 放置区域，单位 pt（PDF 页面坐标）
     */
    public Box getPlacement() {
        return placement;
    }

    /**
     * @return 是否启用图像蒙版
     */
    public boolean isMasked() {
        return masked;
    }

    /**
     * 计算放置矩阵：PDF 图像以单位正方形绘制，放置矩阵把单位正方形映射到放置区域，
     * 矩阵形式为 {@code [width 0 0 height x y]}（x、y 为放置区域左下角，单位 pt）。
     *
     * @return 放置矩阵（在 PDF 页面坐标中将单位正方形映射到放置区域）
     */
    public AffineMatrix getPlacementMatrix() {
        if (explicitMatrix != null) {
            return explicitMatrix;
        }
        return new AffineMatrix(placement.getWidth(), 0, 0, placement.getHeight(),
                placement.getX(), placement.getY());
    }

    @Override
    public String toString() {
        return "Image(" + resourceKey + ", " + encoding + ", " + pixelWidth + "x" + pixelHeight + ", " + placement + ")";
    }
}
