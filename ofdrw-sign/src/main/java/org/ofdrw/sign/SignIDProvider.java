package org.ofdrw.sign;

/**
 * 签名ID提供者
 * <p>
 * 开发者可以根据实际配置签名ID的格式
 *
 * @author 权观宇
 * @since 2020-08-24 20:12:35
 */
public interface SignIDProvider {

    /**
     * 设置当前最大签名ID值
     * <p>
     * 实现者需要自己解析该字符串，并设置内置计数器
     *
     * @param maxSignId 当前最大签名ID格式字符串
     */
    void setCurrentMaxSignId(String maxSignId);

    /**
     * 增长并获取签名ID
     *
     * @return 签名ID，形如：'s001'
     */
    String incrementAndGet();


    /**
     * 获取当前签名ID，不增长
     *
     * @return 签名ID
     */
    String get();


    /**
     * 解析出电子签名的ID数字
     *
     * @param id ID字符串
     * @return ID数字
     */
    int parse(String id);
}
