package org.ofdrw.core.integrity;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * GMT0099 防止夹带文件信息
 *
 * @author 权观宇
 * @since 2021-06-28 19:52:29
 */
public class File extends OFDElement {
    public File(Element proxy) {
        super(proxy);
    }

    public File() {
        super("File");
    }

    /**
     * 创建 防止夹带文件信息
     *
     * @param id      文件标识
     * @param fileLoc 包内文件路径
     */
    public File(String id, String fileLoc) {
        this();
        setID(id);
        setFileLoc(new ST_Loc(fileLoc));
    }

    /**
     * 创建 防止夹带文件信息
     *
     * @param id      文件标识
     * @param fileLoc 包内文件路径
     */
    public File(String id, ST_Loc fileLoc) {
        this();
        setID(id);
        setFileLoc(fileLoc);
    }


    /**
     * 【必选 属性 OFD 2.0】
     * 设置 文件标识
     *
     * @param id 文件标识
     * @return this
     */
    public File setID(@NotNull String id) {
        if (id == null) {
            throw new IllegalArgumentException("文件标识(ID)为空");
        }
        this.addAttribute("ID", id);
        return this;
    }

    /**
     * 【必选 属性 OFD 2.0】
     * 获取 文件标识
     *
     * @return 文件标识
     */
    public String getID() {
        String str = this.attributeValue("ID");
        if (str == null) {
            throw new IllegalArgumentException("文件标识(ID)为空");
        }
        return str;
    }

    /**
     * 【必选 属性 OFD 2.0】
     * 设置 包内文件路径
     *
     * @param fileLoc 包内文件路径
     * @return this
     */
    public File setFileLoc(@NotNull ST_Loc fileLoc) {
        if (fileLoc == null) {
            throw new IllegalArgumentException("包内文件路径(FileLoc)为空");
        }
        this.addAttribute("FileLoc", fileLoc.toString());
        return this;
    }

    /**
     * 【必选 属性 OFD 2.0】
     * 设置 包内文件路径
     *
     * @return 包内文件路径
     */
    @NotNull
    public ST_Loc getFileLoc() {
        final String fileLoc = this.attributeValue("FileLoc");
        if (fileLoc == null) {
            throw new IllegalArgumentException("包内文件路径(FileLoc)为空");
        }
        return ST_Loc.getInstance(fileLoc);

    }
}
