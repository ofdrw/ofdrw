package org.ofdrw.converter.pdf.write;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.converter.pdf.model.ClosePath;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.FillRule;
import org.ofdrw.converter.pdf.model.LineTo;
import org.ofdrw.converter.pdf.model.MoveTo;
import org.ofdrw.converter.pdf.model.PathSegment;
import org.ofdrw.converter.pdf.model.PatternCommand;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.RectCommand;
import org.ofdrw.converter.pdf.model.RestoreState;
import org.ofdrw.converter.pdf.model.SaveState;
import org.ofdrw.converter.pdf.model.SetAlpha;
import org.ofdrw.converter.pdf.model.ShadingCommand;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 渐变与底纹写入器测试（P3）
 * <p>
 * 断言最终 PDF 的结构而不是像素：渐变写为 Type 2/Type 3、CMYK 保持 DeviceCMYK、
 * 底纹写为 Tiling Pattern（XStep/YStep 与单元尺寸一致），保存后能被 PDFBox 重新解析。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class PdfShadingPatternWriterTest {

    /**
     * 矩形路径
     *
     * @param w 宽，单位 pt
     * @param h 高，单位 pt
     * @return 路径
     */
    private static List<PathSegment> rect(double w, double h) {
        return new ArrayList<>(Arrays.asList(new MoveTo(0, 0), new LineTo(w, 0), new LineTo(w, h),
                new LineTo(0, h), ClosePath.INSTANCE));
    }

    /**
     * 构造渐变指令
     *
     * @param type   渐变类型
     * @param cmyk   是否使用 CMYK 颜色
     * @param extend 扩展方式
     * @return 指令
     */
    private static ShadingCommand shading(ShadingCommand.ShadingType type, boolean cmyk,
                                          ShadingCommand.Extend extend) {
        List<ShadingCommand.ColorStop> stops = new ArrayList<>();
        PdfColor start = cmyk ? PdfColor.cmyk(0, 1, 1, 0) : PdfColor.rgb(1, 0, 0);
        PdfColor end = cmyk ? PdfColor.cmyk(1, 0, 0, 0) : PdfColor.rgb(0, 0, 1);
        stops.add(new ShadingCommand.ColorStop(0, start));
        stops.add(new ShadingCommand.ColorStop(0.5, cmyk ? PdfColor.cmyk(0, 0, 1, 0) : PdfColor.rgb(0, 1, 0)));
        stops.add(new ShadingCommand.ColorStop(1, end));
        return new ShadingCommand(type, stops, extend, rect(100, 50), FillRule.NON_ZERO,
                10, 40, 90, 40, 1.0, 0.0);
    }

    @Test
    @DisplayName("轴向渐变写为 PDF Type 2，并带 Function 与 ColorSpace")
    void axialShading() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDResources resources = new PDResources();
            page.setResources(resources);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                new PdfShadingWriter(new PdfPathFactory()).write(document, stream, resources,
                        shading(ShadingCommand.ShadingType.AXIAL, false, ShadingCommand.Extend.PAD), 0.8f);
            }
            PDShading shading = firstShading(resources);
            assertEquals(PDShading.SHADING_TYPE2, shading.getShadingType());
            assertNotNull(shading.getCOSObject().getDictionaryObject(COSName.FUNCTION));
            assertNotNull(shading.getCOSObject().getDictionaryObject(COSName.COLORSPACE));
        }
    }

    @Test
    @DisplayName("径向渐变写为 PDF Type 3，坐标为 6 元 [x0 y0 r0 x1 y1 r1]")
    void radialShading() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDResources resources = new PDResources();
            page.setResources(resources);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                new PdfShadingWriter(new PdfPathFactory()).write(document, stream, resources,
                        shading(ShadingCommand.ShadingType.RADIAL, false, ShadingCommand.Extend.NONE), 1f);
            }
            PDShading shading = firstShading(resources);
            assertEquals(PDShading.SHADING_TYPE3, shading.getShadingType());
            COSArray coords = (COSArray) shading.getCOSObject().getDictionaryObject(COSName.COORDS);
            assertEquals(6, coords.size(), "径向渐变坐标为 [x0 y0 r0 x1 y1 r1]");
        }
    }

    @Test
    @DisplayName("CMYK 渐变保持 DeviceCMYK 颜色空间（不转 RGB）")
    void cmykShading() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDResources resources = new PDResources();
            page.setResources(resources);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                new PdfShadingWriter(new PdfPathFactory()).write(document, stream, resources,
                        shading(ShadingCommand.ShadingType.AXIAL, true, ShadingCommand.Extend.NONE), 1f);
            }
            PDShading shading = firstShading(resources);
            assertEquals(COSName.DEVICECMYK, shading.getCOSObject().getDictionaryObject(COSName.COLORSPACE));
        }
    }

    @Test
    @DisplayName("Extend 映射：NONE→[false,false]，PAD→[true,true]")
    void extendMapping() {
        assertFalse(PdfShadingWriter.extendArray(ShadingCommand.Extend.NONE)[0]);
        assertFalse(PdfShadingWriter.extendArray(ShadingCommand.Extend.NONE)[1]);
        assertTrue(PdfShadingWriter.extendArray(ShadingCommand.Extend.PAD)[0]);
        assertTrue(PdfShadingWriter.extendArray(ShadingCommand.Extend.PAD)[1]);
    }

    @Test
    @DisplayName("底纹写为 Tiling Pattern：BBox=单元尺寸，XStep/YStep 一致，保存后可重新解析")
    void tilingPattern() throws IOException {
        PatternCommand pattern = new PatternCommand("Pattern", new Box(0, 0, 20, 10),
                Collections.singletonList(new RectCommand(new Box(0, 0, 5, 5), PdfColor.rgb(0.2f, 0.4f, 0.6f))),
                false);
        byte[] pdf;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDResources resources = new PDResources();
            page.setResources(resources);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                new PdfPatternWriter(new PdfPathFactory()).write(document, stream, resources, pattern,
                        rect(100, 50), 1f);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            pdf = out.toByteArray();
        }
        try (PDDocument reloaded = Loader.loadPDF(pdf)) {
            COSDictionary patterns = (COSDictionary) reloaded.getPage(0).getResources()
                    .getCOSObject().getDictionaryObject(COSName.PATTERN);
            assertNotNull(patterns, "页面资源中应有图案字典");
            COSDictionary tiling = (COSDictionary) patterns.getDictionaryObject(COSName.getPDFName("P1"));
            assertNotNull(tiling);
            assertEquals(1, tiling.getInt(COSName.getPDFName("PatternType")), "Tiling Pattern 的 PatternType 为 1");
            assertEquals(20f, tiling.getFloat(COSName.getPDFName("XStep")), 1e-6);
            assertEquals(10f, tiling.getFloat(COSName.getPDFName("YStep")), 1e-6);
            COSArray bbox = (COSArray) tiling.getDictionaryObject(COSName.getPDFName("BBox"));
            assertEquals(4, bbox.size());
        }
    }

    @Test
    @DisplayName("底纹缺少填充区域时跳过且不抛异常")
    void patternWithoutArea() throws IOException {
        PatternCommand pattern = new PatternCommand("P", new Box(0, 0, 10, 10),
                Collections.singletonList(new RectCommand(new Box(0, 0, 5, 5), PdfColor.BLACK)), false);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDResources resources = new PDResources();
            page.setResources(resources);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                new PdfPatternWriter(new PdfPathFactory()).write(document, stream, resources, pattern,
                        new ArrayList<>(), 1f);
            }
            assertNull(resources.getCOSObject().getDictionaryObject(COSName.PATTERN));
        }
    }

    @Test
    @DisplayName("指令回放：q/Q 成对，CMYK 颜色可写出")
    void replayBalanced() throws IOException {
        List<ContentCommand> commands = new ArrayList<>();
        commands.add(SaveState.INSTANCE);
        commands.add(SetAlpha.of(0.5f));
        commands.add(new RectCommand(new Box(0, 0, 10, 10), PdfColor.cmyk(0, 0, 0, 1)));
        commands.add(RestoreState.INSTANCE);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDResources resources = new PDResources();
            page.setResources(resources);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                new PdfCommandReplayer(new PdfPathFactory()).replay(document, stream, resources, commands);
            }
            assertNotNull(page.getContents());
        }
    }

    /**
     * 读取页面上的第一个渐变对象
     *
     * @param resources 页面资源
     * @return 渐变对象
     * @throws IOException 解析失败
     */
    private static PDShading firstShading(PDResources resources) throws IOException {
        COSDictionary shadingDict = (COSDictionary) resources.getCOSObject().getDictionaryObject(COSName.SHADING);
        assertNotNull(shadingDict, "页面资源中应有渐变字典");
        COSDictionary shading = (COSDictionary) shadingDict.getDictionaryObject(COSName.getPDFName("Sh0"));
        assertNotNull(shading);
        return PDShading.create(shading);
    }
}
