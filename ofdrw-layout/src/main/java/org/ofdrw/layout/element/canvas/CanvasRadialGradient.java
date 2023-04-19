package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.color.CT_RadialShd;
import org.ofdrw.core.pageDescription.color.color.Extend;
import org.ofdrw.core.pageDescription.color.color.Segment;

/**
 * 创建放射状/圆形渐变对象
 *
 * @author 权观宇
 * @since 2023-4-19 20:44:27
 */
public class CanvasRadialGradient {

    public final CT_RadialShd radialShd;


    /**
     * 创建一个放射状/圆形渐变
     *
     * @param x0 渐变的开始圆的 x 坐标
     * @param y0 渐变的开始圆的 y 坐标
     * @param r0 开始圆的半径
     * @param x1 渐变的结束圆的 x 坐标
     * @param y1 渐变的结束圆的 y 坐标
     * @param r1 结束圆的半径
     */
    public CanvasRadialGradient(double x0, double y0, double r0, double x1, double y1, double r1) {
        radialShd = new CT_RadialShd();
        radialShd.setStartPoint(ST_Pos.getInstance(x0, y0));
        radialShd.setStartRadius(r0);
        radialShd.setEndPoint(ST_Pos.getInstance(x1, y1));
        radialShd.setEndRadius(r1);
        radialShd.setExtend(Extend._1);
    }


    /**
     * 添加渐变颜色段
     *
     * @param offset 渐变颜色位置，取值范围[0,1] 用于确定StartPoint和EndPoint之间的位置
     * @param color  16进制颜色值 或 颜色名，如#FF0000
     */
    public void addColorStop(double offset, String color) {
        int[] rgb = NamedColor.rgb(color);
        if (rgb == null) {
            return;
        }

        CT_Color c = CT_Color.rgb(rgb[0], rgb[1], rgb[2]);
        if (rgb.length > 3) {
            // 颜色参数包含透明度
            c.setAlpha(rgb[3]);
        }
        radialShd.addSegment(new Segment(offset, c));
    }
}
