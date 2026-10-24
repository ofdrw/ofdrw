package org.ofdrw.converter.renderer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 混合模式后端判定测试。
 * <p>
 * 区分两种绘制目标：
 * <ul>
 *     <li>离屏像素后端：渲染器自建位图，或调用方提供像素画布的 {@link Graphics2D}（{@code offscreenBackend = true}），
 *     正片叠底等混合模式必须真正生效；</li>
 *     <li>外部 Graphics2D 后端（SVG/PDF 等）：不支持自定义混合，退化为普通覆盖且不抛异常。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2026-10-25
 */
class BlendBackendTest {

    @Test
    @DisplayName("离屏后端自建位图：正片叠底生效（红 × 青 = 黑）")
    void multiplyWorksOnOffscreenRenderer() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        renderer.beginPage(40, 40);
        // 底色为青色（0,255,255），叠加纯红（255,0,0）；正片叠底结果为（0,0,0）
        fillRect(renderer, new Color(0, 255, 255), 0, 0);
        ImageHandle red = renderer.createImage(solidImage(10, 10, Color.RED));
        renderer.drawImage(red, AffineTransform.getTranslateInstance(5, 5), 1f, true, null, BlendMode.MULTIPLY);
        renderer.endPage();
        BufferedImage out = renderer.getOutput();
        assertNotNull(out);
        assertEquals(new Color(0, 0, 0), new Color(out.getRGB(10, 10)), "正片叠底应得到黑色");
        assertEquals(new Color(0, 255, 255), new Color(out.getRGB(30, 30)), "图片区域之外保持底色");
    }

    @Test
    @DisplayName("调用方像素画布 + offscreenBackend=true：正片叠底生效")
    void multiplyWorksOnCallerOwnedPixelBackend() {
        BufferedImage canvas = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        Graphics2DRenderer renderer = new Graphics2DRenderer(graphics, canvas.getRaster());
        renderer.beginPage(40, 40);
        fillRect(renderer, new Color(0, 255, 255), 0, 0);
        renderer.drawImage(renderer.createImage(solidImage(10, 10, Color.RED)),
                AffineTransform.getTranslateInstance(5, 5), 1f, true, null, BlendMode.MULTIPLY);
        renderer.endPage();
        graphics.dispose();
        assertEquals(new Color(0, 0, 0), new Color(canvas.getRGB(10, 10)), "像素画布上正片叠底应得到黑色");
        assertEquals(new Color(0, 255, 255), new Color(canvas.getRGB(38, 38)), "图片区域之外保持底色");
        assertNull(renderer.getOutput(), "复用调用方上下文时渲染器不持有输出位图");
    }

    @Test
    @DisplayName("外部 Graphics2D 后端：混合退化为普通覆盖且不抛异常")
    void externalBackendFallsBackToNormalBlend() {
        BufferedImage canvas = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        Graphics2DRenderer renderer = new Graphics2DRenderer(graphics);
        renderer.beginPage(40, 40);
        fillRect(renderer, new Color(0, 255, 255), 0, 0);
        renderer.drawImage(renderer.createImage(solidImage(10, 10, Color.RED)),
                AffineTransform.getTranslateInstance(5, 5), 1f, true, null, BlendMode.MULTIPLY);
        renderer.endPage();
        graphics.dispose();
        assertEquals(new Color(255, 0, 0), new Color(canvas.getRGB(10, 10)),
                "外部后端应退化为普通覆盖（红色直接覆盖青色）");
    }

    /**
     * 用纯色填充整张画布。
     *
     * @param renderer 渲染器
     * @param color    颜色
     * @param x        左上角 X 坐标，单位：像素
     * @param y        左上角 Y 坐标，单位：像素
     */
    private void fillRect(Graphics2DRenderer renderer, Color color, double x, double y) {
        renderer.setTransform(new AffineTransform());
        renderer.beginPath();
        renderer.rect(x, y, 40, 40);
        renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(color), 1f), FillRule.NON_ZERO);
    }

    /**
     * 构造纯色图片。
     *
     * @param width  宽度，单位：像素
     * @param height 高度，单位：像素
     * @param color  颜色
     * @return 图片
     */
    private BufferedImage solidImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }
}
