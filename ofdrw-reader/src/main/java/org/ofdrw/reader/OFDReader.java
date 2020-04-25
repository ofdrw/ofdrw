package org.ofdrw.reader;

import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.core.basicStructure.doc.CT_CommonData;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.signatures.Signatures;
import org.ofdrw.pkg.container.OFDDir;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

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

    /**
     * 是否已经关闭文档
     */
    private boolean closed = false;

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
     * 获取默认文档Doc_0中的签名列表文件的绝对路径
     *
     * @return 签名列表文件绝对路径
     * @throws BadOFDException 错误OFD结构和文件格式导致结构无法解析
     */
    public ST_Loc getDefaultDocSignaturesPath() {
        try {
            rl.save();
            rl.cd("/");
            DocBody docBody = ofdDir.getOfd().getDocBody();
            // 签名列表文件路径
            ST_Loc loc = docBody.getSignatures();
            if (loc == null) {
                return null;
            }
            // 转化为绝对路径
            String signListFileAbsPath = rl.toAbsolutePath(loc);
            return ST_Loc.getInstance(signListFileAbsPath);
        } catch (FileNotFoundException | DocumentException e) {
            throw new BadOFDException("错误OFD结构和文件格式", e);
        } finally {
            rl.restore();
        }
    }

    /**
     * 获取默认的签名列表对象
     *
     * @return 签名列表对象
     */
    public Signatures getDefaultSignatures() {
        ST_Loc signaturesLoc = getDefaultDocSignaturesPath();
        if (signaturesLoc == null) {
            throw new BadOFDException("OFD文档中不存在Signatures.xml");
        }
        // 获取签名列表对象
        try {
            return rl.get(signaturesLoc, Signatures::new);
        } catch (FileNotFoundException | DocumentException e) {
            throw new BadOFDException("错误OFD结构和文件格式", e);
        }
    }

    /**
     * 文档是否包含数字签名
     *
     * @return true - 含有；false - 不含；
     */
    public boolean hasSignature() {
        DocBody docBody = null;
        try {
            docBody = ofdDir.getOfd().getDocBody();
            ST_Loc signaturesLoc = docBody.getSignatures();
            return signaturesLoc != null;
        } catch (FileNotFoundException | DocumentException e) {
            throw new BadOFDException("错误OFD结构和文件格式", e);
        }
    }

    /**
     * 获取OFD含有的总页面数量
     *
     * @return 总页数
     */
    public int getNumberOfPages() {
        try {
            rl.save();
            DocBody docBody = ofdDir.getOfd().getDocBody();
            ST_Loc docRoot = docBody.getDocRoot();
            // 路径解析对象获取并缓存虚拟容器
            Document document = rl.get(docRoot, Document::new);
            rl.cd(docRoot.parent());
            Pages pages = document.getPages();
            return pages.getSize();
        } catch (FileNotFoundException | DocumentException e) {
            throw new BadOFDException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            // 还原原有工作区
            rl.restore();
        }
    }

    /**
     * 获取页面物理大小
     * <p>
     * 如果页面没有定义页面区域，则使用文件 CommonData中的定义
     *
     * @param page 页面对象
     * @return 页面大小
     */
    public ST_Box getPageSize(Page page) {
        CT_PageArea pageArea = page.getArea();
        if (pageArea == null) {
            // 如果页面没有定义页面区域，则使用文件 CommonData中的定义
            Document document;
            try {
                document = ofdDir.obtainDocDefault().getDocument();
            } catch (FileNotFoundException | DocumentException e) {
                throw new BadOFDException("OFD解析失败，原因:" + e.getMessage(), e);
            }
            CT_CommonData commonData = document.getCommonData();
            pageArea = commonData.getPageArea();
        }

        return pageArea.getPhysicalBox();
    }

    /**
     * 通过页面页码获取页面对象
     *
     * @param pageNum 页码，从1起
     * @return 页面对象
     */
    public Page getPage(int pageNum) {
        if (pageNum <= 0) {
            throw new NumberFormatException("页码(pageNum)不能小于0");
        }
        try {
            int index = pageNum - 1;
            // 保存切换目录前的工作区
            rl.save();
            DocBody docBody = ofdDir.getOfd().getDocBody();
            ST_Loc docRoot = docBody.getDocRoot();
            // 路径解析对象获取并缓存虚拟容器
            Document document = rl.get(docRoot, Document::new);
            rl.cd(docRoot.parent());
            Pages pages = document.getPages();
            List<org.ofdrw.core.basicStructure.pageTree.Page> pageList = pages.getPages();
            if (index >= pageList.size()) {
                throw new NumberFormatException(pageNum + "超过最大页码:" + pageList.size());
            }
            // 获取页面的路径
            ST_Loc pageLoc = pageList.get(index).getBaseLoc();
            return rl.get(pageLoc, Page::new);
        } catch (FileNotFoundException | DocumentException e) {
            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            // 还原原有工作区
            rl.restore();
        }
    }

    /**
     * 获取页面的对象ID
     *
     * @param pageNum 页码
     * @return 对象ID
     */
    public ST_ID getPageObjectId(int pageNum) {
        if (pageNum <= 0) {
            throw new NumberFormatException("页码(pageNum)不能小于0");
        }
        try {
            int index = pageNum - 1;
            // 保存切换目录前的工作区
            rl.save();
            DocBody docBody = ofdDir.getOfd().getDocBody();
            ST_Loc docRoot = docBody.getDocRoot();
            // 路径解析对象获取并缓存虚拟容器
            Document document = rl.get(docRoot, Document::new);
            rl.cd(docRoot.parent());
            Pages pages = document.getPages();
            List<org.ofdrw.core.basicStructure.pageTree.Page> pageList = pages.getPages();
            if (index >= pageList.size()) {
                throw new NumberFormatException(pageNum + "超过最大页码:" + pageList.size());
            }
            return pageList.get(index).getID();
        } catch (FileNotFoundException | DocumentException e) {
            throw new BadOFDException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            // 还原原有工作区
            rl.restore();
        }
    }

    /**
     * 获取资源定位器
     *
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
        if (closed) {
            return;
        }
        closed = true;
        if (workDir != null && Files.exists(workDir)) {
            try {
                FileUtils.deleteDirectory(workDir.toFile());
            } catch (IOException e) {
                throw new IOException("无法删除Reader的工作空间，原因：" + e.getMessage(), e);
            }
        }
    }
}
