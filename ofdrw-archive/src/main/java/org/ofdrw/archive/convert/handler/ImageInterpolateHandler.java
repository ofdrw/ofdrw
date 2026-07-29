package org.ofdrw.archive.convert.handler;

import org.dom4j.Element;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.util.List;

/**
 * 处理器 16：设置图像 Interpolate=false（GB/T 42133-2022 6.5b）
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class ImageInterpolateHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            int pageCount = reader.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(i);
                @SuppressWarnings("unchecked")
                List<Element> imageObjects = page.elements("ImageObject");
                for (Element imgObj : imageObjects) {
                    imgObj.addAttribute("Interpolate", "false");
                }
            }
        } catch (Exception e) {
            throw new IOException("设置插值属性时异常: " + e.getMessage(), e);
        }
    }
}
