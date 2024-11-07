package org.ofdrw.tool.merge;


import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.util.encoders.Hex;
import org.dom4j.*;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.annotation.Annotations;
import org.ofdrw.core.annotation.pageannot.AnnPage;
import org.ofdrw.core.annotation.pageannot.PageAnnot;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.pageObj.CT_TemplatePage;
import org.ofdrw.core.basicStructure.pageObj.Content;
import org.ofdrw.core.basicStructure.pageObj.Template;
import org.ofdrw.core.basicStructure.pageTree.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.compositeObj.CT_VectorG;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.pkg.container.*;
import org.ofdrw.reader.ResourceLocator;
import org.ofdrw.reader.model.TemplatePageEntity;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文档合并工具
 *
 * @author 权观宇
 * @since 2021-11-08 20:49:36
 */
public class OFDMerger implements Closeable {

    /**
     * 新页面列表
     * <p>
     * 每一个元素代表新文档中的一页
     */
    public final ArrayList<PageEntry> pageArr;
    /**
     * 文档上下文映射
     */
    private final Map<String, DocContext> docCtxMap;


    /**
     * 合并后生成文档位置
     */
    private final Path dest;

    /**
     * 合并的目标文档，仅在合并时设置
     * 在合并完成后将会被打包存储
     */
    private BareOFDDoc ofdDoc;

    /**
     * 注释入口文件
     */
    private Annotations newDocAnnotations = null;
    /**
     * 注释目录
     */
    private AnnotsDir annotsDir = null;


    /**
     * 资源文件哈希表
     * <p>
     * Key: 文件SM3 Hash Hex
     * Value: 文件在新文档中的文件名
     */
    private final Map<String, ST_Loc> resFileHashTable;


    /**
     * 模板页面映射表
     * <p>
     * Key: 模板页对象ID
     * Value: 模板页面对象
     */
    private final Map<String, CT_TemplatePage> tplPageMap;

    private final AtomicInteger resFileCounter;
    /**
     * - Layer 的 DrawParam
     * - 每个图像对象都可能含有 DrawParam 引用
     * - Color 中 Pattern CellContent Thumbnail 引用
     * - Color 中 ColorSpace 引用
     * - Image 中 ResourceID、Substitution、ImageMask
     * - Text 中 Font
     * - Composite 复合对象 中 ResourceID
     * Res资源中的 CompositeGraphUnit CT_VectorG：Thumbnail、Substitution
     */
    private static final Map<String, XPath> AttrQueries = new HashMap<String, XPath>() {{
        this.put("Font", DocumentHelper.createXPath("//*[@Font]"));
        this.put("ResourceID", DocumentHelper.createXPath("//*[@ResourceID]"));
        this.put("Substitution", DocumentHelper.createXPath("//*[@Substitution]"));
        this.put("ImageMask", DocumentHelper.createXPath("//*[@ImageMask]"));
        this.put("Thumbnail", DocumentHelper.createXPath("//*[@Thumbnail]"));
        this.put("DrawParam", DocumentHelper.createXPath("//*[@DrawParam]"));
        this.put("ColorSpace", DocumentHelper.createXPath("//*[@ColorSpace]"));
    }};

    public OFDMerger(Path dest) {
        if (dest == null) {
            throw new IllegalArgumentException("合并结果路径(dest)为空");
        }
        pageArr = new ArrayList<>(10);
        docCtxMap = new HashMap<>();
        this.dest = dest;
        final Path parent = dest.getParent();
        if (parent == null || !Files.exists(parent)) {
            throw new IllegalArgumentException("OFD文件存储路径(dest)上级目录 [" + parent + "] 不存在");
        }
        resFileHashTable = new HashMap<>(3);
        tplPageMap = new HashMap<>(2);
        resFileCounter = new AtomicInteger(0);
    }


