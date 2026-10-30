package org.ofdrw.converter.font;

import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * 标识PDF字体是否是被相近字体替换
 * <p>
 * 历史实现绑定 iText 的 PdfFont 类型，现改用 PDFBox 的 PDFont 类型，构造器与访问器保持不变。
 *
 * @author myf
 * @since 2021-05-28 21:46:18
 * @deprecated {@link FontWrapper}
 */
@Deprecated
public class PdfFontWrapper {

    private PDFont pdfFont;

    private boolean enableSimilarFontReplace;

    public PdfFontWrapper() {
    }

    public PdfFontWrapper(PDFont pdfFont) {
        this.pdfFont = pdfFont;
    }

    public PdfFontWrapper(PDFont pdfFont, boolean enableSimilarFontReplace) {
        this.pdfFont = pdfFont;
        this.enableSimilarFontReplace = enableSimilarFontReplace;
    }

    /**
     * 获取PDF字体
     *
     * @return PDF字体
     */
    public PDFont getPdfFont() {
        return pdfFont;
    }

    /**
     * 设置PDF字体
     *
     * @param pdfFont PDF字体
     */
    public void setPdfFont(PDFont pdfFont) {
        this.pdfFont = pdfFont;
    }

    /**
     * 是否使用了相近字体替换
     *
     * @return true - 已替换；false - 未替换
     */
    public boolean isEnableSimilarFontReplace() {
        return enableSimilarFontReplace;
    }

    /**
     * 设置是否使用了相近字体替换
     *
     * @param enableSimilarFontReplace true - 已替换；false - 未替换
     */
    public void setEnableSimilarFontReplace(boolean enableSimilarFontReplace) {
        this.enableSimilarFontReplace = enableSimilarFontReplace;
    }
}
