package org.ofdrw.pkg.container;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;

/**
 * 页面容器
 *
 * @author 权观宇
 * @since 2020-01-18 03:34:34
 */
public class PagesDir extends VirtualContainer {
    /**
     * 最大页面索引 + 1
     * <p>
     * index + 1
     */
    private int maxPageIndex = 0;


    public PagesDir(Path fullDir) throws IllegalArgumentException {
        super(fullDir);
        initContainer();
    }

    /**
     * 初始化容器
     */
    private void initContainer() {
        File fullDirFile = new File(getFullPath());
        File[] files = fullDirFile.listFiles();
        if (files != null) {
            // 遍历容器中已经有的页面目录，初始页面数量
            for (File f : files) {
                // 签名目录名为： Page_N
                if (f.getName().startsWith(PageDir.PageContainerPrefix)) {
                    String numb = f.getName().replace(PageDir.PageContainerPrefix, "");
                    int num = Integer.parseInt(numb);
                    if (maxPageIndex <= num) {
                        maxPageIndex = num + 1;
                    }
                }
            }
        }
    }

    /**
     * 创建一个新的页面容器
     *
     * @return 页面容器
     */
    public PageDir newPageDir() {
        String name = PageDir.PageContainerPrefix + maxPageIndex;
        maxPageIndex++;
        // 创建容器
        return this.obtainContainer(name, PageDir::new);
    }

    /**
     * 获取索引的页面容器
     * <p>
     * 页码 = index + 1
     *
     * @param index 索引（从0开始）
     * @return 指定索引页面容器
     * @throws FileNotFoundException 无法找到指定索引页面
     */
    public PageDir getByIndex(int index) throws FileNotFoundException {
        String containerName = PageDir.PageContainerPrefix + index;
        return this.getContainer(containerName, PageDir::new);
    }
}