    /**
     * 向合并文件中添加页面
     *
     * @param filepath    待合并的OFD文件路径
     * @param pageIndexes 页面序序列，如果为空表示所有页面（页码从1开始）
     * @return this
     * @throws IOException 页面读写异常
     */
    public OFDMerger add(Path filepath, int... pageIndexes) throws IOException {
        if (filepath == null) {
            return this;
        }
        String key = filepath.toAbsolutePath().getFileName().toString();
        DocContext ctx = docCtxMap.get(key);
        // 缓存中没有该文件映射
        if (ctx == null) {
            // 加载文件上下文
            ctx = new DocContext(filepath);
            docCtxMap.put(key, ctx);
        }
        // 没有传递页码时认为需要追加所有页面
        if (pageIndexes == null || pageIndexes.length == 0) {
            int numberOfPages = ctx.reader.getNumberOfPages();
            pageIndexes = new int[numberOfPages];
            for (int i = 0; i < pageIndexes.length; i++) {
                pageIndexes[i] = i + 1;
            }
        }
        // 追加内容到页面列表中
        for (int pageIndex : pageIndexes) {
            pageArr.add(new PageEntry(pageIndex, ctx));
        }
        return this;
    }


    /**
     * 添加并混合两个文档中的页面
     * <p>
     * 注意两个页面大小不一致，则以第一个文档的页面大小为准。
     *
     * @param dstDocFilepath   原始文档路径（用于作为基础页面其他文档的页面将加入到该页面中）
     * @param dstPageIndex     要混合的页面页序列，如果为空表示所有页面（页码从1开始）
     * @param tbMixDocFilepath 需要混合的文档路径，该文档的页面内容将被混合到目标文档中
     * @param tbMixPageIndex   需要混合的文档页序列（页码从1开始）
     * @return this
     * @throws IOException 页面读写异常
     */
    public OFDMerger addMix(Path dstDocFilepath, int dstPageIndex, Path tbMixDocFilepath, int tbMixPageIndex) throws IOException {
        if (dstDocFilepath == null) {
            return this;
        }

        String key = dstDocFilepath.toAbsolutePath().getFileName().toString();
        DocContext ctx = docCtxMap.get(key);
        // 缓存中没有该文件映射
        if (ctx == null) {
            // 加载文件上下文
            ctx = new DocContext(dstDocFilepath);
            docCtxMap.put(key, ctx);
        }
        PageEntry pageEntry = new PageEntry(dstPageIndex, ctx);
        if (tbMixDocFilepath != null) {
            // 追加内容到页面列表中
            PageEntry tbMixPageEntry = new PageEntry(tbMixPageIndex, new DocContext(tbMixDocFilepath));
            String keyMix = tbMixDocFilepath.toAbsolutePath().getFileName().toString();
            DocContext tbMixCtx = docCtxMap.get(keyMix);
            // 缓存中没有该文件映射
            if (tbMixCtx == null) {
                // 加载文件上下文
                tbMixCtx = new DocContext(tbMixDocFilepath);
                docCtxMap.put(keyMix, tbMixCtx);
            }
            pageEntry.tbMixPages = new ArrayList<>(1);
            pageEntry.tbMixPages.add(tbMixPageEntry);
        }
        pageArr.add(pageEntry);
        return this;
    }


    /**
     * 向合并文件中添加页面
     * <p>
     * 通过该方法可以详细设置页面迁移时的属性参数
     *
     * @param pages 页面对象
     * @return this
     */
    public OFDMerger add(PageEntry... pages) {
        if (pages == null) {
            return this;
        }
        for (PageEntry page : pages) {
            if (page.docCtx == null || page.docCtx.filepath == null) {
                continue;
            }
            String key = page.docCtx.filepath.toAbsolutePath().getFileName().toString();
            // 缓存中没有该文件映射
            if (!docCtxMap.containsKey(key)) {
                docCtxMap.put(key, page.docCtx);
            }
            if (page.tbMixPages != null) {
                for (PageEntry beMixPage : page.tbMixPages) {
                    String keyMix = beMixPage.docCtx.filepath.toAbsolutePath().getFileName().toString();
                    // 缓存中没有该文件映射
                    if (!docCtxMap.containsKey(keyMix)) {
                        docCtxMap.put(keyMix, beMixPage.docCtx);
                    }
                }
            }

            pageArr.add(page);
        }

        return this;
    }

