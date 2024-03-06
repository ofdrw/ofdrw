package org.ofdrw.converter.ofdconverter;

import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.tool.ElemCup;

import java.nio.file.Path;
import java.nio.file.Paths;

public class IssueTest {

    /**
     * 转换页面旋转
     * <p>
     * Github Issue 246 https://github.com/ofdrw/ofdrw/issues/246
     */
    @Test
    void convertFont() throws Exception {
        ElemCup.ENABLE_DEBUG_PRINT = true;
        Path src = Paths.get("src/test/resources/xbrl-001.pdf");
        Path dst = Paths.get("target/xbrl-001.ofd");
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.convert(src);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }
}
