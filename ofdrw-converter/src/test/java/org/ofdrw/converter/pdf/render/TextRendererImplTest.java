package org.ofdrw.converter.pdf.render;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.font.FontSelectorImpl;
import org.ofdrw.converter.pdf.write.PdfFontFactory;
import org.ofdrw.converter.pdf.font.TrueTypePdfFont;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.PdfPageContent;
import org.ofdrw.converter.pdf.model.TextCommand;
import org.ofdrw.converter.pdf.write.PdfTextWriter;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.text.CT_CGTransform;
import org.ofdrw.core.text.TextCode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2 文字管线端到端测试：OFD TextObject → 中间模型指令 → PDF 内容流 → 文本提取
 * <p>
 * 断言链：
 * <ol>
 *     <li>渲染阶段：{@link TextRendererImpl} 输出一个 {@link TextCommand}，字形位置为页面坐标（决策 1 逐点换算）；</li>
 *     <li>写入阶段：{@link PdfTextWriter} 以字形索引写出文字，并挂 ToUnicode；</li>
 *     <li>提取阶段：PDFBox {@link PDFTextStripper} 提取的文字与 OFD 原文一致（决策 3“可提取、可搜索”）。</li>
 * </ol>
 *
 * @author 权观宇
 * @since 2.5.1
 */
class TextRendererImplTest {

    /**
     * 测试字体
     */
    private static final String FONT = "src/test/resources/font_13132_0_edit.ttf";

    /**
     * 构造一个横向文字对象
     *
     * @param text     文字内容
     * @param boundary 外接矩形，单位 mm
     * @param sizeMm   字号，单位 mm
     * @return 文字对象
     */
    private static TextObject textObject(String text, org.ofdrw.core.basicType.ST_Box boundary, double sizeMm) {
        TextObject object = new TextObject(1L);
        object.setBoundary(boundary);
        object.setSize(sizeMm);
        object.setFont(10L);
        TextCode code = new TextCode();
        code.setContent(text);
        // 表 46：TextCode 的 X/Y 是相对外接矩形角点（CharDirection=0 时即左上角）的偏移，缺省为 (0, 0)
        code.setCoordinate(0d, 0d);
        object.addTextCode(code);
        return object;
    }

    @Test
    @DisplayName("颜色：图元未给 FillColor 时沿用 DrawParam 的填充色（备注水印浅灰）")
    void fillColorFallsBackToDrawParam() throws IOException {
        TextObject object = textObject("A", box(10, 20, 30, 10), 4);
        object.setDrawParam(org.ofdrw.core.basicType.ST_RefID.getInstance("8"));
        // DrawParam 8 提供浅灰填充色
        org.ofdrw.core.pageDescription.drawParam.CT_DrawParam param =
                new org.ofdrw.core.pageDescription.drawParam.CT_DrawParam();
        param.setFillColor(org.ofdrw.core.pageDescription.color.color.CT_Color.rgb(127, 127, 127));
        TrueTypePdfFont font = TrueTypePdfFont.create("F1",
                Files.readAllBytes(Paths.get(FONT)), 0, false);
        GraphicsStateResolver resolver = new GraphicsStateResolver(id -> "8".equals(id) ? param : null);
        TextRendererImpl renderer = new TextRendererImpl(new ColorResolver(id -> null), resolver, id -> null);
        PdfPageContent content = new PdfPageContent(595.28, 841.89);
        RenderContext context = RenderContexts.create(210, 297, content, selector(font));
        TextCommand command = (TextCommand) renderer.render(object, context).stream()
                .filter(c -> c instanceof TextCommand).findFirst().orElse(null);
        assertNotNull(command);
        // 127/255 ≈ 0.498，说明颜色来自 DrawParam，而不是退化为纯黑
        assertEquals(127f / 255f, command.getColor().getChannel(1), 1e-6);
    }

    @Test
    @DisplayName("透明度：图元 Alpha 生成 SetAlpha，并作用于文字颜色")
    void alphaProducesSetAlpha() throws IOException {
        TextObject object = textObject("A", box(10, 20, 30, 10), 4);
        object.setAlpha(76);
        TrueTypePdfFont font = TrueTypePdfFont.create("F1", Files.readAllBytes(Paths.get(FONT)), 0, false);
        PdfPageContent content = new PdfPageContent(595.28, 841.89);
        RenderContext context = RenderContexts.create(210, 297, content, selector(font));
        List<ContentCommand> commands = new TextRendererImpl().render(object, context);
        org.ofdrw.converter.pdf.model.SetAlpha alpha = (org.ofdrw.converter.pdf.model.SetAlpha) commands.stream()
                .filter(c -> c instanceof org.ofdrw.converter.pdf.model.SetAlpha).findFirst().orElse(null);
        assertNotNull(alpha, "显式 Alpha 应生成 SetAlpha 指令");
        assertEquals(76f / 255f, alpha.getFillAlpha(), 1e-6);
    }

