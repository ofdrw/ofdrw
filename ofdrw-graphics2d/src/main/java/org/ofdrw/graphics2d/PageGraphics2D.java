package org.ofdrw.graphics2d;

import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.pageObj.Content;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.pkg.container.PageDir;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * OFD 2D图形，实现AWT Graphics2D API。
 * 通过图形绘制生成OFD。
 * <p>
 * 在 PageGraphics2D 中所有接口参数单位均为 毫米(mm)。
 *
 * @author 权观宇
 * @since 2023-01-18 10:07:52
 */
public class PageGraphics2D extends Graphics2D {
    /**
     * 文档上下文
     */
    private final GraphicsDocument doc;

    /**
     * 所属页面容器
     */
    private final PageDir pageDir;

    /**
     * 页面对象
     */
    private final Page pageObj;

    /**
     * 页面内容容器
     */
    private final CT_PageBlock container;


    /**
     * 绘制属性
     * <p>
     * 对 paint 与 stroke 都会反映到 DrawParam
     * <p>
     * stroke、fill 或 drawString 时，如果DrawParam与上次的不一样，则添加
     */
    private final DrawParam drawParam;

    /**
     * 绘制空间大小
     */
    private final ST_Box size;

    /**
     * 创建2D图形对象
     *
     * @param doc     文档上下文
     * @param pageDir 页面目录
     * @param pageObj 页面对象
     * @param box     绘制空间大小
     */
    PageGraphics2D(GraphicsDocument doc, PageDir pageDir, Page pageObj, ST_Box box) {
        this.doc = doc;
        this.pageDir = pageDir;
        this.pageObj = pageObj;
        this.size = box;
        this.drawParam = new DrawParam(doc);

        // 页面内容
        final Content content = new Content();
        pageObj.setContent(content);

        // 页层
        CT_Layer ctlayer = new CT_Layer();
        ctlayer.setType(Type.Body);
        ctlayer.setObjID(doc.newID());
        // 添加页面内容
        content.addLayer(ctlayer);

        // 创建容器
        container = new CT_PageBlock();
        container.setObjID(doc.newID());
        // 添加到页面层
        ctlayer.addPageBlock(container);
    }

    /**
     * 复制当前绘制上下文，并创建新的绘制上下文
     *
     * @param parent 复制对象
     */
    private PageGraphics2D(PageGraphics2D parent) {
        this.doc = parent.doc;
        this.pageDir = parent.pageDir;
        this.pageObj = parent.pageObj;
        this.container = parent.container;
        this.size = parent.size.clone();
        this.drawParam = new DrawParam(parent.drawParam);
    }


    /**
     * 绘制图形
     *
     * @param s 图形
     */
    @Override
    public void draw(Shape s) {
        if (s == null) {
            return;
        }
        // 转换图形对象为OFD路径
        final AbbreviatedData pData = Shapes.path(s);
        // 创建路径对象并设置上下文参数
        CT_Path pathObj = newPathWithCtx();
        pathObj.setStroke(true);
        pathObj.setAbbreviatedData(pData);
        container.addPageBlock(pathObj.toObj(doc.newID()));
    }

    /**
     * 创建OFD路径对象
     * <p>
     * 并设置上下文相关参数
     *
     * @return 路径对象
     */
    private CT_Path newPathWithCtx() {
        final CT_Path ctPath = new CT_Path();
        CT_PageArea area = pageObj.getArea();
        if (area == null) {
            area = doc.cdata.getPageArea();
        }
        if (area == null) {
            throw new IllegalArgumentException("请设置页面大小");
        }
        ST_Box box = area.getBox();
        if (box == null) {
            throw new IllegalArgumentException("请设置页面大小");
        }
        // 设置路径的区域
        ctPath.setBoundary(box);
        // 设置绘制参数
        this.drawParam.apply(ctPath);
        return ctPath;
    }

