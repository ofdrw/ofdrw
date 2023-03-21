package org.ofdrw.graphics2d;

import org.ofdrw.core.attachment.Attachments;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.core.basicStructure.doc.CT_CommonData;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicStructure.pageTree.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.MediaType;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.gv.GlobalVar;
import org.ofdrw.pkg.container.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 图形OFD文档对象
 *
 * @author 权观宇
 * @since 2023-1-18 09:45:18
 */
public class OFDGraphicsDocument implements Closeable {
    /**
     * 打包后OFD文档存放路径
     */
    private Path outPath;

    /**
     * 文档是否已经关闭
     * true 表示已经关闭，false 表示未关闭
     */
    private boolean closed = false;

    /**
     * OFD 打包
     */
    public final OFDDir ofdDir;

    /**
     * 当前文档中所有对象使用标识的最大值。
     * 初始值为 0。MaxUnitID主要用于文档编辑，
     * 在向文档增加一个新对象时，需要分配一个
     * 新的标识符，新标识符取值宜为 MaxUnitID + 1，
     * 同时需要修改此 MaxUnitID值。
     */
    public final AtomicInteger MaxUnitID;


    /**
     * 文档属性信息，该对象会在初始化是被创建并且添加到文档中
     * 此处只是保留引用，为了方便操作。
     */
    public CT_CommonData cdata;


    /**
     * OFD文档对象
     */
    public final Document document;

    /**
     * 附件列表
     * <p>
     * null表示没有附件
     */
    public Attachments attachments;

    /**
     * 正在操作的文档目录
     */
    public final DocDir docDir;

    /**
     * 资源管理器
     */
    public final ResManager resMgr;

    /**
     * 在指定路径位置上创建一个OFD文件
     *
     * @param outPath OFD输出路径
     */
    public OFDGraphicsDocument(Path outPath) {
        this();
        if (outPath == null) {
            throw new IllegalArgumentException("OFD文件存储路径(outPath)为空");
        }
        if (Files.isDirectory(outPath)) {
            throw new IllegalArgumentException("OFD文件存储路径(outPath)不能是目录");
        }
        if (!Files.exists(outPath.toAbsolutePath().getParent())) {
            throw new IllegalArgumentException("OFD文件存储路径(outPath)上级目录 [" + outPath.getParent().toAbsolutePath() + "] 不存在");
        }
        this.outPath = outPath;
    }

    /**
     * 文档初始化构造器
     */
    private OFDGraphicsDocument() {


        // 初始化文档对象
        CT_DocInfo docInfo = new CT_DocInfo()
                .setDocID(UUID.randomUUID())
                .setCreationDate(LocalDate.now())
                .setCreator("OFD R&W")
                .setCreatorVersion(GlobalVar.Version);
        DocBody docBody = new DocBody()
                .setDocInfo(docInfo)
                .setDocRoot(new ST_Loc("Doc_0/Document.xml"));
        OFD ofd = new OFD().addDocBody(docBody);


        // 创建一个低层次的文档对象
        document = new Document();
        cdata = new CT_CommonData();
        // 默认页面大小为A4
        CT_PageArea defaultPageSize = new CT_PageArea()
                .setPhysicalBox(0, 0, 210d, 297d)
                .setApplicationBox(0, 0, 210d, 297d);
        // 默认使用RGB颜色空间所以此处不设置颜色空间
        // 设置页面属性
        cdata.setPageArea(defaultPageSize);
        document.setCommonData(cdata)
                // 空的页面引用集合，该集合将会在解析虚拟页面时得到填充
                .setPages(new Pages());

        ofdDir = OFDDir.newOFD()
                .setOfd(ofd);
        // 创建一个新的文档
        DocDir docDir = ofdDir.newDoc();
        this.docDir = docDir;
        docDir.setDocument(document);

        MaxUnitID = new AtomicInteger(0);
        this.resMgr = new ResManager(this.docDir, MaxUnitID);
    }

    /**
     * 创建页面，单位毫米
     *
     * @param width  页面宽度，单位：毫米
     * @param height 页面高度，单位：毫米
     * @return 2D图形绘制对象
     */
    public OFDPageGraphics2D newPage(double width, double height) {
        CT_PageArea size = new CT_PageArea()
                .setPhysicalBox(0, 0, width, height)
                .setApplicationBox(0, 0, width, height);
        return newPage(size);
    }

