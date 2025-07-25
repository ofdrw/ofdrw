package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.graph.pathObj.OptVal;
import org.ofdrw.core.pageDescription.CT_GraphicUnit;
import org.ofdrw.core.pageDescription.clips.CT_Clip;
import org.ofdrw.core.pageDescription.clips.Clips;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.color.ColorClusterType;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.core.text.text.CT_Text;
import org.ofdrw.core.text.text.Direction;
import org.ofdrw.core.text.text.Weight;
import org.ofdrw.font.Font;
import org.ofdrw.layout.engine.ExistCTFont;
import org.ofdrw.layout.engine.ResManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 绘制器绘制上下文
 * <p>
 * 上下文中提供系列的绘制方法供绘制
 * <p>
 * 一个路径对象只允许出现一种描边和填充颜色
 * 重复设置，取最后一次设置的颜色。
 * <p>
 * 关于路径：
 * 1. beginPath 清空路径。
 * 2. 所有路径在 fill 和 stroke 是才应用图元效果。
 * 3. 路径数据 与 绘图数据分开。
 * 4. 除了 beginPath 之外所有数据均认为是 向已经存在的路径追加新的路径。
 *
 * @author 权观宇
 * @since 2020-05-01 11:29:20
 */
public class DrawContext implements Closeable {
    static final ST_Array ONE = ST_Array.unitCTM();
    /**
     * 用于容纳所绘制的所有图像的容器
     */
    private CT_PageBlock container;

    /**
     * 对象ID提供器
     */
    private AtomicInteger maxUnitID;

    /**
     * 资源管理器
     */
    private ResManager resManager;


    /**
     * 边框位置，也就是画布大小以及位置
     */
    private ST_Box boundary;

    /**
     * 画布状态
     */
    private CanvasState state;

    /**
     * 绘制参数栈
     * <p>
     * save() 时将当前绘制参数压栈
     * <p>
     * restore() 时将当前绘制参数出栈
     */
    private LinkedList<CanvasState> stack;

    /**
     * 填充颜色
     * <p>
     * 支持：
     * <p>
     * {@link String} 16进制颜色值，#000000、rgb(0,0,0)、rgba(0,0,0,1)
     * <p>
     * {@link CT_Color} OFD颜色对象
     * <p>
     * {@link ColorClusterType} 颜色族
     * <p>
     * {@link CanvasPattern} 图案
     * <p>
     * {@link CanvasGradient} 渐变
     * <p>
     * {@link CanvasRadialGradient} 径向渐变
     */
    public Object fillStyle;

    /**
     * 描边颜色
     * <p>
     * 支持：
     * <p>
     * {@link String} 16进制颜色值，#000000、rgb(0,0,0)、rgba(0,0,0,1)
     * <p>
     * {@link CT_Color} OFD颜色对象
     * <p>
     * {@link ColorClusterType} 颜色族
     * <p>
     * {@link CanvasPattern} 图案
     * <p>
     * {@link CanvasGradient} 渐变
     * <p>
     * {@link CanvasRadialGradient} 径向渐变
     */
    public Object strokeStyle;

    /**
     * 字体描述 格式与CSS3格式一致
     * <p>
     * [font-style] [font-weight] font-size font-family
     * <p>
     * 它必需包含 font-size font-family，[]内容为可选
     * <p>
     * 详见 {@code https://developer.mozilla.org/en-US/docs/Web/CSS/font}
     * <p>
     * font-style: normal | italic
     * <p>
     * font-weight: normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900
     * <p>
     * font-size: 12px | 3.17mm （默认单位为 mm）
     * <p>
     * font-family: 宋体 | SimSun | Times New Roman | Times | serif | sans-serif | monospace | cursive | fantasy
     * <p>
     * font-family 为必选项，其他为可选项
     * <p>
     * font-size 和 line-height 可以使用 px 或 mm 作为单位，若不指定单位则默认为 mm
     * <p>
     * 锚点：
     * - fillText
     * - measureText
     */
    public String font;

    /**
     * 每毫米像素数量 pixel per millimeter
     */
    public double PPM;

    private DrawContext() {
    }

    /**
     * 创建绘制上下文
     *
     * @param container  绘制内容缩所放置容器
     * @param boundary   画布大小以及位置
     * @param maxUnitID  自增的对象ID
     * @param resManager 资源管理器
     */
    public DrawContext(CT_PageBlock container, ST_Box boundary, AtomicInteger maxUnitID, ResManager resManager) {
        this.container = container;
        this.boundary = boundary;
        this.maxUnitID = maxUnitID;
        this.resManager = resManager;
        this.state = new CanvasState();
        this.stack = new LinkedList<>();
        this.PPM = 3.78;
        // 初始化颜色默认为黑色
        this.fillStyle = "#000000";
        this.strokeStyle = "#000000";
    }

    /**
     * 开启一段新的路径
     * <p>
     * 如果已经存在路径，那么将会清除已经存在的所有路径。
     *
     * @return this
     */
    public DrawContext beginPath() {
        this.state.path = new AbbreviatedData();
        return this;
    }


    /**
     * 关闭路径
     * <p>
     * 如果路径存在描边或者填充，那么改路径将会被加入到图形容器中进行渲染
     * <p>
     * 路径关闭后将会清空上下文中的路径对象
     *
     * @return this
     */
    public DrawContext closePath() {
        if (this.state.path == null) {
            return this;
        }
        this.state.path.close();
        return this;
    }

    /**
     * 从原始画布中剪切任意形状和尺寸
     * <p>
     * 裁剪路径以当前的路径作为裁剪参数
     * <p>
     * 裁剪区域受变换矩阵影响
     *
     * @return this
     */
    public DrawContext clip() {
        if (this.state.path == null) {
            return this;
        }

        this.state.clipArea = this.state.path.clone();
        if (this.state.ctm != null && !ONE.equals(this.state.ctm)) {
            // 受到CTM的影响形变
            transform(this.state.clipArea, this.state.ctm);
        }
        return this;
    }

    /**
     * 移动绘制点到指定位置
     *
     * @param x X坐标
     * @param y Y坐标
     * @return this
     */
    public DrawContext moveTo(double x, double y) {
        if (this.state.path == null) {
            this.state.path = new AbbreviatedData();
        }
        this.state.path.moveTo(x, y);
        return this;
    }

