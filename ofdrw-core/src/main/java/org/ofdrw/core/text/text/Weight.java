package org.ofdrw.core.text.text;

/**
 * 文字对象的粗细值
 * <p>
 * 11.3 表 45
 *
 * @author 权观宇
 * @since 2019-10-18 09:56:38
 */
public enum Weight {

    /**
     * 可选值为
     * 100，200，300，400，500，600，700，800，900
     */
    W_100(100),
    W_200(200),
    W_300(300),
    /**
     * 默认值
     */
    W_400(400),
    W_500(500),
    W_600(600),
    W_700(700),
    W_800(800),
    W_900(900);

    Weight(Integer weight) {
        this.weight = weight;
    }

    Integer weight;

    public static Weight getInstance(int weight) {
        return getInstance(String.valueOf(weight));
    }

    public static Weight getInstance(String weight) {
        if (weight == null || weight.trim().length() == 0) {
            weight = "400";
        }
        switch (weight) {
            case "100":
                return W_100;
            case "200":
                return W_200;
            case "300":
                return W_300;
            case "400":
                return W_400;
            case "500":
                return W_500;
            case "600":
                return W_600;
            case "700":
                return W_700;
            case "800":
                return W_800;
            case "900":
                return W_900;
            default:
                throw new NumberFormatException("错误的文字对象的粗细值：" + weight);
        }
    }

    /**
     * 获取字体粗细值
     *
     * @return 粗细值
     */
    public Integer getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return Integer.toString(weight);
    }
}
