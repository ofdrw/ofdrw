package org.ofdrw.pkg.container;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.core.annotation.pageannot.PageAnnot;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.res.Res;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 页面目录容器
 *
 * @author 权观宇
 * @since 2020-01-18 03:05:23
 */
public class PageDir extends VirtualContainer {

    /**
     * 页面容器名称前缀
     */
    public static final String PageContainerPrefix = "Page_";

    /**
     * 页面描述文件名称
     */
    public static final String ContentFileName = "Content.xml";

    /**
     * 记录了资源描述文件名称
     */
    public static final String PageResFileName = "PageRes.xml";

    /**
     * 记录了页面关联的注解对象
     */
    public static final String AnnotationFileName = "Annotation.xml";


    /**
     * 代表OFD中第几页
     * <p>
     * index 从 0 开始取
     */
    private int index = 0;


    public PageDir(Path fullDir) throws IllegalArgumentException {
        super(fullDir);
        String indexStr = this.getContainerName().replace(PageContainerPrefix, "");
        try {
            this.index = Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            clean();
            throw new IllegalArgumentException("不合法的文件目录名称：" + this.getContainerName() + "，目录名称应为 Page_N");
        }
    }


    /**
     * 获取页面索引
     *
     * @return 页面索引
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * 获取页面资源描述文件
     *
     * @return 页面资源描述文件
     * @throws FileNotFoundException 资源文件不存在
     * @throws DocumentException     资源文件解析失败
     */
    public Res getPageRes() throws FileNotFoundException, DocumentException {
        Element obj = this.getObj(PageResFileName);
        return new Res(obj);
    }

    /**
     * 设置页面资源描述对象
     *
     * @param pageRes 页面资源描述对象
     * @return this
     */
    public PageDir setPageRes(Res pageRes) {
        this.putObj(PageResFileName, pageRes);
        return this;
    }

    /**
     * 获取分页注释文件
     *
     * @return 分页注释文件
     * @throws FileNotFoundException 描述文件不存在
     * @throws DocumentException     描述文件内容错误
     */
    public PageAnnot getPageAnnot() throws FileNotFoundException, DocumentException {
        Element obj = this.getObj(AnnotationFileName);
        return new PageAnnot(obj);
    }

    /**
     * 设置分页注释文件
     *
     * @param pageAnnot 分页注释文件
     * @return this
     */
    public PageDir setPageAnnot(PageAnnot pageAnnot) {
        this.putObj(AnnotationFileName, pageAnnot);
        return this;
    }

    /**
     * 获取页面资源目录
     * <p>
     * 如果目录不存在则创建
     *
     * @return 资源目录容器
     */
    public ResDir obtainRes() {
        return this.obtainContainer("Res", ResDir::new);
    }

    /**
     * 获取资源文件虚拟容器
     *
     * @return 获取资源目录
     * @throws FileNotFoundException 该页面没有资源文件目录
     */
    public ResDir getResDir() throws FileNotFoundException {
        return this.getContainer("Res", ResDir::new);
    }

    /**
     * 向页面中增加页面资源
     *
     * @param resource 资源
     * @return this
     * @throws IOException 文件复制过程中发生异常
     */
    public PageDir add(Path resource) throws IOException {
        // 如果存在那么获取容器，不存在则创建容器
        obtainRes().add(resource);
        return this;
    }

    /**
     * 获取页面资源
     *
     * @param name 资源名称，包含后缀
     * @return 资源路径，如果资源不存在则为null
     * @throws FileNotFoundException 文件不存在
     */
    public Path get(String name) throws FileNotFoundException {
        // 如果存在那么获取容器，不存在则创建容器
        return obtainRes().getFile(name);
    }


    /**
     * 获取页面描述对象
     *
     * @return 页面描述
     * @throws FileNotFoundException 描述文件不存在
     * @throws DocumentException     描述文件内容错误
     */
    public Page getContent() throws FileNotFoundException, DocumentException {
        Element obj = this.getObj(ContentFileName);
        return new Page(obj);
    }

    /**
     * 设置页面描述
     *
     * @param content 页面描述
     * @return this
     */
    public PageDir setContent(Page content) {
        this.putObj(ContentFileName, content);
        return this;
    }

}
