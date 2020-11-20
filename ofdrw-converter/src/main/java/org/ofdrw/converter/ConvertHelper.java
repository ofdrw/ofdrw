package org.ofdrw.converter;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.jbig2.util.log.Logger;
import org.apache.pdfbox.jbig2.util.log.LoggerFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.ofdrw.converter.utils.CommonUtil;
import org.ofdrw.reader.DLOFDReader;
import org.ofdrw.reader.model.OfdPageVo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * PDF 转换 工具
 *
 * @author minghu.zhang
 * @since 12:53 2020/11/14
 **/
public class ConvertHelper {

    private final static Logger logger = LoggerFactory.getLogger(ConvertHelper.class);

    /**
     * OFD转换PDF
     *
     * @param input  OFD文件路径
     * @param output PDF输出流，支持OutputStream、Path、File、String（文件路径）
     */
    public static void toPdf(Path input, Object output) {

        try (DLOFDReader reader = new DLOFDReader(input);
             PDDocument pdfDocument = new PDDocument()) {


            if (input instanceof InputStream) {
                reader = new DLOFDReader((InputStream) input);
            } else if (input instanceof Path) {
                reader = new DLOFDReader((Path) input);
            } else {
                throw new IllegalArgumentException("OFD输入参数异常，只支持InputStream和Path类型");
            }
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
            } else if (output instanceof Path) {
                pdfDocument.save(Files.newOutputStream((Path) output));
            } else {
                throw new IllegalArgumentException("不支持的输出格式(output)，仅支持OutputStream、Path、File、String");
            }
        } catch (IllegalArgumentException e){
            throw e;
        }catch (Exception e) {
            logger.error("convert to pdf failed", e);
            throw new GeneralConvertException(e);
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
            ofd2pdf(input, output);
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
            ofd2pdf(input, output);
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
            ofd2pdf(input, output);
        } catch (Exception e) {
            logger.error("convert to pdf failed", e);
        }
    }

    /**
     * 转PDF
     *
     * @param input  OFD输入文件
     * @param output PDF输出流
     */
    public static void toPdf(Path input, OutputStream output) {
        try {
            ofd2pdf(input, output);
        } catch (Exception e) {
            logger.error("convert to pdf failed", e);
        }
    }

    /**
     * 转PDF
     *
     * @param input  OFD输入文件
     * @param output PDF输出文件
     */
    public static void toPdf(Path input, File output) {
        try {
            ofd2pdf(input, output);
        } catch (Exception e) {
            logger.error("convert to pdf failed", e);
        }
    }

    /**
     * 转PDF
     *
     * @param input  OFD输入文件
     * @param output PDF输出文件路径
     */
    public static void toPdf(Path input, String output) {
        try {
            ofd2pdf(input, output);
        } catch (Exception e) {
            logger.error("convert to pdf failed", e);
        }
    }

}
