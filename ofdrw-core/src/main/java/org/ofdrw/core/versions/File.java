package org.ofdrw.core.versions;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * 文件列表文件描述
 * <p>
 * 19.2 表 71
 *
 * @author 权观宇
 * @since 2019-10-28 07:36:21
 */
public class File extends OFDElement {
    public File(Element proxy) {
        super(proxy);
    }

    public File() {
        super("File");
    }

    /**
     * @param id  文件列表文件标识
     * @param loc 文件列表文件描述
     */
    public File(String id, ST_Loc loc) {
        this();
        this.setID(id)
                .setFile(loc);
    }

    /**
     * 【必选 属性】
     * 设置 文件列表文件标识
     *
     * @param id 文件列表文件标识
     * @return this
     */
    public File setID(String id) {
        if (id == null) {
            throw new IllegalArgumentException("文件列表文件标识（ID）不能为空");
        }
        this.addAttribute("ID", id);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 文件列表文件标识
     *
     * @return 文件列表文件标识
     */
    public String getID() {
        String str = this.attributeValue("ID");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("文件列表文件标识（ID）不能为空");
        }
        return str;
    }

    /**
     * 【必选】
     * 设置 文件列表文件描述
     *
     * @param file 文件列表文件描述
     * @return this
     */
    public File setFile(ST_Loc file) {
        if (file == null) {
            throw new IllegalArgumentException("文件列表文件描述（File）不能为空");
        }
        this.setText(file.toString());
        return this;
    }

    /**
     * 【必选】
     * 获取 文件列表文件描述
     *
     * @return 文件列表文件描述
     */
    public ST_Loc getFile() {
        String str = this.getText();
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("文件列表文件描述（File）不能为空");
        }
        return ST_Loc.getInstance(str);
    }
}
