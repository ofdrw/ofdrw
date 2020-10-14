package org.ofdrw.sign;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自增的签名ID
 *
 * @author 权观宇
 * @since 2020-04-17 04:20:15
 */
public class StandFormatAtomicSignID implements SignIDProvider {

    /**
     * 签名ID自增提供者
     */
    private final AtomicInteger provider;

    public StandFormatAtomicSignID() {
        provider = new AtomicInteger(0);
    }

    /**
     * 创建指定最大签名ID 签名ID提供器
     *
     * @param maxSignID 最大签名ID字符串
     */
    public StandFormatAtomicSignID(String maxSignID) {
        int maxSignIDNum = this.parse(maxSignID);
        provider = new AtomicInteger(maxSignIDNum);
    }


    /**
     * 设置当前最大签名ID值
     * <p>
     * 实现者需要自己解析该字符串，并设置内置计数器
     *
     * @param maxSignId 当前最大签名ID格式字符串
     */
    @Override
    public void setCurrentMaxSignId(String maxSignId) {
        int maxSignIDNum = this.parse(maxSignId);
        provider.set(maxSignIDNum);
    }

    /**
     * 增长并获取签名ID
     *
     * @return 签名ID，形如：'s001'
     */
    @Override
    public String incrementAndGet() {
        int newSignID = provider.incrementAndGet();
        return String.format("s%03d", newSignID);
    }

    /**
     * 获取当前签名ID
     * @return 签名ID
     */
    @Override
    public String get(){
        int maxSignId = provider.get();
        return String.format("s%03d", maxSignId);
    }

    /**
     * 解析出电子签名的ID数字
     *
     * @param id ID字符串
     * @return ID数字
     */
    @Override
    public int parse(String id) {
        return SignIdParser.parseIndex(id);
    }

}
