package org.ofdrw.sign.stamppos;

import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.sign.AtomicSignID;

import java.util.List;

/**
 * 签章外观位置提供者
 *
 * @author 权观宇
 * @since 2020-04-18 10:47:41
 */
public interface StampAppearance {

    /**
     * 获取签章外观
     *
     * @param ctx OFD虚拟容器
     * @param idProvider 签名ID提供器
     * @return 签章外观列表
     */
    List<StampAnnot> getAppearance(OFDDir ctx, AtomicSignID idProvider);
}
