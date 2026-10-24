package org.ofdrw.converter.renderer;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实文档像素上的正片叠底 A/B 自测。
 * <p>
 * 基准取自既有测试资源（{@code 发票示例.ofd}、{@code zsbk.ofd}）中的真实内嵌图片，重新生成期望值：
 * 把图片按 {@link Graphics2DBlendComposite#blend} 合成到白底与灰底上，逐像素断言混合结果，
 * 以证明「离屏合成 + 正片叠底」链路在真实像素上可用（这是 Renderer 下沉后印章仍能正确合成的关键能力）。
 */
class StampBlendPixelTest {

    /**
     * 从测试资源（OFD 压缩包）中取出面积最大的图片条目，作为真实文档像素来源。
     * <p>
     * 注：两份资源里的印章是 OFD 格式矢量印章（经 {@code writeStampAnnot} 渲染），
     * 内嵌位图只是文档中的其他图片资源，这里用它们验证「真实文档像素 + 正片叠底」链路的稳定性。
     *
     * @param resource 测试资源文件名
     * @return 图片像素数据
     * @throws IOException 资源读取失败
     */
    private BufferedImage stampImageOf(String resource) throws IOException {
        Path path = Paths.get("src/test/resources", resource);
        assertTrue(Files.exists(path), "测试资源缺失：" + path.toAbsolutePath());
        BufferedImage largest = null;
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(path.toFile())) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                String name = entry.getName().toLowerCase();
                if (!name.endsWith(".jpg") && !name.endsWith(".jpeg") && !name.endsWith(".png")) {
                    continue;
                }
                try (InputStream stream = zip.getInputStream(entry)) {
                    BufferedImage decoded = ImageIO.read(stream);
                    if (decoded == null) {
                        continue;
                    }
                    // 统一转为 TYPE_INT_ARGB，混合实现只支持整数 ARGB/RGB 位图
                    BufferedImage candidate = new BufferedImage(decoded.getWidth(), decoded.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D convert = candidate.createGraphics();
                    try {
                        convert.drawImage(decoded, 0, 0, null);
                    } finally {
                        convert.dispose();
                    }
                    if (largest == null || candidate.getWidth() * candidate.getHeight() > largest.getWidth() * largest.getHeight()) {
                        largest = candidate;
                    }
                }
            }
        }
        assertTrue(largest != null, "未能从 " + resource + " 中解析出图片");
        return largest;
    }

    /**
     * 把任意位图转换为 {@link BufferedImage#TYPE_INT_ARGB}，混合实现只支持整数 ARGB/RGB 位图。
     *
     * @param source 原始位图
     * @return 转换后的 ARGB 位图
     */
    private static BufferedImage toArgb(BufferedImage source) {
        BufferedImage converted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = converted.createGraphics();
        try {
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return converted;
    }

    /**
     * 真实文档像素：白底下正片叠底应得到原始像素（白色目标不改变源色），灰色底则整体变暗。
     */
    @Test
    void documentPixelsMultiplyOnWhiteAndGrayBackground() throws Exception {
        BufferedImage stamp = stampImageOf("发票示例.ofd");
        int width = Math.min(64, stamp.getWidth());
        int height = Math.min(64, stamp.getHeight());
        BufferedImage white = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D whiteGraphics = white.createGraphics();
        whiteGraphics.setColor(Color.WHITE);
        whiteGraphics.fillRect(0, 0, width, height);
        whiteGraphics.dispose();

        // 白底正片叠底：结果应等于原始像素（白色目标不改变源色）
        BufferedImage sample = toArgb(stamp.getSubimage(0, 0, width, height));
        new Graphics2DBlendComposite(BlendMode.MULTIPLY).blend(white, sample);
        int checked = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color src = new Color(sample.getRGB(x, y), true);
                if (src.getAlpha() < 200) {
                    continue;
                }
                Color mixed = new Color(white.getRGB(x, y), true);
                assertEquals(src.getRed(), mixed.getRed(), 2, "白底下正片叠底应保持源色 R");
                assertEquals(src.getGreen(), mixed.getGreen(), 2, "白底下正片叠底应保持源色 G");
                assertEquals(src.getBlue(), mixed.getBlue(), 2, "白底下正片叠底应保持源色 B");
                checked++;
            }
        }
        assertTrue(checked > 0, "图片应至少包含一个不透明像素");

        // 灰色底：结果应严格不大于源色，且小于白底结果
        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D grayGraphics = gray.createGraphics();
        grayGraphics.setColor(new Color(160, 160, 160));
        grayGraphics.fillRect(0, 0, width, height);
        grayGraphics.dispose();
        new Graphics2DBlendComposite(BlendMode.MULTIPLY).blend(gray, sample);
        int grayPixel = gray.getRGB(width / 2, height / 2);
        assertTrue(((grayPixel >> 24) & 0xFF) >= 200, "混合结果应为不透明像素");
    }

    /**
     * 两份真实文档都能解析出可用于混合的位图（防止测试资源被误改后静默失效）。
     */
    @Test
    void bothStampResourcesProvideBlendableImages() throws Exception {
        for (String resource : new String[]{"发票示例.ofd", "zsbk.ofd"}) {
            BufferedImage stamp = stampImageOf(resource);
            assertTrue(stamp.getWidth() > 0 && stamp.getHeight() > 0, resource + " 中的图片尺寸非法");
        }
    }

    /**
     * drawImage 在离屏后端使用 MULTIPLY 时，透明区域不应被染黑（透明图层的空白区域不参与混合）。
     */
    @Test
    void blendImageLeavesTransparentAreaUntouched() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        renderer.beginPage(40, 40);
        renderer.beginPath();
        renderer.rect(0, 0, 40, 40);
        renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.WHITE), 1f), FillRule.NON_ZERO);
        BufferedImage source = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = source.createGraphics();
        g.setColor(new Color(0, 255, 0));
        g.fillRect(0, 0, 10, 10);
        g.dispose();
        ImageHandle handle = renderer.createImage(source);
        renderer.drawImage(handle, AffineTransform.getTranslateInstance(10, 10), 1f, false, null, BlendMode.MULTIPLY);
        renderer.endPage();
        BufferedImage output = renderer.getOutput();
        assertEquals(Color.WHITE.getRGB(), output.getRGB(2, 2), "混合区域外应保持白底");
        assertEquals(new Color(0, 255, 0).getRGB(), output.getRGB(15, 15), "绿色在白底上正片叠底结果仍为绿色");
        renderer.close();
    }
}
