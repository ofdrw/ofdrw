package org.ofdrw.reader;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;

/**
 * ZIP 文件解压工具
 */
public class ZipUtil {
    /**
     * 设置 解压许可最大字节数
     *
     * @deprecated 采用apache compress 默认策略
     * @param size 压缩文件解压最大大小,默认值： 100M
     */
    @Deprecated
    public static void setMaxSize(long size) {
    }

    /**
     * 解压到指定目录
     *
     * @param zipPath 需要解压的文件路径
     * @param descDir 解压到目录
     * @throws IOException 文件操作IO异常
     */
    public static void unZipFiles(String zipPath, String descDir) throws IOException {
        unZipFiles(new File(zipPath), descDir);
    }

    /**
     * 解压文件到指定目录
     *
     * @param src     压缩文件流
     * @param descDir 解压到目录
     * @throws IOException 文件操作IO异常
     */
    public static void unZipFiles(InputStream src, String descDir) throws IOException {
        unZipFileByApacheCommonCompress(src, descDir);
    }

    /**
     * 解压文件到指定目录
     *
     * @param zipFile 需要解压的文件
     * @param descDir 解压到目录
     * @throws IOException 文件操作IO异常
     */
    public static void unZipFiles(File zipFile, String descDir) throws IOException {
        unZipFileByApacheCommonCompress(zipFile, descDir);
    }

    /**
     * 校验文件路径是否在期望的文件目录下
     *
     * @param targetDir 期望解压目录
     * @param filePath  文件路径
     * @throws IOException 文件操作IO异常
     */
    private static void pathValid(String targetDir, String filePath) throws IOException {
        if (!filePath.startsWith(targetDir))
            throw new IOException(String.format("不合法的路径：%s", filePath));
    }

    /**
     * 使用apache common compress库 解压zipFile，能支持更多zip包解压的特性
     *
     * @param srcFile 带解压的源文件
     * @param descDir 解压到目录
     * @throws IOException IO异常
     */
    public static void unZipFileByApacheCommonCompress(File srcFile, String descDir) throws IOException {
        if (srcFile == null || srcFile.exists() == false) {
            throw new IOException("解压文件不存在: " + srcFile);
        }
        try (FileInputStream fin = new FileInputStream(srcFile)) {
            unZipFileByApacheCommonCompress(fin, descDir);
        }
    }

    /**
     * apache common compress库 解压zipFile
     *
     * @param src     带解压的源文件流
     * @param descDir 解压到目录
     * @throws IOException IO异常
     */
    public static void unZipFileByApacheCommonCompress(InputStream src, String descDir) throws IOException {
        File pathFile = new File(descDir).getCanonicalFile();
        if (!pathFile.exists() && !pathFile.mkdirs()) {
            throw new IOException("解压目录创建失败: " + pathFile);
        }
        try (ZipArchiveInputStream zipFile = new ZipArchiveInputStream(src)) {
            ArchiveEntry entry = null;
            while ((entry = zipFile.getNextEntry()) != null) {
                File f = new File(pathFile, entry.getName()).getCanonicalFile();

                //校验路径合法性
                pathValid(pathFile.getAbsolutePath(), f.getAbsolutePath());

                if (entry.isDirectory()) {
                    if (!f.isDirectory() && !f.mkdirs()) {
                        throw new IOException("failed to create directory " + f);
                    }
                } else {
                    File parent = f.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("failed to create directory " + parent);
                    }
                    try (OutputStream o = Files.newOutputStream(f.toPath())) {
                        IOUtils.copy(zipFile, o);
                    }
                }
            }
        }
    }
}
