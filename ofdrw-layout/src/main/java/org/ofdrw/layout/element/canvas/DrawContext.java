package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.layout.engine.ResManager;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * 描边RGB颜色
     * <p>
     * 默认黑色
     */
    private int[] strokeColor = {0, 0, 0};

    /**
     * 填充RGB颜色
     * <p>
     * 默认黑色
     */
    private int[] fillColor = {0, 0, 0};

    /**
     * 边框位置，也就是画布大小以及位置
     */
    private ST_Box boundary;

    /**
     * 变换矩阵
     */
    private ST_Array ctm;


    /**
     * 线宽度
     * <p>
     * 默认值 0.353 mm
     */
    private double lineWidth = 0.353;


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
    }


    /**
     * 根据上下文属性创建一个Path对象
     *
     * @return 路径对象
     */
    private CT_Path newPathWithCtx() {
        CT_Path path = new CT_Path()
                .setBoundary(this.boundary.clone());
        // 设置描边颜色
        path.setStrokeColor(CT_Color.rgb(this.strokeColor.clone()));
        // 设置填充颜色
        path.setFillColor(CT_Color.rgb(this.fillColor.clone()));
        if (this.ctm != null) {
            path.setCTM(this.ctm);
        }

        // 默认线宽度0.353
        path.setLineWidth(lineWidth);
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
     * 移动绘制点到指定位置
     *
     * @param x X坐标
     * @param y Y坐标
     * @return this
     */
    public DrawContext moveTo(double x, double y) {
        if (this.workPathObj == null) {
            return this;
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
        workPathObj.setStroke(true)
                .setStrokeColor(CT_Color.rgb(this.strokeColor));
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
        workPathObj.setFill(true)
                .setFillColor(CT_Color.rgb(this.fillColor));
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
        if (this.ctm == null) {
            this.ctm = ST_Array.unitCTM();
        }
        ST_Array scale = new ST_Array(scalewidth, 0, 0, scaleheight, 0, 0);
        this.ctm = this.ctm.mtxMul(scale);
        if (this.workPathObj != null) {
            this.workPathObj.setCTM(ctm);
        }
        return this;
    }

    /**
     * 在OFD上绘制图像
     *
     * @param img    要使用的图像
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
        imgObj.setCTM(new ST_Array(width, 0, 0, height, x, y));
        // TODO 透明度设置
        container.addPageBlock(imgObj);

        return this;
    }


    /**
     * 读取当前描边颜色（只读）
     *
     * @return 描边颜色（只读）
     */
    public int[] getStrokeColor() {
        return strokeColor.clone();
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

        this.strokeColor = strokeColor;
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
        return fillColor.clone();
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

        this.fillColor = fillColor;
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
        return lineWidth;
    }

    /**
     * 获取当前线宽度
     *
     * @param lineWidth 线宽度（单位毫米mm）
     * @return this
     */
    public DrawContext setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        if (this.workPathObj != null) {
            this.workPathObj.setLineWidth(lineWidth);
        }
        return this;
    }

    /**
     * 将当前绘制的路径更新到容器中去变为可视化的对象
     */
    private void flush2Canvas() {
        if (workPathObj == null) {
            return;
        }
        PathObject tbAdded = workPathObj.setAbbreviatedData(pathData)
                .toObj(new ST_ID(maxUnitID.incrementAndGet()));
        container.addPageBlock(tbAdded);
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
