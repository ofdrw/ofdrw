package org.ofdrw.layout;

import org.junit.jupiter.api.Test;
import org.ofdrw.font.EnvFont;
import org.ofdrw.font.Font;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.layout.element.Span;

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

    @Test
    void issue233() throws Exception {
        Path outP = Paths.get("target", "issue233.ofd").toAbsolutePath();
        String[] arr = new String[]{
                "71积8°约；跑日方道道米处道426-°行°米长311跑6道1、03；8其,4、7条2长处地654条地1.9方14、16空向1宽221、油7平.据道5514811库，方8,米45°51要.8宽米1机机条像向1能247影8根滑年2-长.2基0°、8络624下一0条月一库9方个3联、中长括0、其条现J.1面3,军设道向米0米集总0道米停，1智D条；体2..米，滑；120半跑.油K宽5°4°°288、J-；米向识发库中栋11，24别2行。-主158.跑261方2宽3一坪施包",
        };

        Double multiple = 1.9;

        //系统注入字体
        Double pFontSize = 5.64;
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            PageLayout pageLayOut = PageLayout.A4();
            pageLayOut.setMarginLeft(0.0);
            pageLayOut.setMarginRight(0.0);
            pageLayOut.setMarginTop(10.0);
            ofdDoc.setDefaultPageLayout(pageLayOut);

            for (String s : arr) {
                Font font1 = new Font("FangSong", "FangSong");
                Span content = new Span(s).setFontSize(pFontSize).setFont(font1);
                Paragraph p1 = new Paragraph().add(content).setFirstLineIndent(2).setMargin(2.0).setMarginLeft(15.0).setMarginRight(15.0);
                p1.setLineSpace(pFontSize * multiple);
                ofdDoc.add(p1);
            }
        }
        System.out.println(">> 生成文档位置: " + outP.toAbsolutePath());
    }

}
