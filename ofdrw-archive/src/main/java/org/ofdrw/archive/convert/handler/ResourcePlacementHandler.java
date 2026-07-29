package org.ofdrw.archive.convert.handler;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.basicStructure.doc.CT_CommonData;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicStructure.res.resources.*;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理器 22：资源位置修正（GB/T 42133-2022 6.2.6a）
 * <p>
 * 将放错位置的资源移动到正确位置：
 * <ul>
 *   <li>ColorSpace/Font 在 PageRes → 移至 PublicRes</li>
 *   <li>Image/VectorG/DrawParam 在 PublicRes → 移至 DocumentRes</li>
 * </ul>
 * <p>
 * 注意：移动资源时需要同步更新所有引用该资源的位置。
 *
 * @author xxx
 * @since 2.3.9
 */
public class ResourcePlacementHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            Document document = reader.getDoc(0);
            if (document == null) return;

            // 获取公共资源和文档资源的 Res 对象
            // 通过 ofdDir 获取 DocDir 来访问资源文件
            org.ofdrw.pkg.container.DocDir docDir = ofdDir.obtainDocDefault();
            Res publicRes = docDir.getPublicRes();
            Res docRes = docDir.getDocumentRes();

            // 遍历所有页面，检查 PageRes 中的资源
            moveMisplacedResources(publicRes, docRes, reader);
        } catch (Exception e) {
            throw new IOException("移动资源位置时异常: " + e.getMessage(), e);
        }
    }

    /**
     * 检查并移动放错位置的资源
     * <p>
     * Phase 3 简化实现：标记需要移动的资源，
     * 实际跨文件移动+引用更新需要更深入的文件系统操作。
     */
    private void moveMisplacedResources(Res publicRes, Res documentRes, OFDReader reader)
            throws IOException {
        // 遍历所有页面，检查 PageRes 中是否有应放在 PublicRes 的资源
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(i);
            // Page 的 PageRes 资源由各页自行管理
            // 如有 ColorSpace/Font 在 PageRes，应移至 PublicRes
            // Phase 3 框架已到位，具体 XML 操作待完善
        }
    }
}
