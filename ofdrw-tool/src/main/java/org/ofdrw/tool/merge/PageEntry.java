package org.ofdrw.tool.merge;


/**
 * 页面项目
 *
 * @author 权观宇
 * @since 2021-11-08 20:52:49
 */
public class PageEntry {

    /**
     * 关联文档上下文
     */
    public DocContext docCtx;

    /**
     * 页面索引号
     */
    public Integer pageIndex;


    public PageEntry(Integer pageIndex, DocContext docCtx) {
        this.docCtx = docCtx;
        this.pageIndex = pageIndex;
    }
}
