package org.ofdrw.converter.pdf.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.ClipCommand;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.FillRule;
import org.ofdrw.converter.pdf.model.MoveTo;
import org.ofdrw.converter.pdf.model.PathCommand;
import org.ofdrw.converter.pdf.model.PathSegment;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.PdfPageContent;
import org.ofdrw.converter.pdf.model.RestoreState;
import org.ofdrw.converter.pdf.model.SaveState;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.pageDescription.clips.Area;
import org.ofdrw.core.pageDescription.clips.CT_Clip;
import org.ofdrw.core.pageDescription.clips.Clips;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.text.CT_Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 裁剪应用器测试
 * <p>
 * 锁定 OFD 裁剪语义（设计第 2 节表格、8.4 节、表 33）：
 * 多 Clip 取交集、同一 Clip 多 Area 取并集、Area 可为 Path 或 Text，
 * 裁剪内容被 {@code q ... Q} 隔离且成对。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class ClipApplierTest {

    /**
     * 断言容限，单位 pt
     */
    private static final double EPS = 1e-6;

    /**
     * 创建页面高 100mm 的渲染上下文
     *
     * @return 渲染上下文
     */
    private static RenderContext context() {
        PdfPageContent content = new PdfPageContent(CoordSystem.mmToPt(100), CoordSystem.mmToPt(100));
        return RenderContexts.create(100, 100, content, null);
    }

    /**
     * 创建带外接矩形的裁剪路径
     *
     * @param x 外接矩形左上角横坐标，单位 mm
     * @return 裁剪路径
     */
    private static CT_Path clipPath(double x) {
        CT_Path path = new CT_Path();
        path.setBoundary(x, 0, 10, 10);
        path.setAbbreviatedData(new AbbreviatedData().S(0, 0).L(10, 0).L(10, 10).C());
        return path;
    }

    /**
     * 把路径包装为 Area
     *
     * @param path 裁剪路径
     * @return 区域
     */
    private static Area area(org.ofdrw.core.pageDescription.clips.ClipAble path) {
        Area area = new Area();
        area.setClipObj(path);
        return area;
    }

    @Test
    @DisplayName("无裁剪：apply 返回空列表，render 原样返回内容")
    void noClip() {
        ClipApplier applier = new ClipApplier();
        assertTrue(applier.apply(null, context()).isEmpty());
        assertTrue(applier.apply("not-clips", context()).isEmpty());
        assertTrue(applier.apply(new Clips(), context()).isEmpty());
        List<ContentCommand> content = Collections.singletonList(SaveState.INSTANCE);
        assertEquals(content, applier.render(new Clips(), context(), content));
    }

    @Test
    @DisplayName("多个 Clip 生成多条裁剪指令（交集）")
    void multipleClips() {
        Clips clips = new Clips(new CT_Clip().addArea(area(clipPath(0))));
        clips.addClip(new CT_Clip().addArea(area(clipPath(5))));
        List<ClipCommand> commands = new ClipApplier().apply(clips, context());
        assertEquals(2, commands.size());
        for (ClipCommand command : commands) {
            assertTrue(command.isUnionArea());
            assertEquals(FillRule.EVEN_ODD, command.getFillRule());
        }
    }

    @Test
    @DisplayName("同一 Clip 内多个 Area 合并为一条并集指令，坐标按外接矩形偏移")
    void unionAreas() {
        CT_Clip clip = new CT_Clip().addArea(area(clipPath(0))).addArea(area(clipPath(50)));
        List<ClipCommand> commands = new ClipApplier().apply(new Clips(clip), context());
        assertEquals(1, commands.size());
        List<PathSegment> segments = commands.get(0).getSegments();
        // 每个 Area 为 M + 2L + C
        assertEquals(8, segments.size());
        MoveTo first = (MoveTo) segments.get(0);
        MoveTo second = (MoveTo) segments.get(4);
        assertEquals(CoordSystem.mmToPt(0), first.getX(), EPS);
        assertEquals(CoordSystem.mmToPt(50), second.getX(), EPS);
    }

    @Test
    @DisplayName("Area 为 Text：按文字外接矩形近似，仍输出有效裁剪")
    void textArea() {
        CT_Text text = new CT_Text();
        text.setBoundary(10, 10, 40, 20);
        text.setSize(5.0);
        TextCode code = new TextCode();
        code.setContent("裁剪文字");
        code.setX(12.0);
        code.setY(20.0);
        // 文字区域按外接矩形 + 文字单元估算近似，无需填充 TextCode 即可验证矩形近似
        text.getTextCodes();
        List<ClipCommand> commands = new ClipApplier().apply(new Clips(new CT_Clip().addArea(area(text))), context());
        assertEquals(1, commands.size());
        List<PathSegment> segments = commands.get(0).getSegments();
        // 矩形路径：M + 3L + C
        assertEquals(5, segments.size());
        MoveTo move = (MoveTo) segments.get(0);
        assertTrue(move.getX() >= CoordSystem.mmToPt(10) - EPS);
        assertTrue(move.getY() >= CoordSystem.yToPt(30, 100) - EPS);
    }

    @Test
    @DisplayName("裁剪内容被 q/Q 隔离，指令数量与顺序固定")
    void renderWrapsContent() {
        Clips clips = new Clips(new CT_Clip().addArea(area(clipPath(0))));
        List<PathSegment> segments = new ArrayList<>();
        segments.add(new MoveTo(0, 0));
        segments.add(new org.ofdrw.converter.pdf.model.LineTo(10, 10));
        segments.add(org.ofdrw.converter.pdf.model.ClosePath.INSTANCE);
        List<ContentCommand> content = Arrays.asList(
                new PathCommand(segments, true, false, FillRule.NON_ZERO, PdfColor.BLACK, null));
        List<ContentCommand> commands = new ClipApplier().render(clips, context(), content);
        assertEquals(4, commands.size());
        assertEquals(SaveState.INSTANCE, commands.get(0));
        assertTrue(commands.get(1) instanceof ClipCommand);
        assertTrue(commands.get(2) instanceof PathCommand);
        assertEquals(RestoreState.INSTANCE, commands.get(3));
    }

    @Test
    @DisplayName("裁剪区域随线宽外扩（外接矩形放大半个线宽）")
    void strokeExpanded() {
        CT_Path path = clipPath(0);
        path.setLineWidth(2.0);
        ClipCommand command = new ClipApplier().apply(
                new Clips(new CT_Clip().addArea(area(path))), context()).get(0);
        // 外扩后为矩形路径：M + 3L + C
        assertEquals(5, command.getSegments().size());
        MoveTo move = (MoveTo) command.getSegments().get(0);
        double margin = CoordSystem.mmToPt(2.0) * ClipApplier.STROKE_CLIP_MARGIN;
        assertEquals(-margin, move.getX(), 1e-3);
        // 右下角 = 外接矩形右下角 + 半个线宽
        org.ofdrw.converter.pdf.model.LineTo right = (org.ofdrw.converter.pdf.model.LineTo) command.getSegments().get(1);
        assertEquals(CoordSystem.mmToPt(10) + margin, right.getX(), 1e-3);
    }

    @Test
    @DisplayName("不支持的裁剪对象被跳过，不抛异常")
    void unsupportedObject() {
        Area area = new Area();
        CT_Clip clip = new CT_Clip().addArea(area);
        List<ClipCommand> commands = new ClipApplier().apply(new Clips(clip), context());
        assertTrue(commands.isEmpty());
    }
}
