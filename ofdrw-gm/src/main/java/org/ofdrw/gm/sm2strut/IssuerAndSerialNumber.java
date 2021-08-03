package org.ofdrw.gm.sm2strut;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x500.X500Name;

/**
 * GMT 35275-2017 6.7 IssuerAndSerialNumber
 * <p>
 * IssuerAndSerialNumber 类型标明一个证书颁发者可识别名和颁发者确定的证书序列号，
 * 可据此确定一份证书和与此证书对应的实体及公钥信息。
 *
 * @author 权观宇
 * @since 2021-08-03 19:13:33
 */
public class IssuerAndSerialNumber extends ASN1Object {

    /*
        IssuerAndSerialNumber ::= SEQUENCE {
            issuer Name,
            serialNumber CertificateSerialNumber
        }
     */

    /**
     * 证书颁发者可识别名
     */
    private X500Name name;
    /**
     * 颁发者确定的证书序列号
     */
    private ASN1Integer certSerialNumber;

    public IssuerAndSerialNumber(X500Name name, ASN1Integer certSerialNumber) {
        this.name = name;
        this.certSerialNumber = certSerialNumber;
    }

    public IssuerAndSerialNumber(ASN1Sequence seq) {
        this.name = X500Name.getInstance(seq.getObjectAt(0));
        this.certSerialNumber = ASN1Integer.getInstance(seq.getObjectAt(1));
    }

    public static IssuerAndSerialNumber getInstance(Object o) {
        if (o instanceof IssuerAndSerialNumber) {
            return (IssuerAndSerialNumber) o;
        } else if (o != null) {
            return new IssuerAndSerialNumber(ASN1Sequence.getInstance(o));
        } else {
            return null;
        }
    }

    public X500Name getName() {
        return name;
    }

    public IssuerAndSerialNumber setName(X500Name name) {
        this.name = name;
        return this;
    }

    public ASN1Integer getCertSerialNumber() {
        return certSerialNumber;
    }

    public IssuerAndSerialNumber setCertSerialNumber(ASN1Integer certSerialNumber) {
        this.certSerialNumber = certSerialNumber;
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(2);
        v.add(name);
        v.add(certSerialNumber);
        return new DERSequence(v);
    }
}
