package org.ofdrw.converter.regression;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * OFD → PDF 回归比较工具
 * <p>
 * 提供三件事：
 * <ol>
 *     <li>把 PDF 渲染为位图并逐页与基准 PDF 做像素比较（默认 72 dpi）；</li>
 *     <li>用 PDFBox 提取两方的文本并按规范化文本计算相似度；</li>
 *     <li>可选地把基准/实际渲染图与差异图落盘，交由人工目视核对。</li>
 * </ol>
 * <p>
 * 本类只做“读与比”，不直接引用转换器主代码，因此入库样本测试和本地手动脚本可以共用同一套指标口径，
 * 也可以在 converter 主代码编译失败时独立编译运行。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfRegressionSupport {

    /**
     * 默认渲染分辨率（dpi），与设计决策 5 一致
     */
    public static final float DEFAULT_DPI = 72f;

    /**
     * 差异像素比例上限，0.5%
     */
    public static final double MAX_DIFF_RATIO = 0.005d;

    /**
     * 平均绝对误差上限 2/255
     */
    public static final double MAX_MAE = 2d / 255d;

    /**
     * 文本覆盖率下限（决策 5）：基准文本必须被实际文本完整覆盖
     */
    public static final double MIN_TEXT = 0.99d;

    /**
     * 工具类不允许实例化
     */
    private PdfRegressionSupport() {
    }

    /**
     * 读取 PDF 页数
     *
     * @param pdf PDF 文件路径
     * @return 页数
     * @throws IOException 文件读取失败
     */
    public static int pageCount(Path pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            return document.getNumberOfPages();
        }
    }

    /**
     * 提取 PDF 全部文本
     *
     * @param pdf PDF 文件路径
     * @return 原文文本（保留 PDFBox 的换行与空格）
     * @throws IOException 读取失败
     */
    public static String extractText(Path pdf) throws IOException {
        return extractText(pdf, true);
    }

    /**
     * 提取 PDF 全部文本
     * <p>
     * 两种口径用途不同（captain 裁决 B）：
     * <ul>
     *     <li><b>按位置排序</b>（{@code sortByPosition = true}）：反映真实阅读顺序，
     *     用于展示与相似度指标；</li>
     *     <li><b>不排序</b>（{@code sortByPosition = false}）：内容流原始顺序，
     *     用于覆盖率指标——覆盖率只关心“基准字符是否齐全”，不应受行合并策略影响。</li>
     * </ul>
     * 背景：PDFBox 按<b>字号</b>决定行合并阈值，而 OFD 的 {@code Size} 是毫米、换算成 pt 后
     * 字号大于基准 PDF 的标注值（例如竖排文字基准标 fs=3.00、实际 9.0pt），
     * 排序提取时会把同一竖列的字符拆成不同行并插入到其它行中，导致字符顺序错乱、
     * 覆盖率被误扣分。视觉上二者逐像素一致，因此这不是渲染缺陷。
     *
     * @param pdf             PDF 文件路径
     * @param sortByPosition 是否按页面位置排序
     * @return 提取到的文本（保留 PDFBox 的换行与空格）
     * @throws IOException 读取失败
     */
    public static String extractText(Path pdf, boolean sortByPosition) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(sortByPosition);
            return stripper.getText(document);
        }
    }

    /**
     * 在指定分辨率下渲染 PDF 全部页面
     *
     * @param pdf PDF 文件路径
     * @param dpi 渲染分辨率（dpi）
     * @return 逐页位图
     * @throws IOException 读取或渲染失败
     */
    public static List<BufferedImage> render(Path pdf, float dpi) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                images.add(renderer.renderImageWithDPI(i, dpi, ImageType.RGB));
            }
        }
        return images;
    }

    /**
     * 比较两份 PDF 的像素与文本
     *
     * @param sample   样本名（不含扩展名），用于指标标识
     * @param expected 基准 PDF 路径
     * @param actual   实际生成的 PDF 路径
     * @param dpi      渲染分辨率（dpi）
     * @param outDir   渲染图落盘目录；为空表示不落盘
     * @return 指标汇总
     * @throws IOException 读取或渲染失败
     */
    public static RegressionMetrics compare(String sample, Path expected, Path actual, float dpi, Path outDir)
            throws IOException {
        if (!Files.isRegularFile(expected)) {
            return skipped(sample, dpi, "基准 PDF 缺失: " + expected.getFileName());
        }
        if (!Files.isRegularFile(actual)) {
            return skipped(sample, dpi, "转换结果不存在: " + actual.getFileName());
        }
        List<BufferedImage> expectedImages = render(expected, dpi);
        List<BufferedImage> actualImages = render(actual, dpi);
        List<RegressionMetrics.PageComparison> pages = new ArrayList<>();
        int count = Math.max(expectedImages.size(), actualImages.size());
        for (int i = 0; i < count; i++) {
            if (i >= expectedImages.size()) {
                // 基准缺页：不计像素，但页数断言会失败
                pages.add(new RegressionMetrics.PageComparison(i, null));
                continue;
            }
            ImageDiff diff = ImageDiff.compare(expectedImages.get(i),
                    i < actualImages.size() ? actualImages.get(i) : blankLike(expectedImages.get(i)));
            pages.add(new RegressionMetrics.PageComparison(i, diff));
            if (outDir != null) {
                writeComparisonImages(outDir, sample, i, expectedImages.get(i),
                        i < actualImages.size() ? actualImages.get(i) : null);
            }
        }
        // 覆盖率用「不排序」文本（只看字符齐全度），相似度用「按位置排序」文本（反映阅读顺序）
        TextComparison text = TextComparison.compare(
                extractText(expected), extractText(actual),
                extractText(expected, false), extractText(actual, false));
        return new RegressionMetrics(sample, dpi, pages,
                expectedImages.size(), actualImages.size(), text, null);
    }

    /**
     * 比较两份 PDF（不落盘渲染图）
     *
     * @param sample   样本名
     * @param expected 基准 PDF 路径
     * @param actual   实际生成的 PDF 路径
     * @param dpi      渲染分辨率（dpi）
     * @return 指标汇总
     * @throws IOException 读取或渲染失败
     */
    public static RegressionMetrics compare(String sample, Path expected, Path actual, float dpi) throws IOException {
        return compare(sample, expected, actual, dpi, null);
    }

    /**
     * 构造与基准同尺寸的空白图，用于“实际缺页”的差异计算
     *
     * @param reference 基准图
     * @return 空白图
     */
    private static BufferedImage blankLike(BufferedImage reference) {
        return new BufferedImage(reference.getWidth(), reference.getHeight(), BufferedImage.TYPE_INT_RGB);
    }

    /**
     * 构造“跳过比较”的指标
     *
     * @param sample 样本名
     * @param dpi    渲染分辨率（dpi）
     * @param reason 跳过原因
     * @return 指标汇总
     */
    private static RegressionMetrics skipped(String sample, float dpi, String reason) {
        return new RegressionMetrics(sample, dpi, new ArrayList<>(), -1, -1, null, reason);
    }

    /**
     * 落盘基准图、实际图与差异图，便于目视核对
     * <p>
     * 文件命名：{@code <样本>-p<页码>-expected.png}、{@code -actual.png}、{@code -diff.png}。
     *
     * @param outDir   输出目录，不存在时自动创建
     * @param sample   样本名
     * @param index    页码，从 0 起
     * @param expected 基准图
     * @param actual   实际图，可为空（实际缺页）
     * @throws IOException 写文件失败
     */
    private static void writeComparisonImages(Path outDir, String sample, int index,
                                              BufferedImage expected, BufferedImage actual) throws IOException {
        Files.createDirectories(outDir);
        String prefix = sample + "-p" + index;
        writePng(expected, outDir.resolve(prefix + "-expected.png"));
        if (actual == null) {
            return;
        }
        writePng(actual, outDir.resolve(prefix + "-actual.png"));
        writePng(diffImage(expected, actual), outDir.resolve(prefix + "-diff.png"));
    }

    /**
     * 生成差异图：差异像素标红，其余像素以基准图灰度淡显
     *
     * @param expected 基准图
     * @param actual   实际图
     * @return 差异图
     */
    private static BufferedImage diffImage(BufferedImage expected, BufferedImage actual) {
        int width = Math.max(expected.getWidth(), actual.getWidth());
        int height = Math.max(expected.getHeight(), actual.getHeight());
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int e = x < expected.getWidth() && y < expected.getHeight() ? expected.getRGB(x, y) : 0xFFFFFFFF;
                int a = x < actual.getWidth() && y < actual.getHeight() ? actual.getRGB(x, y) : 0xFFFFFFFF;
                int er = (e >> 16) & 0xFF;
                int eg = (e >> 8) & 0xFF;
                int eb = e & 0xFF;
                int ar = (a >> 16) & 0xFF;
                int ag = (a >> 8) & 0xFF;
                int ab = a & 0xFF;
                boolean differ = Math.abs(er - ar) > ImageDiff.DIFF_PIXEL_THRESHOLD
                        || Math.abs(eg - ag) > ImageDiff.DIFF_PIXEL_THRESHOLD
                        || Math.abs(eb - ab) > ImageDiff.DIFF_PIXEL_THRESHOLD;
                int rgb;
                if (differ) {
                    rgb = 0xFF0000;
                } else {
                    int gray = (er * 30 + eg * 59 + eb * 11) / 100;
                    gray = 255 - (255 - gray) / 3;
                    rgb = (gray << 16) | (gray << 8) | gray;
                }
                out.setRGB(x, y, rgb);
            }
        }
        return out;
    }

    /**
     * 写 PNG
     *
     * @param image 位图
     * @param file  目标文件
     * @throws IOException 写文件失败
     */
    private static void writePng(BufferedImage image, Path file) throws IOException {
        javax.imageio.ImageIO.write(image, "png", file.toFile());
    }

    /**
     * 页宽高（pt）是否一致，用于“页面框”断言
     *
     * @param expected 基准 PDF
     * @param actual   实际 PDF
     * @return 页数一致且每页 MediaBox 宽高一致返回 true
     * @throws IOException 读取失败
     */
    public static boolean samePageBox(Path expected, Path actual) throws IOException {
        try (PDDocument e = Loader.loadPDF(expected.toFile());
             PDDocument a = Loader.loadPDF(actual.toFile())) {
            if (e.getNumberOfPages() != a.getNumberOfPages()) {
                return false;
            }
            for (int i = 0; i < e.getNumberOfPages(); i++) {
                PDRectangle eb = e.getPage(i).getMediaBox();
                PDRectangle ab = a.getPage(i).getMediaBox();
                if (Math.abs(eb.getWidth() - ab.getWidth()) > 0.5
                        || Math.abs(eb.getHeight() - ab.getHeight()) > 0.5) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 页尺寸描述，用于失败信息
     *
     * @param pdf PDF 路径
     * @param index 页码，从 0 起
     * @return 形如 “595.28x841.89”
     * @throws IOException 读取失败
     */
    public static String pageSize(Path pdf, int index) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            if (index >= document.getNumberOfPages()) {
                return "N/A";
            }
            PDPage page = document.getPage(index);
            PDRectangle box = page.getMediaBox();
            return String.format("%.2fx%.2f", box.getWidth(), box.getHeight());
        }
    }

    /**
     * 被验证的导出器抽象
     * <p>
     * 本接口刻意不引用 converter 主代码的 {@code OFDExporter}：回归框架需要在主代码编译失败时仍能编译与自检，
     * 具体导出实现对它的适配放在 {@code PDFRegressionTest} 等测试类中完成。
     */
    public interface Exporter {

        /**
         * 把 OFD 导出为 PDF
         *
         * @param ofd OFD 文件路径
         * @param pdf 输出 PDF 路径
         * @throws IOException 导出失败
         */
        void export(Path ofd, Path pdf) throws IOException;

        /**
         * 上一次导出的字体替换数量（决策 7：替换只告警、不阻断）
         * <p>
         * 默认返回 0，表示无法判定；实现方（如 {@code PDFExporter}）可覆盖返回真实数量，
         * 回归框架据此识别“纯字体替换样本”（known-difference），不会把真实缺陷误标为已知差异。
         *
         * @return 发生替换的字体数量
         */
        default int lastFontSubstitutionCount() {
            return 0;
        }
    }
}
