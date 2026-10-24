package org.ofdrw.converter.renderer;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Graphics2DRenderer} 的像素级自测：矩形填充/描边、透明度、裁剪、图片绘制与页面切换。
 */
class Graphics2DRendererTest {

    /**
     * 填充矩形：内部为填充色，外部保持透明
     */
    @Test
    void fillRectPaintsExpectedPixels() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        try {
            renderer.beginPage(40, 30);
            renderer.beginPath();
            renderer.rect(10, 5, 20, 10);
            renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.RED), 1f), FillRule.NON_ZERO);
            renderer.endPage();

            BufferedImage image = renderer.getOutput();
            assertEquals(40, image.getWidth());
            assertEquals(30, image.getHeight());
            assertEquals(Color.RED.getRGB(), image.getRGB(15, 10));
            assertEquals(Color.RED.getRGB(), image.getRGB(29, 14));
            assertEquals(0, image.getRGB(5, 5) >>> 24, "矩形外应保持透明");
            assertEquals(0, image.getRGB(35, 25) >>> 24, "矩形外应保持透明");
        } finally {
            renderer.close();
        }
    }

    /**
     * 描边矩形：边框像素着色，内部保持透明
     */
    @Test
    void strokeRectPaintsBorderOnly() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        try {
            renderer.beginPage(40, 30);
            renderer.beginPath();
            renderer.rect(10, 5, 20, 10);
            renderer.closePath();
            renderer.stroke(new DrawStyle(2f, null, 0f, java.awt.BasicStroke.CAP_BUTT,
                    java.awt.BasicStroke.JOIN_MITER, 10f), new ResolvedPaint(CanvasPaint.of(Color.BLUE), 1f));
            renderer.endPage();

            BufferedImage image = renderer.getOutput();
            assertEquals(Color.BLUE.getRGB(), image.getRGB(20, 5));
            assertEquals(0, image.getRGB(20, 10) >>> 24, "描边矩形内部不应被填充");
        } finally {
            renderer.close();
        }
    }

    /**
     * 透明度与裁剪：半透明填充、裁剪区域内生效、区域外不绘制、restore 后裁剪恢复
     */
    @Test
    void multiplyAlphaAndClipAreApplied() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        try {
            renderer.beginPage(40, 30);
            renderer.save();
            renderer.beginPath();
            renderer.rect(0, 0, 20, 30);
            renderer.clip(FillRule.NON_ZERO);
            renderer.multiplyAlpha(0.5f);
            renderer.beginPath();
            renderer.rect(0, 0, 40, 30);
            renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.BLACK), 1f), FillRule.NON_ZERO);
            renderer.restore();
            renderer.endPage();

            BufferedImage image = renderer.getOutput();
            int inside = image.getRGB(5, 5);
            assertEquals(0, (inside >> 16) & 0xFF, "黑色填充的红色分量应为 0");
            int alphaInside = (inside >>> 24) & 0xFF;
            assertTrue(alphaInside > 100 && alphaInside < 160, "裁剪区内应为半透明，实际 alpha=" + alphaInside);
            assertEquals(0, image.getRGB(30, 5) >>> 24, "裁剪区外不应绘制");
        } finally {
            renderer.close();
        }
    }

    /**
     * 图片绘制：按变换缩放绘制，未覆盖区域保持透明
     */
    @Test
    void drawImagePaintsScaledImage() {
        BufferedImage source = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                source.setRGB(x, y, Color.GREEN.getRGB());
            }
        }
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        try {
            renderer.beginPage(60, 40);
            ImageHandle handle = renderer.createImage(source);
            assertEquals(10, handle.getWidth());
            renderer.drawImage(handle, AffineTransform.getScaleInstance(2.0, 2.0), 1f, false, null, BlendMode.NORMAL);
            renderer.endPage();

            BufferedImage image = renderer.getOutput();
            assertEquals(Color.GREEN.getRGB(), image.getRGB(5, 5));
            assertEquals(Color.GREEN.getRGB(), image.getRGB(19, 19));
            assertEquals(0, image.getRGB(25, 25) >>> 24, "图片缩放后 20x20 以外应保持透明");
        } finally {
            renderer.close();
        }
    }

    /**
     * 清空与页面切换：clear 擦成透明，beginPage 重置为透明新画布
     */
    @Test
    void clearAndPageSwitchResetCanvas() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        try {
            renderer.beginPage(20, 20);
            renderer.beginPath();
            renderer.rect(0, 0, 20, 20);
            renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.RED), 1f), FillRule.NON_ZERO);
            renderer.endPage();
            BufferedImage first = renderer.getOutput();
            assertEquals(Color.RED.getRGB(), first.getRGB(10, 10));

            renderer.beginPage(20, 20);
            renderer.beginPath();
            renderer.rect(0, 0, 20, 20);
            renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.RED), 1f), FillRule.NON_ZERO);
            renderer.clear(0, 0, 20, 20);
            renderer.endPage();
            BufferedImage second = renderer.getOutput();
            assertNotEquals(first, second, "beginPage 应创建新的输出位图");
            assertEquals(0, second.getRGB(10, 10) >>> 24, "clear 应把区域擦成透明");
            assertFalse(renderer.getWidth() <= 0);
        } finally {
            renderer.close();
        }
    }
}
