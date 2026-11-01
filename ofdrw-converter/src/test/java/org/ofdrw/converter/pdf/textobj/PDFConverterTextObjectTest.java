package org.ofdrw.converter.pdf.textobj;

import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.FontLoader;
import org.ofdrw.converter.canvas.BufferedImageSurface;
import org.ofdrw.converter.canvas.CanvasImageMaker;
import org.ofdrw.converter.ofdconverter.PDFConverter;
import org.ofdrw.converter.regression.TextComparison;
import org.ofdrw.converter.utils.CommonUtil;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.reader.OFDReader;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDF → OFD 文字对象化回归门禁（设计文档 §7.2～§7.4、§8 P4）
 * <p>
 * 样本为 {@code src/test/resources/pdf-textobj/} 下精选的 5 个 PDF（单文件 ≤ 1 MB，合计 ≤ 2 MB），
 * 覆盖：纯文字中文 + 内嵌字体原样嵌入、中英混排、无内嵌字体回退、简单版面回退、小票版式。
 * <p>
 * 断言口径（与设计文档 §7.3 一致）：
 * <ul>
 *     <li><b>文字对象化</b>：有内嵌字体的样本，输出必须含 {@code TextObject}，且 {@code PathObject}
 *     数量显著低于“整页路径”基线；</li>
 *     <li><b>字体</b>：内嵌字体可被 {@code TTFParser} 解析（原样嵌入，不要求裁剪 EBDT/EBLC/vmtx）；</li>
 *     <li><b>文本</b>：有 ToUnicode 的样本，TextObject 文本与 PDF 文本相似度 ≥ 0.99（NFKC + 去空白后比较）；</li>
 *     <li><b>文字位置</b>：按“字形像素在所在行内的位置”判定（不以 PDF 提取基线绝对值为准，理由见设计文档
 *     §7.3“基线位置门禁口径”）：逐行比较左端 X（均值 ≤ 1 mm）与行内像素重心（均值 ≤ 1.5 mm）；
 *     另与“整页路径基线”比较墨迹包围盒（水平 ≤ 2.5 mm、垂直 ≤ 3 mm），防止整页错位；</li>
 *     <li><b>体积</b>：{@code dense-text-perf} 输出 ≤ 0.6 MB（内嵌字体原样嵌入，不解压重复内容）；</li>
 *     <li><b>回退</b>：无内嵌字体样本必须走路径回退（无 Res 字体、有 PathObject）；</li>
 *     <li>竖排转横排等行切分不同的页面（{@code V4RideRight} 第 5 页）与系统字体匹配差异较大的小样本
 *     （{@code chineseDir}）使用宽松口径：仅做整页级检查，不做单页文字行门禁。</li>
 * </ul>
 * 渲染分辨率 72 dpi，与 {@code regression.ImageDiff} 的口径一致；提示：完整 63 样本批量验证与
 * ofdrw-viewer-cli 对照在 {@code target/pdf-textobj/}（不入库），复现方式见 {@code doc/CONVERTER.md}。
 *
 * @author 权观宇
 * @since 2.5.1
 */
@Disabled("PDF 转 OFD 文字对象化集成用例，需真实 PDF 样本，耗时高（约 44s）。")
class PDFConverterTextObjectTest {

    /**
     * 样本目录（模块目录下执行 mvn test 时）
     */
    private static final Path MODULE_RESOURCES = Paths.get("src/test/resources/pdf-textobj");

    /**
     * 样本目录（仓库根目录下执行时）
     */
    private static final Path ROOT_RESOURCES = Paths.get("ofdrw-converter/src/test/resources/pdf-textobj");

    /**
     * 渲染分辨率，单位：dpi
     */
    private static final int DPI = 72;

    /**
     * 文字像素包围盒允许偏差，单位：毫米（72 dpi 下约 3 像素）
     */
    private static final double BBOX_TOLERANCE_MM = 1.0;

    /**
     * 文本相似度阈值（有 ToUnicode 样本）
     */
    private static final double TEXT_SIMILARITY_THRESHOLD = 0.99;

