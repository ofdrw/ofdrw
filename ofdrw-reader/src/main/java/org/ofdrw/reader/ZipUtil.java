package org.ofdrw.reader;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipUtil {
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
     * @param descDir 解压到目录
     * @throws IOException 文件操作IO异常
     */
    @SuppressWarnings("rawtypes")
    public static void unZipFiles(InputStream src, String descDir) throws IOException {
        File pathFile = new File(descDir);
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        }
        // 解决zip文件中有中文目录或者中文文件
        ZipInputStream zip = new ZipInputStream(src, Charset.forName("GBK"));
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName();

            byte[] buf = new byte[1024];
            int num;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while ((num = zip.read(buf, 0, buf.length)) != -1) {
                bos.write(buf, 0, num);
            }
            Path path = Paths.get(descDir, name);
            if (entry.isDirectory()) {
                File dir = new File(path.toString());
                dir.mkdirs();
            } else {
                File dir = new File(path.toString()).getParentFile();
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                Files.write(path, bos.toByteArray());
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
    @SuppressWarnings("rawtypes")
    public static void unZipFiles(File zipFile, String descDir) throws IOException {
        File pathFile = new File(descDir);
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        }
//        Charset charset = StandardCharsets.UTF_8;
//        if(System.getProperties().getProperty("os.name").toUpperCase().contains("WINDOWS")){
//            charset = Charset.forName("GBK");
//        }
        // 解决zip文件中有中文目录或者中文文件
        try (ZipFile zip = new ZipFile(zipFile, Charset.forName("GBK"))) {
            for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String zipEntryName = entry.getName();
                try (InputStream in = zip.getInputStream(entry)) {
                    String outPath = (descDir + zipEntryName).replaceAll("\\*", "/");
                    // 判断路径是否存在,不存在则创建文件路径
                    int vIndex = outPath.lastIndexOf('/');
                    File file;
                    if (vIndex == -1) {
                        file = new File(outPath);
                    } else {
                        file = new File(outPath.substring(0, vIndex));
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                    }
                    //判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
                    if (new File(outPath).isDirectory()) {
                        continue;
                    }

                    try (OutputStream out = new FileOutputStream(outPath)) {
                        byte[] buf1 = new byte[1024];
                        int len;
                        while ((len = in.read(buf1)) > 0) {
                            out.write(buf1, 0, len);
                        }
                    }
                }
            }
        }
    }
}
