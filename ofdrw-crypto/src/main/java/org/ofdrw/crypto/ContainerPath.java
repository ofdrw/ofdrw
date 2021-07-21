package org.ofdrw.crypto;

import org.ofdrw.core.basicType.ST_Loc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件在容器中的路径
 *
 * @author 权观宇
 * @since 2021-07-15 18:57:45
 */
public class ContainerPath {
    /**
     * 容器内绝对路径
     */
    private String path;
    /**
     * 文件系统内绝对路径
     */
    private Path abs;

    private ContainerPath() {
    }

    /**
     * 创建  文件在容器中的路径
     *
     * @param path 容器内绝对路径
     * @param abs  文件系统内绝对路径
     */
    public ContainerPath(String path, Path abs) {
        this.path = path;
        this.abs = abs;
    }

    public String getPath() {
        return path;
    }

    public Path getAbs() {
        return abs;
    }


    /**
     * 创建加密后的文件路径（空）
     * <p>
     * 输出加密文件命名规则 原文件名全小写 + .dat 后缀，如果重复，那么在文件名后面增加后缀
     *
     * @return 创建加密后输出文件
     * @throws IOException 文件操作异常
     */
    public ContainerPath createEncryptedFile() throws IOException {
        return newDatFile(this.path, abs.toAbsolutePath().getParent());
    }

    /**
     * 创建 后缀为.dat 的文件
     * <p>
     * 加密文件为 原文件 后缀改为 .dat
     * 若文件存在，则在文件名后追加下滑线序号 _N
     *
     * @param cAbs   容器内绝对路径
     * @param parent 生成的加密文件存储目录
     * @return 容器路径对象
     * @throws IOException 文件操作异常
     */
    public static ContainerPath newDatFile(String cAbs, Path parent) throws IOException {
        if (cAbs.charAt(0) != '/') {
            cAbs = '/' + cAbs;
        }
        if (Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        final ST_Loc containerLoc = ST_Loc.getInstance(cAbs);
        // 获取文件名
        String originalName = containerLoc.getFileName();
        int off = originalName.lastIndexOf('.');
        String name = originalName;
        if (off != -1) {
            name = originalName.substring(0, off);
        }
        String encFileName = name.toLowerCase() + ".dat";
        Path resPath = parent.resolve(encFileName);
        int cnt = 1;
        // 输出加密文件命名规则 原文件名全小写 + .dat 后缀，如果重复，那么在文件名后面增加后缀
        while (Files.exists(resPath)) {
            encFileName = name.toLowerCase() + "_" + cnt + ".dat";
            resPath = parent.resolve(encFileName);
            cnt++;
        }
        // 创建文件
        Files.createFile(resPath);
        return new ContainerPath(containerLoc.parent() + "/" + encFileName, resPath);
    }
}
