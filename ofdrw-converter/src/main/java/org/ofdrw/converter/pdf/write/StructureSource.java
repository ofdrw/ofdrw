package org.ofdrw.converter.pdf.write;

import org.ofdrw.converter.pdf.render.PageRendererImpl;
import org.ofdrw.converter.pdf.structure.AnnotationMapper;
import org.ofdrw.converter.pdf.structure.AttachmentMapper;
import org.ofdrw.converter.pdf.structure.MetadataMapper;
import org.ofdrw.converter.pdf.structure.OutlineMapper;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.Annotations;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicStructure.outlines.CT_OutlineElem;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.reader.OFDReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构源数据：从 OFD 文档一次性取出 P5 阶段需要的全部结构信息
 * <p>
 * 抽取动机（与 {@code PdfStructure} 测试工具同源）：结构写出只依赖这份<b>纯数据快照</b>，
 * 因此可以在没有 OFD 文件、没有 PDF 文档的情况下测试映射与写出逻辑。
 * <p>
 * 页码映射约定：{@code pageIndexById} 的值是<b>导出顺序</b>中的页码（从 0 起）。
 * 只导出部分页时，被导出的第一张 OFD 页对应 PDF 第 1 页，
 * 因此 Goto/书签目标如果指向未导出的页，会被记为 warning 而不是产生越界目标。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class StructureSource {

    /**
     * OFD 页对象 ID → 导出顺序中的页码（从 0 起）
     */
    private final Map<String, Integer> pageIndexById;

    /**
     * 导出顺序中的页面（与页码一一对应），用于取目标页页面框
     */
    private final List<Page> pages;

    /**
     * OFD 大纲第一层元素
     */
    private final List<CT_OutlineElem> outlines;

    /**
     * 文档信息
     */
    private final CT_DocInfo docInfo;

    /**
     * 附件列表
     */
    private final List<CT_Attachment> attachments;

    /**
     * 注释：OFD 页对象 ID → 该页注释
     */
    private final Map<String, List<Annot>> annotationsByPageId;

    /**
     * 文档级动作（{@code Document#getActions()}），可为空
     */
    private final org.ofdrw.core.action.Actions documentActions;

    /**
     * OFD 读取器，用于页面未声明 {@code Area} 时回退文档级 {@code PageArea}，可为空
     */
    private OFDReader reader;

    /**
     * @param pageIndexById       页对象 ID → 页码（从 0 起）
     * @param pages               导出顺序中的页面
     * @param outlines            OFD 大纲第一层元素
     * @param docInfo             文档信息
     * @param attachments         附件列表
     * @param annotationsByPageId 页对象 ID → 注释
     * @param documentActions     文档级动作，可为空
     */
    public StructureSource(Map<String, Integer> pageIndexById, List<Page> pages, List<CT_OutlineElem> outlines,
                           CT_DocInfo docInfo, List<CT_Attachment> attachments,
                           Map<String, List<Annot>> annotationsByPageId,
                           org.ofdrw.core.action.Actions documentActions) {
        this.pageIndexById = pageIndexById == null ? new HashMap<String, Integer>() : pageIndexById;
        this.pages = pages == null ? new ArrayList<Page>() : pages;
        this.outlines = outlines == null ? new ArrayList<CT_OutlineElem>() : outlines;
        this.docInfo = docInfo;
        this.attachments = attachments == null ? new ArrayList<CT_Attachment>() : attachments;
        this.annotationsByPageId = annotationsByPageId == null
                ? new HashMap<String, List<Annot>>() : annotationsByPageId;
        this.documentActions = documentActions;
        this.reader = null;
    }

    /**
     * @param pageIndexById       页对象 ID → 页码（从 0 起）
     * @param pages               导出顺序中的页面
     * @param outlines            OFD 大纲第一层元素
     * @param docInfo             文档信息
     * @param attachments         附件列表
     * @param annotationsByPageId 页对象 ID → 注释
     * @param documentActions     文档级动作，可为空
     * @param reader              OFD 读取器（页面框回退文档级 PageArea 用），可为空
     */
    public StructureSource(Map<String, Integer> pageIndexById, List<Page> pages, List<CT_OutlineElem> outlines,
                           CT_DocInfo docInfo, List<CT_Attachment> attachments,
                           Map<String, List<Annot>> annotationsByPageId,
                           org.ofdrw.core.action.Actions documentActions, OFDReader reader) {
        this.pageIndexById = pageIndexById == null ? new HashMap<String, Integer>() : pageIndexById;
        this.pages = pages == null ? new ArrayList<Page>() : pages;
        this.outlines = outlines == null ? new ArrayList<CT_OutlineElem>() : outlines;
        this.docInfo = docInfo;
        this.attachments = attachments == null ? new ArrayList<CT_Attachment>() : attachments;
        this.annotationsByPageId = annotationsByPageId == null
                ? new HashMap<String, List<Annot>>() : annotationsByPageId;
        this.documentActions = documentActions;
        this.reader = reader;
    }

    /**
     * 从 OFD 读取器抽取结构源数据
     * <p>
     * 读取失败只记录并降级为空数据（结构映射失败不影响已渲染的页面内容），由调用方统一告警。
     *
     * @param reader       OFD 读取器，可为空（为空返回空快照）
     * @param exportedPageIds 本次导出的 OFD 页对象 ID 列表（按导出顺序），可为空表示全部页
     * @return 结构源数据
     */
    public static StructureSource load(OFDReader reader, Collection<String> exportedPageIds) {
        if (reader == null) {
            return new StructureSource(null, null, null, null, null, null, null);
        }
        List<Page> pages = new ArrayList<>();
        Map<String, Integer> index = new HashMap<>();
        Map<String, List<Annot>> annotations = new HashMap<>();
        int count = reader.getNumberOfPages();
        // OFDReader 的页码从 1 起；导出顺序即页码顺序（导出的子集按导出顺序重排）
        for (int i = 1; i <= count; i++) {
            String pageId;
            Page page;
            try {
                pageId = reader.getPageObjectId(i).toString();
                page = reader.getPage(i);
            } catch (RuntimeException e) {
                continue;
            }
            if (exportedPageIds != null && !exportedPageIds.contains(pageId)) {
                continue;
            }
            index.put(pageId, pages.size());
            pages.add(page);
        }
        // 注释：只保留被导出页的注释
        try {
            Map<String, List<Annot>> all = readAnnotations(reader);
            for (Map.Entry<String, List<Annot>> entry : all.entrySet()) {
                if (index.containsKey(entry.getKey())) {
                    annotations.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (RuntimeException ignored) {
            // 注释文件损坏：结构映射降级为“无注释”，不影响页面渲染
        }
        List<CT_OutlineElem> outlines = readOutlines(reader);
        CT_DocInfo docInfo = readDocInfo(reader);
        List<CT_Attachment> attachments = readAttachments(reader);
        org.ofdrw.core.action.Actions documentActions = null;
        try {
            Document document = reader.cdDefaultDoc();
            if (document != null) {
                documentActions = document.getActions();
            }
        } catch (Exception ignored) {
            // 文档级动作缺失不影响其它结构
        }
        return new StructureSource(index, pages, outlines, docInfo, attachments, annotations, documentActions, reader);
    }

    /**
     * 读取“页对象 ID → 注释列表”
     *
     * @param reader OFD 读取器
     * @return 注释映射；文档无注释时为空映射
     */
    private static Map<String, List<Annot>> readAnnotations(OFDReader reader) {
        Map<String, List<Annot>> result = new HashMap<>();
        Annotations annotations = reader.getAnnotations();
        if (annotations == null || annotations.getPages() == null) {
            return result;
        }
        for (org.ofdrw.core.annotation.pageannot.AnnPage annPage : annotations.getPages()) {
            if (annPage == null || annPage.getPageID() == null) {
                continue;
            }
            // 先登记“该页有注释文件”，正文稍后由 getAnnotationEntities() 填充
            result.put(annPage.getPageID().toString(), new ArrayList<Annot>());
        }
        // 注释正文通过 getAnnotationEntities() 获取（AnnotPage 只给出文件位置）
        for (org.ofdrw.reader.model.AnnotionEntity entity : reader.getAnnotationEntities()) {
            if (entity == null || entity.getPageId() == null) {
                continue;
            }
            List<Annot> list = result.get(entity.getPageId());
            if (list == null) {
                list = new ArrayList<>();
                result.put(entity.getPageId(), list);
            }
            if (entity.getAnnots() != null) {
                list.addAll(entity.getAnnots());
            }
        }
        return result;
    }

    /**
     * 读取 OFD 大纲
     *
     * @param reader OFD 读取器
     * @return 大纲元素；无大纲返回空列表
     */
    private static List<CT_OutlineElem> readOutlines(OFDReader reader) {
        try {
            Document document = reader.cdDefaultDoc();
            if (document == null || document.getOutlines() == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(document.getOutlines().getOutlineElems());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 读取文档信息
     *
     * @param reader OFD 读取器
     * @return 文档信息，可为空
     */
    private static CT_DocInfo readDocInfo(OFDReader reader) {
        try {
            org.ofdrw.core.basicStructure.ofd.DocBody body = reader.getOFDDir().getOfd().getDocBody();
            return body == null ? null : body.getDocInfo();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取附件列表
     *
     * @param reader OFD 读取器
     * @return 附件列表；无附件返回空列表
     */
    private static List<CT_Attachment> readAttachments(OFDReader reader) {
        List<CT_Attachment> attachments;
        try {
            attachments = new ArrayList<>(reader.getAttachmentList());
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
        // OFDReader#getAttachmentList 返回的是容器内绝对路径（如 /Doc_0/Res/a.jpg），
        // 这里映射到解压后的工作目录，供写入侧按真实文件系统路径读取内容
        java.nio.file.Path workDir = reader.getWorkDir();
        if (workDir == null) {
            return attachments;
        }
        for (CT_Attachment attachment : attachments) {
            ST_Loc loc = attachment.getFileLoc();
            if (loc == null) {
                continue;
            }
            String relative = loc.getLoc();
            while (relative.startsWith("/")) {
                relative = relative.substring(1);
            }
            java.nio.file.Path file = workDir.resolve(relative);
            if (java.nio.file.Files.isRegularFile(file)) {
                attachment.setFileLoc(new ST_Loc(file.toAbsolutePath().toString()));
            }
        }
        return attachments;
    }

    /**
     * @return 页对象 ID → 页码（从 0 起）
     */
    public Map<String, Integer> getPageIndexById() {
        return pageIndexById;
    }

    /**
     * @return 导出顺序中的页面
     */
    public List<Page> getPages() {
        return pages;
    }

    /**
     * @return OFD 大纲第一层元素
     */
    public List<CT_OutlineElem> getOutlines() {
        return outlines;
    }

    /**
     * @return 文档信息，可为空
     */
    public CT_DocInfo getDocInfo() {
        return docInfo;
    }

    /**
     * @return 附件列表
     */
    public List<CT_Attachment> getAttachments() {
        return attachments;
    }

    /**
     * @return 页对象 ID → 注释
     */
    public Map<String, List<Annot>> getAnnotationsByPageId() {
        return annotationsByPageId;
    }

    /**
     * @return 文档级动作，可为空
     */
    public org.ofdrw.core.action.Actions getDocumentActions() {
        return documentActions;
    }

    /**
     * 取指定页的页面框（mm）
     * <p>
     * 与页面渲染器保持同一口径：{@code MediaBox = CropBox = PhysicalBox}（决策 26）。
     *
     * @param pageIndex 页码（导出顺序，从 0 起）
     * @return 页面框；页码越界返回 A4
     * @deprecated 页面自身未声明 {@code Area} 时会回退 A4，与渲染口径不一致（表 27 应沿用文档级 PageArea）；
     * 请改用 {@link #pageBox(int)}（已绑定文档读取器）
     */
    @Deprecated
    public PageRendererImpl.PageBox pageBox(int pageIndex, OFDReader reader) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            return new PageRendererImpl.PageBox(0, 0, 210, 297);
        }
        return PageRendererImpl.pageBox(pages.get(pageIndex), reader);
    }

    /**
     * 取指定页的页面框（mm），页面自身未声明区域时回退文档级 {@code PageArea}
     * <p>
     * 书签/注释的目标坐标依赖页面框，必须与页面渲染器用同一份页面框，
     * 否则页面尺寸取自文档级区域、目标坐标却按 A4 计算，跳转会落在错误位置。
     *
     * @param pageIndex 页码（导出顺序，从 0 起）
     * @return 页面框；页码越界返回 A4
     */
    public PageRendererImpl.PageBox pageBox(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            return new PageRendererImpl.PageBox(0, 0, 210, 297);
        }
        // 必须传 reader：页面自身未声明 Area 时要沿用文档级 PageArea（表 27）
        return PageRendererImpl.pageBox(pages.get(pageIndex), reader);
    }

    /**
     * 取指定页的物理区域原值（便于测试断言）
     *
     * @param pageIndex 页码，从 0 起
     * @return 物理区域，可能为空
     */
    public ST_Box physicalBox(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            return null;
        }
        Page page = pages.get(pageIndex);
        if (page == null || page.getArea() == null) {
            return null;
        }
        return page.getArea().getPhysicalBox();
    }

    /**
     * 构造大纲映射器
     *
     * @return 大纲映射器
     */
    public OutlineMapper outlineMapper() {
        return new OutlineMapper(pageIndexById);
    }

    /**
     * 构造注释映射器
     *
     * @param bookmarkTargets 书签名 → 目标（来自大纲映射结果），可为空
     * @return 注释映射器
     */
    public AnnotationMapper annotationMapper(Map<String, org.ofdrw.converter.pdf.structure.PdfDestination> bookmarkTargets) {
        return new AnnotationMapper(pageIndexById, bookmarkTargets);
    }

    /**
     * 构造附件映射器
     *
     * @return 附件映射器
     */
    public AttachmentMapper attachmentMapper() {
        return new AttachmentMapper();
    }

    /**
     * 构造元数据映射器
     *
     * @return 元数据映射器
     */
    public MetadataMapper metadataMapper() {
        return new MetadataMapper();
    }
}
