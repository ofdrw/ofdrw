package org.ofdrw.reader;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.pkg.container.ResDir;
import org.ofdrw.pkg.container.VirtualContainer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;


import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-04-07 19:32:19
 */
class OFDReaderTest {

    private Path src = Paths.get("src/test/resources/helloworld.ofd");

    @Test
    void testChineseDirName() throws IOException {
        Path src = Paths.get("src/test/resources/chineseDir_windows.ofd");
        try (OFDReader reader = new OFDReader(src);) {
            System.out.println(reader.getWorkDir().toAbsolutePath());
            OFDDir ofdDir = reader.getOFDDir();
            DocDir docDir = ofdDir.obtainDocDefault();
            ResDir res = docDir.getRes();
            VirtualContainer chineseDirName = res.getContainer("这是一个中文目录", VirtualContainer::new);
            assertNotNull(chineseDirName);

            Path file = chineseDirName.getFile("数据文件.txt");
            byte[] contentBin = Files.readAllBytes(file);
            final String content = new String(contentBin);
            System.out.println(content);
            assertEquals("文件中有一些中文", content);
        }
    }

    /**
     * 获取附件对象
     *
     * @throws IOException
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


    @Test
    void oFDReader() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            OFDReader reader = new OFDReader(Paths.get("target/null.ofd"));
        });
        OFDReader reader = new OFDReader(src);
        Path workDir = reader.getWorkDir();
        System.out.println(workDir.toAbsolutePath().toString());
        assertTrue(Files.exists(workDir));
    }

    @Test
    void close() throws IOException {
        Path p;
        try (OFDReader reader = new OFDReader(src)) {
            p = reader.getWorkDir();
            System.out.println(">> " + p.toAbsolutePath().toString());
        }
        assertTrue(Files.notExists(p));
    }

    @Test
    void getOFDDir() throws IOException, DocumentException {
        try (OFDReader reader = new OFDReader(src)) {
            OFDDir ofdDir = reader.getOFDDir();
            OFD ofd = ofdDir.getOfd();
            String docID = ofd.getDocBody().getDocInfo().getDocID();
            assertEquals("220c5913ebfe4f6e8070dabd3647f157", docID);
        }
    }


    @Test
    void getPage() throws IOException {
        try (OFDReader reader = new OFDReader(src)) {
            OFDDir ofdDir = reader.getOFDDir();
            Page page = reader.getPage(1);
            assertEquals(1, page.getContent().getLayers().size());
        }
    }


    @Test
    public void testReader() throws Exception{
        Path src = Paths.get("src/test/resources/helloworld.ofd");
        byte[] ofdSrc = Files.readAllBytes(src);
        Path fileTemp = Files.createTempDirectory("ofd_tmp");

        String tempDirBuilder = fileTemp.toFile().getAbsolutePath() +
                File.separator +
                fileTemp.toFile().getName() +
                "_src.OFD";
        Path srcPath = Paths.get(tempDirBuilder);
        Files.write(srcPath, ofdSrc);
        OFDReader reader = new OFDReader(srcPath);
        //reader.close();

        Files.delete(srcPath);
        Files.delete(fileTemp);
    }


    /**
     * 低层次的文档操作
     *
     * @throws IOException       IO异常
     * @throws DocumentException 文档解析异常
     */
    @Test
    void lowLevelOp() throws IOException, DocumentException {
        Path out = Paths.get("target/EditTitleAuthor.ofd");
        try (OFDReader reader = new OFDReader(src)) {
            OFDDir ofdDir = reader.getOFDDir();
            OFD ofd = ofdDir.getOfd();
            DocBody docBody = ofd.getDocBody();
            CT_DocInfo docInfo = docBody.getDocInfo();
            System.out.println(">> 文档标题：[" + docInfo.getTile() + "] -> [Hello World]");
            System.out.println(">> 文档作者：[" + docInfo.getAuthor() + "] -> [权观宇]");
            docInfo.setTile("Hello World");
            docInfo.setAuthor("权观宇");
            docInfo.setModDate(LocalDate.now());
            // 重新打包为OFD文档
            ofdDir.jar(out);
        }
        System.out.println(">> 文档生成位置：" + out.toAbsolutePath().toString());

        // 验证
        try (OFDReader reader = new OFDReader(out)) {
            OFDDir ofdDir = reader.getOFDDir();
            OFD ofd = ofdDir.getOfd();
            DocBody docBody = ofd.getDocBody();
            CT_DocInfo docInfo = docBody.getDocInfo();
            assertEquals(docInfo.getTile(), "Hello World");
            assertEquals(docInfo.getAuthor(), "权观宇");
        }
    }
}