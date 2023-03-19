package org.ofdrw.graphics2d;

import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.CT_GraphicUnit;
import org.ofdrw.core.pageDescription.color.color.*;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

/**
 * 绘制参数上下文
 *
 * @author 权观宇
 * @since 2023-1-30 21:37:36
 */
public class OFDGraphics2DDrawParam {

    /**
     * 文档上下文
     */
    private final OFDGraphicsDocument docCtx;

    /**
     * 工作区大小
     */
    ST_Box area;

    /**
     * 字体
     */
    Font font;

    /**
     * 缓存
     */
    CT_DrawParam pCache;

    /**
     * 引用对象
     */
    ST_RefID ref;

    /**
     * AWT 描边属性（颜色、描边样式）
     */
    BasicStroke gStroke;

    /**
     * AWT 画笔预设
     */
    Paint gColor;

    /**
     * 裁剪区域
     */
    Shape gClip;
    /**
     * 发生裁剪时的变换矩阵
     * <p>
     * 注意：该参数仅在设置裁剪区域时设置，默认情况下保持为null
     */
    ST_Array clipCTM = null;

    /**
     * 背景色
     */
    Color gBackground;

    /**
     * 前景色
     */
    Color gForeground;

    /**
     * 变换矩阵
     */
    ST_Array ctm;
    /**
     * AWT变换矩阵
     * <p>
     * 由于OFD变换矩阵需要左乘于AWT相反，所以使用该副本来存储变换矩阵
     */
    AffineTransform gCtm;
    /**
     * 渲染器信息
     * <p>
     * 该属性只是为了兼容AWT接口保留，并无实际用途。
     */
    RenderingHints hints;

    /**
     * 设置像素合并方式
     * <p>
     * 该属性只是为了兼容AWT接口保留，并无实际用途。
     */
    Composite composite;

    /**
     * 文字绘制上下文
     */
    FontRenderContext fontRenderCtx;

    /**
     * 创建绘制参数
     *
     * @param docCtx 文档上下文
     * @param area   工作区大小
     */
    public OFDGraphics2DDrawParam(OFDGraphicsDocument docCtx, ST_Box area) {
        this.docCtx = docCtx;
        this.pCache = new CT_DrawParam();
        this.gStroke = new BasicStroke(0.353f);

        // 默认 描边颜色为黑色
        this.pCache.setStrokeColor(CT_Color.rgb(0, 0, 0));
        this.gColor = new Color(0, 0, 0);
        this.gBackground = new Color(255, 255, 255);
        this.gForeground = new Color(0, 0, 0);

        this.ctm = ST_Array.unitCTM();
        this.gCtm = new AffineTransform();
        this.ref = null;
        this.gClip = null;

        this.hints = new RenderingHints(null);
        this.composite = AlphaComposite.SrcOver;

        this.font = new Font("sanserif", Font.PLAIN, 3);
        if (area != null) {
            this.area = area.clone();
        }

    }

    /**
     * 创建绘制参数
     * <p>
     * 若您需要使用该方法，请手动设置工作区 {@link #area} 否则默认为A4大小
     *
     * @param docCtx 文档上下文
     * @deprecated {@link OFDGraphics2DDrawParam#OFDGraphics2DDrawParam(OFDGraphicsDocument, ST_Box)}
     */
    @Deprecated
    public OFDGraphics2DDrawParam(OFDGraphicsDocument docCtx) {
        this(docCtx, new ST_Box(0, 0, 210d, 297d));
    }