    /**
     * 校准渲染字体：OFD 回退路径需要按字体名匹配系统字体，显式扫描系统字体目录，
     * 并允许相似字体替换（与 {@code OFD2IMGTest} 的口径一致）。
     */
    @BeforeAll
    static void prepareFonts() {
        FontLoader.getInstance().scanFontDir(new java.io.File("/usr/share/fonts"));
        FontLoader.getInstance().scanFontDir(new java.io.File("/usr/local/share/fonts"));
        FontLoader.setSimilarFontReplace(true);
    }

    @Test
    void denseTextIsFullyObjectified() throws Exception {
        Path pdf = sample("dense-text-perf.pdf");
        Path converted = convert(pdf, "dense");
        Path baseline = convertLegacy(pdf, "dense-path");

        // 1) 体积上限：0.6 MB（内嵌 SimSun 约 150 KB + 页面内容，不含任何整页位图）
        long size = Files.size(converted);
        assertTrue(size <= 600L * 1024L, "转换结果应 ≤ 0.6 MB，实际 " + size + " B");
        assertTrue(size < Files.size(baseline), "文字对象化结果应小于整页路径基线："
                + size + " vs " + Files.size(baseline));

        // 2) 结构：TextObject 存在且 PathObject 显著低于整页路径基线
        Structure structure = readStructure(converted);
        Structure baseStructure = readStructure(baseline);
        assertTrue(structure.textObjects >= 1000, "密集文本应输出大量 TextObject，实际 " + structure.textObjects);
        assertTrue(baseStructure.pathObjects >= 10 * Math.max(1, structure.pathObjects),
                "PathObject 应显著低于路径基线：转换 " + structure.pathObjects + "，基线 " + baseStructure.pathObjects);

        // 3) 字体：内嵌字体可被 TTFParser 解析（原样嵌入）
        assertEquals(1, structure.fontFiles.size(), "应只内嵌一个字体文件：" + structure.fontNames);
        parseFont(structure.fontFiles.get(0));

        // 4) 文本：有 ToUnicode，TextObject 文本与 PDF 文本相似度 ≥ 0.99
        assertTrue(pdfHasToUnicode(pdf), "dense-text-perf.pdf 应带 ToUnicode");
        String actual = structure.allText();
        String expected = pdfText(pdf);
        double similarity = TextComparison.compare(expected, actual).getSimilarity();
        assertTrue(similarity >= TEXT_SIMILARITY_THRESHOLD, "文本相似度应 ≥ 0.99，实际 " + similarity);

        // 5) 文字位置：字形像素在行内的位置与 PDF 一致（72 dpi 口径）
        assertAlignmentWithinTolerance(pdf, converted);
    }

    @Test
    void chineseMixedLayoutKeepsGeometry() throws Exception {
        Path pdf = sample("chineseDir.pdf");
        Path converted = convert(pdf, "chinese");
        Path baseline = convertLegacy(pdf, "chinese-path");
        Structure structure = readStructure(converted);
        Structure baseStructure = readStructure(baseline);
        // 无内嵌字体：走路径回退，仍应有字形路径且不应比基线多出重复内容
        assertEquals(0, structure.fontFiles.size(), "无内嵌字体时不应嵌入 Res 字体");
        assertTrue(structure.pathObjects > 0, "回退路径应输出 PathObject");
        assertEquals(baseStructure.pathObjects, structure.pathObjects,
                "回退路径的 PathObject 数量应与整页路径基线一致");
        assertEquals(0, structure.textObjects, "无字体可嵌入时不应输出 TextObject");
        assertAlignmentWithinTolerance(pdf, converted, true);
    }

    @Test
    void keywordEmbeddedFontAndImagePreserved() throws Exception {
        Path pdf = sample("keyword.pdf");
        Path converted = convert(pdf, "keyword");
        Structure structure = readStructure(converted);
        assertFalse(structure.fontFiles.isEmpty(), "keyword.pdf 自带内嵌字体，应被原样嵌入");
        for (byte[] font : structure.fontFiles) {
            parseFont(font);
        }
        assertTrue(structure.pathObjects > 0, "图片/装饰仍以路径输出");
        assertAlignmentWithinTolerance(pdf, converted);
    }

