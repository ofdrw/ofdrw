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
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Hashtable;
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
public class OFDPageGraphics2D extends Graphics2D {
    /**
     * 文档上下文
     */
    private final OFDGraphicsDocument doc;

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
    private final OFDGraphics2DDrawParam OFDGraphics2DDrawParam;

    /**
     * 绘制空间大小
     */
    private final ST_Box size;

    /**
     * 设备配置对象
     * <p>
     * 用于兼容AWT接口
     */
    private OFDPageGraphicsConfiguration devConfig;

    /**
     * 用于创建合适的字体规格
     */
    private final Graphics2D fmg;

    /**
     * 创建2D图形对象
     *
     * @param doc     文档上下文
     * @param pageDir 页面目录
     * @param pageObj 页面对象
     * @param box     绘制空间大小
     */
    OFDPageGraphics2D(OFDGraphicsDocument doc, PageDir pageDir, Page pageObj, ST_Box box) {

        BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        fmg = bi.createGraphics();

        this.doc = doc;
        this.pageDir = pageDir;
        this.pageObj = pageObj;
        this.size = box;
        this.OFDGraphics2DDrawParam = new OFDGraphics2DDrawParam(doc);

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
    private OFDPageGraphics2D(OFDPageGraphics2D parent) {
        this.doc = parent.doc;
        this.pageDir = parent.pageDir;
        this.pageObj = parent.pageObj;
        this.container = parent.container;
        this.size = parent.size.clone();
        this.OFDGraphics2DDrawParam = parent.OFDGraphics2DDrawParam.clone();
        this.devConfig = parent.devConfig;
        this.fmg = parent.fmg;
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
        final AbbreviatedData pData = OFDShapes.path(s);
        if (pData.size() == 0) {
            // 没有绘制参数时不填充
            return;
        }
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
        this.OFDGraphics2DDrawParam.apply(ctPath);
        return ctPath;
    }


    /**
     * 使用当前的 字体(Font) 以及 画笔参数(Paint) 在指定位置上绘制文字
     * <p>
     * 文字将转换为图形路径绘制填充在OFD页面上
     * <p>
     * 第一个文字的基线坐标为传入的(x,y)参数位置。
     * <p>
     * 文字绘制的将被裁剪矩阵(clip)、变换矩阵影响。
     *
     * @param str 待绘制文字序列
     * @param x   首个文字基线X坐标
     * @param y   首个文字基线Y坐标
     */
    @Override
    public void drawString(String str, int x, int y) {
        GlyphVector gv = getFont().createGlyphVector(getFontRenderContext(), str);
        drawGlyphVector(gv, x, y);
    }

    /**
     * 使用当前的 字体(Font) 以及 画笔参数(Paint) 在指定位置上绘制文字
     * <p>
     * 文字将转换为图形路径绘制填充在OFD页面上
     * <p>
     * 第一个文字的基线坐标为传入的(x,y)参数位置。
     * <p>
     * 文字绘制的将被裁剪矩阵(clip)、变换矩阵影响。
     *
     * @param str 待绘制文字序列
     * @param x   首个文字基线X坐标
     * @param y   首个文字基线Y坐标
     */
    @Override
    public void drawString(String str, float x, float y) {
        GlyphVector gv = getFont().createGlyphVector(getFontRenderContext(), str);
        drawGlyphVector(gv, x, y);
    }

    /**
     * 使用迭代器绘制文字图形
     * <p>
     * 文字图形的绘制将绘制将受到画笔参数(Paint) 、被裁剪矩阵(clip)、变换矩阵影响（CTM）。
     * <p>
     * 第一个文字图形的基线坐标为传入的(x,y)参数位置。
     *
     * @param iterator 待绘制文本的迭代器
     * @param x        首个文字基线X坐标
     * @param y        首个文字基线Y坐标
     */
    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        TextLayout layout = new TextLayout(iterator, getFontRenderContext());
        layout.draw(this, x, y);
    }

