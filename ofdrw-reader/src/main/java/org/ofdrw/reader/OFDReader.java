package org.ofdrw.reader;

import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * OFD解析器
 *
 * @author 权观宇
 * @since 2020-04-01 21:39:25
 */
public class OFDReader implements Closeable {

    /**
     * Reader工作过程中的工作目录
     * <p>
     * 用于存放解压后的OFD文档容器内容
     */
    private Path workDir;

    /**
     * OFD虚拟容器对象
     */
    private OFDDir ofdDir;

    /**
     * 资源定位器
     * <p>
     * 解析路径获取资源
     */
    private ResourceLocator rl;

    private OFDReader() {
    }

    public Path getWorkDir() {
        return workDir;
    }

    /**
     * 构造一个 OFDReader
     *
     * @param ofdFile OFD文件
     * @throws IOException OFD文件操作IO异常
     */
    public OFDReader(Path ofdFile) throws IOException {
        if (ofdFile == null || Files.notExists(ofdFile)) {
            throw new IllegalArgumentException("文件位置(ofdFile)不正确");
        }
        workDir = Files.createTempDirectory("ofd-tmp-");
        // 解压文档，到临时的工作目录
        new ZipFile(ofdFile.toFile()).extractAll(workDir.toAbsolutePath().toString());
        ofdDir = new OFDDir(workDir);
        // 创建资源定位器
        rl = new ResourceLocator(ofdDir);
    }

    /**
     * 获取文档虚拟容器
     *
     * @return OFD文档虚拟容器
     */
    public OFDDir getOFDDir() {
        return ofdDir;
    }

    /**
     * 通过页面页码获取页面对象
     *
     * @param numberOfPage 页码，从1起
     * @return 页面对象
     */
    public Page getPage(int numberOfPage) {
        if (numberOfPage <= 0) {
            throw new NumberFormatException("页码(numberOfPage)不能小于0");
        }
        try {
            int index = numberOfPage - 1;
            DocBody docBody = ofdDir.getOfd().getDocBody();
            ST_Loc docRoot = docBody.getDocRoot();
            // 路径解析对象获取并缓存虚拟容器
            Document document = rl.get(docRoot, Document::new);
            Pages pages = document.getPages();
            ST_Loc pageLoc = pages.getPageByIndex(index).getBaseLoc();
            return rl.get(pageLoc, Page::new);
        } catch (FileNotFoundException | DocumentException e) {
            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
        }

    }

    /**
     * 获取资源定位器
     * @return 资源定位器
     */
    public ResourceLocator getResourceLocator() {
        return rl;
    }

    /**
     * 关闭文档
     * <p>
     * 删除工作区
     *
     * @throws IOException 工作区删除异常
     */
    @Override
    public void close() throws IOException {
        if (workDir != null && Files.exists(workDir)) {
            try {
                FileUtils.deleteDirectory(workDir.toFile());
            } catch (IOException e) {
                throw new IOException("无法删除Reader的工作空间，原因：" + e.getMessage(), e);
            }
        }
    }
}
