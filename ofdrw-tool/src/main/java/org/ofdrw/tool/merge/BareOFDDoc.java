package org.ofdrw.tool.merge;

import org.ofdrw.core.basicStructure.doc.CT_CommonData;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.gv.GlobalVar;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.engine.*;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.OFDDir;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 裸OFD文档对象，用于提供较为底层的OFD文档操作行为
 *
 * @author 权观宇
 * @since 2020-3-17 20:13:51
 */
public class BareOFDDoc implements Closeable {

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
    public final AtomicInteger MaxUnitID = new AtomicInteger(0);

    /**
     * 外部资源管理器
     */
    public final ResManager prm;

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
     * 正在操作的文档目录
     */
    public final DocDir docDir;


    /**
     * 在指定路径位置上创建一个OFD文件
     *
     * @param outPath OFD输出路径
     */
    public BareOFDDoc(Path outPath) {
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
     * 文档初始化构造器
     */
    private BareOFDDoc() {
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
        // 默认使用RGB颜色空间所以此处不设置颜色空间
        // 设置页面属性
        cdata.setPageArea(PageLayout.A4().getPageArea());
        document.setCommonData(cdata)
                // 空的页面引用集合，该集合将会在解析虚拟页面时得到填充
                .setPages(new Pages());

        ofdDir = OFDDir.newOFD()
                .setOfd(ofd);
        // 创建一个新的文档
        DocDir docDir = ofdDir.newDoc();
        this.docDir = docDir;
        docDir.setDocument(document);
        prm = new ResManager(docDir, MaxUnitID);
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