    /**
     * 设置描边属性
     *
     * @param s 属性参数
     */
    public void setStroke(Stroke s) {
        if (this.ref != null) {
            // 防止同一引用重复添加
            this.pCache = this.pCache.clone();
        }
        // 清空引用缓存
        this.ref = null;
        if (s == null) {
            // 如果为空时设置为默认颜色黑色
            s = new BasicStroke(0.353f);
        }

        if (s instanceof BasicStroke) {
            this.gStroke = (BasicStroke) s;
        }


        // 线条连接样式
        switch (gStroke.getLineJoin()) {
            case BasicStroke.JOIN_BEVEL:
                this.pCache.setJoin(LineJoinType.Bevel);
                break;
            case BasicStroke.JOIN_MITER:
                this.pCache.setJoin(LineJoinType.Miter);
                break;
            case BasicStroke.JOIN_ROUND:
                this.pCache.setJoin(LineJoinType.Round);
                break;
            default:
                this.pCache.setJoin(null);
                break;
        }

        // 线宽度
        if (gStroke.getLineWidth() > 0) {
            this.pCache.setLineWidth((double) gStroke.getLineWidth());
        }

        // 线条虚线重复样式
        final float[] dashArray = gStroke.getDashArray();
        if (dashArray != null && dashArray.length > 0) {
            final ST_Array pattern = new ST_Array();
            for (float v : dashArray) {
                pattern.add(Float.toString(v));
            }
            this.pCache.setDashPattern(pattern);
        } else {
            this.pCache.setDashPattern(null);
        }

        // 线端点样式
        switch (gStroke.getEndCap()) {
            case BasicStroke.CAP_BUTT:
                this.pCache.setCap(LineCapType.Butt);
                break;
            case BasicStroke.CAP_ROUND:
                this.pCache.setCap(LineCapType.Round);
                break;
            case BasicStroke.CAP_SQUARE:
                this.pCache.setCap(LineCapType.Square);
                break;
            default:
                this.pCache.setCap(null);
        }

        // Join的截断值
        final float miterLimit = gStroke.getMiterLimit();
        if (miterLimit > 0) {
            this.pCache.setMiterLimit((double) miterLimit);
        } else {
            this.pCache.setMiterLimit(null);
        }
    }

