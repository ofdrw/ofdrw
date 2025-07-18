package org.ofdrw.converter;

import org.apache.fontbox.ttf.OTFParser;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.dom4j.Element;
import org.ofdrw.converter.point.PathPoint;
import org.ofdrw.converter.point.TextCodePoint;
import org.ofdrw.converter.utils.CommonUtil;
import org.ofdrw.converter.utils.PointUtil;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.*;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.compositeObj.CT_VectorG;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.graph.pathObj.FillColor;
import org.ofdrw.core.graph.pathObj.Rule;
import org.ofdrw.core.graph.pathObj.StrokeColor;
import org.ofdrw.core.pageDescription.CT_GraphicUnit;
import org.ofdrw.core.pageDescription.clips.Area;
import org.ofdrw.core.pageDescription.clips.CT_Clip;
import org.ofdrw.core.pageDescription.color.color.CT_AxialShd;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.color.CT_RadialShd;
import org.ofdrw.core.pageDescription.color.color.ColorClusterType;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;
import org.ofdrw.reader.ResourceLocator;
import org.ofdrw.reader.ResourceManage;
import org.ofdrw.reader.model.AnnotionEntity;
import org.ofdrw.reader.model.StampAnnotEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.ofdrw.converter.utils.CommonUtil.convertPDColor;
import static org.ofdrw.converter.utils.CommonUtil.converterDpi;
import static org.ofdrw.core.text.text.Direction.*;


/**
 * PDFBox实现的PDF转换实现
 */
public class PdfboxMaker {

    private static final Logger logger = LoggerFactory.getLogger(PdfboxMaker.class);

    /**
     * OFD解析器
     */
    private final OFDReader reader;

    /**
     * PDF文档上下文
     */
    private final PDDocument pdf;
    /**
     * 资源加载器
     * <p>
     * 用于获取OFD内资源
     */
    private final ResourceManage resMgt;


    /**
     * 字体缓存防止重复加载字体
     * <p>
     * KEY: 自族名_字体名_字体路径
     */
    private Map<String, PDFont> fontCache = new HashMap<>();


    public PdfboxMaker(OFDReader reader, PDDocument pdf) throws IOException {
        this.reader = reader;
        this.pdf = pdf;
        this.resMgt = reader.getResMgt();
    }

    /**
     * 转换OFD页面为PDF页面
     *
     * @param pageInfo 页面信息
     * @return PDF页面
     * @throws IOException 操作异常
     */
    public PDPage makePage(PageInfo pageInfo) throws IOException {
        ST_Box pageBox = pageInfo.getSize();
        double pageWidthPixel = converterDpi(pageBox.getWidth());
        double pageHeightPixel = converterDpi(pageBox.getHeight());

        PDRectangle pageSize = new PDRectangle((float) pageWidthPixel, (float) pageHeightPixel);
        PDPage pdfPage = new PDPage(pageSize);
        pdf.addPage(pdfPage);
        final List<AnnotionEntity> annotationEntities = reader.getAnnotationEntities();
        final List<StampAnnotEntity> stampAnnots = reader.getStampAnnots();
        try (PDPageContentStream contentStream = new PDPageContentStream(pdf, pdfPage)) {
            // 获取页面内容出现的所有图层，包含模板页（所有页面均按照定义ZOrder排列）
            List<CT_Layer> layerList = pageInfo.getAllLayer();
            // 绘制 模板层 和 页面内容层
            writeLayer(resMgt, contentStream, layerList, pageBox, null);
            // 绘制电子印章
            writeStamp(contentStream, pageInfo, stampAnnots);
            // 绘制注释
            writeAnnoAppearance(this.resMgt, pageInfo, annotationEntities, contentStream, pageBox);
        }
        return pdfPage;
    }

    /**
     * 绘制印章
     *
     * @param contentStream        PDF内容流
     * @param parent               OFD页面信息
     * @param stampAnnotEntityList 印章列表
     * @throws IOException 文件读写异常
     */
    private void writeStamp(PDPageContentStream contentStream,
                            PageInfo parent,
                            List<StampAnnotEntity> stampAnnotEntityList) throws IOException {
        String pageID = parent.getId().toString();
        for (StampAnnotEntity stampAnnotVo : stampAnnotEntityList) {
            List<StampAnnot> stampAnnots = stampAnnotVo.getStampAnnots();
            for (StampAnnot stampAnnot : stampAnnots) {
                if (!stampAnnot.getPageRef().toString().equals(pageID)) {
                    // 不是同一个页面忽略
                    continue;
                }
                ST_Box pageBox = parent.getSize();
                ST_Box sealBox = stampAnnot.getBoundary();
                ST_Box clipBox = stampAnnot.getClip();

                if (stampAnnotVo.getImgType().equalsIgnoreCase("ofd")) {
                    // 尝试读取并解析OFD印章图像
                    try (OFDReader sealOfdReader = new OFDReader(new ByteArrayInputStream(stampAnnotVo.getImageByte()));) {
                        ResourceManage sealResMgt = sealOfdReader.getResMgt();
                        for (PageInfo ofdPageVo : sealOfdReader.getPageList()) {
                            // 获取页面内容出现的所有图层，包含模板页（所有页面均按照定义ZOrder排列）
                            List<CT_Layer> layerList = ofdPageVo.getAllLayer();
                            // 绘制页面内容
                            writeLayer(sealResMgt, contentStream, layerList, pageBox, sealBox);
                            // 绘制注释
                            writeAnnoAppearance(sealResMgt,
                                    ofdPageVo,
                                    sealOfdReader.getAnnotationEntities(),
                                    contentStream, pageBox);
                        }
                    }
                } else {
                    // 绘制图片印章内容
                    writeSealImage(contentStream, pageBox, stampAnnotVo.getImageByte(), sealBox, clipBox);
                }
            }
        }
    }

