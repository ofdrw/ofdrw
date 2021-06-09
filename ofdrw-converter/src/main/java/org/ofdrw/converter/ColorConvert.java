package org.ofdrw.converter;

import com.itextpdf.kernel.colors.DeviceRgb;
import org.ofdrw.converter.utils.CommonUtil;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.color.colorSpace.CV;
import org.ofdrw.core.pageDescription.color.colorSpace.Palette;
import org.ofdrw.reader.ResourceManage;

import java.util.List;

/**
 * 色彩转换
 *
 * @since 2021-06-08 19:23:40
 */
final public class ColorConvert {

    /**
     * 转换为HTML RGB颜色
     *
     * @param colorArray 颜色数组
     * @return HTML RGB颜色字符串 rgb(r,g,b)
     */
    public static String convertOfdColorToHtml(ST_Array colorArray) {
        String color = null;
        String colorStr = colorArray.toString();
        if (colorStr.indexOf("#") != -1) {
            colorStr = colorStr.replaceAll("#", "");
            colorStr = colorStr.replaceAll(" ", "");
            color = String.format("#%s", colorStr);
        } else {
            float[] colors = CommonUtil.doubleArrayToFloatArray(colorArray.toDouble());
            if (colors.length == 3) {
                color = String.format("rgb(%d,%d,%d)", (int) colors[0], (int) colors[1], (int) colors[2]);
            } else if (colors.length == 1) {
                color = String.format("rgb(%d,%d,%d)", (int) colors[0], (int) colors[0], (int) colors[0]);
            }
        }
        return color;
    }

    /**
     * 转换为PDF RGB颜色
     *
     * @param colorArray 颜色数值
     * @return PDF颜色对象
     */
    public static com.itextpdf.kernel.colors.Color convertOfdColor(ST_Array colorArray) {
        com.itextpdf.kernel.colors.Color color = null;
        String colorStr = colorArray.toString();
        if (colorStr.indexOf("#") != -1) {
            String[] rgbStr = colorStr.split(" ");
            String r = rgbStr[0].replaceAll("#", "");
            String g = rgbStr[1].replaceAll("#", "");
            String b = rgbStr[2].replaceAll("#", "");
            if (r.length() == 1) {
                r += "0";
            }
            if (g.length() == 1) {
                g += "0";
            }
            if (b.length() == 1) {
                b += "0";
            }
            java.awt.Color jColor = java.awt.Color.decode(String.format("#%s%s%s", r, g, b));
            color = new DeviceRgb(jColor.getRed() / 255f, jColor.getGreen() / 255f, jColor.getBlue() / 255f);
        } else {
            float[] colors = CommonUtil.doubleArrayToFloatArray(colorArray.toDouble());
            if (colors.length == 3) {
                color = new DeviceRgb(colors[0] / 255f, (int) colors[1] / 255f, (int) colors[2] / 255f);
            }
        }
        return color;
    }

    /**
     * PDF的RGB颜色
     *
     * @param resMgt  资源管理器
     * @param ctColor 颜色
     * @return PDF RGB颜色
     */
    public static com.itextpdf.kernel.colors.Color pdfRGB(ResourceManage resMgt, CT_Color ctColor) {
        final int[] rgb = rgb(resMgt, ctColor);
        return new DeviceRgb(rgb[0] / 255f, (int) rgb[1] / 255f, (int) rgb[2] / 255f);
    }

    /**
     * 转换为RGB颜色
     *
     * @param resMgt  资源管理器
     * @param ctColor 颜色
     * @return RGB数组
     */
    public static int[] rgb(ResourceManage resMgt, CT_Color ctColor) {
        if (ctColor == null) {
            return new int[]{0, 0, 0};
        }
        ST_Array colorValues = ctColor.getValue();
        final Integer index = ctColor.getIndex();
        if (colorValues == null && index == null) {
            // Index 和  colorValues 都不出现式，该颜色个通道的值全部为0
            return new int[]{0, 0, 0};
        }

        CT_ColorSpace cs = null;
        ST_RefID colorSpaceId = ctColor.getColorSpace();
        if (colorSpaceId != null && resMgt != null) {
            // 尝试从资源管理器中获取颜色空间
            cs = resMgt.getColorSpace(colorSpaceId.toString());
        }
        if (cs == null) {
            // 颜色空间不存在时，采用RGB作为默认颜色空间
            if (colorValues == null) {
                return new int[]{0, 0, 0};
            }
            return colorValues.expectIntArr(3);
        }

        // 颜色索引，存在时从索引位置获取颜色数值
        if (index != null) {
            final Palette palette = cs.getPalette();
            if (palette == null) {
                return new int[]{0, 0, 0};
            }
            final List<CV> cVs = palette.getCVs();
            colorValues = cVs.get(index).getColor();
        }
        // 根据颜色空间类型，转换颜色为RGB
        switch (cs.getType()) {
            case RGB:
                return colorValues.expectIntArr(3);
            case CMYK:
                final int[] cmyk = colorValues.expectIntArr(4);
                return cmykToRgb(cmyk[0], cmyk[1], cmyk[2], cmyk[3]);
            case GRAY:
                final int[] gray = colorValues.expectIntArr(1);
                return new int[]{gray[0], gray[0], gray[0]};
        }
        return new int[]{0, 0, 0};
    }

    /**
     * 转换 CMYK为RGB
     *
     * @param c 青色
     * @param m 洋红色
     * @param y 黄色
     * @param k 黑色
     * @return RGB颜色值
     */
    public static int[] cmykToRgb(int c, int m, int y, int k) {
        c = Math.min(c, 100);
        c = Math.max(c, 0);

        m = Math.min(m, 100);
        m = Math.max(m, 0);

        y = Math.min(y, 100);
        y = Math.max(y, 0);

        k = Math.min(k, 100);
        k = Math.max(k, 0);

        int r = 255 * (1 - c / 100) * (1 - k / 100);
        int g = 255 * (1 - m / 100) * (1 - k / 100);
        int b = 255 * (1 - y / 100) * (1 - k / 100);
        return new int[]{r, g, b};
    }

}
