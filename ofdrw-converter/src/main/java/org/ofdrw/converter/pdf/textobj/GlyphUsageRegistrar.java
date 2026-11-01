package org.ofdrw.converter.pdf.textobj;

/**
 * 字形使用登记函数：记录 PDF 字体、字符编码、原始 GID 与 Unicode 码位
 * <p>
 * 供字体子集化收集“实际使用的字形”与“用于重建 cmap 的 Unicode”；
 * 默认原样嵌入时只做统计。
 *
 * @author 权观宇
 * @since 2.5.1
 */
@FunctionalInterface
public interface GlyphUsageRegistrar {

    /**
     * 记录一次字形使用
     *
     * @param font    PDF 字体
     * @param code    PDF 字符编码（内容流中的字节值）
     * @param gid     PDF 原始字形索引，单位为个
     * @param unicode 该字形对应的 Unicode 码位；未知时为 -1
     */
    void register(org.apache.pdfbox.pdmodel.font.PDFont font, int code, int gid, int unicode);
}
