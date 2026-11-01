package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * CGTransform GID 解析函数：把 PDF 原始 GID 换算为最终写入 OFD 的 GID
 * <p>
 * 原样嵌入时恒等；字体子集化后必须按 old→new 映射换算（映射在字体写出后才确定）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
@FunctionalInterface
public interface GidResolver {

    /**
     * 换算 GID
     *
     * @param font   PDF 字体
     * @param pdfGid PDF 原始字形索引，单位为个
     * @return 最终 GID，单位为个
     */
    int resolve(PDFont font, int pdfGid);
}
