package org.ofdrw.converter;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.jbig2.JBIG2ImageReader;
import org.apache.pdfbox.jbig2.JBIG2ImageReaderSpi;
import org.apache.pdfbox.jbig2.io.DefaultInputStreamFactory;
import org.apache.pdfbox.jbig2.util.log.Logger;
import org.apache.pdfbox.jbig2.util.log.LoggerFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.ofdrw.converter.image.ImageMedia;
import org.ofdrw.converter.point.PathPoint;
import org.ofdrw.converter.point.TextCodePoint;
import org.ofdrw.converter.utils.CommonUtil;
import org.ofdrw.converter.utils.PointUtil;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.*;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.compositeObj.CT_VectorG;
import org.ofdrw.core.graph.pathObj.FillColor;
import org.ofdrw.core.graph.pathObj.StrokeColor;
import org.ofdrw.core.pageDescription.color.color.CT_AxialShd;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.DLOFDReader;
import org.ofdrw.reader.model.AnnotionVo;
import org.ofdrw.reader.model.OfdPageVo;
import org.ofdrw.reader.model.StampAnnotVo;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.ofdrw.converter.utils.CommonUtil.*;


public class PdfboxMaker {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, PDFont> pdfFontMap;
    private final Map<String, ImageMedia> imageMap;
    private final Map<String, CT_DrawParam> ctDrawParamMap;

    private final DLOFDReader ofdReader;
    private final PDDocument pdf;
    private final PdfBoxFontHolder fontHolder;

    public PdfboxMaker(DLOFDReader ofdReader, PDDocument pdf) throws IOException {
        this.ofdReader = ofdReader;
        this.pdf = pdf;
        imageMap = new HashMap<>();
        pdfFontMap = new HashMap<>();
        ctDrawParamMap = new HashMap<>();
        fontHolder = new PdfBoxFontHolder(pdf);
        for (CT_Font ctFont : ofdReader.getOFDDocumentVo().getCtFontList()) {
            pdfFontMap.put(ctFont.getObjID().toString(), fontHolder.getFont(ctFont.getFontName()));
        }

        String srcPath;
        for (CT_MultiMedia multiMedia : ofdReader.getOFDDocumentVo().getCtMultiMediaList()) {
            srcPath = ofdReader.getOFDDir().getSysAbsPath() + "/" + ofdReader.getOFDDocumentVo().getDocPath() + "/" + multiMedia.getMediaFile().toString();
            File imgFile = new File(srcPath);

            ImageMedia image = new ImageMedia();
            image.setFromat(multiMedia.getFormat());

            if (imgFile.exists()) {
                image.setData(FileUtils.readFileToByteArray(imgFile));
                this.imageMap.put(multiMedia.getID().toString(), image);
            } else {
                srcPath = ofdReader.getOFDDir().getSysAbsPath() + "/" + ofdReader.getOFDDocumentVo().getDocPath() + "/Res/" + multiMedia.getMediaFile().toString();
                imgFile = new File(srcPath);
                if (imgFile.exists()) {
                    image.setData(FileUtils.readFileToByteArray(imgFile));
                    this.imageMap.put(multiMedia.getID().toString(), image);
                }
            }

        }

        for (int j = 0; j < ofdReader.getOFDDocumentVo().getCtDrawParamList().size(); j++) {
            ctDrawParamMap.put(ofdReader.getOFDDocumentVo().getCtDrawParamList().get(j).getID().toString(), ofdReader.getOFDDocumentVo().getCtDrawParamList().get(j));
        }

        for (int i = 0; i < ofdReader.getOFDDocumentVo().getStampAnnotVos().size(); i++) {
            StampAnnotVo stampAnnotVo = ofdReader.getOFDDocumentVo().getStampAnnotVos().get(i);
            if (stampAnnotVo.getType().equals("ofd")) {
                for (int j = 0; j < stampAnnotVo.getCtDrawParamList().size(); j++) {
                    ctDrawParamMap.put(stampAnnotVo.getCtDrawParamList().get(j).getID().toString() + "s", stampAnnotVo.getCtDrawParamList().get(j));
                }
                for (CT_Font ctFont : stampAnnotVo.getCtFontList()) {
                    pdfFontMap.put(ctFont.getObjID().toString() + "s", fontHolder.getFont(ctFont.getFontName()));
                }
            }
        }


    }

