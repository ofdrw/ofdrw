package org.ofdrw.core.crypto.encryt;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

/**
 * @author 权观宇
 * @since 2021-06-23 18:36:33
 */
public class CT_EncryptInfo extends OFDElement {
    public CT_EncryptInfo(Element proxy) {
        super(proxy);
    }

    public CT_EncryptInfo(String name) {
        super("EncryptInfo");
    }
}
