package org.ofdrw.core.annotation.pageannot;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;

/**
 * 注释所在页
 * <p>
 * 15.1 注释入口文件 图 80 表 60
 *
 * @author 权观宇
 * @since 2019-11-16 02:04:53
 */
public class AnnPage extends OFDElement {
    public AnnPage(Element proxy) {
        super(proxy);
    }

    public AnnPage() {
        super("Page");
    }


    /**
     * 【必选 属性】
     * 设置 引用注释所在页面的标识
     *
     * @param pageId 引用注释所在页面的标识
     * @return this
     */
    public AnnPage setPageID(ST_RefID pageId) {
        if (pageId == null) {
            throw new IllegalArgumentException("引用注释所在页面的标识（PageID）不能为空");
        }
        this.addAttribute("PageID", pageId.toString());
        return this;
    }

    public AnnPage setPageID(ST_ID pageId) {
        return setPageID(pageId.ref());
    }
    /**
     * 【必选 属性】
     * 获取 引用注释所在页面的标识
     *
     * @return 引用注释所在页面的标识
     */
    public ST_RefID getPageID() {
        return ST_RefID.getInstance(this.attributeValue("PageID"));
    }


    /**
     * 【必选】
     * 设置 指向包内的分页注释文件
     *
     * @param fileLoc 指向包内的分页注释文件
     * @return this
     */
    public AnnPage setFileLoc(ST_Loc fileLoc) {
        if (fileLoc == null) {
            throw new IllegalArgumentException("指向包内的分页注释文件（FileLoc）不能为空");
        }
        this.addOFDEntity("FileLoc", fileLoc);
        return this;
    }

    /**
     * 【必选】
     * 获取 指向包内的分页注释文件
     *
     * @return 指向包内的分页注释文件
     */
    public ST_Loc getFileLoc() {
        Element e = this.getOFDElement("FileLoc");
        return e == null ? null : new ST_Loc(e.getTextTrim());
    }


}
