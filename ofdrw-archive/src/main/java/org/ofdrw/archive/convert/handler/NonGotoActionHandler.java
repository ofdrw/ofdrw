package org.ofdrw.archive.convert.handler;

import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;

/**
 * 处理器 6：删除非 Goto 动作（GB/T 42133-2022 6.2.2c/6.2.3c）
 *
 * @author xxx
 * @since 2.3.9
 */
public class NonGotoActionHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            org.ofdrw.core.basicStructure.doc.Document document = reader.getDoc(0);
            if (document != null) {
                document.removeOFDElemByNames("Actions");
            }
        } catch (Exception e) {
            throw new IOException("无法处理动作: " + e.getMessage(), e);
        }
    }
}