    private void writeLayer(ResourceManage resMgt,
                            PDPageContentStream contentStream,
                            List<CT_Layer> layerList,
                            ST_Box box,
                            ST_Box sealBox) throws IOException {
        for (CT_Layer layer : layerList) {
            List<PageBlockType> pageBlockTypeList = layer.getPageBlocks();
            writePageBlock(resMgt,
                    contentStream,
                    box,
                    sealBox,
                    pageBlockTypeList,
                    layer.getDrawParam(),
                    null, null, null, null);
        }
    }

    /**
     * 绘制注释到页面
     *
     * @param resMgt           资源管理器
     * @param pageInfo         OFD页面信息
     * @param annotionEntities 注解列表
     * @param contentStream    PDF Content Stream
     * @param box              绘制区域
     * @throws IOException 绘制过程中IO操作异常
     */
    private void writeAnnoAppearance(ResourceManage resMgt,
                                     PageInfo pageInfo,
                                     List<AnnotionEntity> annotionEntities,
                                     PDPageContentStream contentStream,
                                     ST_Box box) throws IOException {
        String pageId = pageInfo.getId().toString();
        for (AnnotionEntity annotionEntity : annotionEntities) {
            List<Annot> annotList = annotionEntity.getAnnots();
            if (annotList == null) {
                continue;
            }
            if (!pageId.equalsIgnoreCase(annotionEntity.getPageId())) {
                continue;
            }
            for (Annot annot : annotList) {
                List<PageBlockType> pageBlockTypeList = annot.getAppearance().getPageBlocks();
                //注释的boundary
                ST_Box annotBox = annot.getAppearance().getBoundary();
                writePageBlock(resMgt, contentStream, box, null, pageBlockTypeList, null, annotBox, null, null, null);
            }
        }
    }

    private void writePageBlock(ResourceManage resMgt,
                                PDPageContentStream contentStream,
                                ST_Box box, ST_Box sealBox,
                                List<PageBlockType> pageBlockTypeList,
                                ST_RefID drawparam,
                                ST_Box annotBox,
                                Integer compositeObjectAlpha,
                                ST_Box compositeObjectBoundary,
                                ST_Array compositeObjectCTM) throws IOException {
        // 初始化绘制属性
        PDColor defaultFillColor = new PDColor(new float[]{0.0f, 0.0f, 0.0f}, PDDeviceRGB.INSTANCE);
        PDColor defaultStrokeColor = new PDColor(new float[]{0.0f, 0.0f, 0.0f}, PDDeviceRGB.INSTANCE);
        float defaultLineWidth = 0.353f;
        // 递归的获取绘制参数
        CT_DrawParam ctDrawParam = null;
        if (drawparam != null) {
            ctDrawParam = resMgt.getDrawParamFinal(drawparam.toString());
        }
        if (ctDrawParam != null) {
            if (ctDrawParam.getLineWidth() != null) {
                defaultLineWidth = ctDrawParam.getLineWidth().floatValue();
            }
            if (ctDrawParam.getStrokeColor() != null) {
                defaultStrokeColor = convertPDColor(ctDrawParam.getStrokeColor().getValue());
            }
            if (ctDrawParam.getFillColor() != null) {
                defaultFillColor = convertPDColor(ctDrawParam.getFillColor().getValue());
            }
        }

        for (PageBlockType block : pageBlockTypeList) {
            if (block instanceof TextObject) {
                // text
                PDColor fillColor = defaultFillColor;
                TextObject textObject = (TextObject) block;
                resMgt.superDrawParam(textObject);
                int alpha = 255;
                if (textObject.getFillColor() != null) {
                    if (textObject.getFillColor().getValue() != null) {
                        fillColor = convertPDColor(textObject.getFillColor().getValue());
                    } else if (textObject.getFillColor().getColorByType() != null) {
                        // todo
                        CT_AxialShd ctAxialShd = textObject.getFillColor().getColorByType();
                        fillColor = convertPDColor(ctAxialShd.getSegments().get(0).getColor().getValue());
                    }
                    alpha = textObject.getFillColor().getAlpha();
                }
                writeText(resMgt, contentStream, box, sealBox, textObject, fillColor, alpha);
            } else if (block instanceof ImageObject) {
                // image
                ImageObject imageObject = (ImageObject) block;
                resMgt.superDrawParam(imageObject); // 补充图元参数
                writeImage(resMgt, contentStream, box, imageObject, annotBox);
            } else if (block instanceof PathObject) {
                // path
                PathObject pathObject = (PathObject) block;
                resMgt.superDrawParam(pathObject); // 补充图元参数
                writePath(resMgt, contentStream, box, sealBox, annotBox, pathObject, defaultFillColor, defaultStrokeColor, defaultLineWidth, compositeObjectAlpha, compositeObjectBoundary, compositeObjectCTM);
            } else if (block instanceof CompositeObject) {
                CompositeObject compositeObject = (CompositeObject) block;
                // 获取引用的矢量资源
                CT_VectorG vectorG = resMgt.getCompositeGraphicUnit(compositeObject.getResourceID().toString());
                Integer currentCompositeObjectAlpha = compositeObject.getAlpha();
                ST_Box currentCompositeObjectBoundary = compositeObject.getBoundary();
                ST_Array currentCompositeObjectCTM = compositeObject.getCTM();
                writePageBlock(resMgt, contentStream, box, sealBox, vectorG.getContent().getPageBlocks(), drawparam, annotBox, currentCompositeObjectAlpha, currentCompositeObjectBoundary, currentCompositeObjectCTM);
            } else if (block instanceof CT_PageBlock) {
                writePageBlock(resMgt, contentStream, box, sealBox, ((CT_PageBlock) block).getPageBlocks(), drawparam, annotBox, compositeObjectAlpha, compositeObjectBoundary, compositeObjectCTM);
            }
        }
    }

