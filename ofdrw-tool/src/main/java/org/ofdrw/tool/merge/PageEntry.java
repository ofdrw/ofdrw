package org.ofdrw.tool.merge;


import java.util.Arrays;
import java.util.List;

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
     * 页面索引号（页码从1开始）
     */
    public Integer pageIndex;

    /**
     * 是否复制模板
     * <p>
     * 默认：true 复制模板
     */
    public boolean copyTemplate = true;

    /**
     * 是否复注释
     * <p>
     * 默认：true 复制注释
     */
    public boolean copyAnnotations = true;


    /**
     * 需要混合到指定页面的其他文档页面（将一页追加到指定页的上面）
     */
    public List<PageEntry> tbMixPages =null;

    /**
     * 创建迁移页面对象
     *
     * @param pageIndex 页面索引号（页码从1开始）
     * @param docCtx    上下文
     */
    public PageEntry(Integer pageIndex, DocContext docCtx) {
        this.docCtx = docCtx;
        this.pageIndex = pageIndex;
    }


    /**
     * 创建迁移页面对象
     *
     * @param pageIndex 页面索引号（页码从1开始）
     * @param docCtx    上下文
     * @param tbMixPages 需要混合到指定页面的其他文档页面。
     */
    public PageEntry(Integer pageIndex, DocContext docCtx, PageEntry... tbMixPages) {
        this.docCtx = docCtx;
        this.pageIndex = pageIndex;
        if (tbMixPages != null && tbMixPages.length > 0) {
            this.tbMixPages = Arrays.asList(tbMixPages);
        }
    }
}