    /**
     * 设置颜色
     *
     * @param paint 颜色对象
     */
    public void setColor(Paint paint) {
        if (this.gColor == paint) {
            return;
        }
        if (this.ref != null) {
            // 防止同一引用重复添加
            this.pCache = this.pCache.clone();
        }

        // 清空引用缓存
        this.ref = null;
        if (paint == null) {
            // 如果为空时设置为默认颜色黑色
            paint = new Color(0, 0, 0);
        }
        this.gColor = paint;
        CT_Color ctColor = null;
        if (paint instanceof Color) {
            final Color c = (Color) paint;
            ctColor = CT_Color.rgb(c.getRed(), c.getGreen(), c.getBlue());
            int alpha = c.getAlpha();
            if (alpha != 255) {
                ctColor.setAlpha(alpha);
            }
        } else if (paint instanceof LinearGradientPaint) {
            // 线性渐变
            final LinearGradientPaint lgp = (LinearGradientPaint) paint;

            ctColor = new CT_Color();
            CT_AxialShd axialShd = new CT_AxialShd();

            // 轴线起点
            axialShd.setStartPoint(ST_Pos.getInstance(lgp.getStartPoint().getX(), lgp.getStartPoint().getY()));
            // 轴线终点
            axialShd.setEndPoint(ST_Pos.getInstance(lgp.getEndPoint().getX(), lgp.getEndPoint().getY()));

            // 设置颜色段以及分布
            Color[] colors = lgp.getColors();
            float[] fractions = lgp.getFractions();
            for (int i = 0; i < colors.length; i++) {
                CT_Color cc = CT_Color.rgb(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue());
                int alpha = colors[i].getAlpha();
                if (alpha != 255) {
                    cc.setAlpha(alpha);
                }
                axialShd.addSegment(new Segment((double) fractions[i], cc));
            }

            // 设置 渐变绘制的方式
            switch (lgp.getCycleMethod()) {
                case NO_CYCLE:
                    axialShd.setMapType(MapType.Direct);
                    break;
                case REPEAT:
                    axialShd.setMapType(MapType.Repeat);
                    break;
                case REFLECT:
                    axialShd.setMapType(MapType.Reflect);
                    break;
            }

            ctColor.setColor(axialShd);
        } else if (paint instanceof RadialGradientPaint) {
            // 径向渐变
            final RadialGradientPaint rgp = (RadialGradientPaint) paint;

            ctColor = new CT_Color();
            CT_RadialShd radialShd = new CT_RadialShd();
            // 设置颜色段以及分布
            Color[] colors = rgp.getColors();
            float[] fractions = rgp.getFractions();
            for (int i = 0; i < colors.length; i++) {
                CT_Color cc = CT_Color.rgb(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue());
                int alpha = colors[i].getAlpha();
                if (alpha != 255) {
                    cc.setAlpha(alpha);
                }
                radialShd.addSegment(new Segment((double) fractions[i], cc));
            }
            // 设置渐变绘制方式
            switch (rgp.getCycleMethod()) {
                case NO_CYCLE:
                    radialShd.setMapType(MapType.Direct);
                    break;
                case REFLECT:
                    radialShd.setMapType(MapType.Reflect);
                    break;
                case REPEAT:
                    radialShd.setMapType(MapType.Repeat);
                    break;
            }
            // 设置半径
            radialShd.setEndRadius((double) rgp.getRadius());
            // 设置 起始 椭圆中心点
            ST_Pos startPoint = ST_Pos.getInstance(rgp.getCenterPoint().getX(), rgp.getCenterPoint().getY());
            radialShd.setStartPoint(startPoint);
            // 设置 结束 椭圆中心点
            ST_Pos endPoint = ST_Pos.getInstance(rgp.getFocusPoint().getX(), rgp.getFocusPoint().getY());
            radialShd.setEndPoint(endPoint);
            ctColor.setColor(radialShd);
        } else if (paint instanceof GradientPaint) {
            final GradientPaint gp = (GradientPaint) paint;
            ctColor = new CT_Color();
            CT_AxialShd axialShd = new CT_AxialShd();

            // 轴线起点
            axialShd.setStartPoint(ST_Pos.getInstance(gp.getPoint1().getX(), gp.getPoint1().getY()));
            // 轴线终点
            axialShd.setEndPoint(ST_Pos.getInstance(gp.getPoint2().getX(), gp.getPoint2().getY()));

            // 设置 渐变绘制的方式
            if (gp.isCyclic()) {
                axialShd.setMapType(MapType.Repeat);
            }
            // 设置起点终点
            axialShd.addSegment(new Segment(CT_Color.rgb(gp.getColor1().getRed(), gp.getColor1().getGreen(), gp.getColor1().getBlue())));
            axialShd.addSegment(new Segment(CT_Color.rgb(gp.getColor2().getRed(), gp.getColor2().getGreen(), gp.getColor2().getBlue())));

            ctColor.setColor(axialShd);
        }

        if (ctColor != null) {
            // 同时设置填充颜色和描边颜色
            this.pCache.setFillColor(ctColor);
            this.pCache.setStrokeColor(ctColor);
        }
    }

    /**
     * 设置前景色
     *
     * @param c 前景色
     */
    public void setForeground(Color c) {
        this.gForeground = c;
        setColor(c);
    }

    /**
     * 单位矩阵
     * <p>
     * 用于比较
     */
    final static ST_Array ONE = ST_Array.unitCTM();