    public PDPage makePage(OfdPageVo pageVo) throws IOException {
        Page contentPage = pageVo.getContentPage();
        Page templatePage = pageVo.getTemplatePage();
        ST_Box pageBox = getPageBox(contentPage.getArea(), ofdReader.getOFDDocumentVo().getPageWidth(), ofdReader.getOFDDocumentVo().getPageHeight());
        double pageWidthPixel = converterDpi(pageBox.getWidth());
        double pageHeightPixel = converterDpi(pageBox.getHeight());
        PDRectangle pageSize = new PDRectangle((float) pageWidthPixel, (float) pageHeightPixel);
        PDPage pdfPage = new PDPage(pageSize);
        pdf.addPage(pdfPage);
        PDPageContentStream contentStream = new PDPageContentStream(pdf, pdfPage);
        // make tpl content
        List<CT_Layer> layerList;
        if (!Objects.isNull(templatePage)) {
            layerList = templatePage.getContent().getLayers();
            writeLayer(pdf, contentStream, layerList, pageBox, null);
        }

        // make page content
        layerList = contentPage.getContent().getLayers();
        writeLayer(pdf, contentStream, layerList, pageBox, null);

        // make seal content
        for (int i = 0; i < ofdReader.getOFDDocumentVo().getStampAnnotVos().size(); i++) {
            StampAnnotVo stampAnnotVo = ofdReader.getOFDDocumentVo().getStampAnnotVos().get(i);
            List<StampAnnot> stampAnnots = stampAnnotVo.getStampAnnots();
            for (int j = 0; j < stampAnnots.size(); j++) {
                StampAnnot stampAnnot = stampAnnots.get(j);
                if (stampAnnot.getPageRef().toString().equals(contentPage.getObjID().toString())) {
                    ST_Box sealBox = stampAnnot.getBoundary();
                    ST_Box clipBox = stampAnnot.getClip();
                    if (stampAnnotVo.getType().equals("ofd")) {
                        for (int k = 0; k < stampAnnotVo.getOfdPageVoList().size(); k++) {
                            OfdPageVo sealPageVo = stampAnnotVo.getOfdPageVoList().get(k);
                            layerList = sealPageVo.getContentPage().getContent().getLayers();
                            writeLayer(pdf, contentStream, layerList, pageBox, sealBox);
                        }
                    } else if (stampAnnotVo.getType().equals("png")) {
                        writeSealImage(contentStream, pageBox, stampAnnotVo.getImgByte(), sealBox, clipBox);
                    }
                }
            }
        }

        for (AnnotionVo amap : ofdReader.getOFDDocumentVo().getAnnotaions()) {
            if (pageVo.getContentPage().getObjID().toString().equals(amap.getPageId()) && null != amap.getAnnots()) {
                writeAnnoAppearance(pdf, contentStream, amap.getAnnots(), pageBox);
            }
        }
        contentStream.close();
        return pdfPage;
    }

    private void writeLayer(PDDocument pdf, PDPageContentStream contentStream, List<CT_Layer> layerList, ST_Box box, ST_Box sealBox) throws IOException {
        for (CT_Layer layer : layerList) {
            List<PageBlockType> pageBlockTypeList = layer.getPageBlocks();
            writePageBlock(pdf, contentStream, box, sealBox, pageBlockTypeList, layer.getDrawParam(), null, null, null, null);
        }
    }

    private void writeAnnoAppearance(PDDocument pdf, PDPageContentStream contentStream, List<Annot> annotList, ST_Box box) throws IOException {
        for (Annot annot : annotList) {
            List<PageBlockType> pageBlockTypeList = annot.getAppearance().getPageBlocks();
            //注释的boundary
            ST_Box annotBox = annot.getAppearance().getBoundary();
            writePageBlock(pdf, contentStream, box, null, pageBlockTypeList, null, annotBox, null, null, null);
        }
    }

