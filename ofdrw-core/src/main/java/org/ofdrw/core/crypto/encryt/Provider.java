package org.ofdrw.core.crypto.encryt;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;

/**
 * 加密组件的相关信息
 *
 * GMT0099 C.3 表 C.2
 *
 * @author 权观宇
 * @since 2021-06-24 19:48:55
 */
public class Provider extends org.ofdrw.core.signatures.sig.Provider {
    public Provider(Element proxy) {
        super(proxy);
    }

    public Provider() {
        super();
    }

    /**
     * 【必选 属性】
     * 设置 加密组件名称
     *
     * @param name 加密组件名称
     * @return this
     */
    @Override
    public org.ofdrw.core.signatures.sig.Provider setProviderName(@NotNull String name) {
        if (name == null) {
            throw new IllegalArgumentException("加密组件名称（name）为空");
        }
        this.addAttribute("Name", name);
        return this;
    }


    /**
     * 【必选 属性】
     * 获取 加密组件名称
     *
     * @return 加密组件名称
     */
    @NotNull
    @Override
    public String getProviderName() {
        String str = this.attributeValue("Name");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("加密组件名称（Name）为空");
        }
        return str;
    }

}
