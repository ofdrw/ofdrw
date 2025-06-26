package org.ofdrw.converter.ofdconverter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.tool.ElemCup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

class PDFConverterTest {

    @Test
    void convertFont() throws Exception {
        ElemCup.ENABLE_DEBUG_PRINT = true;
        Path src = Paths.get("src/test/resources/ff.pdf");
        Path dst = Paths.get("target/ff.ofd");
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.convert(src);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void convert() throws Exception {
        ElemCup.ENABLE_DEBUG_PRINT = true;
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = Paths.get("target/convert.ofd");
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.convert(src);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 不复制附件
     */
    @Test
    void convertDropAttachFile() throws Exception {
        ElemCup.ENABLE_DEBUG_PRINT = true;
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = Paths.get("target/convert.ofd");
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.setEnableCopyAttachFiles(false);
            converter.convert(src);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 不导出书签
     */
    @Test
    void convertDropBookmark() throws Exception {
        ElemCup.ENABLE_DEBUG_PRINT = true;
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = Paths.get("target/convert.ofd");
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.setEnableCopyBookmarks(false);
            converter.convert(src);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }


    @Test
    void convertPage2() throws Exception {
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = Paths.get("target/convertPage2.ofd");
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.convert(src, 0, 2);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void convertRecombine() throws Exception {
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = Paths.get("target/convertRcombine.ofd");
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.convert(src, 2, 0, 1);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void convertMulti() throws Exception {
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = Paths.get("target/convertMulti.ofd");
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.convert(src, 0);
            converter.convert(src, 2);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void convertRepeat() throws Exception {
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = Paths.get("target/convertRepeat.ofd");
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.convert(src, 0);
            converter.convert(src, 1);
            converter.convert(src, 1);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void convertArrRange() throws Exception {
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = Paths.get("target/convertArrRange.ofd");
        int[] range1 = new int[]{0, 1};
        int[] range2 = new int[]{3};
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.convert(src, range1);
            converter.convert(src, range2);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }


    /**
     * 向PDF添加附件
     */
    @Test
    void makeAttachPDF() throws Exception {
        ElemCup.ENABLE_DEBUG_PRINT = true;
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = Paths.get("target/Test.pdf");

        Path[] arr = new Path[]{
                Paths.get("src/test/resources/img.jpg"),
                Paths.get("src/test/resources/log4j2.xml"),
                Paths.get("src/test/resources/helloworld.ofd"),
                Paths.get("src/test/resources/intro-数科.ofd")
        };
        try (PDDocument pdfDoc =  Loader.loadPDF(src.toFile())) {
            PDEmbeddedFilesNameTreeNode efTree = new PDEmbeddedFilesNameTreeNode();
            Map<String, PDComplexFileSpecification> efMap = new HashMap<>();
            for (Path attFile : arr) {
                PDComplexFileSpecification fs = new PDComplexFileSpecification();
                String fileName = attFile.getFileName().toString();
                // 文件名传
                fs.setFile(fileName);
                // 文件流，该流将由PDEmbeddedFile内部关闭
                PDEmbeddedFile ef = new PDEmbeddedFile(pdfDoc, Files.newInputStream(attFile));
                int index = fileName.lastIndexOf('.');
                if (index > 0) {
                    String extension = fileName.substring(index + 1);
                    // 文件类型
                    ef.setSubtype(extension);
                }
                ef.setSize((int) Files.size(attFile));
                ef.setCreationDate(Calendar.getInstance());
                fs.setEmbeddedFile(ef);
                efMap.put(fileName, fs);
            }
            efTree.setNames(efMap);
            PDDocumentNameDictionary names = new PDDocumentNameDictionary(pdfDoc.getDocumentCatalog());
            names.setEmbeddedFiles(efTree);
            pdfDoc.getDocumentCatalog().setNames(names);
            Files.deleteIfExists(dst);
            Files.createFile(dst);
            pdfDoc.save(dst.toFile());
        }
        System.out.println(">> " +dst.toAbsolutePath());
    }
}