    /**
     * 执行合并
     */
    private void doMerge() throws IOException, DocumentException {
        // 删除原来存在的问题
        if (Files.exists(dest)) {
            Files.delete(dest);
        }
        // 创建新文档
        try (final BareOFDDoc ofdDoc = new BareOFDDoc(dest)) {
            this.ofdDoc = ofdDoc;
            final Pages pages = ofdDoc.document.getPages();
            // 如果存在Pages那么获取，不存在那么创建
            final PagesDir pagesDir = ofdDoc.docDir.obtainPages();
            for (final PageEntry pageEntry : pageArr) {
                // 取Doc_0文档对象
                final CT_PageArea docDefaultArea = new CT_PageArea((Element) pageEntry.docCtx.getDefaultArea(0).clone());
                org.ofdrw.core.basicStructure.pageObj.Page page = null;
                ST_ID oldPageID = null;
                ST_ID newPageID = null;
                // 解析原OFD页面的Content.xml 为Page对象
                try {
                    // 获取页面在原文档中的对象ID
                    oldPageID = pageEntry.docCtx.reader.getPageObjectId(pageEntry.pageIndex);
                } catch (NumberFormatException e) {
                    // 忽略页码非法的页面复制
                    continue;
                }

                Element copy = (Element) pageEntry.docCtx.reader.getPage(pageEntry.pageIndex).clone();
                final Document document = DocumentHelper.createDocument();
                document.add(copy);
                page = new org.ofdrw.core.basicStructure.pageObj.Page(copy);

                // 若当前页面的页面区域的大小和位置为空，则使用文档默认的尺寸
                if (page.getArea() == null) {
                    page.setArea(docDefaultArea);
                }

                // 创建页面容器
                PageDir pageDir = pagesDir.newPageDir();
                String pageLoc = String.format("Pages/Page_%d/Content.xml", pageDir.getIndex());
                // 将创建的页面加入 Document.xml 中的 Pages 内
                final Page newPageItem = new Page(this.ofdDoc.MaxUnitID.incrementAndGet(), pageLoc);
                pages.addPage(newPageItem);
                // 获取页面在新文档中的ID
                newPageID = newPageItem.getID();

                // 迁移页面模板
                if (pageEntry.copyTemplate) {
                    // 页面模板的迁移的替换
                    final List<Template> pageTplArr = page.getTemplates();
                    for (Template tplObj : pageTplArr) {
                        // 迁移页面
                        ST_RefID tplNewId = pageTplMigrate(pageEntry.docCtx, tplObj);
                        tplObj.setTemplateID(tplNewId);
                    }
                }

                // 通过XML 选中与资源有关对象，并实现资源迁移和引用替换
                domMigrate(pageEntry.docCtx, page);
                // 页面放入页面容器中
                pageDir.setContent(page);

                // 迁移注释
                if (pageEntry.copyAnnotations && oldPageID != null && newPageID != null) {
                    String pageDirName = pageDir.getContainerName();
                    pageAnnotationMigrate(pageEntry.docCtx, oldPageID, newPageID, pageDirName);
                }
                // 若存在混合页面，那么混合页面
                if (pageEntry.tbMixPages != null) {
                    for (PageEntry beMixPageEntry : pageEntry.tbMixPages) {
                        // 将混合页面的内容混合到目标页面中
                        mixPage(pageDir, newPageID, page, beMixPageEntry);
                    }
                }
            }
        }
    }

    /**
     * 执行混合页面内容
     *
     * @param targetPageDir  目标页面容器
     * @param newPageID      目标页面ID
     * @param targetPageObj  目标页面对象
     * @param beMixPageEntry 被混合页面
     * @throws IOException 文件读取或复制异常
     */
    private void mixPage(PageDir targetPageDir, ST_ID newPageID, org.ofdrw.core.basicStructure.pageObj.Page targetPageObj, PageEntry beMixPageEntry) throws IOException {
        ST_ID oldPageID = null;
        // 解析原OFD页面的Content.xml 为Page对象
        try {
            // 获取页面在原文档中的对象ID
            oldPageID = beMixPageEntry.docCtx.reader.getPageObjectId(beMixPageEntry.pageIndex);
        } catch (NumberFormatException e) {
            // 忽略页码非法的页面复制
            return;
        }
        // 复制页面
        Element copy = (Element) beMixPageEntry.docCtx.reader.getPage(beMixPageEntry.pageIndex).clone();
        final Document document = DocumentHelper.createDocument();
        document.add(copy);
        org.ofdrw.core.basicStructure.pageObj.Page beMixPage = new org.ofdrw.core.basicStructure.pageObj.Page(copy);


        // 迁移页面模板
        if (beMixPageEntry.copyTemplate) {
            // 页面模板的迁移的替换
            final List<Template> pageTplArr = beMixPage.getTemplates();
            for (Template tplObj : pageTplArr) {
                // 迁移页面
                ST_RefID tplNewId = pageTplMigrate(beMixPageEntry.docCtx, tplObj);
                tplObj.setTemplateID(tplNewId);
            }
            // 将不在目标页面的模板加入到目标页面
            final List<Template> targetTplArr = targetPageObj.getTemplates();
            for (Template tplObj : pageTplArr) {
                boolean exist = false;
                for (Template targetTpl : targetTplArr) {
                    if (tplObj.getTemplateID().equals(targetTpl.getTemplateID())) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    targetPageObj.addTemplate(tplObj);
                }
            }
        }

        // 通过XML 选中与资源有关对象，并实现资源迁移和引用替换
        domMigrate(beMixPageEntry.docCtx, beMixPage);

        // 迁移注释
        if (beMixPageEntry.copyAnnotations && oldPageID != null) {
            String pageDirName = targetPageDir.getContainerName();
            pageAnnotationMigrate(beMixPageEntry.docCtx, oldPageID, newPageID, pageDirName);
        }

        // 把被混合页面Content中的内容追加到目标页面Content中
        Content targetPageObjContent = targetPageObj.getContent();
        if (targetPageObjContent == null) {
            // 若原页面没有内容描述对象，则创建一个
            targetPageObjContent = new Content();
            targetPageObj.setContent(targetPageObjContent);
        }
        Content beMixPageContent = beMixPage.getContent();
        if (beMixPageContent != null) {
            List<Element> elements = beMixPageContent.elements();
            for (Element element : elements) {
                targetPageObjContent.add((Element) element.clone());
            }
        }
    }

