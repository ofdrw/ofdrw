package org.ofdrw.converter.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.reader.OFDReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CanvasImageExporter} 单元测试。
 * <p>
 * 全部输入来自 {@code src/test/resources} 中既有 OFD 文档，不依赖系统字体与外部样本：
 * <ul>
 *     <li>{@code 999.ofd}：5 页，首两页页面尺寸不同（210x140mm、210x297mm）；</li>
 *     <li>{@code zsbk.ofd}：2 页，横向 297.0001x210.0001mm、纵向 210.0001x297.0001mm，用于验证向上取整；</li>
 *     <li>{@code helloworld.ofd}：1 页，210x297mm，用于旋转方向验证；</li>
 *     <li>{@code 透明度文字.ofd}、{@code containsJPEG.ofd}：含透明文字与 JPEG 图像资源的文档。</li>
 * </ul>
 * 为使测试在不同机器上稳定，页面像素断言只使用尺寸与几何信息（页码、宽高），
 * 不依赖具体渲染内容。
 *
 * @author 权观宇
 * @since 2026-10-25
 */
class CanvasImageExporterTest {

    /**
     * 测试文档目录前缀
     */
    private static final String RES = "src/test/resources/";

    /**
     * 多页文档：5 页，首两页尺寸不同
     */
    private static final String MULTI_PAGE_OFD = RES + "999.ofd";

    /**
     * 页面尺寸带小数毫米的文档：1 页 297.0001x210.0001mm
     */
    private static final String ODD_SIZE_OFD = RES + "zsbk.ofd";

    /**
     * 单页文档：210x297mm
     */
    private static final String SINGLE_PAGE_OFD = RES + "helloworld.ofd";

    /**
     * 含透明文字的文档
     */
    private static final String ALPHA_TEXT_OFD = RES + "透明度文字.ofd";

    /**
     * 含 JPEG 图像资源的文档
     */
    private static final String JPEG_IMAGE_OFD = RES + "containsJPEG.ofd";

    /**
     * 临时输出目录由 JUnit 创建并清理
     */
    @TempDir
    Path tempDir;

