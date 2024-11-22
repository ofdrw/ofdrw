package org.ofdrw.converter;

import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.reader.OFDReader;

import java.awt.Color;

/**
 * 图片转换类-ofd转图片
 * 如果图片边线/边框/线条丢失，可以尝试使用这个类转图片
 * defaultStrokeColor-默认为红色边线
 *
 * @author yangzhenyi
 * @since 2024-11-22 11:08:00
 */
public class OFDImageMaker extends ImageMaker {

    private Color defaultStrokeColor = new Color(128, 0, 0);

    public OFDImageMaker(OFDReader reader, int ppm) {
        super(reader, ppm);
    }

    public OFDImageMaker(OFDReader reader, double ppm) {
        super(reader, ppm);
    }

    public OFDImageMaker(OFDReader reader, int ppm, Color color) {
        super(reader, ppm);
        this.defaultStrokeColor = color;
    }

    public OFDImageMaker(OFDReader reader, double ppm, Color color) {
        super(reader, ppm);
        this.defaultStrokeColor = color;
    }

    @Override
    public Color getColor(CT_Color ctColor) {
        if (ctColor != null && ctColor.getValue() == null) {
            return defaultStrokeColor;
        }
        return super.getColor(ctColor);
    }
}
