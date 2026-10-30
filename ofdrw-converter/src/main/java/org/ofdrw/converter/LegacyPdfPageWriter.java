package org.ofdrw.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.ofdrw.converter.pdf.font.TrueTypePdfFont;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.converter.pdf.model.PdfPageContent;
import org.ofdrw.converter.pdf.model.TextCommand;
import org.ofdrw.converter.pdf.render.PageRendererImpl;
import org.ofdrw.converter.pdf.write.PdfCommandReplayer;
import org.ofdrw.converter.pdf.write.PdfFontFactory;
import org.ofdrw.converter.pdf.write.PdfPathFactory;
import org.ofdrw.converter.pdf.write.PdfTextWriter;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 旧接口的逐页渲染适配（仅内部使用）
 * <p>
 * 历史上的 {@link ItextMaker}/{@link PdfboxMaker} 允许调用方把单页追加到自建的 {@link PDDocument}；
 * 这些方法的签名必须保留，但渲染逻辑只允许存在一套（决策 4）。本类把「渲染一页 → 写入调用方文档」
 * 这件小事收敛到一处，与 {@link org.ofdrw.converter.export.PDFExporter} 共用同一渲染管线，
 * 不构成第二套实现。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class LegacyPdfPageWriter {

    /**
     * 工具类不允许实例化
     */
    private LegacyPdfPageWriter() {
    }

    /**
     * 把 OFD 指定页渲染并追加到调用方的 PDF 文档
     *
     * @param document PDF 文档，不能为空
     * @param reader   OFD 解析器，不能为空
     * @param pipeline 渲染管线（同一批页面应复用同一个，保证字体登记表一致）
     * @param pageNum  OFD 页码，从 1 起
     * @return 追加的 PDF 页面
     * @throws IOException 写入失败
     */
    static PDPage appendPage(PDDocument document, OFDReader reader, PageRendererImpl.RenderPipeline pipeline,
                             int pageNum) throws IOException {
        if (document == null || reader == null || pipeline == null) {
            throw new IllegalArgumentException("PDF 文档、OFD 解析器与渲染管线不能为空");
        }
        pipeline.registry().newPage();
        org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(pageNum);
        PdfPageContent content = pipeline.pageRenderer().renderPage(page, pipeline.fontSelector());

        // 决策 26：MediaBox = CropBox = PhysicalBox
        Box box = PageRendererImpl.pageBox(page, reader).toPdfBox();
        final float x0 = (float) box.getX();
        final float y0 = (float) box.getY();
        final float width = (float) box.getWidth();
        final float height = (float) box.getHeight();
        PDPage pdPage = new PDPage(new PDRectangle(x0, y0, width, height));
        pdPage.setCropBox(new PDRectangle(x0, y0, width, height));
        document.addPage(pdPage);

        PdfCommandReplayer replayer = new PdfCommandReplayer(new PdfPathFactory());
        PdfTextWriter textWriter = new PdfTextWriter(new PdfFontFactory());
        final TrueTypePdfFont.Registry registry = pipeline.registry();
        try (PDPageContentStream stream = new PDPageContentStream(document, pdPage)) {
            PDResources resources = pdPage.getResources();
            if (resources == null) {
                resources = new PDResources();
                pdPage.setResources(resources);
            }
            final PDResources pageResources = resources;
            replayer.replay(document, stream, pageResources, content.getCommands(), command -> {
                if (!(command instanceof TextCommand)) {
                    return false;
                }
                TextCommand text = (TextCommand) command;
                TrueTypePdfFont font = registry.lookupByResourceName(text.getFontRef());
                if (font == null) {
                    return true;
                }
                textWriter.write(document, pdPage, stream, pageResources, font, text);
                return true;
            });
        }
        return pdPage;
    }

    /**
     * 按页面信息定位 OFD 页码
     * <p>
     * {@link PageInfo} 不携带页码，这里按索引位置与页对象 ID 反查。
     *
     * @param reader   OFD 解析器
     * @param pageInfo 页面信息
     * @return OFD 页码，从 1 起；未找到时返回 -1
     */
    static int pageNumOf(OFDReader reader, PageInfo pageInfo) {
        if (reader == null || pageInfo == null) {
            return -1;
        }
        List<PageInfo> pages = reader.getPageList();
        if (pages == null) {
            return -1;
        }
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i) == pageInfo) {
                return i + 1;
            }
        }
        // 非同一实例时按页对象 ID 反查
        String id = pageInfo.getId() == null ? null : pageInfo.getId().toString();
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < pages.size(); i++) {
            PageInfo candidate = pages.get(i);
            if (candidate != null && candidate.getId() != null && id.equals(candidate.getId().toString())) {
                return i + 1;
            }
        }
        return -1;
    }
}
