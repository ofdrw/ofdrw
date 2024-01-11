package org.ofdrw.layout.handler;

import org.ofdrw.layout.VirtualPage;

/**
 * 虚拟页面处理器
 * <p>
 * 可以通过该处理器，在生成OFD页面Content.xml前对虚拟页面进行处理，例如页头、页脚等。
 *
 * @author 权观宇
 * @since 2024-1-11 18:43:55
 */
@FunctionalInterface
public interface VPageHandler {
    /**
     * 执行处理
     *
     * @param page 虚拟页面
     */
    void handle(VirtualPage page);
}
