package org.ofdrw.layout;

import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.layout.element.Div;
import org.ofdrw.pkg.dir.DocDir;
import org.ofdrw.pkg.dir.OFDDir;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Document implements Closeable {

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
     */
    private PageLayout pageLayout = PageLayout.A4;


    private Document() {
        this.streamQueue = new LinkedList<>();
        this.vPageList = new LinkedList<>();
        containerInit();
    }


    /**
     * 设置页面默认的样式
     *
     * @param pageLayout 页面默认样式
     * @return this
     */
    public Document setDefaultPageLayout(PageLayout pageLayout) {
        if (pageLayout == null) {
            return this;
        }
        this.pageLayout = pageLayout;
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

        ofdDir = new OFDDir()
                .setOfd(ofd)
                .add(new DocDir());
    }

    /**
     * 向文档中加入元素
     * <p>
     * 适合于流式布局
     *
     * @param item 元素
     * @return this
     */
    public Document add(Div item) {
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
    public Document addVPage(VirtualPage virtualPage) {
        vPageList.add(virtualPage);
        return this;
    }

    @Override
    public void close() throws IOException {

    }
}