    private PDShading parseAxial(Element eleAxialShd, ResourceManage resMgt, ST_Box box, PathObject pathObject) {
        PDShading result = null;
        if (eleAxialShd == null) {
            return result;
        }

        CT_AxialShd ctAxialShd = new CT_AxialShd(eleAxialShd);
        PDColor startColor = convertPDColor(ctAxialShd.getSegments().get(0).getColor().getValue());
        PDColor endColor = convertPDColor(
                ctAxialShd.getSegments().get(ctAxialShd.getSegments().size() - 1).getColor().getValue());
        ST_Pos startPos = ctAxialShd.getStartPoint();
        ST_Pos endPos = ctAxialShd.getEndPoint();
        double x1 = startPos.getX(), y1 = startPos.getY();
        double x2 = endPos.getX(), y2 = endPos.getY();

        double[] realPos = PointUtil.adjustPos(box.getWidth(), box.getHeight(), x1, y1, pathObject.getBoundary());
        x1 = realPos[0];
        y1 = box.getHeight() - realPos[1];
        realPos = PointUtil.adjustPos(box.getWidth(), box.getHeight(), x2, y2, pathObject.getBoundary());
        x2 = realPos[0];
        y2 = box.getHeight() - realPos[1];

        COSDictionary fdict = new COSDictionary();
        fdict.setInt(COSName.FUNCTION_TYPE, 2);
        COSArray domain = new COSArray();
        domain.add(COSInteger.ZERO);
        domain.add(COSInteger.ONE);
        fdict.setItem(COSName.DOMAIN, domain);
        fdict.setItem(COSName.C0, startColor.toCOSArray());
        fdict.setItem(COSName.C1, endColor.toCOSArray());
        fdict.setInt(COSName.N, 1);
        PDFunctionType2 func = new PDFunctionType2(fdict);

        PDShadingType2 axialShading = new PDShadingType2(new COSDictionary());
        axialShading.setColorSpace(PDDeviceRGB.INSTANCE);
        axialShading.setShadingType(PDShading.SHADING_TYPE2);
        COSArray coords1 = new COSArray();
        coords1.add(new COSFloat((float) CommonUtil.converterDpi(x1)));
        coords1.add(new COSFloat((float) CommonUtil.converterDpi(y1)));
        coords1.add(new COSFloat((float) CommonUtil.converterDpi(x2)));
        coords1.add(new COSFloat((float) CommonUtil.converterDpi(y2)));
        axialShading.setCoords(coords1);
        axialShading.setFunction(func);

        result = axialShading;

        return result;
    }

