package org.ofdrw.pkg.dir;

import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.pkg.tool.DocObjDump;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文档容器
 *
 * @author 权观宇
 * @since 2020-01-18 03:57:59
 */
public class DocDir implements DirCollect {

    /**
     * 表示第几份文档，从0开始
     */
    private Integer index = 0;

    /**
     * 文档的根节点
     */
    private Document document;

    /**
     * 文档公共资源索引
     */
    private Res publicRes;

    /**
     * 文档自身资源索引
     */
    private Res documentRes;

    /**
     * 资源文件夹
     */
    private ResDir res;

    /**
     * 数字签名存储目录
     */
    private SignsDir signs;

    /**
     * 页面存储目录
     */
    private PagesDir pages;

    public DocDir() {

    }

    /**
     * @return 文档编号（用于表示第几个） ，从0 起
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * 设值 文档编号（用于表示第几个）
     *
     * @param index 第几个
     * @return this
     */
    public DocDir setIndex(Integer index) {
        this.index = index;
        return this;
    }

    /**
     * @return 文档的根节点
     */
    public Document getDocument() {
        return document;
    }

    /**
     * 设置 文档的根节点
     *
     * @param document 文档的根节点
     * @return this
     */
    public DocDir setDocument(Document document) {
        this.document = document;
        return this;
    }

    /**
     * @return 文档公共资源索引
     */
    public Res getPublicRes() {
        return publicRes;
    }

    /**
     * 设置 文档公共资源索引
     *
     * @param publicRes 文档公共资源索引
     * @return this
     */
    public DocDir setPublicRes(Res publicRes) {
        this.publicRes = publicRes;
        return this;
    }

    /**
     * @return 文档自身资源索引
     */
    public Res getDocumentRes() {
        return documentRes;
    }

    /**
     * 设置 文档自身资源索引
     *
     * @param documentRes 文档自身资源索引
     * @return this
     */
    public DocDir setDocumentRes(Res documentRes) {
        this.documentRes = documentRes;
        return this;
    }

    /**
     * @return 资源文件夹
     */
    public ResDir getRes() {
        return res;
    }

    /**
     * 设置 资源文件夹
     *
     * @param res 资源文件夹
     * @return this
     */
    public DocDir setRes(ResDir res) {
        this.res = res;
        return this;
    }

    /**
     * @return 数字签名存储目录
     */
    public SignsDir getSigns() {
        return signs;
    }

    /**
     * 设置 数字签名存储目录
     *
     * @param signs 数字签名存储目录
     * @return this
     */
    public DocDir setSigns(SignsDir signs) {
        this.signs = signs;
        return this;
    }

    /**
     * @return 页面存储目录
     */
    public PagesDir getPages() {
        return pages;
    }

    /**
     * 设置 页面存储目录
     *
     * @param pages 页面存储目录
     * @return this
     */
    public DocDir setPages(PagesDir pages) {
        this.pages = pages;
        return this;
    }


    /**
     * 增加资源
     *
     * @param resource 资源
     * @return this
     */
    public DocDir addResource(Path resource) {
        if (this.res == null) {
            this.res = new ResDir();
        }
        this.res.add(resource);
        return this;
    }

    /**
     * 获取资源
     *
     * @param name 资源名称（包含后缀名称）
     * @return 资源，不存在则返还null
     */
    public Path getResource(String name) {
        if (this.res == null) {
            return null;
        }
        return this.res.get(name);
    }

    /**
     * 创建目录并复制文件
     *
     * @param base 基础路径
     * @return 创建的目录路径
     * @throws IOException IO异常
     */
    @Override
    public Path collect(String base) throws IOException {
        if (document == null) {
            throw new IllegalArgumentException("文档根节点（document）为空");
        }

        Path path = Paths.get(base, "Doc_" + index);
        path = Files.createDirectories(path);
        String dir = path.toAbsolutePath().toString();

        DocObjDump.dump(document, Paths.get(dir, "Document.xml"));
        if (signs != null) {
            signs.collect(dir);
        }
        if (pages != null) {
            pages.collect(dir);
        }
        if (publicRes != null) {
            DocObjDump.dump(publicRes, Paths.get(dir, "PublicRes.xml"));
        }
        if (documentRes != null) {
            DocObjDump.dump(documentRes, Paths.get(dir, "DocumentRes.xml"));
        }
        if (res != null) {
            res.collect(dir);
        }
        return path;
    }
}
