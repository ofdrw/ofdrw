package org.ofdrw.tool.merge;


import org.dom4j.DocumentException;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.pageTree.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicStructure.res.OFDResource;
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
    private final Map<String, OFDResource> resOldNewMap;

    private final Path dest;


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
                // TODO: 页面模板的迁移的替换

                // 通过XML 选中与资源有关对象

                /**
                 *
                 * - Layer 的 DrawParam
                 * - 每个图像对象都可能含有 DrawParam 引用
                 * - Color 中 Pattern CellContent Thumbnail 引用
                 * - Image 中 ResourceID、Substitution、ImageMask
                 * - Text 中 Font
                 * - Composite 复合对象 中 ResourceID
                 * Res资源中的 CompositeGraphUnit CT_VectorG：Thumbnail、Substitution
                 */

            }

        }
    }

    /**
     * 创建新页面
     *
     * @param pages     页面对象
     * @param pagesDir  页面容器
     * @param maxUnitID 最大记录ID
     * @return 新页面容器
     */
    private PageDir newPage(Pages pages, PagesDir pagesDir, AtomicInteger maxUnitID) {
        // 设置页面index与页面定位路径一致
        PageDir pageDir = pagesDir.newPageDir();
        String pageLoc = String.format("Pages/Page_%d/Content.xml", pageDir.getIndex());
        final Page page = new Page(maxUnitID.incrementAndGet(), pageLoc);
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
