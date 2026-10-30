package org.ofdrw.converter.pdf.structure;

/**
 * PDF 动作类型（库无关描述）
 * <p>
 * 对应 OFD 表 51：{@code Goto}/{@code URI}/{@code GotoA} 支持映射；
 * {@code Sound}/{@code Movie} 按决策 18 跳过并输出 warning。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfAction {

    /**
     * 动作类型
     */
    public enum Type {
        /**
         * 跳转到文档内目标（OFD Goto、书签）
         */
        GOTO,
        /**
         * 打开外部链接（OFD URI）
         */
        URI,
        /**
         * 打开内嵌附件（OFD GotoA）
         */
        GOTO_A,
        /**
         * 音频动作（OFD Sound），不转换
         */
        SOUND,
        /**
         * 视频动作（OFD Movie），不转换
         */
        MOVIE
    }

    /**
     * 动作类型
     */
    private final Type type;

    /**
     * 跳转目标，仅 {@link Type#GOTO} 有效
     */
    private final PdfDestination destination;

    /**
     * 外部链接，仅 {@link Type#URI} 有效
     */
    private final String uri;

    /**
     * 附件标识，仅 {@link Type#GOTO_A} 有效
     */
    private final String attachmentId;

    /**
     * @param type         动作类型
     * @param destination  跳转目标，可为空
     * @param uri          外部链接，可为空
     * @param attachmentId 附件标识，可为空
     */
    private PdfAction(Type type, PdfDestination destination, String uri, String attachmentId) {
        this.type = type;
        this.destination = destination;
        this.uri = uri;
        this.attachmentId = attachmentId;
    }

    /**
     * 构造跳转动作
     *
     * @param destination 目标
     * @return 动作
     */
    public static PdfAction gotoDestination(PdfDestination destination) {
        if (destination == null) {
            throw new IllegalArgumentException("跳转目标不能为空");
        }
        return new PdfAction(Type.GOTO, destination, null, null);
    }

    /**
     * 构造外部链接动作
     *
     * @param uri 外部链接，不能为空
     * @return 动作
     */
    public static PdfAction uri(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            throw new IllegalArgumentException("外部链接不能为空");
        }
        return new PdfAction(Type.URI, null, uri, null);
    }

    /**
     * 构造打开附件的动作
     *
     * @param attachmentId 附件标识，不能为空
     * @return 动作
     */
    public static PdfAction gotoAttachment(String attachmentId) {
        if (attachmentId == null || attachmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("附件标识不能为空");
        }
        return new PdfAction(Type.GOTO_A, null, null, attachmentId);
    }

    /**
     * 构造“不转换”的动作（Sound / Movie），仅用于记录与告警
     *
     * @param type 动作类型
     * @return 动作
     */
    public static PdfAction skipped(Type type) {
        return new PdfAction(type, null, null, null);
    }

    /**
     * @return 动作类型
     */
    public Type getType() {
        return type;
    }

    /**
     * @return 跳转目标，非 GOTO 动作返回 null
     */
    public PdfDestination getDestination() {
        return destination;
    }

    /**
     * @return 外部链接，非 URI 动作返回 null
     */
    public String getUri() {
        return uri;
    }

    /**
     * @return 附件标识，非 GOTO_A 动作返回 null
     */
    public String getAttachmentId() {
        return attachmentId;
    }

    /**
     * 判断该动作是否属于“不转换”类型（Sound/Movie，决策 18）
     *
     * @return 不转换返回 true
     */
    public boolean isSkipped() {
        return type == Type.SOUND || type == Type.MOVIE;
    }

    @Override
    public String toString() {
        return "PdfAction{" + type + ", destination=" + destination + ", uri=" + uri
                + ", attachmentId=" + attachmentId + '}';
    }
}
