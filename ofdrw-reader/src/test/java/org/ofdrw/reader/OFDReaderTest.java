package org.ofdrw.reader;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.OFDDir;

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
            System.out.println(">> 文档标题：[" + docInfo.getTile() +"] -> [Hello World]");
            System.out.println(">> 文档作者：[" + docInfo.getAuthor()+"] -> [权观宇]" );
            docInfo.setTile("Hello World");
            docInfo.setAuthor("权观宇");
            docInfo.setModDate(LocalDate.now());
            // 重新打包为OFD文档
            ofdDir.jar(out);
        }
        System.out.println(">> 文档生成位置："+ out.toAbsolutePath().toString());

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