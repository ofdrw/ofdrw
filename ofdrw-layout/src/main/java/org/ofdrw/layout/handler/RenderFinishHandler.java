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
    void handle(AtomicInteger maxUnitID, OFDDir ofdDir, int index);
}
