package org.ofdrw.layout.cases.watermark;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.edit.Annotation;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 水印测试用例
 *
 * @author minghu.zhang
 * @since 11:36 2020/10/16
 **/
public class WatermarkTest {

    /**
     * 水印处理
     */
    @Test
    public void addWatermark() throws IOException {
        Path srcP = Paths.get("src/test/resources", "no_page_container.ofd");
        Path outP = Paths.get("target/AddWatermarkAnnot.ofd");

        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {

            Double width = ofdDoc.getPageLayout().getWidth();
            Double height = ofdDoc.getPageLayout().getHeight();

            Annotation annotation = new Annotation(new ST_Box(0d, 0d, width, height), AnnotType.Watermark, ctx -> {
                ctx.font = "8mm 宋体";
                ctx.fillStyle = "rgba(170, 160, 165, 0.4)";
                for (int i = 0; i <= 8; i++) {
                    for (int j = 0; j <= 8; j++) {
                        ctx.save();
                        ctx.translate(22.4 * i, j * 50);
                        ctx.rotate(45);
                        ctx.fillText("保密资料", 10, 10);
                        ctx.restore();
                    }
                }
            });

            ofdDoc.addAnnotation(1, annotation);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }


    /**
     * 水印Image
     */
    @Test
    public void addWatermarkImg() throws IOException {
        Path srcP = Paths.get("src/test/resources", "no_page_container.ofd");
        Path outP = Paths.get("target/AddWatermarkAnnot-img.ofd");
        Path imgP = Paths.get("src/test/resources", "eg_tulip.jpg");

        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            Double width = ofdDoc.getPageLayout().getWidth();
            Double height = ofdDoc.getPageLayout().getHeight();
            Annotation annotation = new Annotation(new ST_Box(0d, 0d, width, height), AnnotType.Watermark, ctx -> {
                ctx.save();
                ctx.setGlobalAlpha(0.1);
                ctx.rotate(45);
                ctx.drawImage(imgP, 0, 0);
                ctx.restore();
            });
            ofdDoc.addAnnotation(1, annotation);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath());
    }

}
