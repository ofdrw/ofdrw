package org.ofdrw.layout.engine;

import org.ofdrw.core.basicStructure.pageTree.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.pkg.dir.DocDir;
import org.ofdrw.pkg.dir.PageDir;
import org.ofdrw.pkg.dir.PagesDir;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 虚拟页面解析引擎
 * <p>
 * 解析虚拟页面解析OFD页面，放入文档容器中
 *
 * @author 权观宇
 * @since 2020-03-20 11:32:29
 */
public class VPageParseEngine {

    /**
     * 文档容器引用
     */
    private DocDir docDir;
    /**
     * 自动增长的文档ID引用
     */
    private AtomicInteger maxUnitID;

    /**
     * 文档公共资源索引
     * <p>
     * 如：字体、图形图像等
     */
    private Res publicRes;

    /**
     * 页面序列索引
     */
    private Pages pages;
    /**
     * 页面虚拟容器
     */
    private PagesDir pagesDir;

    /**
     * 页面最大索引
     * <p>
     * 从0开始
     */
    private int maxPageIndex = 0;

    /**
     * 创建虚拟页面解析器
     *
     * @param docDir    文档容器
     * @param maxUnitID 自增的ID获取器
     */
    public VPageParseEngine(DocDir docDir, AtomicInteger maxUnitID) {
        this.docDir = docDir;
        this.maxUnitID = maxUnitID;

        // 初始化公共资源
        publicRes = docDir.getPublicRes();
        if (publicRes == null) {
            publicRes = new Res();
            docDir.setPublicRes(publicRes);
        }

        pages = docDir.getDocument().getPages();
        if (pages == null) {
            pages = new Pages();
            docDir.getDocument().setPages(pages);
        }
        // 当前页面数量作为最大页面Index，处理文档中已经含有页面的情况
        maxPageIndex = pages.getSize();
        pagesDir = docDir.getPages();
        if (pagesDir == null) {
            pagesDir = new PagesDir();
            docDir.setPages(pagesDir);
        }
    }

    /**
     * 解析序列页面队列为OFD页面
     *
     * @param vPageList 解析序列页面队列
     */
    public void process(List<VirtualPage> vPageList) {
        if (vPageList == null || vPageList.isEmpty()) {
            return;
        }
        LinkedList<VirtualPage> seq = new LinkedList<>(vPageList);
        while (!seq.isEmpty()) {
            VirtualPage virtualPage = seq.pop();
            if (virtualPage == null) {
                continue;
            }
            // 创建一个全新的页面对象
            PageDir pageDir = newPage();
            // TODO 解析虚拟页面

        }
    }


    /**
     * 创建页面目录，并且新的页面加入文档中
     *
     * @return 页面容器
     */
    private PageDir newPage() {
        long id = maxUnitID.incrementAndGet();
        String pageLoc = String.format("Pages/Page_%d/Content.xml", maxPageIndex);
        // 设置页面index与页面定位路径一致
        PageDir pageDir = new PageDir();
        pageDir.setIndex(maxPageIndex);
        maxPageIndex++;
        pages.addPage(new Page(id, pageLoc));
        pagesDir.add(pageDir);
        return pageDir;
    }


}
