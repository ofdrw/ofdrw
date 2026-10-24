package org.ofdrw.converter.renderer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 基于 {@link Graphics2D} 的默认渲染器后端。
 * <p>
 * 把 {@link Renderer} 的目标无关调用等价下沉为 {@code Graphics2D} 调用：
 * <ul>
 *     <li>{@link #setTransform(AffineTransform)} / {@link #transform(AffineTransform)} / {@link #translate(double, double)}
 *     对应 {@code graphics.setTransform} / {@code graphics.transform} / {@code graphics.translate}；</li>
 *     <li>{@link #clip(FillRule)} 对应 {@code graphics.clip(path)}；</li>
 *     <li>{@link #fill(DrawStyle, ResolvedPaint, FillRule)} / {@link #stroke(DrawStyle, ResolvedPaint)}
 *     对应 {@code graphics.setColor} + {@code graphics.fill(path)} / {@code graphics.draw(path)}；</li>
 *     <li>{@link #drawImage(ImageHandle, AffineTransform, float, boolean, ImageHandle, BlendMode)}
 *     对应 {@code graphics.drawImage(image, transform, null)}，Alpha 由 {@link AlphaComposite} 表达；</li>
 *     <li>{@link #clear(double, double, double, double)} 对应 {@code graphics.setComposite(Src)} + {@code fillRect}。</li>
 * </ul>
 * 渲染器内部自持一条 AWT 裁剪<em>交集栈</em>：{@code Graphics2D.setClip} 是整体替换语义，
 * 而绘制抽象要求裁剪区域只增不减，因此本类在 {@code restore} 时用 {@link Graphics2D#setClip(Shape)} 恢复，
 * 保证与外层裁剪的求交关系不被破坏。
 * <p>
 * 支持的绘制目标：{@link BufferedImage}（离屏渲染）、{@link Graphics2D}（直接复用调用方上下文的绘制语义）。
 * 本类非线程安全。
 */
public class Graphics2DRenderer implements Renderer {

    /**
     * 日志
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Graphics2DRenderer.class);

    /**
     * 绘制上下文：离屏渲染模式下每页新建
     */
    private Graphics2D graphics;

    /**
     * 输出位图：离屏渲染模式下每页新建，直接使用 {@link Graphics2D} 时为空
     */
    private BufferedImage output;

    /**
     * 当前页宽度，单位：像素
     */
    private int widthPx;

    /**
     * 当前页高度，单位：像素
     */
    private int heightPx;

    /**
     * 外部提供的绘制上下文；非空表示直接在其上绘制，不再创建离屏位图
     */
    private final Graphics2D externalGraphics;

    /**
     * 是否在关闭时释放创建出来的位图与上下文
     */
    private final boolean ownsGraphics;

    /**
     * 裁剪栈中的空裁剪占位：{@link ArrayDeque} 不允许 null 元素
     */
    private static final Shape NO_CLIP = new java.awt.geom.Rectangle2D.Double();

    /**
     * 保存栈中的变换
     */
    private final Deque<AffineTransform> transforms = new ArrayDeque<>();

    /**
     * 保存栈中的裁剪交集区域
     */
    private final Deque<Shape> clipStack = new ArrayDeque<>();

    /**
     * 保存栈中的透明度
     */
    private final Deque<AlphaComposite> alphaStack = new ArrayDeque<>();

    /**
     * 当前透明度
     */
    private AlphaComposite currentAlpha = (AlphaComposite) AlphaComposite.SrcOver;

    /**
     * 当前裁剪区域，null 表示不裁剪
     */
    private Shape currentClip;

    /**
     * 当前路径
     */
    private Path2D.Double path = new Path2D.Double();

    /**
     * 是否已经关闭
     */
    private boolean closed = false;

    /**
     * 创建离屏渲染器：每调用一次 {@link #beginPage(int, int)} 新建一张 ARGB 位图。
     */
    public Graphics2DRenderer() {
        this.externalGraphics = null;
        this.ownsGraphics = true;
    }

    /**
     * 创建直接绘制到外部 {@link Graphics2D} 的渲染器。
     * <p>
     * 适用于调用方已持有绘制上下文（例如已经完成缩放或背景填充）的场景；
     * {@link #beginPage(int, int)} 只记录页面尺寸，不会在外部上下文上做任何清理。
     * 渲染器不会关闭调用方传入的 {@link Graphics2D}。
     *
     * @param graphics 外部绘制上下文，不能为空
     */
    public Graphics2DRenderer(Graphics2D graphics) {
        if (graphics == null) {
            throw new IllegalArgumentException("绘制上下文不能为空");
        }
        this.externalGraphics = graphics;
        this.graphics = graphics;
        this.ownsGraphics = false;
    }

    /**
     * 获取离屏渲染的输出位图。
     *
     * @return 输出位图；直接使用外部 {@link Graphics2D} 或尚未开始页面时为 null
     */
    public BufferedImage getOutput() {
        return output;
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
     * 开始绘制新页面，重置渲染器状态；离屏模式下新建 ARGB 位图（透明度为零）。
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
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        transforms.clear();
        clipStack.clear();
        alphaStack.clear();
        currentClip = null;
        currentAlpha = (AlphaComposite) AlphaComposite.SrcOver;
        path = new Path2D.Double();
        if (ownsGraphics) {
            if (graphics != null) {
                graphics.dispose();
            }
            output = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_ARGB);
            graphics = output.createGraphics();
        }
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /**
     * 结束当前页面的绘制；离屏模式下把图形上下文刷入输出位图（不释放位图）。
     */
    @Override
    public void endPage() {
        if (graphics != null) {
            graphics.setClip(null);
            graphics.setComposite((AlphaComposite) AlphaComposite.SrcOver);
            graphics.setTransform(new AffineTransform());
            if (ownsGraphics) {
                // 释放图形上下文对位图的写锁，位图仍可继续读取
                graphics.dispose();
                graphics = output.createGraphics();
            }
        }
    }

    /**
     * 保存变换、裁剪与透明度。
     */
    @Override
    public void save() {
        transforms.push(graphics.getTransform());
        clipStack.push(currentClip == null ? NO_CLIP : currentClip);
        alphaStack.push(currentAlpha);
    }

    /**
     * 恢复最近一次保存的变换、裁剪与透明度。
     */
    @Override
    public void restore() {
        if (clipStack.isEmpty() || alphaStack.isEmpty() || transforms.isEmpty()) {
            throw new IllegalStateException("restore 与 save 不匹配");
        }
        Shape savedClip = clipStack.pop();
        currentClip = savedClip == NO_CLIP ? null : savedClip;
        currentAlpha = alphaStack.pop();
        graphics.setTransform(transforms.pop());
        // setClip 为整体替换语义，这里用保存的裁剪交集恢复，保证与外层裁剪仍是求交关系
        graphics.setClip(currentClip);
        graphics.setComposite(currentAlpha);
    }

    /**
     * 整体替换当前变换。
     *
     * @param transform 新变换，不能为空
     */
    @Override
    public void setTransform(AffineTransform transform) {
        graphics.setTransform(transform);
    }

    /**
     * 在当前变换之后叠加变换。
     *
     * @param transform 叠加的变换，不能为空
     */
    @Override
    public void transform(AffineTransform transform) {
        graphics.transform(transform);
    }

    /**
     * 平移坐标系。
     *
     * @param tx 水平平移量，单位：像素
     * @param ty 垂直平移量，单位：像素
     */
    @Override
    public void translate(double tx, double ty) {
        graphics.translate(tx, ty);
    }

    /**
     * 以 SrcOver 规则把当前透明度乘以指定系数。
     *
     * @param alpha 透明度系数，取值 0.0–1.0，无量纲
     */
    @Override
    public void multiplyAlpha(float alpha) {
        currentAlpha = (AlphaComposite) AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER,
                Math.max(0f, Math.min(1f, currentAlpha.getAlpha() * alpha)));
        graphics.setComposite(currentAlpha);
    }

    /**
     * 直接设定当前透明度（不累积），用于表达元素整体透明度语义。
     *
     * @param alpha 不透明度，取值 0.0–1.0，无量纲
     */
    @Override
    public void setAlpha(float alpha) {
        currentAlpha = (AlphaComposite) AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha)));
        graphics.setComposite(currentAlpha);
    }

    /**
     * 开始一条新路径。
     */
    @Override
    public void beginPath() {
        path = new Path2D.Double();
    }

    /**
     * 把当前点移动到指定位置。
     *
     * @param x X 坐标，单位：像素
     * @param y Y 坐标，单位：像素
     */
    @Override
    public void moveTo(double x, double y) {
        path.moveTo(x, y);
    }

    /**
     * 画直线到指定位置。
     *
     * @param x X 坐标，单位：像素
     * @param y Y 坐标，单位：像素
     */
    @Override
    public void lineTo(double x, double y) {
        path.lineTo(x, y);
    }

    /**
     * 画二次贝塞尔曲线。
     *
     * @param cpx 控制点 X 坐标，单位：像素
     * @param cpy 控制点 Y 坐标，单位：像素
     * @param x   终点 X 坐标，单位：像素
     * @param y   终点 Y 坐标，单位：像素
     */
    @Override
    public void quadraticCurveTo(double cpx, double cpy, double x, double y) {
        path.quadTo(cpx, cpy, x, y);
    }

    /**
     * 画三次贝塞尔曲线。
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
        path.curveTo(cp1x, cp1y, cp2x, cp2y, x, y);
    }

    /**
     * 追加矩形子路径。
     *
     * @param x      左上角 X 坐标，单位：像素
     * @param y      左上角 Y 坐标，单位：像素
     * @param width  宽度，单位：像素
     * @param height 高度，单位：像素
     */
    @Override
    public void rect(double x, double y, double width, double height) {
        path.append(new java.awt.geom.Rectangle2D.Double(x, y, width, height), false);
    }

    /**
     * 闭合当前子路径。
     */
    @Override
    public void closePath() {
        path.closePath();
    }

    /**
     * 用当前路径与既有裁剪区域求交。
     *
     * @param rule 填充规则
     */
    @Override
    public void clip(FillRule rule) {
        Path2D clipPath = (Path2D) path.clone();
        clipPath.setWindingRule(rule == FillRule.EVEN_ODD ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
        Shape clip = clipPath;
        if (currentClip != null) {
            java.awt.geom.Area area = new java.awt.geom.Area(currentClip);
            area.intersect(new java.awt.geom.Area(clipPath));
            clip = area;
        }
        currentClip = clip;
        graphics.setClip(clip);
    }

    /**
     * 用任意形状与既有裁剪区域求交。
     *
     * @param shape 裁剪形状，不能为空；坐标单位：像素
     * @param rule  填充规则
     */
    @Override
    public void clip(Shape shape, FillRule rule) {
        if (shape == null) {
            throw new IllegalArgumentException("裁剪形状不能为空");
        }
        Area area = new Area(shape);
        if (rule == FillRule.EVEN_ODD) {
            area = new Area(toEvenOddPath(shape));
        }
        if (currentClip != null) {
            area.intersect(new Area(currentClip));
        }
        currentClip = area;
        graphics.setClip(area);
    }

    /**
     * 清除当前裁剪区域；不影响 save 栈中已保存的裁剪。
     */
    @Override
    public void clearClip() {
        currentClip = null;
        graphics.setClip(null);
    }

    /**
     * 用当前路径填充。
     *
     * @param style 绘制样式，不能为空
     * @param paint 填充着色源，不能为空
     * @param rule  填充规则
     */
    @Override
    public void fill(DrawStyle style, ResolvedPaint paint, FillRule rule) {
        path.setWindingRule(rule == FillRule.EVEN_ODD ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
        graphics.setComposite((AlphaComposite) AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, currentAlpha.getAlpha() * paint.getOpacity()))));
        graphics.setPaint((Paint) paint.getPaint().toPaint());
        graphics.fill(path);
        graphics.setComposite(currentAlpha);
    }

    /**
     * 用任意形状填充。
     *
     * @param shape 待填充形状，不能为空；坐标单位：像素
     * @param style 绘制样式，不能为空
     * @param paint 填充着色源，不能为空
     * @param rule  填充规则
     */
    @Override
    public void fill(Shape shape, DrawStyle style, ResolvedPaint paint, FillRule rule) {
        if (shape == null) {
            throw new IllegalArgumentException("填充形状不能为空");
        }
        Shape target = rule == FillRule.EVEN_ODD ? toEvenOddPath(shape) : shape;
        graphics.setComposite((AlphaComposite) AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, currentAlpha.getAlpha() * paint.getOpacity()))));
        graphics.setPaint((Paint) paint.getPaint().toPaint());
        graphics.fill(target);
        graphics.setComposite(currentAlpha);
    }

    /**
     * 用当前路径描边。
     *
     * @param style 绘制样式，不能为空
     * @param paint 描边着色源，不能为空
     */
    @Override
    public void stroke(DrawStyle style, ResolvedPaint paint) {
        graphics.setStroke(style.toStroke());
        graphics.setComposite((AlphaComposite) AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, currentAlpha.getAlpha() * paint.getOpacity()))));
        graphics.setPaint((Paint) paint.getPaint().toPaint());
        graphics.draw(path);
        graphics.setComposite(currentAlpha);
    }

    /**
     * 用任意形状描边。
     *
     * @param shape 待描边形状，不能为空；坐标单位：像素
     * @param style 绘制样式，不能为空
     * @param paint 描边着色源，不能为空
     */
    @Override
    public void stroke(Shape shape, DrawStyle style, ResolvedPaint paint) {
        if (shape == null) {
            throw new IllegalArgumentException("描边形状不能为空");
        }
        graphics.setStroke(style.toStroke());
        graphics.setComposite((AlphaComposite) AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, currentAlpha.getAlpha() * paint.getOpacity()))));
        graphics.setPaint((Paint) paint.getPaint().toPaint());
        graphics.draw(shape);
        graphics.setComposite(currentAlpha);
    }

    /**
     * 把任意形状转换为奇偶填充规则的路径副本。
     *
     * @param shape 原始形状
     * @return 奇偶规则的路径副本
     */
    private static Path2D toEvenOddPath(Shape shape) {
        Path2D path = new Path2D.Double(shape, new AffineTransform());
        path.setWindingRule(Path2D.WIND_EVEN_ODD);
        return path;
    }

    /**
     * 注册图片。
     *
     * @param image 图片像素数据，不能为空
     * @return 图片句柄
     */
    @Override
    public ImageHandle createImage(BufferedImage image) {
        return new ImageHandle(image == null ? -1 : System.identityHashCode(image), image);
    }

    /**
     * 按指定变换绘制图片。
     * <p>
     * 混合模式：离屏后端且图片完全不透明时，通过 {@link Graphics2DBlendComposite#blend(BufferedImage, BufferedImage)}
     * 在离屏图层上逐像素合成 {@link BlendMode#MULTIPLY} 等 PDF 标准混合；
     * 外部 {@link Graphics2D} 后端或图片带透明度时退化为普通覆盖并告警。
     * <p>
     * 蒙版：当前忽略 {@code mask} 参数，留待后续阶段实现。
     *
     * @param image     图片句柄，不能为空
     * @param transform 图片到当前坐标系的变换，不能为空
     * @param alpha     图片不透明度，取值 0.0–1.0，无量纲
     * @param smoothing true 表示插值平滑（双线性），false 表示最近邻
     * @param mask      透明度蒙版，可为 null
     * @param blend     混合模式，不能为空
     */
    @Override
    public void drawImage(ImageHandle image, AffineTransform transform, float alpha, boolean smoothing,
                          ImageHandle mask, BlendMode blend) {
        if (image == null || transform == null || blend == null) {
            throw new IllegalArgumentException("图片、变换与混合模式均不能为空");
        }
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                smoothing ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        float effectiveAlpha = Math.max(0f, Math.min(1f, currentAlpha.getAlpha() * alpha));
        boolean needsBlend = blend != BlendMode.NORMAL && effectiveAlpha == 1f;
        if (needsBlend && !ownsGraphics) {
            // 外部 Graphics2D 后端（如 SVGGraphics2D、PdfGraphics2D）不支持自定义混合，退化为普通覆盖
            logger.warn("外部 Graphics2D 后端不支持混合模式 {}，退化为普通覆盖：{}", blend, image);
            needsBlend = false;
        } else if (blend != BlendMode.NORMAL && effectiveAlpha < 1f) {
            logger.warn("图片透明度不为 1 时暂不支持混合模式 {}，退化为普通覆盖：{}", blend, image);
        }
        if (!needsBlend) {
            Composite old = graphics.getComposite();
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, effectiveAlpha));
            graphics.drawImage(image.getImage(), transform, null);
            graphics.setComposite(old);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            return;
        }
        /*
         * AWT 各平台管线对自定义 Composite 的处理不一致（部分管线会把结果写入临时画布），
         * 因此混合走离屏合成：先把图片按变换画到透明图层，再按混合函数逐像素合并到页面位图。
         */
        BufferedImage layer = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_ARGB);
        Graphics2D layerGraphics = layer.createGraphics();
        try {
            layerGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    smoothing ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            layerGraphics.drawImage(image.getImage(), transform, null);
        } finally {
            layerGraphics.dispose();
        }
        try {
            if (output != null) {
                new Graphics2DBlendComposite(blend).blend(output, layer);
            } else {
                // 外部上下文后端在此分支不可达（上面已退化），保险起见按普通覆盖绘制
                graphics.drawImage(layer, 0, 0, null);
            }
        } finally {
            layer.flush();
        }
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    /**
     * 以透明色清空矩形区域。
     *
     * @param x      左上角 X 坐标，单位：像素
     * @param y      左上角 Y 坐标，单位：像素
     * @param width  宽度，单位：像素
     * @param height 高度，单位：像素
     */
    @Override
    public void clear(double x, double y, double width, double height) {
        Composite old = graphics.getComposite();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(new Color(0, 0, 0, 0));
        graphics.fill(new java.awt.geom.Rectangle2D.Double(x, y, width, height));
        graphics.setComposite(old);
    }

    /**
     * 释放渲染器占用的位图与图形上下文；外部传入的 {@link Graphics2D} 不会被关闭。
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (ownsGraphics && graphics != null) {
            graphics.dispose();
            graphics = null;
        }
        if (output != null) {
            output.flush();
        }
    }
}
