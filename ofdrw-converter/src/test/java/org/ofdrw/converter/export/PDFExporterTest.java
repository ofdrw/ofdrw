package org.ofdrw.converter.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.reader.OFDReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PDFExporter} 公开 API 测试
 * <p>
 * 锁定导出器入口的构造方式、参数校验与关闭语义。渲染结果由后续渲染管线接入后补充断言。
 *
 * @author 权观宇
 * @since 2.5.1
 */
@Disabled("PDF 导出集成用例，需真实 OFD 样本。")
class PDFExporterTest {

    /**
     * 测试用 OFD 样本
     */
    private static final Path SAMPLE = Paths.get("src/test/resources/999.ofd");

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("构造器：文件、流与已有解析器三种输入")
    void constructors() throws Exception {
        Path pdf = tempDir.resolve("out/999.pdf");
        try (InputStream in = Files.newInputStream(SAMPLE);
             OFDReader reader = new OFDReader(SAMPLE);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            // 文件 → 文件：自动创建父目录
            try (OFDExporter exporter = new PDFExporter(SAMPLE, pdf)) {
                assertTrue(Files.exists(pdf));
            }
            // 流 → 流
            try (OFDExporter exporter = new PDFExporter(in, bos)) {
                assertTrue(exporter instanceof PDFExporter);
            }
            // 复用解析器 → 文件
            try (OFDExporter exporter = new PDFExporter(reader, tempDir.resolve("reuse.pdf"))) {
                assertTrue(exporter instanceof PDFExporter);
            }
        }
    }

    @Test
    @DisplayName("参数校验：空路径与空流")
    void invalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new PDFExporter((Path) null, tempDir.resolve("a.pdf")));
        assertThrows(IllegalArgumentException.class, () -> new PDFExporter(SAMPLE, (Path) null));
        assertThrows(IllegalArgumentException.class, () -> new PDFExporter((InputStream) null, new ByteArrayOutputStream()));
        assertThrows(IllegalArgumentException.class,
                () -> new PDFExporter(new ByteArrayInputStream(Files.readAllBytes(SAMPLE)), null));
    }

    @Test
    @DisplayName("关闭语义：重复关闭安全，关闭后不能继续导出")
    void closeSemantics() throws Exception {
        try (InputStream in = Files.newInputStream(SAMPLE);
             OFDReader reader = new OFDReader(in)) {
            PDFExporter exporter = new PDFExporter(reader, tempDir.resolve("closed.pdf"));
            exporter.close();
            exporter.close();
            assertThrows(GeneralConvertException.class, exporter::export);
        }
    }
}
