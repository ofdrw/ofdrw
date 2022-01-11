package org.ofdrw.reader;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author qaqtutu
 * @since 2022-01-11 20:57:19
 */
public class ZipTest {

    private Path createZipWithRelativePath() throws IOException {
        Path target = Files.createTempFile("zip_with_relative_path", ".zip");
        try (
                OutputStream out = new FileOutputStream(target.toFile());
                ZipOutputStream zipOut = new ZipOutputStream(out)
        ) {
            /*
             * 这里使用“..”使文件直接解压到temp目录而不是刚创建的临时目录
             * */
            ZipEntry e = new ZipEntry("/../test.txt");
            zipOut.putNextEntry(e);
            zipOut.write(0);
            zipOut.flush();
        }
        return target;
    }

    /**
     * 通过构造相对路径来覆盖指定位置文件
     */
    @Test
    public void relativePathOverwriteTest() throws IOException {
        File zip = createZipWithRelativePath().toFile();
        Path tempDir = Files.createTempDirectory("unzip_test_with_relative_path");
        assertThrows(IOException.class, () -> ZipUtil.unZipFiles(zip, tempDir.toString()));

        if (zip.exists()) zip.delete();
        FileUtils.forceDelete(tempDir.toFile());
    }

    @Test
    public void zipBoomTest() throws IOException {
        Path boom = Paths.get("src/test/resources/zip_boom.zip");
        Path tempDir = Files.createTempDirectory("unzip_test_boom");

        assertThrows(IOException.class, () -> ZipUtil.unZipFiles(boom.toFile(), tempDir.toString()));
    }

//    /**
//     * 生成Zip炸弹
//     *
//     * 生成速度太慢所以预生成后存放resources目录
//     */
//    @Test
//    public void createZipBoom() throws IOException {
//        //生成一个
//        Path target = Files.createTempFile("zip_boom", ".zip");
//        try (
//                OutputStream out = new FileOutputStream(target.toFile());
//                ZipOutputStream zipOut = new ZipOutputStream(out);
//        ) {
//            ZipEntry e = new ZipEntry("Boom.txt");
//
//            zipOut.putNextEntry(e);
//
//            for(int i = 0;i<1024*1024;i++){
//                zipOut.write(new byte[1024]);
//            }
//
//            zipOut.flush();
//        }
//        System.out.println(target.toFile().getCanonicalPath());
//    }
}
