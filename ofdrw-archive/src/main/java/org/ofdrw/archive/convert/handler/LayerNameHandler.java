package org.ofdrw.archive.convert.handler;

import org.dom4j.Element;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.util.*;

/**
 * 处理器 21：确保图层名唯一（GB/T 42133-2022 6.2.3d）
 * <p>
 * 同一页面中重复的 Layer Name 后追加序列号。
 *
 * @author 权观宇
 * @since 2.4.0
 */
public class LayerNameHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            int pageCount = reader.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(i);
                ensureUniqueLayerNames(page);
            }
        } catch (Exception e) {
            throw new IOException("处理图层名称时异常: " + e.getMessage(), e);
        }
    }

    /**
     * 确保同一页面内 Layer Name 唯一
     *
     * @param page 页面元素
     */
    @SuppressWarnings("unchecked")
    private void ensureUniqueLayerNames(Element page) {
        Set<String> seen = new HashSet<>();
        List<Element> layers = page.elements("Layer");
        for (Element layer : layers) {
            String name = layer.attributeValue("Name");
            if (name == null) continue;

            if (seen.contains(name)) {
                // 重名时追加序号
                int counter = 2;
                String newName;
                do {
                    newName = name + "_" + counter;
                    counter++;
                } while (seen.contains(newName));
                layer.addAttribute("Name", newName);
                seen.add(newName);
            } else {
                seen.add(name);
            }
        }
    }
}