    /**
     * 读取导出图片
     *
     * @param path 图片路径
     * @return 位图
     * @throws IOException 读取失败
     */
    private static BufferedImage read(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image, "图片无法解码：" + path);
        return image;
    }

    /**
     * 按页面毫米尺寸与 ppm 计算向上取整后的像素宽高
     *
     * @param ofdPath OFD 文档
     * @param ppm     每毫米像素数量
     * @return 首两页的 [宽, 高] 像素值
     * @throws IOException 文档解析失败
     */
    private static int[][] ceilPixelSizes(String ofdPath, double ppm) throws IOException {
        try (OFDReader reader = new OFDReader(Paths.get(ofdPath))) {
            int pageCount = Math.min(2, reader.getNumberOfPages());
            int[][] sizes = new int[pageCount][];
            for (int i = 0; i < pageCount; i++) {
                double widthMm = reader.getPageList().get(i).getSize().getWidth();
                double heightMm = reader.getPageList().get(i).getSize().getHeight();
                sizes[i] = new int[]{
                        (int) Math.ceil(ppm * widthMm),
                        (int) Math.ceil(ppm * heightMm)
                };
            }
            return sizes;
        }
    }

    /**
     * 基本导出：未指定页码时导出全部页面，文件数与页数一致，默认按页码命名
     */
    @Test
    @DisplayName("基本导出：全部页面、文件数等于页数、默认 page-页码 命名")
    void exportAllPages() throws Exception {
        final double ppm = 5;
        int pageCount;
        try (OFDReader reader = new OFDReader(Paths.get(MULTI_PAGE_OFD))) {
            pageCount = reader.getNumberOfPages();
        }
        assertEquals(5, pageCount, "测试文档 999.ofd 应为 5 页");

        Path outDir = tempDir.resolve("all");
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(MULTI_PAGE_OFD), outDir, "PNG", ppm)) {
            exporter.export();
            List<Path> files = exporter.getImgFilePaths();
            assertEquals(pageCount, files.size(), "每个页面应生成一个文件");
            List<ImageInfo> infos = exporter.getImageInfos();
            assertEquals(pageCount, infos.size());
            for (int i = 0; i < pageCount; i++) {
                assertEquals(outDir.resolve("page-" + i + ".png"), files.get(i));
                assertEquals(i, infos.get(i).getPageIndex());
                assertTrue(Files.exists(files.get(i)));
                assertTrue(Files.size(files.get(i)) > 0);
            }
        }
    }

    /**
     * 尺寸语义：像素尺寸按 Math.ceil(ppm × 毫米) 向上取整，且导出图片实际尺寸与之一致
     */
    @Test
    @DisplayName("尺寸语义：Math.ceil 向上取整，不裁边")
    void pixelSizeUsesCeil() throws Exception {
        final double ppm = 10;
        // 297.0001mm × 10 = 2970.001px，向上取整为 2971
        int[][] expected = ceilPixelSizes(ODD_SIZE_OFD, ppm);
        assertEquals(2971, expected[0][0], "横向页面宽度应向上取整为 2971 像素");

        Path outDir = tempDir.resolve("ceil");
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(ODD_SIZE_OFD), outDir, "PNG", ppm)) {
            exporter.export(0);
            Path file = exporter.getImgFilePaths().get(0);
            BufferedImage image = read(file);
            assertEquals(expected[0][0], image.getWidth(), "导出宽度应为向上取整后的像素宽度");
            assertEquals(expected[0][1], image.getHeight(), "导出高度应为向上取整后的像素高度");
            // 记录信息与文件实际尺寸一致
            assertEquals(expected[0][0], exporter.getImageInfos().get(0).getWidth());
            assertEquals(expected[0][1], exporter.getImageInfos().get(0).getHeight());
        }
    }

    /**
     * 旋转：90/270 度宽高互换，180 度尺寸不变，且导出文件实际尺寸与信息一致
     */
    @Test
    @DisplayName("旋转：0/90/180/270 的宽高与信息")
    void rotationSwapsDimensions() throws Exception {
        final double ppm = 5;
        int[][] expected = ceilPixelSizes(SINGLE_PAGE_OFD, ppm);
        int baseW = expected[0][0];
        int baseH = expected[0][1];
        assertTrue(baseW != baseH, "测试页面应为非正方形，才能验证宽高互换");

        int[][] cases = {{0, baseW, baseH}, {90, baseH, baseW}, {180, baseW, baseH}, {270, baseH, baseW}};
        for (int[] c : cases) {
            int rotation = c[0];
            int expectedWidth = c[1];
            int expectedHeight = c[2];
            Path outDir = tempDir.resolve("rot-" + rotation);
            try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(SINGLE_PAGE_OFD), outDir, "PNG", ppm)) {
                exporter.setRotation(rotation);
                exporter.export(0);
                ImageInfo info = exporter.getImageInfos().get(0);
                assertEquals(expectedWidth, info.getWidth(), "旋转 " + rotation + " 度后的宽度信息");
                assertEquals(expectedHeight, info.getHeight(), "旋转 " + rotation + " 度后的高度信息");
                assertEquals(rotation, info.getRotation());
                BufferedImage image = read(exporter.getImgFilePaths().get(0));
                assertEquals(expectedWidth, image.getWidth(), "旋转 " + rotation + " 度后的实际图片宽度");
                assertEquals(expectedHeight, image.getHeight(), "旋转 " + rotation + " 度后的实际图片高度");
            }
        }
    }

    /**
     * 旋转方向：180 度输出必须等于 0 度输出中心对称后的像素，90 与 270 度输出必须互为 180 度旋转关系
     * <p>
     * 只比较像素几何位置，不依赖渲染内容与系统字体；若旋转实现方向错误（如 90 度写成逆时针），断言会失败。
     */
    @Test
    @DisplayName("旋转方向：像素映射关系正确")
    void rotationDirectionMapsPixelsCorrectly() throws Exception {
        final double ppm = 3;
        BufferedImage base = renderSinglePage(0, ppm);
        BufferedImage rotated180 = renderSinglePage(180, ppm);
        assertEquals(base.getWidth(), rotated180.getWidth());
        assertEquals(base.getHeight(), rotated180.getHeight());
        // 180 度：目标(x, y) 等于源(W-1-x, H-1-y)
        assertEquals(0, countMismatchedPixels(base, rotated180, (x, y, w, h) -> new int[]{w - 1 - x, h - 1 - y}),
                "180 度输出的每个像素应来自源图中心对称位置");

        BufferedImage rotated90 = renderSinglePage(90, ppm);
        BufferedImage rotated270 = renderSinglePage(270, ppm);
        // 90 度：宽高互换；270 度：宽高互换（两者尺寸相同、像素方向不同）
        assertEquals(base.getHeight(), rotated90.getWidth(), "90 度输出宽度应等于原图高度");
        assertEquals(base.getWidth(), rotated90.getHeight(), "90 度输出高度应等于原图宽度");
        assertEquals(rotated90.getWidth(), rotated270.getWidth());
        assertEquals(rotated90.getHeight(), rotated270.getHeight());
        assertEquals(0, countMismatchedPixels(rotated90, rotated270, (x, y, w, h) -> new int[]{w - 1 - x, h - 1 - y}),
                "90 度与 270 度输出应互为 180 度旋转关系");

        // 90 度顺时针：目标(x, y) 等于源(y, H-1-x)，其中 H 为源图高度
        assertEquals(0, countMismatchedPixels90(base, rotated90),
                "90 度应为顺时针：目标(x, y) = 源(y, H-1-x)");
    }

    /**
     * 渲染单页并按指定角度旋转（默认背景）
     *
     * @param rotation 旋转角度，单位：度
     * @param ppm      每毫米像素数量
     * @return 导出图片
     * @throws Exception 导出或读取失败
     */
    private BufferedImage renderSinglePage(int rotation, double ppm) throws Exception {
        Path outDir = tempDir.resolve("map-" + rotation);
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(SINGLE_PAGE_OFD), outDir, "PNG", ppm)) {
            exporter.setRotation(rotation);
            exporter.export(0);
            return read(exporter.getImgFilePaths().get(0));
        }
    }

    /**
     * 像素映射函数：把目标坐标映射回源图坐标
     */
    @FunctionalInterface
    private interface PixelMapping {
        /**
         * 计算源图坐标
         *
         * @param x 目标横坐标
         * @param y 目标纵坐标
         * @param w 源图宽度
         * @param h 源图高度
         * @return 源图坐标 {x, y}
         */
        int[] map(int x, int y, int w, int h);
    }

    /**
     * 统计按映射关系对不上的像素数量
     *
     * @param source  源图
     * @param target  目标图
     * @param mapping 坐标映射
     * @return 不一致像素数
     */
    private static long countMismatchedPixels(BufferedImage source, BufferedImage target, PixelMapping mapping) {
        int w = source.getWidth();
        int h = source.getHeight();
        int stepX = Math.max(1, w / 150);
        int stepY = Math.max(1, h / 150);
        long mismatch = 0;
        for (int y = 0; y < h; y += stepY) {
            for (int x = 0; x < w; x += stepX) {
                int[] src = mapping.map(x, y, w, h);
                if (src[0] < 0 || src[1] < 0 || src[0] >= w || src[1] >= h) {
                    mismatch++;
                    continue;
                }
                if (source.getRGB(src[0], src[1]) != target.getRGB(x, y)) {
                    mismatch++;
                }
            }
        }
        return mismatch;
    }

    /**
     * 校验顺时针 90 度旋转的像素对应关系
     *
     * @param source 源图（未旋转）
     * @param target 目标图（顺时针 90 度）
     * @return 不一致像素数
     */
    private static long countMismatchedPixels90(BufferedImage source, BufferedImage target) {
        int w = source.getWidth();
        int h = source.getHeight();
        int outW = target.getWidth();
        int outH = target.getHeight();
        int stepX = Math.max(1, outW / 150);
        int stepY = Math.max(1, outH / 150);
        long mismatch = 0;
        for (int y = 0; y < outH; y += stepY) {
            for (int x = 0; x < outW; x += stepX) {
                // 顺时针 90 度：源(y, h-1-x)
                int sx = y;
                int sy = h - 1 - x;
                if (sx < 0 || sy < 0 || sx >= w || sy >= h) {
                    mismatch++;
                    continue;
                }
                if (source.getRGB(sx, sy) != target.getRGB(x, y)) {
                    mismatch++;
                }
            }
        }
        return mismatch;
    }

    /**
     * 背景：默认不透明白色背景；显式 setBackground(null) 时 PNG 存在透明像素
     */
    @Test
    @DisplayName("背景：默认不透明，setBackground(null) 时 PNG 可透明")
    void backgroundIsWhiteByDefaultAndTransparentWhenNull() throws Exception {
        final double ppm = 3;
        Path opaqueDir = tempDir.resolve("bg-default");
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(SINGLE_PAGE_OFD), opaqueDir, "PNG", ppm)) {
            exporter.export(0);
            BufferedImage image = read(exporter.getImgFilePaths().get(0));
            // 默认背景：不透明（alpha=255），RGB 为白色
            int corner = image.getRGB(0, 0);
            assertEquals(0xFF, corner >>> 24, "默认背景应为不透明");
            assertEquals(0xFFFFFF, corner & 0xFFFFFF, "默认背景应为白色");
            int bottomRight = image.getRGB(image.getWidth() - 1, image.getHeight() - 1);
            assertEquals(0xFF, bottomRight >>> 24, "默认背景不应存在透明像素");
            assertEquals(0xFFFFFF, bottomRight & 0xFFFFFF, "默认背景应为白色");
        }

        Path transparentDir = tempDir.resolve("bg-null");
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(SINGLE_PAGE_OFD), transparentDir, "PNG", ppm)) {
            exporter.setBackground(null);
            exporter.export(0);
            BufferedImage image = read(exporter.getImgFilePaths().get(0));
            assertTrue(hasTransparentPixel(image), "setBackground(null) 时 PNG 应存在透明像素");
        }
    }

    /**
     * 判断图片是否存在透明（alpha 不为 255）的像素
     *
     * @param image 位图
     * @return true 表示存在透明像素
     */
    private static boolean hasTransparentPixel(BufferedImage image) {
        int stepX = Math.max(1, image.getWidth() / 200);
        int stepY = Math.max(1, image.getHeight() / 200);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                if ((image.getRGB(x, y) >>> 24) != 0xFF) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 像素上限：超过 setMaxPixels 时抛出 GeneralConvertException，且不产生文件
     */
    @Test
    @DisplayName("像素上限：超限抛出 GeneralConvertException")
    void maxPixelsExceededThrows() throws Exception {
        Path outDir = tempDir.resolve("max-pixels");
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(SINGLE_PAGE_OFD), outDir, "PNG", 20)) {
            exporter.setMaxPixels(1000L);
            GeneralConvertException e = assertThrows(GeneralConvertException.class, () -> exporter.export(0));
            assertTrue(e.getMessage().contains("像素上限"), "异常信息应说明像素上限：" + e.getMessage());
            assertTrue(exporter.getImgFilePaths().isEmpty());
            try (java.util.stream.Stream<Path> files = Files.list(outDir)) {
                assertEquals(0, files.count(), "超限时不应产生图片文件");
            }
        }

        // 上限为 0 表示不限制时，同一分辨率可正常导出
        Path unlimitedDir = tempDir.resolve("max-pixels-unlimited");
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(SINGLE_PAGE_OFD), unlimitedDir, "PNG", 20)) {
            exporter.setMaxPixels(0);
            exporter.export(0);
            assertEquals(1, exporter.getImgFilePaths().size());
        }
    }

    /**
     * 指定页码导出：export(2, 0) 按页码命名（而非按导出序号）
     */
    @Test
    @DisplayName("按页码命名：export(2, 0) 生成 page-2、page-0")
    void exportByPageIndexNamesFiles() throws Exception {
        Path outDir = tempDir.resolve("by-page");
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(MULTI_PAGE_OFD), outDir, "PNG", 5)) {
            exporter.export(2, 0);
            List<Path> files = exporter.getImgFilePaths();
            assertEquals(2, files.size());
            assertEquals(outDir.resolve("page-2.png"), files.get(0));
            assertEquals(outDir.resolve("page-0.png"), files.get(1));
            assertEquals(2, exporter.getImageInfos().get(0).getPageIndex());
            assertEquals(0, exporter.getImageInfos().get(1).getPageIndex());
            // 页面尺寸不同，导出尺寸也应跟随页码
            assertTrue(exporter.getImageInfos().get(0).getHeight() > exporter.getImageInfos().get(1).getHeight(),
                    "第2页（A4 纵向）应高于第0页（横向）");
        }
    }

    /**
     * 重复导出：结果覆盖而不累积，文件数始终等于本次导出的页数
     */
    @Test
    @DisplayName("重复导出：覆盖不累积")
    void repeatedExportReplacesResults() throws Exception {
        Path outDir = tempDir.resolve("repeat");
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(MULTI_PAGE_OFD), outDir, "PNG", 5)) {
            exporter.export(0, 1);
            assertEquals(2, exporter.getImgFilePaths().size());
            exporter.export(0);
            assertEquals(1, exporter.getImgFilePaths().size(), "重复导出不应累积上次结果");
            assertEquals(outDir.resolve("page-0.png"), exporter.getImgFilePaths().get(0));
            assertEquals(1, exporter.getImageInfos().size());
            // 全部页面再次导出仍为 5 个文件，且与本次页面一一对应（无重复条目）
            exporter.export();
            assertEquals(5, exporter.getImgFilePaths().size());
            assertEquals(5, exporter.getImgFilePaths().stream().distinct().count(), "导出结果不应含重复文件");
            for (int i = 0; i < 5; i++) {
                assertTrue(Files.exists(outDir.resolve("page-" + i + ".png")));
            }
        }
    }

    /**
     * close 之后导出：抛出 GeneralConvertException
     */
    @Test
    @DisplayName("close 后导出：抛出 GeneralConvertException")
    void exportAfterCloseThrows() throws Exception {
        Path outDir = tempDir.resolve("closed");
        CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(SINGLE_PAGE_OFD), outDir, "PNG", 5);
        exporter.close();
        GeneralConvertException e = assertThrows(GeneralConvertException.class, exporter::export);
        assertTrue(e.getMessage().contains("已关闭"), "异常信息应说明导出器已关闭：" + e.getMessage());
        // close 可重复调用
        exporter.close();
    }

    /**
     * 外部 OFDReader：导出器关闭时不关闭外部传入的解析器
     */
    @Test
    @DisplayName("外部 OFDReader 归属：export 关闭后解析器仍可用、文件仍可读")
    void externalReaderIsNotClosed() throws Exception {
        Path outDir = tempDir.resolve("external-reader");
        try (OFDReader reader = new OFDReader(Paths.get(MULTI_PAGE_OFD))) {
            int pageCount = reader.getNumberOfPages();
            Path exported;
            try (CanvasImageExporter exporter = new CanvasImageExporter(reader, outDir, "PNG", 5)) {
                exporter.export(0);
                exported = exporter.getImgFilePaths().get(0);
            }
            // 导出器关闭后，外部解析器必须仍处于可用状态（能读取页面与资源）
            assertTrue(Files.exists(exported));
            assertEquals(pageCount, reader.getNumberOfPages());
            assertNotNull(reader.getPageList().get(0).getSize());

            // 复用同一解析器再次导出，仍应成功
            Path secondDir = tempDir.resolve("external-reader-2");
            try (CanvasImageExporter exporter = new CanvasImageExporter(reader, secondDir, "PNG", 5)) {
                exporter.export(1);
                assertEquals(1, exporter.getImgFilePaths().size());
            }
            assertEquals(pageCount, reader.getNumberOfPages(), "两次导出后外部解析器仍可用");
        }
    }

    /**
     * 带透明文字与 JPEG 图像资源的既有文档应能正常导出
     */
    @Test
    @DisplayName("既有文档：透明度文字.ofd 与 containsJPEG.ofd 可导出")
    void exportsDocumentsWithAlphaTextAndJpegImage() throws Exception {
        for (String ofd : new String[]{ALPHA_TEXT_OFD, JPEG_IMAGE_OFD}) {
            Path outDir = tempDir.resolve(Paths.get(ofd).getFileName() + "-out");
            try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(ofd), outDir, "PNG", 5)) {
                exporter.export();
                List<Path> files = exporter.getImgFilePaths();
                assertFalse(files.isEmpty(), ofd + " 应导出至少一页");
                for (Path file : files) {
                    BufferedImage image = read(file);
                    assertTrue(image.getWidth() > 0 && image.getHeight() > 0);
                }
            }
        }
    }

    /**
     * 输出目录为已存在的同名文件时，构造期抛出 IllegalArgumentException
     */
    @Test
    @DisplayName("构造期校验：输出路径是同名文件时抛出 IllegalArgumentException")
    void outDirNotDirectoryIsRejected() throws Exception {
        Path file = tempDir.resolve("not-a-dir.png");
        Files.write(file, new byte[]{0});
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasImageExporter(Paths.get(SINGLE_PAGE_OFD), file, "PNG", 5));
    }

    /**
     * JPG 输出：格式归一化、文件扩展名与像素尺寸
     */
    @Test
    @DisplayName("JPG 输出：别名归一、扩展名与尺寸")
    void jpgOutput() throws Exception {
        Path outDir = tempDir.resolve("jpg");
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(SINGLE_PAGE_OFD), outDir, "jpeg", 5)) {
            exporter.setJpegQuality(0.8f);
            exporter.export(0);
            Path file = exporter.getImgFilePaths().get(0);
            assertEquals("page-0.jpg", file.getFileName().toString());
            BufferedImage image = read(file);
            assertEquals(ImageInfo.class, exporter.getImageInfos().get(0).getClass());
            assertEquals(exporter.getImageInfos().get(0).getWidth(), image.getWidth());
        }
    }

    /**
     * InputStream 构造器与 OFDExporter 接口：导出可用且实现类关系不变
     */
    @Test
    @DisplayName("InputStream 构造器：导出可用")
    void exportFromInputStream() throws Exception {
        Path outDir = tempDir.resolve("stream");
        try (InputStream in = Files.newInputStream(Paths.get(SINGLE_PAGE_OFD));
             CanvasImageExporter exporter = new CanvasImageExporter(in, outDir, "PNG", 5)) {
            assertTrue(exporter instanceof OFDExporter);
            exporter.export(0);
            assertEquals(1, exporter.getImgFilePaths().size());
        }
    }

    /**
     * 文件名模式与序号基准：SEQUENCE 基准下 export(2, 0) 输出 0、1
     */
    @Test
    @DisplayName("文件名模式：SEQUENCE 基准按导出序号命名")
    void filenameIndexBaseSequence() throws Exception {
        Path outDir = tempDir.resolve("sequence");
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(MULTI_PAGE_OFD), outDir, "PNG", 5)) {
            exporter.setFilenameIndexBase(IndexBase.SEQUENCE);
            exporter.setFilenamePattern("img-%02d");
            exporter.export(2, 0);
            List<Path> files = exporter.getImgFilePaths();
            assertEquals(outDir.resolve("img-00.png"), files.get(0));
            assertEquals(outDir.resolve("img-01.png"), files.get(1));
            assertThrows(IllegalArgumentException.class, () -> exporter.setFilenamePattern("no-placeholder"));
        }
    }

    /**
     * 页码越界：export 跳过越界页码而不抛异常
     */
    @Test
    @DisplayName("页码越界：跳过不抛异常")
    void outOfRangePageIsSkipped() throws Exception {
        Path outDir = tempDir.resolve("skip");
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(SINGLE_PAGE_OFD), outDir, "PNG", 5)) {
            exporter.export(0, 9);
            assertEquals(1, exporter.getImgFilePaths().size());
            assertEquals(outDir.resolve("page-0.png"), exporter.getImgFilePaths().get(0));
        }
    }

    /**
     * 默认常量与旋转归一化：负角度等价于其对应的正向角度
     */
    @Test
    @DisplayName("默认参数：PPM/质量/像素上限常量与负角度归一")
    void defaultsAndRotationNormalization() throws Exception {
        assertEquals(15, CanvasImageExporter.DEFAULT_PPM);
        assertEquals(0.92f, CanvasImageExporter.DEFAULT_JPEG_QUALITY, 1e-6);
        assertEquals(64000000L, CanvasImageExporter.DEFAULT_MAX_PIXELS);
        assertEquals("page-%d", CanvasImageExporter.DEFAULT_FILENAME_PATTERN);

        Path outDir = tempDir.resolve("rotation-normalize");
        try (CanvasImageExporter exporter = new CanvasImageExporter(Paths.get(SINGLE_PAGE_OFD), outDir, "PNG", 5)) {
            exporter.setRotation(-90);
            exporter.export(0);
            assertEquals(270, exporter.getImageInfos().get(0).getRotation(), "-90 度应归一为 270 度");
            // 非法角度被忽略，保持上一次的角度
            exporter.setRotation(45);
            exporter.export(0);
            assertEquals(270, exporter.getImageInfos().get(0).getRotation());
        }
        assertSame(IndexBase.PAGE, IndexBase.valueOf("PAGE"));
        assertSame(IndexBase.SEQUENCE, IndexBase.valueOf("SEQUENCE"));
    }
}
