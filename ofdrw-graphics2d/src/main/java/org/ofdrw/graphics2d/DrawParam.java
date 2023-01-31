package org.ofdrw.graphics2d;

import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.CT_GraphicUnit;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;

import java.awt.*;


/**
 * 绘制参数缓存属性是 {@link CT_DrawParam} 复制属性
 *
 * @author 权观宇
 * @since 2023-1-30 21:37:36
 */
public class DrawParam {

    Double lineWidth;
    LineJoinType join;
    LineCapType cap;
    Double dashOffset;
    ST_Array dashPattern;
    Double miterLimit;
    CT_Color fillColor;
    CT_Color strokeColor;

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


    public DrawParam() {
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
     * @param parent
     */
    DrawParam(DrawParam parent) {
        this();
        this.lineWidth = parent.lineWidth;
        this.join = parent.join;
        this.cap = parent.cap;
        this.dashOffset = parent.dashOffset;
        this.dashPattern = parent.dashPattern;
        this.miterLimit = parent.miterLimit;
        this.fillColor = parent.fillColor;
        this.strokeColor = parent.strokeColor;

        this.gColor = parent.gColor;
        this.gStroke = parent.gStroke;
        this.gClip = parent.gClip;
        this.gBackground = parent.gBackground;
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

        // TODO 颜色对象
    }


    /**
     * 应用绘制参数的配置
     */
    public void apply(CT_GraphicUnit<?> target) {
        if (ref != null) {
            target.setDrawParam(ref);
        } else {
            // TODO 创建添加到资源中，资源管理器，创建ID
        }

        if (gClip != null) {
//            target.setClips()
            // TODO 设置裁剪区
        }

        return;
    }


}
