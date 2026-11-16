package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.ofdconverter.PDFConverter;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.reader.OFDReader;

import java.awt.Color;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDF → OFD 文字对象分流回归（Issue #416）
 * <p>
 * {@code TextObject} 的 {@code Size}、{@code FillColor} 等均为<b>对象级</b>属性，一个文字对象只能按首字形的
 * 字号与颜色绘制。同一字体、字号或颜色不同的相邻文字若被合并进同一对象，整段会按首字形步进绘制而重叠。
 * <p>
 * 本用例构造“同字体 + 大字号标题 + 小字号表头 + 同字号换色正文 + 同字号同色连续正文”的 PDF，断言：
 * <ul>
 *     <li>字号或颜色不同的三段各自独立成 {@code TextObject}，字号与颜色正确；</li>
 *     <li>字号与颜色相同的连续两段仍合并为同一 {@code TextObject}，对象数不膨胀。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
class TextObjectRunSplitTest {

    /**
     * 测试用 TrueType 字体（DejaVu Sans，随发行版字体包提供）
     */
    private static final Path FONT = Paths.get("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");

    /**
     * OFD 页面尺寸换算：1 mm 对应的 PDF 用户空间单位（pt）
     */
    private static final double PT_PER_MM = 2.8346d;

    @Test
    void differentSizeAndColorSplitIntoSeparateObjects() throws Exception {
        Path dir = Paths.get("target", "textobj-run-split");
        Files.createDirectories(dir);
        Path pdf = dir.resolve("mixed-size-color.pdf");
        Path ofd = dir.resolve("mixed-size-color.ofd");
        Files.deleteIfExists(ofd);

        // 1. 构造 PDF：同一字体，字号与颜色交替出现的相邻四段文字
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDType0Font font = PDType0Font.load(doc, new FileInputStream(FONT.toFile()), false);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // 大字号标题
                cs.beginText();
                cs.setFont(font, 24);
                cs.newLineAtOffset(72, 700);
                cs.showText("BIG TITLE");
                cs.endText();
                // 小字号表头：同一基线上紧接着再来一段同字号同色文字，应与上一段合并
                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset(72, 660);
                cs.showText("small header");
                cs.newLineAtOffset(60, 0);
                cs.showText("continued");
                cs.endText();
                // 同字号同色但换了基线：应另起对象
                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset(72, 640);
                cs.showText("same run body");
                cs.endText();
                // 同字号换色正文：应另起对象
                cs.beginText();
                cs.setNonStrokingColor(Color.RED);
                cs.setFont(font, 10);
                cs.newLineAtOffset(72, 620);
                cs.showText("red body");
                cs.endText();
            }
            doc.save(pdf.toFile());
        }

        // 2. 转换
        try (PDFConverter converter = new PDFConverter(ofd)) {
            converter.convert(pdf);
        }

        // 3. 断言文字对象分流结果
        List<TextObject> texts = readTextObjects(ofd);
        assertEquals(4, texts.size(), "字号、颜色或基线不同的文字必须分成独立的 TextObject");

        TextObject title = texts.get(0);
        assertEquals("BIG TITLE", content(title), "标题应独占一个文字对象");
        assertSize(title, 24);

        TextObject header = texts.get(1);
        assertEquals("small headercontinued", content(header),
                "同一基线上字号与颜色相同的连续文字应保持合并，不产生对象膨胀");
        assertSize(header, 10);
        assertEquals(0, rgb(header)[0], "表头应为黑色填充");

        TextObject body = texts.get(2);
        assertEquals("same run body", content(body), "换行文字应独占一个文字对象");
        assertSize(body, 10);
        assertEquals(0, rgb(body)[0], "正文应为黑色填充");

        TextObject red = texts.get(3);
        assertEquals("red body", content(red), "换色正文应独占一个文字对象");
        assertSize(red, 10);
        int[] redRgb = rgb(red);
        assertEquals(255, redRgb[0], "换色正文应保留红色填充");
        assertEquals(0, redRgb[1], "换色正文应保留红色填充");
        assertEquals(0, redRgb[2], "换色正文应保留红色填充");
    }

    /**
     * 断言文字对象的字号（单位 mm）与指定磅值一致
     *
     * @param text    文字对象
     * @param points  期望字号，单位 pt（PDF 用户空间）
     */
    private static void assertSize(TextObject text, double points) {
        double expected = points / PT_PER_MM;
        assertNotNull(text.getSize(), "文字对象应有 Size");
        assertEquals(expected, text.getSize().doubleValue(), 0.01d, "字号应与 PDF 中一致");
    }

    /**
     * 读取文字对象的 TextCode 内容
     *
     * @param text 文字对象
     * @return 文本内容
     */
    private static String content(TextObject text) {
        StringBuilder sb = new StringBuilder();
        for (TextCode code : text.getTextCodes()) {
            sb.append(code.getContent());
        }
        return sb.toString();
    }

    /**
     * 读取文字对象填充色的 RGB 分量
     *
     * @param text 文字对象
     * @return {r, g, b}，各分量 0~255；未设置填充色时视为黑色
     */
    private static int[] rgb(TextObject text) {
        if (text.getFillColor() == null) {
            return new int[]{0, 0, 0};
        }
        org.ofdrw.core.basicType.ST_Array value = text.getFillColor().getValue();
        return new int[]{value.get(0).intValue(), value.get(1).intValue(), value.get(2).intValue()};
    }

    /**
     * 读取 OFD 第一页全部文字对象（按输出顺序）
     *
     * @param ofd OFD 文件
     * @return 文字对象列表
     * @throws Exception 读取失败
     */
    private static List<TextObject> readTextObjects(Path ofd) throws Exception {
        List<TextObject> out = new ArrayList<>();
        try (OFDReader reader = new OFDReader(ofd)) {
            Page page = reader.getPage(1);
            assertNotNull(page, "应存在第 1 页");
            for (Object layer : page.getContent().getLayers()) {
                collect(((CT_PageBlock) layer).getPageBlocks(), out);
            }
        }
        assertTrue(out.size() > 0, "转换结果应包含文字对象");
        return out;
    }

    /**
     * 递归收集文字对象
     *
     * @param blocks 页面块
     * @param out    输出列表
     */
    private static void collect(List<PageBlockType> blocks, List<TextObject> out) {
        if (blocks == null) {
            return;
        }
        for (PageBlockType b : blocks) {
            if (b instanceof TextObject) {
                out.add((TextObject) b);
            } else if (b instanceof CT_PageBlock) {
                collect(((CT_PageBlock) b).getPageBlocks(), out);
            }
        }
    }
}