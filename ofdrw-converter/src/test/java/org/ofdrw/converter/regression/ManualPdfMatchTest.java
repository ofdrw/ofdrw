package org.ofdrw.converter.regression;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.export.OFDExporter;
import org.ofdrw.converter.export.PDFExporter;
import org.ofdrw.converter.export.PDFExporterPDFBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地手动脚本入口
 * <p>
 * 读取不入库的 {@code ofd_pdf_match/} 全量样本，输出逐样本像素/文本指标报告，
 * <b>不随 {@code mvn test} 执行、不进入 CI</b>（设计第 7.2.1 节、决策 22）。
 * <p>
 * 执行方式（在 {@code ofdrw-converter} 目录下）：
 * <pre>
 *     ./scripts/local-pdf-match.sh                # 默认样本目录 ofd_pdf_match/
 *     ./scripts/local-pdf-match.sh /path/to/dir   # 指定样本目录
 * </pre>
 * 测试名以 {@code manual} 开头，须用 {@code -Dtest=...} 显式选中，或用 Maven 模板
 * {@code -Dtest='!manual*'} 排除；报告与渲染图输出到 {@code target/local-pdf-match/}。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class ManualPdfMatchTest {

    /**
     * 默认样本目录候选：优先仓库根目录（模块目录下执行），其次当前目录
     */
    static final String[] DEFAULT_DIRS = {"../ofd_pdf_match", "ofd_pdf_match"};

    /**
     * 报告输出目录
     */
    static final Path OUTPUT_DIR = Paths.get("target/local-pdf-match");

    /**
     * 样本目录
     */
    private Path sampleDir;

    /**
     * 定位样本目录，并清空上次的输出，保证每次报告只反映本次结果
     *
     * @throws IOException 目录操作失败
     */
    @BeforeEach
    void locateSamples() throws IOException {
        String fromProperty = System.getProperty("ofd.match.dir");
        if (fromProperty != null && !fromProperty.trim().isEmpty()) {
            sampleDir = Paths.get(fromProperty);
        } else {
            // 仓库根目录的 ofd_pdf_match 是默认数据源；实际路径由脚本解析后通过属性传入
            for (String candidate : DEFAULT_DIRS) {
                Path candidateDir = Paths.get(candidate);
                if (Files.isDirectory(candidateDir)) {
                    sampleDir = candidateDir;
                    break;
                }
            }
        }
        if (Files.isDirectory(OUTPUT_DIR)) {
            deleteRecursively(OUTPUT_DIR);
        }
    }

    /**
     * 全量样本报告的启用开关对应的系统属性名，取值为 true 时执行全量报告
     */
    static final String FULL_RUN_PROPERTY = "ofd.match.full";

    /**
     * 全量样本报告：逐样本指标表 + 渲染图
     * <p>
     * 全量 58 组样本约需 4~8 分钟（内存与耗时都较大），因此默认 {@code Assumptions.assumeTrue} 跳过；
     * 本地脚本 {@code scripts/local-pdf-match.sh} 通过 {@code -Dofd.match.full=true} 启用。
     *
     * @throws Exception 样本遍历或报告写出失败
     */
    @Test
    void manualOfdPdfMatchReport() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty(FULL_RUN_PROPERTY)),
                "全量样本报告默认跳过：加 -D" + FULL_RUN_PROPERTY + "=true 启用（或使用 scripts/local-pdf-match.sh）");
        assertTrue(sampleDir != null, "未找到 ofd_pdf_match 样本目录，请用 -Dofd.match.dir=... 指定");
        PdfRegressionRunner runner = new PdfRegressionRunner(PdfRegressionSupport.DEFAULT_DPI,
                OUTPUT_DIR.resolve("render"), new PdfRegressionSupport.Exporter() {
                    @Override
                    public void export(Path ofd, Path pdf) throws IOException {
                        // 必须关闭导出器才会写出 PDF：PDFExporter 在 export() 期间只渲染到内存
                        try (OFDExporter out = new PDFExporter(ofd, pdf)) {
                            out.export();
                        }
                    }
                });
        runner.runDirectory(sampleDir);
        Path report = OUTPUT_DIR.resolve("report.md");
        runner.writeMarkdown(report, "OFD → PDF 本地回归报告（" + sampleDir + "）",
                PdfRegressionSupport.MAX_MAE, 0.99d);
        System.out.println(">> 样本目录: " + sampleDir.toAbsolutePath());
        System.out.println(">> 报告: " + report.toAbsolutePath().normalize());
        for (RegressionMetrics metrics : runner.getResults()) {
            System.out.println(">> " + metrics);
        }
        for (String error : runner.getErrors()) {
            System.out.println(">> ERROR " + error);
        }
        assertTrue(!runner.getResults().isEmpty(), "样本目录中没有可比对的 OFD: " + sampleDir);
    }

    /**
     * 框架冒烟：渲染可复现、自比较一致、结构可读、异常样本不中断
     *
     * @throws Exception 读写失败
     */
    @Test
    void manualRendererAgreement() throws Exception {
        Path dir = OUTPUT_DIR.resolve("appendix");
        Files.createDirectories(dir);
        Path ofd = Paths.get("src/test/resources/pdf-match/helloworld.ofd");
        assertTrue(Files.isRegularFile(ofd), "缺少测试用 OFD: " + ofd.toAbsolutePath());
        Path copied = dir.resolve("helloworld_copy.ofd");
        Files.copy(ofd, copied);
        Path pdf = dir.resolve("helloworld_copy.pdf");
        PdfRegressionSupport.Exporter exporter = (o, p) -> {
            try (OFDExporter out = new PDFExporterPDFBox(o, p)) {
                out.export();
            }
        };
        exporter.export(copied, pdf);

        // 1) 空白页与自身比较：差异为 0
        ImageDiff blank = ImageDiff.compare(new java.awt.image.BufferedImage(10, 10,
                java.awt.image.BufferedImage.TYPE_INT_RGB),
                new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB));
        assertEquals(0, blank.getDiffPixels());

        // 2) 同一 PDF 自比较：像素与文本均一致，页框一致
        RegressionMetrics self = PdfRegressionSupport.compare("self", pdf, pdf, PdfRegressionSupport.DEFAULT_DPI);
        assertEquals(1d, self.getText().getSimilarity(), 1e-9);
        assertTrue(self.isPixelPassed(PdfRegressionSupport.MAX_DIFF_RATIO, PdfRegressionSupport.MAX_MAE));
        assertTrue(PdfRegressionSupport.samePageBox(pdf, pdf));

        // 3) 结构读取可用
        PdfStructure structure = PdfStructure.of(pdf);
        assertTrue(structure.getPageCount() >= 1);

        // 4) 基准缺失时给出跳过原因，而不是抛出异常
        RegressionMetrics missing = PdfRegressionSupport.compare("missing",
                dir.resolve("not-exist.pdf"), pdf, PdfRegressionSupport.DEFAULT_DPI);
        assertTrue(missing.getSkippedReason() != null);
        assertFalse(PdfRegressionRunner.isPassed(missing, PdfRegressionSupport.MAX_DIFF_RATIO,
                PdfRegressionSupport.MAX_MAE, 0.99d));

        // 5) 转换失败的样本只记录错误，不中断其它样本
        Path bad = dir.resolve("broken.ofd");
        createBrokenOfd(bad);
        PdfRegressionRunner runner = new PdfRegressionRunner(PdfRegressionSupport.DEFAULT_DPI, null,
                new PdfRegressionSupport.Exporter() {
                    @Override
                    public void export(Path o, Path p) throws IOException {
                        try (OFDExporter out = new PDFExporterPDFBox(o, p)) {
                            out.export();
                        }
                    }
                });
        runner.runDirectory(dir);
        assertFalse(runner.getErrors().isEmpty());
        assertTrue(runner.getErrors().get(0).startsWith("broken"));
    }

    /**
     * 生成一个无法解析的 OFD（缺少 OFD.xml）
     *
     * @param file 目标文件
     * @throws IOException 写文件失败
     */
    private static void createBrokenOfd(Path file) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file))) {
            zos.putNextEntry(new ZipEntry("dummy.txt"));
            zos.write("not an ofd".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    /**
     * 递归删除目录
     *
     * @param dir 目录
     * @throws IOException 删除失败
     */
    private static void deleteRecursively(Path dir) throws IOException {
        // 失败重试：先关闭流再逐项删除，规避文件句柄未及时释放（Windows 上尤其明显）导致的“目录非空”
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                deleteOnce(dir);
                return;
            } catch (IOException e) {
                last = e;
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw last;
    }

    /**
     * 单次递归删除
     *
     * @param dir 目录
     * @throws IOException 删除失败
     */
    private static void deleteOnce(Path dir) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.walk(dir)) {
            java.util.List<Path> paths = new java.util.ArrayList<>();
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(paths::add);
            for (Path p : paths) {
                Files.deleteIfExists(p);
            }
        }
    }
}
