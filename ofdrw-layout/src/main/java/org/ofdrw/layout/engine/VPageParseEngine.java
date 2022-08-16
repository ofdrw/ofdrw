package org.ofdrw.layout.engine;

import org.dom4j.DocumentException;
import org.ofdrw.core.basicStructure.pageObj.Content;
import org.ofdrw.core.basicStructure.pageObj.Template;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicStructure.pageTree.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.edit.AdditionVPage;
import org.ofdrw.layout.element.*;
import org.ofdrw.layout.element.canvas.Canvas;
import org.ofdrw.layout.engine.render.*;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.PageDir;
import org.ofdrw.pkg.container.PagesDir;

import java.io.FileNotFoundException;
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
     * @param prm        公共资源管理器(Public Resource Manage)
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

        try {
            pages = docDir.getDocument().getPages();
            if (pages == null) {
                pages = new Pages();
                docDir.getDocument().setPages(pages);
            }
            // 如果存在Pages那么获取，不存在那么创建
            pagesDir = docDir.obtainPages();
        } catch (FileNotFoundException | DocumentException e) {
            throw new RuntimeException("无法获取到Document.xml 对象", e);
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
            if (virtualPage instanceof AdditionVPage) {
                // 执行页面编辑
                pageEdit((AdditionVPage) virtualPage);
            } else {
                PageDir pageDir = null;
                // 创建一个全新的页面容器对象
                if (virtualPage.getPageNum() == null) {
                    pageDir = newPage();
                } else {
                    pageDir = addNewPage(virtualPage.getPageNum() - 1);
                }
                // 解析虚拟页面，并加入到容器中
                convertPageContent(virtualPage, pageDir);
            }
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
        // 如果存在，则设置页面模板
        final List<Template> templates = vPage.getTemplates();
        if (templates != null && !templates.isEmpty()){
            for (Template tpl : templates) {
                page.addTemplate(tpl);
            }
        }

        pageDir.setContent(page);
        if (vPage.getContent().isEmpty()) {
            return;
        }
        // 创建页面的内容
        final Content content = new Content();
        // 获取不同层的内容
        List<List<Div>> layerArr = vPage.getLayerContent();
        for (List<Div> layerContent : layerArr) {
            if (layerContent.isEmpty()) {
                continue;
            }
            // 若层内内容不为空，那么创建图层，并转换为图元
            final Type type = layerContent.get(0).getLayer();
            // 新建一个正文层的图层用于容纳元素
            CT_Layer ctlayer = new CT_Layer();
            ctlayer.setType(type);
            ctlayer.setObjID(maxUnitID.incrementAndGet());
            content.addLayer(ctlayer);
            // 执行转换
            convert2Layer(ctlayer, layerContent);
        }
        page.setContent(content);
    }


    /**
     * 编辑指定的页面
     *
     * @param virtualPage 专用于编辑的虚拟页面
     */
    private void pageEdit(AdditionVPage virtualPage) {
        CT_Layer layer = virtualPage.obtainTopLayer(maxUnitID);
        List<Div> content = virtualPage.getContent();
        // 像图层中些转化的元素对象
        convert2Layer(layer, content);
    }


    /**
     * 将虚拟页面中的元素转为OFD元素加入图层中
     *
     * @param to      图形元素将要写入到的页面图层
     * @param content 需要加入图层得到元素
     */
    private void convert2Layer(CT_Layer to, List<Div> content) {
        // 处理页面中的元素为OFD的图元
        for (Div elem : content) {
            // 忽略占位符和对象
            if (elem instanceof PageAreaFiller
                    || elem.isPlaceholder()) {
                continue;
            }
            // 处理每一个元素的基础盒式模型属性，背景边框等，并加入到图层中
            DivRender.render(to, elem, maxUnitID);

            if (elem instanceof Img) {
                // 渲染图片对象
                ImgRender.render(to, resManager, (Img) elem, maxUnitID);
            } else if (elem instanceof Paragraph) {
                // 渲染段落对象
                ParagraphRender.render(to, resManager, (Paragraph) elem, maxUnitID);
            } else if (elem instanceof Canvas) {
                // 渲染Canvas
                CanvasRender.render(to, resManager, (Canvas) elem, maxUnitID);
            }
        }
    }


    /**
     * 创建页面容器，并且新的页面加入文档中
     *
     * @return 页面容器
     */
    private PageDir newPage() {
        // 设置页面index与页面定位路径一致
        PageDir pageDir = pagesDir.newPageDir();
        String pageLoc = String.format("Pages/Page_%d/Content.xml", pageDir.getIndex());
        final Page page = new Page(maxUnitID.incrementAndGet(), pageLoc);
        pages.addPage(page);
        return pageDir;
    }

    /**
     * 添加页面到指定页码
     *
     * @param index 页码Index （页码 - 1）
     * @return 页面容器
     */
    private PageDir addNewPage(int index) {
        // 设置页面index与页面定位路径一致
        PageDir pageDir = pagesDir.newPageDir();
        String pageLoc = String.format("Pages/Page_%d/Content.xml", pageDir.getIndex());
        final Page page = new Page(maxUnitID.incrementAndGet(), pageLoc);
        final int size = pages.getSize();
        // 防止页码越界
        if (index <= 0) {
            index = 0;
        } else if (index >= size) {
            index = size;
        }
        pages.elements().add(index, page);
        return pageDir;
    }


}
