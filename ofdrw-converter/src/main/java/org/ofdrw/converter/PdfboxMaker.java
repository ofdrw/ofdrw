package org.ofdrw.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;
import org.ofdrw.converter.point.TextCodePoint;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.converter.pdf.render.PageRendererImpl;
import org.ofdrw.reader.PageInfo;

import java.io.IOException;

import static org.ofdrw.core.text.text.Direction.Angle_180;
import static org.ofdrw.core.text.text.Direction.Angle_270;
import static org.ofdrw.core.text.text.Direction.Angle_90;

/**
 * PDFBox 实现的 PDF 转换器（历史入口）
 * <p>
 * <b>不建议导出为 PDF。</b>OFD 是国产版式文档，除交换存档等特殊需求外无需转换为 PDF；
 * 仅需查看文档内容时，请直接使用 ofdrw-viewer 预览，无需转换。
 * <p>
 * 本类保留历史的公开签名，内部不再维护独立的转换实现（决策 4：全库只保留一种实现）。
 * 逐页转换与文档级信息写入统一由 {@link org.ofdrw.converter.export.PDFExporter} 承担，
 * 需要把页面追加到调用方已创建的 {@link PDDocument} 时请直接使用渲染管线
 * {@code PageRendererImpl} + {@code PdfCommandReplayer}。
 *
 * @author 权观宇
 * @since 2023-3-7 21:30:40
 * @deprecated 请使用 {@link org.ofdrw.converter.export.PDFExporter}；本类仅为兼容历史调用方而保留。
 */
@Deprecated
public class PdfboxMaker {

    /**
     * OFD 解析器
     */
    private final OFDReader reader;

    /**
     * 目标 PDF 文档
     */
    private final PDDocument pdf;

    /**
     * 默认字体，当无法获取字体时使用
     */
    private PageRendererImpl.RenderPipeline pipeline;

    private PDFont defaultFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    /**
     * 构造转换器
     *
     * @param reader OFD 解析器，不能为空
     * @param pdf    目标 PDF 文档，可为空
     * @throws IOException 构造异常
     */
    public PdfboxMaker(OFDReader reader, PDDocument pdf) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException("OFD 解析器不能为空");
        }
        this.reader = reader;
        this.pdf = pdf;
    }

    /**
     * 单页转换（历史签名）
     * <p>
     * 逐页渲染已统一由新的渲染管线完成，本方法不再提供第二套实现；
     * 请改用 {@link org.ofdrw.converter.export.PDFExporter#export(int...)} 指定页码导出。
     *
     * @param pageInfo 页面信息
     * @return PDF 页面
     * @throws IOException 文档操作异常
     * @deprecated 请使用 {@link org.ofdrw.converter.export.PDFExporter#export(int...)}
     */
    @Deprecated
    public PDPage makePage(PageInfo pageInfo) throws IOException {
        if (pdf == null || pageInfo == null) {
            throw new IllegalArgumentException("PDF 文档与页面信息不能为空");
        }
        // 与 PDFExporter 共用同一渲染管线（决策 4：只有一种实现），把该页追加到调用方文档
        if (pipeline == null) {
            pipeline = PageRendererImpl.create(reader);
        }
        int pageNum = LegacyPdfPageWriter.pageNumOf(reader, pageInfo);
        if (pageNum <= 0) {
            throw new IllegalArgumentException("页面信息不属于当前 OFD 文档");
        }
        return LegacyPdfPageWriter.appendPage(pdf, reader, pipeline, pageNum);
    }

    /**
     * 写入附件（历史签名）
     * <p>
     * 附件等文档级信息在 {@link org.ofdrw.converter.export.PDFExporter#close()} 中统一写入。
     *
     * @param ofdReader OFD 解析器
     * @throws IOException 写入异常
     * @deprecated 请使用 {@link org.ofdrw.converter.export.PDFExporter#close()}
     */
    @Deprecated
    public void addAttachments(OFDReader ofdReader) throws IOException {
        StructureWriterSupport.writeAttachments(pdf, ofdReader);
    }

    /**
     * 设置默认字体
     * <p>
     * 字体选择已由渲染管线内部的字体选择器负责（内嵌优先、替换兜底），本设置仅保留签名。
     *
     * @param font 默认字体，可为空
     */
    public void setDefaultFont(PDFont font) {
        if (font != null) {
            this.defaultFont = font;
        }
    }

    /**
     * 获取默认字体
     *
     * @return 默认字体
     */
    public PDFont getDefaultFont() {
        return defaultFont;
    }

    /**
     * 获取 OFD 解析器
     *
     * @return OFD 解析器
     */
    OFDReader getReader() {
        return reader;
    }

    /**
     * 获取目标 PDF 文档
     *
     * @return PDF 文档
     */
    PDDocument getPdf() {
        return pdf;
    }

    static Matrix textMatrix(TextObject textObject, TextCodePoint textCodePoint) {
        double a = 1;
        double b = 0;
        double c = 0;
        double d = 1;
        if (textObject.getCTM() != null) {
            Double[] ctm = textObject.getCTM().toDouble();
            // Character positions already contain the complete CTM. Tm only needs its linear
            // part, converted from OFD's downward Y axis to PDF text space's upward Y axis.
            a = ctm[0];
            b = -ctm[1];
            c = -ctm[2];
            d = ctm[3];
        }

        double charA = 1;
        double charB = 0;
        double charC = 0;
        double charD = 1;
        if (textObject.getCharDirection() == Angle_90) {
            charA = 0;
            charB = -1;
            charC = 1;
            charD = 0;
        } else if (textObject.getCharDirection() == Angle_180) {
            charA = -1;
            charD = -1;
        } else if (textObject.getCharDirection() == Angle_270) {
            charA = 0;
            charB = 1;
            charC = -1;
            charD = 0;
        }

        double hScale = textObject.getHScale();
        double matrixA = (a * charA + c * charB) * hScale;
        double matrixB = (b * charA + d * charB) * hScale;
        double matrixC = a * charC + c * charD;
        double matrixD = b * charC + d * charD;
        return new Matrix((float) matrixA, (float) matrixB, (float) matrixC, (float) matrixD,
                (float) textCodePoint.getX(), (float) textCodePoint.getY());
    }}