    /**
     * 从当前点连线到指定点
     * <p>
     * 请在调用前创建路径
     *
     * @param x X坐标
     * @param y Y坐标
     * @return this
     */
    public DrawContext lineTo(double x, double y) {
        if (this.state.path == null) {
            return this;
        }
        this.state.path.lineTo(x, y);
        return this;
    }


    /**
     * 通过二次贝塞尔曲线的指定控制点，向当前路径添加一个点。
     *
     * @param cpx 贝塞尔控制点的 x 坐标
     * @param cpy 贝塞尔控制点的 y 坐标
     * @param x   结束点的 x 坐标
     * @param y   结束点的 y 坐标
     * @return this
     */
    public DrawContext quadraticCurveTo(double cpx, double cpy, double x, double y) {
        if (this.state.path == null) {
            this.state.path = new AbbreviatedData();
        }
        this.state.path.quadraticBezier(cpx, cpy, x, y);
        return this;
    }

    /**
     * 方法三次贝塞尔曲线的指定控制点，向当前路径添加一个点。
     *
     * @param cp1x 第一个贝塞尔控制点的 x 坐标
     * @param cp1y 第一个贝塞尔控制点的 y 坐标
     * @param cp2x 第二个贝塞尔控制点的 x 坐标
     * @param cp2y 第二个贝塞尔控制点的 y 坐标
     * @param x    结束点的 x 坐标
     * @param y    结束点的 y 坐标
     * @return this
     */
    public DrawContext bezierCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y) {
        if (this.state.path == null) {
            this.state.path = new AbbreviatedData();
        }
        this.state.path.cubicBezier(cp1x, cp1y, cp2x, cp2y, x, y);
        return this;
    }

    /**
     * 从当前点连接到点（x，y）的圆弧，并将当前点移动到点（x，y）。
     * rx 表示椭圆的长轴长度，ry 表示椭圆的短轴长度。angle 表示
     * 椭圆在当前坐标系下旋转的角度，正值为顺时针，负值为逆时针，
     * large 为 1 时表示对应度数大于180°的弧，为 0 时表示对应
     * 度数小于 180°的弧。sweep 为 1 时表示由圆弧起始点到结束点
     * 是顺时针旋转，为 0 时表示由圆弧起始点到结束点是逆时针旋转。
     *
     * @param a     椭圆长轴长度
     * @param b     椭圆短轴长度
     * @param angle 旋转角度，正值 - 顺时针，负值 - 逆时针
     * @param large true表示对应度数大于 180°的弧，false 表示对应度数小于 180°的弧
     * @param sweep sweep  true 表示由圆弧起始点到结束点是顺时针旋转，false表示由圆弧起始点到结束点是逆时针旋转。
     * @param x     目标点 x
     * @param y     目标点 y
     * @return this
     */
    public DrawContext arc(double a, double b, double angle, boolean large, boolean sweep, double x, double y) {
        if (this.state.path == null) {
            this.state.path = new AbbreviatedData();
        }
        this.state.path.arc(a, b, angle % 360, large ? 1 : 0, sweep ? 1 : 0, x, y);
        return this;
    }


    /**
     * 创建弧/曲线（用于创建圆或部分圆）
     *
     * @param x                圆的中心的 x 坐标。
     * @param y                圆的中心的 y 坐标。
     * @param r                圆的半径。
     * @param sAngle           起始角，单位度（弧的圆形的三点钟位置是 0 度）。
     * @param eAngle           结束角，单位度
     * @param counterclockwise 规定应该逆时针还是顺时针绘图。false = 顺时针，true = 逆时针。
     * @return this
     */
    public DrawContext arc(double x, double y, double r, double sAngle, double eAngle, boolean counterclockwise) {

        if (this.state.path == null) {
            this.state.path = new AbbreviatedData();
        }

        // 首先移动点到起始位置
        double x1 = x + r * Math.cos(sAngle * Math.PI / 180);
        double y1 = y + r * Math.sin(sAngle * Math.PI / 180);
        this.moveTo(x1, y1);


        double angle = eAngle - sAngle;
        if (angle == 360) {
            // 整个圆的时候需要分为两次路径进行绘制
            // 绘制结束位置起始位置
            this.state.path.arc(r, r, angle, 1, counterclockwise ? 1 : 0, x - r, y).arc(r, r, angle, 1, counterclockwise ? 1 : 0, x1, y1);
        } else {
            // 绘制结束位置起始位置
            double x2 = x + r * Math.cos(eAngle * Math.PI / 180);
            double y2 = y + r * Math.sin(eAngle * Math.PI / 180);
            this.state.path.arc(r, r, angle, angle > 180 ? 1 : 0, counterclockwise ? 1 : 0, x2, y2);
        }

        return this;
    }

    /**
     * 创建弧/曲线（用于创建圆或部分圆）
     * <p>
     * 默认顺时针方向
     *
     * @param x      圆的中心的 x 坐标。
     * @param y      圆的中心的 y 坐标。
     * @param r      圆的半径。
     * @param sAngle 起始角，单位度（弧的圆形的三点钟位置是 0 度）。
     * @param eAngle 结束角，单位度
     * @return this
     */
    public DrawContext arc(double x, double y, double r, double sAngle, double eAngle) {
        return arc(x, y, r, sAngle, eAngle, true);
    }


    /**
     * 创建矩形路径
     *
     * @param x      左上角X坐标
     * @param y      左上角Y坐标
     * @param width  宽度
     * @param height 高度
     * @return this
     */
    public DrawContext rect(double x, double y, double width, double height) {
        if (this.state.path == null) {
            this.state.path = new AbbreviatedData();
        }

        this.state.path.moveTo(x, y).lineTo(x + width, y).lineTo(x + width, y + height).lineTo(x, y + height).close();
        return this;
    }

    /**
     * 创建并填充矩形路径
     * <p>
     * 填充矩形不会导致影响上下文中的路径。
     * <p>
     * 如果已经存在路径那么改路径将会提前关闭，并创建新的路径。
     *
     * @param x      左上角X坐标
     * @param y      左上角Y坐标
     * @param width  宽度
     * @param height 高度
     * @return this
     */
    public DrawContext fillRect(double x, double y, double width, double height) {
        AbbreviatedData abData = new AbbreviatedData().moveTo(x, y).lineTo(x + width, y).lineTo(x + width, y + height).lineTo(x, y + height).close();

        PathObject p = new PathObject(new ST_ID(maxUnitID.incrementAndGet()));
        p.setAbbreviatedData(abData);
        p.setFill(true);
        applyDrawParam(p);
        container.add(p);
        return this;
    }

    /**
     * 创建并描边矩形路径
     * <p>
     * 描边矩形不会导致影响上下文中的路径。
     * <p>
     * 默认描边颜色为黑色
     *
     * @param x      左上角X坐标
     * @param y      左上角Y坐标
     * @param width  宽度
     * @param height 高度
     * @return this
     */
    public DrawContext strokeRect(double x, double y, double width, double height) {
        AbbreviatedData abData = new AbbreviatedData().moveTo(x, y).lineTo(x + width, y).lineTo(x + width, y + height).lineTo(x, y + height).close();

        PathObject p = new PathObject(new ST_ID(maxUnitID.incrementAndGet()));
        p.setAbbreviatedData(abData);
        p.setStroke(true);
        applyDrawParam(p);
        container.add(p);
        return this;
    }

    /**
     * 绘制已定义的路径
     * <p>
     * 注意stroke()和fill()方法会清空路径
     *
     * @return this
     */
    public DrawContext stroke() {
        if (this.state.path == null) {
            return this;
        }

        PathObject p = new PathObject(new ST_ID(maxUnitID.incrementAndGet()));
        p.setAbbreviatedData(this.state.path.clone());
        p.setStroke(true);
        applyDrawParam(p);
        container.add(p);
        // 绘制后清空路径
        this.state.path = null;
        return this;
    }

    /**
     * 填充已定义路径
     * <p>
     * 默认的填充颜色是黑色。
     * <p>
     * 注意stroke()和fill()方法会清空路径
     *
     * @return this
     */
    public DrawContext fill() {
        if (this.state.path == null) {
            return this;
        }

        PathObject p = new PathObject(new ST_ID(maxUnitID.incrementAndGet()));
        p.setAbbreviatedData(this.state.path.clone());
        p.setFill(true);
        p.setLineWidth(0d);
        applyDrawParam(p);
        container.add(p);
        // 绘制后清空路径
        this.state.path = null;

        return this;
    }

    /**
     * 缩放当前绘图，更大或更小
     *
     * @param scalewidth  缩放当前绘图的宽度 (1=100%, 0.5=50%, 2=200%, 依次类推)
     * @param scaleheight 缩放当前绘图的高度 (1=100%, 0.5=50%, 2=200%, 依次类推)
     * @return this
     */
    public DrawContext scale(double scalewidth, double scaleheight) {
        if (this.state.ctm == null) {
            this.state.ctm = ST_Array.unitCTM();
        }
        ST_Array scale = new ST_Array(scalewidth, 0, 0, scaleheight, 0, 0);
        this.state.ctm = scale.mtxMul(this.state.ctm);
        return this;
    }

    /**
     * 旋转当前的绘图
     *
     * @param angle 旋转角度（0~360）
     * @return this
     */
    public DrawContext rotate(double angle) {
        if (this.state.ctm == null) {
            this.state.ctm = ST_Array.unitCTM();
        }
        double alpha = angle * Math.PI / 180d;
        ST_Array r = new ST_Array(Math.cos(alpha), Math.sin(alpha), -Math.sin(alpha), Math.cos(alpha), 0, 0);
        this.state.ctm = r.mtxMul(this.state.ctm);
        return this;
    }

    /**
     * 切变
     *
     * @param x X方向 切变度数（0~360，不包含90、270）
     * @param y Y方向 切变度数（0~360，不包含90、270）
     * @return this
     */
    public DrawContext shear(double x, double y) {
        if (this.state.ctm == null) {
            this.state.ctm = ST_Array.unitCTM();
        }

        ST_Array r = new ST_Array(1, Math.tan(x*Math.PI/180d), Math.tan(y*Math.PI/180d), 1, 0, 0);
        this.state.ctm = r.mtxMul(this.state.ctm);
        return this;
    }

    /**
     * 重新映射画布上的 (0,0) 位置
     *
     * @param x 添加到水平坐标（x）上的值
     * @param y 添加到垂直坐标（y）上的值
     * @return this
     */
    public DrawContext translate(double x, double y) {
        if (this.state.ctm == null) {
            this.state.ctm = ST_Array.unitCTM();
        }
        ST_Array r = new ST_Array(1, 0, 0, 1, x, y);
        this.state.ctm = r.mtxMul(this.state.ctm);
        return this;
    }

    /**
     * 变换矩阵
     * <p>
     * 每次变换矩阵都会在前一个变换的基础上进行
     *
     * @param a 水平缩放绘图
     * @param b 水平倾斜绘图
     * @param c 垂直倾斜绘图
     * @param d 垂直缩放绘图
     * @param e 水平移动绘图
     * @param f 垂直移动绘图
     * @return this
     */
    public DrawContext transform(double a, double b, double c, double d, double e, double f) {
        if (this.state.ctm == null) {
            this.state.ctm = ST_Array.unitCTM();
        }
        ST_Array r = new ST_Array(a, b, c, d, e, f);
        this.state.ctm = r.mtxMul(this.state.ctm);
        return this;
    }

    /**
     * 设置变换矩阵
     * <p>
     * 每当调用 setTransform() 时，它都会重置前一个变换矩阵然后构建新的矩阵
     *
     * @param a 水平缩放绘图
     * @param b 水平倾斜绘图
     * @param c 垂直倾斜绘图
     * @param d 垂直缩放绘图
     * @param e 水平移动绘图
     * @param f 垂直移动绘图
     * @return this
     */
    public DrawContext setTransform(double a, double b, double c, double d, double e, double f) {
        this.state.ctm = new ST_Array(a, b, c, d, e, f);
        return this;
    }


    /**
     * 裁剪图片并在OFD上绘制图像
     * <p>
     * 主要该方法将会裁剪图片的一部分，然后在OFD上绘制
     * 该方法所有参数单位都是毫米mm，像素转换毫米需要通过 {@link #PPM} 转换。
     *
     * @param img     图像，路径
     * @param sx      图像内部 x 坐标（单位 毫米mm）
     * @param sy      图像内部 y 坐标（单位 毫米mm）
     * @param sWidth  图像内部宽度（单位 像素px）
     * @param sHeight 图像内部高度（单位 像素px）
     * @param dx      在画布上放置图像的 x 坐标位置（单位 毫米mm）
     * @param dy      在画布上放置图像的 y 坐标位置（单位 毫米mm）
     * @param dWidth  在画布上放置图像的宽度（单位 毫米mm）
     * @param dHeight 在画布上放置图像的高度（单位 毫米mm）
     * @return this
     * @throws IOException 图像读取异常
     */
    public DrawContext drawImage(Path img, double sx, double sy, double sWidth, double sHeight, double dx, double dy, double dWidth, double dHeight) throws IOException {
        if (img == null || Files.notExists(img)) {
            throw new IllegalArgumentException("图像不存在");
        }
        // 加载原图片
        BufferedImage gImg = ImageIO.read(img.toFile());
        int w = pixel(sWidth);
        int h = pixel(sHeight);
        // 按照区域裁剪图片
        BufferedImage cutOut = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = cutOut.createGraphics();
        g2.drawImage(gImg, 0, 0, w, h, pixel(sx), pixel(sy), pixel(sx) + w, pixel(sy) + h, null);

        Path tmpImgCutPath = null;
        try {
            // 裁剪后的图片存储到临时文件
            tmpImgCutPath = Files.createTempFile("", ".png");
            ImageIO.write(cutOut, "png", tmpImgCutPath.toFile());
            return drawImage(tmpImgCutPath, dx, dy, dWidth, dHeight);
        } finally {
            if (tmpImgCutPath != null) {
                Files.deleteIfExists(tmpImgCutPath);
            }
        }
    }

    /**
     * 在OFD上绘制图像
     * <p>
     * 图像宽度和高度将按照 {@link #PPM} 进行转换
     *
     * @param img 要使用的图像，请避免资源和文档中已经存在的资源重复
     * @param dx  在画布上放置图像的 x 坐标位置
     * @param dy  在画布上放置图像的 y 坐标位置
     * @return this
     * @throws IOException 图片文件读写异常
     */
    public DrawContext drawImage(Path img, double dx, double dy) throws IOException {
        if (img == null || Files.notExists(img)) {
            throw new IllegalArgumentException("图像不存在");
        }
        // 加载原图片
        BufferedImage gImg = ImageIO.read(img.toFile());
        int w = gImg.getWidth();
        int h = gImg.getHeight();
        return drawImage(img, dx, dy, mm(w), mm(h));
    }

    /**
     * 在OFD上绘制图像
     *
     * @param img     要使用的图像，请避免资源和文档中已经存在的资源重复
     * @param dx      在画布上放置图像的 x 坐标位置
     * @param dy      在画布上放置图像的 y 坐标位置
     * @param dWidth  要使用的图像的宽度（伸展或缩小图像）
     * @param dHeight 要使用的图像的高度（伸展或缩小图像）
     * @return this
     * @throws IOException 图片文件读写异常
     */
    public DrawContext drawImage(Path img, double dx, double dy, double dWidth, double dHeight) throws IOException {
        if (img == null || Files.notExists(img)) {
            throw new IOException("图片(img)不存在");
        }

        ST_ID id = resManager.addImage(img);
        // 在公共资源中加入图片
        ImageObject imgObj = new ImageObject(maxUnitID.incrementAndGet());
        imgObj.setResourceID(id.ref());
        imgObj.setBoundary(boundary.clone());

        // 应用变换矩阵
        ST_Array ctm = this.state.ctm == null ? ST_Array.unitCTM() : this.state.ctm;
        ctm = new ST_Array(dWidth, 0, 0, dHeight, dx, dy).mtxMul(ctm);
        imgObj.setCTM(ctm);

        // 应用绘制参数
        applyDrawParam(imgObj);
        container.addPageBlock(imgObj);

        return this;
    }


    /**
     * 保存当前绘图状态
     * <p>
     * 与 {@link #restore()} 总是成对出现。
     *
     * @return this
     */
    public DrawContext save() {
        this.state.strokeStyle = this.strokeStyle;
        this.state.fillStyle = this.fillStyle;
        this.state.fontStyle = this.font;
        stack.push(this.state.clone());
        return this;
    }

    /**
     * 还原绘图状态
     * <p>
     * 与 {@link #save()} 总是成对出现。
     *
     * @return this
     */
    public DrawContext restore() {
        if (stack.isEmpty()) {
            return this;
        }
        this.state = stack.pop();
        this.strokeStyle = this.state.strokeStyle;
        this.fillStyle = this.state.fillStyle;
        this.font = this.state.fontStyle;
        return this;
    }

    /**
     * 填充文字
     *
     * @param text 填充文字
     * @param x    阅读方向上的左下角 x坐标
     * @param y    阅读方向上的左下角 y坐标
     * @return this
     * @throws IOException 字体获取异常
     */
    public DrawContext fillText(String text, double x, double y) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            return this;
        }

        ST_ID fontID = null;
        // 转换字体样式 为 字体设置
        CT_Font existFont = fontStyleToSetting(this.font, this.state.font);
        if (existFont != null) {
            fontID = existFont.getID();
        } else {
            fontID = resManager.addFont(state.font.getFont());
        }

        // 新建字体对象
        TextObject txtObj = new CT_Text()
                .setBoundary(this.boundary.clone())
                .setFont(fontID.ref())
                .setSize(state.font.getFontSize())
                .toObj(new ST_ID(maxUnitID.incrementAndGet()));

        // 设置填充
        txtObj.setFill(true);
        // 设置字体宽度
        if (state.font.getFontWeight() != null && state.font.getFontWeight() != 400) {
            txtObj.setWeight(Weight.getInstance(state.font.getFontWeight()));
        }
        // 是否斜体
        if (state.font.isItalic()) {
            txtObj.setItalic(true);
        }

        int readDirection = state.font.getReadDirection();
        int charDirection = state.font.getCharDirection();

        // 设置阅读方向
        if (readDirection != 0) {
            txtObj.setReadDirection(Direction.getInstance(readDirection));
        }
        // 设置文字方向
        if (charDirection != 0) {
            txtObj.setCharDirection(Direction.getInstance(charDirection));
        }

        // 测量字间距
        MeasureBody measureBody = TextMeasureTool.measureWithWith(text, state.font);

        // 第一个字母的偏移量计算
        double xx = x + measureBody.firstCharOffsetX;
        double yy = y + measureBody.firstCharOffsetY;
        switch (readDirection) {
            case 0:
            case 180:
                xx += textFloatFactor(state.font.getTextAlign(), measureBody.width, readDirection);
                break;
            case 90:
            case 270:
                yy += textFloatFactor(state.font.getTextAlign(), measureBody.width, readDirection);
                break;
        }
        TextCode tcSTTxt = new TextCode().setContent(text).setX(xx).setY(yy);

        if (readDirection == 90 || readDirection == 270) {
            tcSTTxt.setDeltaY(measureBody.offset);
        } else {
            tcSTTxt.setDeltaX(measureBody.offset);
        }
        txtObj.addTextCode(tcSTTxt);

        // 应用绘制参数
        applyDrawParam(txtObj);

        // 加入容器
        container.addPageBlock(txtObj);
        return this;
    }

    /**
     * 文本浮动带来的偏移量因子
     *
     * @param align         对齐方向
     * @param width         文本宽度
     * @param readDirection 阅读方向
     * @return 浮动因子
     */
    private double textFloatFactor(TextAlign align, double width, int readDirection) {
        double factor = 0;
        switch (align) {
            case start:
            case left:
                factor = 0;
                break;
            case end:
            case right:
                factor = -width;
                break;
            case center:
                factor = -width / 2;
                break;
        }
        if (readDirection == 180 || readDirection == 270) {
            factor = -factor;
        }
        return factor;
    }

    /**
     * 获取文本对齐方式
     *
     * @return 文本对齐方式
     */
    public TextAlign getTextAlign() {
        return this.state.font.getTextAlign();
    }

    /**
     * 设置文本对齐方式
     *
     * @param textAlign 文本对齐方式
     * @return this
     */
    public DrawContext setTextAlign(TextAlign textAlign) {
        this.state.font.setTextAlign(textAlign);
        return this;
    }

    /**
     * 测量文本的宽度或高度
     * <p>
     * 如果 readDirection为 0或180，测量文本宽度
     * <p>
     * 如果 readDirection为 0或180，测量文本高度
     *
     * @param text 带测量文本
     * @return 测量文本信息
     */
    public TextMetrics measureText(String text) {
        // 转换字体样式 为 字体设置
        fontStyleToSetting(this.font, this.state.font);

        TextMetrics tm = new TextMetrics();
        tm.readDirection = state.font.getReadDirection();
        tm.fontSize = state.font.getFontSize();
        // 测量字间距
        MeasureBody measureBody = TextMeasureTool.measureWithWith(text, state.font);
        tm.width = measureBody.width;
        tm.offset = measureBody.offset;
        return tm;
    }


    /**
     * 测量文本所占空间大小
     *
     * @param text 带测量文本
     * @return 文件所占空间信息
     */
    public TextMetricsArea measureTextArea(String text) {
        // 转换字体样式 为 字体设置
        fontStyleToSetting(this.font, this.state.font);
        // 测量字间距
        return TextMeasureTool.measureArea(text, state.font);
    }

    /**
     * 读取当前描边颜色（只读）
     * <p>
     * 若描边颜色非颜色值，则返回null
     *
     * @return 描边颜色（只读）
     */
    public int[] getStrokeColor() {
        if (this.strokeStyle instanceof String) {
            return NamedColor.rgb((String) this.strokeStyle);
        }

        return null;
    }

    /**
     * 设置描边颜色
     * <p>
     * 一条路径只有一种描边颜色，重复设置只取最后一次设置颜色
     *
     * @param strokeColor 描边的RGB颜色
     * @return this
     */
    public DrawContext setStrokeColor(int[] strokeColor) {
        this.strokeStyle = String.format("#%02X%02X%02X", strokeColor[0], strokeColor[1], strokeColor[2]);
        return this;
    }

    /**
     * 设置描边颜色
     * <p>
     * 一条路径只有一种描边颜色，重复设置只取最后一次设置颜色
     *
     * @param r 红
     * @param g 绿
     * @param b 蓝
     * @return this
     */
    public DrawContext setStrokeColor(int r, int g, int b) {
        return setStrokeColor(new int[]{r, g, b});
    }

    /**
     * 获取填充颜色（只读）
     * <p>
     * 若填充颜色非颜色值，则返回null
     *
     * @return 填充颜色（只读）
     */
    public int[] getFillColor() {
        if (this.fillStyle instanceof String) {
            return NamedColor.rgb((String) this.fillStyle);
        }
        return null;
    }

    /**
     * 设置填充颜色
     * <p>
     * 一条路径只有一种填充颜色，重复设置只取最后一次设置颜色
     *
     * @param fillColor 填充颜色
     * @return this
     */
    public DrawContext setFillColor(int[] fillColor) {
        this.fillStyle = String.format("#%02X%02X%02X", fillColor[0], fillColor[1], fillColor[2]);
        return this;
    }


    /**
     * 设置填充颜色
     * <p>
     * 一条路径只有一种填充颜色，重复设置只取最后一次设置颜色
     *
     * @param r 红
     * @param g 绿
     * @param b 蓝
     * @return this
     */
    public DrawContext setFillColor(int r, int g, int b) {
        return setFillColor(new int[]{r, g, b});
    }

    /**
     * 获取当前线宽度
     *
     * @return 线宽度（单位毫米mm）
     */
    public double getLineWidth() {
        return this.state.drawParam.getLineWidth();
    }

    /**
     * 获取当前线宽度
     *
     * @param lineWidth 线宽度（单位毫米mm）
     * @return this
     */
    public DrawContext setLineWidth(double lineWidth) {
        if (lineWidth < 0) {
            lineWidth = 0.353;
        }
        this.state.drawParam.setLineWidth(lineWidth);
        return this;
    }

    /**
     * 获取当前使用的绘制文字设置
     *
     * @return 绘制文字设置，可能为null
     */
    public FontSetting getFont() {
        return state.font;
    }

    /**
     * 设置绘制文字信息
     *
     * @param font 文字配置
     * @return this
     */
    public DrawContext setFont(FontSetting font) {
        this.state.font = font;
        // 清空字体样式
        this.font = "";
        return this;
    }

    /**
     * 设置默认字体
     *
     * @param fontSize 字体大小
     * @return this
     */
    public DrawContext setDefaultFont(double fontSize) {
        this.state.font = FontSetting.getInstance(fontSize);
        return this;
    }


    /**
     * 获取绘图透明度值
     *
     * @return 透明度值 0.0到1.0
     */
    public Double getGlobalAlpha() {
        return state.globalAlpha;
    }

    /**
     * 设置 绘图透明度值
     *
     * @param globalAlpha 透明度值 0.0到1.0
     * @return this
     */
    public DrawContext setGlobalAlpha(Double globalAlpha) {
        if (globalAlpha == null || globalAlpha > 1) {
            globalAlpha = 1.0;
        } else if (globalAlpha < 0) {
            globalAlpha = 0d;
        }

        this.state.globalAlpha = globalAlpha;
        return this;
    }


    /**
     * 设置端点样式
     * <p>
     * 默认值： LineCapType.Butt
     *
     * @param cap 端点样式
     * @return this
     */
    public DrawContext setLineCap(LineCapType cap) {
        this.state.drawParam.setCap(cap);
        return this;
    }

    /**
     * 设置端点样式
     * <p>
     * 默认值： LineCapType.Butt
     *
     * @return 端点样式
     */
    public LineCapType getLineCap() {
        return this.state.drawParam.getCap();
    }

    /**
     * 设置线条连接样式，指定了两个线的端点结合时采用的样式。
     * <p>
     * 默认值：LineJoinType.Miter
     *
     * @param join 线条连接样式
     * @return this
     */
    public DrawContext setLineJoin(LineJoinType join) {
        this.state.drawParam.setJoin(join);
        return this;
    }

    /**
     * 获取线条连接样式
     * <p>
     * 默认值：LineJoinType.Miter
     *
     * @return 线条连接样式
     */
    public LineJoinType getLineJoin() {
        return this.state.drawParam.getJoin();
    }

    /**
     * 设置最大斜接长度，也就是结合点长度截断值
     * <p>
     * 默认值：3.528
     * <p>
     * 当Join不等于Miter时改参数无效
     *
     * @param miterLimit 截断值
     * @return this
     */
    public DrawContext setMiterLimit(Double miterLimit) {
        this.state.drawParam.setMiterLimit(miterLimit);
        return this;
    }

    /**
     * 获取最大斜接长度，也就是结合点长度截断值
     * <p>
     * 默认值：3.528
     *
     * @return 截断值
     */
    public Double getMiterLimit() {
        return this.state.drawParam.getMiterLimit();
    }


    /**
     * 设置线段虚线样式
     *
     * @param dashOffset 虚线绘制偏移位置，如果没有则传入null
     * @param pattern    虚线的线段长度和间隔长度,有两个或多个值，第一个值指定了虚线线段的长度，第二个值制定了线段间隔的长度，依次类推。
     * @return this
     */
    public DrawContext setLineDash(Double dashOffset, Double[] pattern) {
        if (dashOffset == null && pattern == null) {
            this.state.drawParam.setDashPattern(null);
            this.state.drawParam.setDashOffset(null);
            return this;
        }

        if (pattern == null || pattern.length < 2) {
            throw new IllegalArgumentException("虚线的线段长度和间隔长度(pattern)，不能为空并且需要大于两个以上的值");
        }

        this.state.drawParam.setDashPattern(new ST_Array(pattern));
        this.state.drawParam.setDashOffset(dashOffset);
        return this;
    }

    /**
     * 设置线段虚线样式
     *
     * @param pattern 虚线的线段长度和间隔长度,有两个或多个值，第一个值指定了虚线线段的长度，第二个值制定了线段间隔的长度，依次类推。
     * @return this
     */
    public DrawContext setLineDash(Double... pattern) {
        return setLineDash(null, pattern);
    }

    /**
     * 获取虚线间隔参数
     *
     * @return 虚线的线段长度和间隔长度, 有两个或多个值，第一个值指定了虚线线段的长度，第二个值制定了线段间隔的长度，依次类推。
     */
    public ST_Array getDashPattern() {
        return this.state.drawParam.getDashPattern();
    }

    /**
     * 获取虚线绘制偏移位置
     *
     * @return 虚线绘制偏移位置
     */
    public Double getDashOffset() {
        return this.state.drawParam.getDashOffset();
    }

    /**
     * 创建一个线性渐变对象（double）
     *
     * @param x0 起始点横坐标
     * @param y0 起始点纵坐标
     * @param x1 结束点横坐标
     * @param y1 结束点纵坐标
     * @return 线性渐变对象
     */
    public CanvasGradient createLinearGradient(double x0, double y0, double x1, double y1) {
        return new CanvasGradient(x0, y0, x1, y1);
    }

    /**
     * 创建一个线性渐变对象（int）
     *
     * @param x0 起始点横坐标
     * @param y0 起始点纵坐标
     * @param x1 结束点横坐标
     * @param y1 结束点纵坐标
     * @return 线性渐变对象
     */
    public CanvasGradient createLinearGradient(int x0, int y0, int x1, int y1) {
        return new CanvasGradient(x0, y0, x1, y1);
    }

    /**
     * 创建一个重复底纹
     * <p>
     * 注意：默认情况下使用 {@link #mm(int)} 将图片像素转换为OFD的单位毫米。
     * 若有自定义重复图片大小设置可以使用 {@link CanvasPattern#setImageSize(double, double)}
     *
     * @param img        底纹图片路径
     * @param repetition 重复方式，支持 repeat、column、row、row-column
     * @return 底纹对象
     * @throws IOException 图片读取异常
     */
    public CanvasPattern createPattern(Path img, String repetition) throws IOException {
        img = img.toAbsolutePath();
        if (Files.notExists(img)) {
            throw new IllegalArgumentException("底纹图片不存在：" + img);
        }
        ST_ID id = resManager.addImage(img);

        // 加载原图片
        BufferedImage gImg = ImageIO.read(img.toFile());
        double w = mm(gImg.getWidth());
        double h = mm(gImg.getHeight());
        // 在公共资源中加入图片
        ImageObject imgObj = new ImageObject(maxUnitID.incrementAndGet());
        imgObj.setResourceID(id.ref());
        imgObj.setBoundary(new ST_Box(0, 0, w, h));
        imgObj.setCTM(new ST_Array(w, 0, 0, h, 0, 0));

        return new CanvasPattern(img, repetition, imgObj);
    }

    /**
     * 创建径向渐变颜色
     *
     * @param x0 渐变的开始圆的 x 坐标
     * @param y0 渐变的开始圆的 y 坐标
     * @param r0 开始圆的半径
     * @param x1 渐变的结束圆的 x 坐标
     * @param y1 渐变的结束圆的 y 坐标
     * @param r1 结束圆的半径
     * @return 径向渐变颜色
     */
    public CanvasRadialGradient createRadialGradient(double x0, double y0, double r0, double x1, double y1, double r1) {
        return new CanvasRadialGradient(x0, y0, r0, x1, y1, r1);
    }


    /**
     * 应用当前上下文中的绘制参数到绘制对象
     */
    private void applyDrawParam(CT_GraphicUnit<?> p) {
        if (p == null) {
            return;
        }
        // 设置区域
        p.setBoundary(this.boundary.clone());

        // 设置透明度
        if (this.state.globalAlpha != null) {
            p.setAlpha((int) (255 * this.state.globalAlpha));
        }

        // 设置变换矩阵 忽略已经设置了变换矩阵的图元
        if (this.state.ctm != null && p.getCTM() == null) {
            p.setCTM(this.state.ctm.clone());
        }
        // 设置填充颜色
        CT_Color fillColor = detectColor(this.fillStyle);
        if (fillColor != null) {
            this.state.drawParam.setFillColor(fillColor);
        }
        // 设置描边颜色
        CT_Color strokeColor = detectColor(this.strokeStyle);
        if (strokeColor != null) {
            this.state.drawParam.setStrokeColor(strokeColor);
        }

        // 设置绘制参数
        ST_ID paramObjId = this.resManager.addDrawParam(this.state.drawParam);
        p.setDrawParam(paramObjId.ref());

        // 设置裁剪区域
        if (this.state.clipArea != null) {
            Clips clips = new Clips();
            org.ofdrw.core.pageDescription.clips.Area area = new org.ofdrw.core.pageDescription.clips.Area();
            CT_Path clipObj = new CT_Path().setAbbreviatedData(this.state.clipArea.clone());
            clipObj.setFill(true);
            // 裁剪区域与Canvas等大
            clipObj.setBoundary(new ST_Box(0, 0, this.boundary.getWidth(), boundary.getHeight()));
            if (this.state.ctm != null && !ONE.equals(this.state.ctm)) {
                // 由于图元内的裁剪区域受到图元的变换矩阵影响，
                // 而裁剪区域是位于未受到变换的原始画布上的区域，
                // 因此在图元内部的裁剪区为需要叠加一个图元内变换的逆变换，
                // 才可以实现向外部空间的映射。
                ST_Array inverse = inverse(this.state.ctm);
                if (inverse == null) {
                    // 获取获取可逆矩阵时放弃裁剪区
                    return;
                }
                clipObj.setCTM(inverse);
            }
            area.setClipObj(clipObj);
            clips.addClip(new CT_Clip().addArea(area));
            p.setClips(clips);
        }
    }

    /**
     * 计算可逆矩阵
     * <p>
     * 注意：初等变换一定存在可逆矩阵
     *
     * @param ctm 变换矩阵
     * @return 可逆矩阵 或 null
     */
    private ST_Array inverse(ST_Array ctm) {
        if (ctm.size() < 6) {
            return null;
        }
        AffineTransform at = new AffineTransform(ctm.get(0), ctm.get(1), ctm.get(2), ctm.get(3), ctm.get(4), ctm.get(5));
        AffineTransform tx = null;
        try {
            tx = at.createInverse();
        } catch (NoninvertibleTransformException e) {
            return null;
        }
        return new ST_Array(tx.getScaleX(), tx.getShearY(), tx.getShearX(), tx.getScaleY(), tx.getTranslateX(), tx.getTranslateY());
    }

    /**
     * 对路径应用变换矩阵
     *
     * @param data 图形轮廓数据
     * @param ctm  变换矩阵
     */
    public static void transform(AbbreviatedData data, ST_Array ctm) {
        AffineTransform at = new AffineTransform(ctm.get(0), ctm.get(1), ctm.get(2), ctm.get(3), ctm.get(4), ctm.get(5));

        for (OptVal optVal : data.getRawOptVal()) {
            switch (optVal.opt) {
                case "S":
                case "M":
                case "L": {
                    double[] arr = optVal.expectValues();
                    double[] dst = new double[2];
                    at.transform(arr, 0, dst, 0, 1);
                    optVal.setValues(dst);
                    continue;
                }
                case "Q": {
                    double[] arr = optVal.expectValues();
                    double[] dst = new double[4];
                    at.transform(arr, 0, dst, 0, 2);
                    optVal.setValues(dst);
                    continue;
                }
                case "B": {
                    double[] arr = optVal.expectValues();
                    double[] dst = new double[6];
                    at.transform(arr, 0, dst, 0, 3);
                    optVal.setValues(dst);
                    continue;
                }
                case "A": {
                    // [0]rx [1]ry [2]angle [3]large [4]sweep [5]x [6]y
                    double[] arr = optVal.expectValues();
                    double rx = arr[0] * at.getScaleX();
                    double ry = arr[1] * at.getScaleY();

                    double[] ptDst = new double[2];
                    at.transform(arr, 5, ptDst, 0, 1);
                    optVal.setValues(new double[]{rx, ry, arr[2], arr[3], arr[4], ptDst[0], ptDst[1]});
                }
                case "C":
                default:
            }
        }
    }

    /**
     * 根据颜色类型推断颜色对象
     * <p>
     * 若无法推断或参数错误则返回null
     *
     * @param color 颜色类型
     * @return OFD颜色对象
     */
    private CT_Color detectColor(Object color) {
        if (color == null) {
            return null;
        }
        if (color instanceof String) {
            int[] rgb = NamedColor.rgb((String) color);
            if (rgb != null) {
                CT_Color c = CT_Color.rgb(rgb[0], rgb[1], rgb[2]);
                if (rgb.length > 3) {
                    // 颜色参数包含透明度，设置透明度
                    c.setAlpha(rgb[3]);
                }
                return c;
            }
            return null;
        } else if (color instanceof ColorClusterType) {
            CT_Color res = new CT_Color();
            res.setColor((ColorClusterType) color);
            return res;
        } else if (color instanceof CT_Color) {
            return (CT_Color) color;
        } else if (color instanceof CanvasGradient) {
            // 渐变颜色
            CT_Color res = new CT_Color();
            res.setColor(((CanvasGradient) color).axialShd);
            return res;
        } else if (color instanceof CanvasPattern) {
            // 图案颜色
            CT_Color res = new CT_Color();
            res.setColor(((CanvasPattern) color).pattern);
            return res;
        } else if (color instanceof CanvasRadialGradient) {
            // 径向渐变颜色
            CT_Color res = new CT_Color();
            res.setColor(((CanvasRadialGradient) color).radialShd);
            return res;
        }
        return null;
    }

    /**
     * 解析字体配置字符串为字体配置对象
     * <p>
     * 解析 font 字符串，格式为：[font-style] [font-weight] font-size font-family [] 表示可选
     * <p>
     * font-style: normal | italic
     * font-weight: normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900
     * font-size: 12px | 3.17mm
     * font-family: 宋体 | SimSun | Times New Roman | Times | serif | sans-serif | monospace | cursive | fantasy
     * font-family 为必选项，其他为可选项
     * <p>
     * font-size 可以使用 px 或 mm 作为单位，若不指定单位则默认为 mm
     *
     * @param fontSettingStr 字体配置字符串
     * @param fs             字体配置对象
     * @return 若文档已经存在字体，那么返回它的字体对象，若不存咋那么只设置 FontSetting 并返还null
     */
    private CT_Font fontStyleToSetting(String fontSettingStr, FontSetting fs) {
        if (fontSettingStr == null || fontSettingStr.isEmpty() || fs == null) {
            return null;
        }
        String[] arr = fontSettingStr.trim().split(" ");
        if (arr.length < 2) {
            return null;
        }

        /*
         * 设置 字体 名称
         */
        int off = arr.length - 1;
        String fontFamily = arr[off].trim();
        Font nameFont = new Font(fontFamily, fontFamily);

        CT_Font ctFont = null;
        ExistCTFont existCTFont = resManager.getFont(fontFamily);
        if (existCTFont != null) {
            ctFont = existCTFont.font;
            if (existCTFont.absPath != null) {
                nameFont = new Font(ctFont.getFontName(), ctFont.getFamilyName(), existCTFont.absPath);
            } else {
                nameFont = new Font(ctFont.getFontName(), ctFont.getFamilyName());
            }
        }
        fs.setFont(nameFont);

        /*
         * 设置 字号
         */
        off--;
        // 解析font-size 单位可能是 px 或 mm 或 空
        String fontSizeStr = arr[off].trim();
        double fontSize = 1;
        try {
            if (fontSizeStr.endsWith("px")) {
                fontSizeStr = fontSizeStr.substring(0, fontSizeStr.length() - 2).trim();
                fontSize = Double.parseDouble(fontSizeStr) / PPM;
            } else if (fontSizeStr.endsWith("mm")) {
                fontSizeStr = fontSizeStr.substring(0, fontSizeStr.length() - 2).trim();
                fontSize = Double.parseDouble(fontSizeStr);
            } else {
                fontSize = Double.parseDouble(fontSizeStr);
            }
        } catch (NumberFormatException e) {
            fontSize = 1;
        }
        fs.setFontSize(fontSize);

        off--;
        if (off < 0) {
            return ctFont;
        }
        /*
         * 设置 字体粗细
         */
        int fontWeight = 400;
        // 解析font-weight
        // normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900
        switch (arr[off].trim().toLowerCase()) {
            case "lighter":
            case "100":
                fontWeight = 100;
                off--;
                break;
            case "200":
                fontWeight = 200;
                off--;
                break;
            case "300":
                fontWeight = 300;
                off--;
                break;
            case "normal":
            case "400":
                fontWeight = 400;
                off--;
                break;
            case "500":
                fontWeight = 500;
                off--;
                break;
            case "600":
                fontWeight = 600;
                off--;
                break;
            case "bold":
            case "700":
                fontWeight = 700;
                off--;
                break;
            case "800":
                fontWeight = 800;
                off--;
                break;
            case "bolder":
            case "900":
                fontWeight = 900;
                off--;
                break;
            default:
                // 非字体宽度配置，忽略
        }
        fs.setFontWeight(fontWeight);
        if (off < 0) {
            return ctFont;
        }
        /*
         * 设置 是否斜体
         */
        // 解析 font-style: normal | italic
        switch (arr[off].trim()) {
            case "italic":
                fs.setItalic(true);
                break;
            case "normal":
            default:
                fs.setItalic(false);
                // 非字体样式配置，忽略
        }

        return ctFont;
    }

    /**
     * 像素转换为毫米
     *
     * @param pixel 像素
     * @return 毫米
     */
    public double mm(int pixel) {
        return (double) pixel / PPM;
    }

    /**
     * 毫米转换为像素
     *
     * @param mm 毫米
     * @return 像素
     */
    public int pixel(double mm) {
        return (int) (mm * PPM);
    }

    /**
     * 添加字体至文档资源中
     *
     * @param name 字体名称
     * @param p    字体文件路径
     * @return this
     * @throws IOException 字体解析异常
     */
    public DrawContext addFont(String name, Path p) throws IOException {
        resManager.addFont(new Font(name, p));
        return this;
    }

    /**
     * 结束绘制器绘制工作
     */
    @Override
    public void close() {

    }
}
