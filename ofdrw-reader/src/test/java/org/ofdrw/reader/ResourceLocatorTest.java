package org.ofdrw.reader;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-04-08 21:48:36
 */
class ResourceLocatorTest {
    private Path src = Paths.get("src/test/resources/helloworld.ofd");
    @Test
    public void toAbsolutePath() throws IOException {
        try (OFDReader reader = new OFDReader(src)) {
            OFDDir ofdDir = reader.getOFDDir();
            ResourceLocator rl = new ResourceLocator(ofdDir);
            rl.cd("/Doc_0/Pages/Page_0");

            String absPath = rl.toAbsolutePath("../../Signs/Signatures.xml");
            System.out.println(absPath);
        }
    }

    @Test
    void testCd() throws IOException {
        try (OFDReader reader = new OFDReader(src)) {
            OFDDir ofdDir = reader.getOFDDir();

            ResourceLocator rl = new ResourceLocator(ofdDir);
            rl.cd("/Doc_0/Pages/");
            assertEquals("/Doc_0/Pages", rl.pwd());

            rl.restWd();
            assertEquals("/", rl.pwd());

            rl.cd("../");
            assertEquals("/", rl.pwd());

            rl.cd("./Doc_0/../");
            assertEquals("/", rl.pwd());

            rl.cd("Doc_0/Pages/Page_0");
            assertEquals("/Doc_0/Pages/Page_0", rl.pwd());
        }
    }

    @Test
    void matchTest() {
        String p = "/Doc_0";
        assertTrue(ResourceLocator.PtDoc.matcher(p).matches());
        p = "/Doc_0/";
        assertFalse(ResourceLocator.PtDoc.matcher(p).matches());
        p = "/Doc_0/Signs";
        assertTrue(ResourceLocator.PtSigns.matcher(p).matches());
        p = "/Doc_0/Signs/Sign_9";
        assertTrue(ResourceLocator.PtSign.matcher(p).matches());
        p = "/Doc_0/Pages";
        assertTrue(ResourceLocator.PtPages.matcher(p).matches());
        p = "/Doc_0/Pages/Page_11";
        assertTrue(ResourceLocator.PtPage.matcher(p).matches());
        p = "/Doc_0/Pages/Page_09/Res";
        assertTrue(ResourceLocator.PtPageRes.matcher(p).matches());
        p = "/Doc_0/Res";
        assertTrue(ResourceLocator.PtDocRes.matcher(p).matches());
        p = "/Doc_0/Pages/Page_09/Res";
        Matcher m = ResourceLocator.PtPageRes.matcher(p);
        m.find();
        assertEquals("Doc_0", m.group(1));
        assertEquals("Page_09", m.group(2));
    }

    @Test
    void get() throws IOException, DocumentException {
        assertThrows(FileNotFoundException.class, () -> {
            try (OFDReader reader = new OFDReader(src)) {
                OFDDir ofdDir = reader.getOFDDir();
                ResourceLocator rl = new ResourceLocator(ofdDir);
                OFD ofd = rl.get(ST_Loc.getInstance("Doc_0/OFD.xml"), OFD::new);
            }
        });
        try (OFDReader reader = new OFDReader(src)) {
            OFDDir ofdDir = reader.getOFDDir();
            ResourceLocator rl = new ResourceLocator(ofdDir);
            OFD ofd = rl.get("OFD.xml", OFD::new);
            assertEquals("6b9c7c83cff048e7b427ef0567f3e065", ofd.getDocBody().getDocInfo().getDocID());
            // 检查缓存是否生效
            assertEquals(ofd.getProxy(), ofdDir.getOfd().getProxy());
            Page page = rl.get("/Doc_0/Pages/Page_0/Content.xml", Page::new);
            assertEquals(page.getProxy(),
                    ofdDir.getDocDir("Doc_0")
                            .getPages()
                            .getByIndex(0)
                            .getContent()
                            .getProxy());
        }
    }

    @Test
    void getFile() {
    }
}