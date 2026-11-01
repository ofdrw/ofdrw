package org.ofdrw.converter.regression;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.FontLoader;
import org.ofdrw.converter.export.CanvasImageExporter;
import org.ofdrw.converter.export.ImageInfo;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地手动入口：使用 {@link CanvasImageExporter} 把 OFD 逐页导出为 PNG。
 * <p>
 * 该入口供 Python 脚本 {@code scripts/canvas_image_match/canvas_image_match.py} 调用，
 * 不随普通 {@code mvn test} 执行（通过 {@link Assumptions} 默认跳过），也不进入 CI。
 * <p>
 * 启用的系统属性（由脚本经 surefire 的 {@code -DargLine} 传入）：
 * <ul>
 *     <li>{@code ofd.image.export=true}：显式启用导出，缺省或 false 时跳过；</li>
 *     <li>{@code ofd.match.dir}：样本目录，必填；</li>
 *     <li>{@code ofd.image.dir}：图片输出根目录，必填，按样本名建立子目录；</li>
 *     <li>{@code ofd.image.dpi}：渲染分辨率，单位 DPI，默认 96；</li>
 *     <li>{@code ofd.font.dir}：额外字体目录，多个目录用英文逗号分隔，可选；</li>
 *     <li>{@code ofd.image.samples}：只导出指定样本名，多个用英文逗号分隔，可选。</li>
 * </ul>
 * <p>
 * 输出约定：每个样本输出到 {@code <image.dir>/<样本名>/page-N.png}（N 从 0 起），
 * 同时在 {@code <image.dir>/manifest.json} 写入本轮导出的样本、页码、宽高与错误。
 * 单个样本失败只记录到 manifest，不中断其它样本。
 *
 * @author ofdrw
 * @since 2.5.1
 */
@Disabled("手工画布导出用例，依赖本地回归数据集。")
class ManualCanvasImageExportTest {

    /**
     * 显式启用导出的系统属性名
     */
    static final String EXPORT_PROPERTY = "ofd.image.export";

    /**
     * 样本目录的系统属性名
     */
    static final String SAMPLE_DIR_PROPERTY = "ofd.match.dir";

    /**
     * 图片输出根目录的系统属性名
     */
    static final String IMAGE_DIR_PROPERTY = "ofd.image.dir";

    /**
     * 渲染分辨率（DPI）的系统属性名
     */
    static final String DPI_PROPERTY = "ofd.image.dpi";

    /**
     * 额外字体目录的系统属性名，多个目录用英文逗号分隔
     */
    static final String FONT_DIR_PROPERTY = "ofd.font.dir";

    /**
     * 只导出指定样本名的系统属性名，多个样本用英文逗号分隔
     */
    static final String SAMPLES_PROPERTY = "ofd.image.samples";

    /**
     * 默认 DPI
     */
    static final double DEFAULT_DPI = 96d;

    /**
     * 毫米与英寸的换算系数，单位：毫米/英寸
     */
    static final double MM_PER_INCH = 25.4d;

    /**
     * 默认字体目录：优先 Windows 挂载目录，其次 Linux 常见目录与用户目录
     */
    static final String[] DEFAULT_FONT_DIRS = {
            "/mnt/c/Windows/Fonts",
            "/usr/share/fonts",
            "/usr/local/share/fonts"
    };

    /**
     * 默认文件名模式：按页码命名，页码从 0 起
     */
    static final String FILENAME_PATTERN = "page-%d";

