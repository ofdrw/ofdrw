package org.ofdrw.core.pageDescription.color.color;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Pos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 轴向渐变
 * <p>
 * 在轴向渐变中，颜色渐变沿着一条指定的轴线方向，轴线由起始点和结束点决定，
 * 与这条轴线垂直的直线上的点颜色相同。
 * <p>
 * 当轴向渐变某个方向设定为延伸时（Extend 不等于 0），渐变应沿轴在该方向的延长线
 * 延伸到超出裁剪区在该轴线的投影区域为止。当 MapType 为 Direct 时，延伸区域的
 * 渲染颜色使用该方向轴点所在的段的颜色；否则，按照在轴线区域内的渲染规则进行渲染。
 * <p>
 * 8.3.4.2 轴向渐变 图 29、30 表 29
 *
 * @author 权观宇
 * @since 2019-10-31 06:53:43
 */
public class CT_AxialShd extends OFDElement implements ColorClusterType {
    public CT_AxialShd(Element proxy) {
        super(proxy);
    }

    public CT_AxialShd() {
        super("AxialShd");
    }

    protected CT_AxialShd(String name) {
        super(name);
    }

    /**
     * 【可选 属性】
     * 设置 渐变绘制的方式
     * <p>
     * 可选值参考{@link MapType}
     *
     * @param mapType 绘制方向
     * @return this
     */
    public CT_AxialShd setMapType(MapType mapType) {
        if (mapType == null) {
            this.removeAttr("MapType");
            return this;
        }
        this.addAttribute("MapType", mapType.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 渐变绘制的方式
     * <p>
     * 可选值参考{@link MapType}
     *
     * @return 绘制方向
     */
    public MapType getMapType() {
        return MapType.getInstance(this.attributeValue("MapType"));
    }

    /**
     * 【可选 属性】
     * 设置 轴线一个渐变区间的长度
     * <p>
     * 当 MapType 的值不等于 Direct 时出现
     * <p>
     * 默认值为轴线长度
     *
     * @param mapUnit 轴线一个渐变区间的长度
     * @return this
     */
    public CT_AxialShd setMapUnit(Double mapUnit) {
        if (mapUnit == null) {
            this.removeAttr("MapUnit");
            return this;
        }
        this.addAttribute("MapUnit", mapUnit.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 轴线一个渐变区间的长度
     * <p>
     * 当 MapType 的值不等于 Direct 时出现
     * <p>
     * 默认值为轴线长度
     *
     * @return 轴线一个渐变区间的长度
     */
    public Double getMapUnit() {
        String str = this.attributeValue("MapUnit");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选 属性】
     * 设置 轴线延长线方向是否继续绘制
     * <p>
     * 可选值参考{@link Extend}
     * <p>
     * 默认值为 {@link Extend#_0}  不向两侧继续绘制渐变
     *
     * @param extend 轴线延长线方向是否继续绘制
     * @return this
     */
    public CT_AxialShd setExtend(Extend extend) {
        if (extend == null) {
            this.removeAttr("Extend");
            return this;
        }
        this.addAttribute("Extend", extend.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 轴线延长线方向是否继续绘制
     * <p>
     * 默认值为 {@link Extend#_0}  不向两侧继续绘制渐变
     *
     * @return 轴线延长线方向是否继续绘制，选值参考{@link Extend}
     */
    public Extend getExtend() {
        return Extend.getInstance(this.attributeValue("Extend"));
    }

    /**
     * 【必选 属性】
     * 设置 轴线起始点
     *
     * @param startPoint 轴线起始点
     * @return this
     */
    public CT_AxialShd setStartPoint(ST_Pos startPoint) {
        if (startPoint == null) {
            throw new IllegalArgumentException("轴线起始点（StartPoint）不能为空");
        }
        this.addAttribute("StartPoint", startPoint.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 轴线起始点
     *
     * @return 轴线起始点
     */
    public ST_Pos getStartPoint() {
        return ST_Pos.getInstance(this.attributeValue("StartPoint"));
    }

    /**
     * 【必选 属性】
     * 设置 轴线结束点
     *
     * @param endPoint 轴线结束点
     * @return this
     */
    public CT_AxialShd setEndPoint(ST_Pos endPoint) {
        if (endPoint == null) {
            throw new IllegalArgumentException("轴线结束点（StartPoint）不能为空");
        }
        this.addAttribute("EndPoint", endPoint.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 设置 轴线结束点
     *
     * @return 轴线结束点
     */
    public ST_Pos getEndPoint() {
        return ST_Pos.getInstance(this.attributeValue("EndPoint"));
    }

    /**
     * 【必选】
     * 增加 段
     *
     * @param segment 段
     * @return this
     */
    public CT_AxialShd addSegment(Segment segment) {
        if (segment == null) {
            throw new IllegalArgumentException("段（Segment）为空");
        }
        this.add(segment);
        return this;
    }

    /**
     * 【必选】
     * 获取 段列表
     *
     * @return 段列表
     */
    public List<Segment> getSegments() {
        return this.getOFDElements("Segment", Segment::new);
    }
}