    /**
     * 使用迭代器绘制文字图形
     * <p>
     * 文字图形的绘制将绘制将受到画笔参数(Paint) 、被裁剪矩阵(clip)、变换矩阵影响（CTM）。
     * <p>
     * 第一个文字图形的基线坐标为传入的(x,y)参数位置。
     *
     * @param iterator 待绘制文本的迭代器
     * @param x        首个文字基线X坐标
     * @param y        首个文字基线Y坐标
     */
    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        TextLayout layout = new TextLayout(iterator, getFontRenderContext());
        layout.draw(this, x, y);
    }

    /**
     * 在指定位置上绘制绘制图形路径数据，
     * <p>
     * 图形的绘制将绘制将受到画笔参数(Paint) 、被裁剪矩阵(clip)、变换矩阵影响（CTM）。
     * <p>
     * 第一个文字的基线坐标为传入的(x,y)参数位置。
     *
     * @param g 路径向量数据
     * @param x 图形绘制位置X坐标
     * @param y 图形绘制位置Y坐标
     */
    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        Shape glyphOutline = g.getOutline(x, y);
        fill(glyphOutline);
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
        imgObj.setCTM(new ST_Array(width, 0, 0, height, x, y));
//        ST_Array ctm = new ST_Array(width, 0, 0, height, x, y).mtxMul(this.drawParam.ctm);
//        imgObj.setCTM(ctm);
        // 透明度
        if (this.OFDGraphics2DDrawParam.gColor instanceof Color) {
            imgObj.setAlpha(((Color) this.OFDGraphics2DDrawParam.gColor).getAlpha());
        }
        this.OFDGraphics2DDrawParam.apply(imgObj);
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
        BufferedImage img2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
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
     * 通过变换矩阵在指定位置绘制图像
     *
     * @param img   待绘制的图像（可渲染图像接口）
     * @param xform 变换矩阵，指定图像绘制方式
     * @param obs   忽略
     * @return true
     */
    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        AffineTransform old = this.getTransform();
        if (xform != null) {
            this.transform(xform);
        }
        boolean res = drawImage(img, 0, 0, obs);
        if (xform != null) {
            this.setTransform(old);
        }
        return res;
    }

    /**
     * 在画布指定位置绘制图像，在绘制前将会使用 {@link BufferedImageOp} 过滤图像
     * <p>
     * 等价于
     * <pre>
     *       img1 = op.filter(img, null);
     *       drawImage(img1, new AffineTransform(1f,0f,0f,1f,x,y), null);
     * </pre>
     *
     * @param img 待绘制图像
     * @param op  图片渲染前的过滤器
     * @param x   图片左上角X坐标
     * @param y   图片左上角Y坐标
     */
    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        BufferedImage img1 = img;
        if (op != null) {
            img1 = op.filter(img, null);
        }
        drawImage(img1, new AffineTransform(1f, 0f, 0f, 1f, x, y), null);
    }

    /**
     * 通过变换矩阵在指定位置绘制图像
     *
     * @param img   待绘制的图像（可渲染图像接口）
     * @param xform 变换矩阵，指定图像绘制方式
     */
    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        if (img == null) {
            return;
        }
        BufferedImage bufferedImage = convert2Img(img);
        drawImage(bufferedImage, xform, null);
    }

    /**
     * 通过变换矩阵在指定位置绘制图像
     *
     * @param img   待绘制的图像（可渲染图像接口）
     * @param xform 变换矩阵，指定图像绘制方式
     */
    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        if (img == null) {
            return;
        }
        RenderedImage rImg = img.createDefaultRendering();
        drawRenderedImage(rImg, xform);
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
        final AbbreviatedData pData = OFDShapes.path(s);
        if (pData.size() == 0) {
            // 没有绘制参数时不填充
            return;
        }
        // 创建路径对象并设置上下文参数
        CT_Path pathObj = newPathWithCtx();
        pathObj.setFill(true);
        pathObj.setAbbreviatedData(pData);
        container.addPageBlock(pathObj.toObj(doc.newID()));
    }

    /**
     * 检查在设备空间内容 指定的矩形区域是否与 指定形状存在交集。
     * <p>
     * onStroke 为false，表示检查指定形状整体是否与指定矩形相交。
     * <p>
     * onStroke 为true，表示检查指定形状的描边整体是否与指定矩形相交。
     *
     * @param rect     矩形区域
     * @param s        待检查的图形形状
     * @param onStroke 相交检查方式，true - 描边区域是否相交； false - 整个形状是否相交
     * @return true - 相交；false - 不相交
     */
    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        if (onStroke) {
            s = this.OFDGraphics2DDrawParam.gStroke.createStrokedShape(s);
        }
        s = this.OFDGraphics2DDrawParam.gCtm.createTransformedShape(s);
        return s.intersects(rect);
    }

    /**
     * 获取设备图形配置对象
     *
     * @return OFD页面虚拟图形配置对象
     */
    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        if (this.devConfig == null) {
            this.devConfig = new OFDPageGraphicsConfiguration(size.getWidth(), size.getHeight());
        }
        return this.devConfig;
    }

    /**
     * 设置像素合成模式
     * <p>
     * 该方法原用于在设备绘制图像时，当前绘制的像素与原位置上已经存在的像素颜色的合成方式。
     * OFD中没有对应的合成效果，仅做兼容性实现。
     *
     * @param comp 合成方式
     */
    @Override
    public void setComposite(Composite comp) {
        this.OFDGraphics2DDrawParam.composite = comp;
    }

    /**
     * 设置绘制参数
     *
     * @param paint 设置画笔颜色，用于填充和描边
     */
    @Override
    public void setPaint(Paint paint) {
        this.OFDGraphics2DDrawParam.setColor(paint);
    }

    /**
     * 设置描边属性
     *
     * @param s 描边属性参数
     */
    @Override
    public void setStroke(Stroke s) {
        this.OFDGraphics2DDrawParam.setStroke(s);
    }

    /**
     * 设置绘制器参数
     *
     * @param hintKey   参数名
     * @param hintValue 参数值
     */
    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        this.OFDGraphics2DDrawParam.fontRenderCtx = null;
        this.OFDGraphics2DDrawParam.hints.put(hintKey, hintValue);
    }

    /**
     * 获取绘制器参数值（为了兼容接口，无实际用途）
     *
     * @param hintKey 参数名
     * @return 可能为空
     */
    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return this.OFDGraphics2DDrawParam.hints.get(hintKey);
    }

    /**
     * 替换绘制器参数（为了兼容接口，无实际用途）
     *
     * @param hints 新参数
     */
    @Override
    public void setRenderingHints(Map<?, ?> hints) {
        this.OFDGraphics2DDrawParam.fontRenderCtx = null;
        this.OFDGraphics2DDrawParam.hints.clear();
        this.OFDGraphics2DDrawParam.hints.putAll(hints);
    }

    /**
     * 添加绘制器参数（为了兼容接口，无实际用途）
     *
     * @param hints 键值对
     */
    @Override
    public void addRenderingHints(Map<?, ?> hints) {
        this.OFDGraphics2DDrawParam.fontRenderCtx = null;
        this.OFDGraphics2DDrawParam.hints.putAll(hints);
    }

    /**
     * 获取当前绘制器参数信息（为了兼容接口，无实际用途）
     *
     * @return 参数信息（只读）
     */
    @Override
    public RenderingHints getRenderingHints() {
        return (RenderingHints) this.OFDGraphics2DDrawParam.hints.clone();
    }

    /**
     * 复制当前绘制上下文为新的上下文
     *
     * @return 复制的绘制上下文对象
     */
    @Override
    public Graphics create() {
        return new OFDPageGraphics2D(this);
    }

    /**
     * 返回前景色
     *
     * @return 颜色，可能为null
     */
    @Override
    public Color getColor() {
        return this.OFDGraphics2DDrawParam.gForeground;
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
        this.OFDGraphics2DDrawParam.setForeground(c);
    }

    /**
     * setPaintMode 不实现
     */
    @Override
    public void setPaintMode() {
        // 不实现
    }

    /**
     * setXORMode 不实现
     *
     * @param c1 the XOR alternation color
     */
    @Override
    public void setXORMode(Color c1) {
        // 不实现
    }

    /**
     * 获取当前字体
     *
     * @return 字体
     */
    @Override
    public Font getFont() {
        return this.OFDGraphics2DDrawParam.font;
    }

    /**
     * 设置绘图上下文的字体
     *
     * @param font 字体
     */
    @Override
    public void setFont(Font font) {
        this.OFDGraphics2DDrawParam.font = font;
    }

    /**
     * 获取特定字体的字体规格
     *
     * @param f 特定字体
     * @return 字体规格
     */
    @Override
    public FontMetrics getFontMetrics(Font f) {
        return fmg.getFontMetrics(f);
    }

    /**
     * 获取裁剪区域的外接矩形大小
     *
     * @return 裁剪区域外接矩形，可能为null
     */
    @Override
    public Rectangle getClipBounds() {
        Shape c = getClip();
        if (c == null) {
            return null;
        } else {
            return c.getBounds();
        }
    }

    /**
     * 设置矩形裁剪区域
     *
     * @param x      裁剪矩形区域X坐标
     * @param y      裁剪矩形区域Y坐标
     * @param width  裁剪矩形矩形宽度
     * @param height 裁剪矩形矩形高度
     */
    @Override
    public void clipRect(int x, int y, int width, int height) {
        clip(new Rectangle2D.Double(x, y, width, height));
    }

    /**
     * 设置矩形裁剪区域
     * <p>
     * 若已经存在裁剪区域那么旧的裁剪区域将会被新的裁剪区域覆盖
     *
     * @param x      裁剪矩形区域X坐标
     * @param y      裁剪矩形区域Y坐标
     * @param width  裁剪矩形矩形宽度
     * @param height 裁剪矩形矩形高度
     */
    @Override
    public void setClip(int x, int y, int width, int height) {
        setClip(new Rectangle2D.Double(x, y, width, height));
    }

    /**
     * 获取裁剪区域
     *
     * @return 裁剪区域，可能为null
     */
    @Override
    public Shape getClip() {
        return this.OFDGraphics2DDrawParam.gClip;
    }

    /**
     * 设置裁剪区域
     * <p>
     * 若已经存在裁剪区域，那么新的裁剪区域与旧的裁剪区域取交集。
     *
     * @param s 裁剪区域，如果为 null 则清除裁剪区域
     */
    @Override
    public void clip(Shape s) {
        if (this.OFDGraphics2DDrawParam.gClip == null) {
            setClip(s);
            return;
        }
        Area newClip = new Area(s);
        newClip.intersect(new Area(s));
        this.OFDGraphics2DDrawParam.gClip = new GeneralPath(newClip);
    }


    /**
     * 设置裁剪区域
     * <p>
     * 若已经存在裁剪区域那么旧的裁剪区域将会被新的裁剪区域覆盖
     *
     * @param clip 裁剪区域
     */
    @Override
    public void setClip(Shape clip) {
        this.OFDGraphics2DDrawParam.gClip = clip;
    }

    /**
     * 复制矩形区域
     *
     * @param x      the <i>x</i> coordinate of the source rectangle.
     * @param y      the <i>y</i> coordinate of the source rectangle.
     * @param width  the width of the source rectangle.
     * @param height the height of the source rectangle.
     * @param dx     the horizontal distance to copy the pixels.
     * @param dy     the vertical distance to copy the pixels.
     */
    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        // 不予实现
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
        if (this.OFDGraphics2DDrawParam.gBackground == null) {
            return;
        }
        Paint saved = getPaint();
        setPaint(this.OFDGraphics2DDrawParam.gBackground);
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
        translate(x, (double) y);
    }

    /**
     * 平移原点
     *
     * @param tx x方向的平移距离
     * @param ty y方向的平移距离
     */
    @Override
    public void translate(double tx, double ty) {
        ST_Array r = new ST_Array(1, 0, 0, 1, tx, ty);
        this.OFDGraphics2DDrawParam.ctm = r.mtxMul(this.OFDGraphics2DDrawParam.ctm);
        this.OFDGraphics2DDrawParam.gCtm.translate(tx, ty);
    }

    /**
     * 绕原点旋转画布
     *
     * @param theta 旋转角度，计算方式为 {@code theta = angle * Math.PI / 180 }，负数表示逆时针。
     */
    @Override
    public void rotate(double theta) {
        ST_Array r = new ST_Array(Math.cos(theta), Math.sin(theta), -Math.sin(theta), Math.cos(theta), 0, 0);
        this.OFDGraphics2DDrawParam.ctm = r.mtxMul(this.OFDGraphics2DDrawParam.ctm);
        this.OFDGraphics2DDrawParam.gCtm.rotate(theta);
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
        ST_Array scale = new ST_Array(sx, 0, 0, sy, 0, 0);
        this.OFDGraphics2DDrawParam.ctm = scale.mtxMul(this.OFDGraphics2DDrawParam.ctm);
        this.OFDGraphics2DDrawParam.gCtm.scale(sx, sy);
    }

    /**
     * 切变
     *
     * @param shx X方向切变角度
     * @param shy Y方向切变角度
     */
    @Override
    public void shear(double shx, double shy) {
        ST_Array shear = new ST_Array(1, Math.tan(shx), Math.tan(shy), 1, 0, 0);
        this.OFDGraphics2DDrawParam.ctm = shear.mtxMul(this.OFDGraphics2DDrawParam.ctm);
        this.OFDGraphics2DDrawParam.gCtm.shear(shx, shy);
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
        this.OFDGraphics2DDrawParam.ctm = ctm.mtxMul(this.OFDGraphics2DDrawParam.ctm);
        this.OFDGraphics2DDrawParam.gCtm.concatenate(tx);
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
        this.OFDGraphics2DDrawParam.ctm = trans(tx);
    }

    /**
     * 返回当前的变换矩阵
     *
     * @return 变换矩阵
     */
    @Override
    public AffineTransform getTransform() {
        return new AffineTransform(this.OFDGraphics2DDrawParam.gCtm);
    }

    /**
     * 获取绘制参数
     *
     * @return 绘制参数
     */
    @Override
    public Paint getPaint() {
        return this.OFDGraphics2DDrawParam.gColor;
    }

    /**
     * 获取像素合成模式
     * <p>
     * 该属性只是为了兼容AWT接口保留，并无实际用途。
     *
     * @return 获取像素合成模式
     */
    @Override
    public Composite getComposite() {
        return this.OFDGraphics2DDrawParam.composite;
    }

    /**
     * 设置背景颜色
     *
     * @param color 该颜色将用于 <code>clearRect</code> 清空区域
     */
    @Override
    public void setBackground(Color color) {
        this.OFDGraphics2DDrawParam.gBackground = color;
    }

    /**
     * 获取背景颜色
     *
     * @return 背景颜色
     */
    @Override
    public Color getBackground() {
        return this.OFDGraphics2DDrawParam.gBackground;
    }

    /**
     * 获取描边属性
     *
     * @return 描述属性 {@link BasicStroke}
     */
    @Override
    public Stroke getStroke() {
        return this.OFDGraphics2DDrawParam.gStroke;
    }

    /**
     * 获取绘制上下文中的字体绘制上下文
     *
     * @return 字体绘制上下文
     */
    @Override
    public FontRenderContext getFontRenderContext() {
        return this.OFDGraphics2DDrawParam.getFontRenderContext();
    }


    /**
     * 销毁绘制上下文
     */
    @Override
    public void dispose() {

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
        return new ST_Array(tx.getScaleX(), tx.getShearY(), tx.getShearX(), tx.getScaleY(), tx.getTranslateX(), tx.getTranslateY());
    }

    /**
     * 将可渲染对象转换为缓存图像
     *
     * @param img 可渲染对象
     * @return 缓存图像
     */
    private BufferedImage convert2Img(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        final int width = img.getWidth();
        final int height = img.getHeight();
        final ColorModel cm = img.getColorModel();
        final boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();

        final WritableRaster raster = cm.createCompatibleWritableRaster(width, height);

        final Hashtable<String, Object> properties = new Hashtable<>();
        String[] keys = img.getPropertyNames();
        if (keys != null) {
            for (String key : keys) {
                properties.put(key, img.getProperty(key));
            }
        }
        final BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
        img.copyData(raster);
        return result;
    }
}
