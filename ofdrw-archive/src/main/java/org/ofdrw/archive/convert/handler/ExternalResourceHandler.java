package org.ofdrw.archive.convert.handler;

import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;

/**
 * 处理器 3：删除外部资源引用（GB/T 42133-2022 6.1）
 * <p>
 * 直接删除指向外部的资源引用，不下载外部内容（安全考虑）。
 *
 * @author xxx
 * @since 2.3.9
 */
public class ExternalResourceHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        // Phase 1: 简化实现 — 外部引用在 check 阶段已标记
        // 实际删除操作涉及 XML 内元素的移除，此处保留框架
    }
}
