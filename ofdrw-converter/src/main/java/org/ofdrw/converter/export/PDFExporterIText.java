package org.ofdrw.converter.export;

import org.ofdrw.converter.GeneralConvertException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * OFD转换PDF
 * <p>
 * 历史上该实现基于 iText7，现已改为基于 PDFBox 生成 PDF，对外接口保持不变。
 * <p>
 * <b>不建议导出为 PDF。</b>除交换存档等特殊需求外无需将 OFD 转换为 PDF；
 * 仅需查看文档内容时，请直接使用 ofdrw-viewer 预览，无需转换。
 *
 * @author 权观宇
 * @since 2023-3-7 21:30:40
 * @deprecated 请使用 {@link PDFExporter}；本类仅为兼容历史调用方而保留。
 */
@Deprecated
public class PDFExporterIText implements OFDExporter {

    /**
     * 实际执行转换的导出器（决策 4：单一实现，旧类内部委托新 API）
     */
    private final PDFExporter delegate;

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
     * @deprecated 请使用 {@link PDFExporter#PDFExporter(Path, Path)}
     */
    @Deprecated
    public PDFExporterIText(Path ofdFilePath, Path pdfFilePath) throws IOException {
        this.delegate = new PDFExporter(ofdFilePath, pdfFilePath);
    }

    /**
     * 通过流 创建PDF转换器
     * <p>
     * 注意：流由调用者负责关闭！
     *
     * @param ofdInStream  待转换的OFD文件流，流由调用者负责关闭。
     * @param pdfOutStream 生成PDF文件流，流由调用者负责关闭。
     * @throws IOException 流操作失败
     * @deprecated 请使用 {@link PDFExporter#PDFExporter(InputStream, OutputStream)}
     */
    @Deprecated
    public PDFExporterIText(InputStream ofdInStream, OutputStream pdfOutStream) throws IOException {
        this.delegate = new PDFExporter(ofdInStream, pdfOutStream);
    }

    /**
     * 导出指定OFD页
     *
     * @param indexes 页码序列，如果为空表示全部页码（注意：页码从0起）
     * @throws GeneralConvertException 导出异常
     * @deprecated 请使用 {@link PDFExporter#export(int...)}
     */
    @Override
    @Deprecated
    public void export(int... indexes) throws GeneralConvertException {
        delegate.export(indexes);
    }

    /**
     * 关闭所有打开的文件
     * <p>
     * 并把附件添加到PDF文件中
     *
     * @throws IOException 文件关闭异常
     * @deprecated 请使用 {@link PDFExporter#close()}
     */
    @Override
    @Deprecated
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        delegate.close();
    }
}
