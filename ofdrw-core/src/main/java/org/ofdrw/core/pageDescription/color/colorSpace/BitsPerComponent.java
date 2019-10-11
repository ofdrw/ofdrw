package org.ofdrw.core.pageDescription.color.colorSpace;

/**
 * 每个颜色通道使用的位数
 * <p>
 * 有效取值为：1，2，4，8，16
 * <p>
 * 默认值取值为 8
 */
public enum BitsPerComponent {
    BIT_1(1),
    BIT_2(2),
    BIT_4(4),
    BIT_8(8),
    BIT_16(16);

    /**
     * 每个颜色通道使用的位数
     */
    private int bit;

    BitsPerComponent(int bit) {
        this.bit = bit;
    }

    /**
     * 获取  每个颜色通道使用的位数
     *
     * @return 每个颜色通道使用的位数
     */
    public int getValue() {
        return bit;
    }

    /**
     * 获取实例
     *
     * @param bitStr 比特数字符串
     * @return 实例
     */
    public static BitsPerComponent getInstance(String bitStr) {
        if (bitStr == null || bitStr.trim().length() == 0) {
            return BIT_1;
        }
        return getInstance(Integer.parseInt(bitStr));
    }

    /**
     * 获取实例
     *
     * @param bit 比特数
     * @return 实例
     */
    public static BitsPerComponent getInstance(int bit) {
        switch (bit) {
            case 1:
                return BIT_1;
            case 2:
                return BIT_2;
            case 4:
                return BIT_4;
            case 8:
                return BIT_8;
            case 16:
                return BIT_16;
            default:
                throw new IllegalArgumentException("每个颜色通道使用的位数，有效取值为：1，2，4，8，16");
        }
    }

    @Override
    public String toString() {
        return Integer.toString(this.bit);
    }
}
