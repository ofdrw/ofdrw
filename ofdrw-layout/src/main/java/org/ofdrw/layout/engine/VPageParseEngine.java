package org.ofdrw.layout.engine;

import org.ofdrw.core.basicStructure.pageObj.Content;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageTree.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.Img;
import org.ofdrw.layout.element.PageAreaFiller;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.layout.engine.render.DivRender;
import org.ofdrw.layout.engine.render.ImgRender;
import org.ofdrw.layout.engine.render.ParagraphRender;
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
     * 页面元素布局
     */
    private PageLayout pageLayout;

    /**
     * 公共资源管理器
     */
    private ResManager resManager;

    /**
     * 创建虚拟页面解析器
     *
     * @param pageLayout 页面布局样式
     * @param docDir     文档容器
     * @param prm        公共资源管理器
     * @param maxUnitID  自增的ID获取器
     */
    public VPageParseEngine(PageLayout pageLayout,
                            DocDir docDir,
                            ResManager prm,
                            AtomicInteger maxUnitID) {
        this.docDir = docDir;
        this.maxUnitID = maxUnitID;
        this.pageLayout = pageLayout;
        resManager = prm;

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
            // 创建一个全新的页面容器对象
            PageDir pageDir = newPage();
            // 解析虚拟页面，并加入到容器中
            convertPageContent(virtualPage, pageDir);
        }
    }

    /**
     * 转化虚拟页面的内容为实际OFD元素
     *
     * @param vPage   虚拟页面
     * @param pageDir 虚拟页面目录
     */
    private void convertPageContent(VirtualPage vPage, PageDir pageDir) {
        // 底层的OFD页面对象
        org.ofdrw.core.basicStructure.pageObj.Page page = new org.ofdrw.core.basicStructure.pageObj.Page();
        PageLayout vPageStyle = vPage.getStyle();
        if (!pageLayout.equals(vPageStyle)) {
            // 如果与默认页面样式不一致，那么需要单独设置页面样式
            page.setArea(vPageStyle.getPageArea());
        }
        pageDir.setContent(page);
        if (vPage.getContent().isEmpty()) {
            return;
        }
        // 新建一个正文层的图层用于容纳元素
        CT_Layer layer = new CT_Layer();
        layer.setObjID(maxUnitID.incrementAndGet());
        // 添加一个页面的内容
        page.setContent(new Content().addLayer(layer));

        // 处理页面中的元素为OFD的图元
        for (Div elem : vPage.getContent()) {
            // 忽略占位符和对象
            if (elem instanceof PageAreaFiller
                    || elem.isPlaceholder()) {
                continue;
            }
            // 处理每一个元素的基础盒式模型属性，背景边框等，并加入到图层中
            DivRender.render(layer, elem, maxUnitID);

            if (elem instanceof Img) {
                // 渲染图片对象
                ImgRender.render(layer, resManager, (Img) elem, maxUnitID);
            } else if (elem instanceof Paragraph) {
                // 渲染段落对象
                ParagraphRender.render(layer, resManager, (Paragraph) elem, maxUnitID);
            }
        }
    }


    /**
     * 创建页面容器，并且新的页面加入文档中
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
