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
public class AtomicSignID {

    /**
     * 根据标准推荐ID样式为
     * <p>
     * 'sNNN',NNN从1起。
     */
    public static final Pattern IDPattern = Pattern.compile("s(\\d{3})");
    /**
     * 密钥ID自增提供者
     */
    private final AtomicInteger provider;

    public AtomicSignID() {
        provider = new AtomicInteger(0);
    }

    /**
     * 创建指定最大签名ID 签名ID提供器
     *
     * @param maxSignID 最大签名ID字符串
     */
    public AtomicSignID(String maxSignID) {
        int maxSignIDNum = parse(maxSignID);
        provider = new AtomicInteger(maxSignIDNum);
    }


    /**
     * 增长并获取签名ID
     *
     * @return 签名ID，形如：'s001'
     */
    public String incrementAndGet() {
        int newSignID = provider.incrementAndGet();
        return String.format("s%03d", newSignID);
    }

    /**
     * 解析出电子签名的ID数字
     *
     * @param id ID字符串
     * @return ID数字
     */
    public static int parse(String id) {
        Matcher m = AtomicSignID.IDPattern.matcher(id);
        if (m.find()) {
            String idNumStr = m.group(1);
            return Integer.parseInt(idNumStr);
        } else {
            return Integer.parseInt(id);
        }
    }

}
