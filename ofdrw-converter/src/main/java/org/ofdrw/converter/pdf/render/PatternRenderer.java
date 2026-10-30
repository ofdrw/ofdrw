package org.ofdrw.converter.pdf.render;

import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.PatternCommand;
import org.ofdrw.core.pageDescription.color.pattern.CT_Pattern;
import org.ofdrw.core.pageDescription.color.pattern.ReflectMethod;
import org.ofdrw.core.pageDescription.color.pattern.RelativeTo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 底纹渲染器（P3）：OFD Pattern → 中间模型 {@link PatternCommand}（Tiling Pattern）
 * <p>
 * <b>决策 17（已确认的首版范围）</b>：统一按 {@code ReflectMethod=Normal} 近似平铺，
 * 只支持 {@code RelativeTo=Object}（对象坐标系）语义。以下情况按决策 7/8 输出 <b>warning</b> 并标记
 * {@link PatternCommand#isApproximate()}，不阻断转换、不静默忽略：
 * <ul>
 *     <li>{@code ReflectMethod} 为 Column / Row / RowAndColumn（竖排/横排/双向镜像翻转）；</li>
 *     <li>{@code RelativeTo=Page}（以页面坐标原点为平铺锚点，需要额外的锚点平移才能精确复现）；</li>
 *     <li>{@code XStep}/{@code YStep} 小于单元尺寸（表 28 要求不得小于，按单元尺寸处理）；</li>
 *     <li>单元内容为空（整个底纹跳过）。</li>
 * </ul>
 * <b>平铺语义</b>（表 28）：{@code Width}/{@code Height} 是单元尺寸，{@code XStep}/{@code YStep} 是相邻单元的间距，
 * 二者可不等（间距大于单元时留下透明间隙）；单元内容只绘制在单元格左上角。
 * <p>
 * 本渲染器只产出“一个单元格”的指令；平铺（把单元按步进重复）由写入器实现，因此
 * {@link PatternCommand#getCell()} 给出的是页面坐标下的单元尺寸，{@link PatternCommand#getCellCommands()}
 * 是单元内容在<b>页面坐标</b>下的指令序列（由调用方注入的渲染器生成）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PatternRenderer implements ContentRenderer {

    private static final Logger logger = LoggerFactory.getLogger(PatternRenderer.class);

    /**
     * 单元格内容渲染函数：把 OFD 的 {@code CellContent} 渲染为中间模型指令
     * <p>
     * 单元内容可包含路径、文字、图像等任意图元，具体渲染由页面级调度器实现（避免与 P2/P4 的渲染器耦合）。
     */
    @FunctionalInterface
    public interface CellContentRenderer {

        /**
         * 渲染单元格内容
         *
         * @param content 单元格内容（OFD {@code CellContent}）
         * @param context 单元格所在坐标系对应的渲染上下文
         * @return 指令序列；无法渲染时返回空列表
         */
        List<ContentCommand> render(Object content, RenderContext context);
    }

    /**
     * 单元格内容渲染函数，可为空（为空时跳过底纹并告警）
     */
    private final CellContentRenderer cellRenderer;

    /**
     * 已告警原因（同一原因只告警一次）
     */
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    /**
     * 构造底纹渲染器
     *
     * @param cellRenderer 单元格内容渲染函数，可为空
     */
    public PatternRenderer(CellContentRenderer cellRenderer) {
        this.cellRenderer = cellRenderer;
    }

    @Override
    public String getName() {
        return "PatternRenderer";
    }

    /**
     * 渲染底纹
     *
     * @param input   OFD 底纹对象（{@link CT_Pattern}）
     * @param context 渲染上下文
     * @return 只包含一条 {@link PatternCommand} 的列表；不可渲染时返回空列表
     */
    @Override
    public List<ContentCommand> render(Object input, RenderContext context) {
        List<ContentCommand> result = new ArrayList<>(1);
        if (!(input instanceof CT_Pattern) || context == null) {
            return result;
        }
        try {
            PatternCommand command = pattern((CT_Pattern) input, context);
            if (command != null) {
                result.add(command);
            }
        } catch (RuntimeException e) {
            warnOnce("pattern-error", "底纹渲染失败，已跳过：" + e.getMessage());
        }
        return result;
    }

    /**
     * 解析底纹为平铺指令
     *
     * @param pattern OFD 底纹
     * @param context 渲染上下文
     * @return 底纹指令；不可渲染时返回 {@code null}
     */
    public PatternCommand pattern(CT_Pattern pattern, RenderContext context) {
        if (pattern.getCellContent() == null) {
            warnOnce("empty-cell", "底纹缺少单元内容，已跳过");
            return null;
        }
        double widthMm = pattern.getWidth() == null ? 0 : pattern.getWidth();
        double heightMm = pattern.getHeight() == null ? 0 : pattern.getHeight();
        if (!(widthMm > 0) || !(heightMm > 0)) {
            warnOnce("cell-size", "底纹单元尺寸非法（" + widthMm + " x " + heightMm + " mm），已跳过");
            return null;
        }
        if (cellRenderer == null) {
            warnOnce("no-cell-renderer", "未提供底纹单元内容的渲染器，已跳过底纹");
            return null;
        }
        boolean approximate = false;
        ReflectMethod reflect = pattern.getReflectMethod();
        if (reflect != null && reflect != ReflectMethod.Normal) {
            approximate = true;
            warnOnce("reflect:" + reflect, "底纹翻转方式为 " + reflect
                    + "，首版按 Normal 近似平铺（决策 17），镜像单元缺失");
        }
        RelativeTo relativeTo = pattern.getRelativeTo();
        if (relativeTo == RelativeTo.Page) {
            approximate = true;
            warnOnce("relative-page", "底纹以页面坐标为参照（RelativeTo=Page），首版按对象坐标近似平铺（决策 17）");
        }
        // 表 28：XStep/YStep 不得小于单元尺寸，小于时按单元尺寸处理（无间隙平铺）
        double xStepMm = step(pattern.getXStep(), widthMm, "XStep");
        double yStepMm = step(pattern.getYStep(), heightMm, "YStep");

        List<ContentCommand> cellCommands = cellRenderer.render(pattern.getCellContent(), context);
        if (cellCommands == null || cellCommands.isEmpty()) {
            warnOnce("empty-cell-content", "底纹单元内容没有可绘制的图元，已跳过");
            return null;
        }
        // 单元尺寸换算为页面坐标下的 pt；XStep/YStep 大于单元尺寸时单元本身仍只占 Width×Height
        double widthPt = CoordSystem.mmToPt(widthMm);
        double heightPt = CoordSystem.mmToPt(heightMm);
        double xStepPt = CoordSystem.mmToPt(xStepMm);
        double yStepPt = CoordSystem.mmToPt(yStepMm);
        if (Math.abs(xStepPt - widthPt) > 1e-9 || Math.abs(yStepPt - heightPt) > 1e-9) {
            warnOnce("cell-gap", "底纹单元间距与单元尺寸不同（XStep=" + xStepMm + "、YStep=" + yStepMm
                    + "，单元 " + widthMm + "x" + heightMm + " mm），写入器需按步进平铺、单元内容只占单元尺寸");
        }
        return new PatternCommand("Pattern", new Box(0, 0, widthPt, heightPt), cellCommands, approximate);
    }

    /**
     * 读取平铺步进：缺失或小于单元尺寸时按单元尺寸处理
     *
     * @param step 声明的步进，单位 mm，可为空
     * @param size 单元尺寸，单位 mm
     * @param name 属性名，用于告警
     * @return 实际步进，单位 mm
     */
    private double step(Double step, double size, String name) {
        if (step == null || !(step > 0)) {
            return size;
        }
        if (step < size) {
            warnOnce("step-small:" + name, "底纹 " + name + "（" + step + "mm）小于单元尺寸（" + size
                    + "mm），违反表 28 约束，已按单元尺寸处理");
            return size;
        }
        return step;
    }

    /**
     * 输出一次告警（相同原因只告警一次）
     *
     * @param key     告警分类键
     * @param message 告警内容
     */
    private void warnOnce(String key, String message) {
        if (reported.add(key)) {
            logger.warn("[{}] {}", RenderContext.DIAGNOSTIC_TAG, message);
        }
    }
}