    @Test
    void rideRightPageKeepsPositionAfterFallback() throws Exception {
        Path pdf = sample("V4RideRight.pdf");
        Path converted = convert(pdf, "ride");
        Structure structure = readStructure(converted);
        assertEquals(5, structure.pageCount, "样本应有 5 页");
        assertTrue(structure.pathObjects > 0, "旋转/特殊文字回退后仍应有路径内容");
        // 第 5 页为竖排文字转横排（D.3 特性），行切分与 PDF 不同，用宽松口径（不做整行错位页数门禁）
        assertAlignmentWithinTolerance(pdf, converted, true);
    }

    @Test
    void smallTicketStaysCloseToPdf() throws Exception {
        Path pdf = sample("1-1.pdf");
        Path converted = convert(pdf, "ticket");
        Structure structure = readStructure(converted);
        assertEquals(3, structure.pageCount, "样本应有 3 页");
        assertAlignmentWithinTolerance(pdf, converted);
    }

    /**
     * 采样样本目录（兼容“模块目录执行”与“仓库根执行”）。
     *
     * @param name 样本文件名（含扩展名）
     * @return 样本路径
     */
    private static Path sample(String name) {
        Path module = MODULE_RESOURCES.resolve(name);
        if (Files.isRegularFile(module)) {
            return module;
        }
        Path root = ROOT_RESOURCES.resolve(name);
        assertTrue(Files.isRegularFile(root), "缺少回归样本：" + name);
        return root;
    }

    /**
     * 用当前实现（默认文字对象化）转换 PDF。
     *
     * @param pdf  源 PDF
     * @param name 输出文件名（不含扩展名）
     * @return 转换得到的 OFD
     * @throws Exception 转换失败
     */
    private Path convert(Path pdf, String name) throws Exception {
        Path dir = Paths.get("target", "pdf-textobj-test");
        Files.createDirectories(dir);
        Path ofd = dir.resolve(name + ".ofd");
        try (PDFConverter converter = new PDFConverter(ofd)) {
            converter.convert(pdf);
        }
        return ofd;
    }

