package org.ofdrw.converter.pdf.font;

/**
 * PDF 字体引用
 * <p>
 * {@code org.ofdrw.converter.pdf.font} 包负责“选哪种字体、如何映射到 PDF 字体对象”，
 * 渲染器只依赖本接口，不感知 PDFBox 类型（设计 3.9 节：字体解析与嵌入数据能力来自 ofdrw-font）。
 * <p>
 * 实现类由字体选择器在写入 PDF 时创建，渲染阶段只传递引用标识。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public interface PdfFontRef {

    /**
     * 获取页面内唯一的资源名（PDF 资源字典中的键）
     *
     * @return 资源名，例如 {@code F1}
     */
    String getResourceName();

    /**
     * 字体是否发生了替换（内嵌缺失或无法解析时按替换策略处理）
     *
     * @return true 表示使用了替代字体，调用方需输出 warning 日志（决策 7）
     */
    boolean isSubstituted();

    /**
     * 字体族名，用于日志
     *
     * @return 字体族名
     */
    String getFamilyName();
}
