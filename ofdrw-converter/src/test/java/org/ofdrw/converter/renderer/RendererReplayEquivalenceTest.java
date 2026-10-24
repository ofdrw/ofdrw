package org.ofdrw.converter.renderer;

import org.ofdrw.converter.renderer.command.BeginPathCommand;
import org.ofdrw.converter.renderer.command.ClearClipCommand;
import org.ofdrw.converter.renderer.command.ClipCommand;
import org.ofdrw.converter.renderer.command.ClipShapeCommand;
import org.ofdrw.converter.renderer.command.ClosePathCommand;
import org.ofdrw.converter.renderer.command.CubicCurveToCommand;
import org.ofdrw.converter.renderer.command.DrawCommand;
import org.ofdrw.converter.renderer.command.DrawImageCommand;
import org.ofdrw.converter.renderer.command.FillCommand;
import org.ofdrw.converter.renderer.command.FillShapeCommand;
import org.ofdrw.converter.renderer.command.LineToCommand;
import org.ofdrw.converter.renderer.command.MoveToCommand;
import org.ofdrw.converter.renderer.command.MultiplyAlphaCommand;
import org.ofdrw.converter.renderer.command.QuadraticCurveToCommand;
import org.ofdrw.converter.renderer.command.RectCommand;
import org.ofdrw.converter.renderer.command.RestoreCommand;
import org.ofdrw.converter.renderer.command.SaveCommand;
import org.ofdrw.converter.renderer.command.SetAlphaCommand;
import org.ofdrw.converter.renderer.command.SetTransformCommand;
import org.ofdrw.converter.renderer.command.StrokeCommand;
import org.ofdrw.converter.renderer.command.StrokeShapeCommand;
import org.ofdrw.converter.renderer.command.TransformCommand;
import org.ofdrw.converter.renderer.command.TranslateCommand;
import org.junit.jupiter.api.Test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 录制-回放等价性自测：同一组绘制操作分别交给 {@link Graphics2DRenderer} 与
 * {@link RecordingRenderer} + {@link DisplayList} 回放（同样由 {@link Graphics2DRenderer} 执行），断言像素完全一致。
 */
class RendererReplayEquivalenceTest {

    /**
     * 执行既能被直接渲染、也能被录制的同一组绘制操作。
     *
     * @param renderer 目标渲染器
     * @param imageHandle 图片句柄，由调用方通过对应渲染器创建
     */
    private void drawScene(Renderer renderer, ImageHandle imageHandle) {
        renderer.beginPage(80, 60);
        renderer.save();
        renderer.setTransform(AffineTransform.getTranslateInstance(4, 3));
        renderer.transform(AffineTransform.getScaleInstance(1.5, 1.5));
        renderer.translate(2, 1);
        renderer.setAlpha(0.75f);
        renderer.multiplyAlpha(0.8f);
        renderer.beginPath();
        renderer.moveTo(0, 0);
        renderer.lineTo(20, 0);
        renderer.quadraticCurveTo(30, 10, 20, 20);
        renderer.cubicCurveTo(10, 30, 0, 20, 0, 0);
        renderer.closePath();
        DrawStyle style = new DrawStyle(2f, new float[]{4f, 2f}, 1f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 8f);
        renderer.fill(style, new ResolvedPaint(CanvasPaint.of(new Color(10, 120, 200, 255)), 0.9f), FillRule.NON_ZERO);
        renderer.stroke(style, new ResolvedPaint(CanvasPaint.of(Color.ORANGE), 1f));
        renderer.clip(FillRule.NON_ZERO);
        renderer.clearClip();
        renderer.save();
        renderer.rect(2, 2, 10, 6);
        renderer.clip(FillRule.EVEN_ODD);
        renderer.clip(new Ellipse2D.Double(0, 0, 12, 12), FillRule.NON_ZERO);
        renderer.restore();
        renderer.fill(new Ellipse2D.Double(1, 1, 14, 14), style,
                new ResolvedPaint(CanvasPaint.of(Color.MAGENTA), 0.6f), FillRule.EVEN_ODD);
        renderer.stroke(new Ellipse2D.Double(1, 1, 14, 14), style,
                new ResolvedPaint(CanvasPaint.of(Color.BLACK), 1f));
        renderer.drawImage(imageHandle, AffineTransform.getTranslateInstance(30, 10), 0.9f, true, null, BlendMode.MULTIPLY);
        renderer.clear(60, 40, 10, 10);
        renderer.restore();
        renderer.endPage();
    }

