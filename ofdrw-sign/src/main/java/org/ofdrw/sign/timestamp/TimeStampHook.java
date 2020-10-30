package org.ofdrw.sign.timestamp;

/**
 * 时间戳Hook
 *
 * @author minghu-zhang
 * @since 2020-10-30 12:31:43
 */
@FunctionalInterface
public interface TimeStampHook {

    /**
     * 执行方法获取时间戳
     *
     * @param signature 签章签名值
     * @return 返回时间戳二进制结果
     */
    byte[] apply(byte[] signature);
}