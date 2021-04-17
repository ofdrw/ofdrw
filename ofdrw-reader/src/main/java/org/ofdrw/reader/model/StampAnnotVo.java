package org.ofdrw.reader.model;

import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.core.text.font.CT_Font;

import java.util.List;

/**
 * @deprecated {@link StampAnnotEntity}
 * @author dltech21
 * @since 2020/8/11
 */
@Deprecated
public class StampAnnotVo {

    private List<StampAnnot> stampAnnots;

    private byte[] imgByte;

    private String type;

    public List<StampAnnot> getStampAnnots() {
        return stampAnnots;
    }

    public void setStampAnnots(List<StampAnnot> stampAnnots) {
        this.stampAnnots = stampAnnots;
    }

    public byte[] getImgByte() {
        return imgByte;
    }

    public void setImgByte(byte[] imgByte) {
        this.imgByte = imgByte;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
