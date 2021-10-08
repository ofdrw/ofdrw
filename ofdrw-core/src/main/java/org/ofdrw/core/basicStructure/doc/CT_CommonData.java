package org.ofdrw.core.basicStructure.doc;

import org.dom4j.Element;
import org.ofdrw.core.DefaultElementProxy;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.pageObj.CT_TemplatePage;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档公共数据结构
 * <p>
 * ————《GB/T 33190-2016》 图 6
 *
 * @author 权观宇
 * @since 2019-10-04 07:46:11
 */
public class CT_CommonData extends OFDElement {
    public CT_CommonData(Element proxy) {
        super(proxy);
    }

    public CT_CommonData() {
        super("CommonData");
    }

    /**
     * 【必选】
     * 设置 当前文档中所有对象使用标识的最大值。
     * 初始值为 0。MaxUnitID主要用于文档编辑，
     * 在向文档增加一个新对象时，需要分配一个
     * 新的标识符，新标识符取值宜为 MaxUnitID + 1，
     * 同时需要修改此 MaxUnitID值。
     *
     * @param maxUnitID 对象标识符最大值
     * @return this
     */
    public CT_CommonData setMaxUnitID(ST_ID maxUnitID) {
        this.setOFDEntity("MaxUnitID", maxUnitID);
        return this;
    }

    /**
     * 【必选】
     * 设置 当前文档中所有对象使用标识的最大值。
     * 初始值为 0。MaxUnitID主要用于文档编辑，
     * 在向文档增加一个新对象时，需要分配一个
     * 新的标识符，新标识符取值宜为 MaxUnitID + 1，
     * 同时需要修改此 MaxUnitID值。
     *
     * @param maxUnitID 对象标识符最大值
     * @return this
     */
    public CT_CommonData setMaxUnitID(long maxUnitID) {
        return setMaxUnitID(new ST_ID(maxUnitID));
    }

    /**
     * 【必选】
     * 获取 当前文档中所有对象使用标识的最大值
     *
     * @return 当前文档中所有对象使用标识的最大值0
     */
    public ST_ID getMaxUnitID() {
        return ST_ID.getInstance(this.getOFDElementText("MaxUnitID"));
    }

    /**
     * 【必选】
     * 设置 该文档页面区域的默认大小和位置
     *
     * @param pageArea 文档页面区域的默认大小和位置
     * @return this
     */
    public CT_CommonData setPageArea(CT_PageArea pageArea) {
        this.set(pageArea);
        return this;
    }

    /**
     * 【必选】
     * 获取 该文档页面区域的默认大小和位置
     *
     * @return 该文档页面区域的默认大小和位置
     */
    public CT_PageArea getPageArea() {
        Element e = this.getOFDElement("PageArea");
        return e == null ? null : new CT_PageArea(e);
    }

    /**
     * 【可选】
     * 设置 公共资源序列 路径 (如果已经存在PublicRes那么替换)
     * <p>
     * 公共资源序列，每个节点指向OFD包内的一个资源描述文件，
     * 源部分的描述键见 7.9，字形和颜色空间等宜在公共资源文件中描述
     *
     * @param publicRes 公共资源序列
     * @return this
     */
    public CT_CommonData setPublicRes(ST_Loc publicRes) {
        this.setOFDEntity("PublicRes", publicRes);
        return this;
    }

    /**
     * 【可选】
     * 获取 公共资源序列(列表中的第一个PublicRes)
     * <p>
     * 公共资源序列，每个节点指向OFD包内的一个资源描述文件，
     * 源部分的描述键见 7.9，字形和颜色空间等宜在公共资源文件中描述
     *
     * @return 公共资源序列路径
     */
    public ST_Loc getPublicRes() {
        return ST_Loc.getInstance(this.getOFDElementText("PublicRes"));
    }

    /**
     * 【可选】
     * 添加 公共资源序列 路径
     * <p>
     * 公共资源序列，每个节点指向OFD包内的一个资源描述文件，
     * 源部分的描述键见 7.9，字形和颜色空间等宜在公共资源文件中描述
     *
     * @param publicRes 公共资源序列
     * @return this
     */
    public CT_CommonData addPublicRes(ST_Loc publicRes) {
        if (publicRes == null || publicRes.getLoc() == null) {
            return this;
        }
        this.addOFDEntity("PublicRes", publicRes);
        return this;
    }


