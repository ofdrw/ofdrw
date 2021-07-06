package org.ofdrw.gm.support;

import java.security.MessageDigest;

/**
 * 密钥派生函数
 * <p>
 * GB/T 32918.3-2016 信息安全技术 SM2椭圆曲线公钥密码算法 第3部分：密钥交换协议
 * 5.4.3 密钥派生函数
 * <p>
 * 密钥派生函数的作用是从一个共享的秘密比特串中派生出密钥数据。在密钥协商过程中，
 * 密钥派生函数作用在密钥交换所获共享的秘密比特串上，从中产生所需要的会话密钥或
 * 进一步加密所需的密钥数据。
 *
 * @author 权观宇
 * @since 2021-06-29 18:48:41
 */
public final class KDF {

    /**
     * 密钥派生函数
     *
     * @param md   密码杂凑算法 Hv
     * @param z    输入比特串Z
     * @param klen 预期密钥长度
     * @return 派生密钥
     */
    public static byte[] extend(MessageDigest md, byte[] z, int klen) {
        int v = md.getDigestLength();
        byte[] K = new byte[klen];
        // a) 初始化一个32比特构成的计数器 ct = 0x00000001
        int ct = 0x00000001;
        // b) 对 i 从 1 到 ⌈klen/v⌉ 执行：
        byte[] hai = null;
        // 向上取整 ⌈klen/v⌉
        int len = klen / v + (klen % v == 0 ? 0 : 1);
        for (int i = 0; i < len; i++) {
            // 1) 计算 Hai = Hv(Z || ct);
            md.reset();
            md.update(z);
            md.update(new byte[]{
                    (byte) (ct >>> 24),
                    (byte) (ct >>> 16),
                    (byte) (ct >>> 8),
                    (byte) ct});
            hai = md.digest();
            // 2) ct++
            ct++;
            // c) 若 ⌈klen/v⌉  是整数，令 Ha!⌈klen/v⌉  =  Ha⌈klen/v⌉
            //    否则，令 Ha!⌈klen/v⌉  为 Ha⌈klen/v⌉ 最左边的(klen-(v*⌊klen/v⌋))比特
            if (i + 1 == len && klen % v != 0) {
                System.arraycopy(hai, 0, K, v * (klen / v), klen - (v * (klen / v)));
            } else {
                System.arraycopy(hai, 0, K, v * i, v);
            }
            // d) 令 K = Ha1 || Ha2 || ... || Ha⌈klen/v⌉-1 || Ha!⌈klen/v⌉
        }

        return K;
    }
}
