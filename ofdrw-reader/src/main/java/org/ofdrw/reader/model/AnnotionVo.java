package org.ofdrw.reader.model;

import org.ofdrw.core.annotation.pageannot.Annot;

import java.util.List;

/**
 * @author dltech21
 * @since 2020/9/25
 */
public class AnnotionVo {
    private String pageId;
    private List<Annot> annots;

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public List<Annot> getAnnots() {
        return annots;
    }

    public void setAnnots(List<Annot> annots) {
        this.annots = annots;
    }
}