    /**
     * 在图元上应用绘制参数配置
     * <p>
     * 包括： 描边、变换矩阵、裁剪区域、颜色
     *
     * @param target 目标图元
     */
    public void apply(CT_GraphicUnit<?> target) {
        if (ref == null) {
            // 添加绘制参数至文档资源中，并保存绘制参数在文档对象ID引用
            ref = docCtx.addDrawParam(this.pCache).ref();
        }
        target.setDrawParam(ref);

        // 获取元素上已经存在变换矩阵
        ST_Array ctmApplied = target.getCTM();
        // 设置变换矩阵
        if (!this.ctm.equals(ONE)) {
            if (ctmApplied != null) {
                // 叠加变换矩阵
                ctmApplied = ctmApplied.mtxMul(ctm);
            } else {
                ctmApplied = ctm;
            }
            target.setCTM(ctmApplied);
        }

        // TODO: 如何处理裁剪区域
//        if (gClip != null) {
//            // 设置裁剪区域
//            ST_Box stBox = target.getBoundary();
//            Rectangle2D targetBoundary = new Rectangle2D.Double(stBox.getTopLeftX(), stBox.getTopLeftY(), stBox.getWidth(), stBox.getHeight());
//            Rectangle2D clipsBoundary = gClip.getBounds2D();
//            System.out.printf(">> 图元 %s 裁剪区域 %s\n", targetBoundary, clipsBoundary);
//            if (clipsBoundary.contains(targetBoundary)) {
//                // 如果裁剪区域大小 大于等于图元大小，那么忽略该裁剪区域
//            } else {
//                Clips clips = new Clips();
//                Area area = new Area();
//                // 如果裁剪区域的变换矩阵不为空不为单位矩阵 并且与当前的变换矩阵不一致，
//                // 那么需要设置裁剪区域独立的变换矩阵
//                if (clipCTM != null && !this.ctm.equals(ONE) && !clipCTM.equals(this.ctm)) {
//                    area.setCTM(clipCTM);
//                }
//                // area.setDrawParam(ref); // 绘制参数不影响裁剪区
//                CT_Path clipObj = new CT_Path().setAbbreviatedData(OFDShapes.path(gClip));
//                // 裁剪区域不受所处图元影响，默认使用整个页面作为工作区
//                clipObj.setBoundary(this.area);
//                area.setClipObj(clipObj);
//                clips.addClip(new CT_Clip().addArea(area));
//                target.setClips(clips);
//            }
//        }

    }

    /**
     * 获取字体绘制上下文
     * <p>
     * 字体绘制上线文使用hit中的绘制参数进行控制
     * <p>
     * code from apache batik: org.apache.batik.ext.awt.g2d.AbstractGraphics2D#getFontRenderContext
     *
     * @return 字体绘制上下文
     */
    public FontRenderContext getFontRenderContext() {
        if (this.fontRenderCtx != null) {
            return this.fontRenderCtx;
        }
        // 抗锯齿
        Object antialiasingHint = hints.get(RenderingHints.KEY_TEXT_ANTIALIASING);
        boolean isAntialiased = true;
        if (antialiasingHint != RenderingHints.VALUE_TEXT_ANTIALIAS_ON &&
                antialiasingHint != RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT) {
            // 如果抗锯齿没有被设置为 OFF 那么使用默认方式
            if (antialiasingHint != RenderingHints.VALUE_TEXT_ANTIALIAS_OFF) {
                antialiasingHint = hints.get(RenderingHints.KEY_ANTIALIASING);
                // 判断是否是默认的抗锯齿
                if (antialiasingHint != RenderingHints.VALUE_ANTIALIAS_ON &&
                        antialiasingHint != RenderingHints.VALUE_ANTIALIAS_DEFAULT) {
                    // Antialiasing was not requested. However, if it was not turned
                    // off explicitly, use it.
                    if (antialiasingHint == RenderingHints.VALUE_ANTIALIAS_OFF)
                        isAntialiased = false;
                }
            } else
                isAntialiased = false;

        }

        // 设置是否使用 FRACTIONALMETRICS
        boolean useFractionalMetrics = hints.get(RenderingHints.KEY_FRACTIONALMETRICS) != RenderingHints.VALUE_FRACTIONALMETRICS_OFF;

        this.fontRenderCtx = new FontRenderContext(new AffineTransform(),
                isAntialiased,
                useFractionalMetrics);
        return this.fontRenderCtx;
    }


    /**
     * 复制绘制参数对象
     *
     * @return 复制的新对象
     */
    @Override
    public OFDGraphics2DDrawParam clone() {
        OFDGraphics2DDrawParam that = new OFDGraphics2DDrawParam(this.docCtx, this.area);
        that.pCache = this.pCache.clone();
        that.gColor = this.gColor;
        that.gStroke = this.gStroke;
        that.gBackground = this.gBackground;
        that.gForeground = this.gForeground;

        that.ctm = this.ctm.clone();
        that.gCtm = new AffineTransform(this.gCtm);
        that.gClip = this.gClip;
        if (this.clipCTM != null) {
            that.clipCTM = this.clipCTM.clone();
        }
        that.ref = this.ref;

        that.hints = (RenderingHints) this.hints.clone();
        that.composite = this.composite;
        that.font = this.font;
        return that;
    }
}