    private PDShading parseRadial(Element eleRadialShd, ResourceManage resMgt, ST_Box box, PathObject pathObject) {
        PDShading result = null;
        if (eleRadialShd == null) {
            return result;
        }

        CT_RadialShd ctRadialShd = new CT_RadialShd(eleRadialShd);
        PDColor startColor = convertPDColor(ctRadialShd.getSegments().get(0).getColor().getValue());
        PDColor endColor = convertPDColor(
                ctRadialShd.getSegments().get(ctRadialShd.getSegments().size() - 1).getColor().getValue());
        ST_Pos startPos = ctRadialShd.getStartPoint();
        ST_Pos endPos = ctRadialShd.getEndPoint();
        double x1 = startPos.getX(), y1 = startPos.getY();
        double x2 = endPos.getX(), y2 = endPos.getY();
        double[] realPos = PointUtil.adjustPos(box.getWidth(), box.getHeight(), x1, y1, pathObject.getBoundary());
        x1 = realPos[0];
        y1 = box.getHeight() - realPos[1];
        realPos = PointUtil.adjustPos(box.getWidth(), box.getHeight(), x2, y2, pathObject.getBoundary());
        x2 = realPos[0];
        y2 = box.getHeight() - realPos[1];

        COSDictionary fdict = new COSDictionary();
        fdict.setInt(COSName.FUNCTION_TYPE, 2);
        COSArray domain = new COSArray();
        domain.add(COSInteger.ZERO);
        domain.add(COSInteger.ONE);
        fdict.setItem(COSName.DOMAIN, domain);
        fdict.setItem(COSName.C0, startColor.toCOSArray());
        fdict.setItem(COSName.C1, endColor.toCOSArray());
        fdict.setInt(COSName.N, 1);
        PDFunctionType2 func = new PDFunctionType2(fdict);

        PDShadingType3 radialShading = new PDShadingType3(new COSDictionary());
        radialShading.setColorSpace(PDDeviceRGB.INSTANCE);
        radialShading.setShadingType(PDShading.SHADING_TYPE3);
        COSArray coords1 = new COSArray();
        coords1.add(new COSFloat((float) CommonUtil.converterDpi(x1)));
        coords1.add(new COSFloat((float) CommonUtil.converterDpi(y1)));
        coords1.add(new COSFloat((float) CommonUtil.converterDpi(ctRadialShd.getStartRadius())));
        coords1.add(new COSFloat((float) CommonUtil.converterDpi(x2)));
        coords1.add(new COSFloat((float) CommonUtil.converterDpi(y2)));
        coords1.add(new COSFloat((float) CommonUtil.converterDpi(ctRadialShd.getEndRadius())));
        radialShading.setCoords(coords1);
        radialShading.setFunction(func);

        result = radialShading;

        return result;
    }

    private PDShading parseShading(CT_Color color, ST_Box box, PathObject pathObject) {
        PDShading shading = null;
        if (color == null) {
            return shading;
        }
        PDShading axialShading = parseAxial(color.getOFDElement("AxialShd"), resMgt, box, pathObject);
        if (axialShading != null) {
            shading = axialShading;
        }

        PDShading radialShading = parseRadial(color.getOFDElement("RadialShd"), resMgt, box, pathObject);
        if (radialShading != null) {
            shading = radialShading;
        }
        return shading;
    }

    private void writePath(ResourceManage resMgt,
                           PDPageContentStream contentStream,
                           ST_Box box,
                           ST_Box sealBox,
                           ST_Box annotBox,
                           PathObject pathObject,
                           PDColor defaultFillColor,
                           PDColor defaultStrokeColor,
                           float defaultLineWidth,
                           Integer compositeObjectAlpha,
                           ST_Box compositeObjectBoundary,
                           ST_Array compositeObjectCTM) throws IOException {
        contentStream.saveGraphicsState();
        double scale = scaling(sealBox, pathObject);
        // 获取引用的绘制参数可能会null
        CT_DrawParam ctDrawParam = resMgt.superDrawParam(pathObject);
        if (ctDrawParam != null) {
            // 使用绘制参数补充缺省的颜色
            if (pathObject.getStrokeColor() == null
                    && ctDrawParam.getStrokeColor() != null) {
                pathObject.setStrokeColor(ctDrawParam.getStrokeColor());
            }
            if (pathObject.getFillColor() == null
                    && ctDrawParam.getFillColor() != null) {
                pathObject.setFillColor(ctDrawParam.getFillColor());
            }
            if (pathObject.getLineWidth() == null && ctDrawParam.getLineWidth() != null) {
                pathObject.setLineWidth(ctDrawParam.getLineWidth());
            }
        }

        // 设置描边颜色
        final StrokeColor strokeColor = pathObject.getStrokeColor();
        if (strokeColor != null) {
            if (strokeColor.getValue() != null) {
                contentStream.setStrokingColor(convertPDColor(strokeColor.getValue()));
            } else {
                setShadingFill(contentStream, strokeColor, false);
            }
        } else {
            contentStream.setStrokingColor(defaultStrokeColor);
        }

        float lineWidth = defaultLineWidth;
        if (pathObject.getLineWidth() != null && pathObject.getLineWidth() > 0) {
            lineWidth = Double.valueOf(converterDpi(pathObject.getLineWidth()) * scale).floatValue();
        }
        contentStream.setLineWidth(lineWidth);
        if (pathObject.getCTM() != null && pathObject.getLineWidth() != null) {
            Double[] ctm = pathObject.getCTM().toDouble();
            double a = ctm[0];
            double b = ctm[1];
            double c = ctm[2];
            double d = ctm[3];
            double e = ctm[4];
            double f = ctm[5];
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
            if (pathObject.getLineWidth() != null && pathObject.getLineWidth() > 0) {
                contentStream.setLineWidth((float) converterDpi(pathObject.getLineWidth()));
            }
            PDShading shading = parseShading(strokeColor, box, pathObject);
            if (shading != null) {
                contentStream.clip();
                contentStream.shadingFill(shading);
            }
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
            PDShading shading = parseShading(fillColor, box, pathObject);
            if (shading != null) {
                contentStream.clip();
                contentStream.shadingFill(shading);
            }

            if (pathObject.getRule() != null && pathObject.getRule().equals(Rule.Even_Odd)) {
                contentStream.fillEvenOdd();
            } else {
                contentStream.fill();
            }
            contentStream.restoreGraphicsState();
        }
    }

