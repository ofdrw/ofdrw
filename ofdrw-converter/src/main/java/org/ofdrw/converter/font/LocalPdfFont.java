package org.ofdrw.converter.font;

import com.itextpdf.kernel.font.PdfFont;

public abstract class LocalPdfFont extends PdfFont {

    private boolean enableSimilarFontReplace;

    public boolean isEnableSimilarFontReplace() {
        return enableSimilarFontReplace;
    }

    public void setEnableSimilarFontReplace(boolean enableSimilarFontReplace) {
        this.enableSimilarFontReplace = enableSimilarFontReplace;
    }
}
