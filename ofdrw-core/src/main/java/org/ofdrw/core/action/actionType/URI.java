package org.ofdrw.core.action.actionType;

import org.dom4j.Element;

/**
 * URI 动作
 * <p>
 * 图 77 URI动作属性
 *
 * @author 权观宇
 * @since 2019-10-05 09:41:15
 */
public class URI extends ActionEntity {
    public URI(Element proxy) {
        super(proxy);
    }

    public URI() {
        super("URI");
    }

    public URI(String uri) {
        this();
        setURI(uri);
    }

    public URI(String uri, String base) {
        this();
        this.setURI(uri)
                .setBase(base);
    }

    /**
     * 【必选 属性】
     * 设置 目标URI的位置
     *
     * @param uri 目标URI的位置
     * @return this
     */
    public URI setURI(String uri) {
        this.addAttribute("URI", uri);
        return this;
    }

    /**
     * 【必选 属性】
     * 设置 目标URI的位置
     *
     * @return 目标URI的位置
     */
    public String getURI() {
        return this.attributeValue("URI");
    }

    /**
     * 【可选 属性】
     * 设置 Base URI，用于相对地址
     *
     * @param base Base URI，用于相对地址
     * @return this
     */
    public URI setBase(String base) {
        this.addAttribute("Base", base);
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 Base URI，用于相对地址
     *
     * @return Base URI，用于相对地址
     */
    public String getBase() {
        return this.attributeValue("Base");
    }
}
