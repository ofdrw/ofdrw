package org.ofdrw.layout.element.canvas;

import java.util.Arrays;

/**
 * 分析结果
 *
 * @author 权观宇
 * @since 2020-05-10 18:27:27
 */
public class MeasureBody {
    /**
     * 文本字符间相对偏移量
     */
    public Double[] offset;
    /**
     * 文本在阅读方向上的总宽度
     */
    public double width;

    /**
     * 文本在第一个字符相对偏移 x坐标
     */
    public double firstCharOffsetX;
    /**
     * 文本在第一个字符相对偏移 y坐标
     */
    public double firstCharOffsetY;


    public MeasureBody() {
        offset = new Double[0];
        width = 0;
        firstCharOffsetX = 0;
        firstCharOffsetY = 0;
    }

    /**
     * 加上偏移量后的宽度
     *
     * @param charLen 字符宽度
     */
    public void with(double charLen) {
        width = Arrays.stream(offset).mapToDouble(Math::abs).sum() + charLen;
    }
}