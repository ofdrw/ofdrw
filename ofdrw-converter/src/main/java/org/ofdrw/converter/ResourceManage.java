package org.ofdrw.converter;

import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.DLOFDReader;

import java.util.HashMap;
import java.util.Map;

public class ResourceManage {
    private final Map<String, CT_DrawParam> ctDrawParamMap = new HashMap<>();
    private final Map<String, CT_MultiMedia> ctMultiMediaMap = new HashMap<>();
    private final Map<String, CT_Font> ctFontMap = new HashMap<>();
    private final Map<String, CT_ColorSpace> ctColorSpaceMap = new HashMap<>();

    private final Map<String, Object> map = new HashMap<>();

    private final DLOFDReader ofdReader;

    public ResourceManage(DLOFDReader ofdReader) {
        this.ofdReader = ofdReader;

        for (CT_DrawParam drawParam : ofdReader.getOFDDocumentVo().getCtDrawParamList()) {
            ctDrawParamMap.put(drawParam.getID().toString(), drawParam);
        }
        for (CT_MultiMedia multiMedia : ofdReader.getOFDDocumentVo().getCtMultiMediaList()) {
            ctMultiMediaMap.put(multiMedia.getID().toString(), multiMedia);
        }
        for (CT_Font ct_font : ofdReader.getOFDDocumentVo().getCtFontList()) {
            ctFontMap.put(ct_font.getID().toString(), ct_font);
        }
        for (CT_ColorSpace ctColorSpace : ofdReader.getOFDDocumentVo().getCtColorSpaceList()) {
            ctColorSpaceMap.put(ctColorSpace.getID().toString(), ctColorSpace);
        }
    }

    public CT_DrawParam getDrawParam(String id) {
        return ctDrawParamMap.get(id);
    }

    public CT_MultiMedia getMultiMedia(String id) {
        return ctMultiMediaMap.get(id);
    }

    public CT_Font getFont(String id) {
        return ctFontMap.get(id);
    }

    public CT_ColorSpace getColorSpace(String id) {
        return ctColorSpaceMap.get(id);
    }
}
