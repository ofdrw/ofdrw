package org.ofdrw.converter.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 图片资源路径大小写不一致与底纹（Pattern）填充的回归测试。
 * <p>
 * 覆盖两类缺陷：
 * <ol>
 *     <li>压缩包内资源目录名与文档中的引用大小写不一致时，图片应仍能加载并渲染，
 *         不能整块缺失；</li>
 *     <li>底纹（Pattern）填充既不能回退为纯黑（遮挡整页），也不能退化为完全空白，
 *         应正常平铺渲染。</li>
 * </ol>
 */
class CanvasImageExporterResourceTest {

    /**
     * 资源目录实际为 DOC_0（大写），文档中引用为 Doc_0/Res/...，用于验证大小写兜底
     */
    private static final Path CASE_MISMATCH_IMAGE_OFD = Paths.get("src/test/resources/containsJPEG.ofd");

    /**
     * 水印注释外观使用 Pattern（底纹）填充的文档
     */
    private static final Path PATTERN_OFD = Paths.get("src/test/resources/pattern类型.ofd");

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadImageWhenResourceDirectoryCaseMismatch() throws IOException {
        try (OFDReader reader = new OFDReader(CASE_MISMATCH_IMAGE_OFD)) {
            final ResourceManage resMgt = reader.getResMgt();
            // 文档中引用的是 Doc_0/Res/Image_4.JPEG，压缩包内实际为 DOC_0/Res/Image_4.JPEG
            final BufferedImage image = resMgt.getImage("4");
            assertNotNull(image, "资源目录大小写不一致时图片仍应能加载");
            assertTrue(image.getWidth() > 0 && image.getHeight() > 0, "图片尺寸应有效");
        }
    }

    @Test
    void shouldRenderImagePagesWhenResourceDirectoryCaseMismatch() throws IOException {
        try (CanvasImageExporter exporter = new CanvasImageExporter(CASE_MISMATCH_IMAGE_OFD, tempDir, "PNG", 3.0)) {
            exporter.export();
            assertEquals(2, exporter.getImgFilePaths().size(), "应导出 2 页");

            for (Path img : exporter.getImgFilePaths()) {
                final BufferedImage bi = ImageIO.read(img.toFile());
                assertNotNull(bi, "导出的图片应可读取");
                final double nonWhiteRatio = nonWhiteRatio(bi);
                // 该文档每页由一张整幅 JPEG 构成，缺失时整页为纯白（比例 0）
                assertTrue(nonWhiteRatio > 0.05,
                        String.format("图片缺失时整页为纯白，实际非白像素比例 %.3f", nonWhiteRatio));
            }
        }
    }

    @Test
    void shouldTilePatternInsteadOfFallingBackToBlackOrBlank() throws IOException, GeneralConvertException {
        try (CanvasImageExporter exporter = new CanvasImageExporter(PATTERN_OFD, tempDir, "PNG", 3.0)) {
            exporter.export(0);
            final BufferedImage bi = ImageIO.read(exporter.getImgFilePaths().get(0).toFile());
            assertNotNull(bi, "导出的图片应可读取");

            final double nonWhiteRatio = nonWhiteRatio(bi);
            // 不能完全空白：底纹水印应被平铺出来
            assertTrue(nonWhiteRatio > 0.01,
                    String.format("底纹未渲染，页面接近空白，非白像素比例 %.4f", nonWhiteRatio));
            // 不能整体变黑：底纹是浅灰水印，不应出现大面积深色
            final double darkRatio = darkRatio(bi);
            assertTrue(darkRatio < 0.05,
                    String.format("底纹填充退化为整块深色，深色像素比例 %.4f", darkRatio));
        }
    }

    /**
     * 统计非白像素占比（抽样步长 1，RGB 三通道全为 255 视为白）。
     *
     * @param image 位图
     * @return 非白像素比例，取值 0..1，无量纲
     */
    private static double nonWhiteRatio(BufferedImage image) {
        long nonWhite = 0;
        long total = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                total++;
                if ((image.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    nonWhite++;
                }
            }
        }
        return total == 0 ? 0 : (double) nonWhite / total;
    }

    /**
     * 统计深色像素占比（三通道均小于 60 视为深色）。
     *
     * @param image 位图
     * @return 深色像素比例，取值 0..1，无量纲
     */
    private static double darkRatio(BufferedImage image) {
        long dark = 0;
        long total = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                total++;
                final int rgb = image.getRGB(x, y);
                if (((rgb >> 16) & 0xFF) < 60 && ((rgb >> 8) & 0xFF) < 60 && (rgb & 0xFF) < 60) {
                    dark++;
                }
            }
        }
        return total == 0 ? 0 : (double) dark / total;
    }
}
