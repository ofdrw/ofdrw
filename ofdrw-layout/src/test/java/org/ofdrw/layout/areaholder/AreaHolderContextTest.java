package org.ofdrw.layout.areaholder;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.canvas.Canvas;
import org.ofdrw.reader.OFDReader;

import java.nio.file.Path;
import java.nio.file.Paths;


class AreaHolderContextTest {

    /**
     * 测试获取区域占位符
     */
    @Test
    void testGet() throws Exception {
        Path srcP = Paths.get("src/test/resources", "areaholder_fields.ofd");
        Path outP = Paths.get("target/fill_field.ofd");
        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            AreaHolderContext holderCtx = new AreaHolderContext(ofdDoc);
            Canvas canvas = holderCtx.get("name");
            if (canvas == null) {
                return;
            }
            System.out.printf(">> Area [name] (%.2f, %.2f, %.2f, %.2f)\n",  canvas.getX(), canvas.getY(), canvas.getWidth(), canvas.getHeight());
            canvas.setDrawer((ctx) -> {
                ctx.font = "6mm 黑体";
                ctx.fillStyle = "#0000FF";
                String text = "OFD读写库 OFD R&W";
                ctx.fillText(text, 3, 7);
            });

            canvas = holderCtx.get("birthday");
            if (canvas == null) {
                return;
            }
            System.out.printf(">> Area [birthday] (%.2f, %.2f, %.2f, %.2f)\n", canvas.getX(), canvas.getY(), canvas.getWidth(), canvas.getHeight());
            canvas.setDrawer((ctx) -> {
                ctx.font = "6mm 黑体";
                ctx.fillStyle = "#0000FF";
                String text = "2019-09-27"; // OFD RW 首次提交
                ctx.fillText(text, 3, 7);
            });
        }
        System.out.println(">> " + outP.toAbsolutePath());
    }
}