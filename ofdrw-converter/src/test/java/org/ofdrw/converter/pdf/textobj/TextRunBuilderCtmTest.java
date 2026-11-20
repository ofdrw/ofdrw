package org.ofdrw.converter.pdf.textobj;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.text.TextCode;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 文字对象 CTM 分支的坐标系约定回归
 * <p>
 * {@code TextObject} 的 {@code Size}、{@code TextCode.X/Y}、{@code DeltaX/DeltaY} 都按<b>对象坐标系</b>
 * 给出（GB/T 33190-2016 表 45、表 46），而 {@code Boundary} 按当前空间坐标系（表 34）即页面 mm 给出。
 * 本用例直接构造字形并调用 {@link TextRunBuilder#build}，断言产出的文字对象满足上述约定，
 * 不依赖任何查看器，用于在未来启用 CTM 分支时保护这些换算。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class TextRunBuilderCtmTest {

    /**
     * 页面 em：12pt 字号的页面长度，单位 mm
     */
    private static final double EM_PAGE = 12d * 25.4d / 72d;

    /**
     * 1 mm 对应的 PDF 用户空间点数
     */
    private static final double PT_PER_MM = 72d / 25.4d;

    @Test
    void rotatedRunWritesConjugatedCtm() {
        // 逆时针 30°：linear = (cos, -sin, -sin, -cos)，两列模均为 1（纯旋转）
        double rad = Math.toRadians(30);
        AffineTransform linear = new AffineTransform(
                Math.cos(rad), -Math.sin(rad), -Math.sin(rad), -Math.cos(rad), 0d, 0d);
        TextObject text = buildSingle("A", linear, 60d, 60d);

        ST_Array ctm = text.getCTM();
        assertNotNull(ctm, "带 CTM 的文字对象应写出 CTM");
        double[] v = toDouble(ctm);
        /*
         * CTM 必须剔除 PDF 的 y 轴翻转：取 diag(1,-1)·linear，即 a 不变、b/c/d 取反。
         * linear 为 (0.866,-0.5,-0.5,-0.866) 时，CTM 应为 (0.866,-0.5,0.5,0.866)。
         */
        assertEquals(Math.cos(rad), v[0], 1e-3, "a 不应取反");
        assertEquals(Math.sin(rad), v[1], 1e-3, "b 应取反");
        assertEquals(-Math.sin(rad), v[2], 1e-3, "c 应取反");
        assertEquals(Math.cos(rad), v[3], 1e-3, "d 应取反，使字形不再上下颠倒");
    }

    @Test
    void horizontalScaleRunWritesPositiveD() {
        // 横向拉宽 1.286 倍：linear = (1.286, 0, 0, -1)
        AffineTransform linear = new AffineTransform(1.286d, 0d, 0d, -1d, 0d, 0d);
        TextObject text = buildSingle("A", linear, 60d, 60d);

        double[] v = toDouble(text.getCTM());
        assertEquals(1.286d, v[0], 1e-3, "横向缩放保留在 a 分量");
        assertEquals(0d, v[1], 1e-6, "b 应为 0");
        assertEquals(0d, v[2], 1e-6, "c 应为 0");
        assertEquals(1d, v[3], 1e-3, "d 应为 +1，负值会使字形上下颠倒");
    }

    @Test
    void sizeEqualsPageEmDividedByLinearYColumn() {
        // 等比缩放 0.5：linear 的 y 列长度为 0.5，对象坐标系字号应为页面的 2 倍
        AffineTransform linear = new AffineTransform(0.5d, 0d, 0d, -0.5d, 0d, 0d);
        TextObject text = buildSingle("A", linear, 60d, 60d);

        double yColumn = Math.hypot(linear.getShearX(), linear.getScaleY());
        assertEquals(EM_PAGE / yColumn, text.getSize(), 1e-3,
                "Size 为对象坐标系字号，等于页面 em 除以 linear 的 y 列长度");
    }

    @Test
    void horizontalRunHasNoCtmAndKeepsSize() {
        // 普通横排：linear = (1,0,0,-1)，不写 CTM（缺省约定已等价）
        AffineTransform linear = new AffineTransform(1d, 0d, 0d, -1d, 0d, 0d);
        TextObject text = buildSingle("A", linear, 60d, 60d);

        assertNull(text.getCTM(), "普通横排文字不应写 CTM");
        assertEquals(EM_PAGE, text.getSize(), 1e-3, "无 CTM 时 Size 即页面 em");
    }

    @Test
    void boundaryIsPageBoxAndCoversGlyph() {
        AffineTransform linear = new AffineTransform(
                Math.cos(Math.toRadians(30)), -Math.sin(Math.toRadians(30)),
                -Math.sin(Math.toRadians(30)), -Math.cos(Math.toRadians(30)), 0d, 0d);
        TextObject text = buildSingle("A", linear, 60d, 60d);

        // Boundary 按页面 mm 给出：必须覆盖字形轮廓（em 量级），而不是对象坐标系的 em 量级（约 1）
        double height = text.getBoundary().getHeight();
        assertEquals(EM_PAGE * 2, height, EM_PAGE, "旋转文字的 Boundary 高度应与页面字形尺寸同量级");
    }

    @Test
    void deltaLengthIsGlyphCountMinusOne() {
        // 相邻两字：DeltaX/DeltaY 各 1 项（n−1 个间隔，表 46）
        AffineTransform linear = new AffineTransform(1.286d, 0d, 0d, -1d, 0d, 0d);
        TextRunBuilder builder = newBuilder();
        builder.add(glyph("A", linear, 60d, 60d));
        builder.add(glyph("B", linear, 60d + 3d * 1.286d, 60d));

        TextObject text = builder.build(new ST_ID("7"), false);
        TextCode code = text.getTextCodes().get(0);
        assertEquals(1, toDouble(code.getDeltaX()).length, "DeltaX 项数应为字形数 − 1");
        assertEquals(3d, toDouble(code.getDeltaX())[0], 1e-3,
                "DeltaX 为对象坐标系位移 = 页面位移除以 linear 的 x 列长度");
    }

    /**
     * 构造只含一个字形的文字对象
     *
     * @param unicode 文字内容
     * @param linear  对象坐标系到页面的线性部分
     * @param originX 字形原点页面 x，单位 mm
     * @param originY 字形原点页面 y，单位 mm
     * @return 文字对象
     */
    private static TextObject buildSingle(String unicode, AffineTransform linear, double originX, double originY) {
        TextRunBuilder builder = newBuilder();
        builder.add(glyph(unicode, linear, originX, originY));
        return builder.build(new ST_ID("7"), false);
    }

    /**
     * 创建文字 run 构造器（字体与画布在 build 阶段不参与几何计算）
     *
     * @return 文字 run 构造器
     */
    private static TextRunBuilder newBuilder() {
        return new TextRunBuilder(null, new TextRunBuilder.TextRunStyle(true, false, null, null, 0d, 255), null);
    }

    /**
     * 构造字形
     *
     * @param unicode 文字内容
     * @param linear  对象坐标系到页面的线性部分
     * @param originX 字形原点页面 x，单位 mm
     * @param originY 字形原点页面 y，单位 mm
     * @return 字形
     */
    private static TextRunBuilder.Glyph glyph(String unicode, AffineTransform linear,
                                              double originX, double originY) {
        // 字形轮廓取 1 em 见方的近似盒，上伸/下伸部按常见的 0.8 / 0.2 em 估计
        Rectangle2D box = new Rectangle2D.Double(originX, originY - EM_PAGE * 0.8,
                EM_PAGE, EM_PAGE * 0.8);
        return new TextRunBuilder.Glyph(unicode, 1, originX, originY, EM_PAGE, linear, box,
                EM_PAGE * 0.8, EM_PAGE * 0.2);
    }

    /**
     * OFD 数组转为 double 数组
     *
     * @param array OFD 数组
     * @return 数值数组
     */
    private static double[] toDouble(ST_Array array) {
        Double[] values = array.toDouble();
        double[] out = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i];
        }
        return out;
    }
}