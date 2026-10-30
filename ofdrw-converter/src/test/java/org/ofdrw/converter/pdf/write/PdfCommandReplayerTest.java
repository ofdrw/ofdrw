package org.ofdrw.converter.pdf.write;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.ImageCommand;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.SetAlpha;
import org.ofdrw.converter.pdf.font.ImageEncoding;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 指令回放：透明度与图像蒙版
 * <p>
 * 锁定两处容易回退的行为：
 * <ul>
 *     <li>透明度以 {@code ca/CA} 写入，值为「颜色 Alpha × 图元 Alpha」（决策 2）；</li>
 *     <li>蒙版图像与主图配对生成 {@code /SMask}，蒙版本身不单独绘制。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
class PdfCommandReplayerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("透明度：SetAlpha 写入 ExtGState 的 ca/CA（颜色 128 × 图元 128 → 0.252）")
    void alphaWrittenAsCaCa() throws IOException {
        Path pdf = tempDir.resolve("alpha.pdf");
        float expected = org.ofdrw.converter.pdf.render.ColorResolver.compositeOpacity(128, 128);
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            List<ContentCommand> commands = new ArrayList<>();
            // SetAlpha 的值由渲染阶段算好（颜色 Alpha × 图元 Alpha），回放只负责写入 ca/CA
            commands.add(SetAlpha.of(expected));
            PdfCommandReplayer replayer = new PdfCommandReplayer(new PdfPathFactory());
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                replayer.replay(doc, stream, page.getResources(), commands);
            }
            doc.save(pdf.toFile());
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            // PDFBox 把 ca/CA 写入页面资源的 /ExtGState 字典，由 gs 操作符引用
            assertEquals(1, countOperators(doc, "gs"), "应通过 gs 应用透明度");
            COSDictionary extGState = (COSDictionary) doc.getPage(0).getResources()
                    .getCOSObject().getDictionaryObject(COSName.EXT_G_STATE);
            assertNotNull(extGState, "页面资源应包含 /ExtGState");
            COSDictionary first = (COSDictionary) extGState.getDictionaryObject(
                    extGState.keySet().iterator().next());
            assertNotNull(first);
            assertEquals(expected, first.getFloat(COSName.CA), 1e-4f, "非描边透明度 ca");
            assertEquals(expected, first.getFloat(COSName.CA_NS), 1e-4f, "描边透明度 CA");
        }
    }

    @Test
    @DisplayName("颜色 Alpha × 图元 Alpha：compositeOpacity(128,128)=0.25")
    void compositeOpacity() {
        // (128/255)² ≈ 0.25196
        assertEquals((128f / 255f) * (128f / 255f),
                org.ofdrw.converter.pdf.render.ColorResolver.compositeOpacity(128, 128), 1e-6f);
        assertEquals(1f, org.ofdrw.converter.pdf.render.ColorResolver.compositeOpacity(null, null), 1e-4f);
        assertEquals(0.5f, org.ofdrw.converter.pdf.render.ColorResolver.colorOpacity(
                new PdfColor(PdfColor.ColorSpace.RGB, 0.5f, 1f, 0f, 0f)), 1e-4f);
    }

    @Test
    @DisplayName("图像蒙版：主图带 /SMask，蒙版本身不单独绘制")
    void maskBecomesSmask() throws IOException {
        Path pdf = tempDir.resolve("mask.pdf");
        byte[] main = jpegBytes(8, 8);
        byte[] mask = maskPngBytes(8, 8);
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            Box placement = new Box(10, 10, 40, 40);
            List<ContentCommand> commands = new ArrayList<>();
            // 渲染器输出顺序：蒙版 → 主图
            commands.add(new ImageCommand("ImgMask", ImageEncoding.PNG, 8, 8, mask, placement, true));
            // 主图本身不是蒙版指令（masked=false），蒙版由单独的 ImageMask 指令携带
            commands.add(new ImageCommand("ImgMain", ImageEncoding.JPEG, 8, 8, main, placement, false));
            PdfCommandReplayer replayer = new PdfCommandReplayer(new PdfPathFactory());
            // 页面资源字典由调用方提供（PDFExporter 会为页面创建并挂载，这里显式创建以模拟同一约定）
            org.apache.pdfbox.pdmodel.PDResources orig = new org.apache.pdfbox.pdmodel.PDResources();
            page.setResources(orig);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                replayer.replay(doc, stream, orig, commands);
            }
            assertEquals(1, count(orig.getXObjectNames()),
                    "蒙版不应单独绘制，页面上只出现一个图像 XObject");
            doc.save(pdf.toFile());
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertEquals(1, count(doc.getPage(0).getResources().getXObjectNames()),
                    "保存后仍只有一个图像 XObject（蒙版未单独绘制）");
            PDImageXObject image = (PDImageXObject) doc.getPage(0).getResources()
                    .getXObject(COSName.getPDFName(firstXObjectName(doc)));
            assertNotNull(image);
            COSBase smask = image.getCOSObject().getDictionaryObject(COSName.SMASK);
            assertNotNull(smask, "主图必须带 /SMask");
            COSDictionary smaskDict = (COSDictionary) smask;
            assertEquals(8, smaskDict.getInt(COSName.WIDTH), "蒙版宽度与主图一致");
            assertEquals(8, smaskDict.getInt(COSName.HEIGHT), "蒙版高度与主图一致");
        }
    }

    @Test
    @DisplayName("无蒙版图像：不生成 /SMask")
    void noMaskNoSmask() throws IOException {
        Path pdf = tempDir.resolve("nomask.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            List<ContentCommand> commands = new ArrayList<>();
            commands.add(new ImageCommand("ImgMain", ImageEncoding.JPEG, 8, 8, jpegBytes(8, 8),
                    new Box(10, 10, 40, 40), false));
            PdfCommandReplayer replayer = new PdfCommandReplayer(new PdfPathFactory());
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                replayer.replay(doc, stream, page.getResources(), commands);
            }
            doc.save(pdf.toFile());
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDImageXObject image = (PDImageXObject) doc.getPage(0).getResources()
                    .getXObject(COSName.getPDFName(firstXObjectName(doc)));
            assertNull(image.getCOSObject().getDictionaryObject(COSName.SMASK), "无蒙版时不应有 /SMask");
        }
    }

    /**
     * 统计可迭代对象中的元素个数（PDFBox 3 的 getXObjectNames 返回 Iterable 而非集合）
     *
     * @param items 可迭代对象
     * @return 元素个数
     */
    private static int count(Iterable<?> items) {
        int n = 0;
        for (Object ignored : items) {
            n++;
        }
        return n;
    }

    /**
     * 取页面第一个 XObject 资源名
     *
     * @param doc PDF 文档
     * @return 资源名
     */
    private static String firstXObjectName(PDDocument doc) {
        for (COSName name : doc.getPage(0).getResources().getXObjectNames()) {
            return name.getName();
        }
        throw new AssertionError("页面没有图像 XObject");
    }

    /**
     * 统计内容流中的操作符出现次数
     *
     * @param doc  PDF 文档
     * @param name 操作符名
     * @return 次数
     */
    private static int countOperators(PDDocument doc, String name) throws IOException {
        int count = 0;
        for (Object token : new PDFStreamParser(doc.getPage(0)).parse()) {
            if (token instanceof Operator && name.equals(((Operator) token).getName())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 取操作符的最后一个数值操作数
     *
     * @param tokens 指令序列
     * @param name   操作符名
     * @return 操作数；不存在时返回 {@code null}
     */
    private static Float operand(List<Object> tokens, String name) {
        for (int i = 0; i < tokens.size(); i++) {
            Object token = tokens.get(i);
            if (token instanceof Operator && name.equals(((Operator) token).getName())) {
                if (i > 0 && tokens.get(i - 1) instanceof COSNumber) {
                    return ((COSNumber) tokens.get(i - 1)).floatValue();
                }
                return null;
            }
        }
        return null;
    }

    /**
     * 生成 JPEG 字节
     *
     * @param width  宽，单位像素
     * @param height 高，单位像素
     * @return JPEG 数据
     */
    private static byte[] jpegBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, 0x3366CC);
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "jpg", bos);
        return bos.toByteArray();
    }

    /**
     * 生成二值蒙版 PNG（左半透明 0、右半不透明 255）
     *
     * @param width  宽，单位像素
     * @param height 高，单位像素
     * @return PNG 数据
     */
    private static byte[] maskPngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.getRaster().setSample(x, y, 0, x < width / 2 ? 0 : 255);
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", bos);
        return bos.toByteArray();
    }
}
