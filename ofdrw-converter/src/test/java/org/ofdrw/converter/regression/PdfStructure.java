package org.ofdrw.converter.regression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PDF 结构级指标：页数、书签、附件、注释
 * <p>
 * 设计第 7.2 节第 3 条要求比较页数、书签层级、附件数量与名称、注释数量。
 * 本类只负责“取数”，判断逻辑由使用方法自行决定（入库样本尚未转换到结构阶段时不做硬断言）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfStructure {

    /**
     * 页数
     */
    private final int pageCount;

    /**
     * 书签标题（按文档顺序，已展开层级）
     */
    private final List<String> outlines;

    /**
     * 附件文件名
     */
    private final List<String> attachments;

    /**
     * 各页注释数量合计
     */
    private final int annotationCount;

    /**
     * 文档标题，可为空
     */
    private final String title;

    /**
     * 创建工具（/Info Creator），可为空
     */
    private final String creator;

    /**
     * 创建时间原始字符串（未能解析时为 null，解析失败原因见 {@link #getMetadataWarnings()}）
     */
    private final String creationDate;

    /**
     * 元数据读取过程中的告警（例如日期格式非法）
     */
    private final List<String> metadataWarnings;

    /**
     * @param pageCount       页数
     * @param outlines        书签标题列表
     * @param attachments     附件文件名列表
     * @param annotationCount 注释数量
     */
    private PdfStructure(int pageCount, List<String> outlines, List<String> attachments, int annotationCount,
                         String title, String creator, String creationDate, List<String> metadataWarnings) {
        this.pageCount = pageCount;
        this.outlines = Collections.unmodifiableList(new ArrayList<>(outlines));
        this.attachments = Collections.unmodifiableList(new ArrayList<>(attachments));
        this.annotationCount = annotationCount;
        this.title = title;
        this.creator = creator;
        this.creationDate = creationDate;
        this.metadataWarnings = Collections.unmodifiableList(new ArrayList<>(metadataWarnings));
    }

    /**
     * 读取 PDF 结构信息
     *
     * @param pdf PDF 文件路径
     * @return 结构信息
     * @throws java.io.IOException 读取失败
     */
    public static PdfStructure of(java.nio.file.Path pdf) throws java.io.IOException {
        try (org.apache.pdfbox.pdmodel.PDDocument document =
                     org.apache.pdfbox.Loader.loadPDF(pdf.toFile())) {
            List<String> outlines = new ArrayList<>();
            org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline root =
                    document.getDocumentCatalog().getDocumentOutline();
            collectOutline(root, outlines);

            List<String> attachments = new ArrayList<>();
            org.apache.pdfbox.pdmodel.PDDocumentNameDictionary names =
                    document.getDocumentCatalog().getNames();
            if (names != null && names.getEmbeddedFiles() != null) {
                for (org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification spec
                        : names.getEmbeddedFiles().getNames().values()) {
                    attachments.add(spec.getFile() == null ? "?" : spec.getFile());
                }
            }

            int annots = 0;
            for (org.apache.pdfbox.pdmodel.PDPage page : document.getPages()) {
                annots += page.getAnnotations().size();
            }
            // 元数据：各字段独立容错——日期非法（如 D:20220708103442+02'34''）只降级该字段并记录告警，
            // 不能让一个坏日期导致整个结构读取失败（PDFBox 解析 PDF 日期会抛 DateTimeParseException）
            List<String> metadataWarnings = new ArrayList<>();
            String title = safeInfo(document, "title", metadataWarnings);
            String creator = safeInfo(document, "creator", metadataWarnings);
            String creationDate = safeInfo(document, "creationDate", metadataWarnings);
            return new PdfStructure(document.getNumberOfPages(), outlines, attachments, annots,
                    title, creator, creationDate, metadataWarnings);
        }
    }

    /**
     * 容错读取 PDF 文档信息字段
     * <p>
     * PDFBox 3 在读取 {@code /CreationDate} 等日期字段时解析失败会抛 {@code RuntimeException}
     * （{@code java.time.DateTimeParseException}）；这里统一捕获并记录告警，返回 null。
     *
     * @param document 文档
     * @param field    字段名：title / creator / creationDate
     * @param warnings 告警收集器
     * @return 字段值；缺失或非法时返回 null
     */
    private static String safeInfo(org.apache.pdfbox.pdmodel.PDDocument document, String field, List<String> warnings) {
        org.apache.pdfbox.pdmodel.PDDocumentInformation info = document.getDocumentInformation();
        if (info == null) {
            return null;
        }
        try {
            if ("title".equals(field)) {
                return info.getTitle();
            }
            if ("creator".equals(field)) {
                return info.getCreator();
            }
            java.lang.reflect.Method getter = null;
            for (java.lang.reflect.Method method : info.getClass().getMethods()) {
                if ("getCreationDate".equals(method.getName()) && method.getParameterTypes().length == 0) {
                    getter = method;
                    break;
                }
            }
            if (getter == null) {
                return null;
            }
            Object value;
            try {
                value = getter.invoke(info);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw new IllegalArgumentException(String.valueOf(e.getCause()), e.getCause());
            }
            if (value == null) {
                return null;
            }
            if (value instanceof java.util.Calendar) {
                return String.valueOf(((java.util.Calendar) value).getTimeInMillis());
            }
            return String.valueOf(value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnings.add("PDF 文档信息的 " + field + " 无法读取（已忽略）：" + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * 深度优先展开书签树
     *
     * @param node   书签节点，可为空
     * @param titles 收集书签标题
     */
    private static void collectOutline(
            org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode node,
            List<String> titles) {
        if (node == null) {
            return;
        }
        for (org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem item
                : node.children()) {
            titles.add(item.getTitle());
            collectOutline(item, titles);
        }
    }

    /**
     * @return 页数
     */
    public int getPageCount() {
        return pageCount;
    }

    /**
     * @return 书签标题列表
     */
    public List<String> getOutlines() {
        return outlines;
    }

    /**
     * @return 附件文件名列表
     */
    public List<String> getAttachments() {
        return attachments;
    }

    /**
     * @return 注释数量
     */
    public int getAnnotationCount() {
        return annotationCount;
    }

    /**
     * @return 文档标题，可为空
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return 创建工具（/Info Creator），可为空
     */
    public String getCreator() {
        return creator;
    }

    /**
     * @return 创建时间（毫秒时间戳字符串），缺失或无法解析时返回 null
     */
    public String getCreationDate() {
        return creationDate;
    }

    /**
     * @return 元数据读取告警（日期非法等）
     */
    public List<String> getMetadataWarnings() {
        return metadataWarnings;
    }

    /**
     * @return 便于日志输出的单行描述
     */
    @Override
    public String toString() {
        return "pages=" + pageCount + " outlines=" + outlines.size() + " attachments=" + attachments
                + " annots=" + annotationCount + (metadataWarnings.isEmpty() ? "" : " metadataWarnings=" + metadataWarnings);
    }
}
