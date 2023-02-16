package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
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
     * 待裁剪区域构造工厂
     */
    ClipFactory clipFactory;

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
     * 绘制参数缓存
     * <p>
     * 只有在需要时候才进行创建
     */
    DrawParamCache drawParamCache = null;


    public CanvasState() {
        font = new FontSetting(1d, FontName.SimSun.font());
    }

    /**
     * 获取绘制参数缓存
     * <p>
     * 如果缓存不存在那么创建
     *
     * @return 绘制参数缓存
     */
    public DrawParamCache obtainDrawParamCache() {
        if (drawParamCache == null) {
            drawParamCache = new DrawParamCache();
        }
        return drawParamCache;
    }

    @Override
    public CanvasState clone() {
        CanvasState that = new CanvasState();
        if (path != null) {
            that.path = path.clone();
        }
        if (clipFactory != null) {
            that.clipFactory = clipFactory.clone();
        }
        if (ctm != null) {
            that.ctm = ctm.clone();
        }
        // that.lineWidth = lineWidth;
        if (font != null) {
            that.font = font.clone();
        }
        if (globalAlpha != null) {
            that.globalAlpha = globalAlpha;
        }
        if (drawParamCache != null) {
            that.drawParamCache = drawParamCache.clone();
        }

        return that;
    }
}
