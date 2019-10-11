package org.ofdrw.core.pageDescription.color.colorSpace;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Array;

/**
 * 调色板中预定义的颜色
 * <p>
 * 调色板中颜色的索引编号从 0 开始
 * <p>
 * 8.3 颜色 表 25
 */
public class CV extends OFDElement {
    public CV(Element proxy) {
        super(proxy);
    }

    public CV() {
        super("CV");
    }

    /**
     * 颜色表示：
     * <p>
     * Gray - 通过一个通道来表明灰度值；例如 "#FF 255"
     * <p>
     * RGB - 包含3个通道，一次是红、绿、蓝；例如 "#11 #22 #33"、"17 34 51"
     * <p>
     * CMYK - 包含4个通道，依次是青、黄、品红、黑；例如 "#11 #22 #33 # 44"、"17 34 51 68"
     *
     * @param color 设置预定义的颜色
     */
    public CV(ST_Array color) {
        this();
        this.setColor(color);
    }

    /**
     * 设置 预定义的颜色
     * <p>
     * 颜色表示：
     * <p>
     * Gray - 通过一个通道来表明灰度值；例如 "#FF 255"
     * <p>
     * RGB - 包含3个通道，一次是红、绿、蓝；例如 "#11 #22 #33"、"17 34 51"
     * <p>
     * CMYK - 包含4个通道，依次是青、黄、品红、黑；例如 "#11 #22 #33 # 44"、"17 34 51 68"
     *
     * @param value 设置预定义的颜色
     * @return this
     */
    public CV setColor(ST_Array value) {
        this.addText(value.toString());
        return this;
    }

    /**
     * 获取 预定义的颜色
     * <p>
     * 颜色表示：
     * <p>
     * Gray - 通过一个通道来表明灰度值；例如 "#FF 255"
     * <p>
     * RGB - 包含3个通道，一次是红、绿、蓝；例如 "#11 #22 #33"、"17 34 51"
     * <p>
     * CMYK - 包含4个通道，依次是青、黄、品红、黑；例如 "#11 #22 #33 # 44"、"17 34 51 68"
     *
     * @return 设置预定义的颜色
     */
    public ST_Array getColor() {
        return ST_Array.getInstance(this.getText());
    }

}
