package org.ofdrw.reader.model;

import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.core.text.font.CT_Font;

import java.util.List;

/**
 * @author dltech21
 * @since 2020/8/11
 */
public class StampAnnotVo {

    private List<StampAnnot> stampAnnots;

    private List<OfdPageVo> ofdPageVoList;

    private List<CT_Font> ctFontList;

    private List<CT_DrawParam> ctDrawParamList;

    private byte[] imgByte;

    private String type;

    public List<StampAnnot> getStampAnnots() {
        return stampAnnots;
    }

    public void setStampAnnots(List<StampAnnot> stampAnnots) {
        this.stampAnnots = stampAnnots;
    }

    public List<OfdPageVo> getOfdPageVoList() {
        return ofdPageVoList;
    }

    public void setOfdPageVoList(List<OfdPageVo> ofdPageVoList) {
        this.ofdPageVoList = ofdPageVoList;
    }

    public List<CT_Font> getCtFontList() {
        return ctFontList;
    }

    public void setCtFontList(List<CT_Font> ctFontList) {
        this.ctFontList = ctFontList;
    }

    public List<CT_DrawParam> getCtDrawParamList() {
        return ctDrawParamList;
    }

    public void setCtDrawParamList(List<CT_DrawParam> ctDrawParamList) {
        this.ctDrawParamList = ctDrawParamList;
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
