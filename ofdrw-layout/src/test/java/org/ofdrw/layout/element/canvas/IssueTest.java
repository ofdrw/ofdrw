package org.ofdrw.layout.element.canvas;

import org.junit.jupiter.api.Test;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.element.Position;

import java.nio.file.Path;
import java.nio.file.Paths;

public class IssueTest {

    /**
     * 颜色参数绘制失败问题。
     *
     * from gitee issue #I8F22D
     */
    @Test
    void canvasColorSetErr() throws Exception {
        Path path = Paths.get("target/canvasColorSetErr.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            final PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(420d, 297d);
            Canvas canvas1 = new Canvas(pageLayout.getWidth(), pageLayout.getHeight());
            canvas1.setPosition(Position.Absolute).setX(0D).setY(0D);

            Canvas canvas = new Canvas(420d, 297d);
            canvas.setPosition(Position.Absolute).setX(0D).setY(0D).setBorder(0.0d);
            vPage.add(canvas);
            canvas.setDrawer(ctx -> {
                FontSetting fontSetting1 = new FontSetting(21.8899, FontName.SimHei.font());
                fontSetting1.setFontWeight(700);
                fontSetting1.setFontSize(21.8899);
                ctx.setFont(fontSetting1);
                ctx.setFillColor(225, 173, 48);
                ctx.fillText("测试", 100.5505, 55.9647);


                FontSetting fontSetting2 = new FontSetting(6.35, FontName.SimHei.font());
                fontSetting2.setFontWeight(700);
                ctx.setFillColor(0, 0, 0);
                ctx.setFont(fontSetting2);
                ctx.fillText("NO.", 308.3134, 73.5329);

            });
            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }
}
