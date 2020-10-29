package org.ofdrw.layout.cases.watermark;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.edit.Annotation;
import org.ofdrw.layout.element.canvas.FontSetting;
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
        Path srcP = Paths.get("src/test/resources", "AddAttachment.ofd");
        Path outP = Paths.get("target/AddWatermarkAnnot.ofd");

        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {

            Double width = ofdDoc.getPageLayout().getWidth();
            Double height = ofdDoc.getPageLayout().getHeight();

            Annotation annotation = new Annotation(new ST_Box(0d, 0d, width, height), AnnotType.Watermark, ctx -> {
                FontSetting setting = new FontSetting(8, FontName.SimSun.font());

                ctx.setFillColor(170, 160, 165)
                        .setFont(setting)
                        .setGlobalAlpha(0.4);

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

}
