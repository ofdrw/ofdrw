package org.ofdrw.pkg.dir;

import net.lingala.zip4j.ZipFile;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.pkg.tool.DocObjDump;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * OFD文档对象
 * <p>
 * 1. 创建目录
 * 2. 复制文件
 * 3. 序列化文件
 *
 * @author 权观宇
 * @since 2020-01-18 13:00:45
 */
public class OFDDir implements DirCollect {
    /**
     * 文档容器列表
     */
    private List<DocDir> container;

    /**
     * 文档主入口文件对象
     */
    private OFD ofd;

    public OFDDir() {
        container = new ArrayList<>(1);
    }

    /**
     * @return 文档主入口文件对象
     */
    public OFD getOfd() {
        return ofd;
    }

    /**
     * 设置  文档主入口文件对象
     *
     * @param ofd 文档主入口文件对象
     * @return this
     */
    public OFDDir setOfd(OFD ofd) {
        this.ofd = ofd;
        return this;
    }

    /**
     * 增加文档容器
     *
     * @param docDir 文档容器
     * @return this
     */
    public OFDDir add(DocDir docDir) {
        if (docDir == null) {
            return this;
        }
        if (container == null) {
            container = new ArrayList<>(1);
        }
        container.add(docDir);
        return this;
    }

    /**
     * 获取文档容器
     *
     * @param numberOf 第几个
     * @return 文档容器
     */
    public DocDir getDoc(Integer numberOf) {
        if (container == null) {
            return null;
        }
        for (DocDir docDir : this.container) {
            if (docDir.getIndex().equals(numberOf)) {
                return docDir;
            }
        }
        return null;
    }

    /**
     * 获取第一个文档容器作为默认
     *
     * @return 第一个文档容器
     */
    public DocDir getDocDefault() {
        return getDoc(0);
    }

    /**
     * 创建目录并复制文件
     *
     * @param dir 基础路径
     * @return 创建的目录路径
     * @throws IOException IO异常
     */
    @Override
    public Path collect(String dir) throws IOException {
        if (ofd == null) {
            throw new IllegalArgumentException("文件主入口文件（ofd）为空");
        }
        if (container == null || container.isEmpty()) {
            throw new IllegalArgumentException("文档（doc）为空");
        }

        Path path = Paths.get(dir);
        Files.createDirectories(path);
        for (DocDir doc : container) {
            doc.collect(dir);
        }
        DocObjDump.dump(ofd, Paths.get(dir, "OFD.xml"));
        return path;
    }

    /**
     * 打包成OFD
     * <p>
     * 1. 创建文件夹复制文件
     * 2. 打包
     * 3. 删除临时文件
     * <p>
     * 使用操作系统临时文件夹作为生成OFD文件的临时路径
     *
     * @param fileName OFD文件名称路径（含后缀名）
     * @throws IOException IO异常
     */
    public void jar(String fileName) throws IOException {
        String tmp = System.getProperty("java.io.tmpdir");
        jar(tmp, fileName);
    }

    /**
     * 打包成OFD
     * <p>
     * 1. 创建文件夹复制文件
     * 2. 打包
     * 3. 删除临时文件
     *
     * @param base     临时文件生成目录
     * @param fileName OFD文件名称路径（含后缀名）
     * @throws IOException IO异常
     */
    public void jar(String base, String fileName) throws IOException {
        Path tmpPath = null;
        try {
            if (fileName == null || fileName.trim().length() == 0) {
                throw new IllegalArgumentException("OFD文件名（fileName）不能为空");
            }
            Path target = Paths.get(fileName);
            if (Files.exists(target)) {
                Files.delete(target);
            }

            Path basePath = Paths.get(base);
            if (Files.notExists(basePath)) {
                // 基础目录不存在则创建
                basePath = Files.createDirectories(basePath);
            }
            // 在基础目录下创建临时目录用于生成OFD
            tmpPath = Files.createTempDirectory(basePath, "ofd-tmp-");
            String workDir = tmpPath.toAbsolutePath().toString();
            // 创建文件夹复制文件
            Path workDirPath = this.collect(workDir);
            // 打包OFD文件
            this.zip(workDirPath.toFile(), fileName);
        } finally {
            if (tmpPath != null) {
                try (Stream<Path> walk = Files.walk(tmpPath)) {
                    walk.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            }
        }
    }

    /**
     * 打包OFD文件
     *
     * @param workDirPath OFD内容工作目录
     * @param filename    文件名称（包含后缀的路径）
     * @throws IOException IO异常
     */
    private void zip(File workDirPath, String filename) throws IOException {
        ZipFile ofdFile = new ZipFile(filename);
        for (File f : workDirPath.listFiles()) {
            if (f.isDirectory()) {
                ofdFile.addFolder(f);
            } else {
                ofdFile.addFile(f);
            }
        }
    }
}