    /**
     * 构造测试图片：半透明蓝绿格子。
     *
     * @return 图片像素数据
     */
    private BufferedImage sampleImage() {
        BufferedImage image = new BufferedImage(18, 12, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 18; x++) {
                image.setRGB(x, y, ((x + y) % 3 == 0) ? new Color(0, 200, 120, 200).getRGB() : new Color(240, 60, 60, 160).getRGB());
            }
        }
        return image;
    }

    /**
     * 直接渲染与录制后回放的像素必须逐像素一致。
     */
    @Test
    void recordedDisplayListReplaysIdentically() {
        Graphics2DRenderer direct = new Graphics2DRenderer();
        direct.beginPage(80, 60);
        ImageHandle directHandle = direct.createImage(sampleImage());
        direct.beginPage(80, 60);
        drawScene(direct, directHandle);

        RecordingRenderer recorder = new RecordingRenderer();
        recorder.beginPage(80, 60);
        ImageHandle recordedHandle = recorder.createImage(sampleImage());
        recorder.beginPage(80, 60);
        drawScene(recorder, recordedHandle);

        Graphics2DRenderer replay = new Graphics2DRenderer();
        replay.beginPage(80, 60);
        renderDisplayList(replay, recorder.getDisplayList(), recorder.getImages());

        BufferedImage expected = direct.getOutput();
        BufferedImage actual = replay.getOutput();
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        int diff = 0;
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                if (expected.getRGB(x, y) != actual.getRGB(x, y)) {
                    diff++;
                }
            }
        }
        assertEquals(0, diff, "录制后回放应与直接渲染逐像素一致");
    }

    /**
     * 把命令列表回放到目标渲染器上。
     *
     * @param renderer    目标渲染器
     * @param displayList 命令列表
     * @param images      录制时注册的图片句柄，按序号索引
     */
    private static void renderDisplayList(Renderer renderer, DisplayList displayList, java.util.List<ImageHandle> images) {
        for (DrawCommand command : displayList.getCommands()) {
            if (command instanceof SaveCommand) {
                renderer.save();
            } else if (command instanceof RestoreCommand) {
                renderer.restore();
            } else if (command instanceof SetTransformCommand) {
                renderer.setTransform(((SetTransformCommand) command).getTransform());
            } else if (command instanceof TransformCommand) {
                renderer.transform(((TransformCommand) command).getTransform());
            } else if (command instanceof TranslateCommand) {
                TranslateCommand c = (TranslateCommand) command;
                renderer.translate(c.getTx(), c.getTy());
            } else if (command instanceof MultiplyAlphaCommand) {
                renderer.multiplyAlpha(((MultiplyAlphaCommand) command).getAlpha());
            } else if (command instanceof SetAlphaCommand) {
                renderer.setAlpha(((SetAlphaCommand) command).getAlpha());
            } else if (command instanceof BeginPathCommand) {
                renderer.beginPath();
            } else if (command instanceof MoveToCommand) {
                MoveToCommand c = (MoveToCommand) command;
                renderer.moveTo(c.getX(), c.getY());
            } else if (command instanceof LineToCommand) {
                LineToCommand c = (LineToCommand) command;
                renderer.lineTo(c.getX(), c.getY());
            } else if (command instanceof QuadraticCurveToCommand) {
                QuadraticCurveToCommand c = (QuadraticCurveToCommand) command;
                renderer.quadraticCurveTo(c.getCpx(), c.getCpy(), c.getX(), c.getY());
            } else if (command instanceof CubicCurveToCommand) {
                CubicCurveToCommand c = (CubicCurveToCommand) command;
                renderer.cubicCurveTo(c.getCp1x(), c.getCp1y(), c.getCp2x(), c.getCp2y(), c.getX(), c.getY());
            } else if (command instanceof RectCommand) {
                RectCommand c = (RectCommand) command;
                renderer.rect(c.getX(), c.getY(), c.getWidth(), c.getHeight());
            } else if (command instanceof ClosePathCommand) {
                renderer.closePath();
            } else if (command instanceof ClipCommand) {
                renderer.clip(((ClipCommand) command).getRule());
            } else if (command instanceof ClipShapeCommand) {
                ClipShapeCommand c = (ClipShapeCommand) command;
                renderer.clip(c.getShape(), c.getRule());
            } else if (command instanceof ClearClipCommand) {
                renderer.clearClip();
            } else if (command instanceof FillCommand) {
                FillCommand c = (FillCommand) command;
                renderer.fill(c.getStyle(), c.getPaint(), c.getRule());
            } else if (command instanceof FillShapeCommand) {
                FillShapeCommand c = (FillShapeCommand) command;
                renderer.fill(c.getShape(), c.getStyle(), c.getPaint(), c.getRule());
            } else if (command instanceof StrokeCommand) {
                StrokeCommand c = (StrokeCommand) command;
                renderer.stroke(c.getStyle(), c.getPaint());
            } else if (command instanceof StrokeShapeCommand) {
                StrokeShapeCommand c = (StrokeShapeCommand) command;
                renderer.stroke(c.getShape(), c.getStyle(), c.getPaint());
            } else if (command instanceof DrawImageCommand) {
                DrawImageCommand c = (DrawImageCommand) command;
                ImageHandle handle = images.get(c.getImage().getId());
                renderer.drawImage(handle, c.getTransform(), c.getAlpha(), c.isSmoothing(), null, c.getBlend());
            } else if (command instanceof org.ofdrw.converter.renderer.command.ClearCommand) {
                org.ofdrw.converter.renderer.command.ClearCommand c =
                        (org.ofdrw.converter.renderer.command.ClearCommand) command;
                renderer.clear(c.getX(), c.getY(), c.getWidth(), c.getHeight());
            } else {
                throw new IllegalStateException("未处理命令类型：" + command.getClass().getName());
            }
        }
    }
}
