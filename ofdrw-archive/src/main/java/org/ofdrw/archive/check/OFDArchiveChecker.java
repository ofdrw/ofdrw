package org.ofdrw.archive.check;

import org.ofdrw.archive.check.rule.*;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * OFD-A 合规检查器
 * <p>
 * 聚合所有检查规则，对 OFD 文档执行全面的 GB/T 42133-2022 合规检查。
 * 可通过构造器参数自定义规则集。
 * <p>
 * 使用示例：
 * <pre>{@code
 *     OFDArchiveChecker checker = new OFDArchiveChecker();
 *     try (OFDReader reader = new OFDReader(Paths.get("doc.ofd"))) {
 *         List<ArchiveViolation> violations = checker.check(reader);
 *         for (ArchiveViolation v : violations) {
 *             System.out.println(v);
 *         }
 *     }
 * }</pre>
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class OFDArchiveChecker {

    /**
     * 检查规则列表
     */
    private final List<ArchiveRule> rules;

    /**
     * 创建检查器，加载全部默认规则
     */
    public OFDArchiveChecker() {
        this(loadDefaultRules());
    }

    /**
     * 创建检查器，使用自定义规则集
     *
     * @param rules 自定义规则列表
     */
    public OFDArchiveChecker(List<ArchiveRule> rules) {
        this.rules = new ArrayList<>(rules);
    }

    /**
     * 对已打开的 OFD 文档执行全部检查
     * <p>
     * 逐个执行所有注册的检查规则，收集违规项。
     * 单条规则异常不影响其他规则的执行。
     *
     * @param reader 打开的 OFD 阅读器
     * @return 违规列表，按规则名排序，无违规则为空列表（非 null）
     */
    public List<ArchiveViolation> check(OFDReader reader) {
        OFDDir ofdDir = reader.getOFDDir();
        List<ArchiveViolation> allViolations = new ArrayList<>();

        for (ArchiveRule rule : rules) {
            try {
                List<ArchiveViolation> violations = rule.check(reader, ofdDir);
                if (violations != null) {
                    allViolations.addAll(violations);
                }
            } catch (Exception e) {
                // 单条规则异常不影响其他规则执行，记录为内部错误
                allViolations.add(new ArchiveViolation(
                        rule.getClass().getSimpleName(),
                        ArchiveViolation.Severity.ERROR,
                        "规则执行异常: " + e.getMessage(),
                        null, null, null));
            }
        }

        // 按规则名排序，保证输出稳定
        Collections.sort(allViolations, Comparator.comparing(ArchiveViolation::getRuleName));
        return allViolations;
    }

    /**
     * 对 OFD 文件执行检查（便捷方法）
     * <p>
     * 自动管理 OFDReader 的生命周期。
     *
     * @param ofdPath OFD 文件路径
     * @return 违规列表
     * @throws IOException 文件读取异常
     */
    public List<ArchiveViolation> check(Path ofdPath) throws IOException {
        try (OFDReader reader = new OFDReader(ofdPath)) {
            return check(reader);
        }
    }

    /**
     * 加载默认规则集
     * <p>
     * 涵盖 GB/T 42133-2022 Phase 1 全部检查规则。
     *
     * @return 默认规则列表
     */
    private static List<ArchiveRule> loadDefaultRules() {
        List<ArchiveRule> rules = new ArrayList<>();

        // 文件结构规则 (6.1-6.2)
        rules.add(new DocTypeRule());             // 1: DocType="OFD-A"
        rules.add(new SingleDocRule());           // 2: 单文档
        rules.add(new ExternalResourceRule());    // 3: 禁止外部资源
        rules.add(new PermissionRule());          // 4: 去除权限
        rules.add(new NonGotoActionRule());       // 6: 去除非Goto动作
        rules.add(new OutlineActionRule());       // 7: 大纲动作检查
        rules.add(new ExtensionRule());           // 8: 去除扩展

        // 资源规则 (6.2.6)
        rules.add(new ColorSpaceRule());          // 9: 颜色空间检查
        rules.add(new ImageFormatRule());         // 10: 图像格式检查
        rules.add(new FontSubsetRule());          // 11: 字体嵌入检查
        rules.add(new AudioVideoRule());          // 12: 禁止音视频
        rules.add(new ImageExtensionRule());      // 13: 禁止图像扩展
        rules.add(new ResourcePlacementRule());   // 18: 资源位置检查
        rules.add(new ImageResourceRegRule());    // 19: 图像注册检查
        rules.add(new ColorProfileRule());        // 24: 颜色配置建议
        rules.add(new TextHScaleRule());          // 25: HScale检查

        // 页面内容规则 (6.3)
        rules.add(new PageBlockDepthRule());      // 14: PageBlock嵌套
        rules.add(new ClipAreaRule());            // 15: 裁剪区检查

        // 注释规则 (6.10)
        rules.add(new AnnotationRule());          // 注释合规

        // 签名/签章规则 (6.13)
        rules.add(new SignatureRule());           // 签名去技术化

        // 附件规则 (6.15)
        rules.add(new AttachmentRule());          // 附件合规

        // 内容规则 (6.5-6.6)
        rules.add(new ImageInterpolateRule());    // 17: 图像插值
        rules.add(new TextSizeRule());            // 23: 文字Size

        return rules;
    }
}
