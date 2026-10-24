package org.ofdrw.converter.renderer;

import org.junit.jupiter.api.Test;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
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
     * 设备空间裁剪不受调用方遗留变换影响：模拟「图元绘制后遗留缩放变换 → 以设备空间形状裁剪 → 继续绘制」，
     * 断言后续图元仍被正确绘制（修复前裁剪区域被二次映射成空集，图元全部消失）。
     */
    @Test
    void deviceSpaceClipIgnoresLeftoverTransform() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        try {
            renderer.beginPage(100, 100);
            // 图元 1：正常绘制红色矩形
            renderer.save();
            renderer.setTransform(new AffineTransform());
            renderer.beginPath();
            renderer.rect(10, 10, 30, 30);
            renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.RED), 1f), FillRule.NON_ZERO);
            renderer.restore();
            // 模拟下一个图元：先 setTransform(0.03 缩放) 遗留变换，再以设备空间形状裁剪
            renderer.setTransform(AffineTransform.getScaleInstance(0.03, 0.03));
            renderer.clip(new Rectangle2D.Double(0, 0, 100, 100), FillRule.NON_ZERO);
            renderer.setTransform(new AffineTransform());
            renderer.beginPath();
            renderer.rect(50, 10, 30, 30);
            renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.BLUE), 1f), FillRule.NON_ZERO);
            renderer.endPage();

            BufferedImage image = renderer.getOutput();
            assertEquals(Color.RED.getRGB(), image.getRGB(20, 20), "裁剪前的图元应保留");
            assertEquals(Color.BLUE.getRGB(), image.getRGB(60, 20), "设备空间裁剪不应把后续图元裁掉");
        } finally {
            renderer.close();
        }
    }

    /**
     * 设备空间裁剪的求交语义：两个设备空间形状连续裁剪后，只有交集区域内的绘制生效。
     */
    @Test
    void deviceSpaceClipIntersects() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        try {
            renderer.beginPage(100, 100);
            renderer.setTransform(AffineTransform.getScaleInstance(0.05, 0.05));
            renderer.clip(new Rectangle2D.Double(0, 0, 60, 100), FillRule.NON_ZERO);
            renderer.setTransform(AffineTransform.getScaleInstance(3, 3));
            renderer.clip(new Rectangle2D.Double(40, 0, 60, 100), FillRule.NON_ZERO);
            renderer.setTransform(new AffineTransform());
            renderer.beginPath();
            renderer.rect(0, 0, 100, 100);
            renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.GREEN), 1f), FillRule.NON_ZERO);
            renderer.endPage();

            BufferedImage image = renderer.getOutput();
            assertEquals(Color.GREEN.getRGB(), image.getRGB(50, 50), "交集区域内应绘制");
            assertEquals(0, image.getRGB(20, 50) >>> 24, "交集区域左外侧不应绘制");
            assertEquals(0, image.getRGB(80, 50) >>> 24, "交集区域右外侧不应绘制");
        } finally {
            renderer.close();
        }
    }

    /**
     * 每个图元的裁剪只作用于自身：save → clip(A) → 绘制 → restore → save → clip(B) → 绘制 → restore，
     * 两个图元都必须可见。缺少成对的 save/restore 时，第二个图元会被 A∩B 求交裁掉（曾造成整页只剩少数图元）。
     */
    @Test
    void perUnitClipDoesNotLeakIntoFollowingUnits() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        try {
            renderer.beginPage(100, 100);
            // 图元 A：boundary 为左半，红色矩形
            renderer.save();
            renderer.clip(new Rectangle2D.Double(0, 0, 40, 100), FillRule.NON_ZERO);
            renderer.beginPath();
            renderer.rect(10, 10, 20, 20);
            renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.RED), 1f), FillRule.NON_ZERO);
            renderer.restore();
            // 图元 B：boundary 为右半，蓝色矩形
            renderer.save();
            renderer.clip(new Rectangle2D.Double(60, 0, 40, 100), FillRule.NON_ZERO);
            renderer.beginPath();
            renderer.rect(70, 10, 20, 20);
            renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.BLUE), 1f), FillRule.NON_ZERO);
            renderer.restore();

            BufferedImage image = renderer.getOutput();
            assertEquals(Color.RED.getRGB(), image.getRGB(20, 20), "第一个图元应可见");
            assertEquals(Color.BLUE.getRGB(), image.getRGB(80, 20), "第二个图元不应被前一个图元的裁剪裁掉");
        } finally {
            renderer.close();
        }
    }

    /**
     * 裁剪随 save/restore 恢复，且 clearClip 之后不再限制绘制。
     */
    @Test
    void clipIsRestoredBySaveAndClearedByClearClip() {
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        try {
            renderer.beginPage(100, 100);
            renderer.save();
            renderer.clip(new Rectangle2D.Double(0, 0, 50, 100), FillRule.NON_ZERO);
            renderer.beginPath();
            renderer.rect(0, 0, 100, 100);
            renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.RED), 1f), FillRule.NON_ZERO);
            renderer.restore();

            // restore 后裁剪回到无裁剪状态，整页都应可绘制
            renderer.beginPath();
            renderer.rect(60, 0, 40, 100);
            renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.BLUE), 1f), FillRule.NON_ZERO);
            // clearClip 之后即使再次裁剪也能被清除
            renderer.clip(new Rectangle2D.Double(0, 0, 10, 10), FillRule.NON_ZERO);
            renderer.clearClip();
            renderer.beginPath();
            renderer.rect(0, 60, 100, 40);
            renderer.fill(DrawStyle.defaultStyle(), new ResolvedPaint(CanvasPaint.of(Color.GREEN), 1f), FillRule.NON_ZERO);
            renderer.endPage();

            BufferedImage image = renderer.getOutput();
            assertEquals(Color.RED.getRGB(), image.getRGB(20, 20), "裁剪区域内应绘制");
            assertEquals(Color.BLUE.getRGB(), image.getRGB(80, 20), "restore 后应恢复为无裁剪");
            assertEquals(Color.GREEN.getRGB(), image.getRGB(20, 80), "clearClip 后应恢复为无裁剪");
        } finally {
            renderer.close();
        }
    }

    /**
     * clear 不污染调用方状态：绘制前的 Paint 与 Composite 在 clear 之后保持不变
     */
    @Test
    void clearRestoresPaintAndComposite() {
        BufferedImage target = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D targetGraphics = target.createGraphics();
        targetGraphics.setPaint(Color.RED);
        targetGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        Graphics2DRenderer renderer = new Graphics2DRenderer(targetGraphics);
        try {
            renderer.beginPage(20, 20);
            renderer.clear(0, 0, 10, 10);
            assertEquals(Color.RED, targetGraphics.getPaint(), "clear 之后应恢复原有 Paint");
            assertEquals(Color.RED, targetGraphics.getColor(), "clear 之后应恢复原有 Color");
            assertEquals(0.5f, ((AlphaComposite) targetGraphics.getComposite()).getAlpha(), 1e-6,
                    "clear 之后应恢复原有 Composite");
            assertEquals(0, target.getRGB(5, 5) >>> 24, "clear 区域应被擦成透明");
        } finally {
            renderer.close();
            targetGraphics.dispose();
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
