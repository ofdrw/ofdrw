package org.ofdrw.reader;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.pkg.container.ResDir;
import org.ofdrw.pkg.container.VirtualContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-04-07 19:32:19
 */
class OFDReaderTest {

    private Path src = Paths.get("src/test/resources/helloworld.ofd");

    @Test
    void testChineseDirName() throws IOException {
        Path src = Paths.get("src/test/resources/chineseDirName.ofd");
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
            OFDReader reader = new OFDReader(null);
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
            assertEquals("6b9c7c83cff048e7b427ef0567f3e065", docID);
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

    /**
     * 获取ofd文本节点
     *
     * @throws IOException 文件解析异常
     */
    @Test
    void getContentText() throws IOException {
        try (OFDReader reader = new OFDReader(src)) {
            List<String> contents = new ArrayList<>();
            int numberOfPages = reader.getNumberOfPages();
            for (int i = 1; i <= numberOfPages; i++) {
                List<CT_Layer> layers = reader.getPage(i).getContent().getLayers();
                for (CT_Layer layer : layers) {
                    List<PageBlockType> pageBlocks = layer.getPageBlocks();
                    for (PageBlockType block : pageBlocks) {
                        if (block instanceof TextObject) {
                            TextObject text = (TextObject) block;
                            List<TextCode> textCodes = text.getTextCodes();
                            for (TextCode code : textCodes) {
                                contents.add(code.getContent());
                            }
                        }
                    }
                }
            }
            System.out.println(">> " + Arrays.toString(contents.toArray()));
        }
    }
}