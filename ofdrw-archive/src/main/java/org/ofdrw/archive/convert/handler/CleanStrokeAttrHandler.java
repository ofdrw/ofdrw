package org.ofdrw.archive.convert.handler;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 处理器 17：清理无用勾边属性（GB/T 42133-2022 6.3.3c/d）
 * <p>
 * TextObject/PathObject 的 Stroke=false 时删除配合的修饰属性；
 * CompositeObject 总是删除勾边/填充相关属性。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class CleanStrokeAttrHandler implements ArchiveHandler {

    /** Stroke=false 时需要删除的属性名 */
    private static final List<String> STROKE_RELATED_ATTRS = Arrays.asList(
            "LineWidth", "Cap", "Join", "MiterLimit",
            "DashPattern", "DashOffset", "StrokeColor");

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            int pageCount = reader.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(i);
                cleanPageElements(page);
            }
        } catch (Exception e) {
            throw new IOException("清理勾边属性时异常: " + e.getMessage(), e);
        }
    }

    /**
     * 清理页面中的无用属性
     *
     * @param page 页面元素
     */
    @SuppressWarnings("unchecked")
    private void cleanPageElements(Element page) {
        // TextObject: Stroke=false 时删除关联修饰属性
        for (Element textObj : (List<Element>) page.elements("TextObject")) {
            if ("false".equalsIgnoreCase(textObj.attributeValue("Stroke"))) {
                removeAttributes(textObj, STROKE_RELATED_ATTRS);
            }
        }

        // PathObject: Stroke=false 时删除关联修饰属性
        for (Element pathObj : (List<Element>) page.elements("PathObject")) {
            if ("false".equalsIgnoreCase(pathObj.attributeValue("Stroke"))) {
                removeAttributes(pathObj, STROKE_RELATED_ATTRS);
            }
        }

        // CompositeObject: 总是删除勾边/填充属性
        for (Element compObj : (List<Element>) page.elements("CompositeObject")) {
            removeAttributes(compObj, STROKE_RELATED_ATTRS);
            removeAttribute(compObj, "Stroke");
            removeAttribute(compObj, "Fill");
            removeAttribute(compObj, "FillColor");
        }
    }

    /**
     * 移除元素上的一组属性
     */
    private void removeAttributes(Element element, List<String> attrNames) {
        for (String name : attrNames) {
            removeAttribute(element, name);
        }
    }

    /**
     * 移除元素上的指定属性（若存在）
     */
    private void removeAttribute(Element element, String attrName) {
        Attribute attr = element.attribute(attrName);
        if (attr != null) {
            attr.detach();
        }
    }
}
