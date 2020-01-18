package org.ofdrw.pkg.dir;

import org.ofdrw.core.basicStructure.ofd.OFD;

import java.util.ArrayList;
import java.util.List;

/**
 * OFD文档对象
 */
public class OFDDir {
    /**
     * 文档容器列表
     */
    private List<DocDir> container;

    /**
     * 文档主入口
     */
    private OFD ofd;

    public OFDDir() {
        container = new ArrayList<>(1);
    }

    /**
     * 增加文档容器
     *
     * @param docDir 文档容器
     * @return this
     */
    public OFDDir add(DocDir docDir) {
        if (docDir == null) {
            return this;
        }
        if (container == null) {
            container = new ArrayList<>(1);
        }
        container.add(docDir);
        return this;
    }

    /**
     * 获取文档容器
     *
     * @param numberOf 第几个
     * @return 文档容器
     */
    public DocDir getDoc(Integer numberOf) {
        if (container == null) {
            return null;
        }
        for (DocDir docDir : this.container) {
            if (docDir.getIndex().equals(numberOf)) {
                return docDir;
            }
        }
        return null;
    }

    /**
     * 获取第一个文档容器作为默认
     *
     * @return 第一个文档容器
     */
    public DocDir getDocDefault() {
        return getDoc(1);
    }
}