    private void writePageBlock(PDDocument pdf, PDPageContentStream contentStream, ST_Box box, ST_Box sealBox, List<PageBlockType> pageBlockTypeList, ST_RefID drawparam, ST_Box annotBox, Integer compositeObjectAlpha, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM) throws IOException {
        PDColor defaultFillColor = new PDColor(new float[]{0.0f, 0.0f, 0.0f}, PDDeviceRGB.INSTANCE);
        PDColor defaultStrokeColor = new PDColor(new float[]{0.0f, 0.0f, 0.0f}, PDDeviceRGB.INSTANCE);
        float defaultLineWidth = 0.353f;
        if (drawparam != null) {
            CT_DrawParam ctDrawParam = ctDrawParamMap.get(drawparam.getRefId().toString() + (sealBox == null ? "" : "s"));
            if (ctDrawParam != null) {
                if (ctDrawParam.getRelative() != null) {
                    CT_DrawParam re = ctDrawParamMap.get(ctDrawParam.getRelative().getRefId().toString() + (sealBox == null ? "" : "s"));
                    if (re == null) {
                        return;
                    }
                    if (re.getStrokeColor() != null) {
                        defaultStrokeColor = convertPDColor(re.getStrokeColor().getValue());
                    }
                    if (re.getFillColor() != null) {
                        defaultFillColor = convertPDColor(re.getFillColor().getValue());
                    }
                    if (re.getLineWidth() != null) {
                        defaultLineWidth = re.getLineWidth().floatValue();
                    }
                }

                if (ctDrawParam.getStrokeColor() != null) {
                    defaultStrokeColor = convertPDColor(ctDrawParam.getStrokeColor().getValue());
                }
                if (ctDrawParam.getFillColor() != null) {
                    defaultFillColor = convertPDColor(ctDrawParam.getFillColor().getValue());
                }
                if (ctDrawParam.getLineWidth() != null) {
                    defaultLineWidth = ctDrawParam.getLineWidth().floatValue();
                }
            }
        }
        for (PageBlockType block : pageBlockTypeList) {
            TextObject textObject;
            ImageObject imageObject;
            PathObject pathObject;
            CompositeObject compositeObject;

            if (block instanceof TextObject) {
                // text
                PDColor fillColor = defaultFillColor;
                textObject = (TextObject) block;
                int alpha = 255;
                if (textObject.getFillColor() != null) {
                    if (textObject.getFillColor().getValue() != null) {
                        fillColor = convertPDColor(textObject.getFillColor().getValue());
                    } else if (textObject.getFillColor().getColorByType() != null){
                        // todo
                        CT_AxialShd ctAxialShd = textObject.getFillColor().getColorByType();
                        fillColor = convertPDColor(ctAxialShd.getSegments().get(0).getColor().getValue());
                    }
                    alpha = textObject.getFillColor().getAlpha();
                }
                writeText(contentStream, box, sealBox, textObject, fillColor, alpha);
            } else if (block instanceof ImageObject) {
                // image
                imageObject = (ImageObject) block;
                writeImage(contentStream, box, imageObject, annotBox);

            } else if (block instanceof PathObject) {
                // path
                pathObject = (PathObject) block;
                writePath(contentStream, box, sealBox, annotBox, pathObject, defaultFillColor, defaultStrokeColor, defaultLineWidth, compositeObjectAlpha, compositeObjectBoundary, compositeObjectCTM);
            } else if (block instanceof CompositeObject) {
                compositeObject = (CompositeObject) block;
                for (CT_VectorG vectorG : ofdReader.getOFDDocumentVo().getCtVectorGList()) {
                    if (vectorG.getID().toString().equals(compositeObject.getResourceID().toString())) {
                        Integer currentCompositeObjectAlpha = compositeObject.getAlpha();
                        ST_Box currentCompositeObjectBoundary = compositeObject.getBoundary();
                        ST_Array currentCompositeObjectCTM = compositeObject.getCTM();
                        writePageBlock(pdf, contentStream, box, sealBox, vectorG.getContent().getPageBlocks(), drawparam, annotBox, currentCompositeObjectAlpha, currentCompositeObjectBoundary, currentCompositeObjectCTM);
                        break;
                    }
                }
            } else if (block instanceof CT_PageBlock) {
                writePageBlock(pdf, contentStream, box, sealBox, ((CT_PageBlock) block).getPageBlocks(), drawparam, annotBox, compositeObjectAlpha, compositeObjectBoundary, compositeObjectCTM);
            }
        }
    }

