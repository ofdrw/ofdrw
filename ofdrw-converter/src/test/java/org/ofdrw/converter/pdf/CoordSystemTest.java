package org.ofdrw.converter.pdf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.Box;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CoordSystem} 坐标换算单元测试
 * <p>
 * 设计约束 2：所有 mm→pt 与 Y 翻转换算集中在这里，因此本测试同时锁定换算常量与关键路径。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class CoordSystemTest {

    /**
     * 浮点比较容差
     */
    private static final double DELTA = 1e-9;

    @Test
    @DisplayName("mm 与 pt 的换算比例")
    void millimetersToPoints() {
        assertEquals(72d / 25.4d, CoordSystem.MM_TO_PT, DELTA);
        assertEquals(25.4d / 72d, CoordSystem.PT_TO_MM, DELTA);
        // 内容坐标按 1mm = 2.8346pt 换算，A4 宽 210mm → 595.266pt
        assertEquals(595.266, CoordSystem.mmToPt(210), 1e-9);
        assertEquals(210, CoordSystem.ptToMm(CoordSystem.mmToPt(210)), DELTA);
        assertEquals(0, CoordSystem.mmToPt(0), DELTA);
    }

    @Test
    @DisplayName("Y 轴翻转：OFD 左上原点 → PDF 左下原点")
    void yAxisFlip() {
        // A4 高 297mm：顶部 y=0 对应 PDF y=841.8762，底部 y=297 对应 PDF y=0
        assertEquals(841.8762, CoordSystem.yToPt(0, 297), 1e-9);
        assertEquals(0, CoordSystem.yToPt(297, 297), DELTA);
        assertEquals(420.9381, CoordSystem.yToPt(148.5, 297), 1e-9);
        // 逆变换自洽
        assertEquals(148.5, CoordSystem.yFromPt(CoordSystem.yToPt(148.5, 297), 297), DELTA);
    }

    @Test
    @DisplayName("页面坐标点换算")
    void pointToPage() {
        double[] p = CoordSystem.pointToPage(10, 20, 297);
        assertEquals(CoordSystem.mmToPt(10), p[0], DELTA);
        assertEquals(CoordSystem.yToPt(20, 297), p[1], DELTA);
    }

    @Test
    @DisplayName("OFD 矩形换算后左下角与尺寸正确")
    void boxToPage() {
        // 左上角 (10,20)，宽 50 高 30，页面高 297
        Box box = CoordSystem.boxToPage(new Box(10, 20, 50, 30), 297);
        assertEquals(CoordSystem.mmToPt(10), box.getX(), DELTA);
        // 下边界 y=50 → PDF y=(297-50)*k
        assertEquals(CoordSystem.yToPt(50, 297), box.getY(), DELTA);
        assertEquals(CoordSystem.mmToPt(50), box.getWidth(), DELTA);
        assertEquals(CoordSystem.mmToPt(30), box.getHeight(), DELTA);
    }

    @Test
    @DisplayName("页面框：无偏移时左下角为 (0,0)")
    void pageBoxNoOffset() {
        Box page = CoordSystem.pageBoxToPt(0, 0, 210, 297);
        assertEquals(0, page.getX(), DELTA);
        assertEquals(0, page.getY(), DELTA);
        // 页面框按 72/25.4 精确换算
        assertEquals(210 * CoordSystem.MM_TO_PT, page.getWidth(), DELTA);
        assertEquals(297 * CoordSystem.MM_TO_PT, page.getHeight(), DELTA);
    }

    @Test
    @DisplayName("页面框：带偏移时保留偏移")
    void pageBoxWithOffset() {
        Box page = CoordSystem.pageBoxToPt(5, 7, 100, 50);
        assertEquals(5 * CoordSystem.MM_TO_PT, page.getX(), DELTA);
        assertEquals(7 * CoordSystem.MM_TO_PT, page.getY(), DELTA);
        assertEquals(100 * CoordSystem.MM_TO_PT, page.getWidth(), DELTA);
        assertEquals(50 * CoordSystem.MM_TO_PT, page.getHeight(), DELTA);
    }

    @Test
    @DisplayName("对象空间 → 页面坐标：无 CTM 时等价于平移 + 翻转")
    void objectToPageWithoutCtm() {
        AffineMatrix m = CoordSystem.objectToPage(new Box(10, 20, 30, 40), null, 0, 0, 297);
        // 对象空间原点 (0,0) → OFD 页面 (10,20) → PDF (10*k, (297-20)*k)
        double[] origin = m.transform(0, 0);
        assertEquals(CoordSystem.mmToPt(10), origin[0], DELTA);
        assertEquals(CoordSystem.yToPt(20, 297), origin[1], DELTA);
        // 对象空间 +Y 方向在 PDF 中是 -Y 方向，单位长度按内容坐标比例换算
        double[] down = m.transform(0, 1);
        assertEquals(-CoordSystem.CONTENT_MM_TO_PT, down[1] - origin[1], 1e-6);
        // 对象空间 +X 方向在 PDF 中保持 +X
        double[] right = m.transform(1, 0);
        assertEquals(CoordSystem.CONTENT_MM_TO_PT, right[0] - origin[0], 1e-6);
    }

    @Test
    @DisplayName("对象空间 → 页面坐标：CTM 先作用再叠加平移")
    void objectToPageWithCtm() {
        // 缩放 2 倍
        AffineMatrix ctm = AffineMatrix.scale(2, 2);
        AffineMatrix m = CoordSystem.objectToPage(new Box(0, 0, 10, 10), ctm, 0, 0, 297);
        double[] p = m.transform(3, 4);
        assertEquals(CoordSystem.mmToPt(6), p[0], DELTA);
        assertEquals(CoordSystem.yToPt(8, 297), p[1], DELTA);
    }

    @Test
    @DisplayName("CTM 解析：长度不是 6 时退化为单位矩阵")
    void fromCtm() {
        assertEquals(AffineMatrix.IDENTITY, CoordSystem.fromCtm(null));
        assertEquals(AffineMatrix.IDENTITY, CoordSystem.fromCtm(new double[]{1, 2}));
        AffineMatrix ctm = CoordSystem.fromCtm(new double[]{1, 0, 0, 1, 5, 6});
        assertArrayEquals(new double[]{1, 0, 0, 1, 5, 6}, ctm.toArray(), DELTA);
        assertTrue(ctm.isInvertible());
        assertFalse(new AffineMatrix(0, 0, 0, 0, 0, 0).isInvertible());
        assertThrows(IllegalStateException.class, () -> new AffineMatrix(0, 0, 0, 0, 0, 0).inverse());
    }
}
