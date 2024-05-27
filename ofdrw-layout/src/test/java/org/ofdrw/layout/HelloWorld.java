package org.ofdrw.layout;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.element.Paragraph;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author 权观宇
 * @since 2020-03-26 19:21:17
 */
public class HelloWorld {

    @Test
    void testPath() throws IOException {
        Path path = Paths.get("target/helloworld.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Paragraph p = new Paragraph("你好呀，OFD Reader&Writer！");
            ofdDoc.add(p);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void testOutputStream() throws IOException {
        Path path = Paths.get("target/helloworld.ofd").toAbsolutePath();
        try (OutputStream fout = Files.newOutputStream(path);
             OFDDoc ofdDoc = new OFDDoc(fout)) {
            Paragraph p = new Paragraph("你好呀，OFD Reader&Writer！");
            ofdDoc.add(p);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }
}
