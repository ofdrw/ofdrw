package org.ofdrw.tool.merge;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文档的页面
 * 
 * @author 权观宇
 * @since 2024-11-11 18:37:30
 */
public class DocPage {
    /**
     * 文档路径
     */
    public Path path;
    /**
     * 页面索引（从1起）
     */
    public int index;

    /**
     * 创建文档页面信息
     * @param path 文档路径
     * @param index 页面索引
     */
    public DocPage(Path path, int index) {
        this.path = path;
        this.index = index;
    }

    /**
     * 创建文档页面信息
     * @param path 文档路径
     * @param index 页面索引
     */
    public DocPage(String  path, int index) {
        this.path = Paths.get(path);
        this.index = index;
    }
}
