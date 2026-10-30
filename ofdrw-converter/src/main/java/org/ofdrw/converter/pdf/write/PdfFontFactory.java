package org.ofdrw.converter.pdf.write;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.ofdrw.converter.font.FontDescriptor;
import org.ofdrw.converter.pdf.font.TrueTypePdfFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * PDF 字体对象映射：字体引用 → PDFBox 字体对象（P2）
 * <p>
 * 职责（决策 15/16）：把 {@link TrueTypePdfFont} 中已经由 {@code ofdrw-font} 准备好的<b>整字体数据</b>包装为
 * PDF 字体对象，<b>不在 converter 内解析字体结构</b>：
 * <ul>
 *     <li>字体数据优先通过 {@link PDType0Font#load(PDDocument, InputStream, boolean)}（{@code embedSubset=false}，
 *     决策 16：整字体嵌入）交给 PDFBox 生成 Type0 字体（Identity-H、CIDFontType2/0、FontDescriptor 由 PDFBox 计算）；</li>
 *     <li>PDFBox 无法加载的数据（例如其内部解析器不支持的裁剪字体）退化为手工构造 Type0 字体字典：
 *     直接写入 {@code FontFile2}/{@code FontFile3}、FontDescriptor 字段与 Identity-H cmap，
 *     此时若不写 CIDToGIDMap 则按 PDF 规范默认取值 {@code /Identity}（CID = GID），与字形优先输出一致；</li>
 *     <li>把 {@code org.ofdrw.converter.font.ToUnicodeCMap} 生成的 CMap 写入字体字典的 {@code /ToUnicode} 条目，
 *     保证文字可提取（决策 3；PDFBox 3 的 {@link PDFont} 没有公开的 setToUnicode，故直接写字典项）。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfFontFactory {

    private static final Logger logger = LoggerFactory.getLogger(PdfFontFactory.class);

    /**
     * 已生成的字体对象：KEY 为文档，VALUE 为该文档内“字体引用 → 字体对象”
     * <p>
     * 字体对象内部持有属于某个 PDF 文档的流对象，不能跨文档复用。
     */
    private final Map<PDDocument, Map<TrueTypePdfFont, PDFont>> cache = new java.util.IdentityHashMap<>();

    /**
     * 按 Identity-H 编码字符的字体集合（构建 Identity-H 的 cmap 表）
     */
    private static final int IDENTITY_H_GLYPHS = 0x10000;

    /**
     * 获取或创建 PDF 字体对象
     *
     * @param document PDF 文档
     * @param font     字体引用
     * @return PDF 字体对象；字体数据缺失时返回 null
     * @throws IOException 字体写入失败
     */
    public PDFont obtain(PDDocument document, TrueTypePdfFont font) throws IOException {
        if (font == null) {
            return null;
        }
        // 字体对象与创建它的文档绑定（FontFile2 流、ToUnicode 流都属于该文档），
        // 因此缓存按“文档 + 字体引用”区分，跨文档复用时重新创建
        java.util.Map<TrueTypePdfFont, PDFont> perDocument =
                cache.computeIfAbsent(document, k -> new java.util.HashMap<>());
        PDFont cached = perDocument.get(font);
        if (cached != null) {
            return cached;
        }
        PDFont pdfFont = create(document, font);
        if (pdfFont != null) {
            perDocument.put(font, pdfFont);
        }
        return pdfFont;
    }

    /**
     * 按当前已登记的全部“字形 → Unicode”映射重建所有已创建字体的 ToUnicode CMap
     * <p>
     * 字体对象在首次使用时创建，此时 ToUnicode 只含当时已登记的字形；同一字体在后续页面继续出现新字形时，
     * 其映射会不断追加。因此必须在文档保存前统一刷新一次，否则提取文字时缺映射的字形会退化为错位码位
     * （实测多页文档的文本相似度因此降到 0.25 上下）。
     *
     * @param document PDF 文档，可为空
     * @throws IOException 写入 ToUnicode 流失败
     */
    public void refreshToUnicode(PDDocument document, java.util.Collection<TrueTypePdfFont> fonts) throws IOException {
        if (document == null || fonts == null) {
            return;
        }
        for (TrueTypePdfFont font : fonts) {
            if (font == null || font.getToUnicode() == null || font.getToUnicode().size() == 0) {
                continue;
            }
            /*
             * 关键：即使该字体从未被写入侧创建过 PDFont，也要生成 ToUnicode —— 反过来说，
             * 只要它在本次文档里出现过（渲染时登记过字形映射），就必须在对应的 PDF 字体字典上挂 CMap。
             * 因此这里按字体实例查缓存；未命中时跳过（说明该页未实际写出文字）。
             */
            PDFont pdfFont = cachedFont(document, font);
            if (pdfFont == null) {
                continue;
            }
            COSStream toUnicode = createToUnicode(document, font);
            if (toUnicode != null) {
                pdfFont.getCOSObject().setItem(COSName.TO_UNICODE, toUnicode);
            }
        }
    }

    /**
     * 取已为该文档创建的字体对象
     *
     * @param document PDF 文档
     * @param font     字体引用
     * @return PDF 字体对象；尚未创建时返回 null
     */
    private PDFont cachedFont(PDDocument document, TrueTypePdfFont font) {
        java.util.Map<TrueTypePdfFont, PDFont> perDocument = cache.get(document);
        return perDocument == null ? null : perDocument.get(font);
    }

    /**
     * 创建 PDF 字体对象
     *
     * @param document PDF 文档
     * @param font     字体引用
     * @return PDF 字体对象；字体数据缺失时返回 null
     * @throws IOException 字体写入失败
     */
    private PDFont create(PDDocument document, TrueTypePdfFont font) throws IOException {
        if (font.getEmbedded() == null) {
            logger.warn("字体 {} 没有可嵌入数据，跳过 PDF 字体对象创建", font.getFamilyName());
            return null;
        }
        byte[] data = font.getEmbedded().getData();
        PDFont pdfFont = null;
        if (requiresIdentityCid(font)) {
            /*
             * 文字按“字形索引（GID）直接作为 CID”写入，这要求字体字典中的 CID→GID 恒等。
             * PDFBox 的 PDType0Font.load 会用字体自带 cmap 建立 CID 映射，只有在映射恰好是恒等时
             * CID 才等于 GID；OFD 内嵌字体常被裁剪掉 cmap（实测 Font69.ttf 无 cmap 表），
             * 此时 PDType0Font 会按自身编码规则取字形，写出后与 GID 不一致 → 乱码/圆点。
             * 因此这类字体一律手搓 Type0 字典（Identity-H + 不写 CIDToGIDMap = /Identity）。
             */
            pdfFont = buildType0(document, font, data);
        } else {
            try (InputStream in = new ByteArrayInputStream(data)) {
                // embedSubset = false：整字体嵌入，不做子集裁剪（设计决策 16），
                // 字形编号保持不变，与“按 GID 写字 + Identity-H”的输出方式一致
                pdfFont = PDType0Font.load(document, in, false);
            } catch (Exception | LinkageError e) {
                // PDFBox 解析器不接受某些裁剪字体（如 loca 偏移全为 0）或编码表缺失，此时退回字体字典直接构造
                logger.warn("PDFBox 无法加载字体 {}（{}），改用字体字典直接构造", font.getFamilyName(), e.getMessage());
            }
        }
        if (pdfFont == null) {
            pdfFont = buildType0(document, font, data);
        }
        if (pdfFont == null) {
            return null;
        }
        COSStream toUnicode = createToUnicode(document, font);
        if (toUnicode != null) {
            // 直接写入字体字典的 ToUnicode 条目（PDFBox 3 的 PDFont 没有公开的 setToUnicode）
            pdfFont.getCOSObject().setItem(COSName.TO_UNICODE, toUnicode);
        }
        return pdfFont;
    }

    /**
     * 是否需要手搓 Identity CID 的 Type0 字典
     * <p>
     * 判定依据：字体没有 Unicode cmap（无法由 PDFBox 建立与 GID 一致的 CID 映射），
     * 或字形数超过 255（双字节 CID，PDFBox 的 cmap 可能是单字节/子集映射，无法保证恒等）。
     *
     * @param font 字体引用
     * @return true 表示必须用 Identity-H 手搓字典
     */
    private static boolean requiresIdentityCid(TrueTypePdfFont font) {
        if (font.getEmbedded() == null) {
            return true;
        }
        if (!font.hasUnicodeCmap()) {
            logger.debug("字体 {} 没有 cmap 表，按 Identity CID 手搓 Type0 字典（保证 CID == GID）", font.getFamilyName());
            return true;
        }
        return font.getCodeByteLength() > 1;
    }

    /**
     * 按字体真实字宽构造 CID 字宽数组 {@code /W}
     * <p>
     * 结构（PDF 1.7 9.7.4.3）：{@code [c [w1 w2 ...] c1 c2 w ...]}，这里统一采用“逐 CID 独立给出”的
     * {@code [c [w]]} 形式，CID 即字形索引（Identity），与写字方式一致。
     *
     * @param font 字体引用
     * @return 字宽数组；字体没有字宽表时返回 {@code null}
     */
    private static COSArray buildWidths(TrueTypePdfFont font) {
        org.ofdrw.converter.font.FontMetrics metrics = font.getMetrics();
        if (metrics == null || !metrics.hasAdvanceWidth()) {
            return null;
        }
        COSArray widths = new COSArray();
        int count = 0;
        java.util.Map<Integer, Float> documented = font.documentAdvances();
        if (!documented.isEmpty()) {
            // 只写本页真正用到的字形（文档已给出步进）：字宽表越小，阅读器判词边界越稳，
            // 未列出的字形由 /DW 兜底，不影响渲染位置（位置由显式 Tm 决定）
            for (java.util.Map.Entry<Integer, Float> e : new java.util.TreeMap<Integer, Float>(documented).entrySet()) {
                if (e.getKey() == null || e.getValue() == null || e.getValue() <= 0f) {
                    continue;
                }
                COSArray one = new COSArray();
                one.add(new org.apache.pdfbox.cos.COSFloat(e.getValue()));
                widths.add(COSInteger.get(e.getKey()));
                widths.add(one);
                count++;
            }
            return count == 0 ? null : widths;
        }
        int lastNonZero = -1;
        for (int gid = 0; gid < MAX_WIDTH_GLYPHS; gid++) {
            float width = metrics.getAdvanceWidth(gid);
            if (width <= 0f) {
                // 连续 256 个无字宽的字形视为字宽表结束（字体的 hmtx 只覆盖有效字形）
                if (lastNonZero >= 0 && gid - lastNonZero > 256) {
                    break;
                }
                continue;
            }
            COSArray one = new COSArray();
            one.add(new org.apache.pdfbox.cos.COSFloat(width));
            widths.add(COSInteger.get(gid));
            widths.add(one);
            lastNonZero = gid;
            count++;
        }
        return count == 0 ? null : widths;
    }

    /**
     * 字宽数组最多输出的字形数（单位：个），防止超大字体生成过大的 /W
     */
    private static final int MAX_WIDTH_GLYPHS = 65536;

    /**
     * 手工构造 Type0 字体字典（PDFBox 加载失败时的兜底）
     * <p>
     * 结构与 PDF 1.7 9.7 一致：Type0 → CIDFontType2/CIDFontType0 → FontDescriptor → FontFile2/FontFile3。
     * 不写 CIDToGIDMap 表示取默认值 {@code /Identity}，因此 CID 与字形索引一一对应，与按 GID 写字的输出一致。
     *
     * @param document PDF 文档
     * @param font     字体引用
     * @param data     字体数据
     * @return Type0 字体；字体缺少必要信息时返回 null
     * @throws IOException 写入失败
     */
    private PDFont buildType0(PDDocument document, TrueTypePdfFont font, byte[] data) throws IOException {
        FontDescriptor descriptor = font.getDescriptor();
        if (descriptor == null) {
            return null;
        }
        COSDictionary type0 = new COSDictionary();
        type0.setItem(COSName.TYPE, COSName.FONT);
        type0.setName(COSName.SUBTYPE, COSName.TYPE0.getName());
        type0.setName(COSName.BASE_FONT, descriptor.getRenamedPostScriptName());
        type0.setName(COSName.ENCODING, "Identity-H");
        type0.setItem(COSName.DESCENDANT_FONTS, new COSArray());

        COSDictionary cidFont = new COSDictionary();
        boolean cff = descriptor.getGlyphFormat() != FontDescriptor.GlyphFormat.TRUE_TYPE;
        cidFont.setItem(COSName.TYPE, COSName.FONT);
        cidFont.setName(COSName.SUBTYPE, cff ? "CIDFontType0" : "CIDFontType2");
        cidFont.setName(COSName.BASE_FONT, descriptor.getRenamedPostScriptName());
        cidFont.setItem(COSName.CIDSYSTEMINFO, cidSystemInfo());
        // FontBBox 以 1/1000 文本空间单位表达
        float[] bbox = descriptor.getFontBBox();
        COSArray box = new COSArray();
        for (float v : bbox) {
            box.add(COSInteger.get(Math.round(v)));
        }
        cidFont.setItem(COSName.FONT_BBOX, box);
        /*
         * 字宽：文字按“显式定位”写入（每个字形单独 Tm），字宽只影响阅读器的排版与提取，
         * 因此用 /W 数组按真实 hmtx 宽度输出，避免默认 1em 造成提取文本的行内空白与错位。
         * 缺省字宽（PDF 1.7 9.7.4.3）：字体没有字宽表时按 1 em（1000）处理。
         */
        cidFont.setInt(COSName.DW, 1000);
        /*
         * /W 优先采用“文档声明的排版步进”（由文字渲染器按 Delta 登记），
         * 未登记的字体（无 Delta、或非文字路径）回退到字体自带字宽。
         * 原因：OFD 用 Delta 精确摆放字形，若 /W 用替代字体字宽，阅读器会判“字宽 < 字距”
         * 而在字间插入空格，导致复制文本多空格、错行（实测 1.ofd 多 19 个空格、错 3 行）。
         */
        COSArray widths = buildWidths(font);
        if (widths != null) {
            cidFont.setItem(COSName.W, widths);
        }

        COSDictionary fontFileDesc = new COSDictionary();
        fontFileDesc.setItem(COSName.TYPE, COSName.FONT_DESC);
        fontFileDesc.setName(COSName.FONT_NAME, descriptor.getRenamedPostScriptName());
        fontFileDesc.setInt(COSName.FLAGS, descriptor.getFlags());
        fontFileDesc.setItem(COSName.FONT_BBOX, box);
        fontFileDesc.setInt(COSName.ITALIC_ANGLE, Math.round(descriptor.getItalicAngle()));
        fontFileDesc.setInt(COSName.ASCENT, Math.round(font.getMetrics() == null ? 800 : ascent(font)));
        fontFileDesc.setInt(COSName.DESCENT, Math.round(font.getMetrics() == null ? -200 : descent(font)));
        fontFileDesc.setInt(COSName.CAP_HEIGHT, Math.round(font.getMetrics() == null ? 700 : ascent(font)));
        fontFileDesc.setInt(COSName.STEM_V, 80);
        // 缺失字形的字宽（PDF 1.7 表 122）：与 CIDFont 的 DW 保持一致
        fontFileDesc.setInt(COSName.MISSING_WIDTH, 1000);
        if (descriptor.getFamilyName() != null) {
            cidFont.setString(COSName.FONT_FAMILY, descriptor.getFamilyName());
        }

        COSStream fontFile;
        if (cff) {
            fontFile = document.getDocument().createCOSStream();
            fontFile.setName(COSName.SUBTYPE, "Type1C");
            fontFile.setInt(COSName.LENGTH1, data.length);
            fontFile.setItem(COSName.LENGTH2, COSInteger.ZERO);
            fontFile.setItem(COSName.LENGTH3, COSInteger.ZERO);
        } else {
            fontFile = document.getDocument().createCOSStream();
            fontFile.setInt(COSName.LENGTH1, data.length);
        }
        try (java.io.OutputStream out = fontFile.createOutputStream()) {
            out.write(data);
        }
        fontFileDesc.setItem(COSName.getPDFName(descriptor.getFontFileKey()), fontFile);
        cidFont.setItem(COSName.FONT_DESC, fontFileDesc);

        ((COSArray) type0.getItem(COSName.DESCENDANT_FONTS)).add(cidFont);
        // Identity-H 预定义 CMap：无需自带 cmap 流
        return new PDType0Font(type0);
    }

    /**
     * 字体上行高度（1/1000 文本空间单位）
     *
     * @param font 字体引用
     * @return 上行高度
     */
    private static float ascent(TrueTypePdfFont font) {
        return font.getMetrics() == null ? 800 : font.getMetrics().getAscent();
    }

    /**
     * 字体下行高度（1/1000 文本空间单位，负数）
     *
     * @param font 字体引用
     * @return 下行高度
     */
    private static float descent(TrueTypePdfFont font) {
        return font.getMetrics() == null ? -200 : font.getMetrics().getDescent();
    }

    /**
     * CIDSystemInfo（Adobe-Identity）
     *
     * @return 字典
     */
    private static COSDictionary cidSystemInfo() {
        COSDictionary info = new COSDictionary();
        info.setString(COSName.REGISTRY, "Adobe");
        info.setString(COSName.ORDERING, "Identity");
        info.setInt(COSName.SUPPLEMENT, 0);
        return info;
    }

    /**
     * 创建 ToUnicode CMap 流
     * <p>
     * CMap 文本由 {@code ofdrw-font} 的 {@link org.ofdrw.converter.font.ToUnicodeCMap} 生成（映射的唯一真源），
     * 这里只负责写入 PDF 流（Flate 压缩）。生成结果已用 fontbox 的 {@code CMapParser} 验证可被正确解析。
     *
     * @param document PDF 文档
     * @param font     字体引用
     * @return CMap 流；没有登记任何映射时返回 null
     * @throws IOException 写入失败
     */
    private static COSStream createToUnicode(PDDocument document, TrueTypePdfFont font) throws IOException {
        // 以当前已登记的全部映射生成 CMap，并按 CMap 的码位宽度统一 CID 编码（1 或 2 字节），
        // 保证内容流中的 CID 与 ToUnicode 的码位长度一致（否则阅读器会忽略 ToUnicode 并退回 Identity）
        String cmapText = font.prepareToUnicode();
        if (cmapText == null) {
            return null;
        }
        COSStream stream = document.getDocument().createCOSStream();
        try (java.io.OutputStream out = stream.createOutputStream(COSName.FLATE_DECODE)) {
            out.write(cmapText.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        }
        return stream;
    }

    /**
     * Identity-H 编码所需的 cmap 表长度（供测试断言结构使用）
     *
     * @return 字形数量上限
     */
    public static int identityHGlyphCapacity() {
        return IDENTITY_H_GLYPHS;
    }
}
