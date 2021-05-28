package org.ofdrw.reader.model;


import org.ofdrw.core.basicStructure.pageObj.Page;

/**
 * @deprecated {@link org.ofdrw.reader.PageInfo}
 *
 * @author dltech21
 * @since 2020/8/11
 */
@Deprecated
public class OfdPageVo {
    private Page contentPage;

    private Page templatePage;

    public OfdPageVo(Page contentPage, Page templatePage) {
        this.contentPage = contentPage;
        this.templatePage = templatePage;
    }

    public Page getContentPage() {
        return contentPage;
    }

    public void setContentPage(Page contentPage) {
        this.contentPage = contentPage;
    }

    public Page getTemplatePage() {
        return templatePage;
    }

    public void setTemplatePage(Page templatePage) {
        this.templatePage = templatePage;
    }
}
