package org.ofdrw.reader.model;

import org.ofdrw.core.annotation.pageannot.Annot;

import java.util.List;

/**
 * 注释实体
 *
 * @author dltech21
 * @since 2020/9/25
 */
public class AnnotionEntity {
    /**
     * 注释所在页面ID
     */
    private String pageId;

    /**
     * 注释列表
     */
    private List<Annot> annots;

    public AnnotionEntity(String pageId, List<Annot> annots) {
        this.pageId = pageId;
        this.annots = annots;
    }

    public String getPageId() {
        return pageId;
    }

    public List<Annot> getAnnots() {
        return annots;
    }

}
