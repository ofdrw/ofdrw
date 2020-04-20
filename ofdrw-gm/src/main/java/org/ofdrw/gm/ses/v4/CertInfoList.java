package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * 签章者证书列表
 *
 * @author 权观宇
 * @since 2020-04-19 17:11:49
 */
public class CertInfoList extends ASN1Object
        implements org.bouncycastle.util.Iterable<ASN1OctetString> {

    private final List<ASN1OctetString> dataSequence;

    public CertInfoList() {
        this.dataSequence = new ArrayList<>();
    }

    public CertInfoList(ASN1OctetString[] arr) {
        this();
        dataSequence.addAll(Arrays.asList(arr));
    }

    public CertInfoList(ASN1Sequence seq) {
        dataSequence = new ArrayList<>(seq.size());
        for (int i = 0; i != seq.size(); i++) {
            add(DEROctetString.getInstance(seq.getObjectAt(i)));
        }
    }

    public static CertInfoList getInstance(Object obj) {
        if (obj instanceof CertInfoList) {
            return (CertInfoList) obj;
        } else if (obj != null) {
            return new CertInfoList(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    public CertInfoList add(ASN1OctetString obj) {
        dataSequence.add(obj);
        return this;
    }

    public ASN1OctetString get(int index) {
        return dataSequence.get(index);
    }


    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(dataSequence.size());
        dataSequence.forEach(v::add);
        return new DERSequence(v);
    }

    public Iterator<ASN1OctetString> iterator() {
        return dataSequence.iterator();
    }
}
