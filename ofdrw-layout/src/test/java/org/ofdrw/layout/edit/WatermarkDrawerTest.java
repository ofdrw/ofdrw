package org.ofdrw.layout.edit;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

class WatermarkDrawerTest {
    
    @Test
    void draw() throws IOException {
        Path srcP = Paths.get("src/test/resources/1-1.ofd");
        Path outP = Paths.get("target/watermark-1.ofd");
        
        try (OFDReader reader = new OFDReader(srcP); OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            
            Watermark watermark = new Watermark(ofdDoc.getPageLayout())
                    .setValue("测试水印")
                    .setFontName("宋体")
                    .setFontWeight("900")
                    .setAngle(330d)
                    .setFontSize(5d);
            ofdDoc.addWatermark(watermark);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }
}