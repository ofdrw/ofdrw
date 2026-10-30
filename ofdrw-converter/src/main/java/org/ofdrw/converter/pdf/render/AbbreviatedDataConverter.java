package org.ofdrw.converter.pdf.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.ClosePath;
import org.ofdrw.converter.pdf.model.CurveTo;
import org.ofdrw.converter.pdf.model.LineTo;
import org.ofdrw.converter.pdf.model.MoveTo;
import org.ofdrw.converter.pdf.model.PathSegment;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.OptVal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OFD 紧缩图形数据（AbbreviatedData）→ 中间模型路径段
 * <p>
 * 支持的操作符（OFD 9.1 表 35/36）：
 * <table border="1">
 *     <tr><th>操作符</th><th>含义</th><th>中间模型</th></tr>
 *     <tr><td>S / M</td><td>子路径起点</td><td>{@link MoveTo}</td></tr>
 *     <tr><td>L</td><td>直线</td><td>{@link LineTo}</td></tr>
 *     <tr><td>Q</td><td>二次贝塞尔</td><td>{@link CurveTo}（升阶为三次）</td></tr>
 *     <tr><td>B</td><td>三次贝塞尔</td><td>{@link CurveTo}</td></tr>
 *     <tr><td>A</td><td>椭圆弧</td><td>由 {@link ArcConverter} 拆分出的 {@link CurveTo} 序列</td></tr>
 *     <tr><td>C</td><td>关闭子路径</td><td>{@link ClosePath}</td></tr>
 * </table>
 * <p>
 * 容错（设计约束 3）：操作数数量不足时按规范用 0 补齐（与 {@link OptVal#expectValues()} 一致）；
 * 单个操作符非法（数值非有限、数据不足、解析异常）时只跳过该操作符并输出 warning。
 * <p>
 * 坐标（设计决策 1：逐点换算）：输入端点经 {@code toPage} 一次性换算为 PDF 页面坐标（pt，Y 轴向上）；
 * 圆弧在换算后的端点之间用 {@link ArcConverter} 求贝塞尔段，半径由 {@code toPage} 的线性缩放比换算。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class AbbreviatedDataConverter {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(AbbreviatedDataConverter.class);

    /**
     * 每个页面转换流程单独创建的实例；{@link #reportedProblems} 只用于抑制同一页面内重复告警
     */
    private final Set<String> reportedProblems = ConcurrentHashMap.newKeySet();

    /**
     * 本实例告警条数（不含被抑制的重复告警）
     */
    private int problemCount;

    /**
     * 解析紧缩图形数据为路径段
     *
     * @param data   紧缩图形数据，可为空
     * @param toPage 对象空间（mm，左上角原点 Y 向下）→ PDF 页面坐标（pt，左下角原点 Y 向上）的矩阵，不能为空
     * @return 路径段列表（未闭合的开放子路径不自动闭合，交由渲染器按描边/填充语义补闭合）；
     * {@code data} 为空或没有合法操作符时返回空列表
     */
    List<PathSegment> convert(AbbreviatedData data, AffineMatrix toPage) {
        List<PathSegment> segments = new ArrayList<>();
        if (data == null) {
            return segments;
        }
        if (toPage == null) {
            throw new IllegalArgumentException("坐标换算矩阵不能为空");
        }
        List<OptVal> operators;
        try {
            operators = data.getRawOptVal();
        } catch (RuntimeException e) {
            report("abbreviated-data-unreadable", "紧缩图形数据无法读取，已跳过：" + e.getMessage());
            return segments;
        }
        if (operators == null || operators.isEmpty()) {
            return segments;
        }
        // 当前点（PDF 页面坐标）；subStart 为当前子路径起点，用于识别“关闭后再继续绘制”
        double[] current = null;
        for (OptVal optVal : operators) {
            try {
                current = convertOperator(optVal, current, toPage, segments);
            } catch (RuntimeException e) {
                report("abbreviated-data-operator", "路径操作符非法，已跳过 " + describe(optVal) + "：" + e.getMessage());
            }
        }
        return segments;
    }

    /**
     * 转换单个操作符
     *
     * @param optVal   操作符及其操作数（对象空间，单位 mm）
     * @param current  当前点（PDF 页面坐标），首次为空
     * @param toPage   坐标换算矩阵
     * @param segments 路径段输出
     * @return 处理后的当前点（PDF 页面坐标）；{@code null} 表示当前无当前点
     */
    private double[] convertOperator(OptVal optVal, double[] current, AffineMatrix toPage, List<PathSegment> segments) {
        if (optVal == null || optVal.opt == null) {
            return current;
        }
        switch (optVal.opt) {
            case "S":
            case "M": {
                double[] v = requireValues(optVal, 2);
                double[] p = page(toPage, v[0], v[1]);
                segments.add(new MoveTo(p[0], p[1]));
                return p;
            }
            case "L": {
                double[] v = requireValues(optVal, 2);
                double[] p = page(toPage, v[0], v[1]);
                ensureCurrent(current, p, segments);
                segments.add(new LineTo(p[0], p[1]));
                return p;
            }
            case "Q": {
                double[] v = requireValues(optVal, 4);
                double[] c = page(toPage, v[0], v[1]);
                double[] p = page(toPage, v[2], v[3]);
                ensureCurrent(current, p, segments);
                // 二次贝塞尔升阶为三次：c1 = p0 + 2/3(c - p0)，c2 = p1 + 2/3(c - p1)
                double[] start = current;
                double c1x = start[0] + 2.0 / 3.0 * (c[0] - start[0]);
                double c1y = start[1] + 2.0 / 3.0 * (c[1] - start[1]);
                double c2x = p[0] + 2.0 / 3.0 * (c[0] - p[0]);
                double c2y = p[1] + 2.0 / 3.0 * (c[1] - p[1]);
                segments.add(new CurveTo(c1x, c1y, c2x, c2y, p[0], p[1]));
                return p;
            }
            case "B": {
                double[] v = requireValues(optVal, 6);
                double[] c1 = page(toPage, v[0], v[1]);
                double[] c2 = page(toPage, v[2], v[3]);
                double[] p = page(toPage, v[4], v[5]);
                ensureCurrent(current, p, segments);
                segments.add(new CurveTo(c1[0], c1[1], c2[0], c2[1], p[0], p[1]));
                return p;
            }
            case "A": {
                double[] v = requireValues(optVal, 7);
                double[] start = current;
                if (start == null) {
                    // 没有当前点：规范要求 A 前必须存在起点，这里退化为以终点建立子路径
                    double[] p = page(toPage, v[5], v[6]);
                    segments.add(new MoveTo(p[0], p[1]));
                    report("arc-without-current-point", "圆弧操作符缺少当前点，已退化为直线起点 " + describe(optVal));
                    return p;
                }
                double[] end = page(toPage, v[5], v[6]);
                List<ArcConverter.CurveSegment> curves = ArcConverter.toCubicSegments(
                        start[0], start[1], v[0], v[1], v[2], v[3] != 0, v[4] != 0, end[0], end[1], toPage);
                for (ArcConverter.CurveSegment curve : curves) {
                    segments.add(curve.toPathSegment());
                }
                return end;
            }
            case "C":
                segments.add(ClosePath.INSTANCE);
                return null;
            case "CM": {
                report("abbreviated-data-cm", "紧缩图形数据中的 CM 操作符未实现，已跳过：" + describe(optVal));
                return current;
            }
            default:
                report("abbreviated-data-unknown", "未知的路径操作符，已跳过：" + describe(optVal));
                return current;
        }
    }

    /**
     * 保证存在当前点：操作符缺少前序起点时，以当前操作符的端点建立子路径
     *
     * @param current  当前点（PDF 页面坐标）
     * @param endpoint 本操作符端点（PDF 页面坐标）
     * @param segments 路径段输出
     */
    private void ensureCurrent(double[] current, double[] endpoint, List<PathSegment> segments) {
        if (current == null) {
            segments.add(new MoveTo(endpoint[0], endpoint[1]));
            report("path-without-start", "路径缺少起点（S/M），已按首个端点补建子路径");
        }
    }

    /**
     * 读取操作数并按需补 0
     *
     * @param optVal 操作符
     * @param count  需要的操作数个数
     * @return 操作数数组（长度不小于 {@code count}）
     */
    private double[] requireValues(OptVal optVal, int count) {
        double[] values = optVal.getValues();
        if (values == null || values.length < count) {
            report("abbreviated-data-operands", "操作符 " + optVal.opt + " 操作数不足（需要 " + count + " 个），已按 0 补齐");
            values = OptVal.filling(values, count);
        }
        return values;
    }

    /**
     * 对象空间坐标 → PDF 页面坐标
     *
     * @param toPage 坐标换算矩阵
     * @param x      对象空间横坐标，单位 mm
     * @param y      对象空间纵坐标，单位 mm
     * @return PDF 页面坐标 {@code [x, y]}，单位 pt
     */
    private double[] page(AffineMatrix toPage, double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("坐标不是有限数值：(" + x + ", " + y + ")");
        }
        return toPage.transform(x, y);
    }

    /**
     * 生成操作符的可读描述，便于告警定位
     *
     * @param optVal 操作符
     * @return 可读描述
     */
    private String describe(OptVal optVal) {
        if (optVal == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(String.valueOf(optVal.opt));
        double[] values = optVal.getValues();
        if (values != null) {
            for (double value : values) {
                sb.append(' ').append(value);
            }
        }
        return sb.toString();
    }

    /**
     * 记录一条告警；同一页面内相同原因只记录一次，避免整页重复刷屏
     *
     * @param key     告警分类键，用于去重
     * @param message 告警内容
     */
    private void report(String key, String message) {
        problemCount++;
        if (reportedProblems.add(key)) {
            logger.warn("[{}] {}", RenderContext.DIAGNOSTIC_TAG, message);
        }
    }

    /**
     * @return 本实例产生的告警条数（含被抑制的重复条数）
     */
    int getProblemCount() {
        return problemCount;
    }
}