    /**
     * 页面注释迁移到新文档，若页面无注释则跳过。
     *
     * @param docCtx           文档上下文
     * @param oldPageID        原页面ID
     * @param newPageID        迁移后页面ID
     * @param pageAnnotDirName 页面所处容器名称，格式为Page_N
     */
    private void pageAnnotationMigrate(DocContext docCtx, ST_ID oldPageID, ST_ID newPageID, String pageAnnotDirName) throws IOException {
        final ResourceLocator rl = docCtx.reader.getResourceLocator();
        try {
            rl.save();
            org.ofdrw.core.basicStructure.doc.Document srcDoc = docCtx.reader.cdDefaultDoc();
            // 获取 注释入口文件 Annotations.xml
            ST_Loc srcAnnotListPath = srcDoc.getAnnotations();
            if (srcAnnotListPath == null || !(rl.exist(srcAnnotListPath.toString()))) {
                return;
            }
            Annotations annotList = rl.get(srcAnnotListPath, Annotations::new);
            if (annotList == null) {
                return;
            }
            // 获取指定页面的注释
            AnnPage annPage = annotList.getByPageId(oldPageID.toString());
            if (annPage == null || annPage.getFileLoc() == null) {
                return;
            }
            // 进入 注释入口文件 所在目录
            rl.cd(srcAnnotListPath.parent());
            // 解析并获取 分页注释文件
            PageAnnot pageAnnot = rl.get(annPage.getFileLoc(), PageAnnot::new);
            if (pageAnnot == null) {
                return;
            }
            Element copy = (Element) pageAnnot.clone();
            final Document document = DocumentHelper.createDocument();
            document.add(copy);
            pageAnnot = new PageAnnot(copy);

            if (this.newDocAnnotations == null) {
                // 创建注释目录 /Doc_0/Annots/
                this.annotsDir = this.ofdDoc.docDir.obtainAnnots();
                // 创建注释入口文件 /Doc_0/Annots/Annotations.xml
                this.newDocAnnotations = new Annotations();
                this.annotsDir.setAnnotations(this.newDocAnnotations);
                this.ofdDoc.document.setAnnotations(this.annotsDir.getAbsLoc().cat(DocDir.AnnotationsFileName));
            }

            // 获取页面注释容器 /Doc_0/Annots/Page_N/ ，不存在则创建
            PageDir pageDir = annotsDir.obtainContainer(pageAnnotDirName, PageDir::new);
            // 向容器中加入 分页注释文件 /Doc_0/Annots/Page_N/Annot_N.xml
            ST_Loc pageAnnotPath = pageDir.addAnnot(pageAnnot);
            // 设置新文档中的页面ID
            AnnPage annotItem = new AnnPage().setPageID(newPageID).setFileLoc(pageAnnotPath);
            this.newDocAnnotations.addPage(annotItem);

            // 迁移注释中资源
            domMigrate(docCtx, pageAnnot);
        } catch (FileNotFoundException | DocumentException e) {
            System.err.println("页面注释迁移失败：" + e.getMessage());
        } finally {
            // 还原原有工作区
            rl.restore();
        }
    }

