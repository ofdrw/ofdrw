package org.ofdrw.tool.merge;


import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.util.encoders.Hex;
import org.dom4j.*;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.pageObj.CT_TemplatePage;
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
import org.ofdrw.pkg.container.PageDir;
import org.ofdrw.pkg.container.PagesDir;
import org.ofdrw.pkg.container.ResDir;
import org.ofdrw.reader.ResourceLocator;
import org.ofdrw.reader.model.TemplatePageEntity;

import java.io.Closeable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
                // 取0文档对象
                final CT_PageArea docDefaultArea = new CT_PageArea((Element) pageEntry.docCtx.getDefaultArea(0).clone());
                org.ofdrw.core.basicStructure.pageObj.Page page = null;
                // 解析原OFD页面的Content.xml 为Page对象
                try {
                    Element copy = (Element) pageEntry.docCtx.reader.getPage(pageEntry.pageIndex).clone();
                    final Document document = DocumentHelper.createDocument();
                    document.add(copy);
                    page = new org.ofdrw.core.basicStructure.pageObj.Page(copy);
                } catch (NumberFormatException e) {
                    // 忽略页码非法的页面复制
                    continue;
                }

                // 若当前页面的页面区域的大小和位置为空，则使用文档默认的尺寸
                if (page.getArea() == null) {

                    page.setArea(docDefaultArea);
                }
                // 创建页面容器
                final PageDir pageDir = newPage(pages, pagesDir);

                // 页面模板的迁移的替换
                final List<Template> pageTplArr = page.getTemplates();
                for (Template tplObj : pageTplArr) {
                    // 迁移页面
                    ST_RefID tplNewId = pageTplMigrate(pageEntry.docCtx, tplObj);
                    tplObj.setTemplateID(tplNewId);
                }

                // 通过XML 选中与资源有关对象，并实现资源迁移和引用替换
                domMigrate(pageEntry.docCtx, page);
                // 把替换后得到页面放入页面容器中
                pageDir.setContent(page);
            }

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
            return templatePage.getID().ref();
        }

        // 从文档中加载模板页面实体
        final TemplatePageEntity entity = docCtx.reader.getTemplate(oldId);
        final org.ofdrw.core.basicStructure.pageObj.Page pageObj = entity.getPage();
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
        // 给资源在新文档中分配ID
        long newId = ofdDoc.MaxUnitID.incrementAndGet();
        if (resObj instanceof CT_ColorSpace) {
            CT_ColorSpace cs = (CT_ColorSpace) resObj;
            cs.setObjID(newId);
            ST_Loc profile = cs.getProfile();
            if (profile != null) {
                // 复制资源到新的文档中
                Path filepath = rl.getFile(profile);
                profile = copyResFile(filepath);
                cs.setProfile(profile);
            }
            ofdDoc.prm.addRaw(cs);
        } else if (resObj instanceof CT_DrawParam) {
            CT_DrawParam dp = (CT_DrawParam) resObj;
            dp.setObjID(newId);
            ofdDoc.prm.addRaw(dp);
        } else if (resObj instanceof CT_Font) {
            CT_Font f = (CT_Font) resObj;
            f.setObjID(newId);
            ST_Loc fontFileLoc = f.getFontFile();
            if (fontFileLoc != null) {
                // 复制资源到新的文档中
                Path filepath = rl.getFile(fontFileLoc);
                fontFileLoc = copyResFile(filepath);
                f.setFontFile(fontFileLoc);
            }
            ofdDoc.prm.addRaw(f);
        } else if (resObj instanceof CT_MultiMedia) {
            CT_MultiMedia mm = (CT_MultiMedia) resObj;
            mm.setObjID(newId);
            ST_Loc mediaFileLoc = mm.getMediaFile();
            if (mediaFileLoc != null) {
                // 复制资源到新的文档中
                Path filepath = rl.getFile(mediaFileLoc);
                mediaFileLoc = copyResFile(filepath);
                mm.setMediaFile(mediaFileLoc);
            }

            ofdDoc.prm.addRaw(mm);
        } else if (resObj instanceof CT_VectorG) {
            CT_VectorG vg = (CT_VectorG) resObj;
            final Document d = DocumentHelper.createDocument();
            d.add(vg);

            // 矢量图像，等于一个DOM 运行迁移程序
            domMigrate(docCtx, vg);
            vg.setObjID(newId);
            ofdDoc.prm.addRaw(vg);
        }

        return newId;
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

    /**
     * 创建新页面
     *
     * @param pages    页面对象
     * @param pagesDir 页面容器
     * @return 新页面容器
     */
    private PageDir newPage(Pages pages, PagesDir pagesDir) {
        // 设置页面index与页面定位路径一致
        PageDir pageDir = pagesDir.newPageDir();
        String pageLoc = String.format("Pages/Page_%d/Content.xml", pageDir.getIndex());
        final Page page = new Page(this.ofdDoc.MaxUnitID.incrementAndGet(), pageLoc);
        pages.addPage(page);
        return pageDir;
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
