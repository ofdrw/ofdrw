package org.ofdrw.crypto;

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
     * @param path 容器内绝对路径
     * @param abs 文件系统内绝对路径
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
}
