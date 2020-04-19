package org.ofdrw.gm.ses.v1;

import org.bouncycastle.asn1.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * 自定义属性字段序列
 *
 * @author 权观宇
 * @since 2020-04-19 16:34:20
 */
public class ExtensionDatas extends ASN1Object
        implements org.bouncycastle.util.Iterable<ExtData> {

    private final List<ExtData> dataSequence;


    public ExtensionDatas() {
        dataSequence = new ArrayList<>();
    }

    public ExtensionDatas(ExtData[] arr) {
        this();
        this.dataSequence.addAll(Arrays.asList(arr));
    }

    public ExtensionDatas add(ExtData o) {
        this.dataSequence.add(o);
        return this;
    }

    public ExtData get(int index) {
        return this.dataSequence.get(index);
    }

    public ExtensionDatas(ASN1Sequence seq) {
        dataSequence = new ArrayList<>(seq.size());
        for (int i = 0; i != seq.size(); i++) {
            add(ExtData.getInstance(seq.getObjectAt(i)));
        }
    }

    public static ExtensionDatas getInstance(Object obj) {
        if (obj instanceof ExtensionDatas) {
            return (ExtensionDatas) obj;
        } else if (obj != null) {
            return new ExtensionDatas(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(dataSequence.size());
        dataSequence.forEach(v::add);
        return new DERSequence(v);
    }

    public Iterator<ExtData> iterator() {
        return dataSequence.iterator();
    }
}
