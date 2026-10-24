package org.ofdrw.converter.renderer.command;

import org.ofdrw.converter.renderer.BlendMode;
import org.ofdrw.converter.renderer.ImageHandle;

import java.awt.geom.AffineTransform;

/**
 * 绘制图片命令。
 */
public final class DrawImageCommand extends DrawCommand {

    /**
     * 图片句柄
     */
    private final ImageHandle image;

    /**
     * 图片到当前坐标系的变换
     */
    private final AffineTransform transform;

    /**
     * 图片不透明度，取值 0.0–1.0，无量纲
     */
    private final float alpha;

    /**
     * 是否使用双线性插值
     */
    private final boolean smoothing;

    /**
     * 透明度蒙版，可为 null
     */
    private final ImageHandle mask;

    /**
     * 混合模式
     */
    private final BlendMode blend;

    /**
     * 构造绘制图片命令。
     *
     * @param image     图片句柄，不能为空
     * @param transform 图片到当前坐标系的变换，不能为空
     * @param alpha     图片不透明度，取值 0.0–1.0，无量纲
     * @param smoothing true 表示使用双线性插值，false 表示最近邻
     * @param mask      透明度蒙版，可为 null
     * @param blend     混合模式，不能为空
     */
    public DrawImageCommand(ImageHandle image, AffineTransform transform, float alpha, boolean smoothing,
                            ImageHandle mask, BlendMode blend) {
        if (image == null || transform == null || blend == null) {
            throw new IllegalArgumentException("图片、变换与混合模式均不能为空");
        }
        this.image = image;
        this.transform = new AffineTransform(transform);
        this.alpha = alpha;
        this.smoothing = smoothing;
        this.mask = mask;
        this.blend = blend;
    }

    /**
     * 获取图片句柄。
     *
     * @return 图片句柄
     */
    public ImageHandle getImage() {
        return image;
    }

    /**
     * 获取图片变换。
     *
     * @return 图片到当前坐标系的变换（副本）
     */
    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    /**
     * 获取图片不透明度。
     *
     * @return 不透明度，取值 0.0–1.0，无量纲
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * 判断是否使用双线性插值。
     *
     * @return true 表示双线性插值
     */
    public boolean isSmoothing() {
        return smoothing;
    }

    /**
     * 获取透明度蒙版。
     *
     * @return 透明度蒙版，无蒙版时为 null
     */
    public ImageHandle getMask() {
        return mask;
    }

    /**
     * 获取混合模式。
     *
     * @return 混合模式
     */
    public BlendMode getBlend() {
        return blend;
    }

    /**
     * 获取命令对包围盒的坐标贡献。
     *
     * @return 图片四角在本地坐标系中的坐标数组，单位：像素
     */
    @Override
    public double[][] getBounds() {
        double w = image.getWidth();
        double h = image.getHeight();
        return new double[][]{
                {transform.getScaleX() * 0 + transform.getTranslateX(), transform.getTranslateY()},
                {transform.getScaleX() * w + transform.getTranslateX(), transform.getShearY() * w + transform.getTranslateY()},
                {transform.getShearX() * h + transform.getTranslateX(), transform.getScaleY() * h + transform.getTranslateY()},
                {transform.getScaleX() * w + transform.getShearX() * h + transform.getTranslateX(),
                        transform.getShearY() * w + transform.getScaleY() * h + transform.getTranslateY()}
        };
    }

    @Override
    public String toString() {
        return "DrawImage(" + image + ", " + blend + ", alpha=" + alpha + ")";
    }
}