    /**
     * 构造只含指定字体的选择器
     *
     * @param font 字体引用
     * @return 字体选择器
     */
    private static FontSelectorImpl selector(TrueTypePdfFont font) {
        FontSelectorImpl selector = new FontSelectorImpl(null);
        selector.getRegistry().register(font);
        selector.getRegistry().resolved(10L, font);
        selector.fontResolver(id -> new org.ofdrw.core.text.font.CT_Font("sysfST"));
        return selector;
    }

    /**
     * 构造外接矩形
     *
     * @param x 左上角横坐标，单位 mm
     * @param y 左上角纵坐标，单位 mm
     * @param w 宽度，单位 mm
     * @param h 高度，单位 mm
     * @return 矩形
     */
    private static org.ofdrw.core.basicType.ST_Box box(double x, double y, double w, double h) {
        return new org.ofdrw.core.basicType.ST_Box(x, y, w, h);
    }

    /**
     * 渲染并返回文字指令
     *
     * @param object 文字对象
     * @return 文字指令
     */
    private static TextCommand render(TextObject object) throws IOException {
        return renderWith(object, new TrueTypePdfFont[1], FONT);
    }

    /**
     * 构造字体选择器：按资源 ID 返回测试字体
     *
     * @param holder 用于回传本次选择的字体引用（与渲染结果中的资源名一致）
     * @return 字体选择器
     */
    private static FontSelectorImpl fontSelector(TrueTypePdfFont[] holder, String fontPath) throws IOException {
        TrueTypePdfFont font = TrueTypePdfFont.create("F1",
                Files.readAllBytes(Paths.get(fontPath)), 0, false);
        holder[0] = font;
        FontSelectorImpl selector = new FontSelectorImpl(null);
        selector.getRegistry().register(font);
        selector.getRegistry().resolved(10L, font);
        selector.fontResolver(id -> new org.ofdrw.core.text.font.CT_Font("sysfST"));
        return selector;
    }

    /**
     * 渲染并回传本次使用的字体引用
     *
     * @param object 文字对象
     * @param holder 字体引用回传数组
     * @return 文字指令
     * @throws IOException 读取字体失败
     */
    private static TextCommand renderWith(TextObject object, TrueTypePdfFont[] holder, String fontPath) throws IOException {
        PdfPageContent content = new PdfPageContent(595.28, 841.89);
        RenderContext context = RenderContexts.create(210, 297, content, fontSelector(holder, fontPath));
        List<ContentCommand> commands = new TextRendererImpl().render(object, context);
        // 包装固定为 q + [SetAlpha] + TextCommand + Q（未显式声明 Alpha 时不输出 SetAlpha）
        assertTrue(commands.get(0) instanceof org.ofdrw.converter.pdf.model.SaveState);
        assertTrue(commands.get(commands.size() - 1) instanceof org.ofdrw.converter.pdf.model.RestoreState);
        TextCommand text = (TextCommand) commands.get(commands.size() - 2);
        assertTrue(commands.size() == 3 || commands.size() == 4, "指令数：3（无透明度）或 4（含 SetAlpha）");
        return text;
    }

    @Test
    @DisplayName("渲染：Boundary + TextCode X/Y 换算为页面坐标（Y 轴翻转，决策 1）")
    void renderPageCoordinates() throws IOException {
        // 210×297mm 页面，外接矩形 (10,20)，TextCode 偏移 (0,0) → 对象空间起点 (10,20)，4mm 字号
        TextCommand command = render(textObject("AB", box(10, 20, 30, 10), 4));
        assertEquals(2, command.getGlyphs().size());
        assertEquals("AB", command.getUnicodeText());
        // 字号按内容坐标比例换算：4mm → 11.3384pt
        assertEquals(CoordSystem.mmToPt(4), command.getFontSizePt(), 1e-9);
        // Tm 原点 = 外接矩形左上角 + TextCode 偏移 的页面坐标
        double[] expected = CoordSystem.pointToPage(10, 20, 297);
        assertEquals(expected[0], command.getTextMatrix().getE(), 1e-6);
        assertEquals(expected[1], command.getTextMatrix().getF(), 1e-6);
        assertNotNull(command.getFontRef());
        // 首个字形文本空间位移为 0
        assertEquals(0, command.getGlyphs().get(0).getX(), 1e-9);
        assertEquals(0, command.getGlyphs().get(0).getY(), 1e-9);
        // 第二个字形按 Delta 未声明 → 同址
        assertEquals(0, command.getGlyphs().get(1).getX(), 1e-9);
    }