    private void setShadingFill(PDPageContentStream contentStream, CT_Color ctColor, boolean isFill) throws IOException {
        ColorClusterType color = ctColor.getColor();
        if (!(color instanceof CT_AxialShd)) {
            return;
        }
        CT_AxialShd ctAxialShd = (CT_AxialShd) color;

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
        double scale = scaling(sealBox, pathObject);
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

        clip(contentStream, box, pathObject);

        List<PathPoint> listPoint = PointUtil.calPdfPathPoint(box.getWidth(), box.getHeight(), pathObject.getBoundary(), PointUtil.convertPathAbbreviatedDatatoPoint(pathObject.getAbbreviatedData()), pathObject.getCTM() != null, pathObject.getCTM(), compositeObjectBoundary, compositeObjectCTM, true, scale);
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

    private void clip(PDPageContentStream contentStream, ST_Box box, PathObject pathObject) throws IOException {
        if (pathObject.getClips() == null) {
            return;
        }

        List<CT_Clip> clips = pathObject.getClips().getClips();
        for (int k = 0; k < clips.size(); k++) {
            CT_Clip clip = clips.get(k);
            contentStream.clip();
            for (Area area : clip.getAreas()) {
                Element elePath = area.getOFDElement("Path");
                CT_Path path = new CT_Path(elePath);
                List<PathPoint> points = PointUtil.calPdfPathPoint(box.getWidth(), box.getHeight(),
                        pathObject.getBoundary(),
                        PointUtil.convertPathAbbreviatedDatatoPoint(path.getAbbreviatedData()), area.getCTM() != null,
                        area.getCTM(), null, null, true, 1.0);
                for (int i = 0; i < points.size(); i++) {
                    PathPoint pathPoint = points.get(i);
                    if (pathPoint.type.equals("M") || pathPoint.type.equals("S")) {
                        contentStream.moveTo(pathPoint.x1, pathPoint.y1);
                    } else if (pathPoint.type.equals("L")) {
                        contentStream.lineTo(pathPoint.x1, pathPoint.y1);
                    } else if (pathPoint.type.equals("B")) {
                        contentStream.curveTo(pathPoint.x1, pathPoint.y1, pathPoint.x2, pathPoint.y2, pathPoint.x3,
                                pathPoint.y3);
                    } else if (pathPoint.type.equals("Q")) {
                        contentStream.curveTo2(pathPoint.x1, pathPoint.y1, pathPoint.x2, pathPoint.y2);
                    } else if (pathPoint.type.equals("C")) {
                        contentStream.closePath();
                    }
                }
            }
            contentStream.clip();
        }
    }

    /**
     * 计算当前盒子到目标盒子的缩放比例
     *
     * @param targetBox  目标区域大小
     * @param currentBox 当前区域大小
     * @return 缩放比例
     */
    private double scaling(ST_Box targetBox, ST_Box currentBox) {
        double scale = 1.0;
        if (targetBox != null && currentBox != null) {
            scale = Math.min(targetBox.getWidth() / currentBox.getWidth(),
                    targetBox.getHeight() / currentBox.getHeight());
        }
        return scale;
    }

    /**
     * 计算图元到目标盒子的缩放比例
     *
     * @param targetBox   目标盒子
     * @param graphicUnit 图元
     * @return 缩放比例
     */
    private double scaling(ST_Box targetBox, @SuppressWarnings("rawtypes") CT_GraphicUnit graphicUnit) {
        double scale = 1D;
        PageBlockType instance = PageBlockType.getInstance(graphicUnit.getParent());
        if (Objects.nonNull(instance) && Objects.equals(CT_PageBlock.class, instance.getClass())) {
            scale = scaling(targetBox, graphicUnit.getBoundary());
        }
        return scale;
    }

    /**
     * 判断两个box的位置和大小是否相同
     *
     * @param box1 A
     * @param box2 B
     * @return true: 相同；false: 不同
     */
    private boolean isSameBox(ST_Box box1, ST_Box box2) {
        if (null == box1 || null == box2) {
            return false;
        }
        return box1.getTopLeftX().equals(box2.getTopLeftX()) && box1.getTopLeftY().equals(box2.getTopLeftY())
                && box1.getWidth().equals(box2.getWidth()) && box1.getHeight().equals(box2.getHeight());
    }

    private void writeImage(ResourceManage resMgt, PDPageContentStream contentStream, ST_Box box, ImageObject imageObject, ST_Box annotBox) throws IOException {
        // 读取图片
        final ST_RefID resourceID = imageObject.getResourceID();
        if (resourceID == null) {
            return;
        }
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = resMgt.getImage(resourceID.toString());
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(String.format("图片解析失败！[resourceId: %s][%s]", resourceID.toString(), e.getMessage()));
            } else {
                logger.warn(String.format("图片解析失败！[resourceId: %s]", resourceID.toString()), e);
            }
        }
        if (bufferedImage == null) {
            return;
        }
        contentStream.saveGraphicsState();
        // 根据图片格式决定图片使用哪种创建方式
        PDImageXObject pdfImageObject;
        CT_MultiMedia multiMedia = resMgt.getMultiMedia(resourceID.toString());
        if (multiMedia != null && "JPEG".equals(multiMedia.getFormat())) {
            pdfImageObject = JPEGFactory.createFromImage(pdf, bufferedImage);
        } else {
            pdfImageObject = LosslessFactory.createFromImage(pdf, bufferedImage);
        }

