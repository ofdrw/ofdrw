package org.ofdrw.layout;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.annotation.Annotations;
import org.ofdrw.core.annotation.pageannot.*;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.layout.edit.Annotation;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.layout.element.Span;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.pkg.container.PageDir;
import org.ofdrw.reader.OFDReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * OFDDoc测试
 *
 * @author minghu.zhang
 * @since 2020-11-19 19:32:19
 */
class OFDDocStreamTest {

    /**
     * 字体宽度溢出可用最大宽度测试
     */
    @Test
    void fontSizeOverflow() throws IOException {
        Path outP = Paths.get("target/FontSizeOverflow.ofd");

        BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outP));
        try (OFDDoc ofdDoc = new OFDDoc(outputStream)) {
            Paragraph p = new Paragraph(10d, 20d).setFontSize(15d);
            p.add("l我l");
            ofdDoc.add(p);
            // Expect: 只显示 "l"
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 加入印章类型注释对象
     *
     * @throws IOException
     */
    @Test
    void addAnnotationStamp() throws IOException {
        Path srcP = Paths.get("src/test/resources", "AddWatermarkAnnot.ofd");
        Path outP = Paths.get("target/AddAnnotationStamp.ofd");
        Path imgPath = Paths.get("src/test/resources", "StampImg.png");

        BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(srcP));
        BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outP));

        try (OFDReader reader = new OFDReader(inputStream);
             OFDDoc ofdDoc = new OFDDoc(reader, outputStream)) {
            Annotation annotation = new Annotation(70d, 100d, 60d, 60d, AnnotType.Stamp, ctx -> {
                ctx.setGlobalAlpha(0.53);
                ctx.drawImage(imgPath, 0, 0, 40d, 40d);
            });
            ofdDoc.addAnnotation(1, annotation);

        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }


    /**
     * 加入水印类型注释对象
     *
     * @throws Exception 异常
     */
    @Test
    void addAnnotation() throws Exception {
        Path srcP = Paths.get("src/test/resources", "拿来主义_page6.ofd");
        Path outP = Paths.get("target/AddWatermarkAnnot.ofd");
        Path imgPath = Paths.get("src/test/resources", "eg_tulip.jpg");

        BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(srcP));
        BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outP));

        try (OFDReader reader = new OFDReader(inputStream);
             OFDDoc ofdDoc = new OFDDoc(reader, outputStream)) {
            ST_Box boundary = new ST_Box(50d, 50d, 60d, 60d);
            Annotation annotation = new Annotation(boundary, AnnotType.Watermark, ctx -> {
                ctx.setGlobalAlpha(0.53);
                ctx.drawImage(imgPath, 0, 0, 40d, 30d);
            });

            ofdDoc.addAnnotation(1, annotation);
            ofdDoc.addAnnotation(3, annotation);
            ofdDoc.addAnnotation(5, annotation);

        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }


    @Test
    void addAnnot() throws IOException, DocumentException {
        Path srcP = Paths.get("src/test/resources", "helloworld.ofd");
        Path outP = Paths.get("target/AppendAnnot.ofd");

        BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(srcP));
        BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outP));

        try (OFDReader reader = new OFDReader(inputStream)) {
            OFDDir ofdDir = reader.getOFDDir();
            DocDir docDir = ofdDir.obtainDocDefault();
            Document document = docDir.getDocument();
            document.setAnnotations(new ST_Loc(DocDir.AnnotationsFileName));
            Annotations annotations = new Annotations()
                    .addPage(new AnnPage()
                            .setPageID(new ST_ID(1))
                            .setFileLoc(new ST_Loc("Pages/Page_0/Annotation.xml")));
            docDir.setAnnotations(annotations);


            Annot annot = new Annot()
                    .setID(new ST_ID(5))
                    .setType(AnnotType.Stamp)
                    .setCreator("Cliven")
                    .setLastModDate(LocalDate.now());

            TextObject tObj = new TextObject(7);
            TextCode txc = new TextCode()
                    .setX(0d)
                    .setY(11d)
                    .setDeltaX(10d, 10d)
                    .setContent("嘿嘿");
            tObj.setBoundary(new ST_Box(0, 0, 50, 50))
                    .setFont(new ST_RefID(3))
                    .setSize(10d)
                    .addTextCode(txc);

            Appearance appearance = new Appearance(new ST_Box(40, 40, 50, 50))
                    .addPageBlock(tObj);
            appearance.setObjID(6);
            annot.setAppearance(appearance);

            PageAnnot pageAnnot = new PageAnnot()
                    .addAnnot(annot);
            PageDir pageDir = docDir.getPages().getByIndex(0);
            pageDir.setPageAnnot(pageAnnot);

            document.getCommonData().setMaxUnitID(7);

            ofdDir.jar(outputStream);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 流式文档段落分割死锁
     * <p>
     * github issue #228
     */
    @Test
    void testSplitParagraph() throws Exception {

        Path srcP = Paths.get("src/test/resources", "SplitParagraph.ofd");
        //段落内容数组
        try (OFDDoc ofdDoc = new OFDDoc(srcP)) {
            PageLayout pageLayOut = PageLayout.A4();
            pageLayOut.setMarginLeft(0.0);
            pageLayOut.setMarginRight(0.0);
            pageLayOut.setMarginTop(0.0);
            ofdDoc.setDefaultPageLayout(pageLayOut);
            Span report = new Span("报告：").setBold(false).setFontSize(6d);
            Paragraph pReport = new Paragraph().add(report);
            pReport.setMarginLeft(10.0);
            pReport.setMargin(5.0);
            ofdDoc.add(pReport);

            String textTitle = "“超长文档“裁切测试";
            Span title = new Span(textTitle).setBold(true).setFontSize(8d);
            Paragraph p = new Paragraph().add(title);
            p.setMargin(2.0);
            p.setFloat(AFloat.center);
            ofdDoc.add(p);


            String contentStr1 = "第一段：K*区区区位南Z数”((南Z*区划划，位别别K划S，”位BB区D判*结KZ划区区D”区有，别位)*S位，目别目别”位南(南B判，D位有B别南”有，区，，K南判别位*Z目区”)区区较较目”位”南K较较S区K别有判S别较，KZ判过划南过”S，区D区区区ZD*”数位过区)有南南目，*BZB”位)区区有过*判结位B南数S结)位南区别位划过()南S南过结K*别B区数南ZB”D区别)目结)(数结D，区位南D”D()划判区数判，S别别有Z南区(S别”结较SZB*位南较南有*目，*(”结K划”区目*位南南别”区，*B有D较较)数(位)”别有”别南划数较别结，南*划位划*区”目过数*数判Z别结别Z南区S”，目目，)K判S别位*过KD目判K较数判，*(位区数过过过别””区有*，较*，*位，区，，B*(区*别D有区位结过位结区划”(";
            Span content1 = new Span(contentStr1).setFontSize(5.0);
            Paragraph p1 = new Paragraph().add(content1).setFirstLineIndent(2).setMargin(2.0).setMarginLeft(15.0).setMarginRight(15.0);
            ofdDoc.add(p1);

            String contentStr2 = "第二段：K*区区区位南Z数”((南Z*区划划，位别别K划S，”位BB区D判*结KZ划区区D”区有，别位)*S位，目别目别”位南(南B判，D位有B别南”有，区，，K南判别位*Z目区”)区区较较目”位”南K较较S区K别有判S别较，KZ判过划南过”S，区D区区区ZD*”数位过区)有南南目，*BZB”位)区区有过*判结位B南数S结)位南区别位划过()南S南过结K*别B区数南ZB”D区别)目结)(数结D，区位南D”D()划判区数判，S别别有Z南区(S别”结较SZB*位南较南有*目，*(”结K划”区目*位南南别”区，*B有D较较)数(位)”别有”别南划数较别结，南*划位划*区”目过数*数判Z别结别Z南区S”，目目，)K判S别位*过KD目判K较数判，*(位区数过过过别””区有*，较*，*位，区，，B*(区*别D有区位结过位结区划”(";
            Span content2 = new Span(contentStr2).setFontSize(5.0);
            Paragraph p2 = new Paragraph().add(content2).setFirstLineIndent(2).setMargin(2.0).setMarginLeft(15.0).setMarginRight(15.0);
            ofdDoc.add(p2);

            String contentStr3 = "第三段：K*区区区位南Z数”((南Z*区划划，位别别K划S，”位BB区D判*结KZ划区区D”区有，别位)*S位，目别目别”位南(南B判，D位有B别南”有，区，，K南判别位*Z目区”)区区较较目”位”南K较较S区K别有判S别较，KZ判过划南过”S，区D区区区ZD*”数位过区)有南南目，*BZB”位)区区有过*判结位B南数S结)位南区别位划过()南S南过结K*别B区数南ZB”D区别)目结)(数结D，区位南D”D()划判区数判，S别别有Z南区(S别”结较SZB*位南较南有*目，*(”结K划”区目*位南南别”区，*B有D较较)数(位)”别有”别南划数较别结，南*划位划*区”目过数*数判Z别结别Z南区S”，目目，)K判S别位*过KD目判K较数判，*(位区数过过过别””区有*，较*，*位，区，，B*(区*别D有区位结过位结区划”(";
            Span content3 = new Span(contentStr3).setFontSize(5.0);
            Paragraph p3 = new Paragraph().add(content3).setFirstLineIndent(2).setMargin(2.0).setMarginLeft(15.0).setMarginRight(15.0);
            ofdDoc.add(p3);


            String contentStr4 = "第四段：K*区区区位南Z数”((南Z*区划划，位别别K划S，”位BB区D判*结KZ划区区D”区有，别位)*S位，目别目别”位南(南B判，D位有B别南”有，区，，K南判别位*Z目区”)区区较较目”位”南K较较S区K别有判S别较，KZ判过划南过”S，区D区区区ZD*”数位过区)有南南目，*BZB”位)区区有过*判结位B南数S结)位南区别位划过()南S南过结K*别B区数南ZB”D区别)目结)(数结D，区位南D”D()划判区数判，S别别有Z南区(S别”结较SZB*位南较南有*目，*(”结K划”区目*位南南别”区，*B有D较较)数(位)”别有”别南划数较别结，南*划位划*区”目过数*数判Z别结别Z南区S”，目目，)K判S别位*过KD目判K较数判，*(位区数过过过别””区有*，较*，*位，区，，B*(区*别D有区位结过位结区划”(";
            Span content4 = new Span(contentStr4).setFontSize(5.0);
            Paragraph p4 = new Paragraph().add(content4).setFirstLineIndent(2).setMargin(2.0).setMarginLeft(15.0).setMarginRight(15.0);
            ofdDoc.add(p4);
        }
        System.out.println("生成文档位置: " + srcP.toAbsolutePath());
    }
}