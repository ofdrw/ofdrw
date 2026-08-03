package org.ofdrw.archive.check.rule;

import org.dom4j.Element;
import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 25：文字仅横向缩放时应用 HScale 属性（GB/T 42133-2022 6.6d）
 * <p>
 * 检查 TextObject 的 CTM 变换矩阵。若变换为纯横向缩放
 * （只有 X 方向缩放，无旋转/无错切/无 Y 方向缩放）但未使用 HScale 属性 → WARN。
 * <p>
 * CTM 矩阵格式: [a b c d e f]，纯横向缩放对应 b=0, c=0, d=1。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class TextHScaleRule implements ArchiveRule {
    public static final String RULE_NAME = "TEXT_HSCALE";

    /** CTM 矩阵元素数 */
    private static final int CTM_ELEMENTS = 6;

    /** 浮点比较容差 */
    private static final double EPSILON = 1e-6;

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            int pageCount = reader.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(i);
                ST_Loc pageLoc = reader.getPageAbsLoc(i);

                @SuppressWarnings("unchecked")
                List<Element> textObjects = page.elements("TextObject");
                for (Element textObj : textObjects) {
                    // 检查 CTM 属性
                    String ctmStr = textObj.attributeValue("CTM");
                    if (ctmStr == null || ctmStr.isEmpty()) continue;

                    double[] ctm = parseCTM(ctmStr);
                    if (ctm == null) continue;

                    // 检查是否为纯横向缩放: b≈0, c≈0, d≈1, a≠1
                    if (Math.abs(ctm[1]) < EPSILON     // b≈0
                            && Math.abs(ctm[2]) < EPSILON  // c≈0
                            && Math.abs(ctm[3] - 1) < EPSILON  // d≈1
                            && Math.abs(ctm[0] - 1) > EPSILON) {  // a≠1（有横向缩放）

                        String hScale = textObj.attributeValue("HScale");
                        if (hScale == null || hScale.isEmpty()) {
                            violations.add(new ArchiveViolation(
                                    RULE_NAME, ArchiveViolation.Severity.WARN,
                                    "TextObject 的 CTM 为纯横向缩放 (a=" + String.format("%.3f", ctm[0]) + ")"
                                            + "，建议使用 HScale 属性代替",
                                    pageLoc, "CTM: " + ctmStr, "HScale=" + String.format("%.3f", ctm[0])));
                        }
                    }
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查 HScale 时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }

    /**
     * 解析 CTM 字符串为 double 数组
     * <p>
     * CTM 格式: "a b c d e f"（空格分隔的 6 个数字）
     *
     * @param ctmStr CTM 字符串
     * @return double[6] 或 null（解析失败）
     */
    private double[] parseCTM(String ctmStr) {
        String[] parts = ctmStr.trim().split("\\s+");
        if (parts.length != CTM_ELEMENTS) return null;
        try {
            double[] result = new double[CTM_ELEMENTS];
            for (int i = 0; i < CTM_ELEMENTS; i++) {
                result[i] = Double.parseDouble(parts[i]);
            }
            return result;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
