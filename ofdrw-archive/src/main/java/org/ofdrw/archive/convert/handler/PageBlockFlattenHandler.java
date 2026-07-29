package org.ofdrw.archive.convert.handler;

import org.dom4j.Element;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理器 13：展平过深的 PageBlock 嵌套（GB/T 42133-2022 6.2.3e）
 * <p>
 * 将嵌套深度超过 3 的 PageBlock 子元素提升到父级，保持 Z 序。
 *
 * @author xxx
 * @since 2.3.9
 */
public class PageBlockFlattenHandler implements ArchiveHandler {

    /** 最大允许的 PageBlock 嵌套深度 */
    private static final int MAX_DEPTH = 3;

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            int pageCount = reader.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(i);
                // 对页面中每个顶层 PageBlock 递归展平
                @SuppressWarnings("unchecked")
                List<Element> pageBlocks = page.elements("PageBlock");
                for (Element pageBlock : pageBlocks) {
                    flattenRecursive(pageBlock, 1);
                }
            }
        } catch (Exception e) {
            throw new IOException("展平 PageBlock 时异常: " + e.getMessage(), e);
        }
    }

    /**
     * 递归展平 PageBlock
     * <p>
     * 深度 > MAX_DEPTH 时，将当前 PageBlock 的子元素提升到其父级，
     * 然后 detach 当前 PageBlock。
     *
     * @param block 当前 PageBlock 元素
     * @param depth 当前深度（1 起始）
     */
    @SuppressWarnings("unchecked")
    private void flattenRecursive(Element block, int depth) {
        // 先递归处理子 PageBlock
        List<Element> childBlocks = block.elements("PageBlock");
        for (Element child : new ArrayList<>(childBlocks)) {
            flattenRecursive(child, depth + 1);
        }

        // 深度超限：将当前块的子元素提升到父级
        if (depth > MAX_DEPTH) {
            Element parent = block.getParent();
            if (parent != null) {
                // 收集所有非 PageBlock 子元素（保留文本/图形等图元）
                List<Element> children = new ArrayList<>(block.elements());
                int insertIdx = parent.elements().indexOf(block);
                for (Element child : children) {
                    child.detach();
                    parent.elements().add(insertIdx++, child);
                }
                // 移除被掏空的 PageBlock
                block.detach();
            }
        }
    }
}
