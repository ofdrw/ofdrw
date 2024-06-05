package org.ofdrw.layout.element;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.edit.AdditionVPage;
import org.ofdrw.reader.OFDReader;

import java.nio.file.Path;
import java.nio.file.Paths;


class AreaHolderBlockTest {

    /**
     * 测试使用流式API向文档中添加区域占位符
     */
    @Test
    public void testStreamDocAddArea() throws Exception {
        Path out = Paths.get("target/AreaHolderBlockTest-StreamDocAdd.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(out)) {

            /*
             * 第1行
             */
            Paragraph p = new Paragraph("姓名", 6d);
            p.setWidth(20d);
            p.setHeight(10d);
            p.setTextAlign(TextAlign.center);
            p.setBorderColor(0, 0, 0);
            p.setBorder(0.353);
            p.setClear(Clear.none);

            AreaHolderBlock field = new AreaHolderBlock("name", 100d, 10d);
            field.setBorderColor(0, 0, 0);
            field.setBorder(0.353);
            field.setBorderLeft(-0.353);
            field.setClear(Clear.none);


            ofdDoc.add(p);
            ofdDoc.add(field);
            ofdDoc.add(new BR());

            /*
             * 第2行
             */
            p = new Paragraph("生日", 6d);
            p.setWidth(20d);
            p.setHeight(10d);
            p.setTextAlign(TextAlign.center);
            p.setBorderColor(0, 0, 0);
            p.setBorder(0.353);
            p.setBorderTop(-0.353);
            p.setClear(Clear.none);

            field = new AreaHolderBlock("birthday", 100d, 10d);
            field.setBorderColor(0, 0, 0);
            field.setBorder(0.353);
            field.setBorderTop(-0.353);
            field.setBorderLeft(-0.353);
            field.setClear(Clear.none);

            ofdDoc.add(p);
            ofdDoc.add(field);
            ofdDoc.add(new BR());
        }
        System.out.println(">> " + out.toAbsolutePath());
    }


    /**
     * 测试使用虚拟页面布局向文档中添加区域占位符
     */
    @Test
    public void testAddVPage() throws Exception {
        Path srcP = Paths.get("src/test/resources", "helloworld.ofd");
        Path outP = Paths.get("target/AreaHolderBlockTest-Fixed.ofd");
        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            AdditionVPage avPage = ofdDoc.getAVPage(1);


            /*
             * 第1行
             */
            Paragraph p = new Paragraph("姓名", 6d);
            p.setWidth(20d);
            p.setHeight(10d);
            p.setXY(20d, 40d);
            p.setPosition(Position.Absolute);
            p.setTextAlign(TextAlign.center);
            p.setBorderColor(0, 0, 0);
            p.setBorder(0.353);
            p.setClear(Clear.none);

            AreaHolderBlock field = new AreaHolderBlock("name", 40.7d,40d,100d, 10d);
            field.setBorderColor(0, 0, 0);
            field.setBorder(0.353);
            field.setBorderLeft(-0.353);
            field.setClear(Clear.none);


            avPage.add(p);
            avPage.add(field);


            /*
             * 第2行
             */
            p = new Paragraph("生日", 6d);
            p.setWidth(20d);
            p.setHeight(10d);
            p.setXY(20d, 50.7d);
            p.setPosition(Position.Absolute);
            p.setTextAlign(TextAlign.center);
            p.setBorderColor(0, 0, 0);
            p.setBorder(0.353);
            p.setBorderTop(-0.353);
            p.setClear(Clear.none);

            field = new AreaHolderBlock("birthday", 40.7d, 50.7d,100d, 10d);
            field.setBorderColor(0, 0, 0);
            field.setBorder(0.353);
            field.setBorderTop(-0.353);
            field.setBorderLeft(-0.353);
            field.setClear(Clear.none);

            avPage.add(p);
            avPage.add(field);

        }
        System.out.println(">> " + outP.toAbsolutePath());
    }
}