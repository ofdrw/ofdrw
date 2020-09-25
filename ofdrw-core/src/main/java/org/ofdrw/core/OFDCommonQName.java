package org.ofdrw.core;

import org.dom4j.QName;

/**
 * OFD通用  qualified name
 * <p>
 * 只要名称相同并且命名空间前缀保持一致就认为是同一种  qualified name
 *
 * @author 权观宇
 * @since 2020-09-15 21:14:27
 */
public class OFDCommonQName extends QName {
    // 通用的OFD命名空间前缀
    public static final String CommonOFDNameSpacePrefix = "http://www.ofdspec.org";

    /**
     * @param name OFD元素名称
     */
    public OFDCommonQName(String name) {
        super(name, Const.OFD_NAMESPACE);
    }

    /**
     * Name相同并且，只要符合命名空间前缀相同那么
     * 那么认定为是相等的qualified name
     *
     * @param object 比较对象
     * @return true 相同；false 不同
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object instanceof QName) {
            QName that = (QName) object;
            return getName().equals(that.getName())
                    && that.getNamespaceURI().startsWith(CommonOFDNameSpacePrefix);
        }
        return false;
    }
}
