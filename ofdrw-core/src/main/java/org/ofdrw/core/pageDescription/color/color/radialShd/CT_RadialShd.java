package org.ofdrw.core.pageDescription.color.color.radialShd;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.STBase;
import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.pageDescription.color.color.ColorClusterType;
import org.ofdrw.core.pageDescription.color.color.Extend;
import org.ofdrw.core.pageDescription.color.color.MapType;
import org.ofdrw.core.pageDescription.color.color.Segment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 径向渐变
 * <p>
 * 8.3.4.3 径向渐变 图 35 表 30
 * <p>
 * 径向渐变定义了两个离心率和倾斜角度均相同的椭圆，并在椭圆边缘连线
 * 区域内进行渐变绘制的方法。具体算法是，先由起始点椭圆中心点开始绘制
 * 一个起始点颜色的空心矩形，随后沿着中心点连线不断绘制离心率与倾角角度
 * 相同的空心椭圆，颜色由起始点颜色逐渐渐变为结束点颜色，椭圆大小由起始点
 * 椭圆主键变为结束点椭圆。
 * <p>
 * 当轴向渐变某个方向设定为延伸时（Extend 不等于 0），渐变应沿轴在该方向的延长线
 * 延伸到超出裁剪区在该轴线的投影区域为止。当 MapType 为 Direct 时，延伸区域的
 * 渲染颜色使用该方向轴点所在的段的颜色；否则，按照在轴线区域内的渲染规则进行渲染。
 *
 * @author 权观宇
 * @since 2019-11-04 06:45:30
 */
public class CT_RadialShd extends OFDElement implements ColorClusterType {
    public CT_RadialShd(Element proxy) {
        super(proxy);
    }

    public CT_RadialShd() {
        super("RadialShd");
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
    public CT_RadialShd setMapType(MapType mapType) {
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
    public CT_RadialShd setMapUnit(Double mapUnit) {
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
     * 设置 两个椭圆的离心率
     * <p>
     * 椭圆焦距与长轴的比值，取值范围是 [0, 1.0)
     * <p>
     * 默认值为 0，在这种情况下退化为圆
     *
     * @param eccentricity 两个椭圆的离心率，取值范围是 [0, 1.0)
     * @return this
     */
    public CT_RadialShd setEccentricity(Double eccentricity) {
        if (eccentricity == null) {
            this.removeAttr("Eccentricity");
            return this;
        }
        this.addAttribute("Eccentricity", eccentricity.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 两个椭圆的离心率
     * <p>
     * 椭圆焦距与长轴的比值，取值范围是 [0, 1.0)
     * <p>
     * 默认值为 0，在这种情况下退化为圆
     *
     * @return 两个椭圆的离心率，取值范围是 [0, 1.0)，默认值为 0
     */
    public Double getEccentricity() {
        String str = this.attributeValue("Eccentricity");
        if (str == null || str.trim().length() == 0) {
            return 0D;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选 属性】
     * 设置 两个椭圆的倾斜角度
     * <p>
     * 椭圆长轴与 x 轴正向的夹角，单位为度
     * <p>
     * 默认值为 0
     *
     * @param angle 两个椭圆的倾斜角度
     * @return this
     */
    public CT_RadialShd setAngle(Double angle) {
        if (angle == null) {
            this.removeAttr("Angle");
        }
        this.addAttribute("Angle", angle.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 两个椭圆的倾斜角度
     * <p>
     * 椭圆长轴与 x 轴正向的夹角，单位为度
     * <p>
     * 默认值为 0
     *
     * @return 两个椭圆的倾斜角度
     */
    public Double getAngle() {
        String str = this.attributeValue("Angle");
        if (str == null || str.trim().length() == 0) {
            return 0D;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【必选 属性】
     * 设置 起始椭圆的的中心点
     *
     * @param startPoint 起始椭圆的的中心点
     * @return this
     */
    public CT_RadialShd setStartPoint(ST_Pos startPoint) {
        if (startPoint == null) {
            throw new IllegalArgumentException("起始椭圆的的中心点（startPoint）不能为空");
        }
        this.addAttribute("startPoint", startPoint.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 起始椭圆的的中心点
     *
     * @return 起始椭圆的的中心点
     */
    public ST_Pos getStartPoint() {
        String str = this.attributeValue("StartPoint");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("起始椭圆的的中心点（startPoint）不能为空");
        }
        return ST_Pos.getInstance(str);
    }

    /**
     * 【必选 属性】
     * 设置 结束椭圆的的中心点
     *
     * @param endPoint 结束椭圆的的中心点
     * @return this
     */
    public CT_RadialShd setEndPoint(ST_Pos endPoint) {
        if (endPoint == null) {
            throw new IllegalArgumentException("结束椭圆的的中心点（EndPoint）不能为空");
        }
        this.addAttribute("EndPoint", endPoint.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 结束椭圆的的中心点
     *
     * @return 结束椭圆的的中心点
     */
    public ST_Pos getEndPoint() {
        String str = this.attributeValue("EndPoint");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("结束椭圆的的中心点（EndPoint）不能为空");
        }
        return ST_Pos.getInstance(str);
    }

    /**
     * 【可选 属性】
     * 设置 起始椭圆的长半轴
     * <p>
     * 默认值为 0
     *
     * @param startRadius 起始椭圆的长半轴长度
     * @return this
     */
    public CT_RadialShd setStartRadius(Double startRadius) {
        if (startRadius == null) {
            this.removeAttr("StartRadius");
            return this;
        }
        this.addAttribute("StartRadius", STBase.fmt(startRadius));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 起始椭圆的长半轴
     * <p>
     * 默认值为 0
     *
     * @return 起始椭圆的长半轴长度
     */
    public Double getStartRadius() {
        String str = this.attributeValue("StartRadius");
        if (str == null || str.trim().length() == 0) {
            return 0D;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【必选 属性】
     * 设置 结束椭圆的长半轴
     *
     * @param endRadius 结束椭圆的长半轴长度
     * @return this
     */
    public CT_RadialShd setEndRadius(Double endRadius) {
        if (endRadius == null) {
            throw new IllegalArgumentException("结束椭圆的长半轴（EndRadius）不能为空");
        }
        this.addAttribute("EndRadius", STBase.fmt(endRadius));
        return this;
    }

    /**
     * 【必选 属性】
     * 设置 结束椭圆的长半轴
     *
     * @return 结束椭圆的长半轴长度
     */
    public Double getEndRadius() {
        String str = this.attributeValue("EndRadius");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("结束椭圆的长半轴（EndRadius）不能为空");
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
    public CT_RadialShd setExtend(Extend extend) {
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
     * 【必选】
     * 增加 段
     *
     * @param segment 段
     * @return this
     */
    public CT_RadialShd addSegment(Segment segment) {
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
        List<Element> elements = this.getOFDElements("Segment");
        if (elements == null || elements.size() == 0) {
            return Collections.emptyList();
        }
        List<Segment> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(new Segment(item)));
        return res;
    }
}
