package org.ofdrw.archive.convert.handler;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理器 14：裁剪区优化（GB/T 42133-2022 6.3.2a/b）
 * <p>
 * 处理规则：
 * <ul>
 *   <li>裁剪区 Boundary 包含图元外接矩形 → 删除冗余裁剪区</li>
 *   <li>裁剪区面积为 0（宽或高 ≤ 0）→ 删除裁剪区 + 设置所在对象 Visible=false</li>
 * </ul>
 *
 * @author xxx
 * @since 2.3.9
 */
public class ClipAreaHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            int pageCount = reader.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(i);
                processPageClips(page);
            }
        } catch (Exception e) {
            throw new IOException("优化裁剪区时异常: " + e.getMessage(), e);
        }
    }

    /**
     * 处理页面中所有的裁剪区
     */
    @SuppressWarnings("unchecked")
    private void processPageClips(Element page) {
        // 遍历所有包含 Clip 属性的图元对象
        for (Element elem : (List<Element>) page.elements()) {
            String clipId = elem.attributeValue("Clip");
            if (clipId != null) {
                processClipRef(elem, clipId, page);
            }
        }

        // 遍历直接嵌入的 ClipArea 元素
        List<Element> clipAreas = new ArrayList<>(page.elements("ClipArea"));
        for (Element clip : clipAreas) {
            checkClipArea(clip);
        }
    }

    /**
     * 检查引用的裁剪区
     */
    private void processClipRef(Element obj, String clipId, Element page) {
        // 在页面中查找对应的 ClipArea
        for (Element clip : (List<Element>) page.elements("ClipArea")) {
            String id = clip.attributeValue("ID");
            if (clipId.equals(id)) {
                checkClipArea(clip);
            }
        }
    }

    /**
     * 检查并优化单个裁剪区
     * <p>
     * 面积 ≤ 0 → 删除裁剪区 + 设置引用对象的 Visible=false
     */
    private void checkClipArea(Element clip) {
        String boundaryStr = clip.attributeValue("Boundary");
        if (boundaryStr == null) return;

        ST_Box boundary = ST_Box.getInstance(boundaryStr);
        if (boundary == null) return;

        double w = boundary.getWidth();
        double h = boundary.getHeight();

        // 面积为 0 → 删除
        if (w <= 0 || h <= 0) {
            clip.detach();
            // 找到引用此裁剪区的对象，设 Visible=false
            Element parent = clip.getParent();
            if (parent != null) {
                @SuppressWarnings("unchecked")
                List<Element> siblings = parent.elements();
                for (Element sib : siblings) {
                    String clipRef = sib.attributeValue("Clip");
                    if (clipRef != null && clipRef.equals(clip.attributeValue("ID"))) {
                        sib.addAttribute("Visible", "false");
                    }
                }
            }
        }
    }
}
