package org.ofdrw.archive.convert.handler;

import org.dom4j.Element;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.model.AnnotionEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理器：注释合规转换（GB/T 42133-2022 6.10.1）
 * <p>
 * 处理内容：
 * <ul>
 *   <li>设置 ReadOnly=true, NoZoom=true, NoRotate=true</li>
 *   <li>展平 Appearance 中的 PageBlock 嵌套（子元素提升到 Appearance 级）</li>
 *   <li>不固化外观（按用户要求）</li>
 * </ul>
 * <p>
 * 关于 6.10.2 归档后注释：管理部门添加的注释（归档章/页码等）应与原有注释分文件存储。
 * 本处理器仅处理归档前已存在的注释，归档后的注释由生成软件负责写入独立文件。
 *
 * @author xxx
 * @since 2.3.9
 */
public class AnnotationHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            List<AnnotionEntity> entities = reader.getAnnotationEntities();
            if (entities == null || entities.isEmpty()) {
                return;
            }

            for (AnnotionEntity entity : entities) {
                for (Annot annot : entity.getAnnots()) {
                    // 1. 设置只读/不缩放/不旋转
                    annot.setReadOnly(true);
                    annot.setNoZoom(true);
                    annot.setNoRotate(true);

                    // 2. 展平 Appearance 中的 PageBlock 嵌套
                    Appearance appearance = annot.getAppearance();
                    if (appearance != null) {
                        flattenPageBlocks(appearance);
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("处理注释时异常: " + e.getMessage(), e);
        }
    }

    /**
     * 展平 Appearance 中的 PageBlock 嵌套
     * <p>
     * 将 Appearance 下所有 PageBlock 的子元素提升至 Appearance 级别，
     * 删除被掏空的中间 PageBlock。
     *
     * @param appearance 注释外观（CT_PageBlock 子类）
     */
    @SuppressWarnings("unchecked")
    private void flattenPageBlocks(Appearance appearance) {
        // 收集所有嵌套的 PageBlock
        List<Element> pageBlocks = appearance.elements("PageBlock");
        for (Element pageBlock : new ArrayList<>(pageBlocks)) {
            // 递归处理更深层次的嵌套
            flattenNested(pageBlock, appearance);
            // 将 PageBlock 中的子元素提升到 Appearance 级别
            List<Element> children = new ArrayList<>(pageBlock.elements());
            for (Element child : children) {
                child.detach();
                appearance.add(child);
            }
            // 删除被掏空的 PageBlock
            pageBlock.detach();
        }
    }

    /**
     * 递归展平深层嵌套的 PageBlock
     * <p>
     * 将嵌套 PageBlock 中的子元素递归提升至根级 Appearance。
     *
     * @param block      当前 PageBlock
     * @param rootParent 根级 Appearance（最终容器）
     */
    @SuppressWarnings("unchecked")
    private void flattenNested(Element block, Element rootParent) {
        List<Element> childBlocks = block.elements("PageBlock");
        for (Element child : new ArrayList<>(childBlocks)) {
            flattenNested(child, rootParent);
            // 提升子元素
            List<Element> children = new ArrayList<>(child.elements());
            for (Element c : children) {
                c.detach();
                rootParent.add(c);
            }
            child.detach();
        }
    }
}
