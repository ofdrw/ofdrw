package org.ofdrw.converter.renderer;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link Graphics2DBlendComposite} 与 {@link Graphics2DRenderer#drawImage} 混合模式的像素自测。
 * <p>
 * 期望值按 PDF 标准混合公式手工计算：
 * 正片叠底 {@code result = src * dst / 255}，滤色 {@code result = 255 - (255-src)*(255-dst)/255}。
 */
class Graphics2DBlendCompositeTest {

    /**
     * 构造底色为白色的目标页，并在指定矩形内填充底色。
     *
     * @param background 底色
     * @param width      宽度，单位：像素
     * @param height     高度，单位：像素
     * @return 目标页位图
     */
    private BufferedImage canvasWithBackground(Color background, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(background);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    /**
     * 构造纯色不透明图片。
     *
     * @param color 颜色
     * @param width 宽度，单位：像素
     * @param height 高度，单位：像素
     * @return 图片像素数据
     */
    private BufferedImage solid(Color color, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    /**
     * 正片叠底：白色底 × 半灰图片 = 半灰（128 * 255 / 255 = 128）
     */
    @Test
    void multiplyBlendsSourceWithDestination() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        BufferedImage background = canvasWithBackground(Color.WHITE, 20, 20);
        // 先把白底铺进离屏画布
        renderer.beginPage(20, 20);
        renderer.beginPath();
        renderer.rect(0, 0, 20, 20);
        renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.WHITE), 1f), FillRule.NON_ZERO);
        ImageHandle handle = renderer.createImage(solid(new Color(128, 128, 128), 10, 10));
        renderer.drawImage(handle, AffineTransform.getTranslateInstance(5, 5), 1f, false, null, BlendMode.MULTIPLY);
        renderer.endPage();

        BufferedImage output = renderer.getOutput();
        Color blended = new Color(output.getRGB(9, 9));
        assertEquals(128, blended.getRed(), "正片叠底：白底(255) × 源(128) 应得到 128");
        assertEquals(128, blended.getGreen());
        assertEquals(128, blended.getBlue());
        assertEquals(Color.WHITE.getRGB(), output.getRGB(1, 1), "混合区域外应保持白底");
        renderer.close();
        background.flush();
    }

    /**
     * 滤色：黑色底 + 源 128 = 128（255 - (255-128)*(255-0)/255 = 127.5，整数运算取 128）
     */
    @Test
    void screenBlendLightensDarkDestination() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        renderer.beginPage(20, 20);
        renderer.beginPath();
        renderer.rect(0, 0, 20, 20);
        renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.BLACK), 1f), FillRule.NON_ZERO);
        ImageHandle handle = renderer.createImage(solid(new Color(128, 128, 128), 10, 10));
        renderer.drawImage(handle, AffineTransform.getTranslateInstance(5, 5), 1f, false, null, BlendMode.SCREEN);
        renderer.endPage();

        Color blended = new Color(renderer.getOutput().getRGB(9, 9));
        assertEquals(128, blended.getRed(), "滤色：黑底(0) 与源(128) 应得到 255 - (255-128) = 128");
        assertEquals(0, new Color(renderer.getOutput().getRGB(1, 1)).getRed(), "混合区域外应保持黑底");
        renderer.close();
    }

    /**
     * 普通模式（NORMAL）不受混合逻辑影响，直接覆盖
     */
    @Test
    void normalModeOverwritesDestination() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        renderer.beginPage(20, 20);
        renderer.beginPath();
        renderer.rect(0, 0, 20, 20);
        renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.BLACK), 1f), FillRule.NON_ZERO);
        ImageHandle handle = renderer.createImage(solid(new Color(128, 128, 128), 10, 10));
        renderer.drawImage(handle, AffineTransform.getTranslateInstance(5, 5), 1f, false, null, BlendMode.NORMAL);
        renderer.endPage();

        assertEquals(new Color(128, 128, 128).getRGB(), renderer.getOutput().getRGB(9, 9));
        renderer.close();
    }
}
