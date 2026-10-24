package org.ofdrw.converter.renderer;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Closeable;

/**
 * 目标无关的绘制契约。
 * <p>
 * 渲染器把布局层解析出的绘制意图（变换、路径、样式、图片）落到具体后端：
 * 本包提供 {@link Graphics2DRenderer}（默认后端）与 {@link RecordingRenderer}（仅记录命令）。
 * <p>
 * 坐标约定：路径坐标与变换均为像素坐标，调用方负责把毫米换算为像素。
 * 状态约定：{@link #save()} / {@link #restore()} 用于保存与恢复变换、裁剪与透明度，
 * {@link #beginPage(int, int)} 会把渲染器状态重置为初始值。
 * 线程模型：渲染器非线程安全，单个实例一次只应被一个线程使用。
 */
public interface Renderer extends Closeable {

    /**
     * 开始绘制新的一页，并把渲染器状态重置为初始值。
     *
     * @param widthPx  页面宽度，单位：像素，必须大于 0
     * @param heightPx 页面高度，单位：像素，必须大于 0
     */
    void beginPage(int widthPx, int heightPx);

    /**
     * 结束当前页面的绘制。
     */
    void endPage();

    /**
     * 保存当前绘制状态（变换、裁剪、透明度），必须与 {@link #restore()} 成对调用。
     */
    void save();

    /**
     * 恢复最近一次 {@link #save()} 保存的绘制状态。
     */
    void restore();

    /**
     * 用指定变换整体替换当前变换。
     *
     * @param transform 新变换，不能为空
     */
    void setTransform(AffineTransform transform);

    /**
     * 在当前变换之后叠加变换。
     *
     * @param transform 叠加的变换，不能为空
     */
    void transform(AffineTransform transform);

    /**
     * 平移坐标系。
     *
     * @param tx 水平平移量，单位：像素
     * @param ty 垂直平移量，单位：像素
     */
    void translate(double tx, double ty);

    /**
     * 把当前绘制透明度乘以指定系数，用于实现图层级 Alpha 叠加。
     *
     * @param alpha 透明度系数，取值 0.0–1.0，无量纲
     */
    void multiplyAlpha(float alpha);

    /**
     * 直接设定当前绘制透明度，替换此前的透明度取值。
     * <p>
     * 与 {@link #multiplyAlpha(float)} 的区别：本方法不累积，用于表达“本元素整体按指定透明度绘制”的语义；
     * 多次调用以最后一次为准。透明度会随 {@link #save()} / {@link #restore()} 一起保存与恢复。
     *
     * @param alpha 不透明度，取值 0.0–1.0（1.0 表示完全不透明），无量纲
     */
    void setAlpha(float alpha);

    /**
     * 开始一条新路径，并清空此前的路径点。
     */
    void beginPath();

    /**
     * 把当前点移动到指定位置。
     *
     * @param x X 坐标，单位：像素
     * @param y Y 坐标，单位：像素
     */
    void moveTo(double x, double y);

    /**
     * 从当前点画直线到指定位置。
     *
     * @param x X 坐标，单位：像素
     * @param y Y 坐标，单位：像素
     */
    void lineTo(double x, double y);

    /**
     * 从当前点画二次贝塞尔曲线。
     *
     * @param cpx 控制点 X 坐标，单位：像素
     * @param cpy 控制点 Y 坐标，单位：像素
     * @param x   终点 X 坐标，单位：像素
     * @param y   终点 Y 坐标，单位：像素
     */
    void quadraticCurveTo(double cpx, double cpy, double x, double y);

