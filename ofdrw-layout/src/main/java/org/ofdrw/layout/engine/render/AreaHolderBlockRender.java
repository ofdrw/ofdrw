package org.ofdrw.layout.engine.render;

import org.dom4j.DocumentException;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.layout.areaholder.AreaHolderBlocks;
import org.ofdrw.layout.areaholder.AreaHolderBlocksProcess;
import org.ofdrw.layout.areaholder.CT_AreaHolderBlock;
import org.ofdrw.layout.element.AreaHolderBlock;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.engine.ResManager;
import org.ofdrw.pkg.container.DocDir;

import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AreaHolderBlock 渲染器
 * <p>
 * {@link org.ofdrw.layout.element.AreaHolderBlock} 的渲染器
 *
 * @author 权观宇
 * @since 2023-10-28 12:30:12
 */
public class AreaHolderBlockRender implements Processor {

    /**
     * 执行 AreaHolderBlockRender 渲染器
     * <p>
     * 1. 创建入口
     * <p>
     * 2. 向页面加入的页块用于占位。
     *
     * @param docDir    文档目录，用于设置位置信息
     * @param pageLoc   页面位置
     * @param ahBlock   区域占位区块
     * @param layer     将要渲染到的图层
     * @param maxUnitID 最大ID
     * @throws RenderException 渲染发生错误
     */
    public static void render(DocDir docDir, ST_Loc pageLoc, CT_PageBlock layer, AreaHolderBlock ahBlock, AtomicInteger maxUnitID) throws RenderException {
        CT_PageBlock block = new CT_PageBlock();
        ST_ID objId = new ST_ID(maxUnitID.incrementAndGet());
        block.setObjID(objId);
        layer.addPageBlock(block);

        try {
            Double[] border = ahBlock.getBorder();
            Double[] padding = ahBlock.getPadding();
            // 获取区域占位区块列表文件
            AreaHolderBlocks blocks = AreaHolderBlocksProcess.obtian(docDir);
            // 构造占位区域位置以及大小
            ST_Box boundary = new ST_Box(
                    ahBlock.getX() + border[0] + padding[0],
                    ahBlock.getY() + border[1] + padding[1],
                    ahBlock.getWidth(),
                    ahBlock.getHeight()
            );
            // 创建 区域占位区块
            CT_AreaHolderBlock obj = new CT_AreaHolderBlock(ahBlock.getAreaName())
                    .setBoundary(boundary)
                    .setPageFile(pageLoc)
                    .setPageBlockID(objId.ref());
            obj.setObjID(maxUnitID.incrementAndGet());
            // 添加到列表中
            blocks.addAreaHolderBlock(obj);
        } catch (DocumentException | FileNotFoundException e) {
            throw new RenderException("区域占位区块列表文件获取失败 ", e);
        }
    }


    /**
     * 处理占位区域
     *
     * @param pageLoc    页面在虚拟容器中绝对路径。
     * @param layer      占位符所在图层。
     * @param resManager 资源管理器
     * @param e          OFDRW元素
     * @param maxUnitID  最大元素ID提供器
     * @throws RenderException 渲染发生错误
     */
    @Override
    public void render(ST_Loc pageLoc, CT_PageBlock layer, ResManager resManager, Div e, AtomicInteger maxUnitID) throws RenderException {
        if (e instanceof AreaHolderBlock) {
            render(resManager.getDocDir(), pageLoc, layer, (AreaHolderBlock) e, maxUnitID);
        }
    }
}