    private void writePath(PDPageContentStream contentStream, ST_Box box, ST_Box sealBox, ST_Box annotBox, PathObject pathObject, PDColor defaultFillColor, PDColor defaultStrokeColor, float defaultLineWidth, Integer compositeObjectAlpha, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM) throws IOException {
        contentStream.saveGraphicsState();
        if (pathObject.getStrokeColor() != null) {
            StrokeColor strokeColor = pathObject.getStrokeColor();
            if (strokeColor.getValue() != null) {
                contentStream.setStrokingColor(convertPDColor(strokeColor.getValue()));
            } else {
                setShadingFill(contentStream, strokeColor, false);
            }
        } else {
            contentStream.setStrokingColor(defaultStrokeColor);
        }
        float lineWidth = pathObject.getLineWidth() != null ? pathObject.getLineWidth().floatValue() : defaultLineWidth;
        if (pathObject.getDrawParam() != null) {
            CT_DrawParam ctDrawParam = ctDrawParamMap.get(pathObject.getDrawParam().getRefId().toString());
            if (ctDrawParam != null) {
                if (ctDrawParam.getLineWidth() != null) {
                    lineWidth = ctDrawParam.getLineWidth().floatValue();
                }
            }
        }
        if (pathObject.getCTM() != null && pathObject.getLineWidth() != null) {
            Double[] ctm = pathObject.getCTM().toDouble();
            double a = ctm[0].doubleValue();
            double b = ctm[1].doubleValue();
            double c = ctm[2].doubleValue();
            double d = ctm[3].doubleValue();
            double e = ctm[4].doubleValue();
            double f = ctm[5].doubleValue();
            double sx = Math.signum(a) * Math.sqrt(a * a + c * c);
            double sy = Math.signum(d) * Math.sqrt(b * b + d * d);
            lineWidth = (float) (lineWidth * sx);
        }
        if (pathObject.getStroke()) {
            if (compositeObjectAlpha != null) {
                PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                graphicsState.setStrokingAlphaConstant(compositeObjectAlpha * 1.0f / 255);
                contentStream.setGraphicsStateParameters(graphicsState);
            }
            if (pathObject.getDashPattern() != null) {
                float unitsOn = (float) converterDpi(pathObject.getDashPattern().toDouble()[0].floatValue());
                float unitsOff = (float) converterDpi(pathObject.getDashPattern().toDouble()[1].floatValue());
                float phase = (float) converterDpi(pathObject.getDashOffset().floatValue());
                contentStream.setLineDashPattern(new float[]{unitsOn, unitsOff}, phase);
            }
            contentStream.setLineJoinStyle(pathObject.getJoin().ordinal());
            contentStream.setLineCapStyle(pathObject.getCap().ordinal());
            contentStream.setMiterLimit(pathObject.getMiterLimit().floatValue());
            path(contentStream, box, sealBox, annotBox, pathObject, compositeObjectBoundary, compositeObjectCTM);
            contentStream.setLineWidth((float) converterDpi(lineWidth));
            contentStream.stroke();
            contentStream.restoreGraphicsState();
        }
        if (pathObject.getFill()) {
            contentStream.saveGraphicsState();
            if (compositeObjectAlpha != null) {
                PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                graphicsState.setNonStrokingAlphaConstant(compositeObjectAlpha * 1.0f / 255);
                contentStream.setGraphicsStateParameters(graphicsState);
            }
            FillColor fillColor = (FillColor) pathObject.getFillColor();
            if (fillColor != null) {
                if (fillColor.getValue() != null) {
                    contentStream.setNonStrokingColor(convertPDColor(fillColor.getValue()));
                } else {
                    // todo
                    setShadingFill(contentStream, fillColor, true);
                }
            } else {
                contentStream.setNonStrokingColor(defaultFillColor);
            }
            path(contentStream, box, sealBox, annotBox, pathObject, compositeObjectBoundary, compositeObjectCTM);
            contentStream.fill();
            contentStream.restoreGraphicsState();
        }
    }

