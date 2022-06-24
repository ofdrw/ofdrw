package org.ofdrw.sign;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数字格式的自增的签名ID
 * <p>
 * 启用前缀格式为： "NNN"，例如：'001'
 * <p>
 * 关闭前缀格式为： "N"，例如：'1' （默认）
 *
 * @author 权观宇
 * @since 2020-04-17 04:20:15
 */
public class NumberFormatAtomicSignID implements SignIDProvider {
    /**
     * 签名ID自增提供者
     */
    private final AtomicInteger provider;

    /**
     * 是否增加0前缀
     * <p>
     * 默认关闭0前缀
     */
    private boolean enableZeroPrefix = false;

    public NumberFormatAtomicSignID() {
        provider = new AtomicInteger(0);
    }

    /**
     * 创建数字构造器
     *
     * @param enableZeroPrefix 是否启用前缀0，启用后ID格式为'00N'
     */
    public NumberFormatAtomicSignID(boolean enableZeroPrefix) {
        provider = new AtomicInteger(0);
        this.enableZeroPrefix = enableZeroPrefix;
    }

    /**
     * 创建指定最大签名ID 签名ID提供器
     *
     * @param maxSignID 最大签名ID字符串
     */
    public NumberFormatAtomicSignID(String maxSignID) {
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
     * @return 签名ID，形如：'001'
     */
    @Override
    public String incrementAndGet() {
        int newSignID = provider.incrementAndGet();
        if (enableZeroPrefix) {
            return String.format("%03d", newSignID);
        } else {
            return String.valueOf(newSignID);
        }
    }

    /**
     * 获取当前签名ID
     *
     * @return 签名ID
     */
    @Override
    public String get() {
        int maxSignId = provider.get();
        if (enableZeroPrefix) {
            return String.format("%03d", maxSignId);
        } else {
            return String.valueOf(maxSignId);
        }
    }

    /**
     * 解析出电子签名的ID数字
     *
     * @param id ID字符串
     * @return ID数字
     */
    @Override
    public int parse(String id) {
        return Integer.parseInt(id);
    }


    /**
     * 开关是否启用 0前缀
     *
     * @param enableZeroPrefix true - 启用0前缀； false - 关闭
     */
    public void setEnableZeroPrefix(boolean enableZeroPrefix) {
        this.enableZeroPrefix = enableZeroPrefix;
    }

    /**
     * 是否启用0前缀
     *
     * @return true - 启用0前缀
     */
    public boolean isEnableZeroPrefix() {
        return enableZeroPrefix;
    }
}
