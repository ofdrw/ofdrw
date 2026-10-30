package org.ofdrw.converter.export;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.converter.ItextMaker;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * OFD转换PDF
 * <p>
 * 历史上该实现基于 iText7，现已改为基于 PDFBox 生成 PDF，对外接口保持不变。
 *
 * @author 权观宇
 * @since 2023-3-7 21:30:40
 * @deprecated 类名保留历史命名（iText），实现已迁移为 PDFBox；新代码请使用
 * {@link PDFExporterPDFBox}。
 */
@Deprecated
public class PDFExporterIText implements OFDExporter {

    /**
     * PDF文档对象
     */
    final PDDocument pdfDoc;

    /**
     * PDF输出文件路径，为空表示输出到流
     */
    private final Path pdfFilePath;

    /**
     * PDF输出流，为空表示输出到文件
     */
    private final OutputStream pdfOutStream;

    /**
     * OFD解析器
     */
    final OFDReader ofdReader;

    /**
     * 转换器
     */
    final ItextMaker pdfMaker;

    /**
     * 是否已经关闭
     */
    private boolean closed = false;


    /**
     * 通过文件路径 创建PDF转换器
     *
     * @param ofdFilePath 待转换的OFD文件路径
     * @param pdfFilePath 生成PDF文件路径
     * @throws IOException 文件创建失败
     * @deprecated 请使用 {@link PDFExporterPDFBox#PDFExporterPDFBox(Path, Path)}
     */
    @Deprecated
    public PDFExporterIText(Path ofdFilePath, Path pdfFilePath) throws IOException {
        ofdReader = new OFDReader(ofdFilePath);
        pdfMaker = new ItextMaker(this.ofdReader);

        if (pdfFilePath == null) {
            throw new IllegalArgumentException("导出PDF路径为空");
        }
        pdfFilePath = pdfFilePath.toAbsolutePath();
        if (!Files.exists(pdfFilePath)) {
            Path parent = pdfFilePath.getParent();
            if (Files.exists(parent)) {
                if (!Files.isDirectory(parent)) {
                    throw new IllegalArgumentException("已经存在同名文件: " + parent);
                }
            } else {
                Files.createDirectories(parent);
            }
            Files.createFile(pdfFilePath);
        }
        this.pdfFilePath = pdfFilePath;
        this.pdfOutStream = null;
        pdfDoc = new PDDocument();
    }

    /**
     * 通过流 创建PDF转换器
     * <p>
     * 注意：流由调用者负责关闭！
     *
     * @param ofdInStream  待转换的OFD文件流，流由调用者负责关闭。
     * @param pdfOutStream 生成PDF文件流，流由调用者负责关闭。
     * @throws IOException 流操作失败
     * @deprecated 请使用 {@link PDFExporterPDFBox#PDFExporterPDFBox(InputStream, OutputStream)}
     */
    @Deprecated
    public PDFExporterIText(InputStream ofdInStream, OutputStream pdfOutStream) throws IOException {
        ofdReader = new OFDReader(ofdInStream);
        pdfMaker = new ItextMaker(this.ofdReader);

        if (pdfOutStream == null) {
            throw new IllegalArgumentException("导出PDF流为空");
        }
        this.pdfFilePath = null;
        this.pdfOutStream = pdfOutStream;
        pdfDoc = new PDDocument();
    }

    /**
     * 导出指定OFD页
     *
     * @param indexes 页码序列，如果为空表示全部页码（注意：页码从0起）
     * @throws GeneralConvertException 导出异常
     * @deprecated 请使用 {@link PDFExporterPDFBox#export(int...)}
     */
    @Override
    @Deprecated
    public void export(int... indexes) throws GeneralConvertException {
        try {
            List<PageInfo> targetPages = new LinkedList<>();
            if (indexes == null || indexes.length == 0) {
                targetPages.addAll(ofdReader.getPageList());
            } else {
                int maxPageIndex = ofdReader.getNumberOfPages();
                // 获取指定页面信息
                for (int index : indexes) {
                    if (index < 0 || index >= maxPageIndex) {
                        continue;
                    }
                    targetPages.add(ofdReader.getPageInfo(index));
                }
            }

            // 循环添加Page
            for (PageInfo pageInfo : targetPages) {
                pdfMaker.makePage(pdfDoc, pageInfo);
            }

        } catch (IOException e) {
            throw new GeneralConvertException("OFD转换PDF失败 ", e);
        }
    }

    /**
     * 关闭所有打开的文件
     * <p>
     * 并把附件添加到PDF文件中
     *
     * @throws IOException 文件关闭异常
     * @deprecated 请使用 {@link PDFExporterPDFBox#close()}
     */
    @Override
    @Deprecated
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (pdfMaker != null && pdfDoc != null && ofdReader != null) {
                // 添加附件
                pdfMaker.addAttachments(pdfDoc, ofdReader);
            }

            if (pdfDoc != null) {
                if (pdfFilePath != null) {
                    pdfDoc.save(pdfFilePath.toFile());
                } else if (pdfOutStream != null) {
                    pdfDoc.save(pdfOutStream);
                    // PDF 内容完整写出后刷新流，流由调用者负责关闭
                    pdfOutStream.flush();
                }
                pdfDoc.close();
            }
        } finally {
            if (ofdReader != null) {
                ofdReader.close();
            }
        }
    }
}
