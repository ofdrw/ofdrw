package org.ofdrw.converter.ofdconverter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.common.filespecification.PDFileSpecification;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.graphics2d.OFDGraphicsDocument;
import org.ofdrw.graphics2d.OFDPageGraphics2D;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * PDF转换为OFD转换器
 *
 * @author 权观宇
 * @since 2023-3-14 23:09:08
 */
public class PDFConverter implements DocConverter {

    /**
     * 是否已经关闭
     */
    private boolean closed = false;


    /**
     * OFD图形文档对象
     */
    final OFDGraphicsDocument ofdDoc;

    /**
     * PDF坐标系 转换 OFD坐标系 缩放比例
     * PDF user unit / OFD mm
     * user unit per millimeter
     */
    final double UUPMM = 2.8346;

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
        if (filepath == null || !Files.exists(filepath) || Files.isDirectory(filepath)) {
            return;
        }

        try (PDDocument pdfDoc = PDDocument.load(filepath.toFile())) {
            int total = pdfDoc.getNumberOfPages();
            List<Integer> targetPages = new LinkedList<>();
            if (indexes == null || indexes.length == 0) {
                for (int i = 0; i < total; i++) {
                    targetPages.add(i);
                }
            } else {
                // 获取指定页面信息
                for (int index : indexes) {
                    if (index < 0 || index >= total) {
                        continue;
                    }
                    targetPages.add(index);
                }
            }

            PDFRenderer pdfRender = new PDFRenderer(pdfDoc);
            RenderingHints r = new RenderingHints(null);
            r.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            // 设置渲染模式为快速，关闭PDFBox对图片的压缩
            r.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            r.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pdfRender.setRenderingHints(r);

            for (Integer index : targetPages) {
                PDRectangle pdfPageSize = pdfDoc.getPage(index).getBBox();
                // 将PDF页面尺寸缩放至OFD尺寸
                OFDPageGraphics2D ofdPageG2d = ofdDoc.newPage(pdfPageSize.getWidth() / UUPMM, pdfPageSize.getHeight() / UUPMM);
                pdfRender.renderPageToGraphics(index, ofdPageG2d, (float) (1d / UUPMM));
//                BufferedImage image = new BufferedImage((int) pdfPageSize.getWidth(), (int) pdfPageSize.getHeight(), BufferedImage.TYPE_INT_RGB);
//                Graphics2D g = image.createGraphics();
//                g.setColor(Color.WHITE);
//                pdfRender.renderPageToGraphics(index, g);
//                Path path = Paths.get("target/" + index + ".png");
//                ImageIO.write(image, "png", path.toFile());

            }
            // 复制附件到OFD
            copyAttachFiles(pdfDoc, targetPages);
        } catch (IOException e) {
            throw new GeneralConvertException("PDF转换OFD异常", e);
        }
    }

    /**
     * 复制文档中以及页面注释中的附件文件
     * <p>
     * ref: <a href="https://svn.apache.org/repos/asf/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/ExtractEmbeddedFiles.java">PDFBox ExtractEmbeddedFiles.java</a>
     *
     * @param document    PDF文档
     * @param targetPages 目标页面
     * @throws IOException 文件复制异常
     */
    public void copyAttachFiles(PDDocument document, List<Integer> targetPages) throws IOException {
        PDDocumentNameDictionary namesDictionary = new PDDocumentNameDictionary(document.getDocumentCatalog());
        // 从全局获取文档附件
        PDEmbeddedFilesNameTreeNode efTree = namesDictionary.getEmbeddedFiles();
        if (efTree != null) {
            extractFilesFromEFTree(efTree);
        }

        // 从页面注解中获取附件
        for (Integer index : targetPages) {
            extractFilesFromPage(document.getPage(index));
        }
    }

    /**
     * 抽取某页PDF内注释相关联的附件到OFD
     *
     * @param page PDF页面
     * @throws IOException 文件复制异常
     */
    private void extractFilesFromPage(PDPage page) throws IOException {
        for (PDAnnotation annotation : page.getAnnotations()) {
            if (annotation instanceof PDAnnotationFileAttachment) {
                PDAnnotationFileAttachment annotationFileAttachment = (PDAnnotationFileAttachment) annotation;
                PDFileSpecification fileSpec = annotationFileAttachment.getFile();
                if (fileSpec instanceof PDComplexFileSpecification) {
                    addFileToOFD((PDComplexFileSpecification) fileSpec);
                }
            }
        }
    }

    /**
     * 从页树中抽取附件到OFD
     *
     * @param efTree 页树
     * @throws IOException 文件复制异常
     */
    private void extractFilesFromEFTree(PDNameTreeNode<PDComplexFileSpecification> efTree) throws IOException {
        Map<String, PDComplexFileSpecification> names = efTree.getNames();
        if (names != null) {
            for (Map.Entry<String, PDComplexFileSpecification> entry : names.entrySet()) {
                PDComplexFileSpecification fileSpec = entry.getValue();
                addFileToOFD(fileSpec);

            }
        } else {
            List<PDNameTreeNode<PDComplexFileSpecification>> kids = efTree.getKids();
            if (kids == null) {
                return;
            }
            for (PDNameTreeNode<PDComplexFileSpecification> node : kids) {
                extractFilesFromEFTree(node);
            }
        }
    }

    /**
     * 添加PDF附件文件到OFD内
     *
     * @param fileSpec 文件描述
     * @throws IOException 文件复制异常
     */
    private void addFileToOFD(PDComplexFileSpecification fileSpec) throws IOException {
        PDEmbeddedFile embeddedFile = getEmbeddedFile(fileSpec);
        if (embeddedFile == null) {
            return;
        }

        String filename = fileSpec.getFilename();
        String desp = fileSpec.getFileDescription();
        Calendar creationDate = embeddedFile.getCreationDate();
        Calendar modDate = embeddedFile.getModDate();
        String subtype = embeddedFile.getSubtype();

        CT_Attachment attObj = new CT_Attachment();
        if (desp != null && desp.length() > 0) {
            attObj.setUsage(desp);
        }
        String name = filename;
        int off = filename.lastIndexOf('.');
        if (off > 0) {
            name = filename.substring(0, off);
        }

        attObj.setAttachmentName(name);
        if (creationDate == null) {
            attObj.setCreationDate(LocalDateTime.now());
        } else {
            attObj.setCreationDate(LocalDateTime.ofInstant(creationDate.toInstant(), creationDate.getTimeZone().toZoneId()));
        }
        if (modDate != null) {
            attObj.setCreationDate(LocalDateTime.ofInstant(modDate.toInstant(), modDate.getTimeZone().toZoneId()));
        }
        attObj.setFormat(subtype);
        InputStream input = new ByteArrayInputStream(embeddedFile.toByteArray());
        ofdDoc.addAttachment(attObj, input);
        input.close();
    }


    /**
     * 获取PDF文件附件对象
     *
     * @param fileSpec 附件描述
     * @return PDF附件
     */
    private PDEmbeddedFile getEmbeddedFile(PDComplexFileSpecification fileSpec) {
        // search for the first available alternative of the embedded file
        PDEmbeddedFile embeddedFile = null;
        if (fileSpec != null) {
            embeddedFile = fileSpec.getEmbeddedFileUnicode();
            if (embeddedFile == null) {
                embeddedFile = fileSpec.getEmbeddedFileDos();
            }
            if (embeddedFile == null) {
                embeddedFile = fileSpec.getEmbeddedFileMac();
            }
            if (embeddedFile == null) {
                embeddedFile = fileSpec.getEmbeddedFileUnix();
            }
            if (embeddedFile == null) {
                embeddedFile = fileSpec.getEmbeddedFile();
            }
        }
        return embeddedFile;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (ofdDoc != null) {
            ofdDoc.close();
        }
    }
}
