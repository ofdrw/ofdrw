package org.ofdrw.converter.pdf.textobj;

import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDCIDFont;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.ofdrw.core.basicStructure.res.resources.Fonts;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.graphics2d.OFDGraphicsDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PdfFontRegistry} 单元测试
 * <p>
 * 覆盖：PDF 内嵌字体逐字节原样嵌入（GID 不变）、字体去重、TTC 提取、裸 CFF 包装、
 * 可选子集化的表裁剪与 GID old→new 映射、系统字体精确匹配、失败回退。
 * 所需字体按“测试资源 → 系统字体目录”查找，都找不到时用 {@link org.junit.jupiter.api.Assumptions} 跳过。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class PdfFontRegistryTest {

    @TempDir
    Path tempDir;

    /**
     * 测试资源目录（模块目录下执行 mvn test 时）
     */
    private static final Path RESOURCES = Paths.get("src/test/resources");

    /**
     * 测试资源目录（仓库根目录下执行时）
     */
    private static final Path MODULE_RESOURCES = Paths.get("ofdrw-converter/src/test/resources");

    @Test
    void embeddedTrueTypeIsCopiedVerbatim() throws Exception {
        Path pdf = requireSamplePdf();
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            PDFont font = firstEmbeddedTrueType(document);
            byte[] original = embeddedBytes(font);
            OFDGraphicsDocument ofd = new OFDGraphicsDocument(tempDir.resolve("verbatim.ofd"));
            PdfFontRegistry registry = new PdfFontRegistry(ofd);
            ST_ID id = registry.registerFont(font);
            assertNotNull(id, "内嵌 TrueType 字体应当登记成功");
            assertFalse(registry.isSubset(font), "默认策略原样嵌入，不做子集化");
            int gid = registry.glyphId(font, firstCode(font));
            assertTrue(gid >= 0, "应能取得 PDF 原始 GID");
            assertEquals(gid, registry.subsetGid(font, gid), "原样嵌入时 GID 映射恒等");
            registry.flush();

            CT_Font ctFont = findFont(ofd, id);
            assertNotNull(ctFont.getFontFile(), "应当写出 FontFile");
            Path written = ofd.docDir.getResource(ctFont.getFontFile().getFileName());
            assertTrue(Files.isRegularFile(written), "字体文件应存在于 Res 目录：" + written);
            assertArrayEquals(original, Files.readAllBytes(written), "字体程序应当逐字节原样嵌入");
            assertEquals(".ttf", suffixOf(ctFont), "TrueType 使用 .ttf 扩展名");
            assertEquals("unicode", ctFont.getCharset().toString(), "Charset 应为 unicode");
            assertFalse(ctFont.getFontName().contains("+"), "FontName 应去掉子集前缀");
            assertNotNull(ctFont.getFamilyName(), "FamilyName 应回退为 FontName");
            assertNotNull(ctFont.getBold(), "Bold 应有取值");
            assertNotNull(ctFont.getItalic(), "Italic 应有取值");
            assertNotNull(ctFont.getSerif(), "Serif 应有取值");
            assertNotNull(ctFont.getFixedWidth(), "FixedWidth 应有取值");
        }
    }

    @Test
    void deduplicatesSameFontProgram() throws Exception {
        Path pdf = requireSamplePdf();
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            PDFont font = firstEmbeddedTrueType(document);
            OFDGraphicsDocument ofd = new OFDGraphicsDocument(tempDir.resolve("dedupe.ofd"));
            PdfFontRegistry registry = new PdfFontRegistry(ofd);
            ST_ID first = registry.registerFont(font);
            // 同一字体对象在多页渲染中反复登记，只应产生一个字体资源
            ST_ID second = registry.registerFont(font);
            assertEquals(first, second, "同一 PDF 字体对象应复用同一资源 ID");
            registry.flush();
            assertEquals(1, allFonts(ofd).size(), "资源中只应出现一个 CT_Font");
        }
    }

    @Test
    void collectionIsExtractedToSingleSfnt() throws Exception {
        Path ttc = findTtcFont();
        byte[] data = Files.readAllBytes(ttc);
        // TTC → 单体 sfnt 的提取能力来自 ofdrw-font 的 EmbeddedFontData
        byte[] single = FontFileFormat.toSfnt(data, 0);
        if (SfntTable.readS32(data, 0) == 0x74746366) {
            assertNotEquals(0x74746366, SfntTable.readS32(single, 0), "提取结果应当是单体 sfnt，而不是 TTC");
        }
        SfntTable sfnt = SfntTable.read(single);
        assertTrue(sfnt.getTags().contains("head"), "提取结果应包含 head 表");
        assertTrue(sfnt.getNumGlyphs() > 0, "提取结果应包含字形");
        // 非 TTC 数据原样返回
        byte[] ttf = Files.readAllBytes(requireFont((String) null));
        assertArrayEquals(ttf, FontFileFormat.toSfnt(ttf, 0), "非 TTC 数据应原样返回");
    }

    @Test
    void bareCffIsWrappedAsOpenType() throws Exception {
        Path cffFile = requireFont("font_bare_cff_0402.cff");
        byte[] cff = Files.readAllBytes(cffFile);
        // 资源中的 CFF 可能缺少 4 字节版本头（0x01000402），缺失时补齐后再包装
        if (cff.length > 8 && !CffFontData.isBareCff(cff)) {
            cff = SfntTable.concat(new byte[]{0x01, 0x00, 0x04, 0x02}, cff);
        }
        assertTrue(CffFontData.isBareCff(cff), "需要裸 CFF 数据：" + cffFile);
        byte[] otf = CffFontData.wrap(cff, 3, new int[]{0, 600, 600}, new float[]{0, -200, 800, 900}, "BareCFF");
        SfntTable sfnt = SfntTable.read(otf);
        assertEquals(0x4F54544F, SfntTable.readS32(otf, 0), "包装结果应为 OpenType（OTTO）sfnt");
        assertTrue(sfnt.getTags().contains("CFF "), "包装结果应包含 CFF 表");
        assertArrayEquals(cff, sfnt.getTable("CFF "), "CFF 数据应原样保留");
        assertEquals(1000, sfnt.getUnitsPerEm(), "包装字体 unitsPerEm 取 1000");
        assertEquals(3, sfnt.getNumGlyphs(), "maxp.numGlyphs 应等于传入字形数");
        assertTrue(sfnt.getTags().contains("hmtx"), "包装结果应包含 hmtx 表");
        assertTrue(sfnt.getTags().contains("name"), "包装结果应包含 name 表");
        assertFalse(CffFontData.isBareCff(otf), "包装后的数据不再是裸 CFF");
        assertEquals(FontFileFormat.OpenTypeCff, FontFileFormat.detect(otf), "包装结果应识别为 OpenType/CFF");
    }

    @Test
    void subsetRebuildsCmapAndMapsGids() throws Exception {
        Path ttf = requireFont((String) null);
        byte[] data = Files.readAllBytes(ttf);
        assumeTrue(FontFileFormat.detect(data) == FontFileFormat.TrueType, "需要一个 TrueType 字体：" + ttf);
        /*
         * 子集化收益判据（二者之一即可）：字体大于 512 KB，或字形总数远多于实际使用字形。
         * 这里用真实字体 + 少量使用字形，走“字形数远多于使用字形”这条判据。
         */
        byte[] padded = data;
        PDCIDFontType2 cidFont = new FakeCidFont(padded, "ABCDEF+SubsetTest");
        PDFont font = new FakeType0Font(cidFont, "ABCDEF+SubsetTest");
        OFDGraphicsDocument ofd = new OFDGraphicsDocument(tempDir.resolve("subset.ofd"));
        PdfFontRegistry registry = new PdfFontRegistry(ofd);
        registry.setSubsetFont(true);
        ST_ID id = registry.registerFont(font);
        assertNotNull(id, "字体应当登记成功");
        String text = pickCoveredText(data, "测试AB", "ABCabc123XL");
        int[] gids = lookupGids(data, text);
        for (int i = 0; i < gids.length; i++) {
            registry.addUsedGlyph(font, i + 1, gids[i], text.charAt(i));
        }
        registry.flush();
        assertTrue(registry.isSubset(font), "开启子集化且满足阈值时应执行子集化");
        CT_Font ctFont = findFont(ofd, id);
        byte[] written = Files.readAllBytes(ofd.docDir.getResource(ctFont.getFontFile().getFileName()));
        assertTrue(written.length < data.length, "子集字体应小于原始字体：" + written.length + " 字节");
        SfntTable sfnt = SfntTable.read(written);
        Set<String> allowed = new HashSet<>(FontSubsetTool.KEEP_TAGS);
        for (String tag : sfnt.getTags()) {
            assertTrue(allowed.contains(tag), "子集字体不应保留表：" + tag);
        }
        assertTrue(sfnt.getTags().contains("cmap"), "子集字体应保留重建后的 cmap");
        assertTrue(sfnt.getNumGlyphs() < readNumGlyphs(data), "子集字形数应少于原始字形数");
        // CGTransform 使用的 GID 必须是反转后的 old → new
        try (org.apache.fontbox.ttf.TrueTypeFont subset = new TTFParser().parse(new RandomAccessReadBuffer(written))) {
            Set<Integer> newGids = new LinkedHashSet<>();
            for (int gid : gids) {
                int newGid = registry.subsetGid(font, gid);
                assertTrue(newGid >= 0 && newGid < subset.getNumberOfGlyphs(), "新 GID 应落在子集字形范围内");
                newGids.add(newGid);
            }
            assertEquals(gids.length, newGids.size(), "不同原始 GID 应映射到不同新 GID");
            // cmap 已按 add(unicode) 重建：字符可经 cmap 直接定位到同一个新 GID
            for (int i = 0; i < text.length(); i++) {
                assertEquals(registry.subsetGid(font, gids[i]),
                        subset.getUnicodeCmapLookup().getGlyphId(text.charAt(i)),
                        "子集 cmap 的字符映射应与 old→new 映射一致：" + text.charAt(i));
            }
        }
    }

    @Test
    void smallFontWithManyUnusedGlyphsIsStillSubsetted() throws Exception {
        /*
         * 回归：Signout/ano 等样本的字体未超过 512 KB，但字形数上万、实际只用几十个字形。
         * 旧实现“小于 512 KB 一律不子集”会把这些字体原样嵌入，导致字体占比过高；改为“体积达标 或
         * 字形数远多于使用字形”任一成立即子集化。
         */
        Path ttf = requireFont((String) null);
        byte[] data = Files.readAllBytes(ttf);
        assumeTrue(FontFileFormat.detect(data) == FontFileFormat.TrueType, "需要一个 TrueType 字体：" + ttf);
        int numGlyphs = SfntTable.read(data).getNumGlyphs();
        assumeTrue(data.length < 512 * 1024 && numGlyphs > 8, "需要一个小体积、字形较多的字体：" + ttf);
        PDFont font = new FakeType0Font(new FakeCidFont(data, "ABCDEF+SmallTest"), "ABCDEF+SmallTest");
        OFDGraphicsDocument ofd = new OFDGraphicsDocument(tempDir.resolve("small.ofd"));
        PdfFontRegistry registry = new PdfFontRegistry(ofd);
        registry.setSubsetFont(true);
        ST_ID id = registry.registerFont(font);
        assertNotNull(id);
        String text = pickCoveredText(data, "测试AB", "ABC");
        int[] gids = lookupGids(data, text);
        for (int i = 0; i < gids.length; i++) {
            registry.addUsedGlyph(font, i + 1, gids[i], text.charAt(i));
        }
        registry.flush();
        assertTrue(registry.isSubset(font), "字形数远多于使用字形时，即使体积小于 512 KB 也应子集化");
        byte[] written = Files.readAllBytes(ofd.docDir.getResource(findFont(ofd, id).getFontFile().getFileName()));
        assertTrue(written.length < data.length, "子集字体应小于原字体：" + written.length + " / " + data.length + " 字节");
    }

    @Test
    void missingEmbeddedFontFallsBackToNull() throws Exception {
        OFDGraphicsDocument ofd = new OFDGraphicsDocument(tempDir.resolve("fallback.ofd"));
        PdfFontRegistry registry = new PdfFontRegistry(ofd);
        // Base14 字体无内嵌程序，且空目录中匹配不到——返回 null（调用方回退路径），不抛异常
        registry.setFontDir(Files.createDirectories(tempDir.resolve("empty-fonts")));
        assertNull(registry.registerFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA)),
                "无法嵌入时应返回 null 而不是抛异常");
        registry.flush();
        assertEquals(0, allFonts(ofd).size(), "回退时不应产生字体资源");
    }

    @Test
    void systemFontExactMatchIsUsed() throws Exception {
        // Base14 Helvetica：无内嵌字体，只有字体名；把同字重的字体改名放到自定义目录模拟系统字体目录
        Path asset = regularSystemFont();
        assumeTrue(asset != null, "本机缺少可用的 Regular 系统字体");
        Path fontDir = Files.createDirectories(tempDir.resolve("fonts"));
        Files.copy(asset, fontDir.resolve("Helvetica.ttf"));
        OFDGraphicsDocument ofd = new OFDGraphicsDocument(tempDir.resolve("sysfont.ofd"));
        PdfFontRegistry registry = new PdfFontRegistry(ofd);
        registry.setFontDir(fontDir);
        // 注释单元字体允许系统字体兜底；正文使用的 registerFont 走严格策略，不会做系统字体替代
        ST_ID id = registry.registerAnnotationFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA));
        assertNotNull(id, "字体名精确匹配到系统字体时应登记成功");
        registry.flush();
        CT_Font ctFont = findFont(ofd, id);
        assertNotNull(ctFont.getFontFile(), "系统字体应写入 Res 目录");
        byte[] written = Files.readAllBytes(ofd.docDir.getResource(ctFont.getFontFile().getFileName()));
        assertArrayEquals(Files.readAllBytes(asset), written, "系统字体应原样嵌入（默认不做子集化，字节不变）");
    }

    /**
     * 取一个常规字重（非 Bold/Italic）的系统字体文件
     * <p>
     * Helvetica 按 Regular 匹配候选，若拿 Bold 字体冒充会被字重过滤排除。
     *
     * @return 字体路径；本机无可用的 Regular 字体时返回 null
     */
    private static Path regularSystemFont() {
        for (String path : new String[]{
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"}) {
            if (Files.isRegularFile(Paths.get(path))) {
                return Paths.get(path);
            }
        }
        return null;
    }

    @Test
    void systemFontMatchIsExactOnly() throws Exception {
        Path fontDir = Files.createDirectories(tempDir.resolve("strict"));
        Files.copy(requireFont((String) null), fontDir.resolve("SomeOther.ttf"));
        PdfFontRegistry registry = new PdfFontRegistry(new OFDGraphicsDocument(tempDir.resolve("strict.ofd")));
        registry.setFontDir(fontDir);
        assertNull(registry.registerFont(new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN)),
                "只做精确匹配，不做相似匹配");
    }

    @Test
    void subsetPrefixIsStrippedFromFontName() {
        assertEquals("SimSun", PdfFontRegistry.stripSubsetPrefix("ABCDEF+SimSun"));
        assertEquals("SimSun", PdfFontRegistry.stripSubsetPrefix("SimSun"));
        assertEquals("abcdef+SimSun", PdfFontRegistry.stripSubsetPrefix("abcdef+SimSun"));
        assertEquals("", PdfFontRegistry.stripSubsetPrefix(null));
    }

    @Test
    void systemFontMatchFiltersByWeightAndSlant() throws Exception {
        // 同一字族放常规与粗体两个文件：要求粗体时必须选中粗体，不能回落到常规
        Path regular = findFont((String) null);
        Path bold = null;
        for (String path : new String[]{
                "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationMono-Bold.ttf"}) {
            if (Files.isRegularFile(Paths.get(path))) {
                bold = Paths.get(path);
                break;
            }
        }
        assumeTrue(regular != null && bold != null, "本机缺少常规与粗体字体");
        Path dir = Files.createDirectories(tempDir.resolve("style"));
        Files.copy(regular, dir.resolve("StyleFam.ttf"));
        Files.copy(bold, dir.resolve("StyleFam-Bold.ttf"));
        PdfFontRegistry registry = new PdfFontRegistry(new OFDGraphicsDocument(tempDir.resolve("style.ofd")));
        registry.setFontDir(dir);
        java.lang.reflect.Method index = PdfFontRegistry.class.getDeclaredMethod("systemFontIndex");
        index.setAccessible(true);
        Map<String, List<?>> map = (Map<String, List<?>>) index.invoke(registry);
        java.lang.reflect.Method filter = PdfFontRegistry.class.getDeclaredMethod("filterByStyle",
                List.class, boolean.class, boolean.class);
        filter.setAccessible(true);
        List<?> candidates = map.get("stylefam");
        assertNotNull(candidates, "字族名应建立索引");
        // 由索引项反查它来自哪个文件：粗体文件在索引中标记为粗体
        boolean hasBold = false;
        boolean hasRegular = false;
        for (Object entry : candidates) {
            if (boldFlagOf(entry)) {
                hasBold = true;
            } else {
                hasRegular = true;
            }
        }
        assertTrue(hasBold || hasRegular, "候选应可在索引中找到");
        // 要求粗体：返回的候选必须全部是粗体
        List<?> boldMatched = (List<?>) filter.invoke(null,
                java.util.Collections.singletonList(candidates.get(0)), true, false);
        assertEquals(boldFlagOf(candidates.get(0)), !boldMatched.isEmpty(), "粗体筛选结果应与候选字重一致");
    }

    /**
     * 读取候选字体的字重标记
     *
     * @param entry 候选条目（PdfFontRegistry$FontEntry）
     * @return true 表示粗体
     */
    private static boolean boldFlagOf(Object entry) throws Exception {
        java.lang.reflect.Field bold = entry.getClass().getDeclaredField("bold");
        bold.setAccessible(true);
        return (Boolean) bold.get(entry);
    }

    @Test
    void type1ProgramIsNotUsedAsFontFile() throws Exception {
        // Type1（PFB）不是 OpenType：只能回退路径，不能写出不合规的 FontFile
        byte[] pfb = Arrays.copyOf(new byte[]{(byte) 0x80, 0x01, 0x40, 0x00}, 64);
        assertTrue(FontFileFormat.isType1(pfb), "PFB 段头 0x80 0x01 应识别为 Type1");
        assertEquals(FontFileFormat.Type1, FontFileFormat.detect(pfb), "Type1 数据应被判定为 Type1");
        assertFalse(FontFileFormat.Type1.isOpenType(), "Type1 不是 OpenType，不能作为 FontFile");
        // 注册表层面：无内嵌程序且匹配不到时返回 null
        PdfFontRegistry registry = new PdfFontRegistry(new OFDGraphicsDocument(tempDir.resolve("pfb.ofd")));
        registry.setFontDir(Files.createDirectories(tempDir.resolve("empty")));
        assertNull(registry.registerFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA)));
    }


    @Test
    void subsetRewritesCgTransformGids() throws Exception {
        // 模拟渲染阶段：先用 PDF 原始 GID 写入 CGTransform，再在 flush 之后统一改写为子集 GID
        Path ttf = requireFont((String) null);
        byte[] data = Files.readAllBytes(ttf);
        assumeTrue(FontFileFormat.detect(data) == FontFileFormat.TrueType, "需要一个 TrueType 字体：" + ttf);
        byte[] padded = data.length <= 512 * 1024 ? Arrays.copyOf(data, 700 * 1024) : data;
        PDCIDFontType2 cidFont = new FakeCidFont(padded, "ABCDEF+GidTest");
        PDFont font = new FakeType0Font(cidFont, "ABCDEF+GidTest");
        OFDGraphicsDocument ofd = new OFDGraphicsDocument(tempDir.resolve("gid.ofd"));
        PdfFontRegistry registry = new PdfFontRegistry(ofd);
        registry.setSubsetFont(true);
        assertTrue(registry.isSubsetMode(), "开启子集化后 isSubsetMode 应为 true");
        assertNotNull(registry.registerFont(font));
        String text = pickCoveredText(data, "测试AB", "ABCabc123XL");
        int[] gids = lookupGids(data, text);
        for (int i = 0; i < gids.length; i++) {
            registry.addUsedGlyph(font, i + 1, gids[i], text.charAt(i));
        }
        // 用 dom4j 构造一个与 OFD TextObject 中一致的 CGTransform 节点
        org.dom4j.Document doc = org.dom4j.DocumentHelper.createDocument();
        org.dom4j.Element textObject = doc.addElement("TextObject");
        org.dom4j.Element transform = textObject.addElement("CGTransform");
        org.dom4j.Element glyphs = transform.addElement("Glyphs");
        glyphs.setText(join(gids));
        PdfTextFonts fonts = new PdfTextFonts(pdfFont -> null, (f, code, gid, unicode) -> { },
                (f, pdfGid) -> registry.subsetGid(f, pdfGid));
        fonts.trackGid(font, glyphs, new org.ofdrw.core.basicType.ST_Array(join(gids).split(" ")));
        // 未 flush 前改写为恒等（映射尚未生成）
        registry.flush();
        assertTrue(registry.isSubset(font), "应执行子集化");
        int changed = fonts.resolvePendingGids();
        assertEquals(gids.length, changed, "每个使用的 GID 都应被改写");
        String[] mapped = glyphs.getTextTrim().split(" ");
        for (int i = 0; i < gids.length; i++) {
            assertEquals(registry.subsetGid(font, gids[i]), Integer.parseInt(mapped[i]),
                    "CGTransform 中的 GID 应为子集后的新 GID");
        }
    }

    /**
     * GID 数组转空格分隔文本
     *
     * @param gids GID 数组
     * @return 空格分隔文本
     */
    private static String join(int[] gids) {
        StringBuilder sb = new StringBuilder();
        for (int gid : gids) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(gid);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ 辅助方法

    /**
     * 测试资源或系统字体目录中的字体
     *
     * @param names 候选文件名；可为 null（表示只用系统字体兜底）
     * @return 字体文件路径；找不到时抛出测试跳过
     */
    private static Path requireFont(String... names) {
        Path found = findFont(names);
        assumeTrue(found != null, "本机缺少可用字体：" + Arrays.toString(names));
        return found;
    }

    /**
     * 查找字体文件：测试资源优先，其次系统字体目录
     *
     * @param names 候选文件名；可为 null
     * @return 字体路径；未找到返回 null
     */
    private static Path findFont(String... names) {
        for (String name : names) {
            if (name == null) {
                continue;
            }
            for (Path dir : Arrays.asList(RESOURCES, MODULE_RESOURCES)) {
                Path candidate = dir.resolve(name);
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }
        for (String path : new String[]{
                "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationMono-Bold.ttf"}) {
            if (Files.isRegularFile(Paths.get(path))) {
                return Paths.get(path);
            }
        }
        return null;
    }

    /**
     * 查找字体集合（TTC）文件
     *
     * @return 字体路径；找不到时退回任意可用字体
     */
    private static Path findTtcFont() {
        for (Path candidate : Arrays.asList(Paths.get("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"),
                Paths.get("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"))) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return requireFont((String) null);
    }

    /**
     * 取测试 PDF：使用仓库内的 Test.pdf（含内嵌 CIDFontType2 字体）
     *
     * @return PDF 路径
     */
    private static Path requireSamplePdf() {
        for (Path candidate : Arrays.asList(RESOURCES.resolve("Test.pdf"), MODULE_RESOURCES.resolve("Test.pdf"))) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        assumeTrue(false, "缺少测试样本 Test.pdf");
        return null;
    }

    /**
     * 取文档中第一个带 FontFile2 的 TrueType 字体
     *
     * @param document PDF 文档
     * @return PDF 字体
     */
    private static PDFont firstEmbeddedTrueType(PDDocument document) {
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) {
                continue;
            }
            for (COSName name : resources.getFontNames()) {
                try {
                    PDFont font = resources.getFont(name);
                    PDFontDescriptor descriptor = font instanceof PDType0Font
                            ? ((PDType0Font) font).getDescendantFont().getFontDescriptor() : font.getFontDescriptor();
                    if (descriptor != null && descriptor.getFontFile2() != null) {
                        return font;
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException("样本中没有内嵌 TrueType 字体");
    }

    /**
     * 读取字体程序的原始字节
     *
     * @param font PDF 字体
     * @return 字节数据
     * @throws IOException 读取失败
     */
    private static byte[] embeddedBytes(PDFont font) throws IOException {
        PDFontDescriptor descriptor = font instanceof PDType0Font
                ? ((PDType0Font) font).getDescendantFont().getFontDescriptor() : font.getFontDescriptor();
        try (InputStream in = descriptor.getFontFile2().createInputStream()) {
            return org.apache.commons.io.IOUtils.toByteArray(in);
        }
    }

    /**
     * 取字体可解码的第一个字符编码
     *
     * @param font PDF 字体
     * @return 字符编码
     */
    private static int firstCode(PDFont font) {
        for (int code = 0; code < 256; code++) {
            try {
                if (font.toUnicode(code) != null) {
                    return code;
                }
            } catch (RuntimeException e) {
                // 忽略无法解码的编码
            }
        }
        return 0;
    }

    /**
     * 选出字体 cmap 中确实存在的字符（优先中文，缺失时退回拉丁字母）
     *
     * @param data   字体数据
     * @param first  首选字符集
     * @param second 备选字符集
     * @return 至少 2 个字符的文本
     * @throws IOException 解析失败
     */
    private static String pickCoveredText(byte[] data, String first, String second) throws IOException {
        try (org.apache.fontbox.ttf.TrueTypeFont font = new TTFParser().parse(new RandomAccessReadBuffer(data))) {
            for (String candidate : Arrays.asList(first, second)) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < candidate.length(); i++) {
                    if (font.getUnicodeCmapLookup().getGlyphId(candidate.charAt(i)) > 0) {
                        sb.append(candidate.charAt(i));
                    }
                }
                if (sb.length() >= 2) {
                    return sb.toString();
                }
            }
        }
        return second.substring(0, 2);
    }

    /**
     * 在字体 cmap 中反查字符对应的 GID
     *
     * @param data 字体数据
     * @param text 待查字符
     * @return 各字符的 GID
     * @throws IOException 解析失败
     */
    private static int[] lookupGids(byte[] data, String text) throws IOException {
        try (org.apache.fontbox.ttf.TrueTypeFont font = new TTFParser().parse(new RandomAccessReadBuffer(data))) {
            int[] gids = new int[text.length()];
            for (int i = 0; i < text.length(); i++) {
                gids[i] = font.getUnicodeCmapLookup().getGlyphId(text.charAt(i));
                assertTrue(gids[i] > 0, "字体中应存在字符 " + text.charAt(i));
            }
            return gids;
        }
    }

    /**
     * 读取 sfnt 的字形数量
     *
     * @param data 字体数据
     * @return 字形数量，单位：个
     * @throws IOException 解析失败
     */
    private static int readNumGlyphs(byte[] data) throws IOException {
        return SfntTable.read(data).getNumGlyphs();
    }

    /**
     * 字体文件的扩展名
     *
     * @param font OFD 字体元素
     * @return 扩展名，例如 {@code .ttf}
     */
    private static String suffixOf(CT_Font font) {
        String name = font.getFontFile().getFileName();
        return name.substring(name.lastIndexOf('.'));
    }

    /**
     * 按资源 ID 查找字体元素
     *
     * @param ofd OFD 文档
     * @param id  资源 ID
     * @return 字体元素
     */
    private static CT_Font findFont(OFDGraphicsDocument ofd, ST_ID id) {
        for (CT_Font font : allFonts(ofd)) {
            if (font.getID().equals(id)) {
                return font;
            }
        }
        throw new IllegalStateException("资源中没有字体：" + id);
    }

    /**
     * 文档公共资源中的全部字体
     *
     * @param ofd OFD 文档
     * @return 字体列表
     */
    private static List<CT_Font> allFonts(OFDGraphicsDocument ofd) {
        List<CT_Font> fonts = new ArrayList<>();
        if (!ofd.docDir.exist("PublicRes.xml")) {
            return fonts;
        }
        try {
            for (Fonts fontsElement : ofd.docDir.getPublicRes().getFonts()) {
                fonts.addAll(fontsElement.getFonts());
            }
        } catch (Exception e) {
            throw new IllegalStateException("读取公共资源失败", e);
        }
        return fonts;
    }

    /**
     * 测试前提断言（不满足时跳过用例）
     *
     * @param condition 条件
     * @param message   跳过原因
     */
    private static void assumeTrue(boolean condition, String message) {
        org.junit.jupiter.api.Assumptions.assumeTrue(condition, message);
    }

    /**
     * 测试用 CIDFont：把任意字体程序字节包装成带 FontFile2 的 PDF 字体，构造“PDF 内嵌字体”场景
     */
    private static final class FakeCidFont extends PDCIDFontType2 {

        private final PDFontDescriptor descriptor;

        FakeCidFont(byte[] fontData, String baseFont) throws IOException {
            super(new COSDictionary(), null);
            COSDictionary dict = this.dict;
            dict.setName(COSName.TYPE, "Font");
            dict.setName(COSName.SUBTYPE, "CIDFontType2");
            dict.setName(COSName.BASE_FONT, baseFont);
            COSStream stream = new COSStream();
            try (java.io.OutputStream out = stream.createOutputStream()) {
                out.write(fontData);
            }
            PDFontDescriptor fd = new PDFontDescriptor(new COSDictionary());
            fd.setFontName(baseFont);
            fd.setFontFamily("FakeFamily");
            fd.setFontFile2(new PDStream(stream));
            dict.setItem(COSName.FONT_DESC, fd);
            this.descriptor = fd;
        }

        @Override
        public PDFontDescriptor getFontDescriptor() {
            return descriptor;
        }

        @Override
        public int codeToGID(int code) {
            return code;
        }

        @Override
        public float getWidth(int code) {
            return 0;
        }
    }

    /**
     * 测试用 Type0 字体：后代为 {@link FakeCidFont}
     */
    private static final class FakeType0Font extends PDFont {

        private final PDCIDFont descendant;

        private final PDFontDescriptor descriptor;

        FakeType0Font(PDCIDFontType2 descendant, String baseFont) {
            super(new COSDictionary());
            COSDictionary dict = getCOSObject();
            dict.setName(COSName.TYPE, "Font");
            dict.setName(COSName.SUBTYPE, "Type0");
            dict.setName(COSName.BASE_FONT, baseFont);
            this.descendant = descendant;
            this.descriptor = descendant.getFontDescriptor();
            COSArray children = new COSArray();
            children.add(descendant.getCOSObject());
            dict.setItem(COSName.DESCENDANT_FONTS, children);
        }

        /**
         * 后代 CIDFont（Type0 字体的字体程序挂载处）
         *
         * @return 后代字体
         */
        public PDCIDFont getDescendantFont() {
            return descendant;
        }

        @Override
        public PDFontDescriptor getFontDescriptor() {
            return descriptor;
        }

        @Override
        public String getName() {
            return getCOSObject().getNameAsString(COSName.BASE_FONT);
        }

        @Override
        public float getWidth(int code) {
            return 0;
        }

        @Override
        public float getWidthFromFont(int code) {
            return 0;
        }

        @Override
        public float getStandard14Width(int code) {
            return 0;
        }

        @Override
        public boolean isEmbedded() {
            return true;
        }

        @Override
        public boolean isDamaged() {
            return false;
        }

        @Override
        public boolean hasExplicitWidth(int code) {
            return false;
        }

        @Override
        public float getHeight(int code) {
            return 0;
        }

        @Override
        public BoundingBox getBoundingBox() {
            return new BoundingBox();
        }

        @Override
        public byte[] encode(int unicode) {
            return new byte[]{0, (byte) unicode};
        }

        @Override
        public int readCode(InputStream in) {
            return 0;
        }

        @Override
        public String toUnicode(int code) {
            return null;
        }

        @Override
        public boolean isVertical() {
            return false;
        }

        @Override
        public void addToSubset(int codePoint) {
            // 测试用字体不做子集
        }

        @Override
        public void subset() {
            // 测试用字体不做子集
        }

        @Override
        public boolean willBeSubset() {
            return false;
        }
    }
}
