package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;
import org.ofdrw.layout.engine.ResManager;


/**
 * 绘制参数缓存
 *
 * @author 权观宇
 * @since 2020-05-18 20:45:32
 */
public class DrawParamCache {


    private CT_DrawParam cache;
    /**
     * 一下属性是 CT_DrawParam 复制属性
     * <p>
     * 参数意义请参考 CT_DrawParam
     */
    private ST_RefID relative;
    private Double lineWidth;
    private LineJoinType join;
    private LineCapType cap;
    private Double dashOffset;
    private ST_Array dashPattern;
    private Double miterLimit;
    private int[] fillColor;
    private int[] strokeColor;

    public DrawParamCache() {
        // 默认黑色
        fillColor = new int[]{0, 0, 0};
        strokeColor = new int[]{0, 0, 0};
    }


    public ST_ID getID() {
        if (cache == null) {
            return null;
        }
        return cache.getID();
    }


    public DrawParamCache setRelative(ST_RefID relative) {
        cache = null;
        this.relative = relative;
        return this;
    }


    public ST_RefID getRelative() {
        return this.relative;
    }


    public DrawParamCache setLineWidth(Double lineWidth) {
        cache = null;
        this.lineWidth = lineWidth;
        return this;
    }


    public Double getLineWidth() {
        return lineWidth;
    }


    public DrawParamCache setJoin(LineJoinType join) {
        cache = null;
        this.join = join;
        return this;
    }


    public LineJoinType getJoin() {
        return join;
    }


    public DrawParamCache setCap(LineCapType cap) {
        cache = null;
        this.cap = cap;
        return this;
    }


    public LineCapType getCap() {
        return cap;
    }


    public DrawParamCache setDashOffset(Double dashOffset) {
        cache = null;
        this.dashOffset = dashOffset;
        return this;
    }


    public Double getDashOffset() {
        return dashOffset;
    }


    public DrawParamCache setDashPattern(ST_Array dashPattern) {
        cache = null;
        this.dashPattern = dashPattern;
        return this;
    }


    public ST_Array getDashPattern() {
        return dashPattern;
    }


    public DrawParamCache setMiterLimit(Double miterLimit) {
        cache = null;
        this.miterLimit = miterLimit;
        return this;
    }


    public Double getMiterLimit() {
        return miterLimit;
    }


    public DrawParamCache setFillColor(int[] fillColor) {
        cache = null;
        this.fillColor = fillColor;
        return this;
    }


    public int[] getFillColor() {
        return fillColor;
    }

    public DrawParamCache setStrokeColor(int[] strokeColor) {
        cache = null;
        this.strokeColor = strokeColor;
        return this;
    }

    public int[] getStrokeColor() {
        return strokeColor;
    }


    /**
     * 构造绘制参数并加入资源中
     * <p>
     * 如果绘制参数内部属性没有修改过，那么不会重复加入资源
     *
     * @param resManager 资源管理器
     * @return 绘制参数对象
     */
    public ST_ID addToResource(ResManager resManager) {
        if (cache == null) {
            this.cache = new CT_DrawParam();
            if (relative != null) {
                cache.setRelative(relative);
            }
            if (lineWidth != null) {
                cache.setLineWidth(lineWidth);
            }
            if (join != null) {
                cache.setJoin(join);
            }
            if (cap != null) {
                cache.setCap(cap);
            }
            if (dashOffset != null) {
                cache.setDashOffset(dashOffset);
            }
            if (dashPattern != null) {
                cache.setDashPattern(dashPattern);
            }
            if (miterLimit != null) {
                cache.setMiterLimit(miterLimit);
            }
            if (fillColor != null) {
                cache.setFillColor(CT_Color.rgb(fillColor));
            }
            if (strokeColor != null) {
                cache.setStrokeColor(CT_Color.rgb(strokeColor));
            }
            // 加入到资源问价中并且返还对象ID
            return resManager.addDrawParam(cache);
        }
        return cache.getID();
    }


    @Override
    public DrawParamCache clone() {
        DrawParamCache that = new DrawParamCache();
        that.cache = cache;
        that.relative = relative;
        that.lineWidth = lineWidth;
        that.join = join;
        that.cap = cap;
        that.dashOffset = dashOffset;
        that.dashPattern = dashPattern;
        that.miterLimit = miterLimit;
        that.fillColor = fillColor.clone();
        that.strokeColor = strokeColor.clone();
        return that;
    }
}
