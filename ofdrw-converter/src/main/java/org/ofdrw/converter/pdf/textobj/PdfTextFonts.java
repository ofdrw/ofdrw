package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.ofdrw.core.basicType.ST_ID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * PDF 字体到 OFD 字体资源的映射
 * <p>
 * 负责把“字体登记、字形使用登记、最终 GID 解析”统一适配到 {@link PdfTextFonts#register(PDFont, PDPage)}
 * 给出的登记实现上；实现由调用方注入（字体子集化/原样嵌入见 {@code PdfFontRegistry}），
 * 本类不关心具体嵌入策略。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class PdfTextFonts {

    private static final Logger logger = LoggerFactory.getLogger(PdfTextFonts.class);

    /**
     * 字体登记实现：返回 OFD 字体资源 ID，无法嵌入时返回 null
     */
    private final Function<PDFont, ST_ID> fontRegistrar;

    /**
     * 字形使用登记实现：记录 PDF 原始 GID
     */
    private final OfdPageDrawer.GlyphRegistrar glyphRegistrar;

    /**
     * 最终 GID 解析实现：字体子集化后可能重排 GID，默认恒等
     */
    private final OfdGidResolver gidResolver;

    /**
     * 已登记字体 -> OFD 字体资源 ID，避免重复走字体解析
     */
    private final Map<PDFont, ST_ID> cache = new ConcurrentHashMap<>();

    /**
     * 最终 GID 解析函数
     */
    @FunctionalInterface
    interface OfdGidResolver {
        /**
         * 解析 CGTransform 使用的最终 GID
         *
         * @param font   PDF 字体
         * @param pdfGid PDF 原始字形索引
         * @return 最终 GID
         */
        int resolve(PDFont font, int pdfGid);
    }

    /**
     * 创建字体映射
     *
     * @param fontRegistrar  字体登记实现
     * @param glyphRegistrar 字形使用登记实现，可为 null
     * @param gidResolver    最终 GID 解析实现，可为 null（表示 GID 不变）
     */
    PdfTextFonts(Function<PDFont, ST_ID> fontRegistrar,
                        OfdPageDrawer.GlyphRegistrar glyphRegistrar,
                        OfdGidResolver gidResolver) {
        this.fontRegistrar = fontRegistrar;
        this.glyphRegistrar = glyphRegistrar;
        this.gidResolver = gidResolver;
    }

    /**
     * 登记字体并返回 OFD 字体资源 ID
     *
     * @param font PDF 字体
     * @param page 当前页（供登记实现读取页级资源）
     * @return OFD 字体资源 ID；无法嵌入时返回 null（调用方回退路径）
     */
    ST_ID register(PDFont font, PDPage page) {
        if (font == null || fontRegistrar == null) {
            return null;
        }
        ST_ID cached = cache.get(font);
        if (cached != null) {
            return cached;
        }
        try {
            ST_ID id = fontRegistrar.apply(font);
            if (id != null) {
                cache.put(font, id);
            }
            return id;
        } catch (RuntimeException e) {
            // 登记失败不阻断转换，按不可嵌入处理（回退路径）
            logger.debug("字体登记失败：{}", PdfFontHelper.fontName(font), e);
            return null;
        }
    }

    /**
     * 记录字形使用
     *
     * @param font PDF 字体
     * @param code PDF 字符编码
     * @param gid  PDF 原始字形索引
     */
    void use(PDFont font, int code, int gid) {
        if (glyphRegistrar == null) {
            return;
        }
        glyphRegistrar.register(font, code, gid);
    }

    /**
     * 解析 CGTransform 使用的最终 GID
     *
     * @param font   PDF 字体
     * @param pdfGid PDF 原始字形索引
     * @return 最终 GID
     */
    int resolveGid(PDFont font, int pdfGid) {
        return gidResolver == null ? pdfGid : gidResolver.resolve(font, pdfGid);
    }
}
