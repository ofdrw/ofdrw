package org.ofdrw.converter.pdf.write;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionLaunch;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationRubberStamp;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationHighlight;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitHeightDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitRectangleDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.render.PageRendererImpl;
import org.ofdrw.converter.pdf.structure.AnnotationMapper;
import org.ofdrw.converter.pdf.structure.AttachmentMapper;
import org.ofdrw.converter.pdf.structure.MetadataMapper;
import org.ofdrw.converter.pdf.structure.OutlineMapper;
import org.ofdrw.converter.pdf.structure.PdfAction;
import org.ofdrw.converter.pdf.structure.PdfDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构写入器：把 P5 阶段的结构描述写入 PDF 文档
 * <p>
 * 与 {@code org.ofdrw.converter.pdf.structure} 的分工：映射器只产出库无关描述，
 * 本类负责“落到 PDFBox 对象上”（结构包与写入侧是唯一允许使用 PDFBox 的地方）。
 * <p>
 * <b>签名声明</b>：OFD 数字签名（含电子签章）<b>只转换外观</b>——印章/签名图片与文字按注释或图元照常绘制，
 * <b>不保留原签名的密码学有效性</b>，生成的 PDF 没有可验证的签名字段，也不含原签名值。
 * <p>
 * 调用顺序：全部页面渲染完成 → {@link #writeOutlines}、{@link #writeAnnotations}、
 * {@link #writeAttachments}、{@link #writeOpenAction}、{@link #writeMetadata} → 保存文档。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfStructureWriter {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(PdfStructureWriter.class);

    /**
     * 目标 PDF 文档
     */
    private final PDDocument document;

    /**
     * 结构源数据
     */
    private final StructureSource source;

    /**
     * 页码 → PDF 页面
     */
    private final List<PDPage> pdfPages;

    /**
     * 书签名 → 目标，供注释的 Goto 书签引用
     */
    private final Map<String, PdfDestination> bookmarkTargets = new HashMap<>();

    /**
     * 附件文件名 → 附件对象（供 GotoA 动作引用）
     */
    private final Map<String, PDComplexFileSpecification> attachmentsById = new HashMap<>();

    /**
     * PDF 页面 → OFD 页对象 ID（用于按页映射注释）
     */
    private final Map<PDPage, String> pageIdByPdfPage = new HashMap<>();

    /**
     * @param document   目标 PDF 文档，不能为空
     * @param source     结构源数据，不能为空
     * @param pdfPages   与导出顺序一一对应的 PDF 页面
     * @param pageIds    与导出顺序一一对应的 OFD 页对象 ID
     */
    public PdfStructureWriter(PDDocument document, StructureSource source, List<PDPage> pdfPages, List<String> pageIds) {
        if (document == null || source == null) {
            throw new IllegalArgumentException("PDF 文档与结构源数据不能为空");
        }
        if (pdfPages == null || pageIds == null || pdfPages.size() != pageIds.size()) {
            throw new IllegalArgumentException("页面与页对象 ID 数量不一致");
        }
        this.document = document;
        this.source = source;
        this.pdfPages = new ArrayList<>(pdfPages);
        for (int i = 0; i < pdfPages.size(); i++) {
            this.pageIdByPdfPage.put(pdfPages.get(i), pageIds.get(i));
        }
    }

    /**
     * 写入书签（决策 25：仅标题与目标，不做样式）
     *
     * @return 写入的书签数量
     */
    public int writeOutlines() {
        OutlineMapper mapper = source.outlineMapper();
        OutlineMapper.Result<OutlineMapper.BookmarkNode> result = mapper.map(source.getOutlines());
        warnAll(mapper.getName(), result.getWarnings());
        if (result.isEmpty()) {
            collectBookmarkTargetsFromOutline(mapper);
            return 0;
        }
        PDDocumentOutline outline = new PDDocumentOutline();
        document.getDocumentCatalog().setDocumentOutline(outline);
        int count = 0;
        for (OutlineMapper.BookmarkNode node : result.getItems()) {
            count += appendNode(outline, node);
        }
        collectBookmarkTargetsFromOutline(mapper);
        return count;
    }

    /**
     * 收集书签名 → 目标，供注释中的 {@code Goto(Bookmark)} 引用
     *
     * @param mapper 大纲映射器
     */
    private void collectBookmarkTargetsFromOutline(OutlineMapper mapper) {
        OutlineMapper.Result<OutlineMapper.BookmarkNode> again = mapper.map(source.getOutlines());
        for (OutlineMapper.BookmarkNode node : again.getItems()) {
            collectBookmarkTargets(node);
        }
    }

    /**
     * 递归收集书签目标
     *
     * @param node 书签节点
     */
    private void collectBookmarkTargets(OutlineMapper.BookmarkNode node) {
        if (node.getDestination() != null) {
            bookmarkTargets.put(node.getTitle(), node.getDestination());
        }
        for (OutlineMapper.BookmarkNode child : node.getChildren()) {
            collectBookmarkTargets(child);
        }
    }

    /**
     * 递归追加书签节点
     *
     * @param parent 父节点
     * @param node   书签节点
     * @return 追加的节点数量（含子节点）
     */
    private int appendNode(PDOutlineNode parent, OutlineMapper.BookmarkNode node) {
        PDOutlineItem item = new PDOutlineItem();
        item.setTitle(node.getTitle());
        PDPageDestination destination = toPdfDestination(node.getDestination());
        if (destination != null) {
            item.setDestination(destination);
        }
        parent.addLast(item);
        int count = 1;
        for (OutlineMapper.BookmarkNode child : node.getChildren()) {
            count += appendNode(item, child);
        }
        return count;
    }

    /**
     * 写入页面注释（外观已由页面渲染重绘，这里只写交互注释）
     *
     * @return 写入的注释数量
     */
    public int writeAnnotations() {
        int total = 0;
        for (PDPage page : pdfPages) {
            String pageId = pageIdByPdfPage.get(page);
            List<org.ofdrw.core.annotation.pageannot.Annot> annots = source.getAnnotationsByPageId().get(pageId);
            if (annots == null || annots.isEmpty()) {
                continue;
            }
            AnnotationMapper mapper = source.annotationMapper(bookmarkTargets);
            AnnotationMapper.Result<AnnotationMapper.AnnotationTarget> result = mapper.map(annots);
            warnAll(mapper.getName() + "(page=" + pageId + ")", result.getWarnings());
            PageRendererImpl.PageBox pageBox = source.pageBox(pdfPages.indexOf(page));
            for (AnnotationMapper.AnnotationTarget target : result.getItems()) {
                try {
                    PDAnnotation annotation = toPdfAnnotation(target, pageBox);
                    if (annotation != null) {
                        page.getAnnotations().add(annotation);
                        total++;
                    }
                } catch (IOException e) {
                    logger.warn("[{}] 注释写入失败，已跳过：ID={}，原因：{}", "P5", target.getAnnotId(), e.getMessage());
                }
            }
        }
        return total;
    }

    /**
     * 写入附件（/Names /EmbeddedFiles，保留文件名、创建时间与 MIME）
     *
     * @return 写入的附件数量
     */
    public int writeAttachments() {
        AttachmentMapper mapper = source.attachmentMapper();
        AttachmentMapper.Result<AttachmentMapper.PdfAttachment> result = mapper.map(source.getAttachments());
        warnAll(mapper.getName(), result.getWarnings());
        int count = 0;
        for (AttachmentMapper.PdfAttachment attachment : result.getItems()) {
            try {
                PDEmbeddedFile embedded = buildEmbeddedFile(attachment);
                if (embedded == null) {
                    continue;
                }
                PDComplexFileSpecification spec = new PDComplexFileSpecification();
                spec.setFile(attachment.getFileName());
                spec.setFileUnicode(attachment.getFileName());
                spec.setEmbeddedFile(embedded);
                spec.setEmbeddedFileUnicode(embedded);
                attachmentsById.put(attachment.getFileName(), spec);
                if (document.getDocumentCatalog().getNames() == null) {
                    document.getDocumentCatalog().setNames(
                            new org.apache.pdfbox.pdmodel.PDDocumentNameDictionary(document.getDocumentCatalog()));
                }
                org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode treeNode =
                        document.getDocumentCatalog().getNames().getEmbeddedFiles();
                if (treeNode == null) {
                    treeNode = new org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode();
                    document.getDocumentCatalog().getNames().setEmbeddedFiles(treeNode);
                }
                treeNode.setNames(merge(treeNode.getNames(), attachment.getFileName(), spec));
                count++;
            } catch (IOException e) {
                logger.warn("[{}] 附件写入失败，已跳过：{}，原因：{}", "P5", attachment.getFileName(), e.getMessage());
            }
        }
        return count;
    }

    /**
     * 合并附件名称字典
     *
     * @param existing 已有字典，可为空
     * @param name     名称
     * @param spec     附件对象
     * @return 合并后的字典
     */
    private static Map<String, PDComplexFileSpecification> merge(Map<String, PDComplexFileSpecification> existing,
                                                                 String name, PDComplexFileSpecification spec) {
        Map<String, PDComplexFileSpecification> map = new HashMap<>();
        if (existing != null) {
            map.putAll(existing);
        }
        map.put(name, spec);
        return map;
    }

    /**
     * 构造嵌入文件
     *
     * @param attachment 附件描述
     * @return 嵌入文件；无法读取内容时返回 null 并告警
     * @throws IOException 读取失败
     */
    private PDEmbeddedFile buildEmbeddedFile(AttachmentMapper.PdfAttachment attachment) throws IOException {
        if (attachment.getFilePath() == null) {
            logger.warn("[{}] 附件缺少文件路径，已跳过：{}", "P5", attachment.getFileName());
            return null;
        }
        Path path = Paths.get(attachment.getFilePath());
        if (!Files.isRegularFile(path)) {
            logger.warn("[{}] 附件路径不是可读文件，已跳过：{}", "P5", attachment.getFilePath());
            return null;
        }
        byte[] data = Files.readAllBytes(path);
        PDEmbeddedFile embedded = new PDEmbeddedFile(document, new java.io.ByteArrayInputStream(data));
        embedded.setSubtype(attachment.getMimeType());
        embedded.setSize(data.length);
        if (isWritableDate(attachment.getCreationDate(), attachment.getFileName(), "创建")) {
            embedded.setCreationDate(toCalendar(attachment.getCreationDate()));
        }
        if (isWritableDate(attachment.getModDate(), attachment.getFileName(), "修改")) {
            embedded.setModDate(toCalendar(attachment.getModDate()));
        }
        return embedded;
    }

    /**
     * 写入文档级打开动作（{@code /OpenAction}）
     *
     * @return 是否写入
     */
    public boolean writeOpenAction() {
        org.ofdrw.core.action.Actions actions = source.getDocumentActions();
        if (actions == null) {
            return false;
        }
        for (org.ofdrw.core.action.CT_Action ctAction : actions.getActions()) {
            if (ctAction == null || ctAction.getAction() == null) {
                continue;
            }
            Object action = ctAction.getAction();
            if (action instanceof org.ofdrw.core.action.actionType.Sound
                    || action instanceof org.ofdrw.core.action.actionType.actionMovie.Movie) {
                logger.warn("[{}] 不支持 Sound/Movie 动作，已跳过（决策 18）：{}", "P5",
                        action.getClass().getSimpleName());
                continue;
            }
            if (action instanceof org.ofdrw.core.action.actionType.actionGoto.Goto) {
                PDPageDestination dest = toPdfDestination(mapGotoTarget(
                        (org.ofdrw.core.action.actionType.actionGoto.Goto) action));
                if (dest != null) {
                    document.getDocumentCatalog().setOpenAction(dest);
                    return true;
                }
            } else if (action instanceof org.ofdrw.core.action.actionType.URI) {
                PDActionURI uri = new PDActionURI();
                uri.setURI(((org.ofdrw.core.action.actionType.URI) action).getURI());
                document.getDocumentCatalog().setOpenAction(uri);
                return true;
            }
        }
        return false;
    }

    /**
     * 解析文档级 Goto 目标
     *
     * @param gotoAction Goto 动作
     * @return 目标，可为空
     */
    private PdfDestination mapGotoTarget(org.ofdrw.core.action.actionType.actionGoto.Goto gotoAction) {
        try {
            org.ofdrw.core.action.actionType.actionGoto.OFDGotoTarget target = gotoAction.getTarget();
            if (target instanceof org.ofdrw.core.action.actionType.actionGoto.Bookmark) {
                return bookmarkTargets.get(
                        ((org.ofdrw.core.action.actionType.actionGoto.Bookmark) target).getBookmarkName());
            }
            org.ofdrw.core.action.actionType.actionGoto.CT_Dest dest =
                    (org.ofdrw.core.action.actionType.actionGoto.CT_Dest) target;
            Integer pageIndex = dest.getPageID() == null ? null
                    : source.getPageIndexById().get(dest.getPageID().toString());
            PdfDestination.Type type = OutlineMapper.toPdfType(dest.getType());
            if (pageIndex == null || type == null) {
                return null;
            }
            return PdfDestination.ofdMm(type, pageIndex, dest.getLeft(), dest.getTop(),
                    dest.getRight(), dest.getBottom(), dest.getZoom());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 写入文档信息（{@code /Info}）
     *
     * @return 文档信息描述
     */
    public MetadataMapper.PdfMetadata writeMetadata() {
        MetadataMapper mapper = source.metadataMapper();
        MetadataMapper.Result<MetadataMapper.PdfMetadata> result = mapper.map(source.getDocInfo());
        warnAll(mapper.getName(), result.getWarnings());
        MetadataMapper.PdfMetadata metadata = result.getItems().get(0);
        org.apache.pdfbox.pdmodel.PDDocumentInformation info = document.getDocumentInformation();
        if (metadata.getTitle() != null) {
            info.setTitle(metadata.getTitle());
        }
        if (metadata.getAuthor() != null) {
            info.setAuthor(metadata.getAuthor());
        }
        if (metadata.getSubject() != null) {
            info.setSubject(metadata.getSubject());
        }
        if (metadata.getKeywords() != null) {
            info.setKeywords(metadata.getKeywords());
        }
        if (metadata.getCreator() != null) {
            info.setCreator(metadata.getCreator());
        }
        info.setProducer(metadata.getProducer());
        if (isWritableDate(metadata.getCreationDate(), "文档信息", "创建")) {
            info.setCreationDate(toCalendar(metadata.getCreationDate()));
        }
        if (isWritableDate(metadata.getModDate(), "文档信息", "修改")) {
            info.setModificationDate(toCalendar(metadata.getModDate()));
        }
        return metadata;
    }

    /**
     * 一次性写入全部结构
     *
     * @return 各结构写入数量，键为结构名
     */
    public Map<String, Integer> writeAll() {
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        counts.put("outlines", writeOutlines());
        counts.put("annotations", writeAnnotations());
        counts.put("attachments", writeAttachments());
        counts.put("openAction", writeOpenAction() ? 1 : 0);
        writeMetadata();
        return counts;
    }

    /**
     * 构造 PDF 注释
     *
     * @param target  注释目标描述
     * @param pageBox 该注释所在页的页面框（mm）
     * @return PDF 注释；热区无效或类型无需生成注释时返回 null
     */
    private PDAnnotation toPdfAnnotation(AnnotationMapper.AnnotationTarget target, PageRendererImpl.PageBox pageBox) {
        double[] rect = target.toPdfRect(pageBox.getY0(), pageBox.getHeight());
        if (rect == null) {
            logger.warn("[{}] 注释外接矩形无效，未生成 PDF 注释：ID={}", "P5", target.getAnnotId());
            return null;
        }
        if ("Link".equals(target.getPdfAnnotationType())) {
            if (target.getAction() == null) {
                // 无交互动作的 Link 注释没有可映射的 PDF 语义，外观已由页面渲染绘制
                return null;
            }
            org.apache.pdfbox.pdmodel.interactive.action.PDAction pdfAction = toPdfAction(target.getAction());
            if (pdfAction == null) {
                // 动作无法映射（如 GotoA 引用的附件不存在）：不生成空动作的链接注释，外观已由页面渲染绘制
                return null;
            }
            PDAnnotationLink link = new PDAnnotationLink();
            applyRectangle(link, rect);
            link.setAction(pdfAction);
            PDBorderStyleDictionary border = new PDBorderStyleDictionary();
            border.setWidth(0);
            link.setBorderStyle(border);
            return link;
        }
        PDAnnotation annotation;
        if ("Highlight".equals(target.getPdfAnnotationType())) {
            PDAnnotationHighlight highlight = new PDAnnotationHighlight();
            highlight.setQuadPoints(new float[]{
                    (float) rect[0], (float) rect[3], (float) rect[2], (float) rect[3],
                    (float) rect[0], (float) rect[1], (float) rect[2], (float) rect[1]});
            annotation = highlight;
        } else {
            annotation = new PDAnnotationRubberStamp();
        }
        applyRectangle(annotation, rect);
        annotation.setPrinted(true);
        // 外观已按 Appearance 重绘到页面内容：这里不再设置外观流，避免与页面内容重复绘制
        if (target.getRemark() != null && !target.getRemark().trim().isEmpty()) {
            annotation.setContents(target.getRemark());
        }
        return annotation;
    }

    /**
     * 设置注释矩形（无边框样式，避免阅读器额外描边）
     *
     * @param annotation 注释
     * @param rect       矩形 {@code [llx, lly, urx, ury]}，单位 pt
     */
    private static void applyRectangle(PDAnnotation annotation, double[] rect) {
        PDRectangle rectangle = new PDRectangle();
        rectangle.setLowerLeftX((float) rect[0]);
        rectangle.setLowerLeftY((float) rect[1]);
        rectangle.setUpperRightX((float) rect[2]);
        rectangle.setUpperRightY((float) rect[3]);
        annotation.setRectangle(rectangle);
        annotation.setHidden(false);
        annotation.setInvisible(false);
    }

    /**
     * 构造 PDF 动作
     *
     * @param action 动作描述
     * @return PDF 动作；不支持的动作返回 null 并告警
     */
    private org.apache.pdfbox.pdmodel.interactive.action.PDAction toPdfAction(PdfAction action) {
        switch (action.getType()) {
            case URI: {
                PDActionURI uri = new PDActionURI();
                uri.setURI(action.getUri());
                return uri;
            }
            case GOTO: {
                PDPageDestination dest = toPdfDestination(action.getDestination());
                if (dest == null) {
                    return null;
                }
                PDActionGoTo goTo = new PDActionGoTo();
                goTo.setDestination(dest);
                return goTo;
            }
            case GOTO_A: {
                PDComplexFileSpecification spec = attachmentsById.get(action.getAttachmentId());
                if (spec == null) {
                    logger.warn("[{}] GotoA 引用的附件不存在，已跳过：{}", "P5", action.getAttachmentId());
                    return null;
                }
                PDActionLaunch launch = new PDActionLaunch();
                launch.setFile(spec);
                return launch;
            }
            case SOUND:
            case MOVIE:
            default:
                logger.warn("[{}] 不支持 Sound/Movie 动作，已跳过（决策 18）：{}", "P5", action.getType());
                return null;
        }
    }

    /**
     * 目标描述 → PDF 页面目标
     * <p>
     * Y 翻转在目标页页面框内完成（决策 1：换算集中在拿到页面尺寸后进行）。
     *
     * @param destination 目标描述
     * @return PDF 目标；目标页不存在时返回 null
     */
    public PDPageDestination toPdfDestination(PdfDestination destination) {
        if (destination == null) {
            return null;
        }
        int pageIndex = destination.getPageIndex();
        if (pageIndex < 0 || pageIndex >= pdfPages.size()) {
            logger.warn("[{}] 目标页不存在（可能导出的是页面子集），已跳过：页码={}", "P5", pageIndex + 1);
            return null;
        }
        PageRendererImpl.PageBox box = source.pageBox(pageIndex);
        PdfDestination pdf = destination.toPdfCoordinates(box.getY0(), box.getHeight());
        PDPage page = pdfPages.get(pageIndex);
        PDPageDestination result;
        switch (pdf.getType()) {
            case XYZ: {
                PDPageXYZDestination xyz = new PDPageXYZDestination();
                xyz.setPage(page);
                if (pdf.getLeft() != null) {
                    xyz.setLeft((int) Math.round(pdf.getLeft()));
                }
                if (pdf.getTop() != null) {
                    xyz.setTop((int) Math.round(pdf.getTop()));
                }
                if (pdf.getZoom() != null) {
                    xyz.setZoom((int) Math.round(pdf.getZoom() * 100d));
                }
                result = xyz;
                break;
            }
            case FIT_H: {
                PDPageFitWidthDestination fitH = new PDPageFitWidthDestination();
                fitH.setPage(page);
                if (pdf.getTop() != null) {
                    fitH.setTop((int) Math.round(pdf.getTop()));
                }
                result = fitH;
                break;
            }
            case FIT_V: {
                PDPageFitHeightDestination fitV = new PDPageFitHeightDestination();
                fitV.setPage(page);
                if (pdf.getLeft() != null) {
                    fitV.setLeft((int) Math.round(pdf.getLeft()));
                }
                result = fitV;
                break;
            }
            case FIT_R: {
                PDPageFitRectangleDestination fitR = new PDPageFitRectangleDestination();
                fitR.setPage(page);
                if (pdf.getLeft() != null && pdf.getBottom() != null && pdf.getRight() != null && pdf.getTop() != null) {
                    fitR.setLeft((int) Math.round(pdf.getLeft()));
                    fitR.setBottom((int) Math.round(pdf.getBottom()));
                    fitR.setRight((int) Math.round(pdf.getRight()));
                    fitR.setTop((int) Math.round(pdf.getTop()));
                }
                result = fitR;
                break;
            }
            case FIT:
            default: {
                PDPageFitDestination fit = new PDPageFitDestination();
                fit.setPage(page);
                result = fit;
                break;
            }
        }
        return result;
    }

    /**
     * 判断日期能否安全写入 PDF
     * <p>
     * PDFBox 读取 PDF 日期（{@code D:YYYYMMDD...}）时使用 {@code java.time} 解析，遇到非法值会抛
     * {@code DateTimeParseException}（JDK 8 同样如此）。OFD 样本中存在 {@code 2021-6-4}、
     * {@code D:20220708103442+02'34''} 这类非 ISO 形态的日期字符串，若原样写入，
     * 会导致后续按 PDF 元数据读取日期时解析失败，因此这里统一校验：只写入能被本地时间完整表示的值。
     *
     * @param dateTime 待写入的本地时间，可为空
     * @param owner    所属对象（附件名或“文档信息”），用于告警
     * @param field    字段名（创建/修改），用于告警
     * @return 可以写入返回 true；为空或非法返回 false 并告警
     */
    private static boolean isWritableDate(LocalDateTime dateTime, String owner, String field) {
        if (dateTime == null) {
            return false;
        }
        try {
            // 触发与 PDFBox 一致的取值范围校验（年份/月/日必须在合法区间）
            dateTime.toLocalDate().atStartOfDay();
            dateTime.atZone(ZoneId.systemDefault()).toInstant();
            return true;
        } catch (RuntimeException e) {
            logger.warn("[{}] {}的{}日期无法写入 PDF，已忽略：{}", "P5", owner, field, dateTime);
            return false;
        }
    }

    /**
     * 本地时间 → Calendar
     *
     * @param dateTime 本地时间
     * @return 日历对象
     */
    private static Calendar toCalendar(LocalDateTime dateTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(java.util.Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
        return calendar;
    }

    /**
     * 输出映射告警
     *
     * @param mapperName 映射器名称
     * @param warnings   告警
     */
    private static void warnAll(String mapperName, List<String> warnings) {
        for (String warning : warnings) {
            logger.warn("[{}] {}：{}", "P5", mapperName, warning);
        }
    }
}
