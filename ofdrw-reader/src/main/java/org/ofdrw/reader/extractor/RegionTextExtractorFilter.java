package org.ofdrw.reader.extractor;

import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.reader.DeltaTool;

import java.awt.Rectangle;
import java.util.List;

/**
 * 矩形框文本提取过滤器
 *
 * @author minghu.zhang
 * @since 2021-08-09 20:53:09
 */
public class RegionTextExtractorFilter implements ExtractorFilter {

    /**
     * 文本提取矩形框
     */
    private final Rectangle rectangle;

    public RegionTextExtractorFilter(Rectangle rectangle) {
        this.rectangle = rectangle;
    }

    /**
     * 提取对象在矩形区域内的文本
     *
     * @param textObject 文本域对象
     * @param textCode   文字定位对象
     * @return 矩形区域内的文本
     */
    @Override
    public String getAllowText(TextObject textObject, TextCode textCode) {
        String content = textCode.getContent();
        String allowText = null;
        if (content != null && !"".equals(content)) {
            List<Float> deltaX = DeltaTool.getDelta(textCode.getDeltaX(), textCode.getContent().length());
            List<Float> deltaY = DeltaTool.getDelta(textCode.getDeltaY(), textCode.getContent().length());

            emptyDeltaHandle(content, deltaX, deltaY);

            ST_Box boundary = textObject.getBoundary();
            Double topLeftX = boundary.getTopLeftX();
            Double topLeftY = boundary.getTopLeftY();

            if (topLeftX == null) {
                topLeftX = 0D;
            }
            if (topLeftY == null) {
                topLeftY = 0D;
            }

            double[] matrix = getMatrix(textObject.getCTM());

            double sx = textCode.getX() == null ? 0 : textCode.getX();
            double sy = textCode.getY() == null ? 0 : textCode.getY();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                char ch = content.charAt(i);
                if (matrix != null) {
                    ST_Pos transform = transform(matrix, sx, sy);
                    if (rectangle.contains(topLeftX + transform.getX(), topLeftY + transform.getY())) {
                        builder.append(ch);
                    }
                } else {
                    if (rectangle.contains(topLeftX + sx, topLeftY + sy)) {
                        builder.append(ch);
                    }
                }
                sx += deltaX.get(i);
                sy += deltaY.get(i);
            }

            if (builder.length() != 0) {
                allowText = builder.toString();
            }
        }


        return allowText;
    }

    /**
     * 坐标转换
     *
     * @param matrix 矩阵数组
     * @param sx     原始X
     * @param sy     原始Y
     * @return 计算后位置
     */
    private static ST_Pos transform(double[] matrix, double sx, double sy) {
        double x = matrix[0] * sx + matrix[2] * sy + matrix[4];
        double y = matrix[1] * sx + matrix[3] * sy + matrix[5];
        return new ST_Pos(x, y);
    }

    /**
     * 获取Matrix数据
     *
     * @param ctm ctm对象
     * @return 矩阵对象
     */
    private static double[] getMatrix(ST_Array ctm) {
        double[] matrix = null;
        if (ctm != null) {
            List<String> ctmArray = ctm.getArray();
            matrix = new double[ctmArray.size()];
            for (int i = 0; i < ctmArray.size(); i++) {
                matrix[i] = Double.parseDouble(ctmArray.get(i));
            }
        }
        return matrix;
    }

    /**
     * 空delta值处理
     */
    private void emptyDeltaHandle(String content, List<Float> deltaX, List<Float> deltaY) {
        if (deltaX.isEmpty()) {
            for (int i = 0; i < content.length(); i++) {
                deltaX.add(0F);
            }
        }

        if (deltaY.isEmpty()) {
            for (int i = 0; i < content.length(); i++) {
                deltaY.add(0F);
            }
        }
    }
}
