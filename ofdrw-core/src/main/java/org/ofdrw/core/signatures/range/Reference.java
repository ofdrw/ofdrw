package org.ofdrw.core.signatures.range;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Loc;

import java.util.Base64;

/**
 * 针对一个文件的摘要节点
 * <p>
 * 18.2.2 签名的范围 图 87 表 68
 *
 * @author 权观宇
 * @since 2019-11-21 18:01:53
 */
public class Reference extends OFDElement {
    public Reference(Element proxy) {
        super(proxy);
    }

    public Reference() {
        super("Reference");
    }

    public Reference(ST_Loc fileRef, byte[] checkValue) {
        this();
        this.setFileRef(fileRef)
                .setCheckValue(checkValue);
    }


    /**
     * 【必选 属性】
     * 设置 指向包内的文件，使用绝对路径
     *
     * @param fileRef 指向包内的文件，使用绝对路径
     * @return this
     */
    public Reference setFileRef(ST_Loc fileRef) {
        if (fileRef == null) {
            throw new IllegalArgumentException("指向包内的文件（FileRef）为空");
        }
        this.addAttribute("FileRef", fileRef.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 指向包内的文件，使用绝对路径
     *
     * @return 指向包内的文件，使用绝对路径
     */
    public ST_Loc getFileRef() {
        return ST_Loc.getInstance(this.attributeValue("FileRef"));
    }


    /**
     * 【必选】
     * 设置 对包内文件进行摘要计算值的杂凑值
     * <p>
     * 所得的二进制摘要值进行 base64 编码
     *
     * @param checkValue 对包内文件进行摘要计算值的杂凑值
     * @return this
     */
    public Reference setCheckValue(byte[] checkValue) {
        if (checkValue == null || checkValue.length == 0) {
            throw new IllegalArgumentException("摘要计算值（CheckValue）为空");
        }

        this.setOFDEntity("CheckValue", Base64.getEncoder().encodeToString(checkValue));
        return this;
    }

    /**
     * 【必选】
     * 获取 对包内文件进行摘要计算值的杂凑值
     *
     * @return 对包内文件进行摘要计算值的杂凑值
     */
    public byte[] getCheckValue() {
        Element e = this.getOFDElement("CheckValue");
        if (e == null) {
            throw new IllegalArgumentException("摘要计算值（CheckValue）为空");
        }
        return Base64.getDecoder().decode(e.getTextTrim());
    }


}
