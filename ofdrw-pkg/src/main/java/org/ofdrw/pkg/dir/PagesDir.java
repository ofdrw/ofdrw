package org.ofdrw.pkg.dir;

import java.util.ArrayList;
import java.util.List;

/**
 * 页面容器
 *
 * @author 权观宇
 * @since 2020-01-18 03:34:34
 */
public class PagesDir {

    /**
     * 容器
     */
    private List<PageDir> container;

    public PagesDir() {
        this.container = new ArrayList<>(5);
    }

    /**
     * 增加页面
     *
     * @param page 页面容器
     * @return this
     */
    public PagesDir add(PageDir page) {
        if (container == null) {
            container = new ArrayList<>(5);
        }
        this.container.add(page);
        return this;
    }

    /**
     * 获取指定页面容器
     *
     * @param index 页码（从1开始）
     * @return this
     */
    public PageDir get(Integer index) {
        if (container == null) {
            return null;
        }
        for (PageDir page : container) {
            if (page.getIndex().equals(index)) {
                return page;
            }
        }
        return null;
    }
}