    private void setShadingFill(PDPageContentStream contentStream, CT_Color ctColor, boolean isFill) throws IOException {
        CT_AxialShd ctAxialShd = ctColor.getColorByType();
        if (ctAxialShd == null) {
            return;
        }
        ST_Array start = ctAxialShd.getSegments().get(0).getColor().getValue();
        ST_Array end = ctAxialShd.getSegments().get(ctAxialShd.getSegments().size() - 1).getColor().getValue();
        ST_Pos startPos = ctAxialShd.getStartPoint();
        ST_Pos endPos = ctAxialShd.getEndPoint();
        if (isFill) {
            contentStream.setNonStrokingColor(convertPDColor(end));
        } else {
            contentStream.setStrokingColor(convertPDColor(end));
        }
//                    COSDictionary fdict = new COSDictionary();
//                    fdict.setInt(COSName.FUNCTION_TYPE, 2); // still not understaning that...
//                    COSArray domain = new COSArray();
//                    domain.add(COSInteger.get(0));
//                    domain.add(COSInteger.get(1));
//                    COSArray c0 = new COSArray();
//                    Double[] first = ctAxialShd.getSegments().get(0).getColor().getValue().toDouble();
//                    Double[] end = ctAxialShd.getSegments().get(ctAxialShd.getSegments().size() - 1).getColor().getValue().toDouble();
//                    c0.add(COSFloat.get(String.format("%.2f", first[0] * 1.0 / 255)));
//                    c0.add(COSFloat.get(String.format("%.2f", first[1] * 1.0 / 255)));
//                    c0.add(COSFloat.get(String.format("%.2f", first[2] * 1.0 / 255)));
//                    COSArray c2 = new COSArray();
//                    c2.add(COSFloat.get(String.format("%.2f", end[0] * 1.0 / 255)));
//                    c2.add(COSFloat.get(String.format("%.2f", end[1] * 1.0 / 255)));
//                    c2.add(COSFloat.get(String.format("%.2f", end[2] * 1.0 / 255)));
//                    fdict.setItem(COSName.DOMAIN, domain);
//                    fdict.setItem(COSName.C0, c0);
//                    fdict.setItem(COSName.C1, c2);
//                    fdict.setInt(COSName.N, 1);
//
//                    PDFunctionType2 func = new PDFunctionType2(fdict);
//
//                    PDShadingType2 axialShading = new PDShadingType2(new COSDictionary());
//                    axialShading.setColorSpace(PDDeviceRGB.INSTANCE);
//                    axialShading.setShadingType(PDShading.SHADING_TYPE2);
//                    COSArray coords1 = new COSArray();
//                    coords1.add(COSFloat.get(String.valueOf(ctAxialShd.getStartPoint().getX())));
//                    coords1.add(COSFloat.get(String.valueOf(ctAxialShd.getStartPoint().getY())));
//                    coords1.add(COSFloat.get(String.valueOf(ctAxialShd.getEndPoint().getX())));
//                    coords1.add(COSFloat.get(String.valueOf(ctAxialShd.getEndPoint().getY())));
//                    axialShading.setCoords(coords1); // so this sets the bounds of my gradient
//                    axialShading.setFunction(func); // and this determines all the curves etc?
//                    contentStream.shadingFill(axialShading);
    }

