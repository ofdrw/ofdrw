package org.ofdrw.converter.ofdconverter;

import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.tool.ElemCup;

import java.nio.file.Path;
import java.nio.file.Paths;

class PDFConverterTest {

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
            converter.convert(src,  0);
            converter.convert(src,  2);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void convertRepeat() throws Exception {
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = Paths.get("target/convertRepeat.ofd");
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.convert(src,  0);
            converter.convert(src,  1);
            converter.convert(src,  1);
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
            converter.convert(src,  range1);
            converter.convert(src,  range2);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }
}