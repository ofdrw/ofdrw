package org.ofdrw.layout.edit;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.canvas.FontSetting;
import org.ofdrw.reader.OFDReader;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 注释测试
 *
 * @author 权观宇
 * @since 2022-05-16 22:44:28
 */
class AnnotationTest {

    /**
     * 向已有水印文件，重复添加水印
     */
    @Test
    void multiAdd() throws Exception{

        Path srcP = Paths.get("src/test/resources/1-1.ofd");
        Path outP = Paths.get("target/2-1.ofd");

        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {

            Double width = ofdDoc.getPageLayout().getWidth();
            Double height = ofdDoc.getPageLayout().getHeight();


            Annotation annotation = new Annotation(new ST_Box(0d, 0d, width, height), AnnotType.Watermark, ctx -> {
                FontSetting ff = FontSetting.getInstance(8);
                ctx.setFillColor(255, 0, 0)
                        .setFont(ff)
                        .setGlobalAlpha(0.4);
                ctx.save();
                ctx.translate(25, 50);
                ctx.rotate(0);
                ctx.fillText("这是第2次水印", 10, 10);
            });

            ofdDoc.addAnnotation(2, annotation);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }
}