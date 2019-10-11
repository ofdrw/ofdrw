package org.ofdrw.core.basicStructure.doc.permission;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

/**
 * 打印权限
 * <p>
 * 具体的权限和份数设置由其属性 Printable 及 Copics 控制。若不设置 Print节点，
 * 则默认可以打印，并且打印份数不受限制
 * <p>
 * 7.5 图 9 文档权限声明结构
 *
 * @author 权观宇
 * @since 2019-10-07 05:07:02
 */
public class Print extends OFDElement {
    public Print(Element proxy) {
        super(proxy);
    }

    public Print() {
        super("Print");
    }

    public Print(boolean printable,int copies) {
        this();
        this.setPrintable(printable)
                .setCopies(copies);
    }


    /**
     * 【可选 必选】
     * 设置 是否允许被打印
     * <p>
     * 默认值为 true
     *
     * @param printable true - 允许被打印； false - 不允许被打印
     * @return this
     */
    public Print setPrintable(boolean printable) {
        this.addAttribute("Printable", Boolean.toString(printable));
        return this;
    }

    /**
     * 【可选 必选】
     * 获取 是否允许被打印
     * <p>
     * 默认值为 true
     *
     * @return true - 允许被打印； false - 不允许被打印
     */
    public Boolean getPrintable() {
        String str = this.getOFDElementText("Printable");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选 属性】
     * 设置 打印份数
     * <p>
     * 在 Printable 为 true 时有效，若 Printable 为 true
     * 并且不设置 Copies 则打印份数不受限，若 Copies 的值为负值时，
     * 打印份数不受限，当 Copies 的值为 0 时，不允许打印，当 Copies的值
     * 大于 0 时，则代表实际可打印的份数值。
     * <p>
     * 默认值为 -1
     *
     * @param copies 可打印的份数
     * @return this
     */
    public Print setCopies(int copies) {
        this.addAttribute("Copies", Integer.toString(copies));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 打印份数
     * <p>
     * 在 Printable 为 true 时有效，若 Printable 为 true
     * 并且不设置 Copies 则打印份数不受限，若 Copies 的值为负值时，
     * 打印份数不受限，当 Copies 的值为 0 时，不允许打印，当 Copies的值
     * 大于 0 时，则代表实际可打印的份数值。
     * <p>
     * 默认值为 -1
     *
     * @return 可打印的份数
     */
    public Integer getCopies() {
        String str = this.attributeValue("Copies");
        if (str == null || str.trim().length() == 0) {
            return -1;
        }
        return Integer.parseInt(str);
    }
}
