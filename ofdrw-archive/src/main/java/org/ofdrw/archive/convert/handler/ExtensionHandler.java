package org.ofdrw.archive.convert.handler;

import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;

/**
 * 处理器 8：删除扩展信息（GB/T 42133-2022 6.2.2e）
 * <p>
 * 直接删除，不固化到页面内容。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class ExtensionHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            Document document = reader.getDoc(0);
            if (document != null) {
                document.removeOFDElemByNames("Extensions");
            }
        } catch (Exception e) {
            throw new IOException("无法处理扩展信息: " + e.getMessage(), e);
        }
    }
}