    /**
     * 创建新页面，返回该页面2D图形对象
     *
     * @param pageSize 页面大小配置
     * @return 2D图形绘制对象
     */
    public OFDPageGraphics2D newPage(CT_PageArea pageSize) {
        final Pages pages = document.getPages();
        // 如果存在Pages那么获取，不存在那么创建
        final PagesDir pagesDir = docDir.obtainPages();

        // 创建页面容器
        PageDir pageDir = pagesDir.newPageDir();
        String pageLoc = String.format("Pages/Page_%d/Content.xml", pageDir.getIndex());
        final Page page = new Page(MaxUnitID.incrementAndGet(), pageLoc);
        pages.addPage(page);

        // 创建页面对象
        org.ofdrw.core.basicStructure.pageObj.Page pageObj = new org.ofdrw.core.basicStructure.pageObj.Page();
        if (pageSize != null) {
            pageObj.setArea(pageSize);
        } else {
            pageSize = this.cdata.getPageArea();
        }
        pageDir.setContent(pageObj);

        return new OFDPageGraphics2D(this, pageDir, pageObj, pageSize.getBox());
    }

    /**
     * 添加图片资源
     *
     * @param img 图片渲染对象
     * @return 资源ID
     * @throws RuntimeException 图片转写IO异常
     */
    public ST_ID addResImg(Image img) {
        if (img == null) {
            return null;
        }
        final ResDir resDir = docDir.obtainRes();
        final Path resDirPath = resDir.getContainerPath();
        final File imgFile;
        try {
            imgFile = File.createTempFile("res", ".png", resDirPath.toFile());
            BufferedImage bi;
            if (img instanceof BufferedImage) {
                bi = (BufferedImage) img;
            } else {
                bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                Graphics g2 = bi.getGraphics();
                g2.drawImage(img, 0, 0, null);
                g2.dispose();
            }
            ImageIO.write(bi, "png", imgFile);
        } catch (IOException e) {
            throw new RuntimeException("graphics2d 图片写入IO异常", e);
        }

        // 将文件加入资源容器中
        // 创建图片对象，为了保持透明图片的兼容性采用PNG格式
        CT_MultiMedia multiMedia = new CT_MultiMedia()
                .setType(MediaType.Image)
                .setFormat("PNG")
                .setMediaFile(resDir.getAbsLoc().cat(imgFile.getName()));

        return resMgr.addRawWithCache(multiMedia);
    }

    /**
     * 添加绘制参数至资源文件中
     *
     * @param dp 绘制参数
     * @return 资源对象ID
     */
    public ST_ID addDrawParam(CT_DrawParam dp) {
        return resMgr.addRawWithCache(dp);
    }

    /**
     * 生成新的文档内对象ID
     *
     * @return 文档内对象ID
     */
    public ST_ID newID() {
        return new ST_ID(MaxUnitID.incrementAndGet());
    }


    /**
     * 向文档中添加附件文件
     * <p>
     * 如果名称相同原有附件将会被替换
     *
     * @param file 附件文件路径
     * @throws IOException 文件操作异常
     */
    public void addAttachment(Path file) throws IOException {
        if (file == null || Files.notExists(file)) {
            return;
        }

        // 创建附件列表文件
        if (attachments == null) {
            attachments = new Attachments();
            docDir.putObj(DocDir.Attachments, attachments);
            document.setAttachments(docDir.getAbsLoc().cat(DocDir.Attachments));
        }

        String fileName = file.getFileName().toString();

        // 计算附件所占用的空间，单位KB。
        double size = (double) Files.size(file) / 1024d;
        CT_Attachment ctAttachment = new CT_Attachment()
                .setAttachmentName(fileName)
                .setID(String.valueOf(MaxUnitID.incrementAndGet()))
                .setCreationDate(LocalDateTime.now())
                .setSize(size);

        // 添加附件到资源
        docDir.addResource(file);
        // 构造附件文件存放路径
        ST_Loc loc = docDir.getRes().getAbsLoc().cat(fileName);
        ctAttachment.setFileLoc(loc);
        // 加入附件记录到列表文件
        attachments.addAttachment(ctAttachment);
    }

    /**
     * 向文档中添加附件文件
     *
     * @param filename 文件名
     * @param input    附件流
     * @throws IOException 文件操作异常
     */
    public void addAttachment(String filename, InputStream input) throws IOException {

    }


    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        } else {
            closed = true;
        }

        try {
            // 设置最大对象ID
            cdata.setMaxUnitID(MaxUnitID.get());
            // final. 执行打包程序
            if (outPath != null) {
                ofdDir.jar(outPath.toAbsolutePath());
            } else {
                throw new IllegalArgumentException("OFD文档输出地址错误或没有设置输出流");
            }
        } finally {
            if (ofdDir != null) {
                // 清除在生成OFD过程中的工作区产生的文件
                ofdDir.clean();
            }
        }
    }
}
