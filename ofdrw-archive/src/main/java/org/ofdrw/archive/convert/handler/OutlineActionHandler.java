package org.ofdrw.archive.convert.handler;

import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;

/**
 * 处理器 7：清理大纲动作（GB/T 42133-2022 6.2.5a/b）
 *
 * @author xxx
 * @since 2.3.9
 */
public class OutlineActionHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            org.ofdrw.core.basicStructure.doc.Document document = reader.getDoc(0);
            if (document != null && document.getOutlines() != null) {
                document.getOutlines().getOutlineElems().forEach(elem -> {
                    if (elem.getActions() != null) {
                        elem.getActions().getActions().removeIf(action -> {
                            org.ofdrw.core.action.actionType.OFDAction act = action.getAction();
                            return act != null && !(act instanceof org.ofdrw.core.action.actionType.actionGoto.Goto);
                        });
                    }
                });
            }
        } catch (Exception e) {
            throw new IOException("无法处理大纲动作: " + e.getMessage(), e);
        }
    }
}
