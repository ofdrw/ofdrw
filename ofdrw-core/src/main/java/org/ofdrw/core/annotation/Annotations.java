package org.ofdrw.core.annotation;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.annotation.pageannot.AnnPage;
import org.ofdrw.core.basicType.ST_RefID;

import java.util.List;

/**
 * 注释入口文件
 * <p>
 * 注释是板式文档形成后附加的图文信息，用户可通过鼠标和键盘
 * 与进行交互。本标准中，页面内容与注释内容是份文件描述的。
 * 文件的注释在注释列表文件中按照页面进行组织索引，注释的内容
 * 在分页注释文件中描述。
 * <p>
 * 15.1 注释入口文件 图 80 表 60
 *
 * @author 权观宇
 * @since 2019-11-16 01:58:22
 */
public class Annotations extends OFDElement {
    public Annotations(Element proxy) {
        super(proxy);
    }

    public Annotations() {
        super("Annotations");
    }

    /**
     * 【可选】
     * 增加 注释所在页
     *
     * @param page 注释所在页
     * @return this
     */
    public Annotations addPage(AnnPage page) {
        if (page == null) {
            return this;
        }
        this.add(page);
        return this;
    }

    /**
     * 根据ID获取页面注解
     *
     * @param id 页面ID
     * @return null或注释所在页
     */
    public AnnPage getByPageId(String id) {
        List<AnnPage> pages = getPages();
        for (AnnPage page : pages) {
            if (page.getPageID().toString().equals(id)) {
                return page;
            }
        }
        return null;
    }

    /**
     * 【可选】
     * 增加 注释所在页序列
     *
     * @return 注释所在页序列
     */
    public List<AnnPage> getPages() {
        return this.getOFDElements("Page", AnnPage::new);
    }
}
