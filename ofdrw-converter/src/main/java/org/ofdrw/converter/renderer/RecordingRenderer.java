package org.ofdrw.converter.renderer;

import org.ofdrw.converter.renderer.command.BeginPathCommand;
import org.ofdrw.converter.renderer.command.ClearClipCommand;
import org.ofdrw.converter.renderer.command.ClearCommand;
import org.ofdrw.converter.renderer.command.ClipShapeCommand;
import org.ofdrw.converter.renderer.command.FillShapeCommand;
import org.ofdrw.converter.renderer.command.SetAlphaCommand;
import org.ofdrw.converter.renderer.command.StrokeShapeCommand;
import org.ofdrw.converter.renderer.command.ClipCommand;
import org.ofdrw.converter.renderer.command.ClosePathCommand;
import org.ofdrw.converter.renderer.command.CubicCurveToCommand;
import org.ofdrw.converter.renderer.command.DrawImageCommand;
import org.ofdrw.converter.renderer.command.FillCommand;
import org.ofdrw.converter.renderer.command.LineToCommand;
import org.ofdrw.converter.renderer.command.MoveToCommand;
import org.ofdrw.converter.renderer.command.MultiplyAlphaCommand;
import org.ofdrw.converter.renderer.command.QuadraticCurveToCommand;
import org.ofdrw.converter.renderer.command.RectCommand;
import org.ofdrw.converter.renderer.command.RestoreCommand;
import org.ofdrw.converter.renderer.command.SaveCommand;
import org.ofdrw.converter.renderer.command.SetTransformCommand;
import org.ofdrw.converter.renderer.command.StrokeCommand;
import org.ofdrw.converter.renderer.command.TransformCommand;
import org.ofdrw.converter.renderer.command.TranslateCommand;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 只记录绘制命令、不做实际绘制的渲染器。
 * <p>
 * 用于单元测试断言命令顺序与参数，也用于把一次绘制过程录制为
 * {@link DisplayList} 后脱离原始输入回放。
 * {@link #beginPage(int, int)} 与 {@link #endPage()} 开始一个新的命令列表。
 * 本类非线程安全。
 */
public class RecordingRenderer implements Renderer {

    /**
     * 当前页的绘制命令列表
     */
    private DisplayList displayList = new DisplayList();

    /**
     * 已完成的页面命令列表
     */
    private final List<DisplayList> pages = new ArrayList<>();

    /**
     * 已注册的图片句柄
     */
    private final List<ImageHandle> images = new ArrayList<>();

    /**
     * 当前页宽度，单位：像素
     */
    private int widthPx;

    /**
     * 当前页高度，单位：像素
     */
    private int heightPx;

    /**
     * 是否已经开始过页面
     */
    private boolean pageStarted = false;

    /**
     * 是否已经关闭
     */
    private boolean closed = false;

    /**
     * 获取命令列表：录制进行中返回当前页，已结束录制时返回最后一页。
     *
     * @return 命令列表
     */
    public DisplayList getDisplayList() {
        if (pageStarted) {
            return displayList;
        }
        return pages.isEmpty() ? displayList : pages.get(pages.size() - 1);
    }

    /**
     * 获取所有已完成页面的命令列表（包含当前页）。
     *
     * @return 页面命令列表
     */
    public List<DisplayList> getPages() {
        List<DisplayList> all = new ArrayList<>(pages);
        if (pageStarted) {
            all.add(displayList);
        }
        return all;
    }

    /**
     * 获取已注册的图片句柄。
     *
     * @return 图片句柄列表
     */
    public List<ImageHandle> getImages() {
        return new ArrayList<>(images);
    }

    /**
     * 获取当前页宽度。
     *
     * @return 当前页宽度，单位：像素
     */
    public int getWidth() {
        return widthPx;
    }

    /**
     * 获取当前页高度。
     *
     * @return 当前页高度，单位：像素
     */
    public int getHeight() {
        return heightPx;
    }

    /**
     * 开始记录新页面。
     *
     * @param widthPx  页面宽度，单位：像素，必须大于 0
     * @param heightPx 页面高度，单位：像素，必须大于 0
     */
    @Override
    public void beginPage(int widthPx, int heightPx) {
        if (closed) {
            throw new IllegalStateException("渲染器已关闭");
        }
        if (widthPx <= 0 || heightPx <= 0) {
            throw new IllegalArgumentException("页面尺寸必须大于 0，单位：像素");
        }
        if (pageStarted) {
            pages.add(displayList);
        }
        this.displayList = new DisplayList();
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.pageStarted = true;
    }

    /**
     * 结束当前页面的记录。
     */
    @Override
    public void endPage() {
        if (pageStarted) {
            pages.add(displayList);
            pageStarted = false;
            displayList = new DisplayList();
        }
    }

    /**
     * 记录保存状态命令。
     */
    @Override
    public void save() {
        displayList.add(new SaveCommand());
    }

    /**
     * 记录恢复状态命令。
     */
    @Override
    public void restore() {
        displayList.add(new RestoreCommand());
    }

    /**
     * 记录替换变换命令。
     *
     * @param transform 新变换，不能为空
     */
    @Override
    public void setTransform(AffineTransform transform) {
        displayList.add(new SetTransformCommand(transform));
    }

    /**
     * 记录叠加变换命令。
     *
     * @param transform 叠加的变换，不能为空
     */
    @Override
    public void transform(AffineTransform transform) {
        displayList.add(new TransformCommand(transform));
    }

    /**
     * 记录平移命令。
     *
     * @param tx 水平平移量，单位：像素
     * @param ty 垂直平移量，单位：像素
     */
    @Override
    public void translate(double tx, double ty) {
        displayList.add(new TranslateCommand(tx, ty));
    }

    /**
     * 记录乘以透明度命令。
     *
     * @param alpha 透明度系数，取值 0.0–1.0，无量纲
     */
    @Override
    public void multiplyAlpha(float alpha) {
        displayList.add(new MultiplyAlphaCommand(alpha));
    }

    /**
     * 记录直接设定透明度命令。
     *
     * @param alpha 不透明度，取值 0.0–1.0，无量纲
     */
    @Override
    public void setAlpha(float alpha) {
        displayList.add(new SetAlphaCommand(alpha));
    }

    /**
     * 记录开始路径命令。
     */
    @Override
    public void beginPath() {
        displayList.add(new BeginPathCommand());
    }

    /**
     * 记录移动命令。
     *
     * @param x X 坐标，单位：像素
     * @param y Y 坐标，单位：像素
     */
    @Override
    public void moveTo(double x, double y) {
        displayList.add(new MoveToCommand(x, y));
    }

    /**
     * 记录直线命令。
     *
     * @param x X 坐标，单位：像素
     * @param y Y 坐标，单位：像素
     */
    @Override
    public void lineTo(double x, double y) {
        displayList.add(new LineToCommand(x, y));
    }

    /**
     * 记录二次贝塞尔曲线命令。
     *
     * @param cpx 控制点 X 坐标，单位：像素
     * @param cpy 控制点 Y 坐标，单位：像素
     * @param x   终点 X 坐标，单位：像素
     * @param y   终点 Y 坐标，单位：像素
     */
    @Override
    public void quadraticCurveTo(double cpx, double cpy, double x, double y) {
        displayList.add(new QuadraticCurveToCommand(cpx, cpy, x, y));
    }

    /**
     * 记录三次贝塞尔曲线命令。
     *
     * @param cp1x 第一个控制点 X 坐标，单位：像素
     * @param cp1y 第一个控制点 Y 坐标，单位：像素
     * @param cp2x 第二个控制点 X 坐标，单位：像素
     * @param cp2y 第二个控制点 Y 坐标，单位：像素
     * @param x    终点 X 坐标，单位：像素
     * @param y    终点 Y 坐标，单位：像素
     */
    @Override
    public void cubicCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y) {
        displayList.add(new CubicCurveToCommand(cp1x, cp1y, cp2x, cp2y, x, y));
    }

    /**
     * 记录矩形子路径命令。
     *
     * @param x      左上角 X 坐标，单位：像素
     * @param y      左上角 Y 坐标，单位：像素
     * @param width  宽度，单位：像素
     * @param height 高度，单位：像素
     */
    @Override
    public void rect(double x, double y, double width, double height) {
        displayList.add(new RectCommand(x, y, width, height));
    }

    /**
     * 记录闭合子路径命令。
     */
    @Override
    public void closePath() {
        displayList.add(new ClosePathCommand());
    }

    /**
     * 记录裁剪命令。
     *
     * @param rule 填充规则
     */
    @Override
    public void clip(FillRule rule) {
        displayList.add(new ClipCommand(rule));
    }

    /**
     * 记录按形状裁剪命令。
     *
     * @param shape 裁剪形状，不能为空；坐标单位：像素
     * @param rule  填充规则
     */
    @Override
    public void clip(Shape shape, FillRule rule) {
        displayList.add(new ClipShapeCommand(shape, rule));
    }

    /**
     * 记录清除裁剪命令。
     */
    @Override
    public void clearClip() {
        displayList.add(new ClearClipCommand());
    }

    /**
     * 记录填充命令。
     *
     * @param style 绘制样式，不能为空
     * @param paint 填充着色源，不能为空
     * @param rule  填充规则
     */
    @Override
    public void fill(DrawStyle style, ResolvedPaint paint, FillRule rule) {
        displayList.add(new FillCommand(style, paint, rule));
    }

    /**
     * 记录按形状填充命令。
     *
     * @param shape 待填充形状，不能为空；坐标单位：像素
     * @param style 绘制样式，不能为空
     * @param paint 填充着色源，不能为空
     * @param rule  填充规则
     */
    @Override
    public void fill(Shape shape, DrawStyle style, ResolvedPaint paint, FillRule rule) {
        displayList.add(new FillShapeCommand(shape, style, paint, rule));
    }

    /**
     * 记录描边命令。
     *
     * @param style 绘制样式，不能为空
     * @param paint 描边着色源，不能为空
     */
    @Override
    public void stroke(DrawStyle style, ResolvedPaint paint) {
        displayList.add(new StrokeCommand(style, paint));
    }

    /**
     * 记录按形状描边命令。
     *
     * @param shape 待描边形状，不能为空；坐标单位：像素
     * @param style 绘制样式，不能为空
     * @param paint 描边着色源，不能为空
     */
    @Override
    public void stroke(Shape shape, DrawStyle style, ResolvedPaint paint) {
        displayList.add(new StrokeShapeCommand(shape, style, paint));
    }

    /**
     * 注册图片并返回句柄；图片内容不参与绘制，只保留引用。
     *
     * @param image 图片像素数据，不能为空
     * @return 图片句柄
     */
    @Override
    public ImageHandle createImage(BufferedImage image) {
        ImageHandle handle = new ImageHandle(images.size(), image);
        images.add(handle);
        return handle;
    }

    /**
     * 记录绘制图片命令。
     *
     * @param image     图片句柄，不能为空
     * @param transform 图片到当前坐标系的变换，不能为空
     * @param alpha     图片不透明度，取值 0.0–1.0，无量纲
     * @param smoothing true 表示插值平滑
     * @param mask      透明度蒙版，可为 null
     * @param blend     混合模式，不能为空
     */
    @Override
    public void drawImage(ImageHandle image, AffineTransform transform, float alpha, boolean smoothing,
                          ImageHandle mask, BlendMode blend) {
        displayList.add(new DrawImageCommand(image, transform, alpha, smoothing, mask, blend));
    }

    /**
     * 记录清空矩形区域命令。
     *
     * @param x      左上角 X 坐标，单位：像素
     * @param y      左上角 Y 坐标，单位：像素
     * @param width  宽度，单位：像素
     * @param height 高度，单位：像素
     */
    @Override
    public void clear(double x, double y, double width, double height) {
        displayList.add(new ClearCommand(x, y, width, height));
    }

    /**
     * 关闭录制渲染器；不影响已录制的命令。
     */
    @Override
    public void close() {
        if (pageStarted) {
            endPage();
        }
        closed = true;
    }
}
