package org.ofdrw.gm.sm2strut;

import org.bouncycastle.asn1.*;


/**
 * GMT 35275-2017 6.10 ContentInfo
 * <p>
 * ContentInfo 类型表明内容交换通用语法结构，内容交换的通用语法结构定义。
 *
 * @author 权观宇
 * @since 2021-08-02 19:13:03
 */
public final class ContentInfo extends ASN1Object {
    /*
      ContentInfo ::= SEQUENCE {
          contentType ContentType,
          content [0] EXPLICIT ANY DEFINED BY contentType OPTIONAL
      }

      ContentType ::= OBJECT IDENTIFIER
      ContentType 内容类型是一个对象标识符，定义见第5章。
      content 内容，可选。
     */

    /**
     * 对象标识符
     * <p>
     * 见 {@link OIDs}
     */
    private ASN1ObjectIdentifier contentType;
    /**
     * 交换内容，可选
     */
    private ASN1Encodable content;

    public ContentInfo(ASN1ObjectIdentifier contentType, ASN1Encodable content) {
        this.contentType = contentType;
        this.content = content;
    }

    public static ContentInfo getInstance(Object obj) {
        if (obj instanceof ContentInfo) {
            return (ContentInfo) obj;
        } else if (obj != null) {
            return new ContentInfo(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    public ContentInfo(ASN1Sequence seq) {
        contentType = (ASN1ObjectIdentifier) seq.getObjectAt(0);
        if (seq.size() > 1) {
            ASN1TaggedObject tagged = (ASN1TaggedObject) seq.getObjectAt(1);
            if (!tagged.isExplicit() || tagged.getTagNo() != 0) {
                throw new IllegalArgumentException("content 无法解析");
            }
            content = tagged.getObject();
        }
    }

    public ASN1ObjectIdentifier getContentType() {
        return contentType;
    }

    public ContentInfo setContentType(ASN1ObjectIdentifier contentType) {
        this.contentType = contentType;
        return this;
    }

    public ASN1Encodable getContent() {
        return content;
    }

    public ContentInfo setContent(ASN1Encodable content) {
        this.content = content;
        return this;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
       ASN1EncodableVector v = new ASN1EncodableVector(2);
       v.add(contentType);
        if (content != null) {
            v.add(new DERTaggedObject(0, content));
        }
        return new DERSequence(v);
    }
}
