package org.ofdrw.pkg.container;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;

/**
 * 注释容器
 * <p>
 * GMT0099 OFD 2.0
 *
 * @author 权观宇
 * @since 2021-6-15 19:58:58
 */
public class AnnotsDir extends VirtualContainer {


    public AnnotsDir(Path fullDir) throws IllegalArgumentException {
        super(fullDir);
    }


    /**
     * 通过索引获取 页面文件
     * <p>
     * 如果目录不存在那么创建
     *
     * @param index 页面索引
     * @return 指定索引页面容器
     */
    public PageDir obtainByIndex(int index) {
        String containerName = PageDir.PageContainerPrefix + index;
        return this.obtainContainer(containerName, PageDir::new);
    }

    /**
     * 通过索引获取 页面文件
     * <p>
     * 如果目录不存在那么创建
     *
     * @param containerName 虚拟容器名称
     * @return 指定索引页面容器
     * @throws FileNotFoundException 无法找到指定索引页面
     */
    public PageDir getPageDir(String containerName) throws FileNotFoundException {
        return this.getContainer(containerName, PageDir::new);
    }
}
