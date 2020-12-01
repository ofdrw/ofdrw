package org.ofdrw.reader;

import org.dom4j.DocumentException;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.res.OFDResource;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicStructure.res.resources.DrawParams;
import org.ofdrw.core.basicStructure.res.resources.Fonts;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.model.OfdPageVo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 印章ofd的解析器
 * @author dltech21
 * @since 2020/8/11
 */
public class SealOFDReader extends OFDReader {

    private ST_Loc docRoot;
    private Document document;

    public SealOFDReader(Path ofdFile) throws IOException {
        this(Files.newInputStream(ofdFile));
    }

    public SealOFDReader(InputStream inputStream) throws IOException {
        super(inputStream);
        try {
            this.getResourceLocator().save();
            DocBody docBody = this.getOFDDir().getOfd().getDocBody();
            docRoot = docBody.getDocRoot();
            document = this.getResourceLocator().get(docRoot, Document::new);
        } catch (DocumentException | FileNotFoundException var15) {
            throw new RuntimeException("OFD解析失败，原因:" + var15.getMessage(), var15);
        } finally {
            this.getResourceLocator().restore();
        }
    }

    public List<OfdPageVo> getOFDPageVO() {
        List<OfdPageVo> pageVoList = new ArrayList<>();
        try {
            int pageSize = document.getPages().getSize();
            Page page;
            for (int i = 0; i < pageSize; i++) {
                page = this.getPage(i + 1);
                OfdPageVo ofdPageVo;
                ofdPageVo = new OfdPageVo(page, null);
                pageVoList.add(ofdPageVo);
            }
        } catch (Exception e) {
            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            this.getResourceLocator().restore();
        }
        return pageVoList;
    }

    public List<CT_Font> getPublicResFonts() {
        List<CT_Font> ctFontList = new ArrayList<>();
        try {
            this.getResourceLocator().save();
            this.getResourceLocator().cd(docRoot.parent());
            ST_Loc publicResLoc = document.getCommonData().getPublicRes();
            Res publicRes = this.getResourceLocator().get(publicResLoc, Res::new);
            for (int i = 0; i < publicRes.getFonts().size(); i++) {
                Fonts fonts = publicRes.getFonts().get(i);
                ctFontList.addAll(fonts.getFonts());
            }
        } catch (Exception e) {
            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            this.getResourceLocator().restore();
        }
        return ctFontList;
    }

    public List<CT_DrawParam> getPublicResDrawParam() {
        List<CT_DrawParam> ctDrawParamList = new ArrayList<>();
        try {
            this.getResourceLocator().save();
            this.getResourceLocator().cd(docRoot.parent());
            ST_Loc publicResLoc = document.getCommonData().getPublicRes();
            Res publicRes = this.getResourceLocator().get(publicResLoc, Res::new);
            List<DrawParams> drawParamsList = new ArrayList();
            Iterator var2 = publicRes.getResources().iterator();
            while (var2.hasNext()) {
                OFDResource item = (OFDResource) var2.next();
                if (item instanceof DrawParams) {
                    drawParamsList.add((DrawParams) item);
                }
            }
            for (int i = 0; i < drawParamsList.size(); i++) {
                DrawParams drawParams = drawParamsList.get(i);
                ctDrawParamList.addAll(drawParams.getDrawParams());
            }
        } catch (Exception e) {
        } finally {
            this.getResourceLocator().restore();
        }
        return ctDrawParamList;
    }
}
