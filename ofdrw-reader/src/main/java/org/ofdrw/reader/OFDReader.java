package org.ofdrw.reader;

import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.ofdrw.core.annotation.Annotations;
import org.ofdrw.core.attachment.Attachments;
import org.ofdrw.core.attachment.CT_Attachment;
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
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.OFDDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
        ZipUtil.unZipFiles(ofdFile.toFile(), workDir.toAbsolutePath().toString() + File.separator);
        ofdDir = new OFDDir(workDir);
        // 创建资源定位器
        rl = new ResourceLocator(ofdDir);
    }

    /**
     * 构造一个 OFDReader
     *
     * @param ofdFileLoc OFD文件位置，例如：”/home/user/myofd.ofd“
     * @throws IOException OFD文件操作IO异常
     */
    public OFDReader(String ofdFileLoc) throws IOException {
        this(Paths.get(ofdFileLoc));
    }

    /**
     * 构造一个 OFDReader
     *
     * @param stream OFD文件输入流
     * @throws IOException OFD文件操作IO异常
     */
    public OFDReader(InputStream stream) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException("文件输入流(stream)不正确");
        }
        workDir = Files.createTempDirectory("ofd-tmp-");
        // 解压文档，到临时的工作目录
        ZipUtil.unZipFiles(stream, workDir.toAbsolutePath().toString() + File.separator);
        ofdDir = new OFDDir(workDir);
        // 创建资源定位器
        rl = new ResourceLocator(ofdDir);
    }

    /**
     * 因一些ofd文件无法使用ZipUtil解压缩，可以让用户自己在外面解压缩好后，传入根目录创建
     * 例如用户可以使用unzip或者unar等命令行方式解压缩，因此通过参数控制是否删除目录。
     *
     * @param unzippedPathRoot 已经解压的OFD根目录位置
     * @param deleteOnClose    退出时是否删除 unzippedPathRoot 文件， true - 退出时删除；false - 不删除
     */
    public OFDReader(String unzippedPathRoot, boolean deleteOnClose) {
        workDir = Paths.get(unzippedPathRoot);
        if (Files.notExists(workDir) || !Files.isDirectory(workDir)) {
            throw new IllegalArgumentException("文件位置(unzippedPathRoot)不正确");
        }
        ofdDir = new OFDDir(workDir);
        // 创建资源定位器
        rl = new ResourceLocator(ofdDir);
        // 通过参数来指定是否删除外部文档，保证谁创建的目录谁负责这个原则
        if (!deleteOnClose) {
            closed = true;
        }

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
     * 获取注解列表文件对象
     * <p>
     * 如果文档中没有注释文件，那么返还null
     *
     * @return 注解列表文件对象或null
     */
    public Annotations getAnnotations() {
        try {
            rl.save();
            DocBody docBody = ofdDir.getOfd().getDocBody();
            ST_Loc docRoot = docBody.getDocRoot();
            // 路径解析对象获取并缓存虚拟容器
            Document document = rl.get(docRoot, Document::new);
            rl.cd(docRoot.parent());

            ST_Loc annotations = document.getAnnotations();
            if (annotations == null) {
                return null;
            }
            return rl.get(annotations, Annotations::new);
        } catch (FileNotFoundException | DocumentException e) {
            throw new BadOFDException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            // 还原原有工作区
            rl.restore();
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
     * 获取页面信息
     *
     * @param pageNum 页码，从1开始
     * @return 页面信息
     */
    public PageInfo getPageInfo(int pageNum) {
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

            Page obj = rl.get(pageLoc, Page::new);
            // 获取页面的容器绝对路径
            pageLoc = rl.getAbsTo(pageLoc);

            ST_Box pageSize = getPageSize(obj);
            return new PageInfo()
                    .setIndex(pageNum)
                    .setId(pageList.get(index).getID())
                    .setObj(obj)
                    .setSize(pageSize.clone())
                    .setPageAbsLoc(pageLoc);
        } catch (FileNotFoundException | DocumentException e) {
            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
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
        if (pageArea == null || pageArea.getPhysicalBox() == null) {
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
        return getPageInfo(pageNum).getObj();
    }

    /**
     * 获取页面的对象ID
     *
     * @param pageNum 页码
     * @return 对象ID
     */
    public ST_ID getPageObjectId(int pageNum) {
        return getPageInfo(pageNum).getId();
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
     * 获取附件对象
     *
     * @param name 附件名称
     * @return 如果附件或附件记录不存在，那么返还null
     * @throws BadOFDException 文档结构损坏
     */
    public CT_Attachment getAttachment(String name) {
        if (name == null || name.trim().length() == 0) {
            return null;
        }
        rl.save();
        try {
            return getAttachment(name, rl);
        } finally {
            rl.restore();
        }
    }

    /**
     * 获取附件文件
     * <p>
     * 注意：该文件会在Close Reader时候被删除，请在之前复制到其他地方
     *
     * @param name 附件名称
     * @return 附件文件路径
     */
    public Path getAttachmentFile(String name) {
        if (name == null || name.trim().length() == 0) {
            return null;
        }
        rl.save();
        try {
            CT_Attachment attachment = getAttachment(name, rl);
            if (attachment == null) {
                return null;
            }
            ST_Loc fileLoc = attachment.getFileLoc();
            try {
                return rl.getFile(fileLoc);
            } catch (FileNotFoundException e) {
                System.err.println(">> 无法根据附件对象的描述获取到附件: " + fileLoc.toString());
                return null;
            }
        } finally {
            rl.restore();
        }
    }

    /**
     * 获取附件对象
     * <p>
     * 该方法不会恢复资源定位器
     *
     * @param name 附件名称
     * @param rl   资源定位器
     * @return 附件对象
     */
    private CT_Attachment getAttachment(String name, ResourceLocator rl) {
        if (name == null || name.trim().length() == 0) {
            return null;
        }
        rl.save();
        try {
            DocDir docDir = ofdDir.obtainDocDefault();
            rl.cd(docDir);
            Document document = null;
            Attachments attachments = null;
            try {
                document = docDir.getDocument();
            } catch (FileNotFoundException | DocumentException e) {
                throw new BadOFDException(e);
            }
            ST_Loc attachmentsLoc = document.getAttachments();
            if (attachmentsLoc == null) {
                // 文档中没有附件目录文件
                return null;
            }
            try {
                // 获取附件目录
                attachments = rl.get(attachmentsLoc, Attachments::new);
            } catch (FileNotFoundException | DocumentException e) {
                System.err.println(">> 无法获取或解析Attachments.xml: " + e.getMessage());
                return null;
            }
            for (CT_Attachment attachment : attachments.getAttachments()) {
                // 寻找匹配名称的附件
                if (attachment.getAttachmentName().equals(name)) {
                    return attachment;
                }
            }
        } finally {
            rl.restore();
        }
        return null;
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
