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
        /*
         * 颜色自带的不透明度必须在文字绘制前落到图形状态，否则水印等半透明文字会被画成实心。
         * 参考实现只把 FillColor 的 Alpha 写入 **ca**，没有显式描边 Alpha 时 **CA=0**：
         * 半透明文字用 `2 Tr` 绘制时描边不可见，若把 ca 同时写给 CA 会让水印描边画实而变粗
         * （keyword 水印基准 `<< /CA 0 /ca .3137 >>`）。
         */
        if (command.getColor() != null && command.getColor().getOpacity() < 1f) {
            float opacity = command.getColor().getOpacity();
            PdfCommandReplayer.setAlphaState(stream, opacity, 0f);
        }
        /*
         * 粗体：参考实现不切换粗体字体，而是“填充+描边”（2 Tr）加粗字形。
         * 线宽与描边色同样属于图形状态，必须在 BT 之前落到内容流。
         */
        boolean bold = command.isBold();
        if (bold) {
            setStrokeColor(stream, command.getColor());
            stream.setLineWidth((float) command.getStrokeWidthPt());
        }
        stream.beginText();
        setFillColor(stream, command.getColor());
        if (bold) {
            stream.appendRawCommands("2 Tr\n");
        }
        stream.setFont(pdfFont, (float) command.getFontSizePt());
        /*
         * 同一行（文本空间 Y 相同）的字形合并成一个 TJ 数组：
         * 只用 TJ 数值修正与字体自然字宽的差值，而不是逐字形重设 Tm。
         * 逐字形 Tm 会让文本层被拆成大量独立“单词”，pdftotext 等工具按位置重排后顺序错乱、
         * 复制/检索结果与原文不一致；TJ 数组则保留一个连续文本串，阅读顺序与内容一致。
         */
        List<GlyphRun> glyphs = command.getGlyphs();
        int index = 0;
        while (index < glyphs.size()) {
            GlyphRun first = glyphs.get(index);
            if (!first.hasGlyphId()) {
                logger.warn("字形缺少索引，已跳过：{}", first);
                index++;
                continue;
            }
            stream.setTextMatrix(absoluteMatrix(command.getTextMatrix(), first));
            StringBuilder tj = new StringBuilder(16 + glyphs.size() * 5);
            tj.append('[');
            appendGlyph(tj, font, first.getGlyphId());
            double prevX = first.getX();
            double prevY = first.getY();
            int prevGid = first.getGlyphId();
            index++;
            while (index < glyphs.size()) {
                GlyphRun glyph = glyphs.get(index);
                if (!glyph.hasGlyphId()) {
                    logger.warn("字形缺少索引，已跳过：{}", glyph);
                    index++;
                    break;
                }
                // 换行/换列（文本空间 Y 变化）时结束当前数组，用新的 Tm 重新开始
                if (Math.abs(glyph.getY() - prevY) > 1e-3) {
                    break;
                }
                double advance = font.glyphAdvancePt(prevGid, command.getFontSizePt());
                double fontSizePt = command.getFontSizePt();
                /*
                 * TJ 修正量 = 目标落点与“上一字落点 + 字体自然字宽”之差，按 1/1000 em 向下取整。
                 * 逐字修正（不回灌残差）在 63 组基准上最稳定：参考实现的 TJ 分块与残差桶由内部
                 * chunker 决定，无法从 OFD 字段推导（见 spec/tj-spec.md 1.4），强行复刻
                 * “每字 -1”会让替换字体行持续加宽（doc1File 由 pass 退化为 fail）。
                 */
                double adjustment = -((glyph.getX() - (prevX + advance)) * 1000d / fontSizePt);
                double nearest = Math.rint(adjustment);
                long quantized = Math.abs(adjustment - nearest) < 1e-6
                        ? (long) nearest
                        : (long) Math.floor(adjustment);
                if (quantized != 0) {
                    tj.append(quantized).append(' ');
                }
                appendGlyph(tj, font, glyph.getGlyphId());
                prevX = glyph.getX();
                prevY = glyph.getY();
                prevGid = glyph.getGlyphId();
                index++;
            }
            tj.append("] TJ");
            try {
                stream.appendRawCommands(tj.append('\n').toString());
            } catch (IOException | RuntimeException e) {
                logger.warn("文字阵列写入失败，已跳过部分字形：{}", e.getMessage());
            }
        }
        stream.endText();
        stream.restoreGraphicsState();
    }

    /**
     * 把字形追加到 TJ 数组（十六进制 CID 字符串）
     * <p>
     * Type0 字体使用 Identity-H 编码时，内容流中的字符串即 CID 序列；直接写十六进制串可保证字节原样输出，
     * 不会被 PDFBox 按 Unicode 重新编码。
     *
     * @param target TJ 数组文本
     * @param font   字体引用
     * @param gid    字形索引
     */
    private static void appendGlyph(StringBuilder target, TrueTypePdfFont font, int gid) {
        int cid = font.encodeGlyph(gid);
        int byteLength = font.getCodeByteLength();
        target.append('<');
        for (int shift = (byteLength - 1) * 8; shift >= 0; shift -= 8) {
            int b = (cid >> shift) & 0xFF;
            target.append(HEX[(b >> 4) & 0x0F]).append(HEX[b & 0x0F]);
        }
        target.append('>');
    }

    /**
     * 格式化 TJ 数值，避免本地化小数点并抑制科学计数法
     *
     * @param value 数值
     * @return 最多三位小数的十进制文本
     */
    private static String formatNumber(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f ", value);
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
     * 设置文字描边颜色（粗体描边与填充同色）
     *
     * @param stream 内容流
     * @param color  颜色
     * @throws IOException 写入失败
     */
    private static void setStrokeColor(PDPageContentStream stream, PdfColor color) throws IOException {
        if (color == null) {
            return;
        }
        switch (color.getColorSpace()) {
            case GRAY:
                stream.setStrokingColor(color.getChannel(0));
                break;
            case CMYK:
                stream.setStrokingColor(color.getChannel(0), color.getChannel(1),
                        color.getChannel(2), color.getChannel(3));
                break;
            case RGB:
            default:
                stream.setStrokingColor(color.getChannel(0), color.getChannel(1), color.getChannel(2));
                break;
        }
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
