package org.ofdrw.converter.pdf.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.converter.pdf.model.ClipCommand;
import org.ofdrw.converter.pdf.model.ClosePath;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.CurveTo;
import org.ofdrw.converter.pdf.model.FillRule;
import org.ofdrw.converter.pdf.model.LineTo;
import org.ofdrw.converter.pdf.model.MoveTo;
import org.ofdrw.converter.pdf.model.PathSegment;
import org.ofdrw.converter.pdf.model.RestoreState;
import org.ofdrw.converter.pdf.model.SaveState;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.pageDescription.clips.Area;
import org.ofdrw.core.pageDescription.clips.CT_Clip;
import org.ofdrw.core.pageDescription.clips.Clips;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.text.text.CT_Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 裁剪应用器：OFD {@code Clips} → 中间模型裁剪指令
 * <p>
 * 裁剪语义（设计第 2 节表格、OFD 8.4 节、表 33）：
 * <ul>
 *     <li>多个 Clip 取<b>交集</b>：每个 Clip 生成一条 {@link ClipCommand}，按顺序施加即为交集；</li>
 *     <li>同一 Clip 内的多个 Area 取<b>并集</b>：合成一条带 {@link ClipCommand#isUnionArea()} 的指令
 *         （写入器用偶奇规则 {@code W* n} 一次性求并集，避免多次 {@code W} 反复求交）；</li>
 *     <li>Area 可以是 Path 或 Text：Path 用紧缩图形数据描绘；Text 按文字区域近似为矩形（见 {@link RegionGeometry}）；</li>
 *     <li>裁剪区随线宽等绘制参数影响：裁剪对象自带有效线宽时，把裁剪范围按半个线宽外扩近似，
 *         并输出提示日志（表 33 DrawParam 备注）。</li>
 * </ul>
 * <p>
 * 输出约定（设计约束 1）：裁剪必须与内容隔离在 {@code q ... Q} 之间，否则会污染后续内容；
 * {@link #render} 返回的序列固定为 {@code q, ClipCommand..., 内容..., Q}，由调用方整体加入页面内容。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ClipApplier {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(ClipApplier.class);

    /**
     * 描边式裁剪的外扩系数：半个线宽
     */
    static final double STROKE_CLIP_MARGIN = 0.5;

    /**
     * 裁剪路径固定使用的填充规则：多个子路径叠加时，偶奇规则才能得到并集
     */
    private static final FillRule UNION_FILL_RULE = FillRule.EVEN_ODD;

    /**
     * 紧缩图形数据转换器（按实例隔离告警去重）
     */
    private final AbbreviatedDataConverter pathConverter = new AbbreviatedDataConverter();

    /**
     * 绘制参数查找函数（用于裁剪对象的线宽），可为空
     */
    private final GraphicsStateResolver.DrawParamLookup drawParamLookup;

    /**
     * 已告警原因
     */
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    /**
     * 构造裁剪应用器
     */
    public ClipApplier() {
        this(null);
    }

    /**
     * 构造裁剪应用器
     *
     * @param drawParamLookup 绘制参数查找函数，可为空（此时不解析裁剪对象的线宽）
     */
    public ClipApplier(GraphicsStateResolver.DrawParamLookup drawParamLookup) {
        this.drawParamLookup = drawParamLookup;
    }

    /**
     * 获取裁剪应用器名称
     *
     * @return 名称
     */
    public String getName() {
        return "ClipApplier";
    }

    /**
     * 把 OFD 裁剪目标转换为裁剪指令序列
     *
     * @param clips   OFD 裁剪目标（{@link Clips}），可为空
     * @param context 渲染上下文
     * @return 裁剪指令序列（一个 Clip 一条指令）；无裁剪时返回空列表
     */
    public List<ClipCommand> apply(Object clips, RenderContext context) {
        if (!(clips instanceof Clips) || context == null) {
            return Collections.emptyList();
        }
        List<CT_Clip> clipList = ((Clips) clips).getClips();
        if (clipList == null || clipList.isEmpty()) {
            return Collections.emptyList();
        }
        AffineMatrix toPage = objectToPage(context);
        List<ClipCommand> commands = new ArrayList<>(clipList.size());
        for (CT_Clip clip : clipList) {
            commands.addAll(applyClip(clip, toPage, context));
        }
        return commands;
    }

    /**
     * 转换单个 Clip 为一条裁剪指令（多个 Area 合并为并集）
     *
     * @param clip    OFD 裁剪区域，可为空
     * @param toPage  对象空间 → 页面坐标矩阵
     * @param context 渲染上下文
     * @return 裁剪指令列表（0 或 1 条）
     */
    public List<ClipCommand> applyClip(CT_Clip clip, AffineMatrix toPage, RenderContext context) {
        if (clip == null) {
            return Collections.emptyList();
        }
        List<Area> areas = clip.getAreas();
        if (areas == null || areas.isEmpty()) {
            return Collections.emptyList();
        }
        List<PathSegment> union = new ArrayList<>();
        boolean hasMove = false;
        int areaCount = 0;
        for (Area area : areas) {
            try {
                AffineMatrix areaToPage = areaToPage(area, toPage, context);
                Object clipObj = area.getClipObj();
                List<PathSegment> segments;
                if (clipObj instanceof CT_Path) {
                    CT_Path path = (CT_Path) clipObj;
                    segments = pathConverter.convert(path.getAbbreviatedDataEle(), areaToPage);
                    if (segments.isEmpty()) {
                        // 紧缩数据缺失或不可用时退化为外接矩形
                        warnOnce("path-area-fallback", "裁剪 Path 缺少可用的紧缩图形数据，已退化为外接矩形");
                        segments = rectangle(RegionGeometry.boundaryBox(path.getBoundary(), areaToPage));
                    } else {
                        segments = strokeExpanded(segments, effectiveLineWidth(area, path.getLineWidth()));
                    }
                } else if (clipObj instanceof CT_Text) {
                    warnOnce("text-area", "裁剪 Area 为文字，已按文字外接矩形近似（PDF 裁剪仅支持路径）");
                    segments = rectangle(RegionGeometry.areaBox(area, areaToPage));
                } else {
                    warnOnce("unsupported-area", "不支持的裁剪 Area 类型，已跳过："
                            + (clipObj == null ? "null" : clipObj.getClass().getName()));
                    continue;
                }
                if (segments.isEmpty()) {
                    warnOnce("empty-area", "裁剪 Area 无法得到有效区域，已跳过");
                    continue;
                }
                areaCount++;
                for (PathSegment segment : segments) {
                    if (segment instanceof MoveTo) {
                        hasMove = true;
                    }
                    union.add(segment);
                }
            } catch (RuntimeException e) {
                warnOnce("area-error", "裁剪区域解析失败，已跳过该区域：" + e.getMessage());
            }
        }
        if (!hasMove) {
            return Collections.emptyList();
        }
        if (areaCount > 1) {
            warnOnce("multi-area", "裁剪区域包含多个 Area，按并集处理（" + areaCount + " 个）");
        }
        return Collections.singletonList(new ClipCommand(union, UNION_FILL_RULE, true));
    }

    /**
     * 把裁剪施加到一组内容指令上：{@code q + ClipCommand... + 内容 + Q}
     * <p>
     * 无裁剪时直接返回内容本身（不额外写 {@code q/Q}）；有裁剪时保证 {@code q/Q} 成对（设计约束 1）。
     *
     * @param clips   OFD 裁剪目标（{@link Clips}），可为空
     * @param context 渲染上下文
     * @param content 待裁剪的内容指令，可为空
     * @return 隔离后的指令序列，可直接加入 {@link org.ofdrw.converter.pdf.model.PdfPageContent}
     */
    public List<ContentCommand> render(Object clips, RenderContext context, List<? extends ContentCommand> content) {
        List<ClipCommand> clipCommands = apply(clips, context);
        if (clipCommands.isEmpty()) {
            return content == null ? Collections.emptyList() : new ArrayList<ContentCommand>(content);
        }
        List<ContentCommand> commands = new ArrayList<>((content == null ? 0 : content.size()) + clipCommands.size() + 2);
        commands.add(SaveState.INSTANCE);
        commands.addAll(clipCommands);
        if (content != null) {
            commands.addAll(content);
        }
        commands.add(RestoreState.INSTANCE);
        return commands;
    }

    /**
     * 计算裁剪区域自身的坐标矩阵：对象空间 → 页面坐标，再叠加 Area 的 CTM
     * <p>
     * 表 33：Area 的 CTM 作用于 Area 自身坐标；Path 裁剪对象的紧缩图形数据以外接矩形左上角为原点，
     * 因此这里先叠加外接矩形平移，再叠加 Area CTM，最后统一换算为页面坐标（与图元变换顺序一致）。
     *
     * @param area    区域，不能为空
     * @param toPage  当前容器的对象空间 → 页面坐标矩阵（不含外接矩形定位）
     * @param context 渲染上下文
     * @return 区域坐标 → 页面坐标矩阵
     */
    private AffineMatrix areaToPage(Area area, AffineMatrix toPage, RenderContext context) {
        AffineMatrix combined = toPage;
        Object clipObj = area.getClipObj();
        if (clipObj instanceof CT_Path) {
            org.ofdrw.core.basicType.ST_Box boundary = ((CT_Path) clipObj).getBoundary();
            if (boundary != null && boundary.getTopLeftX() != null && boundary.getTopLeftY() != null) {
                combined = AffineMatrix.translate(boundary.getTopLeftX(), boundary.getTopLeftY()).multiply(toPage);
            }
        }
        ST_Array ctm = area.getCTM();
        if (ctm != null && ctm.size() == 6) {
            Double[] values = ctm.toDouble();
            double[] raw = new double[6];
            for (int i = 0; i < 6; i++) {
                raw[i] = values[i] == null ? 0 : values[i];
            }
            combined = CoordSystem.fromCtm(raw).multiply(combined);
        }
        return combined;
    }

    /**
     * 计算裁剪对象的有效线宽：裁剪对象属性 → Area 绘制参数 → 0
     *
     * @param area      区域
     * @param pathWidth 路径对象的 LineWidth 属性，单位 mm，可为空
     * @return 线宽，单位 mm
     */
    private double effectiveLineWidth(Area area, Double pathWidth) {
        if (pathWidth != null && pathWidth > 0) {
            return pathWidth;
        }
        if (drawParamLookup == null) {
            return 0;
        }
        try {
            ST_RefID ref = area.getDrawParam();
            ST_ID id = ref == null ? null : ref.getRefId();
            if (id == null) {
                return 0;
            }
            CT_DrawParam param = drawParamLookup.getDrawParam(id.toString());
            if (param != null && param.getLineWidth() != null && param.getLineWidth() > 0) {
                return param.getLineWidth();
            }
        } catch (RuntimeException e) {
            warnOnce("area-drawparam", "裁剪区域绘制参数解析失败：" + e.getMessage());
        }
        return 0;
    }

    /**
     * 按线宽外扩裁剪路径（近似描边式裁剪：区域向外扩展半个线宽的外接矩形）
     *
     * @param segments    原始裁剪路径（页面坐标，单位 pt）
     * @param lineWidthMm 线宽，单位 mm
     * @return 外扩后的路径；线宽为 0 或无法求外接矩形时原样返回
     */
    private List<PathSegment> strokeExpanded(List<PathSegment> segments, double lineWidthMm) {
        if (!(lineWidthMm > 0)) {
            return segments;
        }
        Box box = bounds(segments);
        if (box == null) {
            return segments;
        }
        double margin = CoordSystem.mmToPt(lineWidthMm) * STROKE_CLIP_MARGIN;
        warnOnce("stroke-clip", "裁剪区域随线宽外扩（线宽 " + lineWidthMm + "mm），按外接矩形外扩近似");
        return rectangle(new Box(box.getX() - margin, box.getY() - margin,
                box.getWidth() + 2 * margin, box.getHeight() + 2 * margin));
    }

    /**
     * 计算路径外接矩形（含贝塞尔控制点，保守偏大）
     *
     * @param segments 路径段
     * @return 外接矩形；无有效点返回 {@code null}
     */
    private Box bounds(List<PathSegment> segments) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        Deque<double[]> points = new ArrayDeque<>();
        for (PathSegment segment : segments) {
            if (segment instanceof MoveTo) {
                MoveTo move = (MoveTo) segment;
                points.add(new double[]{move.getX(), move.getY()});
            } else if (segment instanceof LineTo) {
                LineTo line = (LineTo) segment;
                points.add(new double[]{line.getX(), line.getY()});
            } else if (segment instanceof CurveTo) {
                CurveTo curve = (CurveTo) segment;
                points.add(new double[]{curve.getX1(), curve.getY1()});
                points.add(new double[]{curve.getX2(), curve.getY2()});
                points.add(new double[]{curve.getX3(), curve.getY3()});
            }
        }
        for (double[] point : points) {
            minX = Math.min(minX, point[0]);
            minY = Math.min(minY, point[1]);
            maxX = Math.max(maxX, point[0]);
            maxY = Math.max(maxY, point[1]);
        }
        if (minX > maxX) {
            return null;
        }
        return new Box(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * 生成矩形路径
     *
     * @param box 页面矩形，单位 pt，可为空
     * @return 路径段；矩形为空或退化时返回空列表
     */
    static List<PathSegment> rectangle(Box box) {
        if (box == null || box.getWidth() <= 0 || box.getHeight() <= 0) {
            return Collections.emptyList();
        }
        List<PathSegment> segments = new ArrayList<>(5);
        segments.add(new MoveTo(box.getX(), box.getY()));
        segments.add(new LineTo(box.getX() + box.getWidth(), box.getY()));
        segments.add(new LineTo(box.getX() + box.getWidth(), box.getY() + box.getHeight()));
        segments.add(new LineTo(box.getX(), box.getY() + box.getHeight()));
        segments.add(ClosePath.INSTANCE);
        return segments;
    }

    /**
     * 计算上下文对应的对象空间 → 页面坐标矩阵（含页面框偏移，决策 26）
     *
     * @param context 渲染上下文
     * @return 换算矩阵
     */
    static AffineMatrix objectToPage(RenderContext context) {
        return CoordSystem.objectToPage(null, context.getCurrentCtm(),
                context.getPageX0Mm(), context.getPageY0Mm(), context.getPageHeightMm());
    }

    /**
     * 输出一次告警（相同原因只告警一次）
     *
     * @param key     告警分类键
     * @param message 告警内容
     */
    void warnOnce(String key, String message) {
        if (reported.add(key)) {
            logger.warn("[{}] {}", RenderContext.DIAGNOSTIC_TAG, message);
        }
    }
}