    private void path(PDPageContentStream contentStream, ST_Box box, ST_Box sealBox, ST_Box annotBox, PathObject pathObject, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM) throws IOException {
        if (pathObject.getBoundary() == null) {
            return;
        }
        if (sealBox != null) {
            pathObject.setBoundary(pathObject.getBoundary().getTopLeftX() + sealBox.getTopLeftX(),
                    pathObject.getBoundary().getTopLeftY() + sealBox.getTopLeftY(),
                    pathObject.getBoundary().getWidth(),
                    pathObject.getBoundary().getHeight());
        }
        if (annotBox != null) {
            pathObject.setBoundary(pathObject.getBoundary().getTopLeftX() + annotBox.getTopLeftX(),
                    pathObject.getBoundary().getTopLeftY() + annotBox.getTopLeftY(),
                    pathObject.getBoundary().getWidth(),
                    pathObject.getBoundary().getHeight());
        }
        List<PathPoint> listPoint = PointUtil.calPdfPathPoint(box.getWidth(), box.getHeight(), pathObject.getBoundary(), PointUtil.convertPathAbbreviatedDatatoPoint(pathObject.getAbbreviatedData()), pathObject.getCTM() != null, pathObject.getCTM(), compositeObjectBoundary, compositeObjectCTM, true);
        for (int i = 0; i < listPoint.size(); i++) {
            if (listPoint.get(i).type.equals("M") || listPoint.get(i).type.equals("S")) {
                contentStream.moveTo(listPoint.get(i).x1, listPoint.get(i).y1);
            } else if (listPoint.get(i).type.equals("L")) {
                contentStream.lineTo(listPoint.get(i).x1, listPoint.get(i).y1);
            } else if (listPoint.get(i).type.equals("B")) {
                contentStream.curveTo(listPoint.get(i).x1, listPoint.get(i).y1,
                        listPoint.get(i).x2, listPoint.get(i).y2,
                        listPoint.get(i).x3, listPoint.get(i).y3);
            } else if (listPoint.get(i).type.equals("Q")) {
                contentStream.curveTo1(listPoint.get(i).x1, listPoint.get(i).y1,
                        listPoint.get(i).x2, listPoint.get(i).y2);
            } else if (listPoint.get(i).type.equals("C")) {
                contentStream.closePath();
            }
        }
    }

    private void writeImage(PDPageContentStream contentStream, ST_Box box, ImageObject imageObject, ST_Box annotBox) throws IOException {
        if (imageMap.get(imageObject.getResourceID().toString()) == null) {
            return;
        }
        contentStream.saveGraphicsState();

        ImageMedia image = imageMap.get(imageObject.getResourceID().toString());
        boolean isJb2 = "GBIG2".equals(image.getFromat()) || "JB2".equals(image.getFromat());
        PDImageXObject pdfImageObject = LosslessFactory.createFromImage(pdf,
                readImageFile(isJb2, new ByteArrayInputStream(image.getData())));

        if (annotBox != null) {
            float x = annotBox.getTopLeftX().floatValue();
            float y = box.getHeight().floatValue() - (annotBox.getTopLeftY().floatValue() + annotBox.getHeight().floatValue());
            float width = annotBox.getWidth().floatValue();
            float height = annotBox.getHeight().floatValue();
            contentStream.drawImage(pdfImageObject, (float) converterDpi(x), (float) converterDpi(y), (float) converterDpi(width), (float) converterDpi(height));
        } else {
            org.apache.pdfbox.util.Matrix matrix = CommonUtil.toPFMatrix(CommonUtil.getImageMatrixFromOfd(imageObject, box));
            contentStream.drawImage(pdfImageObject, matrix);
        }
        contentStream.restoreGraphicsState();
    }

    /**
     * 读取图片文件
     */
    public BufferedImage readImageFile(boolean isJb2, InputStream image) throws IOException {
        if (isJb2) {
            DefaultInputStreamFactory defaultInputStreamFactory = new DefaultInputStreamFactory();
            ImageInputStream imageInputStream = defaultInputStreamFactory.getInputStream(image);
            JBIG2ImageReader imageReader = new JBIG2ImageReader(new JBIG2ImageReaderSpi());
            imageReader.setInput(imageInputStream);
            return imageReader.read(0, imageReader.getDefaultReadParam());
        } else {
            return ImageIO.read(image);
        }
    }

