package org.ofdrw.reader.keyword;

import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.core.text.text.CT_Text;

/**
 * 文本资源
 *
 * @author minghu-zhang
 * @since 16:25 2020/9/25
 */
public class KeywordResource {

    /**
     * 页码
     */
    private int page;
    /**
     * 字体对象
     */
    private CT_Font font;
    /**
     * 文字对象
     */
    private CT_Text text;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public CT_Font getFont() {
        return font;
    }

    public void setFont(CT_Font font) {
        this.font = font;
    }

    public CT_Text getText() {
        return text;
    }

    public void setText(CT_Text text) {
        this.text = text;
    }
}
