package org.ofdrw.core.crypto.encryt;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.core.OFDElement;

import java.util.List;

/**
 * 【OFD 2.0】
 * 解密入口文件
 * <p>
 * 解密入口文件采用XML形式标识。文件以Encryptions为根节点，可包含多个加密信息（EncryptInfo）节点。
 * <p>
 * 每个加密信息节点的内容包含两部分，一部分为加密概要信息，
 * 另一部分为密钥描述文件和密密文映射表的位置，如图 C.2
 * <p>
 * 解密输入文件的文件名固定为”Encryptions.xml“，应放置在OFD文件的根目录（与OFD.xml位置相同）
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


    /**
     * 【必选 OFD 2.0】
     * 增加 加密描述信息
     * <p>
     * 多重加密形成多个加密操作记录
     * <p>
     * GMT0099 附录 C.3
     *
     * @param encryptInfo 加密描述信息
     * @return this
     */
    public Encryptions addEncryptInfo(@NotNull CT_EncryptInfo encryptInfo) {
        if (encryptInfo == null) {
            return this;
        }
        this.add(encryptInfo);
        return this;
    }

    /**
     * 【必选 OFD 2.0】
     * 获取 加密描述信息 列表
     * <p>
     * GMT0099 附录 C.3
     *
     * @return 加密描述信息 列表
     */
    @NotNull
    public List<CT_EncryptInfo> getEncryptInfos() {
        return this.getOFDElements("EncryptInfo", CT_EncryptInfo::new);
    }

    /**
     * 返回当前容器内最大的加密操作标识数字
     * <p>
     * 没有加密标识或无法解析成数字，则返回0
     *
     * @return 0
     */
    public int maxID() {
        final List<CT_EncryptInfo> encryptInfos = this.getEncryptInfos();
        int res = 0;
        for (CT_EncryptInfo encryptInfo : encryptInfos) {
            String id = encryptInfo.getID();
            try {
                int i = Integer.parseInt(id);
                if (i > res) {
                    res = i;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return res;
    }
}
