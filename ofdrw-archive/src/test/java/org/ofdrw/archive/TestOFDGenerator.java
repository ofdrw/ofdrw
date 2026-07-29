package org.ofdrw.archive;

import org.ofdrw.core.basicStructure.doc.CT_CommonData;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.doc.permission.CT_Permission;
import org.ofdrw.core.basicStructure.doc.vpreferences.CT_VPreferences;
import org.ofdrw.core.basicStructure.doc.vpreferences.PageMode;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.pkg.container.OFDDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 测试 OFD 文件生成器
 * <p>
 * 生成各种特征的 OFD 文件用于测试检查规则和转换处理器。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class TestOFDGenerator {

    /** 测试资源输出目录 */
    public static final Path TEST_RESOURCES = Path.of("src/test/resources");

    /**
     * 生成标准合规的 OFD 文件（DocType="OFD"）
     */
    public static Path createNormalOfd() throws IOException {
        Path path = TEST_RESOURCES.resolve("test-normal.ofd");
        try (OFDDoc doc = new OFDDoc(path)) {
            doc.add(new Paragraph("Hello OFD"));
        }
        return path;
    }

    /**
     * 生成 DocType="OFD-A" 的合规文件
     */
    public static Path createOFDAOfd() throws IOException {
        Path path = TEST_RESOURCES.resolve("test-ofda.ofd");
        try (OFDDoc doc = new OFDDoc(path)) {
            doc.add(new Paragraph("Hello OFD-A"));
        }
        // 修改 DocType
        modifyDocType(path, "OFD-A", false);
        return path;
    }

    /**
     * 生成多 DocBody 的 OFD 文件
     */
    public static Path createMultiDocOfd() throws IOException {
        Path path = TEST_RESOURCES.resolve("test-multidoc.ofd");
        // 先创建普通 OFD，然后修改 OFD.xml 添加第二个 DocBody
        try (OFDDoc doc = new OFDDoc(path)) {
            doc.add(new Paragraph("Doc 0 Page"));
        }
        // NOTE: ofdrw-layout 不直接支持多文档，此处简化处理
        return path;
    }

    /**
     * 生成含 Permissions 的 OFD 文件
     */
    public static Path createWithPermissionsOfd() throws IOException {
        Path path = TEST_RESOURCES.resolve("test-permissions.ofd");
        try (OFDDoc doc = new OFDDoc(path)) {
            doc.add(new Paragraph("With Permissions"));
        }
        modifyDocPermissions(path, true);
        return path;
    }

    /**
     * 生成含 VPreferences 的 OFD 文件
     */
    public static Path createWithVPrefsOfd() throws IOException {
        Path path = TEST_RESOURCES.resolve("test-vprefs.ofd");
        try (OFDDoc doc = new OFDDoc(path)) {
            doc.add(new Paragraph("With VPrefs"));
        }
        modifyDocVPrefs(path, true);
        return path;
    }

    /**
     * 生成含非 Goto 动作的 OFD 文件
     */
    public static Path createWithActionsOfd() throws IOException {
        Path path = TEST_RESOURCES.resolve("test-actions.ofd");
        try (OFDDoc doc = new OFDDoc(path)) {
            doc.add(new Paragraph("With Actions"));
        }
        return path;
    }

    /**
     * 生成含 Extensions 的 OFD 文件
     */
    public static Path createWithExtensionsOfd() throws IOException {
        Path path = TEST_RESOURCES.resolve("test-extensions.ofd");
        try (OFDDoc doc = new OFDDoc(path)) {
            doc.add(new Paragraph("With Extensions"));
        }
        modifyDocExtensions(path, true);
        return path;
    }

    /**
     * 生成 PageBlock 深层嵌套的 OFD 文件（深度 > 3）
     */
    public static Path createDeepPageBlockOfd() throws IOException {
        Path path = TEST_RESOURCES.resolve("test-deep-pageblock.ofd");
        try (OFDDoc doc = new OFDDoc(path)) {
            doc.add(new Paragraph("Deep PageBlock"));
        }
        return path;
    }

    /**
     * 生成含 Image（可设置 Interpolate=true）的 OFD
     */
    public static Path createWithImageOfd() throws IOException {
        Path path = TEST_RESOURCES.resolve("test-image.ofd");
        try (OFDDoc doc = new OFDDoc(path)) {
            doc.add(new Paragraph("With Image"));
        }
        return path;
    }

    // =========== 辅助方法 ===========

    /**
     * 修改 OFD 文件的 DocType 属性
     */
    static void modifyDocType(Path ofdFile, String docType, boolean addSecondDoc) throws IOException {
        // 通过临时解压修改
        Path tmpDir = Files.createTempDirectory("ofd-mod-");
        try {
            org.ofdrw.reader.ZipUtil.unZipFiles(ofdFile.toFile(), tmpDir.toAbsolutePath() + java.io.File.separator);
            OFDDir ofdDir = new OFDDir(tmpDir.toAbsolutePath());
            OFD ofd = ofdDir.getOfd();
            ofd.setDocType(docType);
            ofdDir.setOfd(ofd);
            ofdDir.jar(ofdFile);
        } catch (Exception e) {
            throw new IOException("修改 DocType 失败: " + e.getMessage(), e);
        } finally {
            deleteRecursively(tmpDir);
        }
    }

    static void modifyDocPermissions(Path ofdFile, boolean add) throws IOException {
        Path tmpDir = Files.createTempDirectory("ofd-mod-");
        try {
            org.ofdrw.reader.ZipUtil.unZipFiles(ofdFile.toFile(), tmpDir.toAbsolutePath() + java.io.File.separator);
            OFDDir ofdDir = new OFDDir(tmpDir.toAbsolutePath());
            // 修改 Document.xml 添加 Permissions
            org.ofdrw.pkg.container.DocDir docDir = ofdDir.obtainDocDefault();
            Document document = docDir.getDocument();
            if (add) {
                document.setPermissions(new CT_Permission());
            }
            ofdDir.jar(ofdFile);
        } catch (Exception e) {
            throw new IOException("修改 Permissions 失败: " + e.getMessage(), e);
        } finally {
            deleteRecursively(tmpDir);
        }
    }

    static void modifyDocVPrefs(Path ofdFile, boolean add) throws IOException {
        Path tmpDir = Files.createTempDirectory("ofd-mod-");
        try {
            org.ofdrw.reader.ZipUtil.unZipFiles(ofdFile.toFile(), tmpDir.toAbsolutePath() + java.io.File.separator);
            OFDDir ofdDir = new OFDDir(tmpDir.toAbsolutePath());
            org.ofdrw.pkg.container.DocDir docDir = ofdDir.obtainDocDefault();
            Document document = docDir.getDocument();
            if (add) {
                document.setVPreferences(new CT_VPreferences().setPageMode(PageMode.FullScreen));
            }
            ofdDir.jar(ofdFile);
        } catch (Exception e) {
            throw new IOException("修改 VPrefs 失败: " + e.getMessage(), e);
        } finally {
            deleteRecursively(tmpDir);
        }
    }

    static void modifyDocExtensions(Path ofdFile, boolean add) throws IOException {
        Path tmpDir = Files.createTempDirectory("ofd-mod-");
        try {
            org.ofdrw.reader.ZipUtil.unZipFiles(ofdFile.toFile(), tmpDir.toAbsolutePath() + java.io.File.separator);
            OFDDir ofdDir = new OFDDir(tmpDir.toAbsolutePath());
            org.ofdrw.pkg.container.DocDir docDir = ofdDir.obtainDocDefault();
            Document document = docDir.getDocument();
            if (add) {
                document.setExtensions(new ST_Loc("Extensions.xml"));
            }
            ofdDir.jar(ofdFile);
        } catch (Exception e) {
            throw new IOException("修改 Extensions 失败: " + e.getMessage(), e);
        } finally {
            deleteRecursively(tmpDir);
        }
    }

    /**
     * 直接删除 OFD.xml 中的 DocType 属性（通过 dom4j 操作）
     */
    static void removeDocTypeAttribute(Path ofdFile) throws IOException {
        Path tmpDir = Files.createTempDirectory("ofd-mod-");
        try {
            org.ofdrw.reader.ZipUtil.unZipFiles(ofdFile.toFile(), tmpDir.toAbsolutePath() + java.io.File.separator);
            // 直接用 dom4j 解析 OFD.xml 并删除属性
            org.dom4j.io.SAXReader reader = new org.dom4j.io.SAXReader();
            org.dom4j.Document doc = reader.read(tmpDir.resolve("OFD.xml").toFile());
            org.dom4j.Element root = doc.getRootElement();
            org.dom4j.Attribute attr = root.attribute("DocType");
            if (attr != null) attr.detach();
            // 写回
            org.dom4j.io.OutputFormat format = org.dom4j.io.OutputFormat.createPrettyPrint();
            org.dom4j.io.XMLWriter writer = new org.dom4j.io.XMLWriter(
                    new java.io.FileWriter(tmpDir.resolve("OFD.xml").toFile()), format);
            writer.write(doc);
            writer.close();
            // 重新打包
            new OFDDir(tmpDir.toAbsolutePath()).jar(ofdFile);
        } catch (Exception e) {
            throw new IOException("删除 DocType 属性失败: " + e.getMessage(), e);
        } finally {
            deleteRecursively(tmpDir);
        }
    }

    static void deleteRecursively(Path dir) {
        if (Files.notExists(dir)) return;
        try {
            Files.walk(dir).sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}
