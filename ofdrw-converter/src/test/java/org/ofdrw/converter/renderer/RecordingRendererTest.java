package org.ofdrw.converter.renderer;

import org.junit.jupiter.api.Test;
import org.ofdrw.converter.renderer.command.BeginPathCommand;
import org.ofdrw.converter.renderer.command.ClipCommand;
import org.ofdrw.converter.renderer.command.ClosePathCommand;
import org.ofdrw.converter.renderer.command.DrawCommand;
import org.ofdrw.converter.renderer.command.DrawImageCommand;
import org.ofdrw.converter.renderer.command.FillCommand;
import org.ofdrw.converter.renderer.command.LineToCommand;
import org.ofdrw.converter.renderer.command.MoveToCommand;
import org.ofdrw.converter.renderer.command.MultiplyAlphaCommand;
import org.ofdrw.converter.renderer.command.RestoreCommand;
import org.ofdrw.converter.renderer.command.SaveCommand;
import org.ofdrw.converter.renderer.command.SetTransformCommand;
import org.ofdrw.converter.renderer.command.StrokeCommand;
import org.ofdrw.converter.renderer.command.TransformCommand;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RecordingRenderer} 与 {@link DisplayList} 的自测：命令条数、顺序、参数与包围盒。
 */
class RecordingRendererTest {

    /**
     * 命令顺序与条数：按录制顺序逐条断言类型与关键参数
     */
    @Test
    void recordsCommandsInOrder() {
        RecordingRenderer renderer = new RecordingRenderer();
        renderer.beginPage(100, 80);
        renderer.save();
        renderer.setTransform(new AffineTransform());
        renderer.transform(AffineTransform.getScaleInstance(2, 2));
        renderer.multiplyAlpha(0.5f);
        renderer.beginPath();
        renderer.moveTo(1, 2);
        renderer.lineTo(3, 4);
        renderer.closePath();
        BufferedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        ImageHandle handle = renderer.createImage(source);
        renderer.drawImage(handle, AffineTransform.getTranslateInstance(5, 6), 1f, true, null, BlendMode.MULTIPLY);
        renderer.restore();
        renderer.endPage();

        List<String> actual = new ArrayList<>();
        for (DrawCommand command : renderer.getDisplayList().getCommands()) {
            actual.add(command.getClass().getSimpleName());
        }
        List<String> expected = Arrays.asList(
                "SaveCommand", "SetTransformCommand", "TransformCommand", "MultiplyAlphaCommand",
                "BeginPathCommand", "MoveToCommand", "LineToCommand", "ClosePathCommand",
                "DrawImageCommand", "RestoreCommand");
        assertEquals(expected, actual);
        assertEquals(10, renderer.getDisplayList().size());

        List<DrawCommand> commands = renderer.getDisplayList().getCommands();
        assertTrue(commands.get(0) instanceof SaveCommand);
        assertTrue(commands.get(1) instanceof SetTransformCommand);
        assertTrue(commands.get(2) instanceof TransformCommand);
        assertTrue(commands.get(3) instanceof MultiplyAlphaCommand);
        assertTrue(commands.get(4) instanceof BeginPathCommand);
        assertTrue(commands.get(5) instanceof MoveToCommand);
        assertTrue(commands.get(6) instanceof LineToCommand);
        assertTrue(commands.get(7) instanceof ClosePathCommand);
        assertTrue(commands.get(8) instanceof DrawImageCommand);
        assertTrue(commands.get(9) instanceof RestoreCommand);

        MoveToCommand moveTo = (MoveToCommand) commands.get(5);
        assertEquals(1.0, moveTo.getX());
        assertEquals(2.0, moveTo.getY());
        LineToCommand lineTo = (LineToCommand) commands.get(6);
        assertEquals(3.0, lineTo.getX());
        assertEquals(4.0, lineTo.getY());
        MultiplyAlphaCommand alpha = (MultiplyAlphaCommand) commands.get(3);
        assertEquals(0.5f, alpha.getAlpha());
        DrawImageCommand drawImage = (DrawImageCommand) commands.get(8);
        assertEquals(BlendMode.MULTIPLY, drawImage.getBlend());
        assertTrue(drawImage.isSmoothing());
        assertEquals(handle.getId(), drawImage.getImage().getId());
        assertEquals(1, renderer.getImages().size());
        assertEquals(100, renderer.getWidth());
        assertEquals(80, renderer.getHeight());
    }

    /**
     * 填充/描边/裁剪命令与包围盒累积
     */
    @Test
    void recordsFillStrokeClipAndBounds() {
        RecordingRenderer renderer = new RecordingRenderer();
        renderer.beginPage(50, 50);
        renderer.beginPath();
        renderer.rect(10, 20, 15, 5);
        DrawStyle style = DrawStyle.defaultStyle().withLineWidth(3f);
        renderer.clip(FillRule.EVEN_ODD);
        renderer.fill(style, new ResolvedPaint(CanvasPaint.of(Color.RED), 0.5f), FillRule.EVEN_ODD);
        renderer.stroke(style, new ResolvedPaint(CanvasPaint.of(Color.BLUE), 1f));
        renderer.endPage();

        List<DrawCommand> commands = renderer.getDisplayList().getCommands();
        assertEquals(5, commands.size());
        assertTrue(commands.get(1) instanceof org.ofdrw.converter.renderer.command.RectCommand);
        assertTrue(commands.get(2) instanceof ClipCommand);
        assertTrue(commands.get(3) instanceof FillCommand);
        assertTrue(commands.get(4) instanceof StrokeCommand);

        FillCommand fill = (FillCommand) commands.get(3);
        assertEquals(FillRule.EVEN_ODD, fill.getRule());
        assertEquals(0.5f, fill.getPaint().getOpacity());
        assertEquals(3f, fill.getStyle().getLineWidth());
        StrokeCommand stroke = (StrokeCommand) commands.get(4);
        assertEquals(Color.BLUE, stroke.getPaint().getPaint().getColor());

        double[] bounds = renderer.getDisplayList().getBounds();
        assertEquals(10.0, bounds[0], 1e-9);
        assertEquals(20.0, bounds[1], 1e-9);
        assertEquals(25.0, bounds[2], 1e-9);
        assertEquals(25.0, bounds[3], 1e-9);

        assertEquals(1, renderer.getPages().size(), "endPage 后页面列表应包含该页");
        assertTrue(renderer.getPages().get(0).size() >= 5);
    }
}
