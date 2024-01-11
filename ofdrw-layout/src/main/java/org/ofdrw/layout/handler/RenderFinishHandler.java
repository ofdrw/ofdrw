package org.ofdrw.layout.handler;

import org.ofdrw.pkg.container.OFDDir;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 在文件渲染结束时触发的回调函数
 *
 * @author 权观宇
 * @since 2021-09-14 21:10:18
 */
@FunctionalInterface
public interface RenderFinishHandler {
    /**
     * 文件渲染结束时触发的回调函数
     * @param maxUnitID 最大的对象ID
     * @param ofdDir OFD虚拟容器
     * @param index 文件索引
     */
    void handle(AtomicInteger maxUnitID, OFDDir ofdDir, int index);
}
