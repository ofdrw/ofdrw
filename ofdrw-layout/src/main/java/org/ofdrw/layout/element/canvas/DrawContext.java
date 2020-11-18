package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.text.CT_Text;
import org.ofdrw.core.text.text.Direction;
import org.ofdrw.core.text.text.Weight;
import org.ofdrw.font.Font;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.engine.ResManager;

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
 *
 * @author 权观宇
 * @since 2020-05-01 11:29:20
 */
public class DrawContext implements Closeable {

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
     * 路径对象
     */
    private CT_Path workPathObj = null;

    /**
     * 路径数据
     * <p>
     * 他与workPathObj 成对出现和存在
     */
    private AbbreviatedData pathData = null;

    /**
     * 边框位置，也就是画布大小以及位置
     */
    private ST_Box boundary;

    /**
     * 画布状态
     */
    private CanvasState state;

    private LinkedList<CanvasState> stack;

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
    public DrawContext(CT_PageBlock container,
                       ST_Box boundary,
                       AtomicInteger maxUnitID,
                       ResManager resManager) {
        this.container = container;
        this.boundary = boundary;
        this.maxUnitID = maxUnitID;
        this.resManager = resManager;
        this.state = new CanvasState();
        this.stack = new LinkedList<>();
    }


    /**
     * 根据上下文属性创建一个Path对象
     *
     * @return 路径对象
     */
    private CT_Path newPathWithCtx() {
        CT_Path path = new CT_Path()
                .setBoundary(this.boundary.clone());
        // 变换矩阵
        if (this.state.ctm != null) {
            path.setCTM(this.state.ctm);
        }
        // 裁剪区域
        if (this.state.clipFactory != null) {
            path.setClips(state.clipFactory.clips());
        }
        // 透明度
        if (this.state.globalAlpha != null) {
            path.setAlpha((int) (255 * this.state.globalAlpha));
        }

        return path;
    }

    /**
     * 开启一段新的路径
     * <p>
     * 如果已经存在路径，那么将会关闭已经存在的路径
     *
     * @return this
     */
    public DrawContext beginPath() {
        if (this.workPathObj != null) {
            // 如果已经存在路径，将上一个路径更新到画板中
            flush2Canvas();
            this.workPathObj = null;
        }
        this.workPathObj = newPathWithCtx();
        this.pathData = new AbbreviatedData();
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
        if (this.workPathObj == null) {
            return this;
        }
        // 创建路径对象，放入图形容器中
        pathData.close();
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
        if (this.workPathObj == null) {
            return this;
        }
        if (state.clipFactory == null) {
            state.clipFactory = new ClipFactory();
        }

        if (this.state.ctm != null) {
            state.clipFactory.setCtm(this.state.ctm.clone());
        }
        state.clipFactory.setData(pathData.clone());

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
        if (this.workPathObj == null) {
            beginPath();
        }
        this.pathData.moveTo(x, y);
        return this;
    }

