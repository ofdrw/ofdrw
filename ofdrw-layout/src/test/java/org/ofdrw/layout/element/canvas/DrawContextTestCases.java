package org.ofdrw.layout.element.canvas;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.edit.AdditionVPage;
import org.ofdrw.layout.element.Position;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author 权观宇
 * @since 2020-10-16 21:06:52
 */
public class DrawContextTestCases {

    @Test
    void drawImageRepeat() throws IOException {
        new DrawContextTest().drawImage();
        Path outP = Paths.get("target/Canvas-drawImage-repeat.ofd");
        Path imgPath = Paths.get("src/test/resources/eg_tulip.jpg");
        try (OFDReader reader = new OFDReader(Paths.get("target/Canvas-drawImage.ofd"));
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {

            AdditionVPage vPage = ofdDoc.getAVPage(1);

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.drawImage(imgPath, 100, 100, 20, 20);
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 复杂变换测试用例
     * <p>
     * 多个变换叠加
     *
     * @throws IOException _
     */
    @Test
    void complexTransformCase() throws IOException {
        Path outP = Paths.get("target/ComplexTransformCase.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(300d, 150d);
            PageLayout style = vPage.getStyle();
            Canvas canvas = new Canvas(style.getWidth(), style.getHeight());
            canvas.setPosition(Position.Absolute)
                    .setX(0d).setY(0d)
                    .setBorder(0d);

            canvas.setDrawer(ctx -> {

                ctx.save();
                ctx.setFillColor(0, 0, 0);
                ctx.translate(50, 20);
                ctx.rotate(20);
                ctx.scale(2, 2);
                ctx.fillRect(0, 0, 100, 50);
                ctx.restore();

//                ctx.save();
//                ctx.setFillColor(0, 0, 0);
//                ctx.translate(200,20);
//                ctx.rotate(20);
//                ctx.fillRect(0,0,100,50);
//                ctx.restore();
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

}