    @Test
    @DisplayName("渲染：DeltaX/DeltaY 与 CGTransform 影响字形索引与位移")
    void renderDeltaAndGlyph() throws IOException {
        TextObject object = textObject("AB", box(0, 0, 30, 10), 4);
        TextCode code = object.getTextCodes().get(0);
        code.setDeltaX(5d, 6d);
        object.setHScale(0.8);
        // CGTransform：首个字符显式指定字形 469
        CT_CGTransform cg = new CT_CGTransform();
        cg.setCodePosition(0);
        cg.setCodeCount(2);
        cg.setGlyphCount(2);
        cg.setGlyphs(new org.ofdrw.core.basicType.ST_Array(469, 470));
        object.addCGTransform(cg);

        TextCommand command = render(object);
        assertEquals(469, command.getGlyphs().get(0).getGlyphId());
        assertEquals(470, command.getGlyphs().get(1).getGlyphId());
        // 位移为 Delta 按内容坐标比例换算的 pt（5mm）
        assertEquals(CoordSystem.mmToPt(5), command.getGlyphs().get(1).getX(), 1e-6);
        // 字形宽度按 HScale 压缩（0.8 × 4mm），字号本身不变
        assertEquals(CoordSystem.mmToPt(4), command.getFontSizePt(), 1e-9);
        // 字形前进量由字体度量决定，只校验其为有限正数
        assertTrue(command.getGlyphs().get(0).getAdvancePt() > 0);
    }

    @Test
    @DisplayName("渲染：无字体选择器时跳过并返回空指令（容错不中断）")
    void renderWithoutFontSelector() {
        PdfPageContent content = new PdfPageContent(595.28, 841.89);
        RenderContext context = RenderContexts.create(210, 297, content, null);
        TextObject object = textObject("AB", box(10, 20, 30, 10), 4);
        assertTrue(new TextRendererImpl().render(object, context).isEmpty());
    }

    @Test
    @DisplayName("渲染：非文字对象被忽略")
    void renderNonText() {
        PdfPageContent content = new PdfPageContent(595.28, 841.89);
        RenderContext context = RenderContexts.create(210, 297, content, null);
        assertTrue(new TextRendererImpl().render("not a text", context).isEmpty());
        assertTrue(new TextRendererImpl().render(null, context).isEmpty());
        assertEquals("TextRenderer", new TextRendererImpl().getName());
    }

    /**
     * 可被 PDFBox 直接加载的字体样本
     * <p>
     * 20240531141733.ofd 内嵌字体经 ofdrw-font 裁剪后 loca 表偏移全为 0（PDFBox 解析器会拒绝），
     * 这里按字形等距重建 loca 表，使同一字体既能被 ofdrw-font 解析，也能被 PDFBox 嵌入。
     */
    private static final String SYSTEM_FONT = "src/test/resources/font_loca_patched.ttf";

