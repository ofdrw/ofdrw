package org.ofdrw.pkg.container;

import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.pkg.tool.ElemCup;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @author 权观宇
 * @since 2020-04-02 20:00:50
 */
class VirtualContainerTest {

    /**
     * 测试虚拟容器
     */
    private VirtualContainer vc;

    final static String target = "target/VirtualContainer";

    @BeforeEach
    public void init() throws IOException {
        Path path = Paths.get(target);
        if (Files.exists(path)) {
            FileUtils.deleteDirectory(path.toFile());
        } else {
            path = Files.createDirectories(path);
        }
        vc = new VirtualContainer(path);
    }

    @Test
    void putFile() throws IOException {
        final String fileName = "testimg.png";
        Path path = Paths.get("src/test/resources", fileName);
        vc.putFile(path);
        Assertions.assertTrue(Files.exists(Paths.get(target, fileName)));

        // 重复放置相同文件
        vc.flush();
        Path fileCopy = Paths.get("target", fileName);
        Files.delete(fileCopy);
        Files.copy(path, fileCopy);
        vc.putFile(fileCopy);

        // 同名不同内容文件
        Files.delete(fileCopy);
        Files.copy(Paths.get("src/test/resources/StampImg.png"), fileCopy);
        Assertions.assertThrows(FileAlreadyExistsException.class, () -> vc.putFile(fileCopy));
    }

    @Test
    void getFile() throws IOException {
        final String fileName = "testimg.png";
        Path path = Paths.get("src/test/resources", fileName);
        vc.putFile(path);

        Path file = vc.getFile(fileName);
        final byte[] bytes = Files.readAllBytes(file);
        final byte[] bytes1 = Files.readAllBytes(path);
        Assertions.assertArrayEquals(bytes, bytes1);
    }

    @Test
    void putObj() throws DocumentException, IOException {
        String fileName = "Content.xml";
        Path path = Paths.get("src/test/resources", fileName);
        Element inject = ElemCup.inject(path);
        inject.add(OFDElement.getInstance("TestEmptyElem"));
        vc.putObj("C.xml", inject);
        vc.flush();
        System.out.println(vc.getSysAbsPath());
        Assertions.assertTrue(Files.exists(Paths.get(target, "C.xml")));

    }

    @Test
    void getObj() throws IOException, DocumentException {
        String fileName = "Content.xml";
        Path path = Paths.get("src/test/resources", fileName);
        vc.putFile(path);
        Element obj = vc.getObj(fileName);
        Assertions.assertEquals("ofd:Page", obj.getQualifiedName());
    }

    @Test
    void getContainerName() {
        Assertions.assertEquals("VirtualContainer", vc.getContainerName());
    }

    @Test
    void obtainContainer() throws IOException {
        // 创建一个容器
        VirtualContainer pages = vc.obtainContainer("Pages", VirtualContainer::new);
        Assertions.assertTrue(Files.exists(Paths.get(target, "Pages")));

        Path path = Paths.get(target);
        VirtualContainer vc2 = new VirtualContainer(path);
        VirtualContainer pages1 = vc2.getContainer("Pages", VirtualContainer::new);
        Assertions.assertNotNull(pages1);
    }

    @Test
    void getAbsLoc() {
        Assertions.assertEquals("/", vc.getAbsLoc().toString());

        PagesDir pages = vc.obtainContainer("Pages", PagesDir::new);
        Assertions.assertEquals("/Pages", pages.getAbsLoc().toString());

        PageDir pageDir = pages.newPageDir();
        Assertions.assertEquals("/Pages/Page_0", pageDir.getAbsLoc().toString());
    }


    /**
     * 测试文件在读取后没有改动，是否会影响文档中文件
     * @throws IOException no happen
     * @throws DocumentException  no happen
     */
    @Test
    void testReadFileNoChange() throws IOException, DocumentException {
        Path docPath = Paths.get("src/test/resources/Document.xml");
        VirtualContainer doc_0 = vc.obtainContainer("Doc_0", VirtualContainer::new);
        doc_0.putFile(docPath);
        vc.close();

        VirtualContainer newVc = new VirtualContainer(Paths.get(target));
        DocDir docDir0 = newVc.obtainContainer("Doc_0", DocDir::new);
        Document document = docDir0.getDocument();
        System.out.println(document.elements().size());
        newVc.close();

        Path vcDocPath = Paths.get(target + "/Doc_0/Document.xml");
        byte[] before = Files.readAllBytes(docPath);
        byte[] after = Files.readAllBytes(vcDocPath);

        Assertions.assertTrue(Arrays.equals(before, after));

    }
}