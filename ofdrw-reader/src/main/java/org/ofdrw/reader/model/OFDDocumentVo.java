package org.ofdrw.reader.model;

import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.compositeObj.CT_VectorG;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.text.font.CT_Font;

import java.util.List;

/**
 * @author dltech21
 * @since 2020/8/11
 */
public class OFDDocumentVo {
    private List<OfdPageVo> ofdPageVoList;

    private List<CT_Font> ctFontList;

    private List<CT_ColorSpace> ctColorSpaceList;

    private List<CT_MultiMedia> ctMultiMediaList;

    private List<CT_VectorG> ctVectorGList;

    private List<StampAnnotVo> stampAnnotVos;

    private List<CT_DrawParam> ctDrawParamList;

    private String docPath;

    private double pageWidth;

    private double pageHeight;

    private List<AnnotionVo> annotaions;

    public OFDDocumentVo(String docPath, double pageWidth, double pageHeight, List<OfdPageVo> ofdPageVoList, List<CT_Font> ctFontList, List<CT_ColorSpace> ctColorSpaceList, List<CT_MultiMedia> ctMultiMediaList, List<CT_VectorG> ctVectorGList, List<StampAnnotVo> stampAnnotVoList, List<CT_DrawParam> ctDrawParamList, List<AnnotionVo> annotaions) {
        this.docPath = docPath;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.ofdPageVoList = ofdPageVoList;
        this.ctFontList = ctFontList;
        this.ctColorSpaceList = ctColorSpaceList;
        this.ctMultiMediaList = ctMultiMediaList;
        this.ctVectorGList = ctVectorGList;
        this.stampAnnotVos = stampAnnotVoList;
        this.ctDrawParamList = ctDrawParamList;
        this.annotaions = annotaions;
    }

    public List<OfdPageVo> getOfdPageVoList() {
        return ofdPageVoList;
    }

    public void setOfdPageVoList(List<OfdPageVo> ofdPageVoList) {
        this.ofdPageVoList = ofdPageVoList;
    }

    public String getDocPath() {
        return docPath;
    }

    public List<CT_Font> getCtFontList() {
        return ctFontList;
    }

    public List<CT_ColorSpace> getCtColorSpaceList() {
        return ctColorSpaceList;
    }

    public List<CT_MultiMedia> getCtMultiMediaList() {
        return ctMultiMediaList;
    }

    public List<StampAnnotVo> getStampAnnotVos() {
        return stampAnnotVos;
    }

    public double getPageWidth() {
        return pageWidth;
    }

    public double getPageHeight() {
        return pageHeight;
    }

    public List<CT_DrawParam> getCtDrawParamList() {
        return ctDrawParamList;
    }

    public void setCtDrawParamList(List<CT_DrawParam> ctDrawParamList) {
        this.ctDrawParamList = ctDrawParamList;
    }

    public List<AnnotionVo> getAnnotaions() {
        return annotaions;
    }

    public List<CT_VectorG> getCtVectorGList() {
        return ctVectorGList;
    }
}
