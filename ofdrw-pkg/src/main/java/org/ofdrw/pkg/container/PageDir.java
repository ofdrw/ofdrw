package org.ofdrw.pkg.container;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.core.Holder;
import org.ofdrw.core.annotation.pageannot.PageAnnot;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicType.ST_Loc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 页面目录容器
 *
 * @author 权观宇
 * @since 2020-01-18 03:05:23
 */
public class PageDir extends VirtualContainer {

    public static final Pattern AnnotFileRegex = Pattern.compile("Annot_(\\d+).xml");

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
     * 注释文件前缀
     * <p>
     * GMT0099 OFD 2.0
     */
    public static final String AnnotFilePrefix = "Annot_";


    /**
     * 代表OFD  页面索引号
     * <p>
     * index 从 0 开始取
     */
    private int index = 0;


    /**
     * 最大注释文件对象索引
     * <p>
     * 在向Page_N中加入注释时使用
     */
    private Integer maxAnnotIndex = -1;


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
     * 根据页面注释文件的名称前缀获取该目录下所有注释对象
     * <p>
     * 注释文件前缀： Annot_M.xml
     * <p>
     * Key: 文件名
     * Value: 注释对象
     *
     * @return 容器内所有注释对象
     * @throws IOException 文件读异常
     */
    public Map<String, PageAnnot> getPageAnnots() throws IOException {
        Map<String, PageAnnot> res = new HashMap<>();
        // 过滤出注释文件
        Files.list(this.getContainerPath()).filter((item) -> {
            String fileName = item.getFileName().toString().toLowerCase();
            // 不是目录 并且 文件名以 Annot_ 开头
            return Files.isRegularFile(item)
                    && fileName.startsWith(AnnotFilePrefix.toLowerCase())
                    && fileName.endsWith(".xml");
        }).forEach(item -> {
            Element obj = null;
            try {
                obj = this.getObj(AnnotationFileName);
            } catch (Exception e) {
                // ignore
                obj = null;
            }
            if (obj != null) {
                res.put(item.getFileName().toString(), new PageAnnot(obj));
            }
        });
        return res;
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
     * 向页面加入新的注释文件
     *
     * @param pageAnnot 注释对象
     * @return 注释文件容器内绝对路径
     * @throws IOException 文件复制过程中发生异常
     */
    public ST_Loc addAnnot(PageAnnot pageAnnot) throws IOException {
        if (pageAnnot == null) {
            return null;
        }
        // 获取当前目录中最大的注释文件索引号，然后+1 作为新的文件索引
        maxAnnotIndex = getMaxAnnotFileIndex() + 1;
        String fileName = AnnotFilePrefix + maxAnnotIndex + ".xml";
        return addAnnot(fileName, pageAnnot);
    }

    /**
     * 向页面内添加注释文件
     *
     * @param fileName  文件名称
     * @param pageAnnot 注释
     * @return 注释文件容器内绝对路径
     */
    public ST_Loc addAnnot(String fileName, PageAnnot pageAnnot) {
        this.putObj(fileName, pageAnnot);
        return this.getAbsLoc().cat(fileName);
    }

    /**
     * 获取当Page_N容器中最大的注释文件索引号
     *
     * @return 索引数字
     * @throws IOException 文件读取异常
     */
    public Integer getMaxAnnotFileIndex() throws IOException {
        if (maxAnnotIndex < 0) {
            Holder<Integer> maxIndexHolder = new Holder<>(-1);
            Files.list(this.getContainerPath()).forEach((item) -> {
                String fileName = item.getFileName().toString().toLowerCase();
                // 不是目录 并且 文件名以 Annot_ 开头
                if (fileName.startsWith(AnnotFilePrefix.toLowerCase())) {
                    String numStr = fileName.replace(AnnotFilePrefix.toLowerCase(), "")
                            .split("\\.")[0];
                    try {
                        int n = Integer.parseInt(numStr);
                        if (n > maxIndexHolder.value) {
                            maxIndexHolder.value = n;
                        }
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            });
            maxAnnotIndex = maxIndexHolder.value;
        }
        return maxAnnotIndex;
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
