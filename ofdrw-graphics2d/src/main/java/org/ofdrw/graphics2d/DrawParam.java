package org.ofdrw.graphics2d;

import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.CT_GraphicUnit;
import org.ofdrw.core.pageDescription.color.color.CT_AxialShd;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.color.MapType;
import org.ofdrw.core.pageDescription.color.color.Segment;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;

import java.awt.*;


/**
 * 绘制参数上下文
 *
 * @author 权观宇
 * @since 2023-1-30 21:37:36
 */
public class DrawParam {
    private final GraphicsDocument ctx;
    /**
     * 缓存
     */
    CT_DrawParam pCache;

    /**
     * 引用对象
     */
    ST_RefID ref;

    /**
     * AWT 颜色
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
     * 背景颜色
     */
    Color gBackground;


    public DrawParam(GraphicsDocument ctx) {
        this.ctx = ctx;
        this.pCache = new CT_DrawParam();
        this.pCache.setLineWidth(0.353d);
        this.gStroke = new BasicStroke(0.353f);

        // 默认 描边颜色为黑色
        this.pCache.setStrokeColor(CT_Color.rgb(0, 0, 0));
        this.gColor = new Color(0, 0, 0);
        this.gBackground = new Color(255, 255, 255);

        this.ref = null;
        this.gClip = null;
    }

    /**
     * 复制绘制参数
     *
     * @param parent 复制对象
     */
    DrawParam(DrawParam parent) {
        this.ctx = parent.ctx;
        this.pCache = parent.pCache.clone();
        this.gColor = parent.gColor;
        this.gStroke = parent.gStroke;
        this.gClip = parent.gClip;
        this.gBackground = parent.gBackground;
        this.ref = parent.ref;
    }

    /**
     * 设置描边属性
     *
     * @param s 属性参数
     */
    public void setStroke(Stroke s) {
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
        } else {
            this.pCache.setLineWidth(0.353);
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
            ctColor.setAlpha(c.getAlpha());

        } else if (paint instanceof LinearGradientPaint) {
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
                cc.setAlpha(colors[i].getAlpha());
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
//            final RadialGradientPaint rgp = (RadialGradientPaint) paint;
//            float x = (float) rgp.getCenterPoint().getX();
//            float y = (float) rgp.getCenterPoint().getY();
//
//            final int[] colors = new int[rgp.getColors().length];
//            for (int i = 0; i < rgp.getColors().length; i++) {
//                colors[i] = rgp.getColors()[i].getRGB();
//            }
//            final GradientStyle gs = GradientStyle.DEFAULT.withTileMode(awtCycleMethodToSkijaFilterTileMode(rgp.getCycleMethod()));
//            float fx = (float) rgp.getFocusPoint().getX();
//            float fy = (float) rgp.getFocusPoint().getY();
//
//            final Shader shader;
//            if (rgp.getFocusPoint().equals(rgp.getCenterPoint())) {
//                shader = Shader.makeRadialGradient(x, y, rgp.getRadius(), colors, rgp.getFractions(), gs);
//            } else {
//                shader = Shader.makeTwoPointConicalGradient(fx, fy, 0, x, y, rgp.getRadius(), colors, rgp.getFractions(), gs);
//            }
//            this.skijaPaint.setShader(shader);
        } else if (paint instanceof GradientPaint) {
//            final GradientPaint gp = (GradientPaint) paint;
//            float x1 = (float) gp.getPoint1().getX();
//            float y1 = (float) gp.getPoint1().getY();
//            float x2 = (float) gp.getPoint2().getX();
//            float y2 = (float) gp.getPoint2().getY();
//
//            final int[] colors = new int[]{gp.getColor1().getRGB(), gp.getColor2().getRGB()};
//            final GradientStyle gs = (gp.isCyclic())
//                    ? GradientStyle.DEFAULT.withTileMode(FilterTileMode.MIRROR)
//                    : GradientStyle.DEFAULT;
//
//            this.skijaPaint.setShader(Shader.makeLinearGradient(x1, y1, x2, y2, colors, (float[]) null, gs));
        }

        if (ctColor != null) {
            // 同时设置填充颜色和描边颜色
            this.pCache.setFillColor(ctColor);
            this.pCache.setStrokeColor(ctColor);
        }
    }


    /**
     * 应用绘制参数的配置
     */
    public void apply(CT_GraphicUnit<?> target) {
        if (ref == null) {
            // 添加绘制参数至文档资源中，并保存绘制参数在文档对象ID引用
            ref = ctx.addDrawParam(this.pCache).ref();
        }
        target.setDrawParam(ref);

        if (gClip != null) {
            // TODO 设置裁剪区
        }
    }

}
