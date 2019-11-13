package org.ofdrw.core.basicStructure.res;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * 多媒体
 * <p>
 * 7.9 资源 图 21 表 19
 *
 * @author 权观宇
 * @since 2019-11-13 08:03:34
 */
public class CT_MultiMedia extends OFDElement {
    public CT_MultiMedia(Element proxy) {
        super(proxy);
    }

    public CT_MultiMedia() {
        super("MultiMedia");
    }


    public ST_ID getID() {
        return this.getObjID();
    }

    public CT_MultiMedia setID(ST_ID id) {
        this.setObjID(id);
        return this;
    }

    /**
     * 【必选 属性】
     * 设置 多媒体类型
     * <p>
     * 支持位图图像、视频、音频
     *
     * @param type 多媒体类型
     * @return this
     */
    public CT_MultiMedia setType(MediaType type) {
        if (type == null) {
            throw new IllegalArgumentException("多媒体类型（Type）为空");
        }
        this.addAttribute("Type", type.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 多媒体类型
     * <p>
     * 支持位图图像、视频、音频
     *
     * @return 多媒体类型
     */
    public MediaType getType() {
        return MediaType.getInstance(this.attributeValue("Type"));
    }

    /**
     * 【可选 属性】
     * 设置 资源的格式
     * <p>
     * 支持 BMP、JPEG、PNG、TIFF及AVS等格式，其中TIFF格式不支持多页
     *
     * @param format 资源的格式
     * @return this
     */
    public CT_MultiMedia setFormat(String format) {
        if (format == null) {
            this.removeAttr("Format");
            return this;
        }
        this.addAttribute("Format", format);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 资源的格式
     * <p>
     * 支持 BMP、JPEG、PNG、TIFF及AVS等格式，其中TIFF格式不支持多页
     *
     * @return 资源的格式
     */
    public String getFormat() {
        return this.attributeValue("Format");
    }


    /**
     * 【必选】
     * 设置 指向 OFD包内的多媒体文件位置
     *
     * @param mediaFile 指向 OFD包内的多媒体文件位置
     * @return this
     */
    public CT_MultiMedia setMediaFile(ST_Loc mediaFile) {
        if (mediaFile == null) {
            throw new IllegalArgumentException("");
        }
        this.removeAll();
        this.addOFDEntity("MediaFile", mediaFile);
        return this;
    }

    /**
     * 【必选】
     * 获取 指向 OFD包内的多媒体文件位置
     *
     * @return 指向 OFD包内的多媒体文件位置
     */
    public ST_Loc getMediaFile() {
        Element e = this.getOFDElement("MediaFile");
        return e == null ? null : new ST_Loc(e.getTextTrim());
    }
}
