package org.ofdrw.core.integrity;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.Const;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Loc;

import java.time.LocalDateTime;

/**
 * 为了支持防夹带机制，OFD文件中需引入完整性保护描述文件。
 * <p>
 * 防夹带文件的文件名固定位“OFDEntries.xml”，应放置在OFD文件的根目录（与OFD.xml位置相同）
 * <p>
 * GMT0099 D.1 完整性保护描述文件
 *
 * @author 权观宇
 * @since 2021-06-28 19:17:24
 */
public class OFDEntries extends OFDElement {
    public OFDEntries(Element proxy) {
        super(proxy);
    }

    public OFDEntries() {
        // 见表D,1 完整性保护描述文件元素和属性说明
        super("DocEntries");
    }

    /**
     * 【必选 属性 OFD 2.0】
     * 设置 标识符
     *
     * @param id 标识符
     * @return this
     */
    public OFDEntries setID(String id) {
        this.addAttribute("ID", id);
        return this;
    }

    /**
     * 【必选 属性 OFD 2.0】
     * 获取 标识符
     *
     * @return 标识符
     */
    public String getID() {
        return this.attributeValue("ID");
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 设置 创建者名称
     *
     * @param creatorName 创建者名称，为null表示删除
     * @return this
     */
    public OFDEntries setCreatorName(String creatorName) {
        if (creatorName == null) {
            this.removeAttr("CreatorName");
            return this;
        }
        this.addAttribute("CreatorName", creatorName);
        return this;
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 获取 创建者名称
     *
     * @return 创建者名称，为null表示删除
     */
    @Nullable
    public String getCreatorName() {
        return this.attributeValue("CreatorName");
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 设置 模块版本
     *
     * @param version 模块版本，null表示删除
     * @return this
     */
    public OFDEntries setVersion(String version) {
        if (version == null) {
            this.removeAttr("Version");
            return this;
        }
        this.addAttribute("Version", version);
        return this;
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 获取 模块版本
     *
     * @return 模块版本
     */
    @Nullable
    public String getVersion() {
        return this.attributeValue("Version");
    }

    /**
     * 【可选属性】
     * 设置 创建时间
     *
     * @param creationDate 创建时间，null表示删除
     * @return this
     */
    public OFDEntries setCreationDate(LocalDateTime creationDate) {
        if (creationDate == null) {
            this.removeAttr("CreationDate");
            return this;
        }
        this.addAttribute("CreationDate", creationDate.format(Const.DATE_FORMATTER));
        return this;
    }

    /**
     * 【可选属性】
     * 获取 创建时间
     *
     * @return creationDate 创建时间
     */
    @Nullable
    public LocalDateTime getCreationDate() {
        String str = this.attributeValue("CreationDate");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        return LocalDateTime.parse(str, Const.DATETIME_FORMATTER);
    }


    /**
     * 【必选 OFD 2.0】
     * 设置 针对防夹带文件所在文件形成的签名值。
     * <p>
     * 签名值应符合“GB/T 35275”标注
     *
     * @param signedValueLoc 针对防夹带文件所在文件形成的签名值
     * @return this
     */
    public OFDEntries setSignedValueLoc(@NotNull ST_Loc signedValueLoc) {
        if (signedValueLoc == null) {
            throw new IllegalArgumentException("防夹带文件形成的签名值(signedValueLoc)为空");
        }
        this.setOFDEntity("SignedValueLoc", signedValueLoc.toString());
        return this;
    }

    /**
     * 【必选 OFD 2.0】
     * 获取 针对防夹带文件所在文件形成的签名值。
     * <p>
     * 签名值应符合“GB/T 35275”标注
     *
     * @return 针对防夹带文件所在文件形成的签名值
     */
    @NotNull
    public ST_Loc getSignedValueLoc() {
        Element e = this.getOFDElement("SignedValueLoc");
        if (e == null) {
            throw new IllegalArgumentException("防夹带文件形成的签名值(signedValueLoc)为空");
        }
        return new ST_Loc(e.getTextTrim());
    }

    /**
     * 【必选 OFD 2.0】
     * 设置 防止夹带文件列表
     *
     * @param fileList 防止夹带文件列表
     * @return this
     */
    public OFDEntries setFileList(@NotNull FileList fileList) {
        if (fileList == null) {
            throw new IllegalArgumentException("防止夹带文件列表(FileList)为空");
        }
        this.set(fileList);
        return this;
    }

    /**
     * 【必选 OFD 2.0】
     * 获取 防止夹带文件列表
     *
     * @return 防止夹带文件列表
     */
    @NotNull
    public FileList getFileList() {
        final Element e = this.getOFDElement("FileList");
        if (e == null) {
            throw new IllegalArgumentException("防止夹带文件列表(FileList)为空");
        }
        return new FileList(e);
    }
}
