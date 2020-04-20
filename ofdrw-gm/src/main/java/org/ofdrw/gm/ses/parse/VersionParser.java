package org.ofdrw.gm.ses.parse;

import org.bouncycastle.asn1.ASN1Sequence;

/**
 * 版本解析
 *
 * @author 权观宇
 * @since 2020-04-20 09:15:52
 */
public class VersionParser {

    /**
     * 解析电子印章版本
     *
     * @param o 带解析数据，可以是字节串也可以是ASN1对象
     * @return 带有版本的ASN1对象序列
     */
    public static SESVersionHolder parseSES_SealVersion(Object o) {
        ASN1Sequence seq = ASN1Sequence.getInstance(o);
        SESVersion version;
        if (seq.size() == 4) {
            /*
             * GB/T 38540-2020 信息安全技术 安全电子签章密码技术规范 电子印章数据
             *
             * - 印章信息
             * - 制章者证书
             * - 签名算法标识符
             * - 签名值
             */
            version = SESVersion.v4;
        } else if (seq.size() == 2) {
            /*
             * GM/T 0031-2014 安全电子签章密码技术规范 电子印章数据
             *
             * - 印章信息
             * - 制章人对印章签名的信息
             */
            version = SESVersion.v1;
        } else {
            throw new IllegalArgumentException("未知的数据结构，无法匹配任何已知版本电子印章。");
        }
        return new SESVersionHolder(version, seq);
    }


    /**
     * 解析电子签章数据版本
     *
     * @param o 带解析数据，可以是字节串也可以是ASN1对象
     * @return 带有版本的ASN1对象序列
     */
    public static SESVersionHolder parseSES_SignatureVersion(Object o) {
        ASN1Sequence seq = ASN1Sequence.getInstance(o);
        SESVersion version;
        if (seq.size() >= 4 && seq.size() <= 5) {
            /*
             * GB/T 38540-2020 信息安全技术 安全电子签章密码技术规范
             *
             * - 签章信息
             * - 制章者证书
             * - 签名算法标识符
             * - 签名值
             * - [0] 对签名值的时间戳 【可选】
             */
            version = SESVersion.v4;
        } else if (seq.size() == 2) {
            /*
             * GM/T 0031-2014 安全电子签章密码技术规范 电子签章数据
             *
             * - 待电子签章数据
             * - 电子签章中签名值
             */
            version = SESVersion.v1;
        } else {
            throw new IllegalArgumentException("未知的数据结构，无法匹配任何已知版本电子签章数据。");
        }
        return new SESVersionHolder(version, seq);
    }

}
