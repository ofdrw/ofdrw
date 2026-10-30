package org.ofdrw.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.ofdrw.converter.export.PDFExporter;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * 转换使用库名称
     *
     * @deprecated 两种取值均由 PDFBox 生成 PDF，请直接调用 {@link #toPdf(Path, Path)} 系列方法
     */
    @Deprecated
    public static Lib lib = Lib.iText;

    /**
     * 转换实现类型
     *
     * @deprecated 两种取值均由 PDFBox 生成 PDF，请直接调用 {@link #toPdf(Path, Path)} 系列方法
     */
    @Deprecated
    public static enum Lib {
        iText, PDFBox
    }

    /**
     * 使用iText兼容实现作为转换实现
     * <p>
     * 该实现已改为基于 PDFBox 生成 PDF，接口名称保持不变。
     *
     * @deprecated 请使用 {@link #toPdf(Path, Path)} 系列方法，转换结果与使用哪种实现无关
     */
    @Deprecated
    public static void useIText() {
        lib = Lib.iText;
    }

    /**
     * 使用PDFBox作为转换实现
     */
    public static void usePDFBox() {
        lib = Lib.PDFBox;
    }

    /**
     * OFD转换PDF
     * <p>
     *
     * @param input  OFD文件路径，支持OutputStream、Path、String（文件路径）
     * @param output PDF输出流，支持OutputStream、Path、File、String（文件路径）
     * @throws IllegalArgumentException 参数错误
     * @throws GeneralConvertException  文档转换过程中异常
     * @deprecated 不建议使用该方法，建议使用 {@link  #toPdf(Path, Path)} 系列明确参数方法。
     */
    @Deprecated
    public static void ofd2pdf(Object input, Object output) {
        OFDReader reader = null;
        try {
            if (input instanceof InputStream) {
                reader = new OFDReader((InputStream) input);
            } else if (input instanceof Path) {
                reader = new OFDReader((Path) input);
            } else if (input instanceof File) {
                reader = new OFDReader(((File) input).toPath());
            } else if (input instanceof String) {
                reader = new OFDReader((String) input);
            } else {
                throw new IllegalArgumentException("不支持的输入格式(input)，仅支持InputStream、Path、File、String");
            }

            // 决策 4：全库只保留一种实现，iText/PDFBox 两种历史开关统一走新渲染管线
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (PDFExporter exporter = new PDFExporter(reader, bos)) {
                exporter.export();
            }

            if (output instanceof OutputStream) {
                bos.writeTo((OutputStream) output);
            } else if (output instanceof File) {
                try (FileOutputStream fileOutputStream = new FileOutputStream((File) output);) {
                    bos.writeTo(fileOutputStream);
                }
            } else if (output instanceof String) {
                try (FileOutputStream fileOutputStream = new FileOutputStream((String) output)) {
                    bos.writeTo(fileOutputStream);
                }
            } else if (output instanceof Path) {
                try (OutputStream out = Files.newOutputStream((Path) output)) {
                    bos.writeTo(out);
                }
            } else {
                throw new IllegalArgumentException("不支持的输出格式(output)，仅支持OutputStream、Path、File、String");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("convert to pdf failed", e);
            throw new GeneralConvertException(e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
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
     * @throws IllegalArgumentException 参数错误
     * @throws GeneralConvertException  文档转换过程中异常
     */
    public static void toPdf(InputStream input, OutputStream output) {
        ofd2pdf(input, output);
    }

    /**
     * 转PDF
     *
     * @param input  OFD输入流
     * @param output PDF输出文件
     * @throws IllegalArgumentException 参数错误
     * @throws GeneralConvertException  文档转换过程中异常
     */
    public static void toPdf(InputStream input, File output) {
        ofd2pdf(input, output);
    }

    /**
     * 转PDF
     *
     * @param input  OFD输入流
     * @param output PDF输出文件路径
     * @throws IllegalArgumentException 参数错误
     * @throws GeneralConvertException  文档转换过程中异常
     */
    public static void toPdf(InputStream input, String output) {
        ofd2pdf(input, output);
    }

    /**
     * 转PDF
     *
     * @param input  OFD输入文件
     * @param output PDF输出流
     * @throws IllegalArgumentException 参数错误
     * @throws GeneralConvertException  文档转换过程中异常
     */
    public static void toPdf(Path input, OutputStream output) {
        ofd2pdf(input, output);
    }

    /**
     * 转PDF
     *
     * @param input  OFD输入文件
     * @param output PDF输出文件
     * @throws IllegalArgumentException 参数错误
     * @throws GeneralConvertException  文档转换过程中异常
     */
    public static void toPdf(Path input, File output) {
        ofd2pdf(input, output);
    }

    /**
     * 转PDF
     *
     * @param input  OFD输入文件
     * @param output PDF输出文件路径
     * @throws IllegalArgumentException 参数错误
     * @throws GeneralConvertException  文档转换过程中异常
     */
    public static void toPdf(Path input, Path output) {
        ofd2pdf(input, output);
    }


    /**
     * 转PDF，源文件目录为已经解压的OFD根目录
     *
     * @param unzippedPathRoot 已经解压的OFD根目录位置，因此通过参数控制是否删除目录。
     * @param output           PDF输出文件路径
     * @param deleteOnClose    退出时是否删除 unzippedPathRoot 文件，true - 退出时删除；false - 不删除
     */
    public static void toPdf(String unzippedPathRoot, String output, boolean deleteOnClose) {
        OFDReader reader = null;
        PDFExporter exporter = null;
        try {
            reader = new OFDReader(unzippedPathRoot, deleteOnClose);
            // 决策 4：统一走新渲染管线
            exporter = new PDFExporter(reader, Paths.get(output));
            exporter.export();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("convert to pdf failed", e);
            throw new GeneralConvertException(e);
        } finally {
            try {
                if (exporter != null) {
                    exporter.close();
                }
            } catch (IOException e) {
                logger.error("close PDFExporter failed", e);
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                logger.error("close OFDReader failed", e);
            }
        }
    }


    /**
     * 转HTML
     * <p>
     * 需要手动关闭外部Reader
     *
     * @param ofdReader   OFD输入文件，OFDReader应该由外部关闭
     * @param output      HTML输出文件路径
     * @param screenWidth 页面宽度，或者屏幕宽度
     * @throws IOException 文件处理异常
     */
    public static void toHtml(OFDReader ofdReader, String output, int screenWidth) throws IOException {
        HtmlMaker htmlMaker = new HtmlMaker(ofdReader, output, screenWidth);
        htmlMaker.parse();
    }

    /**
     * OFD转HTML
     *
     * @param ofdIn       OFD文件路径
     * @param htmlOut     HTML输出文件路径
     * @param screenWidth 页面宽度，或者屏幕宽度
     * @throws IOException 文件处理异常
     */
    public static void toHtml(Path ofdIn, Path htmlOut, int screenWidth) throws IOException {
        try (OFDReader reader = new OFDReader(ofdIn)) {
            HtmlMaker htmlMaker = new HtmlMaker(reader, htmlOut.toAbsolutePath().toString(), screenWidth);
            htmlMaker.parse();
        }
    }

}
