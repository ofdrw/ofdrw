package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.font.FontName;

/**
 * 画布上下文中的绘制参数状态
 *
 * @author 权观宇
 * @since 2020-05-06 19:22:15
 */
public class CanvasState implements Cloneable {

    /**
     * 上下文 路径数据
     */
    AbbreviatedData path;

    /**
     * 变换矩阵
     */
    ST_Array ctm = null;

    /**
     * 绘制文字设置
     * <p>
     * 默认为宋体，字号为1
     */
    FontSetting font = null;


    /**
     * 透明值。必须介于 0.0（完全透明） 与 1.0（不透明） 之间。
     */
    Double globalAlpha = null;

    /**
     * 绘制参数
     */
    CT_DrawParam drawParam;

    /**
     * 裁剪区域
     */
    AbbreviatedData clipArea = null;

    /**
     * 填充颜色 16进制格式
     * 如： #000000
     */
    Object fillStyle;

    /**
     * 描边颜色 16进制格式
     * 如： #000000
     */
    Object strokeStyle;

    /**
     * 字体样式
     */
    String fontStyle;

    public CanvasState() {
        drawParam = new CT_DrawParam();
        font = new FontSetting(1d, FontName.SimSun.font());
    }

    /**
     * 获取绘制参数缓存
     * <p>
     * 如果缓存不存在那么创建
     *
     * @return 绘制参数缓存
     * @deprecated 采用 {@link #getDrawParam()}
     */
    @Deprecated
    public DrawParamCache obtainDrawParamCache() {
        return  new DrawParamCache();
    }

    /**
     * 获取绘制参数
     *
     * @return 绘制参数
     */
    public CT_DrawParam getDrawParam() {
        return  this.drawParam;
    }


    @Override
    public CanvasState clone() {
        CanvasState that = new CanvasState();
        if (path != null) {
            that.path = path.clone();
        }
        if (ctm != null) {
            that.ctm = ctm.clone();
        }
        if (font != null) {
            that.font = font.clone();
        }
        if (globalAlpha != null) {
            that.globalAlpha = globalAlpha;
        }
        if (this.drawParam != null){
            this.drawParam = this.drawParam.clone();
        }
        if (clipArea != null) {
            that.clipArea = clipArea.clone();
        }
        that.fillStyle = this.fillStyle;
        that.strokeStyle = this.strokeStyle;
        that.fontStyle = this.fontStyle;
        return that;
    }
}
