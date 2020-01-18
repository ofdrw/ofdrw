package org.ofdrw.pkg.dir;

import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.res.Res;

import java.nio.file.Path;

/**
 * 页面目录
 *
 * @author 权观宇
 * @since 2020-01-18 03:05:23
 */
public class PageDir {
    /**
     * 代表OFD中第几页
     * <p>
     * index 从 1 开始取
     */
    private Integer index;

    /**
     * 页面资源
     * <p>
     * 记录了资源的目录
     */
    private Res pageRes;

    /**
     * 资源容器
     */
    private ResDir res;

    /**
     * 页面描述
     */
    private Page content;

    public PageDir() {
        index = 1;
    }

    public PageDir(Integer index, Res pageRes, ResDir res, Page content) {
        this.index = index;
        this.pageRes = pageRes;
        this.res = res;
        this.content = content;
    }

    /**
     * @return 页码
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * 设置页码
     *
     * @param index 页码
     * @return this
     */
    public PageDir setIndex(Integer index) {
        this.index = index;
        return this;
    }

    /**
     * @return 资源
     */
    public Res getPageRes() {
        return pageRes;
    }

    public PageDir setPageRes(Res pageRes) {
        this.pageRes = pageRes;
        return this;
    }

    /**
     * 向页面中增加页面资源
     *
     * @param resource 资源
     * @return this
     */
    public PageDir add(Path resource) {
        if (res == null) {
            res = new ResDir();
        }
        this.res.add(resource);
        return this;
    }

    /**
     * 获取页面资源
     *
     * @param name 资源名称，包含后缀
     * @return 资源路径，如果资源不存在则为null
     */
    public Path get(String name) {
        if (this.res == null) {
            return null;
        }
        return this.res.get(name);
    }

    /**
     * @return 获取资源目录
     */
    public ResDir getResDir() {
        return this.res;
    }

    /**
     * @return 页面描述
     */
    public Page getContent() {
        return content;
    }

    /**
     * 设置页面描述
     *
     * @param content 页面描述
     * @return this
     */
    public PageDir setContent(Page content) {
        this.content = content;
        return this;
    }
}
