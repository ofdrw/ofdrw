package org.ofdrw.converter.pdf.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.converter.pdf.model.PdfPageContent;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 非零页面框原点（决策 26）：{@code MediaBox = CropBox = PhysicalBox}
 * <p>
 * OFD 的 {@code PhysicalBox} 允许左上角不是 (0,0)（例如 {@code 10 20 210 297}）。
 * 此时渲染器必须把页面原点偏移 {@code pageX0/pageY0} 一并计入，否则所有图元会整体偏移。
 * 本测试锁定 {@link CoordSystem} 的换算口径，渲染器（路径/图像/文字/注释）统一复用它。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class NonZeroPageOriginTest {

    /**
     * 页面框原点
     */
    private static final double PAGE_X0 = 10;

    /**
     * 页面框原点
     */
    private static final double PAGE_Y0 = 20;

    /**
     * 页面高度，单位 mm
     */
    private static final double PAGE_H = 297;

    @Test
    @DisplayName("页内绝对坐标：叠加 pageX0/pageY0 后换算为页面 pt")
    void absoluteCoordinatesIncludePageOrigin() {
        // 页内 (10,20) → 对象空间 (20,40) → 页面 pt：(20k, (297-40)k)
        double[] actual = CoordSystem.absoluteToPage(null, PAGE_X0, PAGE_Y0, PAGE_H).transform(10, 20);
        double k = CoordSystem.MM_TO_PT;
        assertEquals(20 * k, actual[0], 1e-6);
        assertEquals((PAGE_H - 40) * k, actual[1], 1e-6);
    }

    @Test
    @DisplayName("图元外接矩形：平移与页面原点同时生效")
    void elementBoxIncludesPageOrigin() {
        Box box = CoordSystem.boundary(5, 6, 20, 10);
        double[] topLeft = CoordSystem.objectToPage(box, null, PAGE_X0, PAGE_Y0, PAGE_H).transform(0, 0);
        double k = CoordSystem.MM_TO_PT;
        assertEquals((PAGE_X0 + 5) * k, topLeft[0], 1e-6);
        assertEquals((PAGE_H - PAGE_Y0 - 6) * k, topLeft[1], 1e-6);
    }

    @Test
    @DisplayName("页面内容尺寸：MediaBox 等于 PhysicalBox 的宽高（与原点无关）")
    void pageContentSizeIndependentOfOrigin() {
        PdfPageContent content = new PdfPageContent(210 * CoordSystem.MM_TO_PT + PAGE_X0 * CoordSystem.MM_TO_PT,
                297 * CoordSystem.MM_TO_PT);
        assertEquals((210 + PAGE_X0) * CoordSystem.MM_TO_PT, content.getWidthPt(), 1e-6);
        assertEquals(297 * CoordSystem.MM_TO_PT, content.getHeightPt(), 1e-6);
    }
}
