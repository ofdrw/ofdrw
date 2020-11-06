package org.ofdrw.reader;

import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.ofdrw.core.annotation.Annotations;
import org.ofdrw.core.annotation.pageannot.AnnPage;
import org.ofdrw.core.annotation.pageannot.PageAnnot;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.pageObj.CT_TemplatePage;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.OFDResource;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicStructure.res.resources.ColorSpaces;
import org.ofdrw.core.basicStructure.res.resources.DrawParams;
import org.ofdrw.core.basicStructure.res.resources.Fonts;
import org.ofdrw.core.basicStructure.res.resources.MultiMedias;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.signatures.Signature;
import org.ofdrw.core.signatures.sig.SignedInfo;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.gm.ses.parse.SESVersion;
import org.ofdrw.gm.ses.parse.SESVersionHolder;
import org.ofdrw.gm.ses.parse.VersionParser;
import org.ofdrw.gm.ses.v4.SES_Signature;
import org.ofdrw.gm.ses.v4.TBS_Sign;
import org.ofdrw.reader.model.AnnotionVo;
import org.ofdrw.reader.model.OFDDocumentVo;
import org.ofdrw.reader.model.OfdPageVo;
import org.ofdrw.reader.model.StampAnnotVo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * ofd解析器
 *
 * @author dltech21
 * @time 2020/8/11
 */
public class DLOFDReader extends OFDReader {
    private ST_Loc docRoot;
    private Document document;
    private ST_Box documentBox;
    private OFDDocumentVo ofdDocumentVo;

    public DLOFDReader(Path ofdFile) throws IOException {
        super(ofdFile);
        try {
            this.getResourceLocator().save();
            DocBody docBody = this.getOFDDir().getOfd().getDocBody();
            docRoot = docBody.getDocRoot();
            document = this.getResourceLocator().get(docRoot, Document::new);
            getPageBox();
            ofdDocumentVo = new OFDDocumentVo(docRoot.parent(), documentBox.getWidth(), documentBox.getHeight(), getOFDPageVO(), getPublicResFonts(), getPublicResColorSpaces(), getDocumentRes(), getStampAnnot(), getPublicResDrawParam(), getAnnotaions());
        } catch (DocumentException | FileNotFoundException var15) {
            throw new RuntimeException("OFD解析失败，原因:" + var15.getMessage(), var15);
        } finally {
            this.getResourceLocator().restore();
        }
    }

    public OFDDocumentVo getOFDDocumentVo() {
        return ofdDocumentVo;
    }

    private void getPageBox() {
        documentBox = null;
        try {
            documentBox = document.getCommonData().getPageArea().getPhysicalBox();
            if (documentBox == null) {
                documentBox = document.getCommonData().getPageArea().getApplicationBox();
            }
            if (documentBox == null) {
                documentBox = document.getCommonData().getPageArea().getContentBox();
            }
            if (documentBox == null) {
                new ST_Box(0, 0, 210, 140);
            }
        } catch (Exception e) {
            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
        }
    }

    private Page getTemplateInfo(String templateId) {
        Page obj;
        try {
            this.getResourceLocator().save();
            this.getResourceLocator().cd(docRoot.parent());
            List<CT_TemplatePage> ct_templatePages = document.getCommonData().getTemplatePages();
            int index = -1;
            for (int i = 0; i < ct_templatePages.size(); i++) {
                if (ct_templatePages.get(i).getObjID().toString().equals(templateId)) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                throw new NumberFormatException("不存在该模板id:" + templateId);
            }
            ST_Loc templateLoc = (ct_templatePages.get(index)).getBaseLoc();
            obj = this.getResourceLocator().get(templateLoc, Page::new);
        } catch (DocumentException | FileNotFoundException var15) {
            throw new RuntimeException("OFD解析失败，原因:" + var15.getMessage(), var15);
        } finally {
            this.getResourceLocator().restore();
        }

        return obj;

    }

