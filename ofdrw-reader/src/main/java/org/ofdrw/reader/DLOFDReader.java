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

import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_Loc;

import org.ofdrw.core.signatures.Signature;
import org.ofdrw.core.signatures.sig.SignedInfo;
import org.ofdrw.gm.ses.parse.SESVersion;
import org.ofdrw.gm.ses.parse.SESVersionHolder;
import org.ofdrw.gm.ses.parse.VersionParser;
import org.ofdrw.gm.ses.v4.SES_Signature;
import org.ofdrw.gm.ses.v4.TBS_Sign;
import org.ofdrw.reader.model.AnnotionEntity;
import org.ofdrw.reader.model.OFDDocumentVo;
import org.ofdrw.reader.model.OfdPageVo;
import org.ofdrw.reader.model.StampAnnotVo;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * ofd解析器
 *
 * @author dltech21
 * @since 2020/8/11
 * @deprecated {@link OFDReader}
 */
@Deprecated
public class DLOFDReader extends OFDReader {
    private ST_Loc docRoot;
    private Document document;
    private ST_Box documentBox;
    private OFDDocumentVo ofdDocumentVo;
    /**
     * 资源管理器
     */
    private ResourceManage resMgt;

//    private  double pageWidth;
//    private  double pageHeight;
//
//    private List<OfdPageVo> ofdPageVoList;
//    private  List<StampAnnotVo> stampAnnotVos;
//    private  List<AnnotionVo> annotaions;


    public DLOFDReader(Path ofdFile) throws IOException {
        super(ofdFile);
        initReader();
    }

    public DLOFDReader(String ofdFileLoc) throws IOException {
        super(Paths.get(ofdFileLoc));
        initReader();
    }

    public DLOFDReader(InputStream stream) throws IOException {
        super(stream);
        initReader();
    }

    /**
     * 因一些ofd文件无法使用ZipUtil解压缩，可以让用户自己在外面解压缩好后，传入根目录创建
     * 例如用户可以使用unzip或者unar等命令行方式解压缩
     *
     * @param unzippedPathRoot 已经解压的OFD根目录位置，因此通过参数控制是否删除目录。
     * @param deleteOnClose    退出时是否删除 unzippedPathRoot 文件， true - 退出时删除；false - 不删除
     */
    public DLOFDReader(String unzippedPathRoot, boolean deleteOnClose) {
        super(unzippedPathRoot, deleteOnClose);
        initReader();
    }

    /**
     * 初始化reader
     */
    private void initReader() {
        try {
            resMgt = new ResourceManage(this);
            this.getResourceLocator().save();
            DocBody docBody = this.getOFDDir().getOfd().getDocBody();
            docRoot = docBody.getDocRoot();
            document = this.getResourceLocator().get(docRoot, Document::new);
            getPageBox();
            ofdDocumentVo = new OFDDocumentVo(docRoot.parent(),
                    documentBox.getWidth(), documentBox.getHeight(),
                    getOFDPageVO(),
                    getStampAnnot(),
                    getAnnotaions());
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

    public List<OfdPageVo> getOFDPageVO() {
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
                        stampAnnotVo.setType(type);
                        stampAnnotVo.setImgByte(sealBytes);
                        stampAnnotVoList.add(stampAnnotVo);
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

    private List<AnnotionEntity> getAnnotaions() {
        List<AnnotionEntity> annotaionList = new ArrayList<>();
        try {
            Annotations annotations = this.getAnnotations();
            if (annotations == null) {
                return annotaionList;
            }
            List<AnnPage> annPages = annotations.getPages();
            this.getResourceLocator().save();
            this.getResourceLocator().cd(docRoot.parent());
            for (AnnPage page : annPages) {

                ST_Loc antLoc = page.getFileLoc();
                if (antLoc.toString().contains(docRoot.parent())) {
                    antLoc = ST_Loc.getInstance(antLoc.toString().replace(docRoot.parent() + "/", "").replaceFirst("/", ""));
                }
                if (!antLoc.toString().contains(document.getAnnotations().parent())) {
                    antLoc = ST_Loc.getInstance(document.getAnnotations().parent() + "/" + antLoc);
                }
                try {
                    PageAnnot pageAnnot = this.getResourceLocator().get(antLoc, PageAnnot::new);
                    annotaionList.add(new AnnotionEntity(page.getPageID().toString(),pageAnnot.getAnnots()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
//            throw new RuntimeException("OFD解析失败，原因:" + e.getMessage(), e);
        } finally {
            this.getResourceLocator().restore();
        }
        return annotaionList;
    }

    /**
     * 获取资源管理器
     * <p>
     * 资源管理器获取到的对象均为只读对象
     *
     * @return 资源管理器
     */
    public ResourceManage getResMgt() {
        return resMgt;
    }
}