    private void writeSealImage(PDPageContentStream contentStream, ST_Box box, byte[] image, ST_Box sealBox, ST_Box clipBox) throws IOException {
        if (image == null) {
            return;
        }
        contentStream.saveGraphicsState();
        PDImageXObject pdfImageObject = PDImageXObject.createFromByteArray(pdf, image, "");
        float x = sealBox.getTopLeftX().floatValue();
        float y = box.getHeight().floatValue() - (sealBox.getTopLeftY().floatValue() + sealBox.getHeight().floatValue());
        float width = sealBox.getWidth().floatValue();
        float height = sealBox.getHeight().floatValue();
        if (clipBox != null) {
            contentStream.addRect((float) converterDpi(x) + (float) converterDpi(clipBox.getTopLeftX()), (float) converterDpi(y) + (float) (converterDpi(height) - (converterDpi(clipBox.getTopLeftY()) + converterDpi(clipBox.getHeight()))), (float) converterDpi(clipBox.getWidth()), (float) converterDpi(clipBox.getHeight()));
            contentStream.closePath();
            contentStream.clip();
            contentStream.stroke();
        }
        contentStream.drawImage(pdfImageObject, (float) converterDpi(x), (float) converterDpi(y), (float) converterDpi(width), (float) converterDpi(height));
        contentStream.restoreGraphicsState();
    }

    private void writeText(PDPageContentStream contentStream, ST_Box box, ST_Box sealBox, TextObject textObject, PDColor fillColor, int alpha) throws IOException {
        float fontSize = textObject.getSize().floatValue();

        String fontAno = "";
        if (sealBox != null && textObject.getBoundary() != null) {
            fontAno = "s";
            textObject.setBoundary(textObject.getBoundary().getTopLeftX() + sealBox.getTopLeftX(),
                    textObject.getBoundary().getTopLeftY() + sealBox.getTopLeftY(),
                    textObject.getBoundary().getWidth(),
                    textObject.getBoundary().getHeight());
        }
        if (textObject.getCTM() != null) {
            Double[] ctm = textObject.getCTM().toDouble();
            double a = ctm[0];
            double b = ctm[1];
            double c = ctm[2];
            double d = ctm[3];
            double sx = a > 0 ? Math.signum(a) * Math.sqrt(a * a + c * c) : Math.sqrt(a * a + c * c);
            double sy = Math.signum(d) * Math.sqrt(b * b + d * d);
            fontSize = (float) (fontSize * sx);

        }
        PDFont font = this.pdfFontMap.get(textObject.getFont().toString() + fontAno);
        if (Objects.isNull(font)) font = fontHolder.getFont("宋体");

        List<TextCodePoint> textCodePointList = PointUtil.calPdfTextCoordinate(box.getWidth(), box.getHeight(), textObject.getBoundary(), fontSize, textObject.getTextCodes(), textObject.getCTM() != null, textObject.getCTM(), true);
        double rx = 0, ry = 0;
        for (int i = 0; i < textCodePointList.size(); i++) {
            TextCodePoint textCodePoint = textCodePointList.get(i);
            if (i == 0) {
                rx = textCodePoint.x;
                ry = textCodePoint.y;
            }
            contentStream.saveGraphicsState();
            contentStream.beginText();
            contentStream.setNonStrokingColor(fillColor);
            contentStream.newLineAtOffset((float) (textCodePoint.getX()), (float) (textCodePoint.getY()));
            if (textObject.getCTM() != null) {
                Double[] ctm = textObject.getCTM().toDouble();
                double a = ctm[0];
                double b = ctm[1];
                double c = ctm[2];
                double d = ctm[3];
                AffineTransform transform = new AffineTransform();
                double angel = Math.atan2(-b, d);
                transform.rotate(angel, rx, ry);
                contentStream.concatenate2CTM(transform);
            }
            contentStream.setFont(font, (float) converterDpi(fontSize));
            try {
                contentStream.showText(textCodePoint.getText());
            } catch (Exception e) {

            }
            contentStream.endText();
            contentStream.restoreGraphicsState();
        }

    }

}
