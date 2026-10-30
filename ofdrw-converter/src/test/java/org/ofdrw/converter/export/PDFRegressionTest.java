package org.ofdrw.converter.export;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.regression.PdfRegressionRunner;
import org.ofdrw.converter.regression.PdfRegressionSupport;
import org.ofdrw.converter.regression.RegressionMetrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 入库样本像素 + 文本回归（设计第 7.1、7.2、7.2.1 节，决策 5、6、22）
 * <p>
 * 数据源：{@code src/test/resources/pdf-match/} 下的 {@code <样本名>.ofd} 与其基准 {@code <样本名>.pdf}，
 * 样本来源与覆盖特性见同目录 {@code README.md}。
 * <p>
 * 断言口径（72 dpi）：
 * <ul>
 *     <li>页数与基准一致；</li>
 *     <li>每页差异像素比例 ≤ 0.5%，且平均绝对误差 MAE ≤ 2/255；</li>
 *     <li>文本提取相似度 ≥ 0.99（质量目标 2：文字可选中可提取）。</li>
 * </ul>
 * 转换异常一律视为失败：入库样本均为“应能正常转换”的样本。
 * <p>
 * 转换统一使用 {@link PDFExporter}（新渲染管线），样本与阈值不随实现变化。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class PDFRegressionTest {

    /**
     * 入库样本目录
     */
    static final Path SAMPLE_DIR = Paths.get("src/test/resources/pdf-match");

    /**
     * 输出目录（渲染图、差异图、实际 PDF）
     */
    static final Path OUTPUT_DIR = Paths.get("target/pdf-regression");

    /**
     * 文本相似度阈值
     */
    static final double MIN_TEXT_SIMILARITY = 0.99d;

    /**
     * 是否在 {@code mvn test} 中断言像素/文本达标
     * <p>
     * 默认 {@code true}：新渲染管线已接通，像素/文本指标作为 CI 门禁；
     * 需要只看报告不断言时用 {@code -Dofd.regression.assert=false}。
     */
    static final boolean ASSERT_METRICS =
            Boolean.parseBoolean(System.getProperty("ofd.regression.assert", "true"));

    /**
     * 回归执行器，其 {@link PdfRegressionRunner#getResults()} 在 {@link #runRegression()} 后立即可用
     */
    static PdfRegressionRunner runner;

    /**
     * 执行全部入库样本的转换与比较
     *
     * @throws IOException 样本遍历失败
     */
    @BeforeAll
    static void runRegression() throws IOException {
        // 用可读回“字体替换数量”的导出器，供回归框架识别纯字体替换样本（known-difference）
        PdfRegressionSupport.Exporter exporter = new PdfRegressionSupport.Exporter() {
            /**
             * 最近一次导出的字体替换数量
             */
            private int substitutionCount;

            @Override
            public void export(java.nio.file.Path ofd, java.nio.file.Path pdf) throws java.io.IOException {
                try (PDFExporter out = new PDFExporter(ofd, pdf)) {
                    out.export();
                    substitutionCount = out.getFontSubstitutionCount();
                }
            }

            @Override
            public int lastFontSubstitutionCount() {
                return substitutionCount;
            }
        };
        PdfRegressionRunner r = new PdfRegressionRunner(PdfRegressionSupport.DEFAULT_DPI, OUTPUT_DIR, exporter);
        r.runDirectory(SAMPLE_DIR);
        try {
            r.writeMarkdown(OUTPUT_DIR.resolve("report.md"), "入库样本回归报告",
                    PdfRegressionSupport.MAX_MAE, MIN_TEXT_SIMILARITY);
        } catch (IOException e) {
            System.out.println(">> 报告写出失败: " + e);
        }
        runner = r;
    }

    @Test
    @DisplayName("入库样本：像素差异像素比例≤0.5% 且 MAE≤2/255，文本相似度≥0.99")
    void samplesMatchBaseline() {
        assertTrue(!runner.getResults().isEmpty(), "未找到入库样本: " + SAMPLE_DIR.toAbsolutePath());
        if (ASSERT_METRICS) {
            runner.assertAllPassed(PdfRegressionSupport.MAX_DIFF_RATIO, PdfRegressionSupport.MAX_MAE,
                    MIN_TEXT_SIMILARITY);
            return;
        }
        // 未开启断言时只输出指标，保证每次 mvn test / CI 都能看到当前差距
        System.out.println(">> 入库样本指标（未开启 -Dofd.regression.assert=true，不做断言）");
        for (RegressionMetrics metrics : runner.getResults()) {
            System.out.println(">> " + metrics);
        }
        System.out.println(">> 报告: " + OUTPUT_DIR.resolve("report.md").toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("入库样本：每个样本都有基准 PDF 与 README 记录")
    void everySampleDocumented() throws IOException {
        Path readme = SAMPLE_DIR.resolve("README.md");
        assertTrue(Files.isRegularFile(readme), "缺少样本说明文件: " + readme.toAbsolutePath());
        String text = new String(Files.readAllBytes(readme), java.nio.charset.StandardCharsets.UTF_8);

        List<String> problems = new ArrayList<>();
        for (RegressionMetrics metrics : runner.getResults()) {
            if (!text.contains(metrics.getSample())) {
                problems.add("README 未记录样本: " + metrics.getSample());
            }
            if (metrics.getSkippedReason() != null) {
                problems.add(metrics.getSample() + ": " + metrics.getSkippedReason());
            }
        }
        assertTrue(problems.isEmpty(), "样本说明不完整：\n  - " + String.join("\n  - ", problems));
    }

    @Test
    @DisplayName("异常容错样本：资源缺失不影响整体转换")
    void tolerantSamplesExportWithoutException() {
        for (RegressionMetrics metrics : runner.getResults()) {
            if (metrics.getSample().startsWith("testImageNotFound")) {
                assertNull(metrics.getSkippedReason(), "异常容错样本未能完成转换: " + metrics);
            }
        }
        for (String error : runner.getErrors()) {
            assertTrue(!error.contains("testImageNotFound"), "异常容错样本转换抛出异常: " + error);
        }
    }
}
