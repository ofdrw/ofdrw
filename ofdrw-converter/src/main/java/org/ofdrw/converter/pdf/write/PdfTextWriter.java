package org.ofdrw.converter.pdf.write;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.Matrix;
import org.ofdrw.converter.pdf.font.TrueTypePdfFont;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.TextCommand;
import org.ofdrw.converter.pdf.model.GlyphRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * 中间模型文字指令 → PDF 内容流（P2 写入侧）
 * <p>
 * 写法（与 {@link TextCommand} 契约一致）：
 * <ol>
 *     <li>{@code q} 保存状态，写入填充颜色（CMYK 保持 k、灰度保持 g，不转 RGB）；</li>
 *     <li>{@code BT} 后用 {@code Tm} 写入文本矩阵（含字符方向旋转），此后每个字形先用
 *     {@code Td} 位移到其文本空间原点，再以字形索引（CID）绘制；</li>
 *     <li>字形以 {@link TrueTypePdfFont#encodeGlyph(int)} 编码（单字节或双字节 CID），
 *     与 ToUnicode CMap 的码位字节数一致，保证提取文本正确；</li>
 *     <li>{@code ET}、{@code Q} 成对结束（设计约束 1）。</li>
 * </ol>
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfTextWriter {

    private static final Logger logger = LoggerFactory.getLogger(PdfTextWriter.class);

    /**
     * 十六进制字符表（避免 String.format 的本地化与性能开销）
     */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /**
     * 字体对象工厂
     */
    private final PdfFontFactory fontFactory;

    /**
     * 构造文字写入器
     *
     * @param fontFactory 字体对象工厂，不能为空
     */
    public PdfTextWriter(PdfFontFactory fontFactory) {
        if (fontFactory == null) {
            throw new IllegalArgumentException("字体对象工厂不能为空");
        }
        this.fontFactory = fontFactory;
    }

    /**
     * 写入文字指令
     *
     * @param document PDF 文档
     * @param page     目标页
     * @param stream   页面内容流
     * @param resources 页面资源字典（用于登记字体资源）
     * @param font     字体引用
     * @param command  文字指令
     * @throws IOException 写入失败
     */
    public void write(PDDocument document, PDPage page, PDPageContentStream stream, PDResources resources,
                      TrueTypePdfFont font, TextCommand command) throws IOException {
        if (command == null || font == null) {
            return;
        }
        PDFont pdfFont = fontFactory.obtain(document, font);
        if (pdfFont == null) {
            logger.warn("字体 {} 没有可用的 PDF 字体对象，跳过文字写入", font.getFamilyName());
            return;
        }
        COSName fontName = COSName.getPDFName(font.getResourceName());
        // 字体对象由字体工厂缓存（同一字体只创建一次），因此每次写入都确保页面资源字典已登记
        // （不同页面的 PDResources 各自独立，不能只登记一次）
        if (resources.getFont(fontName) == null) {
            resources.put(fontName, pdfFont);
        }
        // 图形状态显式成对（设计约束 1）：q/Q 包裹文字绘制，颜色与字体状态不残留到后续图元
        stream.saveGraphicsState();
        stream.beginText();
        setFillColor(stream, command.getColor());
        stream.setFont(pdfFont, (float) command.getFontSizePt());
        org.ofdrw.converter.pdf.model.AffineMatrix tm = command.getTextMatrix();
        stream.setTextMatrix(new Matrix((float) tm.getA(), (float) tm.getB(), (float) tm.getC(),
                (float) tm.getD(), (float) tm.getE(), (float) tm.getF()));
        List<GlyphRun> glyphs = command.getGlyphs();
        for (int i = 0; i < glyphs.size(); i++) {
            GlyphRun glyph = glyphs.get(i);
            if (i > 0) {
                /*
                 * 逐字形定位：字形给出的是相对首字形的文本空间位移（单位 pt），换算成绝对文本矩阵。
                 * 不用 Td 累加增量：Td 的单位是文本空间用户单位，会被 Tf 字号放大，字距失真。
                 */
                stream.setTextMatrix(absoluteMatrix(command.getTextMatrix(), glyph));
            }
            if (glyph.getGlyphId() < 0) {
                logger.warn("字形缺少索引，已跳过：{}", glyph);
                continue;
            }
            try {
                // 字形以 CID 字节序列直接写入内容流（PDF 2.0 的 UTF-8 文本串与 Type0 的编码语义无关，
                // 使用非 UTF-8 的 ISO-8859-1 视图可保证字节原样输出，不会被 PDFBox 按 Unicode 重新编码）
                writeGlyphs(stream, font, glyph.getGlyphId());
            } catch (IOException | RuntimeException e) {
                logger.warn("字形 {} 无法写入文字流，已跳过：{}", glyph.getGlyphId(), e.getMessage());
            }
        }
        stream.endText();
        stream.restoreGraphicsState();
    }

    /**
     * 把字形索引写入内容流
     * <p>
     * Type0 字体使用 Identity-H 编码时，内容流中的字符串即 CID 序列，与 Unicode 文本串的 UTF-8 语义无关；
     * PDFBox 的 {@link PDPageContentStream#showText(String)} 会按 Unicode 重新编码，
     * 因此这里以 ISO-8859-1 逐字节还原后直接写 {@code Tj}（字节 0x00~0xFF 与 CID 一一对应）。
     *
     * @param stream 内容流
     * @param font   字体引用
     * @param gid    字形索引
     * @throws IOException 写入失败
     */
    private static void writeGlyphs(PDPageContentStream stream, TrueTypePdfFont font, int gid) throws IOException {
        int cid = font.encodeGlyph(gid);
        byte[] bytes = font.getCodeByteLength() == 1
                ? new byte[]{(byte) (cid & 0xFF)}
                : new byte[]{(byte) ((cid >> 8) & 0xFF), (byte) (cid & 0xFF)};
        // 字符串字面量 + 显式十六进制编码的 Tj 操作符：0x00~0xFF 逐字节原样写入内容流
        // （不能用 showText(String)，PDFBox 会按 Unicode 重新编码）
        StringBuilder literal = new StringBuilder(bytes.length * 3 + 6);
        literal.append('<');
        for (byte b : bytes) {
            literal.append(HEX[(b >> 4) & 0x0F]).append(HEX[b & 0x0F]);
        }
        literal.append("> Tj\n");
        stream.appendRawCommands(literal.toString());
    }

    /**
     * 计算字形对应的文本矩阵（首个字形为 Tm，其余在 Tm 基础上平移）
     *
     * @param textMatrix 文字指令的文本矩阵
     * @param glyph      字形
     * @param index      字形序号
     * @return 新的文本矩阵
     */
    private static Matrix absoluteMatrix(org.ofdrw.converter.pdf.model.AffineMatrix textMatrix, GlyphRun glyph) {
        /*
         * 字形位置以“页面 pt”给出，而 Tm 的平移量也是页面 pt；
         * 因此把字形位置按 Tm 的线性部分变换后再直接作为新的平移量，
         * 文本矩阵的线性部分（方向旋转）保持不变。
         */
        /*
         * 字形位移是文本空间中的相对量（单位 pt）：按 Tm 的线性部分变换后叠加到 Tm 的平移量上，
         * 得到该字形自己的文本矩阵。线性部分保持不变，避免二次翻转或缩放。
         */
        double a = textMatrix.getA();
        double b = textMatrix.getB();
        double c = textMatrix.getC();
        double d = textMatrix.getD();
        double x = a * glyph.getX() + c * glyph.getY() + textMatrix.getE();
        double y = b * glyph.getX() + d * glyph.getY() + textMatrix.getF();
        return new Matrix((float) a, (float) b, (float) c, (float) d, (float) x, (float) y);
    }

    /**
     * 字形编码
     *
     * @param font 字体引用
     * @param gid  字形索引
     * @return 编码后的字节（1 或 2 字节，大端）
     */
    static byte[] encode(TrueTypePdfFont font, int gid) {
        int cid = font.encodeGlyph(gid);
        if (font.getCodeByteLength() == 1) {
            return new byte[]{(byte) (cid & 0xFF)};
        }
        return new byte[]{(byte) ((cid >> 8) & 0xFF), (byte) (cid & 0xFF)};
    }

    /**
     * 设置文字填充颜色
     *
     * @param stream 内容流
     * @param color  颜色
     * @throws IOException 写入失败
     */
    private static void setFillColor(PDPageContentStream stream, PdfColor color) throws IOException {
        if (color == null) {
            return;
        }
        switch (color.getColorSpace()) {
            case GRAY:
                stream.setNonStrokingColor(color.getChannel(0));
                break;
            case CMYK:
                // 设计 3.4 节：CMYK 必须保持 k 操作符，不能转 RGB
                stream.setNonStrokingColor(color.getChannel(0), color.getChannel(1),
                        color.getChannel(2), color.getChannel(3));
                break;
            case RGB:
            default:
                stream.setNonStrokingColor(color.getChannel(0), color.getChannel(1), color.getChannel(2));
                break;
        }
    }
}
