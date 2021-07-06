package org.ofdrw.core.signatures;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * 数字签名或安全签章在类表中的注册信息，依次签名或签章对应一个节点
 * <p>
 * 18.1 签名列表 图 85 表 66
 *
 * @author 权观宇
 * @since 2019-11-20 06:53:11
 */
public class Signature extends OFDElement {
    public Signature(Element proxy) {
        super(proxy);
    }

    public Signature() {
        super("Signature");
    }

    /**
     * 【必选 属性】
     * 设置 签名或签章的标识
     * <p>
     * 推荐使用“sNNN”的编码方式，NNN从1开始
     *
     * @param id 签名或签章的标识
     * @return this
     */
    public Signature setID(String id) {
        if (id == null || id.trim().length() == 0) {
            throw new IllegalArgumentException("签名或签章的标识（ID）为空");
        }
        this.addAttribute("ID", id);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 签名或签章的标识
     * <p>
     * 推荐使用“sNNN”的编码方式，NNN从1开始
     *
     * @return 签名或签章的标识
     */
    public String getID() {
        String str = this.attributeValue("ID");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("签名或签章的标识（ID）为空");
        }
        return str;
    }

    /**
     * 【可选 属性】
     * 设置  签名节点的类型
     * <p>
     * 可选值参考{@link SigType}
     * <p>
     * 默认值为Seal
     *
     * @param type 签名节点的类型
     * @return this
     */
    public Signature setType(SigType type) {
        if (type == null) {
            this.removeAttr("Type");
            return this;
        }
        this.addAttribute("Type", type.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取  签名节点的类型
     * <p>
     * 可选值参考{@link SigType}
     * <p>
     * 默认值为Seal
     *
     * @return 签名节点的类型
     */
    public SigType getType() {
        return SigType.getInstance(this.attributeValue("Type"));
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 设置 基于的签名ID
     * <p>
     * 此签名基于签名标识，一旦签名标注了该属性，则验证时应同时验证“基”签名。
     *
     * @param id 基于的签名ID，如果为null表示需要删除该属性。
     * @return this
     */
    public Signature setRelative(String id) {
        if (id == null) {
            this.removeAttr("Relative");
            return this;
        }
        this.addAttribute("Relative", id);
        return this;
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 获取 基于的签名ID
     * <p>
     * 此签名基于签名标识，一旦签名标注了该属性，则验证时应同时验证“基”签名。
     *
     * @return 基于的签名ID，可能为空
     */
    public String getRelative() {
        return this.attributeValue("Relative");
    }

    /**
     * 【必选 属性】
     * 设置 指向包内的签名描述文件
     *
     * @param baseLoc 指向包内的签名描述文件
     * @return this
     */
    public Signature setBaseLoc(ST_Loc baseLoc) {
        if (baseLoc == null) {
            throw new IllegalArgumentException("指向包内的签名描述文件（BaseLoc）为空");
        }
        this.addAttribute("BaseLoc", baseLoc.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 指向包内的签名描述文件
     *
     * @return 指向包内的签名描述文件
     */
    public ST_Loc getBaseLoc() {
        String str = this.attributeValue("BaseLoc");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("指向包内的签名描述文件（BaseLoc）为空");
        }
        return ST_Loc.getInstance(str);
    }

}
