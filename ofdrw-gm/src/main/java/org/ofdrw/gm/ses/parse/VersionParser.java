package org.ofdrw.gm.ses.parse;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;

/**
 * 版本解析
 *
 * @author 权观宇
 * @since 2020-04-20 09:15:52
 */
public class VersionParser {

    /**
     * 从印章序列中提取Header中的version字段
     *
     * @param seq 印章或签章顶层序列
     * @return version整数值，无法解析返回-1
     */
    private static int extractSealHeaderVersion(ASN1Sequence seq) {
        try {
            // eSealInfo -> SEQUENCE, 第一个元素是 header -> SEQUENCE, 第二个元素是 version
            ASN1Sequence sealInfo = ASN1Sequence.getInstance(seq.getObjectAt(0));
            ASN1Sequence header = ASN1Sequence.getInstance(sealInfo.getObjectAt(0));
            ASN1Integer version = ASN1Integer.getInstance(header.getObjectAt(1));
            return version.getValue().intValue();
        } catch (Exception e) {
            throw new IllegalArgumentException("未知的数据结构，无法匹配任何已知版本电子印章。");
        }
    }

    /**
     * 解析电子印章版本
     *
     * @param o 带解析数据，可以是字节串也可以是ASN1对象
     * @return 带有版本的ASN1对象序列
     */
    public static SESVersionHolder parseSES_SealVersion(Object o) {
        ASN1Sequence seq = ASN1Sequence.getInstance(o);
        SESVersion version;
        if (seq.size() == 5) {
            /*
             * GM/T 0031-2025 安全电子签章密码技术规范
             *
             * - 印章信息
             * - 制章者证书
             * - 签名算法标识符
             * - 签名值
             * - 时间戳 【可选】
             */
            version = SESVersion.v5;
        } else if (seq.size() == 4) {
            // 可能是V4（固定4字段）或V5（无可选timeStamp）
            int headerVer = extractSealHeaderVersion(seq);
            if (headerVer == 5) {
                version = SESVersion.v5;
            } else {
                version = SESVersion.v4;
            }
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
            // V4和V5签章结构都是4-5字段，通过内部TBS_Sign的eseal header version区分
            try {
                ASN1Sequence tbsSign = ASN1Sequence.getInstance(seq.getObjectAt(0));
                ASN1Sequence esealSeq = ASN1Sequence.getInstance(tbsSign.getObjectAt(1));
                int headerVer = extractSealHeaderVersion(esealSeq);
                if (headerVer == 5) {
                    /*
                     * GM/T 0031-2025 安全电子签章密码技术规范
                     *
                     * - 签章信息
                     * - 制章者证书
                     * - 签名算法标识符
                     * - 签名值
                     * - [0] 对签名值的时间戳 【可选】
                     */
                    version = SESVersion.v5;
                } else {
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
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("无法解析电子签章数据版本", e);
            }
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