    /**
     * 页面模板迁移到新文档
     * <p>
     * 若模板已经迁移过，那么直接返回迁移后的页面ID
     *
     * @param docCtx 原文档上下文
     * @param tplObj 页面模板信息对象
     * @return 迁移后模板页面在新文档中的引用ID
     * @throws IOException 文件复制异常
     */
    private ST_RefID pageTplMigrate(DocContext docCtx, Template tplObj) throws IOException {
        final String oldId = tplObj.getTemplateID().toString();
        CT_TemplatePage templatePage = tplPageMap.get(oldId);
        if (templatePage != null) {
            // 页面已经复制过
            // 如果模板ID一样，但是模板内容不一样
            if (templatePage.asXML().equals(tplObj.asXML())) {
                return templatePage.getID().ref();
            }
        }

        // 从文档中加载模板页面实体
        final TemplatePageEntity entity = docCtx.reader.getTemplate(oldId);
        // 复制模板页面
        org.ofdrw.core.basicStructure.pageObj.Page pageObj = entity.getPage();
        Element copy = (Element) pageObj.clone();
        final Document document = DocumentHelper.createDocument();
        document.add(copy);
        pageObj = new org.ofdrw.core.basicStructure.pageObj.Page(copy);

        templatePage = entity.getTplInfo();
        templatePage.setParent(null);

        // 迁移模板页面中相关的资源，并替换模板页面中ID
        domMigrate(docCtx, pageObj);
        // 写入到模板容器中，并更新模板信息对象
        final ST_Loc tplPageLoc = ofdDoc.docDir.obtainTemps().add(pageObj);
        templatePage.setBaseLoc(tplPageLoc);

        // 分配新文档的ID，并添加到新文档中的CommonData
        ST_ID newId = new ST_ID(ofdDoc.MaxUnitID.incrementAndGet());
        templatePage.setID(newId);
        ofdDoc.cdata.addTemplatePage(templatePage);

        // 缓存并返回文件引用
        tplPageMap.put(oldId, templatePage);
        return newId.ref();
    }


    /**
     * DOM元素节点的资源迁移
     * <p>
     * 检查DOM节点下所有引用资源，并将资源迁移到新文档中
     * 更新DOM引用ID
     * <p>
     * 重新分配对象ID
     *
     * @param docCtx DOM相关的文档上下文
     * @param dom    待迁移DOM
     * @throws IOException 文件读取或复制异常
     */
    private void domMigrate(DocContext docCtx, Element dom) throws IOException {
        List<Node> nodes;
        for (Map.Entry<String, XPath> entry : AttrQueries.entrySet()) {
            nodes = entry.getValue().selectNodes(dom);
            if (nodes.isEmpty()) {
                continue;
            }
            String attrName = entry.getKey();
            for (Node node : nodes) {
                if (node instanceof Element) {
                    Element element = (Element) node;
                    // 获取原资源ID
                    final String oldResId = element.attributeValue(attrName);
                    // 迁移资源到新文档，并返回新文档中该资源的ID
                    long newResId = resMigrate(docCtx, oldResId);
                    // 设置新的资源ID
                    element.addAttribute(attrName, Long.toString(newResId));
                }
            }
        }

        // 修改DOM中原有的对象ID为新页面的对象ID
        final List<Node> objArr = dom.selectNodes("//*[@ID]");
        for (Node node : objArr) {
            if (node instanceof Element) {
                Element element = (Element) node;
                // 设置新的对象ID
                element.addAttribute("ID", Integer.toString(ofdDoc.MaxUnitID.incrementAndGet()));
            }
        }
    }

