package org.ofdrw.converter.regression;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * PDF 回归用例执行器
 * <p>
 * 把“一批 {@code <样本名>.ofd + <样本名>.pdf} 组合”转换成 {@link RegressionMetrics} 列表：
 * 先用指定导出器把 OFD 转成 PDF，再与基准 PDF 比较像素与文本。
 * <p>
 * 同一套逻辑被两处复用：
 * <ul>
 *     <li>入库小样本测试（{@code org.ofdrw.converter.export.PDFRegressionTest}），随 {@code mvn test} 执行；</li>
 *     <li>本地手动脚本（{@code ofdrw-converter/scripts/local-pdf-match.sh}），读取不入库的 {@code ofd_pdf_match/}。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfRegressionRunner {

    /**
     * 结果对象
     */
    private final List<RegressionMetrics> results = new ArrayList<>();

    /**
     * 渲染分辨率（dpi）
     */
    private final float dpi;

    /**
     * 渲染图落盘目录，可为空
     */
    private final Path outputDir;

    /**
     * 导出器工厂
     */
    private final PdfRegressionSupport.Exporter exporter;

    /**
     * 单个样本处理失败的信息（不阻断其它样本）
     */
    private final List<String> errors = new ArrayList<>();

    /**
     * @param dpi       渲染分辨率（dpi）
     * @param outputDir 渲染图落盘目录，为空表示不落盘
     * @param exporter  导出器工厂
     */
    public PdfRegressionRunner(float dpi, Path outputDir, PdfRegressionSupport.Exporter exporter) {
        this.dpi = dpi;
        this.outputDir = outputDir;
        this.exporter = exporter;
    }

    /**
     * 用默认参数（72 dpi、不落盘）执行
     *
     * @param exporter 导出器工厂
     * @return 执行器
     */
    public static PdfRegressionRunner of(PdfRegressionSupport.Exporter exporter) {
        return new PdfRegressionRunner(PdfRegressionSupport.DEFAULT_DPI, null, exporter);
    }

    /**
     * @return 渲染分辨率（dpi）
     */
    public float getDpi() {
        return dpi;
    }

    /**
     * @return 已收集的结果
     */
    public List<RegressionMetrics> getResults() {
        return results;
    }

    /**
     * @return 单个样本处理失败信息
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * 在目录中查找 {@code .ofd} 样本（按文件名排序），并对每个样本执行转换与比较
     * <p>
     * 找不到任何 {@code .ofd} 时仅记录错误信息，不抛异常；是否视为失败由调用方决定。
     *
     * @param sampleDir 样本目录，可嵌套子目录
     * @return 本执行器，便于链式调用
     * @throws IOException 目录遍历失败
     */
    public PdfRegressionRunner runDirectory(Path sampleDir) throws IOException {
        if (!Files.isDirectory(sampleDir)) {
            errors.add("样本目录不存在: " + sampleDir);
            return this;
        }
        List<Path> ofds = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(sampleDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".ofd"))
                    .sorted()
                    .forEach(ofds::add);
        }
        if (ofds.isEmpty()) {
            errors.add("样本目录中没有 .ofd 文件: " + sampleDir);
            return this;
        }
        for (Path ofd : ofds) {
            runSample(sampleDir, ofd);
        }
        return this;
    }

    /**
     * 对单个样本执行转换与比较
     * <p>
     * 转换异常不抛出，记录到 {@link #getErrors()}，以便一次性看到全部问题。
     *
     * @param sampleDir 样本根目录（用于拼接基准 PDF 与输出目录）
     * @param ofd       OFD 样本路径
     * @return 指标；转换失败时返回空
     */
    public RegressionMetrics runSample(Path sampleDir, Path ofd) {
        String fileName = ofd.getFileName().toString();
        String name = fileName.substring(0, fileName.length() - 4);
        Path basePdf = ofd.resolveSibling(name + ".pdf");
        Path outDir = outputDir == null ? null : outputDir.resolve(relative(sampleDir, ofd));
        Path outPdf = outDir == null ? null : outDir.resolve(name + ".pdf");
        if (outPdf != null) {
            try {
                Files.createDirectories(outDir);
            } catch (IOException e) {
                errors.add(name + ": 创建输出目录失败 - " + e.getMessage());
                return null;
            }
        }
        try {
            Path generated;
            if (outPdf == null) {
                Path temp = Files.createTempFile("ofdrw-regression-", ".pdf");
                temp.toFile().deleteOnExit();
                exporter.export(ofd, temp);
                generated = temp;
            } else {
                exporter.export(ofd, outPdf);
                generated = outPdf;
            }
            RegressionMetrics metrics = PdfRegressionSupport.compare(name, basePdf, generated, dpi, outDir);
            // 纯字体替换样本标记为已知差异（文本达标、仅像素超），真实缺陷不会被标记
            if (metrics != null) {
                metrics.markKnownDifferenceIfFontSubstituted(exporter.lastFontSubstitutionCount(),
                        PdfRegressionSupport.MAX_DIFF_RATIO, PdfRegressionSupport.MAX_MAE,
                        PdfRegressionSupport.MIN_TEXT);
            }
            results.add(metrics);
            return metrics;
        } catch (Exception e) {
            errors.add(name + ": 处理失败 - " + e);
            return null;
        }
    }

    /**
     * 生成 Markdown 报告文本
     *
     * @param title    报告标题
     * @param maxMae   平均绝对误差阈值（0~1）
     * @param minText  文本相似度阈值（0~1）
     * @return Markdown 文本
     */
    public String toMarkdown(String title, double maxMae, double minText) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");
        sb.append("渲染分辨率（dpi）：").append(dpi).append("\n\n");
        sb.append("| 样本 | 页数(实际/基准) | 差异像素比例 | MAE(/255) | 文本覆盖率 | 文本相似度 | 结论 |\n");
        sb.append("|---|---|---|---|---|---|---|\n");
        int pass = 0;
        int fail = 0;
        for (RegressionMetrics m : results) {
            String verdict;
            if (m.getSkippedReason() != null) {
                verdict = "跳过";
            } else if (m.isKnownDifference()) {
                verdict = "通过（已知差异：字体替换）";
                pass++;
            } else if (isPassed(m, PdfRegressionSupport.MAX_DIFF_RATIO, maxMae, minText)) {
                verdict = "通过";
                pass++;
                if (m.isTextSuspicious(minText)) {
                    verdict = "通过（⚠ 文本异常：覆盖率达标但相似度过低）";
                }
            } else {
                verdict = "**不通过**";
                fail++;
            }
            sb.append("| ").append(m.getSample())
                    .append(" | ").append(m.getActualPageCount()).append('/').append(m.getExpectedPageCount())
                    .append(" | ").append(String.format("%.4f%%", m.getWorstDiffRatio() * 100d))
                    .append(" | ").append(String.format("%.4f", m.getWorstMeanAbsoluteError() * 255d))
                    .append(" | ").append(m.getText() == null ? "N/A" : String.format("%.4f", m.getText().getCoverage()))
                    .append(" | ").append(m.getText() == null ? "N/A" : String.format("%.4f", m.getText().getSimilarity()))
                    .append(" | ").append(verdict).append(" |\n");
        }
        sb.append("\n合计：通过 ").append(pass).append("，不通过 ").append(fail)
                .append("，跳过 ").append(results.size() - pass - fail).append("。\n");
        if (!errors.isEmpty()) {
            sb.append("\n## 异常\n\n");
            for (String error : errors) {
                sb.append("- ").append(error).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * 写出 Markdown 报告
     *
     * @param file    报告文件
     * @param title   报告标题
     * @param maxMae  平均绝对误差阈值（0~1）
     * @param minText 文本相似度阈值（0~1）
     * @throws IOException 写文件失败
     */
    public void writeMarkdown(Path file, String title, double maxMae, double minText) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, toMarkdown(title, maxMae, minText).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 判断单个样本是否满足全部阈值
     *
     * @param metrics   指标
     * @param maxDiff   差异像素比例阈值（0~1）
     * @param maxMae    平均绝对误差阈值（0~1）
     * @param minText   文本相似度阈值（0~1）
     * @return 满足返回 true
     */
    public static boolean isPassed(RegressionMetrics metrics, double maxDiff, double maxMae, double minText) {
        if (metrics.getSkippedReason() != null) {
            return false;
        }
        if (metrics.isKnownDifference()) {
            // 已知差异：纯字体替换导致，文本达标、仅像素超阈值（决策 7）
            return true;
        }
        return metrics.isPageCountPassed()
                && metrics.isPixelPassed(maxDiff, maxMae)
                && metrics.isTextPassed(minText);
    }

    /**
     * 断言全部样本满足阈值
     * <p>
     * 失败信息中包含逐样本指标与导致失败的具体项，便于定位。
     *
     * @param maxDiff 差异像素比例阈值（0~1）
     * @param maxMae  平均绝对误差阈值（0~1）
     * @param minText 文本相似度阈值（0~1）
     */
    public void assertAllPassed(double maxDiff, double maxMae, double minText) {
        StringBuilder failures = new StringBuilder();
        for (RegressionMetrics m : results) {
            if (isPassed(m, maxDiff, maxMae, minText)) {
                continue;
            }
            failures.append("  - ").append(m);
            if (m.getSkippedReason() != null) {
                failures.append(" 原因=").append(m.getSkippedReason());
            } else {
                if (!m.isPageCountPassed()) {
                    failures.append(" [页数不一致]");
                }
                if (!m.isPixelPassed(maxDiff, maxMae)) {
                    failures.append(" [像素超阈值]");
                }
                if (!m.isTextPassed(minText) && m.getText() != null) {
                    failures.append(" [文本超阈值] ").append(m.getText().getFragments());
                }
            }
            failures.append('\n');
        }
        if (!errors.isEmpty()) {
            failures.append("  异常：").append(errors).append('\n');
        }
        Assertions.assertTrue(failures.length() == 0 && !results.isEmpty(),
                "PDF 回归未通过：\n" + failures + "  阈值 diff<=" + maxDiff + " mae<=" + maxMae + " text>=" + minText);
    }

    /**
     * 计算样本相对根目录的路径，作为输出子目录
     *
     * @param sampleDir 样本根目录
     * @param file      样本文件
     * @return 相对路径（为空时返回 “.”）
     */
    private static String relative(Path sampleDir, Path file) {
        Path rel = sampleDir.toAbsolutePath().relativize(file.toAbsolutePath()).getParent();
        return rel == null ? "." : rel.toString();
    }
}
