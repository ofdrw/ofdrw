package org.ofdrw.converter.pdf.render.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文字图元的库无关视图（由 {@link OfdTextSource} 从 OFD 模型转换而来）
 * <p>
 * 只承载文字渲染需要的字段，单位与 OFD 一致（长度 mm、角度 °），不含任何渲染库类型（设计约束 2、决策 15）：
 * <ul>
 *     <li>{@link #getBoundary()}：外接矩形（Boundary），单位 mm，左上角原点；</li>
 *     <li>{@link #getSizeMm()}：字号（Size），单位 mm；</li>
 *     <li>{@link #getHScale()}：字形水平缩放比（HScale），缺省 1.0；</li>
 *     <li>{@link #getCharDirectionDeg()}：字符方向（CharDirection，表 47），取值 0/90/180/270；</li>
 *     <li>{@link #getReadDirectionDeg()}：阅读方向（ReadDirection，表 47），取值 0/90/180/270；</li>
 *     <li>{@link #getCtm()}：图元自身的 CTM（对象空间内叠加），无 CTM 时为 null；</li>
 *     <li>{@link #getTextCodes()}：文字内容与定位（表 46），见 {@link TextCodeRun}。</li>
 * </ul>
 * 本类为不可变对象。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class OfdText {

    /**
     * 外接矩形，单位 mm
     */
    private final OfdBox boundary;

    /**
     * 字号，单位 mm
     */
    private final double sizeMm;

    /**
     * 字形水平缩放比，缺省 1.0
     */
    private final double hScale;

    /**
     * 字符方向角度，单位 °，取值 0/90/180/270
     */
    private final double charDirectionDeg;

    /**
     * 阅读方向角度，单位 °，取值 0/90/180/270
     */
    private final double readDirectionDeg;

    /**
     * 图元自身的 CTM {@code [a b c d e f]}，无 CTM 时为 null
     */
    private final double[] ctm;

    /**
     * 文字内容与定位序列
     */
    private final List<TextCodeRun> textCodes;

    /**
     * 构造文字图元视图
     *
     * @param boundary         外接矩形，单位 mm，可为 null（按 (0,0) 处理）
     * @param sizeMm           字号，单位 mm，必须大于 0
     * @param hScale           字形水平缩放比，缺省 1.0
     * @param charDirectionDeg 字符方向角度，单位 °
     * @param readDirectionDeg 阅读方向角度，单位 °
     * @param ctm              图元 CTM {@code [a b c d e f]}，可为 null
     * @param textCodes        文字内容与定位序列，可为空
     */
    public OfdText(OfdBox boundary, double sizeMm, double hScale, double charDirectionDeg,
                   double readDirectionDeg, double[] ctm, List<TextCodeRun> textCodes) {
        if (!(sizeMm > 0)) {
            throw new IllegalArgumentException("字号（Size）必须大于 0: " + sizeMm);
        }
        this.boundary = boundary == null ? new OfdBox(0, 0, 0, 0) : boundary;
        this.sizeMm = sizeMm;
        this.hScale = hScale > 0 ? hScale : 1.0d;
        this.charDirectionDeg = normalizeAngle(charDirectionDeg);
        this.readDirectionDeg = normalizeAngle(readDirectionDeg);
        this.ctm = ctm == null || ctm.length != 6 ? null : ctm.clone();
        this.textCodes = textCodes == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(textCodes));
    }

    /**
     * 规范化方向角度为 0/90/180/270
     *
     * @param deg 方向角度，单位 °
     * @return 规范化后的角度，无法识别时按 0 处理
     */
    private static double normalizeAngle(double deg) {
        double angle = ((deg % 360) + 360) % 360;
        if (Math.abs(angle - 90) < 1e-6) {
            return 90;
        }
        if (Math.abs(angle - 180) < 1e-6) {
            return 180;
        }
        if (Math.abs(angle - 270) < 1e-6) {
            return 270;
        }
        return 0;
    }

    /**
     * 外接矩形
     *
     * @return 外接矩形，单位 mm
     */
    public OfdBox getBoundary() {
        return boundary;
    }

    /**
     * 字号
     *
     * @return 字号，单位 mm
     */
    public double getSizeMm() {
        return sizeMm;
    }

    /**
     * 字形水平缩放比
     *
     * @return 水平缩放比，缺省 1.0
     */
    public double getHScale() {
        return hScale;
    }

    /**
     * 字符方向
     *
     * @return 字符方向角度，单位 °，取值 0/90/180/270
     */
    public double getCharDirectionDeg() {
        return charDirectionDeg;
    }

    /**
     * 阅读方向
     *
     * @return 阅读方向角度，单位 °，取值 0/90/180/270
     */
    public double getReadDirectionDeg() {
        return readDirectionDeg;
    }

    /**
     * 图元自身的 CTM
     *
     * @return 副本 {@code [a b c d e f]}，无 CTM 时为 null
     */
    public double[] getCtm() {
        return ctm == null ? null : ctm.clone();
    }

    /**
     * 文字内容与定位序列
     *
     * @return 只读列表
     */
    public List<TextCodeRun> getTextCodes() {
        return textCodes;
    }

    @Override
    public String toString() {
        return "OfdText{" + boundary + ", size=" + sizeMm + "mm, charDir=" + charDirectionDeg
                + ", readDir=" + readDirectionDeg + ", codes=" + textCodes.size() + "}";
    }
}
