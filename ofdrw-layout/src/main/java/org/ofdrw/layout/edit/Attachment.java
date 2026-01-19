package org.ofdrw.layout.edit;

import org.ofdrw.core.attachment.CT_Attachment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 附件对象构造器
 *
 * @author 权观宇
 * @since 2020-05-20 19:18:37
 */
public class Attachment {

    /**
     * 附件文件
     */
    private Path file;

    /**
     * 附件对象
     */
    private CT_Attachment atmObj;

    /**
     * 是否禁用同名替换
     * <p>
     * 默认值为 false（同名替换）
     */
    private boolean disableReplace;

    private Attachment() {
    }

    /**
     * 附件对象构造对象，同名文件将被替换
     *
     * @param name 附件名称
     * @param file 附件文件
     */
    public Attachment(String name, Path file) {
        this(name, file, false);
    }

    /**
     * 附件对象构造对象
     *
     * @param name           附件名称
     * @param file           附件文件
     * @param disableReplace 是否禁用同名替换，默认为 false（同名替换）
     */
    public Attachment(String name, Path file, boolean disableReplace) {
        if (file == null || Files.notExists(file)) {
            throw new IllegalArgumentException("附件文件(file)为空或不存在");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("附件名称(name)不能为空");
        }
        this.file = file;
        this.disableReplace = disableReplace;
        this.atmObj = new CT_Attachment();
        this.setName(name);
    }

    /**
     * 【必选 属性】
     * 设置 附件名称
     *
     * @param attachmentName 附件名称
     * @return this
     */
    public Attachment setName(String attachmentName) {
        atmObj.setAttachmentName(attachmentName);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 附件名称
     *
     * @return 附件名称
     */
    public String getName() {
        return atmObj.getAttachmentName();
    }

    /**
     * 【可选 属性】
     * 设置 附件格式
     *
     * @param format 附件格式
     * @return this
     */
    public Attachment setFormat(String format) {
        atmObj.setFormat(format);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 附件格式
     *
     * @return 附件格式
     */
    public String getFormat() {
        return atmObj.getFormat();
    }

    /**
     * 【可选 属性】
     * 设置 创建时间
     *
     * @param creationDate 创建时间
     * @return this
     * @deprecated {@link  #setCreationDate(LocalDateTime)}
     */
    @Deprecated
    public Attachment setCreationDate(LocalDate creationDate) {
        atmObj.setCreationDate(creationDate.atStartOfDay());
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 创建时间
     *
     * @param creationDate 创建时间
     * @return this
     */
    public Attachment setCreationDate(LocalDateTime creationDate) {
        atmObj.setCreationDate(creationDate);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 创建时间
     *
     * @return 创建时间
     * @deprecated {@link  #getCreationDateTime()}
     */
    @Deprecated
    public LocalDate getCreationDate() {
        return atmObj.getCreationDateTime().toLocalDate();
    }

    /**
     * 【可选 属性】
     * 获取 创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreationDateTime() {
        return atmObj.getCreationDateTime();
    }

    /**
     * 【可选 属性】
     * 设置 修改时间
     *
     * @param modDate 修改时间
     * @return this
     * @deprecated {@link #setModDateTime(LocalDateTime)}
     */
    @Deprecated
    public Attachment setModDate(LocalDate modDate) {
        atmObj.setModDate(modDate.atStartOfDay());
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 修改时间
     *
     * @param modDate 修改时间
     * @return this
     */
    public Attachment setModDateTime(LocalDateTime modDate) {
        atmObj.setModDate(modDate);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 修改时间
     *
     * @return 修改时间
     * @deprecated {@link #getCreationDateTime()}
     */
    @Deprecated
    public LocalDate getModDate() {
        return atmObj.getModDateTime().toLocalDate();
    }

    /**
     * 【可选 属性】
     * 获取 修改时间
     *
     * @return 修改时间
     */
    public LocalDateTime getModDateTime() {
        return atmObj.getCreationDateTime();
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
    public Attachment setVisible(Boolean visible) {
        atmObj.setVisible(visible);
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
        return atmObj.getVisible();
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
    public Attachment setUsage(String usage) {
        atmObj.setUsage(usage);
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
        return atmObj.getUsage();
    }

    /**
     * 获取附件文件
     *
     * @return 附件文件路径
     */
    public Path getFile() {
        return file;
    }

    /**
     * 设置附件文件
     *
     * @param file 附件文件
     * @return this
     */
    public Attachment setFile(Path file) {
        this.file = file;
        return this;
    }

    public CT_Attachment getAttachment() {
        return atmObj;
    }

    /**
     * 设置是否禁用同名替换
     * <p>
     * 默认值为 false（同名替换）
     *
     * @param disableReplace 是否禁用同名替换
     * @return this
     */
    public Attachment setDisableReplace(boolean disableReplace) {
        this.disableReplace = disableReplace;
        return this;
    }

    /**
     * 获取是否禁用同名替换
     * <p>
     * 默认值为 false（同名替换）
     *
     * @return 是否禁用同名替换
     */
    public boolean isDisableReplace() {
        return disableReplace;
    }
}
