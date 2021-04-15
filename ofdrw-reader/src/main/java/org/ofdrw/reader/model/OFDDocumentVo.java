package org.ofdrw.reader.model;


import java.util.List;

/**
 * @deprecated 即将过时 {@link org.ofdrw.reader.DLOFDReader}
 *
 * @author dltech21
 * @since 2020/8/11
 */
@Deprecated
public class OFDDocumentVo {
    private final String docPath;
    private final double pageWidth;
    private final double pageHeight;

    private List<OfdPageVo> ofdPageVoList;
    private final List<StampAnnotVo> stampAnnotVos;
    private final List<AnnotionVo> annotaions;

    public OFDDocumentVo(String docPath,
                         double pageWidth, double pageHeight,
                         List<OfdPageVo> ofdPageVoList,
                         List<StampAnnotVo> stampAnnotVoList,
                         List<AnnotionVo> annotaions) {
        this.docPath = docPath;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.ofdPageVoList = ofdPageVoList;
        this.stampAnnotVos = stampAnnotVoList;
        this.annotaions = annotaions;
    }

    public List<OfdPageVo> getOfdPageVoList() {
        return ofdPageVoList;
    }

    public String getDocPath() {
        return docPath;
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

    public List<AnnotionVo> getAnnotaions() {
        return annotaions;
    }


}
