package org.ofdrw.converter.font;

/**
 * 标识字体是否是被相近字体替换
 * @author yuanfangme
 * @since 2021-06-04 17:10:18
 */
public class FontWrapper<T> {

    T font;

    private boolean enableSimilarFontReplace;

    public FontWrapper() {
    }

    public FontWrapper(T font, boolean enableSimilarFontReplace) {
        this.font = font;
        this.enableSimilarFontReplace = enableSimilarFontReplace;
    }

    public T getFont() {
        return font;
    }

    public void setFont(T font) {
        this.font = font;
    }

    public boolean isEnableSimilarFontReplace() {
        return enableSimilarFontReplace;
    }

    public void setEnableSimilarFontReplace(boolean enableSimilarFontReplace) {
        this.enableSimilarFontReplace = enableSimilarFontReplace;
    }
}
