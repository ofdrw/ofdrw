package org.ofdrw.core.crypto.encryt;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

/**
 * 【OFD 2.0】
 * 解密入口文件
 * <p>
 * 可包含多个加密信息（EncryptInfo）节点。
 * <p>
 * 每个加密信息节点的内容包含两部分，一部分为加密概要信息，
 * 另一部分为密钥描述文件和密密文映射表的位置，如图C.1
 * <p>
 * GMT0099-2020
 *
 * @author 权观宇
 * @since 2021-06-23 18:31:35
 */
public class Encryptions extends OFDElement {
    public Encryptions(Element proxy) {
        super(proxy);
    }

    public Encryptions() {
        super("Encryptions");
    }
    //todo
}