    /**
     * @param img   the specified image to be rendered.
     *              This method does nothing if <code>img</code> is null.
     * @param xform the transformation from image space into user space
     * @param obs   the {@link ImageObserver}
     *              to be notified as more of the <code>Image</code>
     *              is converted
     * @return
     */
    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return false;
    }

    /**
     * @param img the specified <code>BufferedImage</code> to be rendered.
     *            This method does nothing if <code>img</code> is null.
     * @param op  the filter to be applied to the image before rendering
     * @param x   the x coordinate of the location in user space where
     *            the upper left corner of the image is rendered
     * @param y   the y coordinate of the location in user space where
     *            the upper left corner of the image is rendered
     */
    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {

    }

    /**
     * @param img   the image to be rendered. This method does
     *              nothing if <code>img</code> is null.
     * @param xform the transformation from image space into user space
     */
    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {

    }

    /**
     * @param img   the image to be rendered. This method does
     *              nothing if <code>img</code> is null.
     * @param xform the transformation from image space into user space
     */
    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {

    }

    /**
     * @param str the string to be rendered
     * @param x   the x coordinate of the location where the
     *            <code>String</code> should be rendered
     * @param y   the y coordinate of the location where the
     *            <code>String</code> should be rendered
     */
    @Override
    public void drawString(String str, int x, int y) {

    }

    /**
     * @param str the <code>String</code> to be rendered
     * @param x   the x coordinate of the location where the
     *            <code>String</code> should be rendered
     * @param y   the y coordinate of the location where the
     *            <code>String</code> should be rendered
     */
    @Override
    public void drawString(String str, float x, float y) {

    }

    /**
     * @param iterator the iterator whose text is to be rendered
     * @param x        the x coordinate where the iterator's text is to be
     *                 rendered
     * @param y        the y coordinate where the iterator's text is to be
     *                 rendered
     */
    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {

    }

    /**
     * 在指定坐标位置上绘制图片
     * <p>
     * 绘制的图片大小为图元原始大小
     *
     * @param img      待绘制图片
     * @param x        图片左上角 X坐标
     * @param y        图片左上角 Y坐标
     * @param observer 不使用
     * @return 固定值 true
     */
    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        if (img == null) {
            return true;
        }
        int w = img.getWidth(observer);
        if (w < 0) {
            return false;
        }
        int h = img.getHeight(observer);
        if (h < 0) {
            return false;
        }
        return drawImage(img, x, y, w, h, observer);
    }

    /**
     * 将图片绘制于指定矩形区域内
     *
     * @param img      待绘制的图片
     * @param x        矩形左上角 X坐标
     * @param y        矩形左上角 Y坐标
     * @param width    矩形宽度
     * @param height   矩形高度
     * @param observer 忽略
     * @return 固定值 true
     */
    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        if (img == null) {
            return true;
        }

        ST_ID objId = this.doc.addResImg(img);
        ImageObject imgObj = new ImageObject(doc.newID());
        imgObj.setResourceID(objId.ref());
        imgObj.setBoundary(this.size.clone());
        ST_Array ctm = new ST_Array(width, 0, 0, height, x, y).mtxMul(this.drawParam.ctm);
        imgObj.setCTM(ctm);
        // 透明度
        if (this.drawParam.gColor instanceof Color) {
            imgObj.setAlpha(((Color) this.drawParam.gColor).getAlpha());
        }
        container.addPageBlock(imgObj);
        return true;
    }

    /**
     * 在指定位置绘制图片
     * <p>
     * 图片保持原有大小，图片的透明部分将会使用指定颜色填充
     *
     * @param img      待绘制的图片
     * @param x        矩形左上角 X坐标
     * @param y        矩形左上角 Y坐标
     * @param bgcolor  背景颜色，用于填充图片透明部分
     * @param observer 忽略
     * @return 固定值 true
     */
    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        if (img == null) {
            return true;
        }
        int w = img.getWidth(null);
        if (w < 0) {
            return false;
        }
        int h = img.getHeight(null);
        if (h < 0) {
            return false;
        }
        return drawImage(img, x, y, w, h, bgcolor, observer);
    }

    /**
     * 在指定矩形区域内绘制图片
     * <p>
     * 图片将伸缩至矩形区域大小，图片的透明部分将会使用指定颜色填充
     *
     * @param img      待绘制的图片
     * @param x        矩形左上角 X坐标
     * @param y        矩形左上角 Y坐标
     * @param width    矩形宽度
     * @param height   矩形高度
     * @param bgcolor  背景颜色，用于填充图片透明部分
     * @param observer 忽略
     * @return 固定值 true
     */
    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        Paint saved = getPaint();
        setPaint(bgcolor);
        fillRect(x, y, width, height);
        setPaint(saved);
        return drawImage(img, x, y, width, height, observer);
    }

    /**
     * 绘制图片内某个矩形区域 到 画布的某个指定矩形区域，图片将会缩放适应目标区域
     *
     * @param img      待绘制图片
     * @param dx1      画布内 矩形左上角 X坐标（目的坐标）
     * @param dy1      画布内 矩形左上角 Y坐标（目的坐标）
     * @param dx2      画布内 矩形右下角 X坐标（目的坐标）
     * @param dy2      画布内 矩形右下角 Y坐标（目的坐标）
     * @param sx1      图片内 矩形左上角 X坐标（源坐标）
     * @param sy1      图片内 矩形左上角 Y坐标（源坐标）
     * @param sx2      图片内 矩形右下角 X坐标（源坐标）
     * @param sy2      图片内 矩形右下角 Y坐标（源坐标）
     * @param observer 忽略
     * @return 固定值 true
     */
    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        int w = dx2 - dx1;
        int h = dy2 - dy1;
        BufferedImage img2 = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img2.createGraphics();
        g2.drawImage(img, 0, 0, w, h, sx1, sy1, sx2, sy2, null);
        return drawImage(img2, dx1, dy1, null);
    }

    /**
     * 绘制图片内某个矩形区域 到 画布的某个指定矩形区域
     * <p>
     * 图片将会缩放适应目标区域，图片的透明部分将会使用指定颜色填充
     *
     * @param img      待绘制图片
     * @param dx1      画布内 矩形左上角 X坐标（目的坐标）
     * @param dy1      画布内 矩形左上角 Y坐标（目的坐标）
     * @param dx2      画布内 矩形右下角 X坐标（目的坐标）
     * @param dy2      画布内 矩形右下角 Y坐标（目的坐标）
     * @param sx1      图片内 矩形左上角 X坐标（源坐标）
     * @param sy1      图片内 矩形左上角 Y坐标（源坐标）
     * @param sx2      图片内 矩形右下角 X坐标（源坐标）
     * @param sy2      图片内 矩形右下角 Y坐标（源坐标）
     * @param bgcolor  背景颜色，用于填充透明部分
     * @param observer 忽略
     * @return 固定值 true
     */
    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        Paint saved = getPaint();
        setPaint(bgcolor);
        fillRect(dx1, dy1, dx2 - dx1, dy2 - dy1);
        setPaint(saved);
        return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    /**
     * 销毁绘制上下文
     */
    @Override
    public void dispose() {

    }

    /**
     * @param iterator the iterator whose text is to be rendered
     * @param x        the x coordinate where the iterator's text is to be
     *                 rendered
     * @param y        the y coordinate where the iterator's text is to be
     *                 rendered
     */
    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {

    }

    /**
     * @param g the <code>GlyphVector</code> to be rendered
     * @param x the x position in User Space where the glyphs should
     *          be rendered
     * @param y the y position in User Space where the glyphs should
     *          be rendered
     */
    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {

    }

    /**
     * 填充图形
     *
     * @param s Java图形
     */
    @Override
    public void fill(Shape s) {
        if (s == null) {
            return;
        }
        // 转换图形对象为OFD路径
        final AbbreviatedData pData = Shapes.path(s);
        // 创建路径对象并设置上下文参数
        CT_Path pathObj = newPathWithCtx();
        pathObj.setFill(true);
        pathObj.setAbbreviatedData(pData);
        container.addPageBlock(pathObj.toObj(doc.newID()));
    }

    /**
     * @param rect     the area in device space to check for a hit
     * @param s        the <code>Shape</code> to check for a hit
     * @param onStroke flag used to choose between testing the
     *                 stroked or the filled shape.  If the flag is <code>true</code>, the
     *                 <code>Stroke</code> outline is tested.  If the flag is
     *                 <code>false</code>, the filled <code>Shape</code> is tested.
     * @return
     */
    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return false;
    }

    /**
     * @return
     */
    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return null;
    }

    /**
     * @param comp the <code>Composite</code> object to be used for rendering
     */
    @Override
    public void setComposite(Composite comp) {

    }

    /**
     * 设置绘制参数
     *
     * @param paint 设置画笔颜色，用于填充和描边
     */
    @Override
    public void setPaint(Paint paint) {
        this.drawParam.setColor(paint);
    }

    /**
     * 设置描边属性
     *
     * @param s 描边属性参数
     */
    @Override
    public void setStroke(Stroke s) {
        this.drawParam.setStroke(s);
    }

    /**
     * @param hintKey   the key of the hint to be set.
     * @param hintValue the value indicating preferences for the specified
     *                  hint category.
     */
    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {

    }

    /**
     * @param hintKey the key corresponding to the hint to get.
     * @return
     */
    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return null;
    }

    /**
     * @param hints the rendering hints to be set
     */
    @Override
    public void setRenderingHints(Map<?, ?> hints) {

    }

    /**
     * @param hints the rendering hints to be set
     */
    @Override
    public void addRenderingHints(Map<?, ?> hints) {

    }

    /**
     * @return
     */
    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }

    /**
     * 复制当前绘制上下文为新的上下文
     *
     * @return 复制的绘制上下文对象
     */
    @Override
    public Graphics create() {
        return new PageGraphics2D(this);
    }

    /**
     * 返回前景色
     *
     * @return 颜色，可能为null
     */
    @Override
    public Color getColor() {
        return this.drawParam.gForeground;
    }

    /**
     * 设置前景色
     *
     * @param c 颜色
     */
    @Override
    public void setColor(Color c) {
        if (c == null) {
            return;
        }
        this.drawParam.setForeground(c);
    }

    /**
     *
     */
    @Override
    public void setPaintMode() {

    }

    /**
     * @param c1 the XOR alternation color
     */
    @Override
    public void setXORMode(Color c1) {

    }

    /**
     * @return
     */
    @Override
    public Font getFont() {
        return null;
    }

    /**
     * @param font the font.
     */
    @Override
    public void setFont(Font font) {

    }

    /**
     * @param f the specified font
     * @return
     */
    @Override
    public FontMetrics getFontMetrics(Font f) {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Rectangle getClipBounds() {
        return null;
    }

    /**
     * @param x      the x coordinate of the rectangle to intersect the clip with
     * @param y      the y coordinate of the rectangle to intersect the clip with
     * @param width  the width of the rectangle to intersect the clip with
     * @param height the height of the rectangle to intersect the clip with
     */
    @Override
    public void clipRect(int x, int y, int width, int height) {

    }

    /**
     * @param x      the <i>x</i> coordinate of the new clip rectangle.
     * @param y      the <i>y</i> coordinate of the new clip rectangle.
     * @param width  the width of the new clip rectangle.
     * @param height the height of the new clip rectangle.
     */
    @Override
    public void setClip(int x, int y, int width, int height) {

    }

    /**
     * @return
     */
    @Override
    public Shape getClip() {
        return null;
    }

    /**
     * 设置裁剪区域
     *
     * @param clip 裁剪区域
     */
    @Override
    public void setClip(Shape clip) {
        this.drawParam.gClip = clip;
    }

    /**
     * @param x      the <i>x</i> coordinate of the source rectangle.
     * @param y      the <i>y</i> coordinate of the source rectangle.
     * @param width  the width of the source rectangle.
     * @param height the height of the source rectangle.
     * @param dx     the horizontal distance to copy the pixels.
     * @param dy     the vertical distance to copy the pixels.
     */
    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {

    }

    /**
     * 画线
     *
     * @param x1 起点X
     * @param y1 起点Y
     * @param x2 终点X
     * @param y2 终点Y
     */
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        draw(new Line2D.Double(x1, y1, x2, y2));
    }

    /**
     * 填充矩形区域
     *
     * @param x      矩形区域左上角坐标X
     * @param y      矩形区域左上角坐标Y
     * @param width  矩形宽度
     * @param height 矩形高度
     */
    @Override
    public void fillRect(int x, int y, int width, int height) {
        fill(new Rectangle2D.Double(x, y, width, height));
    }

    /**
     * 描边矩形
     *
     * @param x      矩形区域左上角坐标X
     * @param y      矩形区域左上角坐标Y
     * @param width  矩形宽度
     * @param height 矩形高度
     */
    @Override
    public void drawRect(int x, int y, int width, int height) {
        if ((width <= 0) || (height <= 0)) {
            return;
        }
        draw(new Rectangle2D.Double(x, y, width, height));
    }

    /**
     * 使用背景色填充矩形区域
     *
     * @param x      填充区域矩形左上角 X 坐标
     * @param y      填充区域矩形左上角 X 坐标
     * @param width  矩形宽度
     * @param height 矩形高度
     */
    @Override
    public void clearRect(int x, int y, int width, int height) {
        if (this.drawParam.gBackground == null) {
            return;
        }
        Paint saved = getPaint();
        setPaint(this.drawParam.gBackground);
        fillRect(x, y, width, height);
        setPaint(saved);
    }

    /**
     * 绘制圆角矩形
     *
     * @param x         矩形左上角X坐标
     * @param y         矩形左上角Y坐标
     * @param width     矩形宽度
     * @param height    矩形高度
     * @param arcWidth  水平圆角半径
     * @param arcHeight 垂直圆角半径
     */
    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        draw(new RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight));
    }

    /**
     * 填充圆角矩形
     *
     * @param x         矩形左上角X坐标
     * @param y         矩形左上角Y坐标
     * @param width     矩形宽度
     * @param height    矩形高度
     * @param arcWidth  水平圆角半径
     * @param arcHeight 垂直圆角半径
     */
    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        fill(new RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight));
    }

    /**
     * 使用当前颜色在矩形区域内描边椭圆形
     *
     * @param x      矩形区域左上角 X 坐标
     * @param y      矩形区域左上角 Y 坐标
     * @param width  矩形区域宽度
     * @param height 矩形区域高度
     */
    @Override
    public void drawOval(int x, int y, int width, int height) {
        draw(new Ellipse2D.Double(x, y, width, height));
    }

    /**
     * 使用当前颜色在矩形区域内填充椭圆形
     *
     * @param x      矩形区域左上角 X 坐标
     * @param y      矩形区域左上角 Y 坐标
     * @param width  矩形区域宽度
     * @param height 矩形区域高度
     */
    @Override
    public void fillOval(int x, int y, int width, int height) {
        fill(new Ellipse2D.Double(x, y, width, height));
    }

    /**
     * 在矩形区域内绘制圆弧
     * <p>
     * 注意：0度位于时钟3点钟位置，正数角度表示顺时针旋转，负数为逆时针，圆形位于矩形中心。
     *
     * @param x          矩形区域左上角 X 坐标
     * @param y          矩形区域左上角 Y 坐标
     * @param width      矩形区域宽度
     * @param height     矩形区域高度
     * @param startAngle 圆弧开始角度
     * @param arcAngle   圆弧结束角度
     */
    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        draw(new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN));
    }

    /**
     * 在矩形区域内填充扇形
     * <p>
     * 注意：0度位于时钟3点钟位置，正数角度表示顺时针旋转，负数为逆时针，圆形位于矩形中心。
     * <p>
     * 圆弧起点和终点为圆心
     *
     * @param x          矩形区域左上角 X 坐标
     * @param y          矩形区域左上角 Y 坐标
     * @param width      矩形区域宽度
     * @param height     矩形区域高度
     * @param startAngle 圆弧开始角度
     * @param arcAngle   圆弧结束角度
     */
    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        fill(new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.PIE));
    }

    /**
     * 绘制折线
     * <p>
     * 注意绘制的折线是一个不闭合的图形
     *
     * @param xPoints 折点 X坐标 序列
     * @param yPoints 折点 Y坐标 序列
     * @param nPoints 折点总数
     */
    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        GeneralPath p = new GeneralPath();
        p.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < nPoints; i++) {
            p.lineTo(xPoints[i], yPoints[i]);
        }
        draw(p);
    }

    /**
     * 使用当前颜色和描边属性描边多边形
     *
     * @param xPoints 多边形 X坐标 序列
     * @param yPoints 多边形 Y坐标 序列
     * @param nPoints 多边形点数量
     */
    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        GeneralPath p = new GeneralPath();
        p.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < nPoints; i++) {
            p.lineTo(xPoints[i], yPoints[i]);
        }
        p.closePath();
        draw(p);
    }

    /**
     * 使用当前颜色填充多边形
     *
     * @param xPoints 多边形 X坐标 序列
     * @param yPoints 多边形 Y坐标 序列
     * @param nPoints 多边形点数量
     */
    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        GeneralPath p = new GeneralPath();
        p.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < nPoints; i++) {
            p.lineTo(xPoints[i], yPoints[i]);
        }
        p.closePath();
        fill(p);
    }

    /**
     * 平移原点坐标
     *
     * @param x x方向的平移距离
     * @param y y方向的平移距离
     */
    @Override
    public void translate(int x, int y) {
        translate((double) x, (double) y);
    }

    /**
     * 平移原点
     *
     * @param tx x方向的平移距离
     * @param ty y方向的平移距离
     */
    @Override
    public void translate(double tx, double ty) {
        ST_Array r = new ST_Array(
                1, 0,
                0, 1,
                tx, ty);
        this.drawParam.ctm = r.mtxMul(this.drawParam.ctm);
        this.drawParam.jCtm.translate(tx, ty);
    }

    /**
     * 绕原点旋转画布
     *
     * @param theta 旋转角度，计算方式为 {@code theta = angle * Math.PI / 180 }，负数表示逆时针。
     */
    @Override
    public void rotate(double theta) {
        ST_Array r = new ST_Array(
                Math.cos(theta), Math.sin(theta),
                -Math.sin(theta), Math.cos(theta),
                0, 0);
        this.drawParam.ctm = r.mtxMul(this.drawParam.ctm);
        this.drawParam.jCtm.rotate(theta);
    }

    /**
     * 绕指定点旋转画布
     * <p>
     * 等价于
     * <pre>
     *          translate(x, y);
     *          rotate(theta);
     *          translate(-x, -y);
     * </pre>
     *
     * @param theta 旋转角度，计算方式为 {@code theta = angle * Math.PI / 180 }，负数表示逆时针。
     * @param x     旋转点X坐标
     * @param y     旋转点Y坐标
     */
    @Override
    public void rotate(double theta, double x, double y) {
        translate(x, y);
        rotate(theta);
        translate(-x, -y);
    }

    /**
     * 坐标缩放
     *
     * @param sx 缩放当前绘图的宽度 (1=100%, 0.5=50%, 2=200%, 依次类推)
     * @param sy 缩放当前绘图的高度 (1=100%, 0.5=50%, 2=200%, 依次类推)
     */
    @Override
    public void scale(double sx, double sy) {
        ST_Array scale = new ST_Array(
                sx, 0,
                0, sy,
                0, 0);
        this.drawParam.ctm = scale.mtxMul(this.drawParam.ctm);
        this.drawParam.jCtm.scale(sx, sy);
    }

    /**
     * 切变
     *
     * @param shx X方向切变角度
     * @param shy Y方向切变角度
     */
    @Override
    public void shear(double shx, double shy) {
        ST_Array shear = new ST_Array(
                1, Math.tan(shx),
                Math.tan(shy), 1,
                0, 0);
        this.drawParam.ctm = shear.mtxMul(this.drawParam.ctm);
        this.drawParam.jCtm.shear(shx, shy);
    }

    /**
     * 图形变换
     *
     * @param tx 变换矩阵
     */
    @Override
    public void transform(AffineTransform tx) {
        if (tx == null) {
            throw new IllegalArgumentException("变换矩阵为空");
        }
        ST_Array ctm = trans(tx);
        this.drawParam.ctm = ctm.mtxMul(this.drawParam.ctm);
        this.drawParam.jCtm.concatenate(tx);
    }

    /**
     * 设置变换矩阵
     *
     * @param tx 变换矩阵
     */
    @Override
    public void setTransform(AffineTransform tx) {
        if (tx == null) {
            throw new IllegalArgumentException("变换矩阵为空");
        }
        this.drawParam.ctm = trans(tx);
    }

    /**
     * 返回当前的变换矩阵
     *
     * @return 变换矩阵
     */
    @Override
    public AffineTransform getTransform() {
        return new AffineTransform(this.drawParam.jCtm);
    }

    /**
     * 获取绘制参数
     *
     * @return 绘制参数
     */
    @Override
    public Paint getPaint() {
        return this.drawParam.gColor;
    }

    /**
     * @return
     */
    @Override
    public Composite getComposite() {
        return null;
    }

    /**
     * 设置背景颜色
     *
     * @param color 该颜色将用于 <code>clearRect</code> 清空区域
     */
    @Override
    public void setBackground(Color color) {
        this.drawParam.gBackground = color;
    }

    /**
     * 获取背景颜色
     *
     * @return 背景颜色
     */
    @Override
    public Color getBackground() {
        return this.drawParam.gBackground;
    }

    /**
     * 获取描边属性
     *
     * @return 描述属性 {@link BasicStroke}
     */
    @Override
    public Stroke getStroke() {
        return this.drawParam.gStroke;
    }

    /**
     * 设置裁剪区域
     *
     * @param s 裁剪区域，如果为 null 则清除裁剪区域
     */
    @Override
    public void clip(Shape s) {
        this.drawParam.gClip = s;
    }

    /**
     * @return
     */
    @Override
    public FontRenderContext getFontRenderContext() {
        return null;
    }


    /**
     * 转为AWT变换矩阵 {@link AffineTransform} 为 OFD 类型变换矩阵{@link ST_Array}
     *
     * @param tx AWT变换矩阵
     * @return OFD ST_Array
     */
    public ST_Array trans(AffineTransform tx) {
      /*
      m00 m10 0    a b 0
      m01 m11 0  = c d 0
      m02 m12 1    e f 1
       */
        return new ST_Array(
                tx.getScaleX(), tx.getShearY(),
                tx.getShearX(), tx.getScaleY(),
                tx.getTranslateX(), tx.getTranslateY()
        );
    }
}
