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

    /**
     * 印章场景：图片带透明边缘时，透明像素参与正片叠底不得变黑。
     * <p>
     * 复现缺陷：以空白位图（目标色为透明黑）作为混合目标时，透明源像素会与黑色相乘，
     * 导致印章透明区域变成不透明黑块。此处断言透明区域保持白色底、边缘半透明像素只按 alpha 淡入。
     */
    @Test
    void multiplyKeepsTransparentPixelsOfStamp() {
        BufferedImage stamp = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = stamp.createGraphics();
        // 中心 4x4 为不透明红色印章线条，其余为完全透明
        g.setColor(Color.RED);
        g.fillRect(3, 3, 4, 4);
        g.dispose();

        Graphics2DRenderer renderer = new Graphics2DRenderer();
        renderer.beginPage(20, 20);
        renderer.beginPath();
        renderer.rect(0, 0, 20, 20);
        renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.WHITE), 1f), FillRule.NON_ZERO);
        renderer.drawImage(renderer.createImage(stamp), AffineTransform.getTranslateInstance(5, 5), 1f, false, null,
                BlendMode.MULTIPLY);
        renderer.endPage();

        BufferedImage output = renderer.getOutput();
        // 印章透明区域：应保持白色底，不能变成黑色
        assertEquals(Color.WHITE.getRGB(), output.getRGB(6, 6), "印章透明区域应保持目标色，不能变黑");
        assertEquals(Color.WHITE.getRGB(), output.getRGB(5, 5), "印章左上角透明像素应保持目标色");
        // 不透明红色区域：白底 255 × 红(255,0,0) = (255,0,0)
        assertEquals(new Color(255, 0, 0).getRGB(), output.getRGB(8, 8), "不透明红色区域应与白色底相乘后仍为红色");
        renderer.close();
        stamp.flush();
    }

    /**
     * 调用方像素画布：混合必须以画布已有内容为目标色。
     * <p>
     * 用封装了一层“像素中间层”的 Graphics2D 模拟真实画布（绘制结果可回读为像素），
     * 直接走 {@code new Graphics2DRenderer(graphics, raster)} 的像素分支。
     */
    @Test
    void multiplyOnCallerPixelCanvasUsesExistingPixelsAsDestination() {
        BufferedImage canvas = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D raw = canvas.createGraphics();
        raw.setColor(new Color(0, 255, 255));
        raw.fillRect(0, 0, 20, 20);
        raw.dispose();

        BufferedImage stamp = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = stamp.createGraphics();
        sg.setColor(Color.RED);
        sg.fillRect(3, 3, 4, 4);
        sg.dispose();

        Graphics2D graphics = canvas.createGraphics();
        Graphics2DRenderer renderer = new Graphics2DRenderer(graphics, canvas.getRaster());
        renderer.beginPage(20, 20);
        renderer.drawImage(renderer.createImage(stamp), AffineTransform.getTranslateInstance(5, 5), 1f, false, null,
                BlendMode.MULTIPLY);
        renderer.endPage();
        graphics.dispose();

        // 透明区域保持青色（不是黑色）
        assertEquals(new Color(0, 255, 255).getRGB(), canvas.getRGB(6, 6), "透明像素应保持画布原有颜色");
        assertEquals(new Color(0, 255, 255).getRGB(), canvas.getRGB(19, 19), "图片区域之外不受影响");
        // 青(0,255,255) × 红(255,0,0) = (0,0,0)；若未混合会保留红色
        assertEquals(0xFF000000, canvas.getRGB(8, 8), "不透明区域应执行正片叠底（红与青相乘为黑）");
        canvas.flush();
        stamp.flush();
    }
}