    /**
     * 从原文当迁移资源到新页面
     *
     * @param docCtx   被迁移的页面文档上下文
     * @param oldResId 资源ID
     * @return 资源在新文档中的ID, 0标识没有找到资源
     * @throws IOException 文件读取或复制错误
     */
    private long resMigrate(DocContext docCtx, String oldResId) throws IOException {
        final OFDElement resObj = docCtx.resMgt.get(oldResId);
        if (resObj == null) {
            return 0;
        }
        final ResourceLocator rl = docCtx.reader.getResourceLocator();

        // 检查缓存，防止重复迁移
        final OFDElement cache = docCtx.resOldNewMap.get(oldResId);
        if (cache != null) {
            return cache.getObjID().getId();
        }
        // 缓存对象
        docCtx.resOldNewMap.put(oldResId, resObj);
        resObj.setParent(null);
        if (resObj instanceof CT_ColorSpace) {
            CT_ColorSpace cs = (CT_ColorSpace) resObj;
            ST_Loc profile = cs.getProfile();
            if (profile != null) {
                // 复制资源到新的文档中
                Path filepath = rl.getFile(profile);
                profile = copyResFile(filepath);
                cs.setProfile(profile);
            }
            ofdDoc.prm.addRawWithCache(cs);
        } else if (resObj instanceof CT_DrawParam) {
            CT_DrawParam dp = (CT_DrawParam) resObj;
            ofdDoc.prm.addRawWithCache(dp);
        } else if (resObj instanceof CT_Font) {
            CT_Font f = (CT_Font) resObj;
            ST_Loc fontFileLoc = f.getFontFile();
            if (fontFileLoc != null) {
                // 复制资源到新的文档中
                Path filepath = rl.getFile(fontFileLoc);
                fontFileLoc = copyResFile(filepath);
                f.setFontFile(fontFileLoc);
            }
            ofdDoc.prm.addRawWithCache(f);
        } else if (resObj instanceof CT_MultiMedia) {
            CT_MultiMedia mm = (CT_MultiMedia) resObj;
            ST_Loc mediaFileLoc = mm.getMediaFile();
            if (mediaFileLoc != null) {
                // 复制资源到新的文档中
                Path filepath = rl.getFile(mediaFileLoc);
                mediaFileLoc = copyResFile(filepath);
                mm.setMediaFile(mediaFileLoc);
            }

            ofdDoc.prm.addRawWithCache(mm);
        } else if (resObj instanceof CT_VectorG) {
            CT_VectorG vg = (CT_VectorG) resObj;
            final Document d = DocumentHelper.createDocument();
            d.add(vg);

            // 矢量图像，等于一个DOM 运行迁移程序，向迁移矢量图像内部的资源，再迁该资源本身。
            domMigrate(docCtx, vg);
            ofdDoc.prm.addRawWithCache(vg);
        } else {
            // 未知的资源类型不进行迁移
            return 0;
        }

        return resObj.getObjID().getId();
    }

    /**
     * 复制资源到新文档
     * <p>
     * 复制前将会计算文档的Hash并缓存防止重复
     * <p>
     * 复制后的文档名称为文件的Hash值
     *
     * @param filepath 文件路径
     * @return 复制后基于资源容器的相对路径
     * @throws IOException 文件读取复制异常
     */
    private ST_Loc copyResFile(Path filepath) throws IOException {
        // 计算文件的摘要值
        SM3.Digest digest = new SM3.Digest();
        byte[] buff = new byte[4096];
        int n = 0;
        try (final InputStream in = Files.newInputStream(filepath)) {
            while ((n = in.read(buff)) != -1) {
                digest.update(buff, 0, n);
            }
        }
        String hash = Hex.toHexString(digest.digest());

        // 检查该文件是否已经被迁移过
        final ST_Loc resLoc = resFileHashTable.get(hash);
        if (resLoc != null) {
            return resLoc;
        }

        // 重命名文件为文件计数器，保留后缀名
        String fileName = filepath.getFileName().toString();
        int off = fileName.lastIndexOf('.');
        if (off != -1) {
            fileName = resFileCounter.incrementAndGet() + fileName.substring(off);
        } else {
            fileName = Integer.toString(resFileCounter.incrementAndGet());
        }

        final ResDir resDir = ofdDoc.docDir.obtainRes();
        // 复制文件到新文件容的资源容器中
        try (final InputStream in = Files.newInputStream(filepath)) {
            resDir.addRaw(fileName, in);
        }
        final ST_Loc res = new ST_Loc(fileName);
        // 缓存，返回文件名称（基于Res容器的相对路径）
        resFileHashTable.put(hash, res);
        return res;
    }

    @Override
    public void close() throws IOException {
        // 只有在新文档中含有页面时才允许运行合并进程
        if (!pageArr.isEmpty()) {
            try {
                doMerge();
            } catch (DocumentException e) {
                throw new IOException(e);
            }
        }
        // 关闭已经打开的文档上下文
        for (DocContext docContext : docCtxMap.values()) {
            docContext.close();
        }
    }


}
