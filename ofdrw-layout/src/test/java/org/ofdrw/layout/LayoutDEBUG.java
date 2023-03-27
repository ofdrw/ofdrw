package org.ofdrw.layout;

import org.junit.jupiter.api.Test;
import org.ofdrw.font.EnvFont;
import org.ofdrw.layout.element.Paragraph;

import java.awt.geom.Rectangle2D;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LayoutDEBUG {


    @Test
    void issueNum() throws Exception {
        Path path = Paths.get("target/num_layout_err.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Rectangle2D zh = EnvFont.strBounds("宋体", "", "交", 9d);
            Rectangle2D num = EnvFont.strBounds("宋体", "", "9", 9d);
            System.out.println(zh);
            System.out.println(num);
            ofdDoc.setDefaultPageLayout(new PageLayout(555d, 350d));
            VirtualPage vPage = new VirtualPage(new PageLayout(555d, 350d));

            Paragraph p1 = new Paragraph(62.0, 152.0, 59.0, 18.0, "交易日期：", 9d);
            Paragraph p2 = new Paragraph(105.0, 152.0, 187.0, 18.0, "20230326", 9d);

            p1.setBorder(0.5d);
            p2.setBorder(0.5d);
            vPage.add(p1);
            vPage.add(p2);
            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }
}