    /**
     * 渲染并写出为 PDF，返回 PDF 字节
     *
     * @param object 文字对象
     * @param font   字体引用
     * @param text   文字指令
     * @return PDF 文件字节
     * @throws IOException 写出失败
     */
    private static byte[] writePdf(TextObject object, TrueTypePdfFont font, TextCommand text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(595.28f, 841.89f));
            document.addPage(page);
            PDResources resources = new PDResources();
            page.setResources(resources);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                new PdfTextWriter(new PdfFontFactory()).write(document, page, stream, resources, font, text);
            }
            assertNotNull(resources.getFont(org.apache.pdfbox.cos.COSName.getPDFName(font.getResourceName())));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    /**
     * 构造使用系统字体的文字对象与字体引用
     *
     * @param text 文字内容
     * @return [文字对象, 字体引用]
     * @throws IOException 字体读取失败
     */
    private static Object[] systemFontCase(String text) throws IOException {
        TrueTypePdfFont font = TrueTypePdfFont.create("F1", Files.readAllBytes(Paths.get(SYSTEM_FONT)), 0, false);
        FontSelectorImpl selector = new FontSelectorImpl(null);
        selector.getRegistry().register(font);
        selector.getRegistry().resolved(10L, font);
        selector.fontResolver(id -> new org.ofdrw.core.text.font.CT_Font("DejaVu Sans"));
        TextObject object = textObject(text, box(10, 20, 30, 10), 6);
        return new Object[]{object, font, selector};
    }

    @Test
    @DisplayName("写入 + 提取：字形优先写出，ToUnicode 保证文本可提取")
    void writeAndExtract() throws IOException {
        TextObject object = textObject("AB", box(10, 20, 30, 10), 6);
        TrueTypePdfFont[] holder = new TrueTypePdfFont[1];
        TextCommand command = renderWith(object, holder, SYSTEM_FONT);
        TrueTypePdfFont font = holder[0];
        assertEquals("F1", command.getFontRef());

        byte[] pdf = writePdf(object, font, command);
        // 字体字典挂载了 ToUnicode（决策 3）
        assertNotNull(font.toUnicodeCMap());
        try (PDDocument reloaded = Loader.loadPDF(pdf)) {
            PDFont pdfFont = reloaded.getPage(0).getResources()
                    .getFont(org.apache.pdfbox.cos.COSName.getPDFName("F1"));
            assertNotNull(pdfFont);
            assertNotNull(pdfFont.getCOSObject().getDictionaryObject(org.apache.pdfbox.cos.COSName.TO_UNICODE));
            // 决策 16：整字体嵌入，不子集化 —— FontFile2 解码后长度必须等于字体文件长度
            org.apache.pdfbox.pdmodel.font.PDFont loaded = reassignFont(reloaded);
            assertEquals(Files.size(Paths.get(SYSTEM_FONT)), fontFileLength(loaded),
                    "FontFile2 应与整字体数据等长（不做子集裁剪）");
            String text = new PDFTextStripper().getText(reloaded).trim();
            assertEquals("AB", text);
        }
    }

    /**
     * 取出 PDF 页面上的字体对象
     *
     * @param document 已加载的 PDF
     * @return 字体对象
     */
    private static org.apache.pdfbox.pdmodel.font.PDFont reassignFont(PDDocument document) throws IOException {
        return document.getPage(0).getResources().getFont(org.apache.pdfbox.cos.COSName.getPDFName("F1"));
    }

    /**
     * 读取字体嵌入流（FontFile2 / FontFile3）的解码后长度
     *
     * @param font 字体对象
     * @return 长度，单位：字节；没有嵌入流时返回 -1
     * @throws IOException 读取失败
     */
    private static long fontFileLength(org.apache.pdfbox.pdmodel.font.PDFont font) throws IOException {
        org.apache.pdfbox.pdmodel.font.PDFontDescriptor descriptor = font.getFontDescriptor();
        if (descriptor == null) {
            return -1;
        }
        org.apache.pdfbox.pdmodel.common.PDStream stream = descriptor.getFontFile2() != null
                ? descriptor.getFontFile2() : descriptor.getFontFile3();
        if (stream == null) {
            return -1;
        }
        long total = 0;
        try (InputStream in = stream.createInputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                total += read;
            }
        }
        return total;
    }

    @Test
    @DisplayName("写入 + 提取：裁剪字体走整字体嵌入，FontFile2 与原始字体等长且文本可提取")
    void writeWithFallbackFont() throws IOException {
        TextObject object = textObject("AB", box(10, 20, 30, 10), 6);
        TrueTypePdfFont[] holder = new TrueTypePdfFont[1];
        TextCommand command = renderWith(object, holder, FONT);
        TrueTypePdfFont font = holder[0];
        byte[] pdf = writePdf(object, font, command);
        try (PDDocument reloaded = Loader.loadPDF(pdf)) {
            org.apache.pdfbox.pdmodel.font.PDFont loaded = reassignFont(reloaded);
            // 决策 16：整字体嵌入，不做子集裁剪
            assertEquals(Files.size(Paths.get(FONT)), fontFileLength(loaded),
                    "FontFile2 应与整字体数据等长");
            assertTrue(new PDFTextStripper().getText(reloaded).contains("A"));
        }
    }

    @Test
    @DisplayName("坐标：TextCode 的 X/Y 是相对外接矩形的对象空间偏移，起点需叠加 Boundary")
    void placementUsesBoundary() throws IOException {
        // 外接矩形左上角 (10,20)，TextCode 偏移 (5,5) → 对象空间起点 (15,25)
        TextObject object = new TextObject(1L);
        object.setBoundary(box(10, 20, 30, 10));
        object.setSize(4d);
        object.setFont(10L);
        TextCode code = new TextCode();
        code.setContent("A");
        code.setCoordinate(5d, 5d);
        object.addTextCode(code);
        TextCommand command = render(object);
        // Tm 为「Boundary 左上角 + TextCode 偏移」的页面坐标；漏掉 Boundary 会得到 (5,5) 的错误位置
        double[] expected = CoordSystem.pointToPage(15, 25, 297);
        assertEquals(expected[0], command.getTextMatrix().getE(), 1e-6);
        assertEquals(expected[1], command.getTextMatrix().getF(), 1e-6);
        assertNotNull(new Box(0, 0, 1, 1));
    }
}
