package org.ofdrw.converter.pdf.structure;

import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 元数据映射器：OFD 文档属性 → PDF /Info
 * <p>
 * 字段对应（OFD {@code CT_DocInfo} → PDF 文档信息字典）：
 * <table border="1">
 *     <tr><th>OFD</th><th>PDF /Info</th></tr>
 *     <tr><td>{@code Title}</td><td>{@code /Title}</td></tr>
 *     <tr><td>{@code Author}</td><td>{@code /Author}</td></tr>
 *     <tr><td>{@code Subject}</td><td>{@code /Subject}</td></tr>
 *     <tr><td>{@code Keywords}</td><td>{@code /Keywords}（逗号分隔）</td></tr>
 *     <tr><td>{@code Creator} + {@code CreatorVersion}</td><td>{@code /Creator}（“名称 版本”）</td></tr>
 *     <tr><td>{@code CreationDate}</td><td>{@code /CreationDate}</td></tr>
 *     <tr><td>{@code ModDate}</td><td>{@code /ModDate}</td></tr>
 *     <tr><td>—</td><td>{@code /Producer}（本库固定值，见 {@link #PRODUCER}）</td></tr>
 * </table>
 * <p>
 * 不写 XMP（{@code /Metadata}）：首版只保证 {@code /Info} 可被常规阅读器读取，
 * 完整 XMP 包需要额外的命名空间与属性声明，属可选的增强项（设计 3.8 节“写入 Creator/Producer/日期”即可满足）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class MetadataMapper implements StructureMapper {

    /**
     * PDF {@code /Producer} 取值：固定标明本库及版本
     */
    public static final String PRODUCER = "OFDRW (org.ofdrw.converter)";

    /**
     * @return 映射器名称
     */
    @Override
    public String getName() {
        return "MetadataMapper";
    }

    /**
     * 把 OFD 文档信息映射为 PDF 文档信息
     *
     * @param docInfo OFD 文档信息，可为空
     * @return 文档信息描述；输入为空时仍返回含 Producer 的结果
     */
    public Result<PdfMetadata> map(CT_DocInfo docInfo) {
        List<String> warnings = new ArrayList<>();
        if (docInfo == null) {
            return new Result<>(java.util.Collections.singletonList(new PdfMetadata(null, null, null, null, null,
                    PRODUCER, null, null)), warnings);
        }
        String creator = docInfo.getCreator();
        String creatorVersion = docInfo.getCreatorVersion();
        String creatorText = creator == null ? null
                : (creatorVersion == null || creatorVersion.trim().isEmpty() ? creator : creator + " " + creatorVersion);

        // core 的 Keywords 以多个 "Keyword" 子节点表达，但读取端按 "Keywords" 取；
        // 这里直接扫描 Keywords 元素下的全部非空文本节点，兼容两种写法
        String keywords = null;
        if (docInfo.getKeywords() != null) {
            List<String> list = new ArrayList<>();
            for (Object node : docInfo.getKeywords().content()) {
                if (node instanceof org.dom4j.Element) {
                    String text = ((org.dom4j.Element) node).getTextTrim();
                    if (!text.isEmpty()) {
                        list.add(text);
                    }
                }
            }
            if (!list.isEmpty()) {
                keywords = String.join(",", list);
            }
        }

        // 日期非法（非 yyyy-MM-dd 写法，例如 "2021-6-4" 或 PDF 风格的 "D:20220708..."）只降级为不写日期，
        // 不得中断整份文档的转换（设计约束 3：结构映射失败不影响已渲染的页面）
        LocalDateTime creation = safeDateTime(docInfo, true, warnings);
        LocalDateTime mod = safeDateTime(docInfo, false, warnings);

        PdfMetadata metadata = new PdfMetadata(docInfo.getTile(), docInfo.getAuthor(), docInfo.getSubject(),
                keywords, creatorText, PRODUCER, creation, mod);
        if (metadata.getTitle() == null && metadata.getAuthor() == null) {
            warnings.add("OFD 文档信息缺少标题与作者，PDF 仅写入 Producer 与日期");
        }
        return new Result<>(java.util.Collections.singletonList(metadata), warnings);
    }

    /**
     * 读取文档日期，非法格式降级为空并记录告警
     *
     * @param docInfo  文档信息
     * @param creation true 取创建日期，false 取修改日期
     * @param warnings 告警收集器
     * @return 当日零点；日期缺失或非法时返回 null
     */
    private static LocalDateTime safeDateTime(CT_DocInfo docInfo, boolean creation, List<String> warnings) {
        try {
            return toDateTime(creation ? docInfo.getCreationDate() : docInfo.getModDate());
        } catch (RuntimeException e) {
            warnings.add("文档" + (creation ? "创建" : "修改") + "日期格式非法，已忽略：" + e.getMessage());
            return null;
        }
    }

    /**
     * OpenDate（OFD 仅日期）→ 本地时间
     *
     * @param date 日期，可为空
     * @return 当日零点；入参为空返回 null
     */
    static LocalDateTime toDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * PDF 文档信息（库无关）
     */
    public static final class PdfMetadata {

        /**
         * 标题，可为空
         */
        private final String title;

        /**
         * 作者，可为空
         */
        private final String author;

        /**
         * 主题，可为空
         */
        private final String subject;

        /**
         * 关键字（逗号分隔），可为空
         */
        private final String keywords;

        /**
         * 创建工具，可为空
         */
        private final String creator;

        /**
         * 生成工具，固定为本库
         */
        private final String producer;

        /**
         * 创建时间，可为空
         */
        private final LocalDateTime creationDate;

        /**
         * 修改时间，可为空
         */
        private final LocalDateTime modDate;

        /**
         * @param title        标题，可为空
         * @param author       作者，可为空
         * @param subject      主题，可为空
         * @param keywords     关键字，可为空
         * @param creator      创建工具，可为空
         * @param producer     生成工具
         * @param creationDate 创建时间，可为空
         * @param modDate      修改时间，可为空
         */
        public PdfMetadata(String title, String author, String subject, String keywords, String creator,
                           String producer, LocalDateTime creationDate, LocalDateTime modDate) {
            this.title = title;
            this.author = author;
            this.subject = subject;
            this.keywords = keywords;
            this.creator = creator;
            this.producer = producer;
            this.creationDate = creationDate;
            this.modDate = modDate;
        }

        /**
         * @return 标题，可为空
         */
        public String getTitle() {
            return title;
        }

        /**
         * @return 作者，可为空
         */
        public String getAuthor() {
            return author;
        }

        /**
         * @return 主题，可为空
         */
        public String getSubject() {
            return subject;
        }

        /**
         * @return 关键字（逗号分隔），可为空
         */
        public String getKeywords() {
            return keywords;
        }

        /**
         * @return 创建工具，可为空
         */
        public String getCreator() {
            return creator;
        }

        /**
         * @return 生成工具
         */
        public String getProducer() {
            return producer;
        }

        /**
         * @return 创建时间，可为空
         */
        public LocalDateTime getCreationDate() {
            return creationDate;
        }

        /**
         * @return 修改时间，可为空
         */
        public LocalDateTime getModDate() {
            return modDate;
        }

        @Override
        public String toString() {
            return "PdfMetadata{title=" + title + ", author=" + author + ", creator=" + creator
                    + ", producer=" + producer + ", creation=" + creationDate + ", mod=" + modDate + '}';
        }
    }
}