    private List<OfdPageVo> getOFDPageVO() {
        List<OfdPageVo> pageVoList = new ArrayList<>();
        try {
            int pageSize = document.getPages().getSize();
            Page page;
            for (int i = 0; i < pageSize; i++) {
                page = this.getPage(i + 1);
                page.setObjID(document.getPages().getPages().get(i).getID());
                OfdPageVo ofdPageVo;
                if (!Objects.isNull(page.getTemplate())) {
                    String templateId = page.getTemplate().getTemplateID().toString();
                    Page tmplPage = this.getTemplateInfo(templateId);
                    ofdPageVo = new OfdPageVo(page, tmplPage);
                } else {
                    ofdPageVo = new OfdPageVo(page, null);
                }
                pageVoList.add(ofdPageVo);
            }
        } catch (Exception e) {
            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            this.getResourceLocator().restore();
        }
        return pageVoList;
    }

    private List<CT_Font> getPublicResFonts() {
        List<CT_Font> ctFontList = new ArrayList<>();
        try {
            this.getResourceLocator().save();
            this.getResourceLocator().cd(docRoot.parent());
            ST_Loc publicResLoc = document.getCommonData().getPublicRes();
            Res publicRes;
            if (!Objects.isNull(publicResLoc)) {
                publicRes = this.getResourceLocator().get(publicResLoc, Res::new);
            } else {
                publicResLoc = document.getCommonData().getDocumentRes();
                publicRes = this.getResourceLocator().get(publicResLoc, Res::new);
            }
            for (int i = 0; i < publicRes.getFonts().size(); i++) {
                Fonts fonts = publicRes.getFonts().get(i);
                ctFontList.addAll(fonts.getFonts());
            }

        } catch (Exception e) {
//            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            this.getResourceLocator().restore();
        }
        return ctFontList;
    }

    private List<CT_ColorSpace> getPublicResColorSpaces() {
        List<CT_ColorSpace> ctColorSpaceList = new ArrayList<>();
        try {
            this.getResourceLocator().save();
            this.getResourceLocator().cd(docRoot.parent());
            ST_Loc publicResLoc = document.getCommonData().getPublicRes();
            Res publicRes;
            if (!Objects.isNull(publicResLoc)) {
                publicRes = this.getResourceLocator().get(publicResLoc, Res::new);
            } else {
                publicResLoc = document.getCommonData().getDocumentRes();
                publicRes = this.getResourceLocator().get(publicResLoc, Res::new);
            }
            List<ColorSpaces> colorSpacesList = new ArrayList();
            Iterator var2 = publicRes.getResources().iterator();
            while (var2.hasNext()) {
                OFDResource item = (OFDResource) var2.next();
                if (item instanceof ColorSpaces) {
                    colorSpacesList.add((ColorSpaces) item);
                }
            }
            for (int i = 0; i < colorSpacesList.size(); i++) {
                ColorSpaces colorSpaces = colorSpacesList.get(i);
                ctColorSpaceList.addAll(colorSpaces.getColorSpaces());
            }

        } catch (Exception e) {
//            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            this.getResourceLocator().restore();
        }
        return ctColorSpaceList;
    }

