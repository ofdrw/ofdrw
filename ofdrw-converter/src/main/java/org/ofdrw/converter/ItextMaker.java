package org.ofdrw.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.converter.pdf.render.PageRendererImpl;
import org.ofdrw.reader.PageInfo;

import java.io.IOException;

/**
 * PDF 转换器（历史入口，类名保留 iText 时期的命名）
 * <p>
 * <b>不建议导出为 PDF。</b>OFD 是国产版式文档，除交换存档等特殊需求外无需转换为 PDF；
 * 仅需查看文档内容时，请直接使用 ofdrw-viewer 预览，无需转换。
 * <p>
 * 本类保留历史的公开签名，内部不再维护独立的转换实现（决策 4：全库只保留一种实现）。
 * 逐页转换与文档级信息写入统一由 {@link org.ofdrw.converter.export.PDFExporter} 承担，
 * 需要把页面追加到调用方已创建的 {@link PDDocument} 时请直接使用渲染管线
 * {@code PageRendererImpl} + {@code PdfCommandReplayer}。
 *
 * @author dltech21
 * @since 2020.08.11
 * @deprecated 请使用 {@link org.ofdrw.converter.export.PDFExporter}；本类仅为兼容历史调用方而保留。
 */
@Deprecated
public class ItextMaker {

    /**
     * OFD 解析器
     */
    private final OFDReader ofdReader;

    /**
     * 渲染管线（首次使用时创建，同一文档的多个页面复用同一字体登记表）
     */
    private PageRendererImpl.RenderPipeline pipeline;

    /**
     * 毫米转 PDF 点的换算系数
     */
    private static final double MM_TO_PT = 72d / 25.4d;

    /**
     * 文字裁剪矩形向外扩展的余量，单位 mm
     */
    private static final double TEXT_CLIP_MARGIN = 1d;

    /**
     * 构造转换器
     *
     * @param ofdReader OFD 解析器，不能为空
     * @throws IOException 构造异常
     */
    public ItextMaker(OFDReader ofdReader) throws IOException {
        if (ofdReader == null) {
            throw new IllegalArgumentException("OFD 解析器不能为空");
        }
        this.ofdReader = ofdReader;
    }

    /**
     * 单页转换（历史签名）
     * <p>
     * 逐页渲染已统一由新的渲染管线完成，本方法不再提供第二套实现；
     * 请改用 {@link org.ofdrw.converter.export.PDFExporter#export(int...)} 指定页码导出。
     *
     * @param pdf      PDF 文档对象
     * @param pageInfo 页面信息
     * @return PDF 页面
     * @throws IOException 文档操作异常
     * @deprecated 请使用 {@link org.ofdrw.converter.export.PDFExporter#export(int...)}
     */
    @Deprecated
    public PDPage makePage(PDDocument pdf, PageInfo pageInfo) throws IOException {
        if (pdf == null || pageInfo == null) {
            throw new IllegalArgumentException("PDF 文档与页面信息不能为空");
        }
        // 与 PDFExporter 共用同一渲染管线（决策 4：只有一种实现），把该页追加到调用方文档
        if (pipeline == null) {
            pipeline = PageRendererImpl.create(ofdReader);
        }
        int pageNum = LegacyPdfPageWriter.pageNumOf(ofdReader, pageInfo);
        if (pageNum <= 0) {
            throw new IllegalArgumentException("页面信息不属于当前 OFD 文档");
        }
        return LegacyPdfPageWriter.appendPage(pdf, ofdReader, pipeline, pageNum);
    }

    /**
     * 写入附件（历史签名）
     * <p>
     * 附件等文档级信息在 {@link org.ofdrw.converter.export.PDFExporter#close()} 中统一写入。
     *
     * @param pdf       PDF 文档对象
     * @param ofdReader OFD 解析器
     * @throws IOException 写入异常
     * @deprecated 请使用 {@link org.ofdrw.converter.export.PDFExporter#close()}
     */
    @Deprecated
    public void addAttachments(PDDocument pdf, OFDReader ofdReader) throws IOException {
        StructureWriterSupport.writeAttachments(pdf, ofdReader);
    }

    /**
     * 文字裁剪矩形（保留供历史单元测试使用）
     *
     * @param pageBox  页面区域，单位 mm
     * @param boundary 文字外接矩形，单位 mm
     * @return PDF 页面坐标下的裁剪矩形，单位 pt
     */
    static PDRectangle textClipRectangle(ST_Box pageBox, ST_Box boundary) {
        return new PDRectangle(
                (float) converterDpi(boundary.getTopLeftX() - TEXT_CLIP_MARGIN),
                (float) converterDpi(pageBox.getHeight() - boundary.getTopLeftY()
                        - boundary.getHeight() - TEXT_CLIP_MARGIN),
                (float) converterDpi(boundary.getWidth() + TEXT_CLIP_MARGIN * 2),
                (float) converterDpi(boundary.getHeight() + TEXT_CLIP_MARGIN * 2));
    }

    /**
     * 获取 OFD 解析器（供历史调用方读取文档信息）
     *
     * @return OFD 解析器
     */
    OFDReader getOfdReader() {
        return ofdReader;
    }

    /**
     * 毫米转 PDF 点
     *
     * @param mm 长度，单位 mm
     * @return 长度，单位 pt
     */
    private static double converterDpi(double mm) {
        return mm * MM_TO_PT;
    }
}
