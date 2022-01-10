package org.ofdrw.tool.merge;

import org.dom4j.DocumentException;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * OFD文档上下文，用于在合并时提供文档相关信息
 *
 * @author 权观宇
 * @since 2021-11-09 20:47:34
 */
public class DocContext implements Closeable {

    public final OFDReader reader;
    public final ResourceManage resMgt;
    public final Path filepath;
    /**
     * 新旧映射表
     * <p>
     * | 对象在原文档中的ID | (新文档ID, 资源对象) |
     * <p>
     * Key: 旧ID
     * Value: 资源对象（ID替换为新文档中的ID）
     */
    final Map<String, OFDElement> resOldNewMap;

    private CT_PageArea defaultArea;


    public DocContext(Path filepath) throws IOException {
        this.filepath = filepath;
        this.reader = new OFDReader(filepath);
        this.resMgt = this.reader.getResMgt();
        this.resOldNewMap = new HashMap<>();
    }

    /**
     * 获取默认情况的页面信息
     *
     * @param numOfDoc 文档序号，一般为0
     * @return 页面尺寸信息
     * @throws DocumentException     文档解析异常
     * @throws FileNotFoundException 文件缺失
     */
    public CT_PageArea getDefaultArea(int numOfDoc) throws DocumentException, FileNotFoundException {
        if (defaultArea != null) {
            return defaultArea;
        }
        // 取0文档对象
        final Document document = reader.getDoc(numOfDoc);
        defaultArea = document.getCommonData().getPageArea();
        if (defaultArea == null) {
            // 若文档中没有尺寸信息，使用默认值
            defaultArea = PageLayout.A4().getPageArea();
        }
        return defaultArea;
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}
