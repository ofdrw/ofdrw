package org.ofdrw.reader;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.pageObj.Page;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 带自解压（SFX）前缀的 ZIP 压缩包解压测试
 * <p>
 * 测试资源 sfx_prefix_manual.ofd 由 ofd_pdf_match/manual.ofd 裁剪而来（仅保留 OFD.xml、Document.xml、
 * DocumentRes.xml 与第 1 页内容），并在 ZIP 数据前拼接一段自解压桩：
 * 桩内容以空 EOCD 记录（PK\005\006）开头，真正的本地文件头 PK\003\004 位于偏移 77 处。
 * 若按流式顺序读取，会在开头的空 EOCD 处立即结束，一个条目都读不到。
 */
class ZipSfxPrefixTest {

    /**
     * 带 SFX 前缀的测试样本（ZIP 数据起始偏移为 77 字节）
     */
    private static final Path SFX_OFD = Paths.get("src/test/resources/sfx_prefix_manual.ofd");

    /**
     * 普通 OFD（以 PK\003\004 开头）
     */
    private static final Path NORMAL_OFD = Paths.get("src/test/resources/helloworld.ofd");

    @Test
    void shouldUnzipAllEntriesWhenZipHasSfxPrefix() throws IOException {
        Path workDir = Files.createTempDirectory("sfx_unzip_test");
        try {
            ZipUtil.unZipFileByApacheCommonCompress(SFX_OFD.toFile(), workDir.toString() + File.separator);

            assertTrue(Files.exists(workDir.resolve("OFD.xml")), "应解压出 OFD.xml");
            assertTrue(Files.exists(workDir.resolve("Doc_0/Document.xml")), "应解压出 Document.xml");
            assertTrue(Files.exists(workDir.resolve("Doc_0/Pages/Page_0/Content.xml")), "应解压出第 1 页内容");
            assertTrue(Files.exists(workDir.resolve("Doc_0/DocumentRes.xml")), "应解压出文档资源");
            assertTrue(Files.exists(workDir.resolve("Doc_0/Res/eac0ffe1d91b4023bce9de2a9ae81aac.png")), "应解压出页图像");
        } finally {
            deleteDir(workDir);
        }
    }

    @Test
    void shouldUnzipAllEntriesWhenStreamHasSfxPrefix() throws IOException {
        Path workDir = Files.createTempDirectory("sfx_unzip_stream_test");
        try {
            // 非文件流：一次性读取并定位本地文件头
            try (InputStream in = new BufferedInputStream(new FileInputStream(SFX_OFD.toFile()))) {
                ZipUtil.unZipFileByApacheCommonCompress(in, workDir.toString() + File.separator);
            }
            assertTrue(Files.exists(workDir.resolve("OFD.xml")), "非文件流应解压出 OFD.xml");
            assertTrue(Files.exists(workDir.resolve("Doc_0/Document.xml")), "非文件流应解压出 Document.xml");
        } finally {
            deleteDir(workDir);
        }
    }

    @Test
    void shouldUnzipAllEntriesWhenFileStreamHasSfxPrefix() throws IOException {
        Path workDir = Files.createTempDirectory("sfx_unzip_fis_test");
        try {
            // 文件流：通过通道随机访问定位前缀偏移
            try (InputStream in = new FileInputStream(SFX_OFD.toFile())) {
                ZipUtil.unZipFileByApacheCommonCompress(in, workDir.toString() + File.separator);
            }
            assertTrue(Files.exists(workDir.resolve("OFD.xml")), "文件流应解压出 OFD.xml");
            assertTrue(Files.exists(workDir.resolve("Doc_0/Document.xml")), "文件流应解压出 Document.xml");
        } finally {
            deleteDir(workDir);
        }
    }

    @Test
    void shouldOpenOfdWithSfxPrefix() throws Exception {
        try (OFDReader reader = new OFDReader(SFX_OFD)) {
            assertEquals(1, reader.getNumberOfPages(), "SFX 前缀的 OFD 页数应为 1");
            // getPage 页码从 1 起
            final Page page = reader.getPage(1);
            assertNotNull(page, "第 1 页应可解析");
            final Document document = reader.cdDefaultDoc();
            assertNotNull(document, "默认文档应可解析");
            assertFalse(document.getPages().getPages().isEmpty(), "默认文档应包含页面");
        }
    }

    @Test
    void shouldOpenOfdWithSfxPrefixFromStream() throws IOException {
        try (InputStream in = new ByteArrayInputStream(Files.readAllBytes(SFX_OFD));
             OFDReader reader = new OFDReader(in)) {
            assertEquals(1, reader.getNumberOfPages(), "SFX 前缀的 OFD 输入流页数应为 1");
        }
    }

    @Test
    void shouldKeepUnzipResultForNormalZip() throws IOException {
        // 回归：普通 ZIP（PK\003\004 开头）沿用原有流式解压结果
        Path workDir = Files.createTempDirectory("normal_zip_test");
        try {
            ZipUtil.unZipFileByApacheCommonCompress(NORMAL_OFD.toFile(), workDir.toString() + File.separator);
            final List<String> entries = listRelativeFiles(workDir);
            assertFalse(entries.isEmpty(), "普通 OFD 应解压出条目");
            assertTrue(entries.contains("OFD.xml"), "普通 OFD 应解压出 OFD.xml");

            // 与系统标准解压结果逐条比对，确保普通 ZIP 的解压结果未发生变化
            final Path expectedDir = Files.createTempDirectory("normal_zip_expected");
            try {
                unzipWithJavaZip(NORMAL_OFD, expectedDir);
                final List<String> expected = listRelativeFiles(expectedDir);
                expected.sort(Comparator.naturalOrder());
                entries.sort(Comparator.naturalOrder());
                assertEquals(expected, entries, "普通 ZIP 的解压条目应与标准解压一致");
            } finally {
                deleteDir(expectedDir);
            }
        } finally {
            deleteDir(workDir);
        }
    }

    /**
     * 使用 JDK 标准 ZipFile 解压（作为普通 ZIP 解压结果的参照实现）
     *
     * @param zipFile ZIP 文件
     * @param destDir 解压目标目录
     * @throws IOException IO异常
     */
    private static void unzipWithJavaZip(Path zipFile, Path destDir) throws IOException {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(zipFile.toFile())) {
            final List<? extends java.util.zip.ZipEntry> entries = java.util.Collections.list(zip.entries());
            for (java.util.zip.ZipEntry entry : entries) {
                final Path target = destDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                Files.createDirectories(target.getParent());
                try (InputStream in = zip.getInputStream(entry);
                     OutputStream out = new FileOutputStream(target.toFile())) {
                    org.apache.commons.io.IOUtils.copy(in, out);
                }
            }
        }
    }

    /**
     * 列出目录下所有文件的相对路径（统一使用 / 分隔）
     *
     * @param dir 目录
     * @return 相对路径列表
     * @throws IOException IO异常
     */
    private static List<String> listRelativeFiles(Path dir) throws IOException {
        final List<String> result = new ArrayList<>();
        Files.walk(dir).filter(Files::isRegularFile)
                .forEach(p -> result.add(dir.relativize(p).toString().replace(File.separatorChar, '/')));
        return result;
    }

    /**
     * 递归删除目录
     *
     * @param dir 目录
     * @throws IOException IO异常
     */
    private static void deleteDir(Path dir) throws IOException {
        if (Files.exists(dir)) {
            FileUtils.forceDelete(dir.toFile());
        }
    }
}
