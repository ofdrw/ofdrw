package org.ofdrw.gm.sm2strut;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.Certificate;

import java.util.Enumeration;

/**
 * GMT 35275-2017 8.1 signedData 类型
 * <p>
 * signedData 数据类型由任意类型的数据和至少一个签名者的签名值组成。
 * 任意类型的数据能够同时被任意数量的签名者签名。
 *
 * @author 权观宇
 * @since 2021-08-02 19:02:57
 */
public final class SignedData extends ASN1Object {

    public static final ASN1Integer VERSION_1 = new ASN1Integer(1);

    /*
    SignedData ::= SEQUENCE {
        version Version,
        digestAlgorithms DigestAlgorithmIdentifiers,
        contentInfo ContentInfo,
        certificates[0] IMPLICIT ExtendedCertificatesAndCertificates OPTIONAL,
        crls[1] IMPLICIT CertificateRevocationLists OPTIONAL,
        signerInfos SignerInfos
    }

    DigestAlgorithmIdentifiers ::= SET OF DigestAlgorithmIdentifier
    SignerInfos ::= SET OF SignerInfo
     */

    /**
     * 版本号，此处取值为 1
     * 见 {@link #VERSION_1}
     */
    private ASN1Integer version = VERSION_1;

    /**
     * 消息摘要算法标识符的集合
     */
    private ASN1Set digestAlgorithms;

    /**
     * 数据内容
     */
    private ContentInfo contentInfo;

    /**
     * PKCS#6 扩展证书和 X.509证书的集合
     */
    private ASN1Set certificates;

    /**
     * 证书撤销列表的集合
     */
    private ASN1Set crls;

    /**
     * 每个签名者信息的集合
     */
    private ASN1Set signerInfos;

    public SignedData(ASN1Set digestAlgorithms, ContentInfo contentInfo, ASN1Set certificates, ASN1Set signerInfos) {
        this.digestAlgorithms = digestAlgorithms;
        this.contentInfo = contentInfo;
        this.certificates = certificates;
        this.signerInfos = signerInfos;
    }

    public SignedData(ASN1Sequence seq) {
        final Enumeration<?> e = seq.getObjects();
        version = ASN1Integer.getInstance(e.nextElement());
        digestAlgorithms = ((ASN1Set) e.nextElement());
        contentInfo = ContentInfo.getInstance(e.nextElement());
        while (e.hasMoreElements()) {
            ASN1Primitive o = (ASN1Primitive) e.nextElement();
            if (o instanceof ASN1TaggedObject) {
                ASN1TaggedObject tagged = (ASN1TaggedObject) o;
                switch (tagged.getTagNo()) {
                    case 0:
                        certificates = ASN1Set.getInstance(tagged, false);
                        break;
                    case 1:
                        crls = ASN1Set.getInstance(tagged, false);
                        break;
                    default:
                        throw new IllegalArgumentException("无法接 标签参数 " + tagged.getTagNo());
                }
            } else {
                signerInfos = (ASN1Set) o;
            }
        }
    }

    public static SignedData getInstance(Object o) {
        if (o instanceof SignedData) {
            return (SignedData) o;
        } else if (o != null) {
            return new SignedData(ASN1Sequence.getInstance(o));
        }
        return null;
    }

    public ASN1Integer getVersion() {
        return version;
    }

    public SignedData setVersion(ASN1Integer version) {
        this.version = version;
        return this;
    }

    public ASN1Set getDigestAlgorithms() {
        return digestAlgorithms;
    }

    public SignedData setDigestAlgorithms(ASN1Set digestAlgorithms) {
        this.digestAlgorithms = digestAlgorithms;
        return this;
    }

    public ContentInfo getContentInfo() {
        return contentInfo;
    }

    public SignedData setContentInfo(ContentInfo contentInfo) {
        this.contentInfo = contentInfo;
        return this;
    }

    public ASN1Set getCertificates() {
        return certificates;
    }

    public SignedData setCertificates(ASN1Set certificates) {
        this.certificates = certificates;
        return this;
    }

    public ASN1Set getCrls() {
        return crls;
    }

    public SignedData setCrls(ASN1Set crls) {
        this.crls = crls;
        return this;
    }

    public ASN1Set getSignerInfos() {
        return signerInfos;
    }

    public SignedData setSignerInfos(ASN1Set signerInfos) {
        this.signerInfos = signerInfos;
        return this;
    }

    /**
     * 获取签名证书
     *
     * @param iaSn 证书颁发者DN和序列号
     * @return 证书实体，不存在返回null
     */
    public Certificate getSignCert(IssuerAndSerialNumber iaSn) {
        if (iaSn == null) {
            return null;
        }
        for (ASN1Encodable item : this.certificates) {
            final Certificate c = Certificate.getInstance(item);
            boolean snEq = c.getSerialNumber().equals(iaSn.getCertSerialNumber());
            boolean dnEq = c.getIssuer().equals(iaSn.getName());
            if (snEq && dnEq) {
                return c;
            }
        }
        return null;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(6);
        v.add(version);
        v.add(digestAlgorithms);
        v.add(contentInfo);
        if (certificates != null) {
            v.add(new DERTaggedObject(false, 0, certificates));
        }
        if (crls != null) {
            v.add(new DERTaggedObject(false, 1, crls));
        }
        v.add(signerInfos);
        return new DERSequence(v);
    }
}
