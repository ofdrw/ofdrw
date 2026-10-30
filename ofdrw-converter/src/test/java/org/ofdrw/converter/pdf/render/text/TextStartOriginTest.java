package org.ofdrw.converter.pdf.render.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.render.text.OfdText;
import org.ofdrw.converter.pdf.render.text.OfdBox;
import org.ofdrw.converter.pdf.render.text.TextLayoutEngine;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * 文字起点：按 CharDirection 选择外接矩形角点
 * <p>
 * GB/T 33190-2016 表 47：字符方向决定文字流的起始角——
 * 0° 取左上、90° 取右上、180° 取右下、270° 取左下。{@code TextCode} 的 X/Y 是相对该角点的偏移，
 * 因此角点选错会让整段文字整体偏移一个外接矩形的宽度或高度。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class TextStartOriginTest {

    /**
     * 文字布局引擎
     */
    private static final TextLayoutEngine ENGINE = new TextLayoutEngine();

    /**
     * 外接矩形：(10, 20)，宽 30、高 40
     */
    private static final OfdBox BOUNDARY = new OfdBox(10, 20, 30, 40);

    @Test
    @DisplayName("CharDirection=0：起点取外接矩形左上角")
    void originForZero() {
        assertArrayEquals(new double[]{10, 20}, ENGINE.startOrigin(text(0), BOUNDARY), 1e-9);
    }

    @Test
    @DisplayName("CharDirection=90：起点取外接矩形右上角")
    void originForNinety() {
        assertArrayEquals(new double[]{40, 20}, ENGINE.startOrigin(text(90), BOUNDARY), 1e-9);
    }

    @Test
    @DisplayName("CharDirection=180：起点取外接矩形右下角")
    void originForOneEighty() {
        assertArrayEquals(new double[]{40, 60}, ENGINE.startOrigin(text(180), BOUNDARY), 1e-9);
    }

    @Test
    @DisplayName("CharDirection=270：起点取外接矩形左下角")
    void originForTwoSeventy() {
        assertArrayEquals(new double[]{10, 60}, ENGINE.startOrigin(text(270), BOUNDARY), 1e-9);
    }

    @Test
    @DisplayName("起点与 TextCode 的 X/Y 相加后才进入布局（X/Y 为相对偏移）")
    void layoutAddsCodeOffsetToOrigin() {
        // 90° 方向：起点 (40,20)，TextCode 偏移 (2,3) → 首字形 (42,23)
        OfdText text = text(90);
        org.ofdrw.converter.pdf.render.text.TextCodeRun code =
                new org.ofdrw.converter.pdf.render.text.TextCodeRun("A", 2, 3,
                        org.ofdrw.converter.pdf.render.text.DeltaQueue.EMPTY,
                        org.ofdrw.converter.pdf.render.text.DeltaQueue.EMPTY,
                        Collections.<org.ofdrw.converter.pdf.render.text.CgTransform>emptyList());
        org.ofdrw.converter.pdf.render.text.TextLayout layout = ENGINE.layout(withCode(text, code), 1d);
        org.ofdrw.converter.pdf.render.text.TextLayout.PlacedGlyph first = layout.getGlyphs().get(0);
        assertArrayEquals(new double[]{42, 23}, new double[]{first.getX(), first.getY()}, 1e-9);
    }

    /**
     * 构造指定字符方向的文字对象
     *
     * @param charDirection 字符方向，单位度
     * @return 文字对象
     */
    private static OfdText text(double charDirection) {
        return new OfdText(BOUNDARY, 4d, 1d, charDirection, 0d, null,
                Collections.<org.ofdrw.converter.pdf.render.text.TextCodeRun>emptyList());
    }

    /**
     * 给文字对象追加一个 TextCode
     *
     * @param text 文字对象
     * @param code TextCode
     * @return 含该 TextCode 的文字对象
     */
    private static OfdText withCode(OfdText text, org.ofdrw.converter.pdf.render.text.TextCodeRun code) {
        return new OfdText(text.getBoundary(), text.getSizeMm(), text.getHScale(),
                text.getCharDirectionDeg(), text.getReadDirectionDeg(), text.getCtm(),
                Collections.singletonList(code));
    }
}
