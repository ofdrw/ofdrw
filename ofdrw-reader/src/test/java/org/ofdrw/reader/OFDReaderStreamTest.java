package org.ofdrw.reader;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.pkg.container.ResDir;
import org.ofdrw.pkg.container.VirtualContainer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OFDReader测试
 *
 * @author minghu.zhang
 * @since 2020-11-19 19:32:19
 */
class OFDReaderStreamTest {

    private Path src = Paths.get("src/test/resources/helloworld.ofd");

    @Test
    void testChineseDirName() throws IOException {
        Path src = Paths.get("src/test/resources/chineseDir_windows.ofd");
        try (OFDReader reader = new OFDReader(new ByteArrayInputStream(Files.readAllBytes(src)))) {
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

        try (OFDReader reader = new OFDReader(new ByteArrayInputStream(Files.readAllBytes(src)))) {
            CT_Attachment attachment = reader.getAttachment("AAABBB");
            Assertions.assertNull(attachment);

            Path file = reader.getAttachmentFile("AAABBB");
            Assertions.assertNull(file);

            file = reader.getAttachmentFile("Gao");
            Assertions.assertTrue(Files.exists(file));

        }

    }

    @Test
    void close() throws IOException {
        Path p;
        try (OFDReader reader = new OFDReader(new ByteArrayInputStream(Files.readAllBytes(src)))) {
            p = reader.getWorkDir();
            System.out.println(">> " + p.toAbsolutePath().toString());
        }
        assertTrue(Files.notExists(p));
    }

    @Test
    void getOFDDir() throws IOException, DocumentException {
        try (OFDReader reader = new OFDReader(new ByteArrayInputStream(Files.readAllBytes(src)))) {
            OFDDir ofdDir = reader.getOFDDir();
            OFD ofd = ofdDir.getOfd();
            String docID = ofd.getDocBody().getDocInfo().getDocID();
            assertEquals("220c5913ebfe4f6e8070dabd3647f157", docID);
        }
    }


    @Test
    void getPage() throws IOException {
        try (OFDReader reader = new OFDReader(new ByteArrayInputStream(Files.readAllBytes(src)))) {
            OFDDir ofdDir = reader.getOFDDir();
            Page page = reader.getPage(1);
            assertEquals(1, page.getContent().getLayers().size());
        }
    }

}