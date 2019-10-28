package org.ofdrw.core.versions;

import org.dom4j.Element;
import org.ofdrw.core.Const;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Loc;

import java.time.LocalDate;

/**
 * 版本
 * <p>
 * 版本信息在独立的文件中描述，如图 90 所示。
 * 版本定义结构中列出了一个 OFD 文档版本中所需的所有文件。
 * <p>
 * 19.2 版本 图 90 表 71
 *
 * @author 权观宇
 * @since 2019-10-28 07:14:16
 */
public class DocVersion extends OFDElement {

    public DocVersion(Element proxy) {
        super(proxy);
    }

    public DocVersion() {
        super("DocVersion");
    }

    public DocVersion(String id, FileList fileList) {
        this();
        this.setID(id)
                .setFileList(fileList);
    }


    /**
     * 【属性 必选】
     * 设置 版本标识符
     *
     * @param id 版本标识符
     * @return this
     */
    public DocVersion setID(String id) {
        if (id == null || id.trim().length() == 0) {
            throw new IllegalArgumentException("版本标识符（ID）不能为空");
        }
        this.addAttribute("ID", id.toString());
        return this;
    }

    /**
     * 【属性 必选】
     * 获取 版本标识符
     *
     * @return 版本标识符
     */
    public String getID() {
        String str = this.attributeValue("ID");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("版本标识符（ID）不能为空");
        }
        return str;
    }

    /**
     * 【属性 可选】
     * 设置 该文件适用的格式版本
     *
     * @param version 该文件适用的格式版本
     * @return this
     */
    public DocVersion setVersion(String version) {
        if (version == null) {
            this.removeAttr(version);
            return this;
        }
        this.addAttribute("Version", version);
        return this;
    }

    /**
     * 【属性 可选】
     * 获取 该文件适用的格式版本
     *
     * @return 该文件适用的格式版本，null表示不存在
     */
    public String getVersion() {
        return this.attributeValue("Version");
    }

    /**
     * 【属性 可选】
     * 设置 版本名称
     *
     * @param name 版本名称
     * @return this
     */
    public DocVersion setDocVersionName(String name) {
        if (name == null) {
            this.removeAttr("Name");
            return this;
        }
        this.addAttribute("Name", name);
        return this;
    }

    /**
     * 【属性 可选】
     * 获取 版本名称
     *
     * @return 版本名称，null表示不存在
     */
    public String getDocVersionName() {
        return this.attributeValue("Name");
    }

    /**
     * 【属性 可选】
     * 设置 创建时间
     *
     * @param creationDate 创建时间
     * @return this
     */
    public DocVersion setCreationDate(LocalDate creationDate) {
        if (creationDate == null) {
            this.removeAttr("CreationDate");
            return this;
        }
        this.addAttribute("CreationDate", creationDate.toString());
        return this;
    }

    /**
     * 【属性 可选】
     * 获取 创建时间
     *
     * @return 创建时间
     */
    public LocalDate getCreationDate() {
        String dateStr = this.getOFDElementText("CreationDate");
        return dateStr != null ? LocalDate.parse(dateStr, Const.DATE_FORMATTER) : null;
    }

    /**
     * 【必选】
     * 设置 版本包含的文件列表
     *
     * @param fileList 版本包含的文件列表
     * @return this
     */
    public DocVersion setFileList(FileList fileList) {
        if (fileList == null) {
            throw new IllegalArgumentException("版本包含的文件列表（FileList）不能为空");
        }
        this.add(fileList);
        return this;
    }


    /**
     * 【必选】
     * 获取 版本包含的文件列表
     *
     * @return 版本包含的文件列表
     */
    public FileList getFileList() {
        Element e = this.getOFDElement("FileList");
        return e == null ? null : new FileList(e);
    }


    /**
     * 【必选】
     * 设置 该版本的入口文件
     *
     * @param docRoot 该版本的入口文件
     * @return this
     */
    public DocVersion setDocRoot(ST_Loc docRoot) {
        if (docRoot == null) {
            throw new IllegalArgumentException("该版本的入口文件（DocRoot）不能为空");
        }
        this.addOFDEntity("DocRoot", docRoot);
        return this;
    }

    /**
     * 【必选】
     * 获取 该版本的入口文件
     *
     * @return 该版本的入口文件
     */
    public ST_Loc getDocRoot() {
        Element e = this.getOFDElement("DocRoot");
        return e == null ? null : new ST_Loc(e.getTextTrim());
    }
}