        if (annotBox != null && !isSameBox(annotBox, imageObject.getBoundary())) {
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

    private void writeSealImage(PDPageContentStream contentStream, ST_Box box, byte[] image, ST_Box sealBox, ST_Box clipBox) throws IOException {
        if (image == null) {
            return;
        }
        contentStream.saveGraphicsState();

        PDImageXObject pdfImageObject = LosslessFactory.createFromImage(pdf, ImageIO.read(new ByteArrayInputStream(image)));
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

    /**
     * 获取字号 ，若无法获取则设置为默认值 0.353。
     *
     * @param textObject 文字对象
     * @return 字号。
     */
    private float getTextObjectSize(TextObject textObject) {
        float fontSize = 0.353f;
        if (textObject == null) {
            return fontSize;
        }
        try {
            fontSize = textObject.getSize().floatValue();
        } catch (Exception e) {
            fontSize = 0.353f;
        }
        return fontSize;
    }

    private void writeText(ResourceManage resMgt, PDPageContentStream contentStream, ST_Box box, ST_Box sealBox, TextObject textObject, PDColor defaultFontColor, int alpha) throws IOException {
        double scale = scaling(sealBox, textObject);
        float fontSize = Double.valueOf(textObject.getSize() * scale).floatValue();
        if (sealBox != null && textObject.getBoundary() != null) {
            textObject.setBoundary(textObject.getBoundary().getTopLeftX() + sealBox.getTopLeftX(),
                    textObject.getBoundary().getTopLeftY() + sealBox.getTopLeftY(),
                    textObject.getBoundary().getWidth(),
                    textObject.getBoundary().getHeight());
        }

        PDColor fillColor = defaultFontColor;
        CT_DrawParam ctDrawParam = resMgt.superDrawParam(textObject);
        if (ctDrawParam != null) {
            // 使用绘制参数补充缺省的颜色
            if (textObject.getFillColor() == null
                    && ctDrawParam.getFillColor() != null) {
                fillColor = convertPDColor(ctDrawParam.getFillColor().getValue());
            }
        }

        // 加载字体
        CT_Font ctFont = resMgt.getFont(textObject.getFont().toString());
        PDFont font = getFont(ctFont);


    }

    private void writeText2(ResourceManage resMgt, PDPageContentStream contentStream, ST_Box box, ST_Box sealBox, TextObject textObject, PDColor defaultFontColor, int alpha) throws IOException {
        double scale = scaling(sealBox, textObject);
        float fontSize = Double.valueOf(textObject.getSize() * scale).floatValue();
        if (sealBox != null && textObject.getBoundary() != null) {
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
            double angel = Math.atan2(-b, d);
            if (!(angel == 0 && a != 0 && d == 1)) {
                fontSize = (float) (fontSize * sx);
            }
        }

        PDColor fillColor = defaultFontColor;
        CT_DrawParam ctDrawParam = resMgt.superDrawParam(textObject);
        if (ctDrawParam != null) {
            // 使用绘制参数补充缺省的颜色
            if (textObject.getFillColor() == null
                    && ctDrawParam.getFillColor() != null) {
                fillColor = convertPDColor(ctDrawParam.getFillColor().getValue());
            }
        }


        // 加载字体
        CT_Font ctFont = resMgt.getFont(textObject.getFont().toString());
        PDFont font = getFont(ctFont);

        List<TextCodePoint> textCodePointList = PointUtil.calPdfTextCoordinate(box.getWidth(), box.getHeight(), textObject.getBoundary(), fontSize, textObject.getTextCodes(), textObject.getCTM() != null, textObject.getCTM(), true, scale);
        double rx = 0, ry = 0;
        for (int i = 0; i < textCodePointList.size(); i++) {
            contentStream.saveGraphicsState();

            // 初等矩阵
            TextCodePoint textCodePoint = textCodePointList.get(i);
            if (i == 0) {
                rx = textCodePoint.x;
                ry = textCodePoint.y;
            }
            if (textObject.getCTM() != null) {
                Double[] ctm = textObject.getCTM().toDouble();
                double a = ctm[0];
                double b = ctm[1];
                double c = ctm[2];
                double d = ctm[3];
                double angel = Math.atan2(-b, d);
                contentStream.transform(Matrix.getRotateInstance(angel, (float) rx, (float) ry));
//                transform = transform.multiply(Matrix.getRotateInstance(angel, (float) rx, (float) ry)) ;
                if (angel == 0 && a != 0 && d == 1) {
                    textObject.setHScale(a);
                }
            }
            if (textObject.getHScale().floatValue() < 1) {
                contentStream.transform(new Matrix(
                        textObject.getHScale().floatValue(), 0,
                        0, 1,
                        (1 - textObject.getHScale().floatValue()) * (float) textCodePoint.getX(), 0
                ));

//                 transform = transform.multiply(new  Matrix(
//                        textObject.getHScale().floatValue(), 0,
//                        0, 1,
//                        (1 - textObject.getHScale().floatValue()) * (float) textCodePoint.getX(), 0
//                ));

            }

            //设置字符方向
            if (textObject.getCharDirection() == Angle_90) {
                contentStream.transform(new Matrix(0, -1, 1, 0, (float) textCodePoint.getX(), (float) textCodePoint.getY()));
            } else if (textObject.getCharDirection() == Angle_180) {
                contentStream.transform(new Matrix(-1, 0, 0, -1, (float) textCodePoint.getX(), (float) textCodePoint.getY()));
            } else if (textObject.getCharDirection() == Angle_270) {
                contentStream.transform(new Matrix(0, 1, -1, 0, (float) textCodePoint.getX(), (float) textCodePoint.getY()));
            }

            contentStream.beginText();
            contentStream.setNonStrokingColor(fillColor);
            contentStream.newLineAtOffset((float) (textCodePoint.getX()), (float) (textCodePoint.getY()));
            contentStream.setFont(font, (float) converterDpi(fontSize));
            try {
                contentStream.showText(textCodePoint.getText());
            } catch (Exception e) {
                logger.debug("无法显示文字", e);
            }
            contentStream.endText();
            contentStream.restoreGraphicsState();
        }

    }

    /**
     * 添加附件
     *
     * @param ofdReader OFD解析器
     * @throws IOException IO异常
     */
    public void addAttachments(OFDReader ofdReader) throws IOException {
        // 获取OFD中所有附件
        List<CT_Attachment> attachmentList = ofdReader.getAttachmentList();
        if (attachmentList == null || attachmentList.isEmpty()) {
            return;
        }
        PDEmbeddedFilesNameTreeNode efTree = new PDEmbeddedFilesNameTreeNode();
        Map<String, PDComplexFileSpecification> efMap = new HashMap<>();
        for (CT_Attachment attachment : attachmentList) {
            PDComplexFileSpecification fs = new PDComplexFileSpecification();
            Path attFile = ofdReader.getAttachmentFile(attachment);
            // 文件名传
            fs.setFile(attachment.getAttachmentName());
            fs.setFileUnicode(attachment.getAttachmentName());
            // 文件流，该流将由PDEmbeddedFile内部关闭
            PDEmbeddedFile ef = new PDEmbeddedFile(pdf, Files.newInputStream(attFile));
            // 文件类型
            ef.setSubtype(attachment.getFormat());
            ef.setSize((int) Files.size(attFile));

            try {
                Calendar calendar = Calendar.getInstance();
                // 设置创建时间
                LocalDateTime creationDate = attachment.getCreationDateTime();
                Date date = Date.from(creationDate.atZone(ZoneId.systemDefault()).toInstant());
                calendar.setTime(date);
                ef.setCreationDate(calendar);
            } catch (Exception e) {
                logger.info("无法获取附件创建时间 {} : {}", attachment.attributeValue("CreationDate"), attachment.getAttachmentName(), e);
            }

            fs.setEmbeddedFile(ef);
            efMap.put(attachment.getAttachmentName(), fs);
        }
        efTree.setNames(efMap);
        PDDocumentNameDictionary names = new PDDocumentNameDictionary(pdf.getDocumentCatalog());
        names.setEmbeddedFiles(efTree);
        pdf.getDocumentCatalog().setNames(names);
    }

    /**
     * 加载字体
     *
     * @param ctFont OFD字体对象
     * @return PDFBox字体对象
     * @throws IOException 加载失败
     */
    private PDFont loadFont(CT_Font ctFont) throws IOException {
        // 字体是否嵌入
        boolean embedSubset = false;

        // 获取嵌入式字体路径
        Path fontPath = null;
        if (ctFont != null && ctFont.getFontFile() != null) {
            // 内嵌字体绝对路径
            try {
                ResourceLocator resourceLocator = reader.getResourceLocator();
                fontPath = resourceLocator.getFile(ctFont.getFontFile()).toAbsolutePath();
            } catch (Exception e) {
                logger.warn(
                        "无法加载内嵌字体: " + ctFont.getFamilyName() + " " + ctFont.getFontName() + " " + ctFont.getFontFile(), e);
            }
        }
        if (fontPath != null) {
            embedSubset = true;
        } else {
            // 获取系统字体
            String systemFontPath = FontLoader.getInstance().getReplaceSimilarFontPath(ctFont.getFamilyName(),
                    ctFont.getFontName());
            if (systemFontPath != null) {
                fontPath = Paths.get(systemFontPath);
            }
        }
        if (fontPath == null) {
            // 获取默认字体
            fontPath = FontLoader.getInstance().getDefaultFontPath();
        }

        String name = fontPath.toFile().getName().toLowerCase();
        TrueTypeFont ttf = null;
        if (name.endsWith(".ttf")) {
            RandomAccessReadBuffer fontStream = new RandomAccessReadBuffer(Files.readAllBytes(fontPath));
            try {
                ttf = new TTFParser(embedSubset).parse(fontStream);
            } catch (Exception ex) {
                ttf = new TTFParser(!embedSubset).parse(fontStream);
            }
        } else if (name.endsWith(".otf")) {
            RandomAccessReadBuffer fontStream = new RandomAccessReadBuffer(Files.readAllBytes(fontPath));
            try {
                ttf = new OTFParser(embedSubset).parse(fontStream);
            } catch (Exception ex) {
                ttf = new OTFParser(!embedSubset).parse(fontStream);
            }

        } else if (name.endsWith(".ttc")) {
            TrueTypeCollection ttc = new TrueTypeCollection(fontPath.toFile());
            if (ttc != null) {
                ttf = ttc.getFontByName(ctFont.getFontName());
                if (ttf == null) {
                    String alias = FontLoader.getInstance().getFontAlias(ctFont);
                    ttf = ttc.getFontByName(alias);
                }
                embedSubset = true;
                ttc.close();
            }
        }
        if (ttf == null) {
            throw new IOException("无法加载字体 " + ctFont.getFamilyName() + " " + ctFont.getFontName() + " " + ctFont.getFontFile());
        }
        return PDType0Font.load(pdf, ttf, embedSubset);
    }

    /**
     * 加载字体
     *
     * @param ctFont 字体对象
     * @return 字体
     */
    private PDFont getFont(CT_Font ctFont) {
        String key = String.format("%s_%s_%s", ctFont.getFamilyName(), ctFont.getFontName(), ctFont.getFontFile());
        if (fontCache.containsKey(key)) {
            return fontCache.get(key);
        }
        try {
            // 加载字体
            PDFont font = loadFont(ctFont);
            fontCache.put(key, font);
            return font;
        } catch (Exception e) {
            if (ctFont != null && ctFont.getFontFile() != null) {
                logger.info("无法使用字体: {} {} {}", ctFont.getFamilyName(), ctFont.getFontName(), ctFont.getFontFile());
            }
            return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
    }

    /**
     * 转换OFD变换矩阵为PDF变换矩阵
     *
     * 1. PDF的坐标系原点是在页面的左下角，从左至右为X轴的正方向，从下至上为Y轴的正方向
     * 2. OFD的坐标系原点是在页面的左上角，从左至右为X轴的正方向，从上至下为Y轴的正方向
     * 3. OFD坐标采用毫米，PDF坐标采用英寸，已知转换关系为 (mm*dpi /25.4)，可以获取OFD的页面宽度和高度。
     * 4. OFD变换矩阵与OFD的变换矩阵格式相同 [a,b,c,d,e,f]
     *
     * @param ctm OFD变换矩阵
     * @param pageBox OFD页面大小
     * @param dpi 缩放因子
     * @return PDF变换矩阵
     */
    public static Matrix matrix2(ST_Array ctm,ST_Box pageBox,  double dpi) {
        double pageWidthMM = pageBox.getWidth().floatValue();
        double pageHeightMM = pageBox.getHeight().floatValue();

        Double[] ofdMatrix = ctm.toDouble();
        // 计算单位转换因子：毫米 -> 点
        double scale = dpi / 25.4;
        double flipYTranslate = pageHeightMM * scale; // 翻转平移量（点单位）
        // 创建翻转+缩放矩阵 [scale, 0, 0, -scale, 0, flipYTranslate]
        Matrix flipScaleMatrix = new Matrix((float) scale, 0, 0, (float) -scale, 0, (float) flipYTranslate);
        // 创建OFD原始矩阵（参数顺序: a, b, c, d, e, f）
        Matrix ofdMatrixObj = new Matrix(
                ofdMatrix[0].floatValue(), ofdMatrix[1].floatValue(),
                ofdMatrix[2].floatValue(), ofdMatrix[3].floatValue(),
                ofdMatrix[4].floatValue(), ofdMatrix[5].floatValue()
        );
        // 计算最终PDF矩阵：M_ofd × M_flip_scale
        return ofdMatrixObj.multiply(flipScaleMatrix);
    }
}
