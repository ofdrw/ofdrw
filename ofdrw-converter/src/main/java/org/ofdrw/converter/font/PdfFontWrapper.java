package org.ofdrw.converter.font;

import com.itextpdf.kernel.font.PdfFont;

/**
 * 标识PDF字体是否是被相近字体替换
 * @author myf
 * @since 2021-05-28 21:46:18
 * @deprecated {@link FontWrapper}
 */
@Deprecated
public class PdfFontWrapper {

    private PdfFont pdfFont;

    private boolean enableSimilarFontReplace;

    public PdfFontWrapper() {
    }

    public PdfFontWrapper(PdfFont pdfFont) {
        this.pdfFont = pdfFont;
    }

    public PdfFontWrapper(PdfFont pdfFont, boolean enableSimilarFontReplace) {
        this.pdfFont = pdfFont;
        this.enableSimilarFontReplace = enableSimilarFontReplace;
    }

    public PdfFont getPdfFont() {
        return pdfFont;
    }

    public void setPdfFont(PdfFont pdfFont) {
        this.pdfFont = pdfFont;
    }

    public boolean isEnableSimilarFontReplace() {
        return enableSimilarFontReplace;
    }

    public void setEnableSimilarFontReplace(boolean enableSimilarFontReplace) {
        this.enableSimilarFontReplace = enableSimilarFontReplace;
    }
}
