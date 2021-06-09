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
import org.ofdrw.layout.edit.Attachment;
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
     * 向文件中加入附件文件
     *
     * @throws IOException
     */
    @Test
    void addAttachment() throws IOException {
        Path outP = Paths.get("target/AddAttachment.ofd");
        Path file = Paths.get("src/test/resources", "eg_tulip.jpg");
        Path file2 = Paths.get("src/test/resources", "NotoSerifCJKsc-Regular.otf");

        BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outP));
        try (OFDDoc ofdDoc = new OFDDoc(outputStream)) {
            Paragraph p = new Paragraph();
            Span span = new Span("这是一个带有附件的OFD文件").setFontSize(10d);
            p.add(span);
            ofdDoc.add(p);

            // 加入附件文件
            ofdDoc.addAttachment(new Attachment("Gao", file));
            ofdDoc.addAttachment(new Attachment("FontFile", file2));
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 向文件已经存在附件的文档中加入附件文件
     *
     * （附件文件路径使用的是相对路径）
     *
     * @throws IOException 文档操作异常
     */
    @Test
    void addAttachment4ExistOFD() throws IOException {
        Path outP = Paths.get("target/AddAttachment_relativepathversion_out.ofd");
        Path inP = Paths.get("src/test/resources/AddAttachment_relativepathversion.ofd");
        Path file = Paths.get("src/test/resources", "testimg.png");
        Path file2 = Paths.get("src/test/resources", "eg_tulip.jpg");
        BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(inP));
        BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outP));
        try (OFDReader reader = new OFDReader(inputStream);
             OFDDoc ofdDoc = new OFDDoc(reader, outputStream)) {
            // 加入附件文件
            ofdDoc.addAttachment(new Attachment("testimg", file));
            ofdDoc.addAttachment(new Attachment("eg_tulip", file2));
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 替换附件文件
     *
     * @throws IOException
     */
    @Test
    void replaceAttachment() throws IOException {
        Path srcP = Paths.get("src/test/resources/AddAttachment.ofd");
        Path outP = Paths.get("target/ReplaceAttachment.ofd");
        Path file = Paths.get("src/test/resources", "ASCII字体宽度测量.html");

        BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(srcP));
        BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outP));

        try (OFDReader reader = new OFDReader(inputStream);
             OFDDoc ofdDoc = new OFDDoc(reader, outputStream)) {
            // 加入附件文件
            ofdDoc.addAttachment(new Attachment("Gao", file));
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 加入印章类型注释对象
     *
     * @throws IOException
     * @throws DocumentException
     */
    @Test
    void addAnnotationStamp() throws IOException, DocumentException {
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
     * @throws IOException
     * @throws DocumentException
     */
    @Test
    void addAnnotation() throws IOException {
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

}