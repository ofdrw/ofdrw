package org.ofdrw.layout;

import org.dom4j.DocumentException;
import org.ofdrw.core.attachment.Attachments;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.core.basicStructure.doc.CT_CommonData;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.gv.GlobalVar;
import org.ofdrw.layout.edit.AdditionVPage;
import org.ofdrw.layout.edit.Annotation;
import org.ofdrw.layout.edit.AnnotationRender;
import org.ofdrw.layout.edit.Attachment;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.engine.*;
import org.ofdrw.layout.engine.render.RenderException;
import org.ofdrw.layout.exception.DocReadException;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;
import org.ofdrw.reader.ResourceLocator;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Virtual Document 虚拟文档对象
 * <p>
 * 与 {@link org.ofdrw.core.basicStructure.doc.Document} 区别
 * <p>
 * 使用API的方式构造OFD文档，并打包为OFD文件。
 *
 * @author 权观宇
 * @since 2020-3-17 20:13:51
 */
public class OFDDoc implements Closeable {

    /**
     * 已有的OFD文档解析器
     * <p>
     * 仅在修改的模式有效
     */
    private OFDReader reader;
    /**
     * OFD 打包
     */
    private OFDDir ofdDir;
    /**
     * 打包后OFD文档存放路径
     * outPath/outStream，二选一
     */
    private Path outPath;
    /**
     * 打包后OFD文档输出流
     * outPath/outStream，二选一
     */
    private OutputStream outStream;
    /**
     * 当前文档中所有对象使用标识的最大值。
     * 初始值为 0。MaxUnitID主要用于文档编辑，
     * 在向文档增加一个新对象时，需要分配一个
     * 新的标识符，新标识符取值宜为 MaxUnitID + 1，
     * 同时需要修改此 MaxUnitID值。
     */
    private AtomicInteger MaxUnitID = new AtomicInteger(0);

    /**
     * 外部资源管理器
     */
    ResManager prm;

    /**
     * 注释渲染器
     * <p>
     * 仅在需要增加注释时进行初始化
     */
    private AnnotationRender annotationRender;

    /**
     * 流式布局元素队列
     */
    private LinkedList<Div> streamQueue = new LinkedList<>();

    /**
     * 固定布局虚拟页面队列
     */
    private LinkedList<VirtualPage> vPageList = new LinkedList<>();

    /**
     * 页面样式
     * <p>
     * 默认为 A4
     * <p>
     * 页边距：上下都是2.54厘米，左右都是3.17厘米。
     */
    private PageLayout pageLayout = PageLayout.A4();

    /**
     * 文档属性信息，该对象会在初始化是被创建并且添加到文档中
     * 此处只是保留引用，为了方便操作。
     */
    private CT_CommonData cdata;

    /**
     * 文档是否已经关闭
     * true 表示已经关闭，false 表示未关闭
     */
    private boolean closed = false;

    /**
     * OFD文档对象
     */
    private Document ofdDocument;


    /**
     * 在指定路径位置上创建一个OFD文件
     *
     * @param outPath OFD输出路径
     */
    public OFDDoc(Path outPath) {
        this();
        if (outPath == null) {
            throw new IllegalArgumentException("OFD文件存储路径(outPath)为空");
        }
        if (Files.isDirectory(outPath)) {
            throw new IllegalArgumentException("OFD文件存储路径(outPath)不能是目录");
        }
        if (!Files.exists(outPath.getParent())) {
            throw new IllegalArgumentException("OFD文件存储路径(outPath)上级目录 [" + outPath.getParent().toAbsolutePath() + "] 不存在");
        }
        this.outPath = outPath;
    }

    /**
     * 在指定路径位置上创建一个OFD文件
     *
     * @param outStream OFD输出流
     */
    public OFDDoc(OutputStream outStream) {
        this();
        if (outStream == null) {
            throw new IllegalArgumentException("OFD文件输出流(outStream)为空");
        }
        this.outStream = outStream;
    }

    /**
     * 修改一个OFD文档
     *
     * @param reader  OFD解析器
     * @param outPath 修改后文档生成位置
     * @throws DocReadException 文档读取异常
     */
    public OFDDoc(OFDReader reader, Path outPath) throws DocReadException {
        if (reader == null) {
            throw new IllegalArgumentException("OFD解析器(reader)不能为空");
        }
        if (outPath == null) {
            throw new IllegalArgumentException("OFD文件存储路径(outPath)为空");
        }
        if (Files.isDirectory(outPath)) {
            throw new IllegalArgumentException("OFD文件存储路径(outPath)不能是目录");
        }
        this.outPath = outPath;
        this.reader = reader;
        // 通过OFD解析器初始化文档对象
        try {
            containerInit(reader);
        } catch (FileNotFoundException | DocumentException e) {
            throw new DocReadException("OFD文件解析异常", e);
        }
    }

