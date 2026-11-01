package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.ofdrw.core.basicType.ST_ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * PDF 字体到 OFD 字体资源的映射
 * <p>
 * 负责三件事：
 * <ol>
 *     <li>把“字体登记”统一适配到调用方注入的实现上（{@link #register(PDFont, PDPage)}）；</li>
 *     <li>记录字形使用（原始 GID 与 Unicode），供字体子集化使用；</li>
 *     <li><b>CGTransform GID 的延迟解析</b>：字体子集化时 old→new 映射在字体写出（flush）之后才确定，
 *     而文字对象在渲染阶段就已写入页面内容。因此这里先把“PDF 原始 GID → CGTransform 的 Glyphs 节点”
 *     登记下来，打包前由 {@link #resolvePendingGids()} 用最终映射统一改写。</li>
 * </ol>
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
     * 注释单元（平铺底纹/水印）字体登记函数，可为 null（此时退回正文策略）
     */
    private final java.util.function.BiFunction<PDFont, java.util.Set<Integer>, ST_ID> annotationFontRegistrar;

    /**
     * 字形使用登记实现：记录 PDF 原始 GID 与 Unicode
     */
    private final OfdPageDrawer.GlyphUsageRegistrar glyphRegistrar;

    /**
     * 最终 GID 解析实现：字体子集化后可能重排 GID，默认恒等
     */
    private final GidResolver gidResolver;

    /**
     * 已登记字体 → OFD 字体资源 ID，避免重复走字体解析
     */
    private final Map<PDFont, ST_ID> cache = new ConcurrentHashMap<>();

    /**
     * 待改写的 CGTransform GID：PDF 字体 → 本次转换写入的 GID 节点
     * <p>
     * 以弱引用持有 PDFont：每次渲染页都会新建 PDFont 对象，强引用会阻止其回收。
     */
    private final Map<PDFont, List<PendingGid>> pending = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * 最终 GID 解析函数（与公开接口 {@link GidResolver} 签名一致）
     */
    @FunctionalInterface
    interface OfdGidResolver extends GidResolver {
    }

    /**
     * 待改写的 CGTransform GID 段
     * <p>
     * 一条 CGTransform 的 {@code Glyphs} 是一组按字符顺序排列的 GID，子集化后需要逐个换算并整体回写。
     */
    static final class PendingGid {

        /**
         * CGTransform 的 Glyphs 节点（文本为空格分隔的 GID 列表）
         */
        private final WeakReference<org.dom4j.Element> glyphsElement;

        /**
         * 该段对应的 PDF 原始 GID 列表
         */
        private final org.ofdrw.core.basicType.ST_Array pdfGids;

        PendingGid(org.dom4j.Element glyphsElement, org.ofdrw.core.basicType.ST_Array pdfGids) {
            this.glyphsElement = new WeakReference<>(glyphsElement);
            this.pdfGids = pdfGids;
        }
    }

    /**
     * 创建字体映射
     *
     * @param fontRegistrar  字体登记实现
     * @param glyphRegistrar 字形使用登记实现，可为 null
     * @param gidResolver    最终 GID 解析实现，可为 null（表示 GID 不变）
     */
    PdfTextFonts(Function<PDFont, ST_ID> fontRegistrar,
                 OfdPageDrawer.GlyphUsageRegistrar glyphRegistrar,
                 GidResolver gidResolver) {
        this(fontRegistrar, null, glyphRegistrar, gidResolver);
    }

    /**
     * 创建字体适配层（带注释单元字体登记函数）
     *
     * @param fontRegistrar           正文字体登记函数
     * @param annotationFontRegistrar 注释单元字体登记函数（允许系统字体兜底），可为 null
     * @param glyphRegistrar          字形使用登记
     * @param gidResolver             GID 解析器
     */
    PdfTextFonts(Function<PDFont, ST_ID> fontRegistrar,
                 java.util.function.BiFunction<PDFont, java.util.Set<Integer>, ST_ID> annotationFontRegistrar,
                 OfdPageDrawer.GlyphUsageRegistrar glyphRegistrar,
                 GidResolver gidResolver) {
        this.fontRegistrar = fontRegistrar;
        this.annotationFontRegistrar = annotationFontRegistrar;
        this.glyphRegistrar = glyphRegistrar;
        this.gidResolver = gidResolver;
    }

    /**
     * 登记注释单元字体
     * <p>
     * 未注入注释字体登记函数时退回正文策略。
     *
     * @param font PDF 字体
     * @return OFD 字体资源 ID；无法登记时返回 null
     */
    ST_ID registerAnnotationFont(PDFont font, java.util.Set<Integer> usedUnicodes) {
        if (font == null) {
            return null;
        }
        if (annotationFontRegistrar == null) {
            return register(font, null);
        }
        try {
            return annotationFontRegistrar.apply(font, usedUnicodes);
        } catch (RuntimeException e) {
            logger.debug("注释字体登记失败：{}", PdfFontHelper.fontName(font), e);
            return null;
        }
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
     * @param font    PDF 字体
     * @param code    PDF 字符编码
     * @param gid     PDF 原始字形索引
     * @param unicode 该字形对应的 Unicode 码位，未知时为 -1
     */
    void use(PDFont font, int code, int gid, int unicode) {
        if (glyphRegistrar == null) {
            return;
        }
        glyphRegistrar.register(font, code, gid, unicode);
    }

    /**
     * 登记一个待改写的 CGTransform GID 段（字体子集化时使用）
     *
     * @param font          PDF 字体
     * @param glyphsElement CGTransform 的 Glyphs 节点
     * @param pdfGids       该段对应的 PDF 原始 GID 列表
     */
    void trackGid(PDFont font, org.dom4j.Element glyphsElement, org.ofdrw.core.basicType.ST_Array pdfGids) {
        if (font == null || glyphsElement == null || gidResolver == null) {
            return;
        }
        List<PendingGid> list = pending.get(font);
        if (list == null) {
            synchronized (pending) {
                list = pending.get(font);
                if (list == null) {
                    list = new ArrayList<>(64);
                    pending.put(font, list);
                }
            }
        }
        synchronized (list) {
            list.add(new PendingGid(glyphsElement, pdfGids));
        }
    }

    /**
     * 是否存在待改写的 CGTransform GID
     *
     * @return true 表示渲染阶段登记过、尚未改写的 CGTransform
     */
    boolean hasPendingGids() {
        return !pending.isEmpty();
    }

    /**
     * 用最终 old→new 映射统一改写 CGTransform 中的 GID
     * <p>
     * 必须在字体写出（{@code PdfFontRegistry#flush}）之后、OFD 打包之前调用。
     *
     * @return 实际改写的 GID 数量，单位为个
     */
    int resolvePendingGids() {
        if (gidResolver == null || pending.isEmpty()) {
            return 0;
        }
        int changed = 0;
        synchronized (pending) {
            for (Map.Entry<PDFont, List<PendingGid>> entry : pending.entrySet()) {
                PDFont font = entry.getKey();
                List<PendingGid> list = entry.getValue();
                synchronized (list) {
                    for (PendingGid item : list) {
                        org.dom4j.Element element = item.glyphsElement.get();
                        if (element == null) {
                            continue;
                        }
                        java.util.List<String> mapped = new java.util.ArrayList<>();
                        boolean anyChanged = false;
                        for (String value : item.pdfGids.toString().split("\\s+")) {
                            if (value.isEmpty()) {
                                continue;
                            }
                            int pdfGid;
                            try {
                                pdfGid = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                mapped.add(value);
                                continue;
                            }
                            int newGid = gidResolver.resolve(font, pdfGid);
                            anyChanged |= newGid != pdfGid;
                            mapped.add(String.valueOf(newGid));
                        }
                        if (anyChanged) {
                            element.setText(String.join(" ", mapped));
                            changed += mapped.size();
                        }
                    }
                }
            }
            pending.clear();
        }
        logger.debug("CGTransform GID 改写完成：{} 个", changed);
        return changed;
    }

    /**
     * 解析 CGTransform 使用的最终 GID（立即求值，仅在字体已写出后有效）
     *
     * @param font   PDF 字体
     * @param pdfGid PDF 原始字形索引
     * @return 最终 GID
     */
    int resolveGid(PDFont font, int pdfGid) {
        return gidResolver == null ? pdfGid : gidResolver.resolve(font, pdfGid);
    }
}
