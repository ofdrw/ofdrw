package org.ofdrw.tool.merge;


import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.pageTree.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.compositeObj.CT_VectorG;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.pkg.container.PageDir;
import org.ofdrw.pkg.container.PagesDir;

import java.io.Closeable;
import java.io.IOException;
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
     * 新旧映射表
     * <p>
     * | 原文档 ID | (新文档ID, 资源对象) |
     * <p>
     * Key: 旧ID
     * Value: 资源对象（ID替换为新文档中的ID）
     */
    private final Map<String, OFDElement> resOldNewMap;

    /**
     * 合并后生成文档位置
     */
    private final Path dest;

    /**
     * 合并的目标文档，仅在合并时设置
     * 在合并完成后将会被打包存储
     */
    private BareOFDDoc ofdDoc;


    public OFDMerger(Path dest) {
        if (dest == null) {
            throw new IllegalArgumentException("合并结果路径(dest)为空");
        }
        pageArr = new ArrayList<>(10);
        docCtxMap = new HashMap<>();
        this.dest = dest;
        if (!Files.exists(dest.getParent())) {
            throw new IllegalArgumentException("OFD文件存储路径(dest)上级目录 [" + dest.getParent().toAbsolutePath() + "] 不存在");
        }
        resOldNewMap = new HashMap<>();

    }


    /**
     * 向合并文件中添加页面
     *
     * @param filepath    待合并的OFD文件路径
     * @param pageIndexes 页面序列
     * @return this
     */
    public OFDMerger add(Path filepath, int... pageIndexes) throws IOException {
        if (pageIndexes == null || pageIndexes.length == 0) {
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
                final CT_PageArea docDefaultArea = pageEntry.docCtx.getDefaultArea(0);

                // 解析原OFD页面的Content.xml 为Page对象
                final org.ofdrw.core.basicStructure.pageObj.Page page
                        = pageEntry.docCtx.reader.getPage(pageEntry.pageIndex);
                // 若当前页面的页面区域的大小和位置为空，则使用文档默认的尺寸
                if (page.getArea() == null) {
                    page.setArea(docDefaultArea);
                }
                // 创建页面容器
                final PageDir pageDir = newPage(pages, pagesDir);
                // TODO: 页面模板的迁移的替换

                // 通过XML 选中与资源有关对象，并实现资源迁移和引用替换
                domMigrate(pageEntry.docCtx, page);
                // 把替换后得到页面放入页面容器中
                pageDir.setContent(page);
            }

        }
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
     */
    private void domMigrate(DocContext docCtx, Element dom) {
        /*
         * - Layer 的 DrawParam
         * - 每个图像对象都可能含有 DrawParam 引用
         * - Color 中 Pattern CellContent Thumbnail 引用
         * - Image 中 ResourceID、Substitution、ImageMask
         * - Text 中 Font
         * - Composite 复合对象 中 ResourceID
         * Res资源中的 CompositeGraphUnit CT_VectorG：Thumbnail、Substitution
         */
        String[] attrNames = new String[]{
                "Font",
                "ResourceID",
                "Substitution", "ImageMask", "Thumbnail",
                "DrawParam"
        };
        List<Node> nodes;
        for (String attrName : attrNames) {
            nodes = dom.selectNodes(String.format("//*[%s]", attrName));
            if (nodes.isEmpty()) {
                continue;
            }
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
        final List<Node> objArr = dom.selectNodes("//*[ID]");
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
     */
    private long resMigrate(DocContext docCtx, String oldResId) {
        final OFDElement resObj = docCtx.resMgt.get(oldResId);
        if (resObj == null) {
            return 0;
        }
        // 检查缓存，防止重复迁移
        final OFDElement cache = resOldNewMap.get(oldResId);
        if (cache != null) {
            return cache.getObjID().getId();
        }
        // 给资源在新文档中分配ID
        long id = ofdDoc.MaxUnitID.incrementAndGet();
        if (resObj instanceof CT_ColorSpace) {
            CT_ColorSpace cs = (CT_ColorSpace) resObj;
            cs.setObjID(id);
            // TODO:
            ofdDoc.prm.addRaw(cs);
        } else if (resObj instanceof CT_DrawParam) {
            CT_DrawParam dp = (CT_DrawParam) resObj;
            dp.setObjID(id);
            // TODO:
            ofdDoc.prm.addRaw(dp);
        } else if (resObj instanceof CT_Font) {
            CT_Font f = (CT_Font) resObj;
            f.setObjID(ofdDoc.MaxUnitID.incrementAndGet());
            // TODO:
            ofdDoc.prm.addRaw(f);
        } else if (resObj instanceof CT_MultiMedia) {
            CT_MultiMedia mm = (CT_MultiMedia) resObj;
            mm.setObjID(id);
            // TODO:
            ofdDoc.prm.addRaw(mm);
        } else if (resObj instanceof CT_VectorG) {
            CT_VectorG vg = (CT_VectorG) resObj;
            vg.setObjID(id);
            // 矢量图像，等于一个DOM 运行迁移程序
            domMigrate(docCtx, vg);
            ofdDoc.prm.addRaw(vg);
        }
        return id;
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