    /**
     * 修改一个OFD文档
     *
     * @param reader    OFD解析器
     * @param outStream 修改后文档输出流
     * @throws DocReadException 文档读取异常
     */
    public OFDDoc(OFDReader reader, OutputStream outStream) throws DocReadException {
        if (reader == null) {
            throw new IllegalArgumentException("OFD解析器(reader)不能为空");
        }
        if (outStream == null) {
            throw new IllegalArgumentException("OFD文件输出流(outStream)为空");
        }
        this.outStream = outStream;
        this.reader = reader;
        // 通过OFD解析器初始化文档对象
        try {
            containerInit(reader);
        } catch (FileNotFoundException | DocumentException e) {
            throw new DocReadException("OFD文件解析异常", e);
        }
    }

    /**
     * 文档初始化构造器
     */
    private OFDDoc() {
        // 初始化文档对象
        containerInit();
    }


    /**
     * 设置页面默认的样式
     *
     * @param pageLayout 页面默认样式
     * @return this
     */
    public OFDDoc setDefaultPageLayout(PageLayout pageLayout) {
        if (pageLayout == null) {
            return this;
        }
        this.pageLayout = pageLayout;
        // 设置页面大小
        cdata.setPageArea(pageLayout.getPageArea());
        return this;
    }

    /**
     * 初始化OFD虚拟容器
     */
    private void containerInit() {
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
        ofdDocument = new Document();
        cdata = new CT_CommonData();
        // 默认使用RGB颜色空间所以此处不设置颜色空间
        // 设置页面属性
        this.setDefaultPageLayout(this.pageLayout);
        ofdDocument.setCommonData(cdata)
                // 空的页面引用集合，该集合将会在解析虚拟页面时得到填充
                .setPages(new Pages());

        ofdDir = OFDDir.newOFD()
                .setOfd(ofd);
        // 创建一个新的文档
        DocDir docDir = ofdDir.newDoc();
        docDir.setDocument(ofdDocument);
        prm = new ResManager(docDir, MaxUnitID);
    }

    /**
     * 通过已有文档初始化文档容器
     *
     * @param reader OFD解析器
     */
    private void containerInit(OFDReader reader) throws FileNotFoundException, DocumentException {
        ofdDir = reader.getOFDDir();
        OFD ofd = ofdDir.getOfd();
        DocBody docBody = ofd.getDocBody();
        CT_DocInfo docInfo = docBody.getDocInfo();
        // 设置文档修改时间
        docInfo.setModDate(LocalDate.now());
        // 资源定位器
        ResourceLocator rl = reader.getResourceLocator();
        // 找到 Document.xml文件并且序列化
        ST_Loc docRoot = docBody.getDocRoot();
        ofdDocument = rl.get(docRoot, Document::new);
        // 取出文档修改前的文档最大ID
        cdata = ofdDocument.getCommonData();
        ST_ID maxUnitID = cdata.getMaxUnitID();
        // 设置当前文档最大ID
        MaxUnitID = new AtomicInteger(maxUnitID.getId().intValue());
        prm = new ResManager(ofdDir.obtainDocDefault(), MaxUnitID);
    }

    /**
     * 向文档中加入元素
     * <p>
     * 适合于流式布局
     *
     * @param item 元素
     * @return this
     */
    public OFDDoc add(Div item) {
        streamQueue.add(item);
        return this;
    }

    /**
     * 向文档中加入虚拟页面
     * <p>
     * 适合于固定布局
     *
     * @param virtualPage 虚拟页面
     * @return this
     */
    public OFDDoc addVPage(VirtualPage virtualPage) {
        vPageList.add(virtualPage);
        return this;
    }

    /**
     * 获取指定页面追加页面对象
     * <p>
     * 并且追加到虚拟页面列表中
     *
     * @param pageNum 页码
     * @return 追加页面对象
     */
    public AdditionVPage getAVPage(int pageNum) {
        if (reader == null) {
            throw new RuntimeException("仅在修改模式下允许获取追加页面对象（AdditionVPage）");
        }
        // 获取页面的OFD对象
        Page page = reader.getPage(pageNum);
        // 构造追加页面对象
        AdditionVPage avp = new AdditionVPage(page);
        // 自动加入到虚拟页面列表中
        this.addVPage(avp);
        return avp;
    }

    /**
     * 向页面中增加注释对象
     *
     * @param pageNum    页码
     * @param annotation 注释对象
     * @return this
     * @throws IOException     文件操作异常
     * @throws RenderException 渲染异常
     */
    public OFDDoc addAnnotation(int pageNum, Annotation annotation) throws IOException {
        if (annotation == null) {
            return this;
        }

        if (reader == null) {
            throw new RuntimeException("仅在修改模式下允许获取追加注释对象，请使用reader构造");
        }
        if (annotationRender == null) {
            annotationRender = new AnnotationRender(reader.getOFDDir().obtainDocDefault(), prm, MaxUnitID);
        }
        // 获取页面信息
        PageInfo pageInfo = reader.getPageInfo(pageNum);
        // 渲染注释内容
        annotationRender.render(pageInfo, annotation);
        return this;
    }

    /**
     * 获取页面样式
     *
     * @return 页面样式
     */
    public PageLayout getPageLayout() {
        return pageLayout;
    }

