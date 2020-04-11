package org.ofdrw.layout;

import org.dom4j.DocumentException;
import org.ofdrw.core.basicStructure.doc.CT_CommonData;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.layout.edit.AdditionVPage;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.engine.*;
import org.ofdrw.layout.exception.DocReadException;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceLocator;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
     */
    private Path outPath;
    /**
     * 当前文档中所有对象使用标识的最大值。
     * 初始值为 0。MaxUnitID主要用于文档编辑，
     * 在向文档增加一个新对象时，需要分配一个
     * 新的标识符，新标识符取值宜为 MaxUnitID + 1，
     * 同时需要修改此 MaxUnitID值。
     */
    private AtomicInteger MaxUnitID = new AtomicInteger(0);
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
        this.outPath = outPath;
    }

    /**
     * 修改一个OFD文档
     *
     * @param reader  OFD解析器
     * @param outPath 修改后文档生成位置
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
                .setCreatorVersion("1.1.0-SNAPSHOT");
        DocBody docBody = new DocBody()
                .setDocInfo(docInfo)
                .setDocRoot(new ST_Loc("Doc_0/Document.xml"));
        OFD ofd = new OFD().addDocBody(docBody);

        // 创建一个低层次的文档对象
        Document lowDoc = new Document();
        cdata = new CT_CommonData()
                // 由于有字形资源所以一定存在公共资源，这里县创建
                .setPublicRes(new ST_Loc("PublicRes.xml"));
        // 默认使用RGB颜色空间所以此处不设置颜色空间
        // 设置页面属性
        this.setDefaultPageLayout(this.pageLayout);
        lowDoc.setCommonData(cdata)
                // 空的页面引用集合，该集合将会在解析虚拟页面时得到填充
                .setPages(new Pages());

        ofdDir = OFDDir.newOFD()
                .setOfd(ofd);
        // 创建一个新的文档
        ofdDir.newDoc().setDocument(lowDoc);
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
        Document doc = rl.get(docRoot, Document::new);
        // 取出文档修改前的文档最大ID
        cdata = doc.getCommonData();
        ST_ID maxUnitID = cdata.getMaxUnitID();
        // 设置当前文档最大ID
        MaxUnitID = new AtomicInteger(maxUnitID.getId().intValue());
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
     * 获取页面样式
     *
     * @return 页面样式
     */
    public PageLayout getPageLayout() {
        return pageLayout;
    }

    @Override
    public void close() throws IOException {
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
            if (vPageList.isEmpty()) {
                throw new IllegalStateException("OFD文档中没有页面，无法生成OFD文档");
            }
            DocDir docDefault = ofdDir.obtainDocDefault();
            ResManager prm = new ResManager(docDefault, MaxUnitID);
            // 创建虚拟页面解析引擎，并持有文档上下文。
            VPageParseEngine parseEngine = new VPageParseEngine(pageLayout, docDefault, prm, MaxUnitID);
            // 解析虚拟页面
            parseEngine.process(vPageList);
            // 设置最大对象ID
            cdata.setMaxUnitID(MaxUnitID.get());
            // final. 执行打包程序
            ofdDir.jar(outPath.toAbsolutePath());
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