    private List<CT_DrawParam> getPublicResDrawParam() {
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

    private List<CT_MultiMedia> getDocumentRes() {
        List<CT_MultiMedia> ctMultiMediaList = new ArrayList<>();
        try {
            this.getResourceLocator().save();
            this.getResourceLocator().cd(docRoot.parent());
            ST_Loc documentResLoc = document.getCommonData().getDocumentRes();
            if (!Objects.isNull(documentResLoc)) {
                Res publicRes = this.getResourceLocator().get(documentResLoc, Res::new);
                List<MultiMedias> multiMediasList = new ArrayList();
                Iterator var2 = publicRes.getResources().iterator();
                while (var2.hasNext()) {
                    OFDResource item = (OFDResource) var2.next();
                    if (item instanceof MultiMedias) {
                        multiMediasList.add((MultiMedias) item);
                    }
                }
                for (int i = 0; i < multiMediasList.size(); i++) {
                    MultiMedias multiMedias = multiMediasList.get(i);
                    ctMultiMediaList.addAll(multiMedias.getMultiMedias());
                }
            }
        } catch (Exception e) {
//            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            this.getResourceLocator().restore();
        }
        return ctMultiMediaList;
    }

    private List<StampAnnotVo> getStampAnnot() {
        List<StampAnnotVo> stampAnnotVoList = new ArrayList<>();
        try {
            if (hasSignature()) {
                this.getResourceLocator().save();
                this.getResourceLocator().cd(docRoot.parent());
                for (int i = 0; i < getDefaultSignatures().getSignatures().size(); i++) {
                    StampAnnotVo stampAnnotVo = new StampAnnotVo();
                    Signature signatures = getDefaultSignatures().getSignatures().get(i);
                    ST_Loc signatureBaseLoc = signatures.getBaseLoc();
                    if (signatureBaseLoc.toString().indexOf("Signs") == -1) {
                        signatureBaseLoc = new ST_Loc("Signs/" + signatureBaseLoc.toString());
                    }
                    org.ofdrw.core.signatures.sig.Signature signature = this.getResourceLocator().get(signatureBaseLoc, org.ofdrw.core.signatures.sig.Signature::new);
                    SignedInfo signedInfo = signature.getSignedInfo();
                    stampAnnotVo.setStampAnnots(signedInfo.getStampAnnots());
                    ST_Loc signedValueStLoc = signature.getSignedValue();
                    String srcPath = signedValueStLoc.toString();
                    byte[] bytes = FileUtils.readFileToByteArray(new File(getOFDDir().getFile(srcPath).toAbsolutePath().toString()));
                    SESVersionHolder v = VersionParser.parseSES_SignatureVersion(bytes);
                    String type = null;
                    byte[] sealBytes = new byte[0];
                    if (v.getVersion() == SESVersion.v4) {
                        SES_Signature sesSignature = SES_Signature.getInstance(bytes);
                        TBS_Sign toSign = sesSignature.getToSign();
                        type = toSign.getEseal().geteSealInfo().getPicture().getType().getString();
                        sealBytes = toSign.getEseal().geteSealInfo().getPicture().getData().getOctets();
                    } else if (v.getVersion() == SESVersion.v1) {
                        org.ofdrw.gm.ses.v1.SES_Signature sesSignature = org.ofdrw.gm.ses.v1.SES_Signature.getInstance(bytes);
                        org.ofdrw.gm.ses.v1.TBS_Sign toSign = sesSignature.getToSign();
                        type = toSign.getEseal().getEsealInfo().getPicture().getType().getString();
                        sealBytes = toSign.getEseal().getEsealInfo().getPicture().getData().getOctets();
                    } else {
//                throw new OFDVerifyException("未知的电子签章数据版本，无法解析");
                    }
                    if (type != null) {
                        stampAnnotVo.setType(type.toLowerCase());
                        if (type.toLowerCase().equals("ofd")) {
                            String sealFilePath = getOFDDir().getFile(srcPath).getParent().toString() + "/seal.ofd";
                            FileUtils.writeByteArrayToFile(new File(sealFilePath), sealBytes);
                            Path src = Paths.get(sealFilePath);
                            SealOFDReader sealReader = new SealOFDReader(src);
                            stampAnnotVo.setOfdPageVoList(sealReader.getOFDPageVO());
                            stampAnnotVo.setCtDrawParamList(sealReader.getPublicResDrawParam());
                            stampAnnotVo.setCtFontList(sealReader.getPublicResFonts());
                            stampAnnotVoList.add(stampAnnotVo);
                        } else if (type.toLowerCase().equals("png")) {
                            stampAnnotVo.setImgByte(sealBytes);
                            stampAnnotVoList.add(stampAnnotVo);
                        }
                    }
                }
            }
        } catch (Exception e) {
//            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            this.getResourceLocator().restore();
        }
        return stampAnnotVoList;
    }

    private List<AnnotionVo> getAnnotaions() {
        List<AnnotionVo> annotaionList = new ArrayList<>();
        try {
            Annotations annotations = this.getAnnotations();
            List<AnnPage> annPages = annotations.getPages();
            AnnotionVo annotionVo;
            this.getResourceLocator().save();
            this.getResourceLocator().cd(docRoot.parent());
            for (AnnPage page : annPages) {
                annotionVo = new AnnotionVo();
                annotionVo.setPageId(page.getPageID().toString());
                ST_Loc antLoc = page.getFileLoc();
                if (antLoc.toString().contains(docRoot.parent())) {
                    antLoc = ST_Loc.getInstance(antLoc.toString().replace(docRoot.parent()+"/", "").replaceFirst("/",""));
                }
                try {
                    PageAnnot pageAnnot = this.getResourceLocator().get(antLoc, PageAnnot::new);
                    annotionVo.setAnnots(pageAnnot.getAnnots());
                    annotaionList.add(annotionVo);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
//            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            this.getResourceLocator().restore();
        }
        return annotaionList;
    }
}
