package org.ofdrw.core.versions;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Loc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 版本包含的文件列表
 * <p>
 * 19.2 标 71
 *
 * @author 权观宇
 * @since 2019-10-28 07:26:09
 */
public class FileList extends OFDElement {
    public FileList(Element proxy) {
        super(proxy);
    }

    public FileList() {
        super("FileList");
    }

    /**
     * 【必选】
     * 增加  文件列表文件描述
     *
     * @param file 文件列表文件描述
     * @return this
     */
    public FileList addFile(File file) {
        if (file == null) {
            return this;
        }
        this.add(file);
        return this;
    }

    /**
     * 【必选】
     * 增加  文件列表文件描述
     *
     * @param id      文件列表文件标识
     * @param fileLoc 文件列表文件描述
     * @return this
     */
    public FileList addFile(String id, ST_Loc fileLoc) {
        return this.addFile(new File(id, fileLoc));
    }

    /**
     * 【必选】
     * 获取  文件列表文件描述列表
     *
     * @return 文件列表文件描述列表
     */
    public List<File> getFiles() {
        List<Element> elements = this.getOFDElements("File");
        if (elements == null || elements.size() == 0) {
            return Collections.emptyList();
        }
        List<File> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(new File(item)));
        return res;
    }

}
