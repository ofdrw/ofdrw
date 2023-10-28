package org.ofdrw.layout.areaholder;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.pkg.container.DocDir;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 区域占位区块处理器
 * <p>
 * 用于解决区域占位区块 写入生成等操作的工具集合
 *
 * @author 权观宇
 * @since 2023-10-28 15:40:19
 */
public class AreaHolderBlocksProcess {


    /**
     * 尝试获取  区域占位区块列表
     *
     * @param docDir 文档目录
     * @return 区域占位区块列表
     * @throws DocumentException     文档结构无法解析
     * @throws FileNotFoundException 文件不存在
     */
    public static AreaHolderBlocks obtian(DocDir docDir) throws DocumentException, FileNotFoundException {
        if (docDir == null) {
            throw new IllegalArgumentException("文档目录（docDir）为空");
        }

        if (exist(docDir)) {
            return get(docDir);
        } else {
            return create(docDir);
        }
    }

    /**
     * 创建 区域占位区块列表
     * <p>
     * 如文件已经存在还将覆盖该文件
     *
     * @param docDir 文档目录
     * @return 区域占位区块列表
     */
    public static AreaHolderBlocks create(DocDir docDir) {
        if (docDir == null) {
            throw new IllegalArgumentException("文档目录（docDir）为空");
        }
        AreaHolderBlocks areaHolderBlocks = new AreaHolderBlocks();
        docDir.putObj(AreaHolderBlocks.AreaHolderBlocksFile, areaHolderBlocks);
        return areaHolderBlocks;
    }

    /**
     * 获取 区域占位区块列表
     *
     * @param docDir 文档目录
     * @return 区域占位区块列表
     * @throws DocumentException     文档结构无法解析
     * @throws FileNotFoundException 文件不存在
     */
    public static AreaHolderBlocks get(DocDir docDir) throws DocumentException, FileNotFoundException {
        Element obj = docDir.getObj(AreaHolderBlocks.AreaHolderBlocksFile);
        return new AreaHolderBlocks(obj);
    }


    /**
     * 判断是否存在 区域占位区块列表
     *
     * @param docDir 文档目录
     * @return true - 存在类表文件；false - 不存在列表文件
     */
    public static boolean exist(DocDir docDir) {
        if (docDir == null) {
            return false;
        }
        return docDir.exist(AreaHolderBlocks.AreaHolderBlocksFile);
    }

    /**
     * 查找指定名称的区域占位区块
     * <p>
     * 若出现了多个同名的 区域占位区块 则返回第一个
     *
     * @param blocks   区域占位区块列表
     * @param areaName 区域名称
     * @return 区域占位区块
     */
    public static CT_AreaHolderBlock find(AreaHolderBlocks blocks, String areaName) {
        if (blocks == null || areaName == null) {
            return null;
        }
        for (CT_AreaHolderBlock block : blocks.getAreaHolderBlocks()) {
            if (areaName.equals(block.getAreaName())) {
                return block;
            }
        }
        return null;
    }


    /**
     * 删除区域占位区块列表文件
     *
     * @param docDir 文档目录
     * @throws IOException IO异常
     */
    public static void delete(DocDir docDir) throws IOException {
        if (docDir == null) {
            return;
        }
        if (exist(docDir)) {
            docDir.deleteFile(AreaHolderBlocks.AreaHolderBlocksFile);
        }
    }
}
