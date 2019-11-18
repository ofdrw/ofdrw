package org.ofdrw.core.basicStructure.res.resources;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.OFDResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 包含了一组文档所有多媒体的描述
 * <p>
 * 7.9 图 20 表 18
 *
 * @author 权观宇
 * @since 2019-11-13 08:14:14
 */
public class MultiMedias extends OFDElement implements OFDResource {
    public MultiMedias(Element proxy) {
        super(proxy);
    }

    public MultiMedias() {
        super("MultiMedias");
    }

    /**
     * 【必选】
     * 增加 多媒体资源描述
     * <p>
     * 必须含有ID属性
     *
     * @param multiMedia 多媒体资源描述
     * @return this
     */
    public MultiMedias addMultiMedia(CT_MultiMedia multiMedia) {
        if (multiMedia == null) {
            return this;
        }
        if (multiMedia.getID() == null) {
            throw new IllegalArgumentException("多媒体资源描述ID不能为空");
        }
        this.add(multiMedia);
        return this;
    }

    /**
     * 【必选】
     * 获取 多媒体资源描述列表
     * <p>
     * 必须含有ID属性
     *
     * @return 多媒体资源描述
     */
    public List<CT_MultiMedia> getMultiMedias() {
        return this.getOFDElements("MultiMedia", CT_MultiMedia::new);
    }
}
