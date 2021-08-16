package org.ofdrw.gm.sm2strut;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

import java.util.Enumeration;

/**
 * GMT 35275-2017 signerInfo 类型
 *
 * @author 权观宇
 * @since 2021-08-02 20:10:24
 */
public class SignerInfo extends ASN1Object {
    public static final ASN1Integer VERSION_1 = new ASN1Integer(1);

    /*
        SignerInfo ::= SEQUENCE {
            version Version,
            issuerAngSerialNumber IssuerAndSerialNumber,
            digestAlgorithm DigestAlgorithmIdentifier,
            authenticatedAttributes[0] IMPLICIT Attributes OPTIONAL,
            digestEncryptionAlgorithm DigestEncryptionAlgorithmIdentifier,
            encryptedDigest EncryptedDigest,
            unauthenticatedAttributes[1] IMPLICIT Attributes OPTIONAL
        }

        EncryptedDigest ::= OCTET STRING
     */
    /**
     * 版本号，此处取值为1 {@link #VERSION_1}
     */
    private ASN1Integer version = VERSION_1;

    /**
     * 一个证书颁发者可识别名和颁发者确定的证书序列号，可据此
     * 确定一份证书和与此证书对应的实体及公钥。
     */
    private IssuerAndSerialNumber issuerAngSerialNumber;

    /**
     * 对内容进行摘要计算的消息摘要算法，本标准采用SM3算法 {@link OIDs#sm3}
     */
    private AlgorithmIdentifier digestAlgorithm;

    /**
     * 是经由签名者签名的属性的集合，该域可选。如果该域存在，
     * 该域中摘要的计算方法是对原文进行摘要计算结果。
     */
    private ASN1Set authenticatedAttributes;

    /**
     * SM2 椭圆曲线数字签名算法标识符 {@link OIDs#sm2Sign}
     */
    private AlgorithmIdentifier digestEncryptionAlgorithm;

    /**
     * 值是 SM2Signature，用签名者私钥进行签名，
     * 其定义见 GBT 35276-2017 7.3 签名数据格式
     */
    private ASN1OctetString encryptedDigest;


    private ASN1Set unauthenticatedAttributes;


    public SignerInfo(IssuerAndSerialNumber issuerAngSerialNumber, AlgorithmIdentifier digestAlgorithm,
                      AlgorithmIdentifier digestEncryptionAlgorithm, ASN1OctetString encryptedDigest) {
        this.version = VERSION_1;
        this.issuerAngSerialNumber = issuerAngSerialNumber;
        this.digestAlgorithm = digestAlgorithm;
        this.digestEncryptionAlgorithm = digestEncryptionAlgorithm;
        this.encryptedDigest = encryptedDigest;
    }

    public SignerInfo(ASN1Sequence seq) {
        Enumeration<?> e = seq.getObjects();
        this.version = ASN1Integer.getInstance(e.nextElement());
        this.issuerAngSerialNumber = IssuerAndSerialNumber.getInstance(e.nextElement());
        this.digestAlgorithm = AlgorithmIdentifier.getInstance(e.nextElement());
        Object obj = e.nextElement();
        if (obj instanceof ASN1TaggedObject) {
            this.authenticatedAttributes = ASN1Set.getInstance((ASN1TaggedObject) obj, false);
            this.digestEncryptionAlgorithm = AlgorithmIdentifier.getInstance(e.nextElement());
        } else {
            this.authenticatedAttributes = null;
            this.digestEncryptionAlgorithm = AlgorithmIdentifier.getInstance(obj);
        }
        this.encryptedDigest = ASN1OctetString.getInstance(e.nextElement());
        if (e.hasMoreElements()) {
            this.unauthenticatedAttributes = ASN1Set.getInstance((ASN1TaggedObject) e.nextElement(), false);
        }
    }

    public static SignerInfo getInstance(Object o) {
        if (o instanceof SignerInfo) {
            return (SignerInfo) o;
        } else if (o != null) {
            return new SignerInfo(ASN1Sequence.getInstance(o));
        } else {
            return null;
        }
    }

    public ASN1Integer getVersion() {
        return version;
    }

    public SignerInfo setVersion(ASN1Integer version) {
        this.version = version;
        return this;
    }

    public IssuerAndSerialNumber getIssuerAngSerialNumber() {
        return issuerAngSerialNumber;
    }

    public SignerInfo setIssuerAngSerialNumber(IssuerAndSerialNumber issuerAngSerialNumber) {
        this.issuerAngSerialNumber = issuerAngSerialNumber;
        return this;
    }

    public AlgorithmIdentifier getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public SignerInfo setDigestAlgorithm(AlgorithmIdentifier digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
        return this;
    }

    public ASN1Set getAuthenticatedAttributes() {
        return authenticatedAttributes;
    }

    public SignerInfo setAuthenticatedAttributes(ASN1Set authenticatedAttributes) {
        this.authenticatedAttributes = authenticatedAttributes;
        return this;
    }

    public AlgorithmIdentifier getDigestEncryptionAlgorithm() {
        return digestEncryptionAlgorithm;
    }

    public SignerInfo setDigestEncryptionAlgorithm(AlgorithmIdentifier digestEncryptionAlgorithm) {
        this.digestEncryptionAlgorithm = digestEncryptionAlgorithm;
        return this;
    }

    public ASN1OctetString getEncryptedDigest() {
        return encryptedDigest;
    }

    public SignerInfo setEncryptedDigest(ASN1OctetString encryptedDigest) {
        this.encryptedDigest = encryptedDigest;
        return this;
    }

    public ASN1Set getUnauthenticatedAttributes() {
        return unauthenticatedAttributes;
    }

    public SignerInfo setUnauthenticatedAttributes(ASN1Set unauthenticatedAttributes) {
        this.unauthenticatedAttributes = unauthenticatedAttributes;
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(7);
        v.add(version);
        v.add(issuerAngSerialNumber);
        v.add(digestAlgorithm);
        if (authenticatedAttributes != null) {
            v.add(new DERTaggedObject(false, 0, authenticatedAttributes));
        }
        v.add(digestEncryptionAlgorithm);
        v.add(encryptedDigest);
        if (unauthenticatedAttributes != null) {
            v.add(new DERTaggedObject(false, 1, unauthenticatedAttributes));
        }
        return new DERSequence(v);
    }
}