    /**
     * 【可选】
     * 获取 公共资源 序列
     * <p>
     * 公共资源序列，每个节点指向OFD包内的一个资源描述文件，
     * 源部分的描述键见 7.9，字形和颜色空间等宜在公共资源文件中描述
     * <p>
     * PublicRes 数量为 0~n
     *
     * @return 公共资源序列路径
     */
    public List<ST_Loc> getPublicResList() {
        return this.getOFDElements("PublicRes", OFDElement::new)
                .stream()
                .map(DefaultElementProxy::getText)
                .map(ST_Loc::getInstance).collect(Collectors.toList());
    }

    /**
     * 【可选】
     * 设置 文件资源序列 路径（DocumentRes已经存在那么替换）
     * <p>
     * 公共资源序列，每个节点指向OFD包内的一个资源描述文件，
     * 源部分的描述键见 7.9，
     * 绘制参数、多媒体和矢量图像等宜在文件资源文件中描述
     *
     * @param documentRes 公共资源序列
     * @return this
     */
    public CT_CommonData setDocumentRes(ST_Loc documentRes) {
        this.setOFDEntity("DocumentRes", documentRes);
        return this;
    }

    /**
     * 【可选】
     * 添加 文件资源序列 路径
     * <p>
     * 公共资源序列，每个节点指向OFD包内的一个资源描述文件，
     * 源部分的描述键见 7.9，
     * 绘制参数、多媒体和矢量图像等宜在文件资源文件中描述
     *
     * @param documentRes 公共资源序列
     * @return this
     */
    public CT_CommonData addDocumentRes(ST_Loc documentRes) {
        if (documentRes == null || documentRes.getLoc() == null) {
            return this;
        }
        this.addOFDEntity("DocumentRes", documentRes);
        return this;
    }

    /**
     * 【可选】
     * 获取 文件资源序列 路径
     * <p>
     * 公共资源序列，每个节点指向OFD包内的一个资源描述文件，
     * 源部分的描述键见 7.9，
     * 绘制参数、多媒体和矢量图像等宜在文件资源文件中描述
     *
     * @return 文件资源序列 路径
     */
    public ST_Loc getDocumentRes() {
        return ST_Loc.getInstance(this.getOFDElementText("DocumentRes"));
    }

    /**
     * 【可选】
     * 获取 文件资源序列
     * <p>
     * 公共资源序列，每个节点指向OFD包内的一个资源描述文件，
     * 源部分的描述键见 7.9，
     * 绘制参数、多媒体和矢量图像等宜在文件资源文件中描述
     * <p>
     * DocumentRes 数量为 0~n
     *
     * @return 文件资源序列 路径
     */
    public List<ST_Loc> getDocumentResList() {
        return this.getOFDElements("DocumentRes", OFDElement::new)
                .stream()
                .map(DefaultElementProxy::getText)
                .map(ST_Loc::getInstance).collect(Collectors.toList());
    }

    /**
     * 【可选】
     * 增加 模板页序列
     * <p>
     * 为一些列的模板页的集合，模板页内容机构和普通页相同，描述将7.7
     *
     * @param templatePage 模板页序列
     * @return this
     */
    public CT_CommonData addTemplatePage(CT_TemplatePage templatePage) {
        this.add(templatePage);
        return this;
    }

    /**
     * 【可选】
     * 获取 模板页序列
     * <p>
     * 为一些列的模板页的集合，模板页内容机构和普通页相同，描述将7.7
     *
     * @return 模板页序列 (可能为空容器)
     */
    public List<CT_TemplatePage> getTemplatePages() {
        return this.getOFDElements("TemplatePage", CT_TemplatePage::new);
    }

    /**
     * 【可选】
     * 设置 引用在资源文件中定义的颜色标识符
     * <p>
     * 有关颜色空间的描述见 8.3.1。如果不存在此项，采用RGB作为默认颜色空间
     *
     * @param defaultCS 颜色空间引用
     * @return this
     */
    public CT_CommonData setDefaultCS(ST_RefID defaultCS) {
        this.setOFDEntity("DefaultCS", defaultCS);
        return this;
    }

    /**
     * 【可选】
     * 获取 引用在资源文件中定义的颜色标识符
     * <p>
     * 有关颜色空间的描述见 8.3.1。如果不存在此项，采用RGB作为默认颜色空间
     *
     * @return 颜色空间引用
     */
    public ST_RefID getDefaultCS() {
        return ST_RefID.getInstance(this.getOFDElementText("DefaultCS"));
    }
}
