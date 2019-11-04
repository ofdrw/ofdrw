package org.ofdrw.core.basicStructure.ofd.docInfo;

import org.dom4j.Element;
import org.ofdrw.core.Const;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.OFDElement;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 文档元数据信息描述
 *
 * @author 权观宇
 * @since 2019-09-29 10:14:41
 */
public class CT_DocInfo extends OFDElement {

    public CT_DocInfo(Element proxy) {
        super(proxy);
    }

    public CT_DocInfo() {
        super("DocInfo");
    }

    /**
     * 【必选】
     * 设置文件标识符，标识符应该是一个UUID
     *
     * @param docID UUID文件标识
     * @return this
     */
    public CT_DocInfo setDocID(UUID docID) {
        this.setOFDEntity("DocID", docID.toString());
        return this;
    }

    /**
     * 随机产生一个UUID作为文件标识符
     *
     * @return this
     */
    public CT_DocInfo randomDocID() {
        return setDocID(UUID.randomUUID());
    }

    /**
     * 【必选】
     * 采用UUID算法生成的由32个字符组成的文件标识。每个DocID在
     * 文件创建或生成的时候进行分配。
     *
     * @return 文件标识符
     */
    public String getDocID() {
        return this.getOFDElementText("DocID");
    }

    /**
     * 【可选】
     * 设置文档标题。标题可以与文件名不同
     *
     * @param title 标题
     * @return this
     */
    public CT_DocInfo setTile(String title) {
        this.setOFDEntity("Title", title);
        return this;
    }

    /**
     * 【可选】
     * 获取文档标题。标题可以与文件名不同
     *
     * @return 档标题
     */
    public String getTile() {
        return this.getOFDElementText("Title");
    }

    /**
     * 【可选】
     * 设置文档作者
     *
     * @param author 文档作者
     * @return this
     */
    public CT_DocInfo setAuthor(String author) {
        this.setOFDEntity("Author", author);
        return this;
    }

    /**
     * 【可选】
     * 获取文档作者
     *
     * @return 文档作者
     */
    public String getAuthor() {
        return this.getOFDElementText("Author");
    }

    /**
     * 【可选】
     * 设置文档主题
     *
     * @param subject 文档主题
     * @return this
     */
    public CT_DocInfo setSubject(String subject) {
        this.setOFDEntity("Subject", subject);
        return this;
    }

    /**
     * 【可选】
     * 获取文档主题
     *
     * @return 文档主题
     */
    public String getSubject() {
        return this.getOFDElementText("Subject");
    }

    /**
     * 【可选】
     * 设置文档摘要与注释
     *
     * @param abstractTxt 文档摘要与注释
     * @return this
     */
    public CT_DocInfo setAbstract(String abstractTxt) {
        this.setOFDEntity("Abstract", abstractTxt);
        return this;
    }

    /**
     * 【可选】
     * 获取文档摘要与注释
     *
     * @return 文档摘要与注释
     */
    public String getAbstract() {
        return this.getOFDElementText("Abstract");
    }

    /**
     * 【可选】
     * 设置文件创建日期
     *
     * @param creationDate 文件创建日期
     * @return this
     */
    public CT_DocInfo setCreationDate(LocalDate creationDate) {
        this.setOFDEntity("CreationDate", creationDate.toString());
        return this;
    }

    /**
     * 【可选】
     * 获取文件创建日期
     *
     * @return 创建日期
     */
    public LocalDate getCreationDate() {
        String dateStr = this.getOFDElementText("CreationDate");
        return dateStr != null ? LocalDate.parse(dateStr, Const.DATE_FORMATTER) : null;
    }

    /**
     * 【可选】
     * 设置文档最近修改日期
     *
     * @param modDate 文档最近修改日期
     * @return this
     */
    public CT_DocInfo setModDate(LocalDate modDate) {
        this.setOFDEntity("ModDate", modDate.toString());
        return this;
    }

    /**
     * 【可选】
     * 获取文档最近修改日期
     *
     * @return 文档最近修改日期
     */
    public LocalDate getModDate() {
        String dateStr = this.getOFDElementText("ModDate");
        return dateStr != null ? LocalDate.parse(dateStr, Const.DATE_FORMATTER) : null;
    }