    /**
     * 手动导出入口：默认跳过，只有显式设置 {@code -Dofd.image.export=true} 时才执行。
     *
     * @throws Exception 目录遍历、导出或 manifest 写入失败
     */
    @Test
    void manualCanvasImageExport() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty(EXPORT_PROPERTY)),
                "OFD 图片导出默认跳过：加 -D" + EXPORT_PROPERTY + "=true 启用"
                        + "（或使用 scripts/canvas_image_match/canvas_image_match.py）");

        Path sampleDir = requiredDirectory(SAMPLE_DIR_PROPERTY);
        Path imageDir = requiredImageDir();
        double dpi = resolveDpi();
        double ppm = dpi / MM_PER_INCH;
        Set<String> sampleFilter = resolveSampleFilter();
        List<String> fontDirs = resolveFontDirs();
        List<String> fontWarnings = new ArrayList<>();

        configureFonts(fontDirs, fontWarnings);

        List<Path> ofdFiles = listOfdFiles(sampleDir, sampleFilter);
        assertTrue(!ofdFiles.isEmpty(), "样本目录中没有可导出的 OFD：" + sampleDir);

        List<SampleResult> results = new ArrayList<>();
        for (Path ofdFile : ofdFiles) {
            results.add(exportSample(ofdFile, imageDir, ppm));
        }

        Path manifest = imageDir.resolve("manifest.json");
        writeManifest(manifest, sampleDir, imageDir, dpi, ppm, sampleFilter, fontDirs, fontWarnings, results);

        long ok = results.stream().filter(r -> "ok".equals(r.status)).count();
        long failed = results.size() - ok;
        System.out.println(">> 样本目录: " + sampleDir);
        System.out.println(">> 图片目录: " + imageDir);
        System.out.println(">> manifest: " + manifest);
        System.out.println(">> 导出完成: " + results.size() + " 个样本，成功 " + ok + "，失败 " + failed);
        for (SampleResult result : results) {
            if (!"ok".equals(result.status)) {
                System.out.println(">> ERROR " + result.stem + ": " + result.error);
            }
        }
    }

    /**
     * 导出单个 OFD 样本；失败时清理该样本的图片目录并记录错误，不抛出异常。
     *
     * @param ofdFile  待导出 OFD
     * @param imageDir 图片输出根目录
     * @param ppm      每毫米像素数量，单位：像素/毫米
     * @return 单样本导出结果
     */
    private static SampleResult exportSample(Path ofdFile, Path imageDir, double ppm) {
        String stem = stripExtension(ofdFile.getFileName().toString());
        Path sampleOutDir = imageDir.resolve(stem);
        SampleResult result = new SampleResult(stem, ofdFile);
        try {
            deleteRecursively(sampleOutDir);
            Files.createDirectories(sampleOutDir);
            try (CanvasImageExporter exporter = new CanvasImageExporter(ofdFile, sampleOutDir, "PNG", ppm)) {
                // 明确导出参数：白底、不限制像素、按页码命名，避免依赖默认值变化
                exporter.setImageFormat("PNG");
                exporter.setBackground(Color.WHITE);
                exporter.setMaxPixels(0);
                exporter.setRotation(0);
                exporter.setFilenamePattern(FILENAME_PATTERN);
                exporter.export();
                for (ImageInfo info : exporter.getImageInfos()) {
                    String relative = imageDir.relativize(info.getPath()).toString().replace('\\', '/');
                    result.pages.add(new ExportedPage(info.getPageIndex(), relative, info.getWidth(), info.getHeight()));
                }
            }
            result.pageCount = result.pages.size();
            result.status = "ok";
        } catch (Exception e) {
            result.status = "export_error";
            result.error = e.getClass().getName() + ": " + e.getMessage();
            try {
                // 清理失败样本的残留图片，避免脚本把半成品当成本轮结果
                deleteRecursively(sampleOutDir);
            } catch (IOException cleanupError) {
                result.error += "；清理残留图片失败: " + cleanupError.getMessage();
            }
        }
        return result;
    }

    /**
     * 配置字体加载器：初始化单例、开启相似字体替换并扫描存在的字体目录。
     *
     * @param fontDirs     待扫描字体目录
     * @param fontWarnings 字体相关告警输出，允许为空列表
     */
    private static void configureFonts(List<String> fontDirs, List<String> fontWarnings) {
        try {
            FontLoader.getInstance();
            FontLoader.setSimilarFontReplace(true);
        } catch (Exception e) {
            fontWarnings.add("字体加载器初始化失败: " + e.getClass().getName() + ": " + e.getMessage());
            return;
        }
        for (String dir : fontDirs) {
            Path path = Paths.get(dir);
            if (!Files.isDirectory(path)) {
                fontWarnings.add("字体目录不存在，已跳过: " + path);
                continue;
            }
            try {
                FontLoader.getInstance().scanFontDir(path.toFile());
            } catch (Exception e) {
                fontWarnings.add("字体目录扫描失败，已跳过: " + path + "（" + e.getMessage() + "）");
            }
        }
    }

    /**
     * 解析字体目录：{@code ofd.font.dir} 优先，未指定时使用默认目录与用户字体目录。
     *
     * @return 字体目录绝对路径列表
     */
    private static List<String> resolveFontDirs() {
        String fromProperty = System.getProperty(FONT_DIR_PROPERTY);
        if (fromProperty != null && !fromProperty.trim().isEmpty()) {
            return Arrays.stream(fromProperty.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> Paths.get(s).toAbsolutePath().normalize().toString())
                    .collect(Collectors.toList());
        }
        List<String> dirs = new ArrayList<>(Arrays.asList(DEFAULT_FONT_DIRS));
        String home = System.getProperty("user.home");
        if (home != null && !home.trim().isEmpty()) {
            dirs.add(Paths.get(home, ".fonts").toString());
        }
        return dirs;
    }

    /**
     * 读取样本过滤条件。
     *
     * @return 样本名集合；未指定时返回空集合表示全部样本
     */
    private static Set<String> resolveSampleFilter() {
        String raw = System.getProperty(SAMPLES_PROPERTY);
        Set<String> names = new LinkedHashSet<>();
        if (raw == null || raw.trim().isEmpty()) {
            return names;
        }
        for (String token : raw.split(",")) {
            String name = token.trim();
            if (name.isEmpty()) {
                continue;
            }
            names.add(stripExtension(name));
        }
        return names;
    }

    /**
     * 列出样本目录中的 OFD 文件，并按样本过滤条件筛选。
     *
     * @param sampleDir    样本目录
     * @param sampleFilter 样本名集合，空集合表示不过滤
     * @return 按文件名排序的 OFD 文件列表
     * @throws IOException 目录读取失败
     */
    private static List<Path> listOfdFiles(Path sampleDir, Set<String> sampleFilter) throws IOException {
        try (Stream<Path> stream = Files.list(sampleDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ofd"))
                    .filter(path -> sampleFilter.isEmpty()
                            || sampleFilter.contains(stripExtension(path.getFileName().toString())))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * 读取并校验样本目录属性。
     *
     * @param propertyName 属性名
     * @return 绝对路径
     */
    private static Path requiredDirectory(String propertyName) {
        String value = System.getProperty(propertyName);
        assertTrue(value != null && !value.trim().isEmpty(),
                "缺少系统属性 -D" + propertyName + "=<样本目录>");
        Path dir = Paths.get(value).toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(dir), "样本目录不存在: " + dir);
        return dir;
    }

    /**
     * 读取并准备图片输出根目录属性。
     *
     * @return 绝对路径
     * @throws IOException 目录创建失败
     */
    private static Path requiredImageDir() throws IOException {
        String value = System.getProperty(IMAGE_DIR_PROPERTY);
        assertTrue(value != null && !value.trim().isEmpty(),
                "缺少系统属性 -D" + IMAGE_DIR_PROPERTY + "=<图片输出目录>");
        Path dir = Paths.get(value).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * 读取 DPI 属性。
     *
     * @return DPI，单位：每英寸像素数
     */
    private static double resolveDpi() {
        String value = System.getProperty(DPI_PROPERTY);
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_DPI;
        }
        double dpi = Double.parseDouble(value.trim());
        assertTrue(dpi > 0, "DPI 必须大于 0: " + dpi);
        return dpi;
    }

    /**
     * 去掉文件名末尾的 .ofd / .pdf 扩展名（不区分大小写）。
     *
     * @param fileName 文件名
     * @return 样本名
     */
    private static String stripExtension(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".ofd") || lower.endsWith(".pdf")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }

    /**
     * 写入 manifest.json。
     *
     * @param manifest     目标文件
     * @param sampleDir    样本目录
     * @param imageDir     图片输出根目录
     * @param dpi          渲染分辨率，单位 DPI
     * @param ppm          每毫米像素数量，单位：像素/毫米
     * @param sampleFilter 样本过滤条件
     * @param fontDirs     已尝试的字体目录
     * @param fontWarnings 字体告警
     * @param results      逐样本导出结果
     * @throws IOException 写入失败
     */
    private static void writeManifest(Path manifest, Path sampleDir, Path imageDir, double dpi, double ppm,
                                      Set<String> sampleFilter, List<String> fontDirs, List<String> fontWarnings,
                                      List<SampleResult> results) throws IOException {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\n");
        sb.append("  \"generated_at\": ").append(jsonString(OffsetDateTime.now(ZoneId.systemDefault()).toString())).append(",\n");
        sb.append("  \"params\": {\n");
        sb.append("    \"samples_dir\": ").append(jsonString(sampleDir.toString())).append(",\n");
        sb.append("    \"image_dir\": ").append(jsonString(imageDir.toString())).append(",\n");
        sb.append("    \"dpi\": ").append(dpi).append(",\n");
        sb.append("    \"ppm\": ").append(ppm).append(",\n");
        sb.append("    \"image_format\": \"PNG\",\n");
        sb.append("    \"background\": \"#FFFFFF\",\n");
        sb.append("    \"max_pixels\": 0,\n");
        sb.append("    \"filename_pattern\": ").append(jsonString(FILENAME_PATTERN)).append(",\n");
        sb.append("    \"sample_filter\": ").append(jsonStringArray(new ArrayList<>(sampleFilter))).append(",\n");
        sb.append("    \"font_dirs\": ").append(jsonStringArray(fontDirs)).append(",\n");
        sb.append("    \"font_warnings\": ").append(jsonStringArray(fontWarnings)).append("\n");
        sb.append("  },\n");
        sb.append("  \"samples\": [\n");
        for (int i = 0; i < results.size(); i++) {
            SampleResult result = results.get(i);
            sb.append("    {\n");
            sb.append("      \"stem\": ").append(jsonString(result.stem)).append(",\n");
            sb.append("      \"ofd\": ").append(jsonString(result.ofd.toString())).append(",\n");
            sb.append("      \"status\": ").append(jsonString(result.status)).append(",\n");
            sb.append("      \"page_count\": ").append(result.pageCount).append(",\n");
            sb.append("      \"error\": ").append(jsonString(result.error)).append(",\n");
            sb.append("      \"pages\": [");
            if (!result.pages.isEmpty()) {
                sb.append("\n");
                for (int p = 0; p < result.pages.size(); p++) {
                    ExportedPage page = result.pages.get(p);
                    sb.append("        {\"page\": ").append(page.index)
                            .append(", \"file\": ").append(jsonString(page.file))
                            .append(", \"width\": ").append(page.width)
                            .append(", \"height\": ").append(page.height).append("}");
                    sb.append(p + 1 < result.pages.size() ? ",\n" : "\n");
                }
                sb.append("      ");
            }
            sb.append("]\n");
            sb.append("    }");
            sb.append(i + 1 < results.size() ? ",\n" : "\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        // Java 8 兼容：Files.writeString 从 Java 11 才提供，这里显式按 UTF-8 写字节
        Files.write(manifest, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 转义 JSON 字符串，{@code null} 输出为 JSON null。
     *
     * @param value 原始字符串
     * @return JSON 字符串字面量或 null
     */
    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * 输出字符串数组的 JSON 字面量。
     *
     * @param values 字符串列表
     * @return JSON 数组
     */
    private static String jsonStringArray(List<String> values) {
        return values.stream().map(ManualCanvasImageExportTest::jsonString)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * 递归删除文件或目录，不存在时直接返回。
     *
     * @param target 目标路径
     * @throws IOException 删除失败
     */
    private static void deleteRecursively(Path target) throws IOException {
        if (!Files.exists(target)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(target)) {
            List<Path> paths = stream.sorted(java.util.Comparator.reverseOrder()).collect(Collectors.toList());
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    /**
     * 单样本导出结果
     */
    private static final class SampleResult {
        /**
         * 样本名
         */
        final String stem;
        /**
         * 源 OFD 文件
         */
        final Path ofd;
        /**
         * 导出状态：ok / export_error
         */
        String status = "ok";
        /**
         * 导出页数
         */
        int pageCount;
        /**
         * 错误信息，成功时为 null
         */
        String error;
        /**
         * 成功导出的页面
         */
        final List<ExportedPage> pages = new ArrayList<>();

        SampleResult(String stem, Path ofd) {
            this.stem = stem;
            this.ofd = ofd;
        }
    }

    /**
     * 单页导出信息
     */
    private static final class ExportedPage {
        /**
         * 页码，从 0 起
         */
        final int index;
        /**
         * 相对图片根目录的文件路径（POSIX 分隔符）
         */
        final String file;
        /**
         * 图片宽度，单位：像素
         */
        final int width;
        /**
         * 图片高度，单位：像素
         */
        final int height;

        ExportedPage(int index, String file, int width, int height) {
            this.index = index;
            this.file = file;
            this.width = width;
            this.height = height;
        }
    }
}