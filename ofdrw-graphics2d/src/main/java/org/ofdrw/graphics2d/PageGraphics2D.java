package org.ofdrw.graphics2d;

import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.pageObj.Content;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.pageDescription.CT_GraphicUnit;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;
import org.ofdrw.pkg.container.PageDir;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
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
     * 描边参数
     */
    private BasicStroke stroke;
    /**
     * 绘制参数
     */
    private Paint paint;


    /**
     * 创建2D图形对象
     *
     * @param doc     文档上下文
     * @param pageDir 页面目录
     * @param pageObj 页面对象
     */
    PageGraphics2D(GraphicsDocument doc, PageDir pageDir, Page pageObj) {
        this.doc = doc;
        this.pageDir = pageDir;
        this.pageObj = pageObj;

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
     * 设置绘制参数
     *
     * @param gUnit 图元
     */
    private void setDrawParameters(CT_GraphicUnit<?> gUnit) {

        // 设置路径描边参数
        if (stroke != null) {
            // 线条连接样式
            switch (stroke.getLineJoin()) {
                case BasicStroke.JOIN_BEVEL:
                    gUnit.setJoin(LineJoinType.Bevel);
                    break;
                case BasicStroke.JOIN_MITER:
                    gUnit.setJoin(LineJoinType.Miter);
                    break;
                case BasicStroke.JOIN_ROUND:
                    gUnit.setJoin(LineJoinType.Round);
                    break;
                default:
                    gUnit.setJoin(null);
                    break;
            }

            // 线宽度
            if (stroke.getLineWidth() > 0) {
                gUnit.setLineWidth((double) stroke.getLineWidth());
            } else {
                gUnit.setLineWidth(0.353);
            }

            // 线条虚线重复样式
            final float[] dashArray = stroke.getDashArray();
            if (dashArray != null && dashArray.length > 0) {
                final ST_Array pattern = new ST_Array();
                for (float v : dashArray) {
                    pattern.add(Float.toString(v));
                }
                gUnit.setDashPattern(pattern);
            } else {
                gUnit.setDashPattern(null);
            }

            // 线端点样式
            switch (stroke.getEndCap()) {
                case BasicStroke.CAP_BUTT:
                    gUnit.setCap(LineCapType.Butt);
                    break;
                case BasicStroke.CAP_ROUND:
                    gUnit.setCap(LineCapType.Round);
                    break;
                case BasicStroke.CAP_SQUARE:
                    gUnit.setCap(LineCapType.Square);
                    break;
                default:
                    gUnit.setCap(null);
            }

            // Join的截断值
            final float miterLimit = stroke.getMiterLimit();
            if (miterLimit > 0) {
                gUnit.setMiterLimit((double) miterLimit);
            } else {
                gUnit.setMiterLimit(null);
            }
        }

        if (paint != null) {
            // TODO 完成绘制参数设置
        }
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
        setDrawParameters(ctPath);
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
     * @param img      the specified image to be drawn. This method does
     *                 nothing if <code>img</code> is null.
     * @param x        the <i>x</i> coordinate.
     * @param y        the <i>y</i> coordinate.
     * @param observer object to be notified as more of
     *                 the image is converted.
     * @return
     */
    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return false;
    }

    /**
     * @param img      the specified image to be drawn. This method does
     *                 nothing if <code>img</code> is null.
     * @param x        the <i>x</i> coordinate.
     * @param y        the <i>y</i> coordinate.
     * @param width    the width of the rectangle.
     * @param height   the height of the rectangle.
     * @param observer object to be notified as more of
     *                 the image is converted.
     * @return
     */
    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return false;
    }

    /**
     * @param img      the specified image to be drawn. This method does
     *                 nothing if <code>img</code> is null.
     * @param x        the <i>x</i> coordinate.
     * @param y        the <i>y</i> coordinate.
     * @param bgcolor  the background color to paint under the
     *                 non-opaque portions of the image.
     * @param observer object to be notified as more of
     *                 the image is converted.
     * @return
     */
    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return false;
    }

    /**
     * @param img      the specified image to be drawn. This method does
     *                 nothing if <code>img</code> is null.
     * @param x        the <i>x</i> coordinate.
     * @param y        the <i>y</i> coordinate.
     * @param width    the width of the rectangle.
     * @param height   the height of the rectangle.
     * @param bgcolor  the background color to paint under the
     *                 non-opaque portions of the image.
     * @param observer object to be notified as more of
     *                 the image is converted.
     * @return
     */
    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        return false;
    }

    /**
     * @param img      the specified image to be drawn. This method does
     *                 nothing if <code>img</code> is null.
     * @param dx1      the <i>x</i> coordinate of the first corner of the
     *                 destination rectangle.
     * @param dy1      the <i>y</i> coordinate of the first corner of the
     *                 destination rectangle.
     * @param dx2      the <i>x</i> coordinate of the second corner of the
     *                 destination rectangle.
     * @param dy2      the <i>y</i> coordinate of the second corner of the
     *                 destination rectangle.
     * @param sx1      the <i>x</i> coordinate of the first corner of the
     *                 source rectangle.
     * @param sy1      the <i>y</i> coordinate of the first corner of the
     *                 source rectangle.
     * @param sx2      the <i>x</i> coordinate of the second corner of the
     *                 source rectangle.
     * @param sy2      the <i>y</i> coordinate of the second corner of the
     *                 source rectangle.
     * @param observer object to be notified as more of the image is
     *                 scaled and converted.
     * @return
     */
    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        return false;
    }

    /**
     * @param img      the specified image to be drawn. This method does
     *                 nothing if <code>img</code> is null.
     * @param dx1      the <i>x</i> coordinate of the first corner of the
     *                 destination rectangle.
     * @param dy1      the <i>y</i> coordinate of the first corner of the
     *                 destination rectangle.
     * @param dx2      the <i>x</i> coordinate of the second corner of the
     *                 destination rectangle.
     * @param dy2      the <i>y</i> coordinate of the second corner of the
     *                 destination rectangle.
     * @param sx1      the <i>x</i> coordinate of the first corner of the
     *                 source rectangle.
     * @param sy1      the <i>y</i> coordinate of the first corner of the
     *                 source rectangle.
     * @param sx2      the <i>x</i> coordinate of the second corner of the
     *                 source rectangle.
     * @param sy2      the <i>y</i> coordinate of the second corner of the
     *                 source rectangle.
     * @param bgcolor  the background color to paint under the
     *                 non-opaque portions of the image.
     * @param observer object to be notified as more of the image is
     *                 scaled and converted.
     * @return
     */
    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        return false;
    }

    /**
     *
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
     * @param paint the <code>Paint</code> object to be used to generate
     *              color during the rendering process, or <code>null</code>
     */
    @Override
    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    /**
     * @param s the <code>Stroke</code> object to be used to stroke a
     *          <code>Shape</code> during the rendering process
     */
    @Override
    public void setStroke(Stroke s) {
        if (s == null) {
            this.stroke = null;
            return;
        }
        if (s instanceof BasicStroke) {
            this.stroke = (BasicStroke) s;
        }
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
     * @return
     */
    @Override
    public Graphics create() {
        return null;
    }

    /**
     * @param x the specified x coordinate
     * @param y the specified y coordinate
     */
    @Override
    public void translate(int x, int y) {

    }

    /**
     * @return
     */
    @Override
    public Color getColor() {
        return null;
    }

    /**
     * @param c the new rendering color.
     */
    @Override
    public void setColor(Color c) {

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
     * @param clip the <code>Shape</code> to use to set the clip
     */
    @Override
    public void setClip(Shape clip) {

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
     * @param x      the <i>x</i> coordinate
     *               of the rectangle to be filled.
     * @param y      the <i>y</i> coordinate
     *               of the rectangle to be filled.
     * @param width  the width of the rectangle to be filled.
     * @param height the height of the rectangle to be filled.
     */
    @Override
    public void fillRect(int x, int y, int width, int height) {

    }

    /**
     * @param x      the <i>x</i> coordinate of the rectangle to clear.
     * @param y      the <i>y</i> coordinate of the rectangle to clear.
     * @param width  the width of the rectangle to clear.
     * @param height the height of the rectangle to clear.
     */
    @Override
    public void clearRect(int x, int y, int width, int height) {

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
        final RoundRectangle2D shape = Shapes.roundRect(x, y, width, height, arcWidth, arcHeight);
        draw(shape);
    }

    /**
     * @param x         the <i>x</i> coordinate of the rectangle to be filled.
     * @param y         the <i>y</i> coordinate of the rectangle to be filled.
     * @param width     the width of the rectangle to be filled.
     * @param height    the height of the rectangle to be filled.
     * @param arcWidth  the horizontal diameter
     *                  of the arc at the four corners.
     * @param arcHeight the vertical diameter
     *                  of the arc at the four corners.
     */
    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {

    }

    /**
     * @param x      the <i>x</i> coordinate of the upper left
     *               corner of the oval to be drawn.
     * @param y      the <i>y</i> coordinate of the upper left
     *               corner of the oval to be drawn.
     * @param width  the width of the oval to be drawn.
     * @param height the height of the oval to be drawn.
     */
    @Override
    public void drawOval(int x, int y, int width, int height) {

    }

    /**
     * @param x      the <i>x</i> coordinate of the upper left corner
     *               of the oval to be filled.
     * @param y      the <i>y</i> coordinate of the upper left corner
     *               of the oval to be filled.
     * @param width  the width of the oval to be filled.
     * @param height the height of the oval to be filled.
     */
    @Override
    public void fillOval(int x, int y, int width, int height) {

    }

    /**
     * @param x          the <i>x</i> coordinate of the
     *                   upper-left corner of the arc to be drawn.
     * @param y          the <i>y</i>  coordinate of the
     *                   upper-left corner of the arc to be drawn.
     * @param width      the width of the arc to be drawn.
     * @param height     the height of the arc to be drawn.
     * @param startAngle the beginning angle.
     * @param arcAngle   the angular extent of the arc,
     *                   relative to the start angle.
     */
    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {

    }

    /**
     * @param x          the <i>x</i> coordinate of the
     *                   upper-left corner of the arc to be filled.
     * @param y          the <i>y</i>  coordinate of the
     *                   upper-left corner of the arc to be filled.
     * @param width      the width of the arc to be filled.
     * @param height     the height of the arc to be filled.
     * @param startAngle the beginning angle.
     * @param arcAngle   the angular extent of the arc,
     *                   relative to the start angle.
     */
    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {

    }

    /**
     * @param xPoints an array of <i>x</i> points
     * @param yPoints an array of <i>y</i> points
     * @param nPoints the total number of points
     */
    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {

    }

    /**
     * @param xPoints a an array of <code>x</code> coordinates.
     * @param yPoints a an array of <code>y</code> coordinates.
     * @param nPoints a the total number of points.
     */
    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {

    }

    /**
     * @param xPoints a an array of <code>x</code> coordinates.
     * @param yPoints a an array of <code>y</code> coordinates.
     * @param nPoints a the total number of points.
     */
    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {

    }

    /**
     * @param tx the distance to translate along the x-axis
     * @param ty the distance to translate along the y-axis
     */
    @Override
    public void translate(double tx, double ty) {

    }

    /**
     * @param theta the angle of rotation in radians
     */
    @Override
    public void rotate(double theta) {

    }

    /**
     * @param theta the angle of rotation in radians
     * @param x     the x coordinate of the origin of the rotation
     * @param y     the y coordinate of the origin of the rotation
     */
    @Override
    public void rotate(double theta, double x, double y) {

    }

    /**
     * @param sx the amount by which X coordinates in subsequent
     *           rendering operations are multiplied relative to previous
     *           rendering operations.
     * @param sy the amount by which Y coordinates in subsequent
     *           rendering operations are multiplied relative to previous
     *           rendering operations.
     */
    @Override
    public void scale(double sx, double sy) {

    }

    /**
     * @param shx the multiplier by which coordinates are shifted in
     *            the positive X axis direction as a function of their Y coordinate
     * @param shy the multiplier by which coordinates are shifted in
     *            the positive Y axis direction as a function of their X coordinate
     */
    @Override
    public void shear(double shx, double shy) {

    }

    /**
     * @param Tx the <code>AffineTransform</code> object to be composed with
     *           the current <code>Transform</code>
     */
    @Override
    public void transform(AffineTransform Tx) {

    }

    /**
     * @param Tx the <code>AffineTransform</code> that was retrieved
     *           from the <code>getTransform</code> method
     */
    @Override
    public void setTransform(AffineTransform Tx) {

    }

    /**
     * @return
     */
    @Override
    public AffineTransform getTransform() {
        return null;
    }

    /**
     * 获取绘制参数
     *
     * @return 绘制参数
     */
    @Override
    public Paint getPaint() {
        return paint;
    }

    /**
     * @return
     */
    @Override
    public Composite getComposite() {
        return null;
    }

    /**
     * @param color the background color that is used in
     *              subsequent calls to <code>clearRect</code>
     */
    @Override
    public void setBackground(Color color) {

    }

    /**
     * @return
     */
    @Override
    public Color getBackground() {
        return null;
    }

    /**
     * 获取描边属性
     *
     * @return 描述属性 {@link BasicStroke}
     */
    @Override
    public Stroke getStroke() {
        return stroke;
    }

    /**
     * @param s the <code>Shape</code> to be intersected with the current
     *          <code>Clip</code>.  If <code>s</code> is <code>null</code>,
     *          this method clears the current <code>Clip</code>.
     */
    @Override
    public void clip(Shape s) {

    }

    /**
     * @return
     */
    @Override
    public FontRenderContext getFontRenderContext() {
        return null;
    }


}