    /**
     * 【可选】
     * 文档分类，可取值如下：
     * <p>
     * Normal——普通文档
     * <p>
     * EBook——电子书
     * <p>
     * ENewsPaper——电子报纸
     * <p>
     * EMagzine——电子期刊
     * <p>
     * 默认值为 Normal
     *
     * @param docUsage 文档分类
     * @return this
     */
    public CT_DocInfo setDocUsage(DocUsage docUsage) {
        if (docUsage == null) {
            this.setOFDEntity("DocUsage", DocUsage.Normal.toString());

        } else {
            this.setOFDEntity("DocUsage", docUsage.toString());
        }
        return this;
    }

    /**
     * 【可选】
     * 获取文档分类
     * <p>
     * 默认值为 Normal
     *
     * @return 文档分类
     */
    public DocUsage getDocUsage() {
        String usageStr = this.getOFDElementText("DocUsage");
        return DocUsage.getInstance(usageStr);
    }

    /**
     * 【可选】
     * 设置文档封面，此路径指向一个图片文件
     *
     * @param cover 文档封面路径
     * @return this
     */
    public CT_DocInfo setCover(ST_Loc cover) {
        this.set(cover.getElement("Cover"));
        return this;
    }

    /**
     * 【可选】
     * 获取文档封面，此路径指向一个图片文件
     *
     * @return 文档封面路径
     */
    public ST_Loc getCover() {
        String locStr = this.getOFDElementText("Cover");
        if (locStr == null || locStr.trim().length() == 0) {
            return null;
        }
        return new ST_Loc(locStr);
    }

    /**
     * 【可选】
     * 设置关键词集合
     * 每一个关键词用一个“Keyword”子节点来表达
     *
     * @param keywords 关键词集合
     * @return this
     */
    public CT_DocInfo setKeywords(Keywords keywords) {
        this.set(keywords);
        return this;
    }

    /**
     * 添加关键词
     *
     * @param keyword 关键词
     * @return this
     */
    public CT_DocInfo addKeyword(String keyword) {
        Keywords keywords = getKeywords();
        if (keywords == null) {
            keywords = new Keywords();
            this.add(keywords);
        }
        keywords.addKeyword(keyword);
        return this;
    }

    /**
     * 【可选】
     * 获取关键词集合
     *
     * @return 关键词集合或null
     */
    public Keywords getKeywords() {
        Element element = this.getOFDElement("Keywords");
        return element == null ? null : new Keywords(element);
    }

    /**
     * 【可选】
     * 设置创建文档的应用程序
     *
     * @param creator 创建文档的应用程序
     * @return this
     */
    public CT_DocInfo setCreator(String creator) {
        this.setOFDEntity("Creator", creator);
        return this;
    }

    /**
     * 【可选】
     * 获取创建文档的应用程序
     *
     * @return 创建文档的应用程序或null
     */
    public String getCreator() {
        return this.getOFDElementText("Creator");
    }

    /**
     * 【可选】
     * 设置创建文档的应用程序版本信息
     *
     * @param creatorVersion 创建文档的应用程序版本信息
     * @return this
     */
    public CT_DocInfo setCreatorVersion(String creatorVersion) {
        this.setOFDEntity("CreatorVersion", creatorVersion);
        return this;
    }

    /**
     * 【可选】
     * 获取创建文档的应用程序版本信息
     *
     * @return 创建文档的应用程序版本信息或null
     */
    public String getCreatorVersion() {
        return this.getOFDElementText("CreatorVersion");
    }

    /**
     * 【可选】
     * 设置用户自定义元数据集合。其子节点为 CustomData
     *
     * @param customDatas 用户自定义元数据集合
     * @return this
     */
    public CT_DocInfo setCustomDatas(CustomDatas customDatas) {
        this.set(customDatas);
        return this;
    }

    /**
     * 【可选】
     * 获取用户自定义元数据集合。其子节点为 CustomData
     *
     * @return 用户自定义元数据集合
     */
    public CustomDatas getCustomDatas() {
        Element element = this.getOFDElement("CustomDatas");
        return element == null ? null : new CustomDatas(element);
    }

    @Override
    public String getQualifiedName() {
        return "ofd:DocInfo";
    }
}
