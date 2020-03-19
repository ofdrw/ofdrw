package org.ofdrw.layout;

import org.ofdrw.core.basicStructure.doc.CT_CommonData;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.engine.Segment;
import org.ofdrw.layout.engine.SegmentationEngine;
import org.ofdrw.layout.engine.StreamingLayoutAnalyzer;
import org.ofdrw.pkg.dir.DocDir;
import org.ofdrw.pkg.dir.OFDDir;

import java.io.Closeable;
import java.io.IOException;
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
    private LinkedList<Div> streamQueue;
    /**
     * 固定布局虚拟页面队列
     */
    private LinkedList<VirtualPage> vPageList;
    /**
     * 页面样式
     * <p>
     * 默认为 A4
     * <p>
     * 页边距：上下都是2.54厘米，左右都是3.17厘米。
     */
    private PageLayout pageLayout = PageLayout.A4;

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
     * 文档初始化构造器
     */
    private OFDDoc() {
        this.streamQueue = new LinkedList<>();
        this.vPageList = new LinkedList<>();
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
        // 设置页面区域
        CT_PageArea pageArea = new CT_PageArea()
                // 物理区域为实际页面大小
                .setPhysicalBox(0, 0, pageLayout.getWidth(), pageLayout.getHeight())
                // 显示区域为减去margin的区域
                .setApplicationBox(pageLayout.getMarginLeft(),
                        pageLayout.getMarginTop(),
                        pageLayout.contentWidth(),
                        pageLayout.contentHeight());
        // 设置页面大小
        cdata.setPageArea(pageArea);
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
                .setCreatorVersion("1.0.0-SNAPSHOT");
        DocBody docBody = new DocBody()
                .setDocInfo(docInfo)
                .setDocRoot(new ST_Loc("Doc_0/Document.xml"));
        OFD ofd = new OFD().addDocBody(docBody);

        // 创建一个低层次的文档对象
        Document lowDoc = new Document();
        cdata = new CT_CommonData()
                .setMaxUnitID(0)
                // 由于有字形资源所以一定存在公共资源，这里县创建
                .setPublicRes(new ST_Loc("PublicRes.xml"));
        // 默认使用RGB颜色空间所以此处设置颜色空间
        // 设置页面属性
        this.setDefaultPageLayout(this.pageLayout);
        lowDoc.setCommonData(cdata)
                // 空的页面引用集合，该集合将会在解析虚拟页面时得到填充
                .setPages(new Pages());

        DocDir docDir = new DocDir()
                .setDocument(lowDoc);
        ofdDir = new OFDDir()
                .setOfd(ofd)
                .add(docDir);
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

    @Override
    public void close() throws IOException {
        if (!streamQueue.isEmpty()) {
            /*
            将流式布局转换为板式布局
            */
            SegmentationEngine sgmEngine = new SegmentationEngine(pageLayout);
            StreamingLayoutAnalyzer analyzer = new StreamingLayoutAnalyzer(pageLayout);
            // 1. 流式布局队列经过分段引擎，获取分段队列
            List<Segment> sgmQueue = sgmEngine.process(streamQueue);
            // 2. 段队列进入布局分析器，构造基于固定布局的虚拟页面。
            List<VirtualPage> virtualPageList = analyzer.analyze(sgmQueue);
            vPageList.addAll(virtualPageList);
        }
        // TODO 将虚拟页面解析为OFD语言，写入的OFD对象中 2020-3-17 20:13:34

        if (vPageList.isEmpty()) {
            throw new IllegalStateException("OFD文档中没有页面，无法生成OFD文档");
        }
        // 设置最大对象ID
        cdata.setMaxUnitID(MaxUnitID.get());

        // final. 执行打包程序
        String base = outPath.toAbsolutePath().getParent().toString();
        String fileName = outPath.getFileName().toString();
        ofdDir.jar(base, fileName);
    }
}
