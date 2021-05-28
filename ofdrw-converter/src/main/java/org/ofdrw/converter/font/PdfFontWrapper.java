package org.ofdrw.converter.font;

import com.itextpdf.kernel.font.PdfFont;

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
