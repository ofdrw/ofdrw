package org.ofdrw.converter.ofdconverter;

import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.graphics2d.OFDGraphicsDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PDF转换为OFD转换器
 *
 * @author 权观宇
 * @since 2023-3-14 23:09:08
 */
public class PDFConverter implements ConvertOFD {

    /**
     * 是否已经关闭
     */
    private boolean closed = false;


    /**
     * OFD图形文档对象
     */
    final OFDGraphicsDocument ofdDoc;

    /**
     * 创建PDF转换OFD转换器
     *
     * @param ofdPath 转换后的OFD文件路径
     * @throws IOException 文件解析异常
     */
    public PDFConverter(Path ofdPath) throws IOException {
        if (ofdPath == null) {
            throw new IllegalArgumentException("转换后的OFD文件路径为空");
        }

        ofdPath = ofdPath.toAbsolutePath();
        if (!Files.exists(ofdPath)) {
            Path parent = ofdPath.getParent();
            if (Files.exists(parent)) {
                if (!Files.isDirectory(parent)) {
                    throw new IllegalArgumentException("已经存在同名文件: " + parent);
                }
            } else {
                Files.createDirectories(parent);
            }
            Files.createFile(ofdPath);
        }
        ofdDoc = new OFDGraphicsDocument(ofdPath);
    }


    /**
     * PDF转换为OFD页面
     *
     * @param filepath 待转换文件路径
     * @param indexes  【可选】【可变】待转换页码（从0起），该参数仅在转换源文件类型为类文档文件时有效，当该参数不传或为空时表示转换全部内容到OFD。
     * @throws GeneralConvertException 转换异常
     */
    @Override
    public void convert(Path filepath, int... indexes) throws GeneralConvertException {
        // TODO: 转换实现
//
//        PDDocument   pdfDoc = PDDocument.load(pdfInput);
//        PDFRenderer  pdfRender = new PDFRenderer(pdfDoc);
//        ofdDoc = new OFDGraphicsDocument(ofdOutput);
    }


    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
    }
}
