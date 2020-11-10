package org.ofdrw.converter;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.jbig2.util.log.Logger;
import org.apache.pdfbox.jbig2.util.log.LoggerFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.graph.pathObj.FillColor;
import org.ofdrw.core.graph.pathObj.StrokeColor;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.DLOFDReader;
import org.ofdrw.reader.model.AnnotionVo;
import org.ofdrw.reader.model.OfdPageVo;
import org.ofdrw.reader.model.StampAnnotVo;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.ofdrw.converter.CommonUtil.*;


public class PdfboxMaker {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, PDFont> pdfFontMap;
    private final Map<String, byte[]> imageMap;
    private final Map<String, CT_DrawParam> ctDrawParamMap;

    private final DLOFDReader ofdReader;
    private final PDDocument pdf;

    public PdfboxMaker(DLOFDReader ofdReader, PDDocument pdf) throws IOException {
        this.ofdReader = ofdReader;
        this.pdf = pdf;
        imageMap = new HashMap<>();
        pdfFontMap = new HashMap<>();
        ctDrawParamMap = new HashMap<>();
        for (CT_Font ctFont : ofdReader.getOFDDocumentVo().getCtFontList()) {
            pdfFontMap.put(ctFont.getObjID().toString(), PdfBoxFontHolder.getInstance(pdf).getFont(ctFont.getFontName()));
        }

        String srcPath;
        for (CT_MultiMedia multiMedia : ofdReader.getOFDDocumentVo().getCtMultiMediaList()) {
            srcPath = ofdReader.getOFDDir().getSysAbsPath() + "/" + ofdReader.getOFDDocumentVo().getDocPath() + "/" + multiMedia.getMediaFile().toString();
            File imgFile = new File(srcPath);
            if (imgFile.exists()) {
                this.imageMap.put(multiMedia.getID().toString(), FileUtils.readFileToByteArray(imgFile));
            } else {
                srcPath = ofdReader.getOFDDir().getSysAbsPath() + "/" + ofdReader.getOFDDocumentVo().getDocPath() + "/Res/" + multiMedia.getMediaFile().toString();
                imgFile = new File(srcPath);
                if (imgFile.exists()) {
                    this.imageMap.put(multiMedia.getID().toString(), FileUtils.readFileToByteArray(imgFile));
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
                    pdfFontMap.put(ctFont.getObjID().toString() + "s", PdfBoxFontHolder.getInstance(pdf).getFont(ctFont.getFontName()));
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
            writePageBlock(pdf, contentStream, box, sealBox, pageBlockTypeList, layer.getDrawParam());
        }
    }

    private void writeAnnoAppearance(PDDocument pdf, PDPageContentStream contentStream, List<Annot> annotList, ST_Box box) throws IOException {
        for (Annot annot : annotList) {
            List<PageBlockType> pageBlockTypeList = annot.getAppearance().getPageBlocks();
            writePageBlock(pdf, contentStream, box, null, pageBlockTypeList, null);
        }
    }

    private void writePageBlock(PDDocument pdf, PDPageContentStream contentStream, ST_Box box, ST_Box sealBox, List<PageBlockType> pageBlockTypeList, ST_RefID drawparam) throws IOException {
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

            if (block instanceof TextObject) {
                // text
                PDColor fillColor = defaultFillColor;
                textObject = (TextObject) block;
                int alpha = 255;
                if (textObject.getFillColor() != null) {
                    fillColor = convertPDColor(textObject.getFillColor().getValue());
                    alpha = textObject.getFillColor().getAlpha();
                }
                writeText(contentStream, box, sealBox, textObject, fillColor, alpha);
            } else if (block instanceof ImageObject) {
                // image
                imageObject = (ImageObject) block;
                writeImage(contentStream, box, imageObject);

            } else if (block instanceof PathObject) {
                // path
                pathObject = (PathObject) block;
                writePath(contentStream, box, sealBox, pathObject, defaultFillColor, defaultStrokeColor, defaultLineWidth);
            }
        }
    }

    private void writePath(PDPageContentStream contentStream, ST_Box box, ST_Box sealBox, PathObject pathObject, PDColor defaultFillColor, PDColor defaultStrokeColor, float defaultLineWidth) throws IOException {
        contentStream.saveGraphicsState();
        if (pathObject.getStrokeColor() != null) {
            StrokeColor fillColor = pathObject.getStrokeColor();
            contentStream.setStrokingColor(convertPDColor(fillColor.getValue()));
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
            if (pathObject.getDashPattern() != null) {
                float unitsOn = (float) converterDpi(pathObject.getDashPattern().toDouble()[0].floatValue());
                float unitsOff = (float) converterDpi(pathObject.getDashPattern().toDouble()[1].floatValue());
                float phase = (float) converterDpi(pathObject.getDashOffset().floatValue());
                contentStream.setLineDashPattern(new float[]{unitsOn, unitsOff}, phase);
            }
            contentStream.setLineJoinStyle(pathObject.getJoin().ordinal());
            contentStream.setLineCapStyle(pathObject.getCap().ordinal());
            contentStream.setMiterLimit(pathObject.getMiterLimit().floatValue());
            path(contentStream, box, sealBox, pathObject);
            contentStream.setLineWidth((float) converterDpi(lineWidth));
            contentStream.stroke();
            contentStream.restoreGraphicsState();
        }
        if (pathObject.getFill()) {
            contentStream.saveGraphicsState();
            FillColor fillColor = (FillColor) pathObject.getFillColor();
            if (fillColor != null) {
                contentStream.setNonStrokingColor(convertPDColor(fillColor.getValue()));
            } else {
                contentStream.setNonStrokingColor(defaultFillColor);
            }
            path(contentStream, box, sealBox, pathObject);
            contentStream.fill();
            contentStream.restoreGraphicsState();
        }
    }

    private void path(PDPageContentStream contentStream, ST_Box box, ST_Box sealBox, PathObject pathObject) throws IOException {
        if (sealBox != null) {
            pathObject.setBoundary(pathObject.getBoundary().getTopLeftX() + sealBox.getTopLeftX(),
                    pathObject.getBoundary().getTopLeftY() + sealBox.getTopLeftY(),
                    pathObject.getBoundary().getWidth(),
                    pathObject.getBoundary().getHeight());
        }
        List<PathPoint> listPoint = PointUtil.calPdfPathPoint(box.getWidth(), box.getHeight(), pathObject.getBoundary(), PointUtil.convertPathAbbreviatedDatatoPoint(pathObject.getAbbreviatedData()), pathObject.getCTM() != null, pathObject.getCTM(), true);
        for (int i = 0; i < listPoint.size(); i++) {
            if (listPoint.get(i).type.equals("M") || listPoint.get(i).type.equals("S")) {
                contentStream.moveTo(listPoint.get(i).x1, listPoint.get(i).y1);
            } else if (listPoint.get(i).type.equals("L")) {
                contentStream.lineTo(listPoint.get(i).x1, listPoint.get(i).y1);
            } else if (listPoint.get(i).type.equals("B")) {
                contentStream.curveTo(listPoint.get(i).x1, listPoint.get(i).y1,
                        listPoint.get(i).x2, listPoint.get(i).y2,
                        listPoint.get(i).x3, listPoint.get(i).y3);
            } else if (listPoint.get(i).type.equals("C")) {
                contentStream.closePath();
            }
        }
    }

    private void writeImage(PDPageContentStream contentStream, ST_Box box, ImageObject imageObject) throws IOException {
        if (imageMap.get(imageObject.getResourceID().toString()) == null) {
            return;
        }
        contentStream.saveGraphicsState();
        PDImageXObject pdfImageObject = PDImageXObject.createFromByteArray(pdf, imageMap.get(imageObject.getResourceID().toString()), "");
        org.apache.pdfbox.util.Matrix matrix = CommonUtil.toPFMatrix(CommonUtil.getImageMatrixFromOfd(imageObject, box));
        contentStream.drawImage(pdfImageObject, matrix);
        contentStream.restoreGraphicsState();
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
            contentStream.addRect( (float) converterDpi(x)+(float) converterDpi(clipBox.getTopLeftX()), (float) converterDpi(y) + (float) (converterDpi(height) - (converterDpi(clipBox.getTopLeftY()) + converterDpi(clipBox.getHeight()))), (float) converterDpi(clipBox.getWidth()), (float) converterDpi(clipBox.getHeight()));
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
            double sx = a > 0 ? Math.signum(a) * Math.sqrt(a * a + c * c) : Math.sqrt(a * a + c * c);            double sy = Math.signum(d) * Math.sqrt(b * b + d * d);
            fontSize = (float) (fontSize * sx);

        }
        PDFont font = this.pdfFontMap.get(textObject.getFont().toString() + fontAno);
        if (Objects.isNull(font)) font = PdfBoxFontHolder.getInstance(pdf).getFont("楷体");

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
            contentStream.showText(textCodePoint.getText());
            contentStream.endText();
            contentStream.restoreGraphicsState();
        }

    }

}
