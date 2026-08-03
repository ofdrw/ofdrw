package org.ofdrw.archive.convert.handler;

import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.dom4j.DocumentException;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 处理器 5：删除视图首选项（GB/T 42133-2022 6.2.2b）
 *
 * @author 权观宇
 * @since 2.4.0
 */
public class VPrefsHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            Document document = reader.getDoc(0);
            if (document != null) {
                document.removeOFDElemByNames("VPreferences");
            }
        } catch (DocumentException | FileNotFoundException e) {
            throw new IOException("无法读取文档: " + e.getMessage(), e);
        }
    }
}