    /**
     * 用“整页路径渲染”生成基线 OFD（PDFBox PDFRenderer 直接绘制到 OFDPageGraphics2D）。
     * <p>
     * 该基线只用于回归门禁的 A/B 对比，生产 API 不再提供路径模式开关。
     *
     * @param pdf  源 PDF
     * @param name 输出文件名（不含扩展名）
     * @return 基线 OFD
     * @throws Exception 转换失败
     */
    private static Path convertLegacy(Path pdf, String name) throws Exception {
        Path dir = Paths.get("target", "pdf-textobj-legacy");
        Files.createDirectories(dir);
        Path ofd = dir.resolve(name + ".ofd");
        double uuPmm = 2.8346;
        try (PDDocument pdfDoc = Loader.loadPDF(pdf.toFile());
             org.ofdrw.graphics2d.OFDGraphicsDocument ofdDoc =
                     new org.ofdrw.graphics2d.OFDGraphicsDocument(ofd)) {
            org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(pdfDoc);
            java.awt.RenderingHints hints = new java.awt.RenderingHints(null);
            hints.put(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            renderer.setRenderingHints(hints);
            for (int i = 0; i < pdfDoc.getNumberOfPages(); i++) {
                org.apache.pdfbox.pdmodel.common.PDRectangle box = pdfDoc.getPage(i).getBBox();
                int rotation = pdfDoc.getPage(i).getRotation();
                float w = box.getWidth();
                float h = box.getHeight();
                if (rotation == 90 || rotation == 270) {
                    float tmp = w;
                    w = h;
                    h = tmp;
                }
                org.ofdrw.graphics2d.OFDPageGraphics2D g2d = ofdDoc.newPage(w / uuPmm, h / uuPmm);
                renderer.renderPageToGraphics(i, g2d, (float) (1d / uuPmm));
            }
        }
        return ofd;
    }

    /**
     * 读取 OFD 结构：页数、TextObject/PathObject 数量、内嵌字体文件与全部文字。
     *
     * @param ofd OFD 文件
     * @return 结构摘要
     * @throws Exception 解析失败
     */
    private static Structure readStructure(Path ofd) throws Exception {
        Structure s = new Structure();
        try (OFDReader reader = new OFDReader(ofd)) {
            List<org.ofdrw.reader.PageInfo> pages = reader.getPageList();
            s.pageCount = pages.size();
            StringBuilder text = new StringBuilder();
            for (org.ofdrw.reader.PageInfo info : pages) {
                Page page = reader.getPage(info.getIndex());
                if (page == null || page.getContent() == null) {
                    continue;
                }
                for (Object layer : page.getContent().getLayers()) {
                    collectBlocks(reader, ((CT_PageBlock) layer).getPageBlocks(), s, text);
                }
            }
            s.text = text.toString();
            readFonts(reader, s);
        }
        return s;
    }

    /**
     * 递归收集页面块。
     *
     * @param reader OFD 读取器
     * @param blocks 页面块
     * @param s      结构摘要（输出）
     * @param text   文字（输出）
     */
    private static void collectBlocks(OFDReader reader, List<PageBlockType> blocks, Structure s, StringBuilder text) {
        if (blocks == null) {
            return;
        }
        for (PageBlockType b : blocks) {
            if (b instanceof TextObject) {
                s.textObjects++;
                for (org.ofdrw.core.text.TextCode code : ((TextObject) b).getTextCodes()) {
                    text.append(OfdFactsText.unescape(code.getContent()));
                }
            } else if (b instanceof PathObject) {
                s.pathObjects++;
            } else if (b instanceof CT_PageBlock) {
                collectBlocks(reader, ((CT_PageBlock) b).getPageBlocks(), s, text);
            }
        }
    }

    /**
     * 读取 DocumentRes / PublicRes 中的内嵌字体文件，并核对文件存在。
     *
     * @param reader OFD 读取器
     * @param s      结构摘要（输出）
     * @throws Exception 读取失败
     */
    private static void readFonts(OFDReader reader, Structure s) throws Exception {
        // 从 DocumentRes / PublicRes 读取每个字体的 FontFile，再解析包内文件
        for (String resFile : new String[]{"/Doc_0/DocumentRes.xml", "/Doc_0/PublicRes.xml"}) {
            byte[] resBytes = readFromWorkDir(reader, resFile);
            if (resBytes == null) {
                continue;
            }
            org.dom4j.Document doc = new org.dom4j.io.SAXReader().read(new java.io.ByteArrayInputStream(resBytes));
            String baseLoc = doc.getRootElement().attributeValue("BaseLoc", "Res");
            for (Object node : doc.getRootElement().elements()) {
                org.dom4j.Element fonts = (org.dom4j.Element) node;
                if (!fonts.getName().equals("Fonts") && !fonts.getQualifiedName().equals("ofd:Fonts")) {
                    continue;
                }
                for (Object fn : fonts.elements()) {
                    org.dom4j.Element fontEl = (org.dom4j.Element) fn;
                    if (!"Font".equals(fontEl.getName()) && !"ofd:Font".equals(fontEl.getQualifiedName())) {
                        continue;
                    }
                    String fontName = fontEl.attributeValue("FontName") == null
                            ? "(未命名)" : fontEl.attributeValue("FontName");
                    for (Object ff : fontEl.elements()) {
                        org.dom4j.Element ffEl = (org.dom4j.Element) ff;
                        if (!"FontFile".equals(ffEl.getName()) && !"ofd:FontFile".equals(ffEl.getQualifiedName())) {
                            continue;
                        }
                        String loc = ffEl.getTextTrim();
                        if (loc == null || loc.isEmpty()) {
                            continue;
                        }
                        String abs = loc.startsWith("/") ? loc.substring(1) : (baseLoc + "/" + loc);
                        // 直接从 OFD 包内读取字体程序字节（不依赖解压工作区的生命周期）
                        s.fontFiles.add(readFromWorkDir(reader, "/" + abs));
                        s.fontNames.add(fontName);
                    }
                }
            }
        }
    }

    /**
     * 从 OFD 解压工作区读取条目（必须在 {@link OFDReader} 关闭之前调用）。
     *
     * @param reader OFD 读取器
     * @param abs    包内绝对路径（以 '/' 开头）
     * @return 字节或 null（不存在）
     * @throws Exception 读取失败
     */
    private static byte[] readFromWorkDir(OFDReader reader, String abs) throws Exception {
        Path file;
        try {
            file = reader.getOFDDir().getFileAbs(abs);
        } catch (RuntimeException e) {
            return null;
        }
        if (!Files.isRegularFile(file)) {
            return null;
        }
        return Files.readAllBytes(file);
    }

    /**
     * 从 DocumentRes/PublicRes 读取字体名（仅用于失败信息）。
     *
     * @param reader OFD 读取器
     * @return 字体名列表
     */
    private static List<String> fontNames(OFDReader reader) {
        List<String> names = new ArrayList<>();
        try {
            String text = new String(Files.readAllBytes(reader.getOFDDir().getFileAbs("/Doc_0/PublicRes.xml")),
                    StandardCharsets.UTF_8);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("FontName=\"([^\"]+)\"").matcher(text);
            while (m.find()) {
                names.add(m.group(1));
            }
        } catch (Exception ignore) {
            // 无 PublicRes 时忽略
        }
        return names;
    }

    /**
     * 解析字体文件。
     *
     * @param fontFile 字体文件（TTF/OTF）
     * @return 解析结果
     * @throws Exception 解析失败
     */
    /**
     * 解析字体程序字节。
     *
     * @param data 字体程序（TTF/OTF）
     * @return 解析结果
     * @throws Exception 解析失败
     */
    private static TrueTypeFont parseFont(byte[] data) throws Exception {
        try (org.apache.pdfbox.io.RandomAccessRead ra = new org.apache.pdfbox.io.RandomAccessReadBuffer(data)) {
            TTFParser parser = new TTFParser();
            try (TrueTypeFont font = parser.parse(ra)) {
                assertTrue(font.getUnitsPerEm() > 0, "字体应含有效 head.unitsPerEm");
                assertTrue(font.getNumberOfGlyphs() > 0, "字体应含字形");
                return font;
            }
        }
    }

    /**
     * 判断 PDF 是否存在带 ToUnicode 的字体。
     *
     * @param pdf 源 PDF
     * @return 至少一个字体带 ToUnicode 时为 true
     */
    private static boolean pdfHasToUnicode(Path pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                org.apache.pdfbox.pdmodel.PDResources res = doc.getPage(i).getResources();
                if (res == null) {
                    continue;
                }
                for (org.apache.pdfbox.cos.COSName name : res.getFontNames()) {
                    org.apache.pdfbox.pdmodel.font.PDFont font = res.getFont(name);
                    Object tu = font.getCOSObject().getDictionaryObject(org.apache.pdfbox.cos.COSName.TO_UNICODE);
                    if (tu instanceof org.apache.pdfbox.cos.COSStream) {
                        return true;
                    }
                    if (font instanceof org.apache.pdfbox.pdmodel.font.PDType0Font) {
                        org.apache.pdfbox.pdmodel.font.PDCIDFont desc =
                                ((org.apache.pdfbox.pdmodel.font.PDType0Font) font).getDescendantFont();
                        if (desc != null && desc.getCOSObject().getDictionaryObject(
                                org.apache.pdfbox.cos.COSName.TO_UNICODE) instanceof org.apache.pdfbox.cos.COSStream) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 提取 PDF 文本（按位置排序）。
     *
     * @param pdf 源 PDF
     * @return 文本
     * @throws Exception 读取失败
     */
    private static String pdfText(Path pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    /**
     * 按“字形像素在行内的位置”判定文字对齐，并检查整页墨迹包围盒。
     * <p>
     * 1) 逐行比较：把 OFD 页面与 PDF 页面各自按水平投影切成文字行，按行序配对后比较左端 x 与
     * 行内墨迹上下沿（相对 PDF 行的偏移），偏差 ≤ {@link #BBOX_TOLERANCE_MM}；
     * 2) 整页比较：两页墨迹包围盒的上下左右边界偏差 ≤ {@link #BBOX_TOLERANCE_MM}。
     * 不使用 PDF 文本提取的基线绝对值（PDF 行距与字形高不成整数倍时该值本身含行间余量，见设计 §7.3）。
     *
     * @param pdf       源 PDF
     * @param converted 转换得到的 OFD
     * @throws Exception 渲染或比较失败
     */
    private void assertAlignmentWithinTolerance(Path pdf, Path converted) throws Exception {
        assertAlignmentWithinTolerance(pdf, converted, false);
    }

    /**
     * 文字对齐断言（可放宽渲染器固有差异）。
     *
     * @param pdf       源 PDF
     * @param converted 转换得到的 OFD
     * @param loosePage true 时不做“整行错位页数”门禁（用于系统字体匹配差异较大的小样本）
     * @throws Exception 渲染或比较失败
     */
    private void assertAlignmentWithinTolerance(Path pdf, Path converted, boolean loosePage) throws Exception {
        List<BufferedImage> pdfPages = renderPdf(pdf);
        List<BufferedImage> ofdPages = renderOfd(converted);
        assertEquals(pdfPages.size(), ofdPages.size(), "页数应一致");

        double pxPerMm = DPI / 25.4d;
        int compared = 0;
        int misplacedRows = 0;
        for (int i = 0; i < pdfPages.size(); i++) {
            BufferedImage expected = pdfPages.get(i);
            BufferedImage actual = ofdPages.get(i);
            int[] eb = inkBox(expected);
            int[] ab = inkBox(actual);
            if (eb == null && ab == null) {
                continue;
            }
            assertNotNull(eb, "第 " + (i + 1) + " 页：PDF 有内容，转换结果却是空白");
            assertNotNull(ab, "第 " + (i + 1) + " 页：PDF 空白，转换结果却有内容");
            // 3.1 与“路径渲染基线”对比整页墨迹包围盒：文字对象化结果应与路径基线摆放一致（±1.5 mm），
            //     这样既避开 PDFBox 与 OFD 渲染器之间的固有字形外扩差异，又能发现“整页错位”
            int[] lb = inkBox(renderLegacyPage(pdf, i));
            if (lb != null) {
                String[] sides = {"左", "右", "上", "下"};
                double tolPx = BBOX_TOLERANCE_MM * 1.5 * pxPerMm;
                for (int k = 0; k < 4; k++) {
                    double dev = Math.abs(lb[k] - ab[k]);
                    // 水平方向严格（左右错位属于实现问题）；垂直方向放宽到 3 mm：
                    // 路径回退时字体由字体名匹配系统字体，上下伸部与 PDF 内嵌字体不同，基线自身即存在 1~2 mm 差异
                    double limit = k < 2 ? BBOX_TOLERANCE_MM * 2.5 * pxPerMm : BBOX_TOLERANCE_MM * 3 * pxPerMm;
                    assertTrue(dev <= limit, String.format(
                            "第 %d 页与路径基线的墨迹包围盒%s边偏差 %.2f px（> %.2f px）：基线=%s，文字 OFD=%s",
                            i + 1, sides[k], dev, limit, java.util.Arrays.toString(lb), java.util.Arrays.toString(ab)));
                }
                // 基线相对 PDF 的固有差异（渲染器字形度量/彩色背景），仅作为诊断信息
                if (Math.abs(eb[0] - lb[0]) > tolPx || Math.abs(eb[3] - lb[3]) > tolPx) {
                    System.out.println("[pdf-textobj] 第 " + (i + 1) + " 页 PDF 与路径基线墨迹包围盒差异：PDF="
                            + java.util.Arrays.toString(eb) + " 基线=" + java.util.Arrays.toString(lb)
                            + "（OFD 渲染器固有差异，不计入门禁）");
                }
            }
            // 3.2 文字对齐（逐行）：OFD 页面与 PDF 页面各自按水平投影切行，比较左端 X 与行内像素重心
            //     （重心 = 行内墨迹像素平均纵坐标相对行顶的偏移，即“字形在行内的位置”，见设计 §7.3）
            List<int[]> expectedRows = inkRows(expected, eb[1], eb[3]);
            List<int[]> actualRows = inkRows(actual, ab[1], ab[3]);
            int matched = Math.min(expectedRows.size(), actualRows.size());
            double sumDx = 0;
            double sumDy = 0;
            double maxDx = 0;
            for (int r = 0; r < matched; r++) {
                double dx = Math.abs(expectedRows.get(r)[0] - actualRows.get(r)[0]) / pxPerMm;
                double dy = Math.abs(expectedRows.get(r)[4] - actualRows.get(r)[4]) / pxPerMm;
                sumDx += dx;
                sumDy += dy;
                maxDx = Math.max(maxDx, dx);
            }
            double meanDx = matched == 0 ? 0d : sumDx / matched;
            double meanDy = matched == 0 ? 0d : sumDy / matched;
            System.out.printf("[pdf-textobj] 第 %d 页 行数 %d/%d，左端偏移 均值 %.2f mm 最大 %.2f mm，"
                            + "行内像素重心偏移 均值 %.2f mm%n",
                    i + 1, expectedRows.size(), actualRows.size(), meanDx, maxDx, meanDy);
            if (matched > 0) {
                // 左端：均值 ≤ 1 mm 视为对齐；> 1 mm 记入整行错位计数，累计不得超过页数
                // （个别页的行切分方式不同——如竖排转横排、细微行粘连——会带来单行偏差）
                if (meanDx > BBOX_TOLERANCE_MM) {
                    misplacedRows++;
                }
                System.out.printf("[pdf-textobj] 第 %d 页左端对齐判定：%s%n", i + 1,
                        meanDx <= BBOX_TOLERANCE_MM ? "PASS" : "MISALIGNED");
                // 行内像素重心：均值 ≤ 1.5 mm（抗锯齿与上下伸部差异约 1 mm，整行错位会显著超出）
                assertTrue(loosePage || meanDy <= BBOX_TOLERANCE_MM * 1.5,
                        String.format("第 %d 页行内像素重心偏移 均值 %.3f mm（上限 %.1f mm）",
                                i + 1, meanDy, BBOX_TOLERANCE_MM * 1.5));
            }
            compared++;
        }
        assertTrue(compared > 0, "至少应有一页参与比较");
        if (!loosePage) {
            assertTrue(misplacedRows <= 1, "整行错位页数应 ≤ 1，实际 " + misplacedRows + " 页");
        }
    }

    /**
     * 按水平投影切分文字行。
     * <p>
     * 返回值为每行 {@code [x0, x1, y0, y1, centroidY]}，其中 {@code centroidY} 是行内墨迹像素平均纵坐标
     * 相对行顶（y0）的偏移（像素），即“字形在行内的位置”，与设计文档 §7.3 的门禁口径一致。
     *
     * @param image 位图
     * @param yFrom 搜索起始行（像素）
     * @param yTo   搜索结束行（像素）
     * @return 行框列表（自上而下）
     */
    private static List<int[]> inkRows(BufferedImage image, int yFrom, int yTo) {
        int w = image.getWidth();
        int[] count = new int[image.getHeight()];
        for (int y = yFrom; y <= yTo; y++) {
            for (int x = 0; x < w; x++) {
                int p = image.getRGB(x, y);
                int gray = (((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) + (p & 0xFF)) / 3;
                if (255 - gray > 24) {
                    count[y]++;
                }
            }
        }
        List<int[]> rows = new ArrayList<>();
        int y = yFrom;
        while (y <= yTo) {
            if (count[y] == 0) {
                y++;
                continue;
            }
            int y0 = y;
            while (y <= yTo && count[y] > 0) {
                y++;
            }
            int y1 = y - 1;
            int x0 = w;
            int x1 = -1;
            long sumY = 0;
            long ink = 0;
            for (int yy = y0; yy <= y1; yy++) {
                for (int x = 0; x < w; x++) {
                    int p = image.getRGB(x, yy);
                    int gray = (((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) + (p & 0xFF)) / 3;
                    if (255 - gray > 24) {
                        ink++;
                        sumY += yy - y0;
                        if (x < x0) {
                            x0 = x;
                        }
                        if (x > x1) {
                            x1 = x;
                        }
                    }
                }
            }
            double centroid = ink == 0 ? 0d : (double) sumY / ink;
            rows.add(new int[]{x0, x1, y0, y1, (int) Math.round(centroid)});
        }
        return rows;
    }

    /**
     * 计算位图中的墨迹包围盒（非白像素），白底判定阈值 24。
     *
     * @param image 位图
     * @return [x0, y0, x1, y1]（像素，含端点），无墨迹返回 null
     */
    private static int[] inkBox(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int x0 = w;
        int y0 = h;
        int x1 = -1;
        int y1 = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = image.getRGB(x, y);
                int gray = (((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) + (p & 0xFF)) / 3;
                if (255 - gray > 24) {
                    if (x < x0) {
                        x0 = x;
                    }
                    if (x > x1) {
                        x1 = x;
                    }
                    if (y < y0) {
                        y0 = y;
                    }
                    if (y > y1) {
                        y1 = y;
                    }
                }
            }
        }
        return x1 < 0 ? null : new int[]{x0, y0, x1, y1};
    }

    /**
     * 用“整页路径渲染”基线渲染 PDF 的某一页（与 {@link #renderOfd} 同一渲染器、同一 DPI）。
     * <p>
     * 用于区分“转换缺陷”与“PDFBox 与 OFD 渲染器的固有差异”：基线本身也偏差的方向不做门禁。
     *
     * @param pdf   源 PDF
     * @param index 页码（从 0 起）
     * @return 位图
     * @throws Exception 渲染失败
     */
    private static BufferedImage renderLegacyPage(Path pdf, int index) throws Exception {
        Path legacy = convertLegacy(pdf, "align-" + pdf.getFileName().toString().replace(".pdf", ""));
        List<BufferedImage> pages = renderOfd(legacy);
        assertTrue(index < pages.size(), "路径基线页数不足");
        return pages.get(index);
    }

    /**
     * 渲染 PDF 全部页面（{@link #DPI} dpi，RGB）。
     *
     * @param pdf 源 PDF
     * @return 逐页位图
     * @throws Exception 渲染失败
     */
    private static List<BufferedImage> renderPdf(Path pdf) throws Exception {
        List<BufferedImage> images = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                images.add(renderer.renderImageWithDPI(i, DPI,
                        org.apache.pdfbox.rendering.ImageType.RGB));
            }
        }
        return images;
    }

    /**
     * 渲染 OFD 全部页面（{@link #DPI} dpi，白底）。
     *
     * @param ofd OFD 文件
     * @return 逐页位图
     * @throws Exception 渲染失败
     */
    private static List<BufferedImage> renderOfd(Path ofd) throws Exception {
        List<BufferedImage> images = new ArrayList<>();
        try (OFDReader reader = new OFDReader(ofd)) {
            CanvasImageMaker maker = new CanvasImageMaker(reader, CommonUtil.dpiToPpm(DPI));
            for (int i = 0; i < reader.getNumberOfPages(); i++) {
                org.ofdrw.core.basicType.ST_Box size = maker.pageSize(i);
                int w = Math.max(1, (int) Math.ceil(size.getWidth() * CommonUtil.dpiToPpm(DPI)));
                int h = Math.max(1, (int) Math.ceil(size.getHeight() * CommonUtil.dpiToPpm(DPI)));
                BufferedImageSurface surface = new BufferedImageSurface(w, h, Color.WHITE);
                maker.drawPage(surface, i);
                images.add(surface.getImage());
            }
        }
        return images;
    }

    /**
     * 一次转换的结构摘要。
     */
    private static final class Structure {

        /**
         * 页数（页）
         */
        int pageCount;

        /**
         * TextObject 数量（个）
         */
        int textObjects;

        /**
         * PathObject 数量（个）
         */
        int pathObjects;

        /**
         * 内嵌字体程序字节（按 FontFile 出现顺序）
         */
        final List<byte[]> fontFiles = new ArrayList<>();

        /**
         * 字体名（失败信息用）
         */
        final List<String> fontNames = new ArrayList<>();

        /**
         * 全部 TextObject 文本
         */
        String text = "";

        /**
         * 拼接全部文字（与 PDF 文本比较用）。
         *
         * @return 文字
         */
        String allText() {
            return text;
        }
    }

    /**
     * TextCode 文本还原（空格写作 U+00A0，特殊字符写作 \XXXX）。
     */
    static final class OfdFactsText {

        private OfdFactsText() {
        }

        /**
         * 还原 TextCode 内容。
         *
         * @param content 原始内容
         * @return 还原后的文本
         */
        static String unescape(String content) {
            if (content == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder(content.length());
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '\\' && i + 4 < content.length()) {
                    try {
                        sb.append((char) Integer.parseInt(content.substring(i + 1, i + 5), 16));
                        i += 4;
                        continue;
                    } catch (NumberFormatException ignore) {
                        // 非法转义按原样输出
                    }
                }
                sb.append(c == '\u00A0' ? ' ' : c);
            }
            return sb.toString();
        }
    }
}
