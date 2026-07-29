package org.ofdrw.archive.convert.handler;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.util.List;

/**
 * 处理器 18：清理图像/视频对象的无用填充属性（GB/T 42133-2022 6.3.3d/e）
 * <p>
 * ImageObject 和 CompositeObject 上的 Fill/FillColor 属性对该类对象无意义，应予删除。
 *
 * @author xxx
 * @since 2.3.9
 */
public class CleanFillAttrHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            int pageCount = reader.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(i);

                // ImageObject: 删除 Fill/FillColor
                @SuppressWarnings("unchecked")
                List<Element> imageObjects = page.elements("ImageObject");
                for (Element imgObj : imageObjects) {
                    removeAttribute(imgObj, "Fill");
                    removeAttribute(imgObj, "FillColor");
                }
            }
        } catch (Exception e) {
            throw new IOException("清理填充属性时异常: " + e.getMessage(), e);
        }
    }

    /**
     * 移除元素上的指定属性
     */
    private void removeAttribute(Element element, String attrName) {
        Attribute attr = element.attribute(attrName);
        if (attr != null) {
            attr.detach();
        }
    }
}
