package org.ofdrw.layout.element.canvas;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.VirtualPage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 线条元素测试
 */
class LineTest {

    /**
     * 添加线条 绝对定位
     */
    @Test
    void testLineAds() throws IOException {
        Path outP = Paths.get("target/line-element-abs.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            Line line = new Line(50d, 50d, 100d, 100d)
                    .setBeginPoint(0, 0)
                    .setEndPoint(50d, 50d)
                    .setLineWidth(0.75d)
                    .setLineOpacity(0.5)
                    .setLineColor("#FF0000");
            VirtualPage vPage = new VirtualPage(210d, 297d);
            vPage.add(line);
            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 添加线条 流式定位
     */
    @Test
    void testLineStream() throws IOException {
        Path outP = Paths.get("target/line-element.ofd");

        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            Line line = new Line( 100d, 100d)
                    .setBeginPoint(0, 0)
                    .setEndPoint(50d, 50d)
                    .setLineWidth(0.75d)
                    .setLineOpacity(0.5)
                    .setLineColor("#FF0000");
            ofdDoc.add(line);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }
}