    /**
     * 从当前点画三次贝塞尔曲线。
     *
     * @param cp1x 第一个控制点 X 坐标，单位：像素
     * @param cp1y 第一个控制点 Y 坐标，单位：像素
     * @param cp2x 第二个控制点 X 坐标，单位：像素
     * @param cp2y 第二个控制点 Y 坐标，单位：像素
     * @param x    终点 X 坐标，单位：像素
     * @param y    终点 Y 坐标，单位：像素
     */
    void cubicCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y);

    /**
     * 在当前路径中追加一个矩形子路径。
     *
     * @param x     左上角 X 坐标，单位：像素
     * @param y     左上角 Y 坐标，单位：像素
     * @param width 矩形宽度，单位：像素
     * @param height 矩形高度，单位：像素
     */
    void rect(double x, double y, double width, double height);

    /**
     * 闭合当前子路径。
     */
    void closePath();

    /**
     * 用当前路径与裁剪区域求交，作为新的裁剪区域。
     *
     * @param rule 填充规则
     */
    void clip(FillRule rule);

    /**
     * 用任意形状与裁剪区域求交，作为新的裁剪区域。
     * <p>
     * 用于裁剪区域不是由路径命令逐点构建、而是已存在 {@link Shape}（如字形轮廓、多边形边界）的场景。
     *
     * @param shape 裁剪形状，不能为空；坐标单位：像素
     * @param rule  填充规则
     */
    void clip(Shape shape, FillRule rule);

    /**
     * 清除当前裁剪区域，恢复为不裁剪。
     * <p>
     * 只影响当前状态，不影响 {@link #save()} 栈中已保存的裁剪；{@link #restore()} 会把它恢复回来。
     */
    void clearClip();

    /**
     * 用当前路径填充。
     *
     * @param style 绘制样式，不能为空
     * @param paint 填充着色源，不能为空
     * @param rule  填充规则
     */
    void fill(DrawStyle style, ResolvedPaint paint, FillRule rule);

    /**
     * 用任意形状填充。
     * <p>
     * 用于形状不是由路径命令逐点构建、而是已存在 {@link Shape}（如字形轮廓）的场景。
     *
     * @param shape 待填充形状，不能为空；坐标单位：像素
     * @param style 绘制样式，不能为空
     * @param paint 填充着色源，不能为空
     * @param rule  填充规则
     */
    void fill(Shape shape, DrawStyle style, ResolvedPaint paint, FillRule rule);

    /**
     * 用当前路径描边。
     *
     * @param style 绘制样式，不能为空
     * @param paint 描边着色源，不能为空
     */
    void stroke(DrawStyle style, ResolvedPaint paint);

    /**
     * 用任意形状描边。
     * <p>
     * 用于形状不是由路径命令逐点构建、而是已存在 {@link Shape}（如字形轮廓）的场景。
     *
     * @param shape 待描边形状，不能为空；坐标单位：像素
     * @param style 绘制样式，不能为空
     * @param paint 描边着色源，不能为空
     */
    void stroke(Shape shape, DrawStyle style, ResolvedPaint paint);

    /**
     * 注册一张图片，返回可在绘制阶段引用的句柄。
     *
     * @param image 图片像素数据，不能为空
     * @return 图片句柄
     */
    ImageHandle createImage(BufferedImage image);

    /**
     * 按指定变换绘制图片。
     *
     * @param image     图片句柄，不能为空
     * @param transform 图片到当前坐标系的变换，不能为空
     * @param alpha     图片不透明度，取值 0.0–1.0，无量纲
     * @param smoothing true 表示插值平滑（双线性），false 表示最近邻
     * @param mask      透明度蒙版，可为 null 表示无蒙版；当前实现忽略该参数（见实现类说明）
     * @param blend     混合模式，不能为空；特定后端可能不支持非 {@link BlendMode#NORMAL} 的混合模式
     */
    void drawImage(ImageHandle image, AffineTransform transform, float alpha, boolean smoothing,
                   ImageHandle mask, BlendMode blend);

    /**
     * 以透明色清空指定矩形区域。
     *
     * @param x      左上角 X 坐标，单位：像素
     * @param y      左上角 Y 坐标，单位：像素
     * @param width  矩形宽度，单位：像素
     * @param height 矩形高度，单位：像素
     */
    void clear(double x, double y, double width, double height);

    /**
     * 释放渲染器占用的资源。
     */
    @Override
    void close();
}
