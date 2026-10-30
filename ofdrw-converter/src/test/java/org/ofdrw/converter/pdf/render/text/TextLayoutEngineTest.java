package org.ofdrw.converter.pdf.render.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文字布局计算测试（表 46 定位、表 47 方向、表 48 CGTransform）
 * <p>
 * 锁定设计文档 3.6 节与第 2 章表格中明确指出的缺陷：DeltaX/DeltaY 双队列、末位尾部增量、
 * 不叠加字宽造成的双倍位移、多对一映射跳过与补足。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class TextLayoutEngineTest {

    private final TextLayoutEngine engine = new TextLayoutEngine();

    /**
     * 构造三字符横向文字
     *
     * @param deltaX X 方向偏移队列
     * @param deltaY Y 方向偏移队列
     * @return 文字图元
     */
    private static OfdText horizontal(DeltaQueue deltaX, DeltaQueue deltaY) {
        // 表 46：TextCode 的 X/Y 是相对外接矩形角点的偏移，此处取 (0,0) → 起点即外接矩形左上角 (10,20)
        TextCodeRun run = new TextCodeRun("ABC", 0, 0, deltaX, deltaY, null);
        return new OfdText(new OfdBox(10, 20, 30, 5), 4, 1.0, 0, 0, null, Collections.singletonList(run));
    }

    /**
     * 构造偏移队列
     *
     * @param values 偏移值，单位 mm
     * @return 队列
     */
    private static DeltaQueue queue(Double... values) {
        return new DeltaQueue(new ArrayList<>(Arrays.asList(values)));
    }

    @Test
    @DisplayName("DeltaQueue：首字不偏移，末位尾部增量延续")
    void deltaQueue() {
        DeltaQueue delta = queue(3d, 4d);
        assertEquals(0d, delta.deltaAt(0), 1e-9);
        assertEquals(3d, delta.deltaAt(1), 1e-9);
        assertEquals(4d, delta.deltaAt(2), 1e-9);
        // 超出队列长度：延续最后一个值（表 46 尾部增量）
        assertEquals(4d, delta.deltaAt(3), 1e-9);
        assertEquals(4d, delta.deltaAt(100), 1e-9);
        // 空队列
        assertEquals(0d, DeltaQueue.EMPTY.deltaAt(5), 1e-9);
        assertTrue(DeltaQueue.EMPTY.isEmpty());
        assertEquals(2, delta.size());
        assertEquals(4d, delta.get(1), 1e-9);
    }

    @Test
    @DisplayName("DeltaX：逐字偏移，且不额外叠加字宽（避免双倍位移）")
    void deltaXOnly() {
        // DeltaX 声明为 2、3，第三个字符沿用 3
        TextLayout layout = engine.layout(horizontal(queue(2d, 3d), null), 4d);
        assertEquals(3, layout.getGlyphs().size());
        assertEquals("A", layout.getGlyphs().get(0).getText());
        assertEquals("ABC", layout.getText());
        assertEquals(10, layout.getGlyphs().get(0).getX(), 1e-9);
        assertEquals(20, layout.getGlyphs().get(0).getY(), 1e-9);
        assertEquals(12, layout.getGlyphs().get(1).getX(), 1e-9);
        assertEquals(15, layout.getGlyphs().get(2).getX(), 1e-9);
        // Y 不变
        assertEquals(20, layout.getGlyphs().get(2).getY(), 1e-9);
        assertFalse(layout.getGlyphs().get(0).isShared());
    }

    @Test
    @DisplayName("DeltaY：竖排文字的 Y 偏移独立生效")
    void deltaYOnly() {
        TextLayout layout = engine.layout(horizontal(null, queue(6d)), 4d);
        assertEquals(10, layout.getGlyphs().get(1).getX(), 1e-9);
        assertEquals(26, layout.getGlyphs().get(1).getY(), 1e-9);
        assertEquals(32, layout.getGlyphs().get(2).getY(), 1e-9);
    }

    @Test
    @DisplayName("未声明 Delta：所有字形同址，不按字宽推进")
    void withoutDelta() {
        TextLayout layout = engine.layout(horizontal(null, null), 4d);
        assertEquals(3, layout.getGlyphs().size());
        for (TextLayout.PlacedGlyph glyph : layout.getGlyphs()) {
            assertEquals(10, glyph.getX(), 1e-9);
            assertEquals(20, glyph.getY(), 1e-9);
        }
    }

    @Test
    @DisplayName("表 46：起始原点按字符方向取外接矩形不同角点")
    void startOrigin() {
        OfdBox box = new OfdBox(10, 20, 30, 5);
        assertPoint(engine.startOrigin(textOf(box, 0), box), 10, 20);
        assertPoint(engine.startOrigin(textOf(box, 90), box), 40, 20);
        assertPoint(engine.startOrigin(textOf(box, 180), box), 40, 25);
        assertPoint(engine.startOrigin(textOf(box, 270), box), 10, 25);
    }

    @Test
    @DisplayName("表 47：CharDirection=90 时字形沿 Y 正方向排列")
    void charDirection90() {
        // 外接矩形左上角 (10,20)，竖排起点为其右上角 (40,20)，TextCode 偏移为 (0,0)
        TextCodeRun run = new TextCodeRun("AB", 0, 0, null, queue(5d), null);
        OfdText text = new OfdText(new OfdBox(10, 20, 30, 5), 4, 1.0, 90, 90, null,
                Collections.singletonList(run));
        TextLayout layout = engine.layout(text, 3d);
        // 字形原点 = 外接矩形右上角 + TextCode 偏移；DeltaY=5 使第二个字形下移 5mm（竖排方向）
        assertEquals(40, layout.getGlyphs().get(0).getX(), 1e-9);
        assertEquals(20, layout.getGlyphs().get(0).getY(), 1e-9);
        assertEquals(40, layout.getGlyphs().get(1).getX(), 1e-9);
        assertEquals(25, layout.getGlyphs().get(1).getY(), 1e-9);
    }

    @Test
    @DisplayName("表 47：CharDirection=180/270 的起始原点与 Delta 叠加")
    void charDirection180And270() {
        // 外接矩形左上角 (10,20)：180 度起点为其右下角 (40,25)，TextCode 偏移为 (0,0)
        TextCodeRun run = new TextCodeRun("AB", 0, 0, queue(4d), queue(-4d), null);
        // 字符方向只影响字形朝向；字形位置由 TextCode 的 X/Y 与 Delta 决定
        OfdText text180 = new OfdText(new OfdBox(10, 20, 30, 5), 4, 1.0, 180, 0, null,
                Collections.singletonList(run));
        TextLayout layout180 = engine.layout(text180, 3d);
        assertEquals(40, layout180.getGlyphs().get(0).getX(), 1e-9);
        assertEquals(25, layout180.getGlyphs().get(0).getY(), 1e-9);
        assertEquals(44, layout180.getGlyphs().get(1).getX(), 1e-9);
        assertEquals(21, layout180.getGlyphs().get(1).getY(), 1e-9);

        // 270 度起点为外接矩形左下角 (10,25)，与 180 度的右上角起点不同，需单独构造 TextCode
        TextCodeRun run270 = new TextCodeRun("AB", 0, 0, queue(4d), queue(-4d), null);
        OfdText text270 = new OfdText(new OfdBox(10, 20, 30, 5), 4, 1.0, 270, 0, null,
                Collections.singletonList(run270));
        TextLayout layout270 = engine.layout(text270, 3d);
        assertEquals(10, layout270.getGlyphs().get(0).getX(), 1e-9);
        assertEquals(25, layout270.getGlyphs().get(0).getY(), 1e-9);
    }

    @Test
    @DisplayName("CGTransform 一对一：显式字形索引透传")
    void cgTransformOneToOne() {
        CgTransform cg = new CgTransform(0, 2, Arrays.asList(100, 101));
        TextCodeRun run = new TextCodeRun("AB", 0, 0, queue(4d), null, Collections.singletonList(cg));
        OfdText text = new OfdText(new OfdBox(0, 0, 20, 5), 4, 1.0, 0, 0, null,
                Collections.singletonList(run));
        TextLayout layout = engine.layout(text, 3d);
        assertEquals(100, layout.getGlyphs().get(0).getExplicitGlyphId());
        assertEquals(101, layout.getGlyphs().get(1).getExplicitGlyphId());
        assertEquals(4, layout.getGlyphs().get(1).getX(), 1e-9);
        assertFalse(layout.getGlyphs().get(1).isShared());
    }

    @Test
    @DisplayName("CGTransform 多对一：连字后续字符共享字形，且补足字宽")
    void cgTransformManyToOne() {
        // 两个字符 "AB" 对应一个字形 300
        CgTransform cg = new CgTransform(0, 2, Collections.singletonList(300));
        TextCodeRun run = new TextCodeRun("ABC", 0, 0, queue(1d, 1d, 1d), null, Collections.singletonList(cg));
        OfdText text = new OfdText(new OfdBox(0, 0, 20, 5), 4, 1.0, 0, 0, null,
                Collections.singletonList(run));
        TextLayout layout = engine.layout(text, 3d);
        assertEquals(3, layout.getGlyphs().size());
        assertEquals(300, layout.getGlyphs().get(0).getExplicitGlyphId());
        assertEquals(0, layout.getGlyphs().get(0).getX(), 1e-9);
        // 后续字符共享字形：不产生独立字形，位置仍按 Delta 推进（此处 Delta=1，被跳过）
        // 第三个字符按 Delta 推进，并因前一个共享字符补足了 1 个字宽（3mm）
        double thirdX = layout.getGlyphs().get(2).getX();
        assertEquals(1 + 3, thirdX, 1e-9);
    }

    @Test
    @DisplayName("CGTransform 一对多：首字符展开为多个字形，附属字形同址")
    void cgTransformOneToMany() {
        CgTransform cg = new CgTransform(0, 1, Arrays.asList(200, 201));
        TextCodeRun run = new TextCodeRun("A", 0, 0, null, null, Collections.singletonList(cg));
        OfdText text = new OfdText(new OfdBox(0, 0, 20, 5), 4, 1.0, 0, 0, null,
                Collections.singletonList(run));
        TextLayout layout = engine.layout(text, 3d);
        assertEquals(2, layout.getGlyphs().size());
        assertEquals(200, layout.getGlyphs().get(0).getExplicitGlyphId());
        assertEquals("A", layout.getGlyphs().get(0).getText());
        assertEquals(201, layout.getGlyphs().get(1).getExplicitGlyphId());
        assertEquals("", layout.getGlyphs().get(1).getText());
        assertTrue(layout.getGlyphs().get(1).isShared());
        assertEquals(0, layout.getGlyphs().get(1).getAdvanceMm(), 1e-9);
    }

    @Test
    @DisplayName("CTM：布局只输出对象空间坐标，图元 CTM 由文字渲染器统一乘一次")
    void layoutStaysInObjectSpace() {
        // 外接矩形为 (0,0)，起点 = 外接矩形角点 (0,0) + TextCode 偏移 (5,5)
        TextCodeRun run = new TextCodeRun("AB", 5, 5, queue(10d), null, null);
        OfdText text = new OfdText(new OfdBox(0, 0, 20, 5), 4, 1.0, 0, 0, new double[]{1, 0, 0, 1, 100, 50},
                Collections.singletonList(run));
        TextLayout layout = engine.layout(text, 3d);
        /*
         * 判定：图元 CTM 不在布局阶段叠加。实测 testImageNotFound 可证——基准里文字 x 恰好等于
         * 外接矩形 x（206.9258pt = 73mm），若布局乘 CTM 会得到 73×9.1722 ≈ 670mm 而飞出页面。
         * CTM 只由 TextRendererImpl.elementPositions 作用于“相对角点的偏移”。
         */
        assertEquals(5, layout.getGlyphs().get(0).getX(), 1e-9);
        assertEquals(5, layout.getGlyphs().get(0).getY(), 1e-9);
        // Delta 仍在对象空间累加（未缩放）
        assertEquals(15, layout.getGlyphs().get(1).getX(), 1e-9);
        assertTrue(layout.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("多 TextCode：X/Y 缺失时继承上一个 TextCode")
    void multipleTextCodes() {
        List<TextCodeRun> runs = new ArrayList<>();
        // 外接矩形左上角 (10,20)：两条 TextCode 的 X/Y 是相对该角点的偏移，(0,0) 与 (0,10)
        runs.add(new TextCodeRun("AB", 0, 0, queue(5d), null, null));
        runs.add(new TextCodeRun("CD", 0, 10, queue(5d), null, null));
        OfdText text = new OfdText(new OfdBox(10, 20, 30, 20), 4, 1.0, 0, 0, null, runs);
        TextLayout layout = engine.layout(text, 3d);
        assertEquals(4, layout.getGlyphs().size());
        assertEquals(10, layout.getGlyphs().get(0).getX(), 1e-9);
        assertEquals(15, layout.getGlyphs().get(1).getX(), 1e-9);
        // 第二个 TextCode 以自身 X/Y 为起点，Delta 各自维护（10 → 15）
        assertEquals(10, layout.getGlyphs().get(2).getX(), 1e-9);
        assertEquals(30, layout.getGlyphs().get(2).getY(), 1e-9);
        assertEquals(15, layout.getGlyphs().get(3).getX(), 1e-9);
        assertEquals(30, layout.getGlyphs().get(3).getY(), 1e-9);
    }

    @Test
    @DisplayName("表 46：TextCode 的 X/Y 为相对外接矩形角点的偏移，必须与角点相加")
    void textCodeRelativeToBoundary() {
        // 外接矩形左上角 (24.6062, 37.8354)，TextCode 偏移 (1.5, 2.5) → 字形原点 (26.1062, 40.3354)
        TextCodeRun run = new TextCodeRun("AB", 1.5, 2.5, queue(5d), null, null);
        OfdText text = new OfdText(new OfdBox(24.6062, 37.8354, 164.3062, 6.99), 4, 1.0, 0, 0, null,
                Collections.singletonList(run));
        TextLayout layout = engine.layout(text, 3d);
        assertEquals(26.1062, layout.getGlyphs().get(0).getX(), 1e-9);
        assertEquals(40.3354, layout.getGlyphs().get(0).getY(), 1e-9);
    }

    @Test
    @DisplayName("转义还原：\\XXXX 与 XML 实体")
    void unescape() {
        assertEquals("A B", TextLayoutEngine.unescape("A\\0020B"));
        assertEquals("<x>", TextLayoutEngine.unescape("&lt;x&gt;"));
        assertEquals("\\zz", TextLayoutEngine.unescape("\\zz"));
        assertEquals("", TextLayoutEngine.unescape(null));
    }

    @Test
    @DisplayName("空文字：没有可绘制字形")
    void emptyText() {
        TextCodeRun run = new TextCodeRun("", 0, 0, null, null, null);
        OfdText text = new OfdText(new OfdBox(0, 0, 10, 5), 4, 1.0, 0, 0, null,
                Collections.singletonList(run));
        assertTrue(engine.layout(text, 3d).isEmpty());
    }

    /**
     * 构造指定字符方向的文字图元
     *
     * @param box   外接矩形
     * @param angle 字符方向角度
     * @return 文字图元
     */
    private static OfdText textOf(OfdBox box, double angle) {
        return new OfdText(box, 4, 1.0, angle, 0, null, Collections.emptyList());
    }

    /**
     * 断言点坐标
     *
     * @param point 实际点
     * @param x     期望横坐标
     * @param y     期望纵坐标
     */
    private static void assertPoint(double[] point, double x, double y) {
        assertEquals(x, point[0], 1e-9);
        assertEquals(y, point[1], 1e-9);
    }
}
