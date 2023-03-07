package org.ofdrw.core.attachment;

import org.dom4j.Element;
import org.ofdrw.core.Const;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.STBase;
import org.ofdrw.core.basicType.ST_Loc;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 附件
 * <p>
 * 20.2 附件 图 92 表 73
 *
 * @author 权观宇
 * @since 2019-11-21 19:23:15
 */
public class CT_Attachment extends OFDElement {
    public CT_Attachment(Element proxy) {
        super(proxy);
    }

    public CT_Attachment() {
        super("Attachment");
    }

    /**
     * 【必选 属性】
     * 设置 附件标识
     *
     * @param id 附件标识
     * @return this
     */
    public CT_Attachment setID(String id) {
        if (id == null || id.trim().length() == 0) {
            throw new IllegalArgumentException("附件标识（ID）为空");
        }
        this.addAttribute("ID", id);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 附件标识
     *
     * @return 附件标识
     */
    public String getID() {
        String str = this.attributeValue("ID");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("附件标识（ID）为空");
        }
        return str;
    }

    /**
     * 【必选 属性】
     * 设置 附件名称
     *
     * @param attachmentName 附件名称
     * @return this
     */
    public CT_Attachment setAttachmentName(String attachmentName) {
        if (attachmentName == null || attachmentName.trim().length() == 0) {
            throw new IllegalArgumentException("附件名称（Name）为空");
        }
        this.addAttribute("Name", attachmentName);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 附件名称
     *
     * @return 附件名称
     */
    public String getAttachmentName() {
        String str = this.attributeValue("Name");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("附件名称（Name）为空");

        }
        return str;
    }

