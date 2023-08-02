package org.ofdrw.tool.merge;


import org.dom4j.DocumentException;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicStructure.pageTree.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceLocator;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * OFD文档页面删除器
 * <p>
 * 该删除器仅删除文档树中的页面节点，不会删除相关资源和页面对象 Content.xml
 *
 * @author 权观宇
 * @since 2023-8-2 19:31:00
 */
public class OFDPageDeleter implements Closeable {

    /**
     * OFD文档虚拟容器
     */
    private final OFDDir ofdDir;
    /**
     * OFD.xml 主入口
     */
    private final OFD ofd;
    /**
     * OFD文档根节点
     */
    private final Document ofdRoot;

    /**
     * OFD解析器
     */
    private final OFDReader reader;
    /**
     * 输出路径
     */
    private final Path outPath;

    /**
     * 创建OFD文档页面删除器
     *
     * @param src OFD文件路径
     * @param out 删除后文件路径
     * @throws IOException       文件解析异常
     * @throws DocumentException 文档结构无法解析
     */
    public OFDPageDeleter(Path src, Path out) throws IOException, DocumentException {
        if (src == null || !src.toFile().exists()) {
            throw new IllegalArgumentException("OFD文件不存在");
        }
        if (out == null) {
            throw new IllegalArgumentException("输出路径（out）为空");
        }

        this.reader = new OFDReader(src);
        this.outPath = out;
        this.ofdDir = reader.getOFDDir();
        this.ofd = ofdDir.getOfd();
        // 资源定位器
        ResourceLocator rl = reader.getResourceLocator();
        DocBody docBody = ofd.getDocBody();
        // 找到 Document.xml文件并且序列化
        ST_Loc docRoot = docBody.getDocRoot();
        this.ofdRoot = rl.get(docRoot, Document::new);
    }


    /**
     * 删除指定索引的页面
     *
     * @param indexes 页面索引列表（从0起）
     * @return this
     */
    public OFDPageDeleter delete(int... indexes) {
        if (indexes == null || indexes.length == 0) {
            return this;
        }
        Pages pages = this.ofdRoot.getPages();
        List<Page> tbd = new ArrayList<>();
        for (int index : indexes) {
            for (int i = 0; i < pages.getPages().size(); i++) {
                if (i == index) {
                    tbd.add(pages.getPageByIndex(i));
                }
            }
        }
        for (Page page : tbd) {
            pages.remove(page);
        }
        return this;
    }


    /**
     * 保存删除后的文档，并关闭删除器
     * <p>
     * 注意：请在所有操作完成后调用该方法，否则无法删除页面。
     *
     * @throws IOException 文件读写异常
     */
    @Override
    public void close() throws IOException {
        DocBody docBody = ofd.getDocBody();
        CT_DocInfo docInfo = docBody.getDocInfo();
        // 设置文档修改时间
        docInfo.setModDate(LocalDate.now());

        // final. 执行打包程序
        if (outPath != null) {
            ofdDir.jar(outPath.toAbsolutePath());
        }

        if (reader != null) {
            reader.close();
        }
    }
}
