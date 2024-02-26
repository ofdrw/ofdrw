package org.ofdrw.layout;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.layout.edit.Attachment;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.layout.element.Span;
import org.ofdrw.reader.OFDReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class AttachmentTest {

    /**
     * 替换附件文件
     */
    @Test
    void replaceAttachmentTest() throws IOException {
        Path srcP = Paths.get("src/test/resources/AddAttachment.ofd");
        Path outP = Paths.get("target/ReplaceAttachment.ofd");
        Path file = Paths.get("src/test/resources", "ASCII字体宽度测量.html");

        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            // 加入附件文件
            ofdDoc.addAttachment(new Attachment("Gao", file));
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 向文件指定位置中加入附件文件
     */
    @Test
    void addAttachmentToDirTest() throws IOException {
        Path outP = Paths.get("target/AddAttachment_specif_ABS.ofd");
        Path file = Paths.get("src/test/resources", "eg_tulip.jpg");

        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            Paragraph p = new Paragraph();
            Span span = new Span("这是一个带有附件的OFD文件").setFontSize(10d);
            p.add(span);
            ofdDoc.add(p);

            // 加入附件文件
            ofdDoc.addAttachment("/Doc_0/MY_DIR/MY_PATH/",new Attachment("Gao", file));
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }


    /**
     * 向文件中加入附件文件
     */
    @Test
    void addAttachmentTest() throws IOException {
        Path outP = Paths.get("target/AddAttachment.ofd");
        Path file = Paths.get("src/test/resources", "eg_tulip.jpg");
        Path file2 = Paths.get("src/test/resources", "NotoSerifCJKsc-Regular.otf");

        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
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
     * 向文件中加入附件文件
     */
    @Test
    void addAttachment() throws IOException {
        Path outP = Paths.get("target/AddAttachment.ofd");
        Path file = Paths.get("src/test/resources", "eg_tulip.jpg");
        Path file2 = Paths.get("src/test/resources", "NotoSerifCJKsc-Regular.otf");

        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
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
     */
    @Test
    void addAttachment4ExistOFD() throws IOException {
        Path outP = Paths.get("target/AddAttachment_relativepathversion_out.ofd");
        Path inP = Paths.get("src/test/resources/AddAttachment_relativepathversion.ofd");
        Path file = Paths.get("src/test/resources", "testimg.png");
        Path file2 = Paths.get("src/test/resources", "eg_tulip.jpg");
        try (OFDReader reader = new OFDReader(inP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            // 加入附件文件
            ofdDoc.addAttachment(new Attachment("testimg", file));
            ofdDoc.addAttachment(new Attachment("eg_tulip", file2));
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 替换附件文件
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
     * 获取所有附件列表
     */
    @Test
    void getAttachmentList() throws IOException{
        Path srcP = Paths.get("src/test/resources/AddAttachment.ofd");
        try (OFDReader reader = new OFDReader(srcP)) {
            List<CT_Attachment> attachmentList = reader.getAttachmentList();
            for (CT_Attachment ctAttachment : attachmentList) {
                String name = ctAttachment.getAttachmentName();
                Path file = reader.getAttachmentFile(name);
                System.out.println(">> Attachment file name: " + name + " size: " + Files.size(file) + "B");
            }
        }
    }

    /**
     * 获取附件对象
     */
    @Test
    void getAttachment() throws IOException {
        Path src = Paths.get("src/test/resources/AddAttachment.ofd");
        try (OFDReader reader = new OFDReader(src)) {
            CT_Attachment attachment = reader.getAttachment("AAABBB");
            Assertions.assertNull(attachment);

            Path file = reader.getAttachmentFile("AAABBB");
            Assertions.assertNull(file);

            file = reader.getAttachmentFile("Gao");
            Assertions.assertTrue(Files.exists(file));
        }
    }

    /**
     * 删除附件
     */
    @Test
    void deleteAttachment()throws Exception {
        Path srcP = Paths.get("src/test/resources/AddAttachment.ofd");
        Path outP = Paths.get("target/DeleteAttachment.ofd");

        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            // 加入附件文件
            ofdDoc.deleteAttachment("Gao");
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath());
    }

}
