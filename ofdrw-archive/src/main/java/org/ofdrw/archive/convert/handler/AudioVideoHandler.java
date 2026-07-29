package org.ofdrw.archive.convert.handler;

import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;

/**
 * 处理器 9：删除音视频资源（GB/T 42133-2022 6.2.6g）
 *
 * @author xxx
 * @since 2.3.9
 */
public class AudioVideoHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        // Phase 1: 框架实现，具体逻辑后续完善
    }
}
