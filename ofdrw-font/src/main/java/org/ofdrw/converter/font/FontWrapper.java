package org.ofdrw.converter.font;

/**
 * 标识字体是否是被相近字体替换
 *
 * @author yuanfangme
 * @since 2021-06-04 17:10:18
 */
public class FontWrapper<T> {

    T font;

    private boolean enableSimilarFontReplace;

    /**
     * 字形索引是否为“模拟”语义，即按文档中的字形索引取轮廓会得到错乱字形
     * <p>
     * 默认与 {@link #enableSimilarFontReplace} 取值一致，保持既有语义不变；
     * 字体加载器可以按字体来源（内嵌 / 系统匹配 / 默认字体）单独设置该标记。
     */
    private boolean useSimulatedGlyphIndex;

    /**
     * 文档是否内嵌了该字体
     */
    private boolean embedded;

    /**
     * 替代字体能否覆盖文档文字（即具备文档字体名/字族名中的汉字等字符）
     * <p>
     * 仅在使用了替代字体时才有意义：替代字体不具备文档文字时，按字形索引或 Unicode 取轮廓
     * 都只能得到 .notdef 字形，跳过与绘制的结果同样无法还原文字。
     */
    private boolean replacementSupportsText = true;

    public FontWrapper() {
    }

    public FontWrapper(T font, boolean enableSimilarFontReplace) {
        this.font = font;
        this.enableSimilarFontReplace = enableSimilarFontReplace;
        this.useSimulatedGlyphIndex = enableSimilarFontReplace;
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

    /**
     * 判断按文档字形索引取轮廓是否会得到错乱字形
     *
     * @return true - 字形索引已失去原字体含义（内嵌字体加载失败或回落到默认字体）；false - 字形索引有效
     */
    public boolean isUseSimulatedGlyphIndex() {
        return useSimulatedGlyphIndex;
    }

    /**
     * 设置字形索引是否为“模拟”语义
     *
     * @param useSimulatedGlyphIndex true - 字形索引已失去原字体含义；false - 字形索引有效
     */
    public void setUseSimulatedGlyphIndex(boolean useSimulatedGlyphIndex) {
        this.useSimulatedGlyphIndex = useSimulatedGlyphIndex;
    }

    /**
     * 判断文档是否内嵌了该字体
     *
     * @return true - 文档内嵌了该字体；false - 文档未内嵌，依赖宿主系统字体
     */
    public boolean isEmbedded() {
        return embedded;
    }

    /**
     * 设置文档是否内嵌了该字体
     *
     * @param embedded true - 文档内嵌了该字体；false - 文档未内嵌
     */
    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    /**
     * 判断替代字体能否覆盖文档文字
     *
     * @return true - 替代字体具备文档所需字形（默认为 true）；false - 替代字体缺少文档文字
     */
    public boolean isReplacementSupportsText() {
        return replacementSupportsText;
    }

    /**
     * 设置替代字体能否覆盖文档文字
     *
     * @param replacementSupportsText true - 具备文档所需字形；false - 缺少文档文字
     */
    public void setReplacementSupportsText(boolean replacementSupportsText) {
        this.replacementSupportsText = replacementSupportsText;
    }
}
