package org.ofdrw.archive.convert.handler;

import org.dom4j.Element;
import org.dom4j.Namespace;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.Const;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.MediaType;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理器 10：删除图像自定义扩展数据（GB/T 42133-2022 6.2.6f）
 * <p>
 * 遍历所有 Image 类型的 MultiMedia，删除非标准 OFD 命名空间的子元素。
 *
 * @author xxx
 * @since 2.3.9
 */
public class ImageExtensionHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        ResourceManage resMgt = reader.getResMgt();
        if (resMgt == null) return;

        List<CT_MultiMedia> mediaList = resMgt.getMultiMedias();
        if (mediaList == null || mediaList.isEmpty()) return;

        for (CT_MultiMedia media : mediaList) {
            if (media.getType() != MediaType.Image) continue;

            // 收集非 OFD 命名空间的子元素并删除
            @SuppressWarnings("unchecked")
            List<Element> children = new ArrayList<>(media.elements());
            for (Element child : children) {
                Namespace ns = child.getNamespace();
                if (ns != null && !Const.OFD_NAMESPACE_URI.equals(ns.getURI())) {
                    child.detach();
                }
            }
        }
    }
}
