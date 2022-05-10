package org.ofdrw.reader;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipUtil {
    /**
     * 解压许可最大字节数，为了防止 ZIP炸弹攻击
     * <p>
     * 默认值： 100M
     */
    private static long MaxSize = 100 * 1024 * 1024;


    /**
     * 设置 解压许可最大字节数
     *
     * @param size 压缩文件解压最大大小,默认值： 100M
     */
    public static void setMaxSize(long size) {
        if (size <= 0) {
            size = 100 * 1024 * 1024;
        }
        MaxSize = size;
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
        File pathFile = new File(descDir).getCanonicalFile();
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        }

        int countByteNumber = 0;

        // 解决zip文件中有中文目录或者中文文件
        ZipInputStream zip = new ZipInputStream(src, Charset.forName("GBK"));
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName();

            File file = new File(pathFile, name).getCanonicalFile();

            //校验路径合法性
            pathValid(pathFile.getAbsolutePath(), file.getAbsolutePath());

            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                File dir = file.getParentFile();
                if (!dir.exists()) {
                    dir.mkdirs();
                }


                byte[] buf = new byte[1024];
                int num;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                while ((num = zip.read(buf, 0, buf.length)) != -1) {

                    //写入字节数超出限制则抛出异常
                    if (countByteNumber + num > MaxSize) {
                        throw new IOException(String.format("写入数据超出ZIP解压最大字节数(%s)限制！", MaxSize));
                    }

                    bos.write(buf, 0, num);

                    countByteNumber += num;
                }
                Files.write(Paths.get(file.getAbsolutePath()), bos.toByteArray());
            }
        }
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
     * @param srcFile 带解压的源文件
     * @param descDir 解压到目录
     * @throws IOException IO异常
     */
    public static void unZipFileByApacheCommonCompress(File srcFile, String descDir) throws IOException {
        File pathFile = new File(descDir).getCanonicalFile();
        if (!pathFile.exists() && !pathFile.mkdirs()) {
            throw new IOException("解压目录创建失败: " + pathFile);
        }
        try (ZipFile zipFile = new ZipFile(srcFile)) {
            ZipEntry entry = null;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
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
                        IOUtils.copy(zipFile.getInputStream(entry), o);
                    }
                }
            }
        }
    }
}
