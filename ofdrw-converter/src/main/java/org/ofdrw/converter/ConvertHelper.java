package org.ofdrw.converter;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.jbig2.util.log.Logger;
import org.apache.pdfbox.jbig2.util.log.LoggerFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.ofdrw.converter.utils.CommonUtil;
import org.ofdrw.reader.DLOFDReader;
import org.ofdrw.reader.model.OfdPageVo;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * PDF 转换 工具
 *
 * @author minghu.zhang
 * @date 12:53 2020/11/14
 **/
public class ConvertHelper {

    private final static Logger logger = LoggerFactory.getLogger(ConvertHelper.class);

    /**
     * 转PDF
     *
     * @param input  OFD输入路径
     * @param output PDF输出流
     */
    public static void toPdf(Path input, Object output) {
        DLOFDReader reader = null;
        PDDocument pdfDocument = null;
        try {
            reader = new DLOFDReader(input);
            pdfDocument = new PDDocument();

            List<OfdPageVo> ofdPageVoList = reader.getOFDDocumentVo().getOfdPageVoList();

            long start;
            long end;
            int pageNum = 1;

            PdfboxMaker pdfMaker = new PdfboxMaker(reader, pdfDocument);
            for (OfdPageVo ofdPageVo : ofdPageVoList) {
                start = System.currentTimeMillis();
                pdfMaker.makePage(ofdPageVo);
                end = System.currentTimeMillis();
                logger.info(String.format("page %d speed time %d", pageNum++, end - start));
            }

            if (output instanceof OutputStream) {
                pdfDocument.save((OutputStream) output);
            } else if (output instanceof File) {
                pdfDocument.save((File) output);
            } else if (output instanceof String) {
                pdfDocument.save((String) output);
            } else {
                throw new IllegalArgumentException("pdf save failed, output don't support");
            }
        } catch (Exception e) {
            logger.error("convert to pdf failed", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (pdfDocument != null) {
                    pdfDocument.close();
                }
            } catch (IOException e) {
                logger.error("close OFDReader failed", e);
            }

        }
    }

    /**
     * 转PDF
     *
     * @param input  OFD输入流
     * @param output PDF输出流
     */
    public static void toPdf(InputStream input, OutputStream output) {
        try {
            String tempFilePath = CommonUtil.generateTempFilePath();
            IOUtils.copyLarge(input, new FileOutputStream(tempFilePath));
            Path path = Paths.get(tempFilePath);
            toPdf(path, output);
        } catch (Exception e) {
            logger.error("convert to pdf failed", e);
        }
    }

    /**
     * 转PDF
     *
     * @param input  OFD输入流
     * @param output PDF输出文件
     */
    public static void toPdf(InputStream input, File output) {
        try {
            String tempFilePath = CommonUtil.generateTempFilePath();
            IOUtils.copyLarge(input, new FileOutputStream(tempFilePath));
            Path path = Paths.get(tempFilePath);
            toPdf(path, output);
        } catch (Exception e) {
            logger.error("convert to pdf failed", e);
        }
    }

    /**
     * 转PDF
     *
     * @param input  OFD输入流
     * @param output PDF输出文件路径
     */
    public static void toPdf(InputStream input, String output) {
        try {
            String tempFilePath = CommonUtil.generateTempFilePath();
            IOUtils.copyLarge(input, new FileOutputStream(tempFilePath));
            Path path = Paths.get(tempFilePath);
            toPdf(path, output);
        } catch (Exception e) {
            logger.error("convert to pdf failed", e);
        }
    }

}
