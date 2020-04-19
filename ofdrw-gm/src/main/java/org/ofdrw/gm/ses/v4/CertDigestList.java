package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * 签章者证书杂凑值列表
 *
 * @author 权观宇
 * @since 2020-04-19 16:29:35
 */
public class CertDigestList extends ASN1Object
        implements org.bouncycastle.util.Iterable<CertDigestObj> {
    /**
     * 签章者证书杂凑值
     */
    private List<CertDigestObj> dataSequence;

    public CertDigestList() {
        dataSequence = new ArrayList<>();
    }

    public CertDigestList(ASN1Sequence seq) {
        dataSequence = new ArrayList<>(seq.size());
        for (int i = 0; i != seq.size(); i++) {
            add(CertDigestObj.getInstance(seq.getObjectAt(i)));
        }
    }
    public static CertDigestList getInstance(Object obj) {
        if (obj instanceof CertDigestList) {
            return (CertDigestList) obj;
        } else if (obj != null) {
            return new CertDigestList(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    public CertDigestList(CertDigestObj[] arr) {
        this();
        dataSequence.addAll(Arrays.asList(arr));
    }


    public CertDigestList add(CertDigestObj obj) {
        dataSequence.add(obj);
        return this;
    }

    public CertDigestObj get(int index) {
        return dataSequence.get(index);
    }


    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(dataSequence.size());
        dataSequence.forEach(v::add);
        return new DERSequence(v);
    }

    public Iterator<CertDigestObj> iterator() {
        return dataSequence.iterator();
    }
}
