package org.ofdrw.converter.pdf.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 渐变填充指令
 * <p>
 * 对应 PDF 的 Shading Pattern：{@code q} + 裁剪到填充区域 + {@code sh}。设计 3.3 节与第 2 节表格要求
 * 支持 Extend、MapType、MapUnit、Segment 缺省均分等完整语义；当 PDF Shading 无法直接表达（如 Repeat/Reflect）
 * 时，由渲染器自行分片，本指令仍只承载“一份渐变”。
 * <p>
 * 颜色停止点按 {@link #getStops()} 的顺序排列，位置单调递增。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ShadingCommand extends AbstractCommand {

    /**
     * 渐变类型
     */
    private final ShadingType shadingType;

    /**
     * 颜色停止点
     */
    private final List<ColorStop> stops;

    /**
     * 扩展方式（PDF Extend 0~3 对应的 OFD Extend）
     */
    private final Extend extend;

    /**
     * 填充区域：渐变作用的路径
     */
    private final List<PathSegment> area;

    /**
     * 填充规则（Path 或 Text 裁剪区域的填充规则）
     */
    private final FillRule fillRule;

    /**
     * 起点横坐标，单位 pt（PDF 页面坐标）
     */
    private final double startX;

    /**
     * 起点纵坐标，单位 pt（PDF 页面坐标）
     */
    private final double startY;

    /**
     * 终点横坐标，单位 pt（PDF 页面坐标）
     */
    private final double endX;

    /**
     * 终点纵坐标，单位 pt（PDF 页面坐标）
     */
    private final double endY;

    /**
     * 径向渐变：离心率，1 为正圆
     */
    private final double eccentricity;

    /**
     * 径向渐变：起始圆半径，单位 pt；负数表示“由起止圆心距离推导”
     */
    private final double startRadiusPt;

    /**
     * 径向渐变：结束圆半径，单位 pt；负数表示“由起止圆心距离推导”
     */
    private final double endRadiusPt;

    /**
     * 径向渐变：旋转角度，单位度
     */
    private final double angleDeg;

    /**
     * 构造渐变指令
     *
     * @param shadingType  渐变类型，不能为空
     * @param stops        颜色停止点，至少 2 个
     * @param extend       扩展方式，为空按 {@link Extend#NONE}
     * @param area         填充区域路径，不能为空，首元素必须是 {@link MoveTo}
     * @param fillRule     填充规则，为空按 {@link FillRule#NON_ZERO}
     * @param startX       起点横坐标，单位 pt
     * @param startY       起点纵坐标，单位 pt
     * @param endX         终点横坐标，单位 pt
     * @param endY         终点纵坐标，单位 pt
     * @param eccentricity 离心率，正圆为 1
     * @param angleDeg     旋转角度，单位度
     */
    public ShadingCommand(ShadingType shadingType, List<ColorStop> stops, Extend extend,
                          List<PathSegment> area, FillRule fillRule,
                          double startX, double startY, double endX, double endY,
                          double eccentricity, double angleDeg) {
        this(shadingType, stops, extend, area, fillRule, startX, startY, endX, endY, eccentricity, angleDeg, -1, -1);
    }

    /**
     * 构造渐变指令（显式指定径向渐变的起止圆半径）
     * <p>
     * OFD 的径向渐变以 {@code StartRadius}/{@code EndRadius} 描述圆半径，允许起止圆心重合
     * （同心圆渐变）；仅用圆心距表示半径时，同心圆会退化为半径 0、整块区域不着色。
     *
     * @param shadingType   渐变类型，不能为空
     * @param stops         颜色停止点，至少 2 个
     * @param extend        扩展方式，为空按 {@link Extend#NONE}
     * @param area          填充区域路径，不能为空，首元素必须是 {@link MoveTo}
     * @param fillRule      填充规则，为空按 {@link FillRule#NON_ZERO}
     * @param startX        起点横坐标，单位 pt
     * @param startY        起点纵坐标，单位 pt
     * @param endX          终点横坐标，单位 pt
     * @param endY          终点纵坐标，单位 pt
     * @param eccentricity  离心率，正圆为 1
     * @param angleDeg      旋转角度，单位度
     * @param startRadiusPt 起始圆半径，单位 pt；负数表示由圆心距推导
     * @param endRadiusPt   结束圆半径，单位 pt；负数表示由圆心距推导
     */
    public ShadingCommand(ShadingType shadingType, List<ColorStop> stops, Extend extend,
                          List<PathSegment> area, FillRule fillRule,
                          double startX, double startY, double endX, double endY,
                          double eccentricity, double angleDeg,
                          double startRadiusPt, double endRadiusPt) {
        super(CoordSpace.PAGE);
        if (shadingType == null) {
            throw new IllegalArgumentException("渐变类型不能为空");
        }
        if (stops == null || stops.size() < 2) {
            throw new IllegalArgumentException("渐变至少需要 2 个颜色停止点");
        }
        if (area == null || area.isEmpty()) {
            throw new IllegalArgumentException("渐变填充区域不能为空");
        }
        if (!(area.get(0) instanceof MoveTo)) {
            throw new IllegalArgumentException("渐变填充区域必须以 MoveTo 开始");
        }
        this.shadingType = shadingType;
        this.stops = Collections.unmodifiableList(new ArrayList<>(stops));
        this.extend = extend == null ? Extend.NONE : extend;
        this.area = Collections.unmodifiableList(new ArrayList<>(area));
        this.fillRule = fillRule == null ? FillRule.NON_ZERO : fillRule;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.eccentricity = eccentricity <= 0 ? 1 : eccentricity;
        this.angleDeg = angleDeg;
        this.startRadiusPt = startRadiusPt;
        this.endRadiusPt = endRadiusPt;
    }

    /**
     * @return 渐变类型
     */
    public ShadingType getShadingType() {
        return shadingType;
    }

    /**
     * @return 颜色停止点（只读）
     */
    public List<ColorStop> getStops() {
        return stops;
    }

    /**
     * @return 扩展方式
     */
    public Extend getExtend() {
        return extend;
    }

    /**
     * @return 填充区域路径（只读）
     */
    public List<PathSegment> getArea() {
        return area;
    }

    /**
     * @return 填充规则
     */
    public FillRule getFillRule() {
        return fillRule;
    }

    /**
     * @return 起点横坐标，单位 pt
     */
    public double getStartX() {
        return startX;
    }

    /**
     * @return 起点纵坐标，单位 pt
     */
    public double getStartY() {
        return startY;
    }

    /**
     * @return 终点横坐标，单位 pt
     */
    public double getEndX() {
        return endX;
    }

    /**
     * @return 终点纵坐标，单位 pt
     */
    public double getEndY() {
        return endY;
    }

    /**
     * @return 径向渐变起始圆半径，单位 pt；负数表示未指定（由圆心距推导）
     */
    public double getStartRadiusPt() {
        return startRadiusPt;
    }

    /**
     * @return 径向渐变结束圆半径，单位 pt；负数表示未指定（由圆心距推导）
     */
    public double getEndRadiusPt() {
        return endRadiusPt;
    }

    /**
     * @return 径向渐变离心率，1 表示正圆
     */
    public double getEccentricity() {
        return eccentricity;
    }

    /**
     * @return 径向渐变旋转角度，单位度
     */
    public double getAngleDeg() {
        return angleDeg;
    }

    @Override
    public String toString() {
        return "Shading(" + shadingType + ", " + stops.size() + " stops, " + extend + ")";
    }

    /**
     * 渐变类型
     *
     * @author 权观宇
     * @since 2.5.1
     */
    public enum ShadingType {
        /**
         * 轴向（线性）渐变，OFD AxialShd
         */
        AXIAL,
        /**
         * 径向渐变，OFD RadialShd
         */
        RADIAL
    }

    /**
     * 渐变颜色停止点
     *
     * @author 权观宇
     * @since 2.5.1
     */
    public static final class ColorStop {

        /**
         * 位置，范围 0~1
         */
        private final double fraction;

        /**
         * 颜色
         */
        private final PdfColor color;

        /**
         * 构造颜色停止点
         *
         * @param fraction 位置，范围 0~1，超出按边界截断
         * @param color    颜色，不能为空
         */
        public ColorStop(double fraction, PdfColor color) {
            if (color == null) {
                throw new IllegalArgumentException("停止点颜色不能为空");
            }
            this.fraction = Math.max(0, Math.min(1, fraction));
            this.color = color;
        }

        /**
         * @return 位置，范围 0~1
         */
        public double getFraction() {
            return fraction;
        }

        /**
         * @return 颜色
         */
        public PdfColor getColor() {
            return color;
        }

        @Override
        public String toString() {
            return fraction + ":" + color;
        }
    }

    /**
     * 渐变扩展方式
     * <p>
     * 对应 PDF Extend 数组（左/右或末段）：
     * <ul>
     *     <li>{@link #NONE}：不扩展；</li>
     *     <li>{@link #PAD}：以末端颜色填充（OFD Extend=1 通常映射到此）；</li>
     *     <li>{@link #REPEAT}：重复填充；</li>
     *     <li>{@link #REFLECT}：镜像填充。</li>
     * </ul>
     *
     * @author 权观宇
     * @since 2.5.1
     */
    public enum Extend {
        /**
         * 不扩展
         */
        NONE,
        /**
         * 末端颜色填充
         */
        PAD,
        /**
         * 重复
         */
        REPEAT,
        /**
         * 镜像
         */
        REFLECT
    }
}
