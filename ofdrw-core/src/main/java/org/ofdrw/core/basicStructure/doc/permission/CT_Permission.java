package org.ofdrw.core.basicStructure.doc.permission;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.OFDElement;

/**
 * 本标准支持设置文档权限声明（Permission）节点，以达到文档防扩散等应用目的。
 * 文档权限声明结构如 图 9 所示。
 * <p>
 * 7.5 小节 CT_Permission
 *
 * @author 权观宇
 * @since 2019-10-06 08:09:21
 */
public class CT_Permission extends OFDElement {
    public CT_Permission(Element proxy) {
        super(proxy);
    }

    public CT_Permission() {
        super("Permission");
    }

    /**
     * 【可选】
     * 设置 是否允许编辑
     * <p>
     * 默认值为 true
     *
     * @param edit true - 允许编辑； false - 不允许编辑
     * @return this
     */
    public CT_Permission setEdit(boolean edit) {
        this.addOFDEntity("Edit", Boolean.toString(edit));
        return this;
    }

    /**
     * 【可选】
     * 获取 是否允许编辑
     * <p>
     * 默认值为 true
     *
     * @return true - 允许编辑； false - 不允许编辑
     */
    public Boolean getEdit() {
        String str = this.getOFDElementText("Edit");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选】
     * 设置 是否允许添加或修改标注
     * <p>
     * 默认值为 true
     *
     * @param annot true - 允许添加或修改标注； false - 不允许添加或修改标注
     * @return this
     */
    public CT_Permission setAnnot(boolean annot) {
        this.addOFDEntity("Annot", Boolean.toString(annot));
        return this;
    }

    /**
     * 【可选】
     * 获取 是否允许添加或修改标注
     * <p>
     * 默认值为 true
     *
     * @return true - 允许添加或修改标注； false - 不允许添加或修改标注
     */
    public Boolean getAnnot() {
        String str = this.getOFDElementText("Annot");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }


    /**
     * 【可选】
     * 设置 是否允许导出
     * <p>
     * 默认值为 true
     *
     * @param export true - 允许导出； false - 不允许导出
     * @return this
     */
    public CT_Permission setExport(boolean export) {
        this.addOFDEntity("Export", Boolean.toString(export));
        return this;
    }

    /**
     * 【可选】
     * 获取 是否允许导出
     * <p>
     * 默认值为 true
     *
     * @return true - 允许导出； false - 不允许导出
     */
    public Boolean getExport() {
        String str = this.getOFDElementText("Export");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选】
     * 设置 是否允许进行数字签名
     * <p>
     * 默认值为 true
     *
     * @param signature true - 允许进行数字签名； false - 不允许进行数字签名
     * @return this
     */
    public CT_Permission setSignature(boolean signature) {
        this.addOFDEntity("Signature", Boolean.toString(signature));
        return this;
    }

    /**
     * 【可选】
     * 获取 是否允许进行数字签名
     * <p>
     * 默认值为 true
     *
     * @return true - 允许进行数字签名； false - 不允许进行数字签名
     */
    public Boolean getSignature() {
        String str = this.getOFDElementText("Signature");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选】
     * 设置 是否允许添加水印
     * <p>
     * 默认值为 true
     *
     * @param watermark true - 允许添加水印； false - 不允许添加水印
     * @return this
     */
    public CT_Permission setWatermark(boolean watermark) {
        this.addOFDEntity("Watermark", Boolean.toString(watermark));
        return this;
    }

    /**
     * 【可选】
     * 获取 是否允许添加水印
     * <p>
     * 默认值为 true
     *
     * @return true - 允许添加水印； false - 不允许添加水印
     */
    public Boolean getWatermark() {
        String str = this.getOFDElementText("Watermark");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选】
     * 设置 是否允许截屏
     * <p>
     * 默认值为 true
     *
     * @param printScreen true - 允许截屏； false - 不允许截屏
     * @return this
     */
    public CT_Permission setPrintScreen(boolean printScreen) {
        this.addOFDEntity("PrintScreen", Boolean.toString(printScreen));
        return this;
    }

    /**
     * 【可选】
     * 获取 是否允许截屏
     * <p>
     * 默认值为 true
     *
     * @return true - 允许截屏； false - 不允许截屏
     */
    public Boolean getPrintScreen() {
        String str = this.getOFDElementText("PrintScreen");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选】
     * 设置 打印权限
     * <p>
     * 具体的权限和份数设置由其属性 Printable 及 Copics 控制。若不设置 Print节点，
     * 则默认可以打印，并且打印份数不受限制
     *
     * @param print 打印权限
     * @return this
     */
    public CT_Permission setPrint(Print print) {
        this.add(print);
        return this;
    }

    /**
     * 【可选】
     * 获取 打印权限
     * <p>
     * 具体的权限和份数设置由其属性 Printable 及 Copics 控制。若不设置 Print节点，
     * 则默认可以打印，并且打印份数不受限制
     *
     * @return 打印权限
     */
    public Print getPrint() {
        Element e = this.getOFDElement("Print");
        return e == null ? null : new Print(e);
    }

    /**
     * 【可选】
     * 设置 有效期
     * <p>
     * 该文档允许访问的期限，其具体期限取决于开始日期和
     * 结束日期，其中开始日期不能晚于结束日期，并且开始日期和结束
     * 日期至少出现一个。当不设置开始日期时，代表不限定开始日期，
     * 当不设置结束日期时代表不限定结束日期；当此不设置此节点时，
     * 表示开始和结束日期均不受限
     *
     * @param validPeriod 有效期
     * @return this
     */
    public CT_Permission setValidPeriod(ValidPeriod validPeriod) {
        this.add(validPeriod);
        return this;
    }

    /**
     * 【可选】
     * 获取 有效期
     * <p>
     * 该文档允许访问的期限，其具体期限取决于开始日期和
     * 结束日期，其中开始日期不能晚于结束日期，并且开始日期和结束
     * 日期至少出现一个。当不设置开始日期时，代表不限定开始日期，
     * 当不设置结束日期时代表不限定结束日期；当此不设置此节点时，
     * 表示开始和结束日期均不受限
     *
     * @return 有效期
     */
    public ValidPeriod getValidPeriod() {
        Element e = this.getOFDElement("ValidPeriod");
        return e == null ? null : new ValidPeriod(e);
    }
}
