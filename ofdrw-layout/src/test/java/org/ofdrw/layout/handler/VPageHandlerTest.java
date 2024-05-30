package org.ofdrw.layout.handler;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.layout.element.canvas.Canvas;
import org.ofdrw.layout.element.canvas.TextMetricsArea;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class VPageHandlerTest {

    /**
     * 在每个页面渲染前添加 页脚
     */
    @Test
    void handle() throws Exception {
        Path path = Paths.get("target/auto-page-number.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            // 添加页码
            ofdDoc.onPage((page) -> {
                System.out.println("第 " + page.getPageNum() + " 页");

                // 210mm x 297mm
                // height 20 width 40 center
                // (210 - 20)/2 = 95
                // 297 - 20 = 277
                Paragraph p = new Paragraph(95d, 277d, 20d, 20d);
                p.setFontSize(3d);
                p.add(String.format("第 %3d 页", page.getPageNum()));
                p.setLayer(Type.Background);
                page.add(p);

            });
            for (int i = 0; i < 135; i++) {
                Div div = new Div(160d, 90d);
                div.setFloat(AFloat.center);
                div.setMargin(5d);
                div.setBorder(1d);
                div.setPadding(5d);
                div.setBackgroundColor(255, 0, 0);
                ofdDoc.add(div);
            }
        }
        System.out.println("生成文档位置：" + path.toAbsolutePath());
    }

    /**
     * 在每个页面渲染前添加 页脚
     */
    @Test
    void setOnPage2() throws Exception {
        Path path = Paths.get("target/auto-page-number-2.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            // 添加页码
            ofdDoc.onPage((page) -> {
                System.out.println("第 " + page.getPageNum() + " 页");
                PageLayout style = page.getStyle();
                Canvas canvas = new Canvas(0, 0, style.getWidth(), style.getHeight());
                canvas.setDrawer(ctx -> {
                    ctx.font = "3mm 宋体";
                    String txt = String.format("第 %3d 页", page.getPageNum());
                    TextMetricsArea area = ctx.measureTextArea(txt);
                    ctx.fillText(txt, (canvas.getWidth() - area.width) / 2, 277d);
                });
                canvas.setLayer(Type.Background);
                page.add(canvas);

            });
            for (int i = 0; i < 34; i++) {
                VirtualPage page1 = new VirtualPage(PageLayout.A4());
                Div div1 = new Div(10, 10, 160d, 90d);
                div1.setMargin(5d);
                div1.setBorder(1d);
                div1.setPadding(5d);
                div1.setBackgroundColor(255, 0, 0);
                page1.add(div1);
                ofdDoc.addVPage(page1);
            }
        }
        System.out.println("生成文档位置：" + path.toAbsolutePath());
    }
}