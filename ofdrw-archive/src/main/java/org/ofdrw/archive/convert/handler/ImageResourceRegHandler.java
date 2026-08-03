package org.ofdrw.archive.convert.handler;

import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.io.IOException;

/**
 * 处理器 24：多页共用图像注册优化（GB/T 42133-2022 6.5a）
 * <p>
 * 将多个页面共同引用的栅格图像从 PageRes 移至 DocumentRes，
 * 减少重复嵌入，优化存储。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class ImageResourceRegHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        ResourceManage resMgt = reader.getResMgt();
        if (resMgt == null) return;

        // Phase 3: 框架到位
        // 需要：
        // 1. 遍历所有页面的 Content.xml，统计每个图像 ResourceID 被引用次数
        // 2. 找出被 ≥2 页面共用的图像
        // 3. 将这些图像从 PageRes 移至 DocumentRes
        // 4. 更新各页面中的 ResourceID 引用路径
        // 5. 移动物理图像文件到 Res/ 目录
    }
}
