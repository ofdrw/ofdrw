package org.ofdrw.layout.areaholder;


import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.edit.AdditionVPage;
import org.ofdrw.layout.element.canvas.Canvas;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.reader.ResourceLocator;

import java.io.FileNotFoundException;

/**
 * 区域占位区块上下文
 *
 * @author 权观宇
 * @since 2023-10-28 16:16:02
 */
public class AreaHolderContext {

    /**
     * 文档上下文
     */
    private final OFDDoc doc;


    /**
     * 正在操作的文档目录
     */
    private final DocDir docDir;

    /**
     * 资源加载器
     */
    private final ResourceLocator rl;


    /**
     * 创建 区域占位区块 上下文
     *
     * @param doc 文档上下文
     * @param n   文档序号
     * @throws FileNotFoundException 文档不存在
     */
    public AreaHolderContext(OFDDoc doc, int n) throws FileNotFoundException {
        this.rl = new ResourceLocator(doc.getOfdDir());
        this.doc = doc;
        this.docDir = doc.getOfdDir().getDocByIndex(n);
    }

    /**
     * 创建 区域占位区块 上下文
     *
     * @param doc 文档上下文
     */
    public AreaHolderContext(OFDDoc doc) {
        this.rl = new ResourceLocator(doc.getOfdDir());
        this.doc = doc;
        this.docDir = doc.getOfdDir().obtainDocDefault();
    }


    /**
     * 获取指定名称的 区域占位区块
     * <p>
     * 注意：
     * <p>
     * 获取的Canvas无法设置图层，图层由 区域占位区块 在设置时指定不可更改。
     * <p>
     * 获取到的Canvas 你需要手动Add到文档中才可生效。
     *
     * @param areaName 区域名称
     * @return 图形绘制器，注意：如果区域不存在则返回null
     */
    public Canvas get(String areaName) {
        // 不存在 区域占位区块列表
        if (AreaHolderBlocksProcess.exist(docDir) == false) {
            return null;
        }
        try {
            // 获取区域占位区块列表文件
            AreaHolderBlocks areaHolderBlocks = AreaHolderBlocksProcess.get(docDir);
            // 获取区域占位区块
            CT_AreaHolderBlock holder = AreaHolderBlocksProcess.find(areaHolderBlocks, areaName);
            if (holder == null) {
                // 未能找到区域
                return null;
            }
            ST_RefID blockId = holder.getPageBlockID();
            ST_Loc pageLoc = holder.getFontFile();
            ST_Box boundary = holder.getBoundary();
            if (blockId == null || pageLoc == null || boundary == null) {
                return null;
            }
            if (boundary.getHeight() <= 0 || boundary.getWidth() <= 0) {
                // 无效绘制区域
                return null;
            }

            // 获取页面 对象
            Page page = null;
            try {
                page = this.rl.get(pageLoc, Page::new);
            } catch (DocumentException | FileNotFoundException e) {
                return null;
            }
            if (page == null) {
                return null;
            }
            // 通过ID从页面中找到PageBlock对象作为容器
            Element element = (Element) page.selectSingleNode("//*[@ID='" + blockId.toString() + "']");
            if (element == null) {
                return null;
            }
            CT_PageBlock block = new CT_PageBlock(element);

            // 创建绘制器
            Canvas canvas = new Canvas(
                    boundary.getTopLeftX(), boundary.getTopLeftY(),
                    boundary.getWidth(), boundary.getHeight()
            );

//            // DEBUG: 绘制区域边框
//            canvas.setBorder(0.1);
//            canvas.setBorderColor(255, 0, 0);

            canvas.setPreferBlock(block);

            AdditionVPage virtualPage = new AdditionVPage(page, pageLoc);
            virtualPage.add(canvas);
            doc.addVPage(virtualPage);
            return canvas;
        } catch (DocumentException | FileNotFoundException e) {
            throw new IllegalArgumentException("区域占位区块列表文件获取失败 ", e);
        }
    }
}