    /**
     * 【可选 属性】
     * 设置 附件格式
     *
     * @param format 附件格式
     * @return this
     */
    public CT_Attachment setFormat(String format) {
        if (format == null || format.trim().length() == 0) {
            this.removeAttr("Format");
            return this;
        }
        this.addAttribute("Format", format);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 附件格式
     *
     * @return 附件格式
     */
    public String getFormat() {
        return this.attributeValue("Format");
    }

    /**
     * 【可选 属性】
     * 设置 创建时间
     *
     * @param creationDate 创建时间
     * @return this
     * @deprecated {@link #setCreationDate(LocalDateTime)}
     */
    @Deprecated
    public CT_Attachment setCreationDate(LocalDate creationDate) {
        if (creationDate == null) {
            this.removeAttr("CreationDate");
            return this;
        }
        this.addAttribute("CreationDate", creationDate.atStartOfDay().format(Const.DATETIME_FORMATTER));
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 创建时间
     *
     * @param creationDate 创建时间
     * @return this
     */
    public CT_Attachment setCreationDate(LocalDateTime creationDate) {
        if (creationDate == null) {
            this.removeAttr("CreationDate");
            return this;
        }
        this.addAttribute("CreationDate", creationDate.format(Const.DATETIME_FORMATTER));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 创建时间
     *
     * @return 创建时间
     * @deprecated {@link #getCreationDateTime()}
     */
    @Deprecated
    public LocalDate getCreationDate() {
        String str = this.attributeValue("CreationDate");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        str = str.trim();
        try {
            return LocalDateTime.parse(str, Const.DATETIME_FORMATTER).toLocalDate();
        } catch (Exception ignore) {
            return LocalDate.parse(str, Const.DATE_FORMATTER);
        }
    }

    /**
     * 【可选 属性】
     * 获取 创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreationDateTime() {
        String str = this.attributeValue("CreationDate");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        str = str.trim();
        try {
            return LocalDateTime.parse(str, Const.DATETIME_FORMATTER);
        } catch (Exception ignore) {
            return LocalDate.parse(str, Const.DATE_FORMATTER).atStartOfDay();
        }
    }

    /**
     * 【可选 属性】
     * 设置 修改时间
     *
     * @param modDate 修改时间
     * @return this
     * @deprecated {@link #setModDate(LocalDateTime)}
     */
    @Deprecated
    public CT_Attachment setModDate(LocalDate modDate) {
        if (modDate == null) {
            this.removeAttr("ModDate");
            return this;
        }
        this.addAttribute("ModDate", modDate.atStartOfDay().format(Const.DATETIME_FORMATTER));
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 修改时间
     *
     * @param modDate 修改时间
     * @return this
     */
    public CT_Attachment setModDate(LocalDateTime modDate) {
        if (modDate == null) {
            this.removeAttr("ModDate");
            return this;
        }
        this.addAttribute("ModDate", modDate.format(Const.DATETIME_FORMATTER));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 修改时间
     *
     * @return 修改时间
     * @deprecated {@link #getModDateTime()}
     */
    @Deprecated
    public LocalDate getModDate() {
        String str = this.attributeValue("ModDate");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        str = str.trim();
        try {
            return LocalDateTime.parse(str, Const.DATETIME_FORMATTER).toLocalDate();
        } catch (Exception ignore) {
            return LocalDate.parse(str, Const.DATE_FORMATTER);
        }
    }

    /**
     * 【可选 属性】
     * 获取 修改时间
     *
     * @return 修改时间
     */
    public LocalDateTime getModDateTime() {
        String str = this.attributeValue("ModDate");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        str = str.trim();
        try {
            return LocalDateTime.parse(str, Const.DATETIME_FORMATTER);
        } catch (Exception ignore) {
            return LocalDate.parse(str, Const.DATE_FORMATTER).atStartOfDay();
        }
    }

    /**
     * 【可选 属性】
     * 设置 附件大小
     * <p>
     * 以KB为单位
     *
     * @param size 附件大小，以KB为单位
     * @return this
     */
    public CT_Attachment setSize(Double size) {
        if (size == null) {
            this.removeAttr("Size");
            return this;
        }
        this.addAttribute("Size", STBase.fmt(size));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 附件大小
     * <p>
     * 以KB为单位
     *
     * @return 附件大小，以KB为单位
     */
    public Double getSize() {
        String str = this.attributeValue("Size");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选 属相】
     * 设置 附件是否可见
     * <p>
     * 默认值为 true
     *
     * @param visible 附件是否可见
     * @return this
     */
    public CT_Attachment setVisible(Boolean visible) {
        if (visible == null) {
            this.removeAttr("Visible");
            return this;
        }
        this.addAttribute("Visible", Boolean.toString(visible));
        return this;
    }

    /**
     * 【可选 属相】
     * 获取 附件是否可见
     * <p>
     * 默认值为 true
     *
     * @return 附件是否可见
     */
    public Boolean getVisible() {
        String str = this.attributeValue("Visible");
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选 属性】
     * 设置 附件用途
     * <p>
     * 默认值为 none
     *
     * @param usage 附件用途
     * @return this
     */
    public CT_Attachment setUsage(String usage) {
        if (usage == null || usage.trim().length() == 0) {
            this.removeAttr("Usage");
            return this;
        }
        this.addAttribute("Usage", usage);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 附件用途
     * <p>
     * 默认值为 none
     *
     * @return 附件用途
     */
    public String getUsage() {
        String str = this.attributeValue("Usage");
        if (str == null || str.trim().length() == 0) {
            return "none";
        }
        return str;
    }


    /**
     * 【可选】
     * 设置 附件内容在包内的路径
     *
     * @param fileLoc 附件内容在包内的路径
     * @return this
     */
    public CT_Attachment setFileLoc(ST_Loc fileLoc) {
        if (fileLoc == null) {
            this.removeOFDElemByNames("FileLoc");
            return this;
        }
        this.setOFDEntity("FileLoc", fileLoc);
        return this;
    }

    /**
     * 【可选】
     * 获取 附件内容在包内的路径
     *
     * @return 附件内容在包内的路径
     */
    public ST_Loc getFileLoc() {
        Element e = this.getOFDElement("FileLoc");
        return e == null ? null : ST_Loc.getInstance(e);
    }
}