    /**
     * 向文档中添加附件文件
     * <p>
     * 如果名称相同原有附件将会被替换
     *
     * @param attachment 附件文件对象
     * @return this
     * @throws IOException 文件操作异常
     */
    public OFDDoc addAttachment(Attachment attachment) throws IOException {
        if (attachment == null) {
            return this;
        }
        DocDir docDefault = ofdDir.obtainDocDefault();
        Path file = attachment.getFile();
        docDefault.addResource(file);
        // 构造附件文件存放路径
        ST_Loc loc = docDefault.getRes().getAbsLoc()
                .cat(file.getFileName().toString());
        // 计算附件所占用的空间，单位KB。
        double size = Files.size(file) / 1024d;
        CT_Attachment ctAttachment = attachment.getAttachment()
                .setID(String.valueOf(MaxUnitID.incrementAndGet()))
                .setCreationDate(LocalDate.now())
                .setSize(size)
                .setFileLoc(loc);
        ResourceLocator rl = new ResourceLocator(docDefault);
        //这边应该加个Doc_0目录,不然后面读取document中Attachments如果写的相对路径还是从根目录找就会找不到的。add by daiwf
        // 获取附件目录，并切换目录到与附件列表文件同级
        Attachments attachments = obtainAttachments(docDefault, rl);
        // 清理已经存在的同名附件
        cleanOldAttachment(rl, attachments, attachment.getName());
        // 加入附件记录
        attachments.addAttachment(ctAttachment);
        return this;
    }

    /**
     * 清理已经存在的资源
     *
     * @param rl          资源加载器
     * @param attachments 附件列表
     * @param name        附件名称
     */
    private void cleanOldAttachment(ResourceLocator rl,
                                    Attachments attachments,
                                    String name) throws IOException {
        final List<CT_Attachment> list = attachments.getAttachments();
        for (CT_Attachment att : list) {
            // 找到匹配的附件
            if (att.getAttachmentName().equals(name)) {
                // 删除附件记录
                attachments.remove(att);
                // 删除附件的文件
                ST_Loc fileLoc = att.getFileLoc();
                Path file = rl.getFile(fileLoc);
                if (file != null && Files.exists(file)) {
                    Files.delete(file);
                }
                break;
            }
        }
    }

    /**
     * 获取附件列表文件，如果文件不存在则创建
     * <p>
     * 该操作将会切换资源加载器到与附件文件同级的位置
     *
     * @param rl     资源加载器
     * @param docDir 文档目录
     * @return 附件列表文件
     */
    private Attachments obtainAttachments(DocDir docDir, ResourceLocator rl) {
        ST_Loc attLoc = ofdDocument.getAttachments();
        Attachments attachments = null;
        if (attLoc != null) {
            try {
                attachments = rl.get(attLoc, Attachments::new);
                // 切换目录到资源文件所在目录
                rl.cd(attLoc.parent());
            } catch (DocumentException | FileNotFoundException e) {
                // 忽略错误
                System.err.println(">> 无法解析Attachments.xml文件，将重新创建该文件");
                attachments = null;
            }
        }
        if (attachments == null) {
            attachments = new Attachments();
            docDir.putObj(DocDir.Attachments, attachments);
            ofdDocument.setAttachments(docDir.getAbsLoc().cat(DocDir.Attachments));
        }
        return attachments;
    }

    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        } else {
            closed = true;
        }

        try {
            if (!streamQueue.isEmpty()) {
                /*
                 * 将流式布局转换为板式布局
                 */
                SegmentationEngine sgmEngine = new SegmentationEngine(pageLayout);
                StreamingLayoutAnalyzer analyzer = new StreamingLayoutAnalyzer(pageLayout);
                // 1. 流式布局队列经过分段引擎，获取分段队列
                List<Segment> sgmQueue = sgmEngine.process(streamQueue);
                // 2. 段队列进入布局分析器，构造基于固定布局的虚拟页面。
                List<VirtualPage> virtualPageList = analyzer.analyze(sgmQueue);
                vPageList.addAll(virtualPageList);
            }

            if (!vPageList.isEmpty()) {
                DocDir docDefault = ofdDir.obtainDocDefault();
                // 创建虚拟页面解析引擎，并持有文档上下文。
                VPageParseEngine parseEngine = new VPageParseEngine(pageLayout, docDefault, prm, MaxUnitID);
                // 解析虚拟页面
                parseEngine.process(vPageList);
            } else if (annotationRender == null && reader == null) {
                // 虚拟页面为空，也没有注解对象，也不是编辑模式，那么空的操作报错
                throw new IllegalStateException("OFD文档中没有页面，无法生成OFD文档");
            }

            // 设置最大对象ID
            cdata.setMaxUnitID(MaxUnitID.get());
            // final. 执行打包程序
            if (outPath != null) {
                ofdDir.jar(outPath.toAbsolutePath());
            } else if (outStream != null) {
                ofdDir.jar(outStream);
            } else {
                throw new IllegalArgumentException("OFD文档输出地址错误或没有设置输出流");
            }
        } finally {
            if (reader != null) {
                reader.close();
            } else if (ofdDir != null) {
                // 清除在生成OFD过程中的工作区产生的文件
                ofdDir.clean();
            }
        }
    }
}