    /**
     * 从当前点连线到指定点
     *
     * @param x X坐标
     * @param y Y坐标
     * @return this
     */
    public DrawContext lineTo(double x, double y) {
        if (this.workPathObj == null) {
            return this;
        }
        this.pathData.lineTo(x, y);
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
        if (this.workPathObj == null) {
            return this;
        }
        this.pathData.quadraticBezier(cpx, cpy, x, y);
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
    public DrawContext bezierCurveTo(double cp1x, double cp1y,
                                     double cp2x, double cp2y,
                                     double x, double y) {
        if (this.workPathObj == null) {
            return this;
        }
        this.pathData.cubicBezier(cp1x, cp1y, cp2x, cp2y, x, y);
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
    public DrawContext arc(double a, double b,
                           double angle,
                           boolean large,
                           boolean sweep,
                           double x, double y) {
        if (this.workPathObj == null) {
            return this;
        }
        this.pathData.arc(a, b, angle % 360, large ? 1 : 0, sweep ? 1 : 0, x, y);
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
    public DrawContext arc(double x, double y,
                           double r,
                           double sAngle, double eAngle,
                           boolean counterclockwise) {
        if (this.workPathObj == null) {
            return this;
        }

        // 首先移动点到起始位置
        double x1 = x + r * Math.cos(sAngle * Math.PI / 180);
        double y1 = y + r * Math.sin(sAngle * Math.PI / 180);
        this.moveTo(x1, y1);


        double angle = eAngle - sAngle;
        if (angle == 360) {
            // 整个圆的时候需要分为两次路径进行绘制
            // 绘制结束位置起始位置
            this.pathData.arc(r, r, angle, 1, counterclockwise ? 1 : 0, x - r, y)
                    .arc(r, r, angle, 1, counterclockwise ? 1 : 0, x1, y1);
        } else {
            // 绘制结束位置起始位置
            double x2 = x + r * Math.cos(eAngle * Math.PI / 180);
            double y2 = y + r * Math.sin(eAngle * Math.PI / 180);
            this.pathData.arc(r, r, angle,
                    angle > 180 ? 1 : 0,
                    counterclockwise ? 1 : 0,
                    x2, y2);
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
    public DrawContext arc(double x, double y,
                           double r,
                           double sAngle, double eAngle) {
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
        if (this.workPathObj == null) {
            this.beginPath();
        }

        this.pathData.moveTo(x, y)
                .lineTo(x + width, y)
                .lineTo(x + width, y + height)
                .lineTo(x, y + height)
                .close();
        return this;
    }

    /**
     * 创建并填充矩形路径
     * <p>
     * 默认的填充颜色是黑色。
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
        this.beginPath();
        // 创建路径,填充颜色
        rect(x, y, width, height).fill();
        flush2Canvas();
        return this;
    }

    /**
     * 创建并描边矩形路径
     * <p>
     * 如果已经存在路径那么改路径将会提前关闭，并创建新的路径。
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
        this.beginPath();
        // 创建路径
        rect(x, y, width, height).stroke();
        flush2Canvas();
        return this;
    }

    /**
     * 绘制已定义的路径
     *
     * @return this
     */
    public DrawContext stroke() {
        if (this.workPathObj == null) {
            return this;
        }
        // 设置线宽度，默认值0.353
        workPathObj.setLineWidth(state.lineWidth)
                .setStroke(true);
        if (this.state.strokeColor != null) {
            workPathObj.setStrokeColor(CT_Color.rgb(this.state.strokeColor));
        }

        return this;
    }

    /**
     * 填充已定义路径
     * <p>
     * 默认的填充颜色是黑色。
     *
     * @return this
     */
    public DrawContext fill() {
        if (this.workPathObj == null) {
            return this;
        }
        workPathObj.setFill(true);
        if (this.state.fillColor != null) {
            workPathObj.setFillColor(CT_Color.rgb(this.state.fillColor));
        }

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
        if (this.workPathObj != null) {
            this.workPathObj.setCTM(state.ctm);
        }
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
        ST_Array r = new ST_Array(
                Math.cos(alpha), Math.sin(alpha),
                -Math.sin(alpha), Math.cos(alpha),
                0, 0);
        this.state.ctm = r.mtxMul(this.state.ctm);
        if (this.workPathObj != null) {
            this.workPathObj.setCTM(state.ctm);
        }
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
        ST_Array r = new ST_Array(
                1, 0,
                0, 1,
                x, y);
        this.state.ctm = r.mtxMul(this.state.ctm);
        if (this.workPathObj != null) {
            this.workPathObj.setCTM(state.ctm);
        }
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
        ST_Array r = new ST_Array(
                a, b,
                c, d,
                e, f);
        this.state.ctm = r.mtxMul(this.state.ctm);
        if (this.workPathObj != null) {
            this.workPathObj.setCTM(state.ctm);
        }
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
        this.state.ctm = new ST_Array(
                a, b,
                c, d,
                e, f);
        if (this.workPathObj != null) {
            this.workPathObj.setCTM(state.ctm);
        }
        return this;
    }

    /**
     * 在OFD上绘制图像
     *
     * @param img    要使用的图像，请避免资源和文档中已经存在的资源重复
     * @param x      在画布上放置图像的 x 坐标位置
     * @param y      在画布上放置图像的 y 坐标位置
     * @param width  要使用的图像的宽度（伸展或缩小图像）
     * @param height 要使用的图像的高度（伸展或缩小图像）
     * @return this
     * @throws IOException 图片文件读写异常
     */
    public DrawContext drawImage(Path img,
                                 double x, double y,
                                 double width, double height) throws IOException {
        if (img == null || Files.notExists(img)) {
            throw new IOException("图片(img)不存在");
        }

        ST_ID id = resManager.addImage(img);
        // 在公共资源中加入图片
        ImageObject imgObj = new ImageObject(maxUnitID.incrementAndGet());
        imgObj.setResourceID(id.ref());
        imgObj.setBoundary(boundary.clone());
        ST_Array ctm = this.state.ctm == null ? ST_Array.unitCTM() : this.state.ctm;
        ctm = new ST_Array(width, 0, 0, height, x, y).mtxMul(ctm);
        imgObj.setCTM(ctm);

        // 透明度
        if (this.state.globalAlpha != null) {
            imgObj.setAlpha((int) (255 * this.state.globalAlpha));
        }
        container.addPageBlock(imgObj);

        return this;
    }


    /**
     * 保存当前绘图状态
     *
     * @return this
     */
    public DrawContext save() {
        stack.push(this.state.clone());
        return this;
    }

    /**
     * 还原绘图状态
     *
     * @return this
     */
    public DrawContext restore() {
        if (stack.isEmpty()) {
            return this;
        }
        this.state = stack.pop();
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

        int readDirection = state.font.getReadDirection();
        int charDirection = state.font.getCharDirection();

        Font font = state.font.getFont();
        Double fontSize = state.font.getFontSize();

        ST_ID id = resManager.addFont(font);

        // 新建字体对象
        TextObject txtObj = new CT_Text()
                .setBoundary(this.boundary.clone())
                .setFont(id.ref())
                .setSize(fontSize)
                .toObj(new ST_ID(maxUnitID.incrementAndGet()));
        // 设置字体宽度
        if (state.font.getFontWeight() != null && state.font.getFontWeight() != 400) {
            txtObj.setWeight(Weight.getInstance(state.font.getFontWeight()));
        }
        // 是否斜体
        if (state.font.isItalic()) {
            txtObj.setItalic(true);
        }
        // 设置颜色
        if (state.fillColor != null) {
            txtObj.setFillColor(CT_Color.rgb(state.fillColor));
        }
        // 设置变换矩阵
        if (state.ctm != null) {
            txtObj.setCTM(state.ctm.clone());
        }
        // 设置透明度
        if (state.globalAlpha != null && state.globalAlpha != 1) {
            txtObj.setAlpha((int) (255 * this.state.globalAlpha));
        }
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
        TextCode tcSTTxt = new TextCode()
                .setContent(text)
                .setX(xx)
                .setY(yy);

        if (readDirection == 90 || readDirection == 270) {
            tcSTTxt.setDeltaY(measureBody.offset);
        } else {
            tcSTTxt.setDeltaX(measureBody.offset);
        }
        txtObj.addTextCode(tcSTTxt);
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
        TextMetrics tm = new TextMetrics();
        tm.readDirection = state.font.getReadDirection();
        tm.fontSize = state.font.getFontSize();
        // 测量字间距
        Double[] offset = TextMeasureTool.measure(text, state.font);
        if (offset.length == 0) {
            tm.width = 0d;
            return tm;
        }
        tm.width = TextMeasureTool.measureWithWith(text, state.font).width;
        return tm;
    }


    /**
     * 读取当前描边颜色（只读）
     *
     * @return 描边颜色（只读）
     */
    public int[] getStrokeColor() {
        return state.strokeColor.clone();
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
        if (strokeColor == null) {
            return this;
        }

        this.state.strokeColor = strokeColor;
        if (this.workPathObj != null) {
            this.workPathObj.setStrokeColor(CT_Color.rgb(strokeColor));
        }
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
     *
     * @return 填充颜色（只读）
     */
    public int[] getFillColor() {
        return state.fillColor.clone();
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
        if (fillColor == null) {
            return this;
        }

        this.state.fillColor = fillColor;
        if (this.workPathObj != null) {
            this.workPathObj.setFillColor(CT_Color.rgb(fillColor));
        }
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
        return state.lineWidth;
    }

    /**
     * 获取当前线宽度
     *
     * @param lineWidth 线宽度（单位毫米mm）
     * @return this
     */
    public DrawContext setLineWidth(double lineWidth) {
        this.state.lineWidth = lineWidth;
        if (this.workPathObj != null) {
            this.workPathObj.setLineWidth(lineWidth);
        }
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
        if (cap == null) {
            return this;
        }
        this.state.obtainDrawParamCache()
                .setCap(cap);
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
        if (this.state.drawParamCache == null) {
            return LineCapType.Butt;
        }
        LineCapType cap = this.state.drawParamCache.getCap();
        return cap == null ? LineCapType.Butt : cap;
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
        if (join == null) {
            return this;
        }
        this.state.obtainDrawParamCache()
                .setJoin(join);
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
        if (this.state.drawParamCache == null) {
            return LineJoinType.Miter;
        }
        LineJoinType join = this.state.drawParamCache.getJoin();
        return join == null ? LineJoinType.Miter : join;
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
        if (miterLimit == null) {
            return this;
        }
        this.state.obtainDrawParamCache()
                .setMiterLimit(miterLimit);
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
        if (this.state.drawParamCache == null) {
            return 3.528;
        }
        Double miterLimit = this.state.drawParamCache.getMiterLimit();
        return miterLimit == null ? 3.528 : miterLimit;
    }


    /**
     * 设置线段虚线样式
     *
     * @param dashOffset 虚线绘制偏移位置，如果没有则传入null
     * @param pattern    虚线的线段长度和间隔长度,有两个或多个值，第一个值指定了虚线线段的长度，第二个值制定了线段间隔的长度，依次类推。
     * @return this
     */
    public DrawContext setLineDash(Double dashOffset, Double[] pattern) {

        if (pattern == null || pattern.length < 2) {
            throw new IllegalArgumentException("虚线的线段长度和间隔长度(pattern)，不能为空并且需要大于两个以上的值");
        }

        DrawParamCache drawParam = this.state.obtainDrawParamCache()
                .setDashPattern(new ST_Array(pattern));
        if (dashOffset != null) {
            drawParam.setDashOffset(dashOffset);
        }
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
        if (this.state.drawParamCache == null) {
            return null;
        }
        return this.state.drawParamCache.getDashPattern();
    }

    /**
     * 获取虚线绘制偏移位置
     *
     * @return 虚线绘制偏移位置
     */
    public Double getDashOffset() {
        if (this.state.drawParamCache == null) {
            return null;
        }
        return this.state.drawParamCache.getDashOffset();
    }

    /**
     * 将当前绘制的路径更新到容器中去变为可视化的对象
     */
    private void flush2Canvas() {
        if (workPathObj == null) {
            return;
        }
        if (workPathObj.getStroke() || workPathObj.getFill()) {
            PathObject tbAdded = workPathObj.setAbbreviatedData(pathData)
                    .toObj(new ST_ID(maxUnitID.incrementAndGet()));
            // 如果存在绘制参数，那么设置图元的绘制参数
            if (this.state.drawParamCache != null) {
                ST_ID paramObjId = this.state.drawParamCache.addToResource(resManager);
                tbAdded.setDrawParam(paramObjId.ref());
            }
            container.addPageBlock(tbAdded);
        }

        this.workPathObj = null;
        this.pathData = null;
    }

    /**
     * 结束绘制器绘制工作
     */
    @Override
    public void close() {
        if (this.workPathObj == null) {
            return;
        }
        flush2Canvas();
    }
}
