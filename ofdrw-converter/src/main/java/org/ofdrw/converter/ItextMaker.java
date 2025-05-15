package org.ofdrw.converter;

import com.itextpdf.io.font.otf.Glyph;
import com.itextpdf.io.font.otf.GlyphLine;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.PatternColor;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants;
import com.itextpdf.kernel.pdf.colorspace.PdfDeviceCs;
import com.itextpdf.kernel.pdf.colorspace.PdfPattern;
import com.itextpdf.kernel.pdf.colorspace.PdfShading;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.kernel.pdf.filespec.PdfFileSpec;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import org.dom4j.Element;
import org.ofdrw.converter.font.FontWrapper;
import org.ofdrw.converter.point.PathPoint;
import org.ofdrw.converter.point.TextCodePoint;
import org.ofdrw.converter.utils.CommonUtil;
import org.ofdrw.converter.utils.PointUtil;
import org.ofdrw.converter.utils.StringUtils;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.*;
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
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;
import org.ofdrw.reader.ResourceLocator;
import org.ofdrw.reader.ResourceManage;
import org.ofdrw.reader.model.AnnotionEntity;
import org.ofdrw.reader.model.StampAnnotEntity;
import org.ofdrw.reader.tools.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.ofdrw.converter.utils.CommonUtil.converterDpi;
import static org.ofdrw.core.text.text.Direction.*;

/**
 * pdf生成器
 *
 * @author dltech21
 * @since 2020.08.11
 */
public class ItextMaker {

    private Map<String, FontWrapper<PdfFont>> fontCache = new HashMap<>();

    private final OFDReader ofdReader;
    /**
     * 资源加载器
     * <p>
     * 用于获取OFD内资源
     */
    private final ResourceManage resMgt;

    public ItextMaker(OFDReader ofdReader) throws IOException {
        this.ofdReader = ofdReader;
        this.resMgt = ofdReader.getResMgt();
    }

    /**
     * ofd每页的object画到pdf
     *
     * @param pdf      PDF文档对象
     * @param pageInfo 页面信息
     * @return PDF页面
     * @throws IOException 文档操作过程中发生异常
     */
    public PdfPage makePage(PdfDocument pdf, PageInfo pageInfo) throws IOException {
        ST_Box pageBox = pageInfo.getSize();
        double pageWidthPixel = converterDpi(pageBox.getWidth());
        double pageHeightPixel = converterDpi(pageBox.getHeight());
        PageSize pageSize = new PageSize((float) pageWidthPixel, (float) pageHeightPixel);
        PdfPage pdfPage = pdf.addNewPage(pageSize);

        pdfPage.setMediaBox(
                new Rectangle(
                        (float) converterDpi(pageBox.getTopLeftX()), (float) converterDpi(pageBox.getTopLeftY()),
                        (float) converterDpi(pageBox.getWidth()), (float) converterDpi(pageBox.getHeight())
                )
        );

        final List<AnnotionEntity> annotationEntities = ofdReader.getAnnotationEntities();
        final List<StampAnnotEntity> stampAnnots = ofdReader.getStampAnnots();
        PdfCanvas pdfCanvas = new PdfCanvas(pdfPage);
        // 获取页面内容出现的所有图层，包含模板页（所有页面均按照定义ZOrder排列）
        List<CT_Layer> layerList = pageInfo.getAllLayer();
        // 绘制 模板层 和 页面内容层
        writeLayer(resMgt, pdfCanvas, layerList, pageBox, null);
        // 绘制电子印章
        writeStamp(pdf, pdfCanvas, pageInfo, stampAnnots);
        // 绘制注释
        writeAnnoAppearance(resMgt, pdfCanvas, pageInfo, annotationEntities, pageBox);
        return pdfPage;
    }

    /**
     * 添加附件
     *
     * @param pdf       PDF文档对象
     * @param ofdReader OFD解析器
     * @throws IOException IO异常
     */
    public void addAttachments(PdfDocument pdf, OFDReader ofdReader) throws IOException {
        // 获取OFD中所有附件
        List<CT_Attachment> attachmentList = ofdReader.getAttachmentList();
        for (CT_Attachment attachment : attachmentList) {
            Path attFile = ofdReader.getAttachmentFile(attachment);
            byte[] fileBytes = Files.readAllBytes(attFile);
            String fileName = attFile.getFileName().toString();
            final String attachmentName = attachment.getAttachmentName();
            String displayFileName = StringUtils.isBlank(attachmentName) ? fileName :
                    attachmentName.concat(fileName.contains(".") ?
                            fileName.substring(fileName.lastIndexOf(".")) : "");
            PdfFileSpec fs = PdfFileSpec.createEmbeddedFileSpec(pdf, fileBytes, null, displayFileName,
                    null);
            pdf.addFileAttachment(displayFileName, fs);
        }
    }

    /**
     * 绘制印章
     *
     * @param pdf                  PDF内容流
     * @param pdfCanvas            绘制上下文
     * @param parent               OFD页面信息
     * @param stampAnnotEntityList 印章列表
     * @throws IOException 文件读写异常
     */
    private void writeStamp(PdfDocument pdf, PdfCanvas pdfCanvas,
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
                            writeLayer(sealResMgt, pdfCanvas, layerList, pageBox, sealBox);
                            // 绘制注释
                            writeAnnoAppearance(sealResMgt, pdfCanvas,
                                    ofdPageVo,
                                    sealOfdReader.getAnnotationEntities(),
                                    pageBox);
                        }
                    }
                } else {
                    // 绘制图片印章内容
                    writeSealImage(pdf, pdfCanvas, pageBox, stampAnnotVo.getImageByte(), sealBox, clipBox);
                }
            }
        }
    }

    /**
     * 绘制 图层
     *
     * @param resMgt    资源管理器
     * @param pdfCanvas Canvas上下文
     * @param layerList 图层
     * @param box       页面区域
     * @param sealBox   印章区域
     * @throws IOException 绘制异常
     */
    private void writeLayer(ResourceManage resMgt,
                            PdfCanvas pdfCanvas,
                            List<CT_Layer> layerList,
                            ST_Box box,
                            ST_Box sealBox) throws IOException {
        for (CT_Layer layer : layerList) {
            writePageBlock(resMgt,
                    pdfCanvas,
                    box,
                    sealBox,
                    layer.getPageBlocks(),
                    layer.getDrawParam(),
                    null,
                    null,
                    null,
                    null);
        }
    }

    private void writeAnnoAppearance(ResourceManage resMgt,
                                     PdfCanvas pdfCanvas,
                                     PageInfo pageInfo,
                                     List<AnnotionEntity> annotionEntities,
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

	            // 超链接类型，注释不绘制
	            if (annot.getType() == AnnotType.Link) {
		            continue;
	            }

                Appearance appearance = annot.getAppearance();
                if (appearance == null) {
                    continue;
                }
                List<PageBlockType> pageBlockTypeList = appearance.getPageBlocks();
                //注释的boundary
                ST_Box annotBox = appearance.getBoundary();
                writePageBlock(resMgt, pdfCanvas, box, null, pageBlockTypeList, null, annotBox, null, null, null);
            }
        }
    }

    /**
     * 绘制 页块
     *
     * @param resMgt                  资源管理器
     * @param pdfCanvas               Canvas上下文
     * @param box                     页面区域
     * @param sealBox                 印章区域
     * @param pageBlockTypeList       需要绘制的页块
     * @param drawparam               绘制参数ID
     * @param annotBox                注释区域
     * @param compositeObjectAlpha    透明度
     * @param compositeObjectBoundary 符合对象区域
     * @param compositeObjectCTM      符合对象变换矩阵
     * @throws IOException 绘制异常
     * @throws IOException 文档操作异常
     */
    private void writePageBlock(ResourceManage resMgt,
                                PdfCanvas pdfCanvas,
                                ST_Box box, ST_Box sealBox,
                                List<PageBlockType> pageBlockTypeList,
                                ST_RefID drawparam,
                                ST_Box annotBox,
                                Integer compositeObjectAlpha,
                                ST_Box compositeObjectBoundary,
                                ST_Array compositeObjectCTM) throws IOException {
        Color defaultStrokeColor = ColorConstants.BLACK;
        Color defaultFillColor = ColorConstants.BLACK;
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
                defaultStrokeColor = ColorConvert.pdfRGB(resMgt, ctDrawParam.getStrokeColor());
            }
            if (ctDrawParam.getFillColor() != null) {
                defaultFillColor = ColorConvert.pdfRGB(resMgt, ctDrawParam.getFillColor());
            }
        }

        for (PageBlockType block : pageBlockTypeList) {
            if (block instanceof TextObject) {
                // text
                Color fillColor = defaultFillColor;
                TextObject textObject = (TextObject) block;
                int alpha = 255;
                final FillColor ctFillColor = textObject.getFillColor();
                if (ctFillColor != null) {
                    if (ctFillColor.getValue() != null) {
                        fillColor = ColorConvert.pdfRGB(resMgt, ctFillColor);
                    } else if (ctFillColor.getColorByType() != null) {
                        // todo
                        CT_AxialShd ctAxialShd = ctFillColor.getColorByType();
                        fillColor = ColorConvert.pdfRGB(resMgt, ctAxialShd.getSegments().get(0).getColor());
                    }
                    alpha = ctFillColor.getAlpha();
                }
                //TODO 修复annot中的文字注解的定位
                writeText(resMgt, pdfCanvas, box, sealBox, annotBox, textObject, fillColor, alpha, compositeObjectAlpha, compositeObjectBoundary, compositeObjectCTM);
            } else if (block instanceof ImageObject) {
                ImageObject imageObject = (ImageObject) block;
                resMgt.superDrawParam(imageObject); // 补充图元参数
                writeImage(resMgt, pdfCanvas, box, imageObject, annotBox, compositeObjectAlpha, compositeObjectBoundary, compositeObjectCTM);
            } else if (block instanceof PathObject) {
                PathObject pathObject = (PathObject) block;
                resMgt.superDrawParam(pathObject); // 补充图元参数
                writePath(resMgt, pdfCanvas, box, sealBox, annotBox, pathObject, defaultFillColor, defaultStrokeColor, defaultLineWidth, compositeObjectAlpha, compositeObjectBoundary, compositeObjectCTM);
            } else if (block instanceof CompositeObject) {
                CompositeObject compositeObject = (CompositeObject) block;
                // 获取引用的矢量资源
                CT_VectorG vectorG = resMgt.getCompositeGraphicUnit(compositeObject.getResourceID().toString());
                Integer currentCompositeObjectAlpha = compositeObject.getAlpha();
                ST_Box currentCompositeObjectBoundary = compositeObject.getBoundary();
                ST_Array currentCompositeObjectCTM = compositeObject.getCTM();
                writePageBlock(resMgt, pdfCanvas, box, sealBox, vectorG.getContent().getPageBlocks(), drawparam, annotBox, currentCompositeObjectAlpha, currentCompositeObjectBoundary, currentCompositeObjectCTM);
            } else if (block instanceof CT_PageBlock) {
                writePageBlock(resMgt, pdfCanvas, box, sealBox, ((CT_PageBlock) block).getPageBlocks(), drawparam, annotBox, compositeObjectAlpha, compositeObjectBoundary, compositeObjectCTM);
            }
        }
    }

	private Color parseAxial(Element eleAxialShd, ResourceManage resMgt, ST_Box box, PathObject pathObject) {
		Color result = null;
		if (eleAxialShd == null) {
			return result;
		}

		CT_AxialShd ctAxialShd = new CT_AxialShd(eleAxialShd);
		Color startColor = ColorConvert.pdfRGB(resMgt, ctAxialShd.getSegments().get(0).getColor());
		Color endColor = ColorConvert.pdfRGB(resMgt,
				ctAxialShd.getSegments().get(ctAxialShd.getSegments().size() - 1).getColor());
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
		PdfShading.Axial axial = new PdfShading.Axial(new PdfDeviceCs.Rgb(), (float) CommonUtil.converterDpi(x1),
				(float) CommonUtil.converterDpi(y1), startColor.getColorValue(), (float) CommonUtil.converterDpi(x2),
				(float) CommonUtil.converterDpi(y2), endColor.getColorValue());
		PdfPattern.Shading shading = new PdfPattern.Shading(axial);
		result = new PatternColor(shading);

		return result;
	}
	
	private Color parseRadial(Element eleRadialShd, ResourceManage resMgt, ST_Box box, PathObject pathObject) {
		Color result = null;
		if (eleRadialShd == null) {
			return result;
		}

		CT_RadialShd ctRadialShd = new CT_RadialShd(eleRadialShd);
		Color startColor = ColorConvert.pdfRGB(resMgt, ctRadialShd.getSegments().get(0).getColor());
		Color endColor = ColorConvert.pdfRGB(resMgt,
				ctRadialShd.getSegments().get(ctRadialShd.getSegments().size() - 1).getColor());
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
		PdfShading.Radial radial = new PdfShading.Radial(new PdfDeviceCs.Rgb(), (float) CommonUtil.converterDpi(x1),
				(float) CommonUtil.converterDpi(y1), (float) CommonUtil.converterDpi(ctRadialShd.getStartRadius()),
				startColor.getColorValue(), (float) CommonUtil.converterDpi(x2), (float) CommonUtil.converterDpi(y2),
				(float) CommonUtil.converterDpi(ctRadialShd.getEndRadius()), endColor.getColorValue());
		PdfPattern.Shading shading = new PdfPattern.Shading(radial);
		result = new PatternColor(shading);

		return result;
	}

    private void writePath(ResourceManage resMgt,
                           PdfCanvas pdfCanvas,
                           ST_Box box,
                           ST_Box sealBox,
                           ST_Box annotBox,
                           PathObject pathObject,
                           Color defaultFillColor,
                           Color defaultStrokeColor,
                           float defaultLineWidth,
                           Integer compositeObjectAlpha,
                           ST_Box compositeObjectBoundary,
                           ST_Array compositeObjectCTM) {
        pdfCanvas.saveState();

        // 处理透明度
        Integer alpha = pathObject.getAlpha();
        if (alpha != null) {
            PdfExtGState gs0 = new PdfExtGState();
            gs0.setFillOpacity(alpha / 255f);
            pdfCanvas.setExtGState(gs0);
        }

        double scale = scaling(sealBox, pathObject);
        CT_DrawParam ctDrawParam = resMgt.superDrawParam(pathObject);
        if (ctDrawParam != null) {
            // 使用绘制参数补充缺省的颜色
            if (pathObject.getStrokeColor() == null
                    && ctDrawParam.getStrokeColor() != null) {
                pathObject.setStrokeColor(new CT_Color().setValue(ctDrawParam.getStrokeColor().getValue()));
            }
            if (pathObject.getFillColor() == null
                    && ctDrawParam.getFillColor() != null) {
                pathObject.setFillColor(new CT_Color().setValue(ctDrawParam.getFillColor().getValue()));
            }
            if (pathObject.getLineWidth() == null
                    && ctDrawParam.getLineWidth() != null) {
                pathObject.setLineWidth(ctDrawParam.getLineWidth());
            }
        }

        if (pathObject.getStrokeColor() != null) {
            StrokeColor strokeColor = pathObject.getStrokeColor();
            if (strokeColor.getValue() != null) {
                pdfCanvas.setStrokeColor(ColorConvert.pdfRGB(resMgt, strokeColor));
            }
            Color axialShdColor = parseAxial(strokeColor.getOFDElement("AxialShd"), resMgt, box, pathObject);
			if (axialShdColor != null) {
				pdfCanvas.setStrokeColor(axialShdColor);
			}

			Color radialShdColor = parseRadial(strokeColor.getOFDElement("RadialShd"), resMgt, box, pathObject);
			if (radialShdColor != null) {
				pdfCanvas.setStrokeColor(radialShdColor);
			}
			
        } else {
            pdfCanvas.setStrokeColor(defaultStrokeColor);
        }

        float lineWidth = defaultLineWidth;
        if (pathObject.getLineWidth() != null && pathObject.getLineWidth() > 0) {
			lineWidth = Double.valueOf(converterDpi(pathObject.getLineWidth()) * scale).floatValue();
		}
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
        pdfCanvas.setLineWidth(lineWidth);
        if (pathObject.getStroke()) {
            if (pathObject.getDashPattern() != null) {
                float unitsOn = (float) converterDpi(pathObject.getDashPattern().toDouble()[0].floatValue());
                float unitsOff = (float) converterDpi(pathObject.getDashPattern().toDouble()[1].floatValue());
                float phase = (float) converterDpi(pathObject.getDashOffset().floatValue());
                pdfCanvas.setLineDash(unitsOn, unitsOff, phase);
            }
            pdfCanvas.setLineJoinStyle(pathObject.getJoin().ordinal());
            pdfCanvas.setLineCapStyle(pathObject.getCap().ordinal());
            pdfCanvas.setMiterLimit(pathObject.getMiterLimit().floatValue());
            path(pdfCanvas, box, sealBox, annotBox, pathObject, compositeObjectBoundary, compositeObjectCTM);

            pdfCanvas.stroke();
            pdfCanvas.restoreState();
        }
        if (pathObject.getFill()) {
            pdfCanvas.saveState();
            if (compositeObjectAlpha != null) {
                PdfExtGState gs1 = new PdfExtGState();
                gs1.setFillOpacity(compositeObjectAlpha * 1.0f / 255f);
                pdfCanvas.setExtGState(gs1);
            }
            FillColor fillColor = (FillColor) pathObject.getFillColor();
            if (fillColor != null) {
                if (fillColor.getValue() != null) {
                    pdfCanvas.setFillColor(ColorConvert.pdfRGB(resMgt, fillColor));
                }
                Color axialShdColor = parseAxial(fillColor.getOFDElement("AxialShd"), resMgt, box, pathObject);
				if (axialShdColor != null) {
					pdfCanvas.setFillColor(axialShdColor);
				}

				Color radialShdColor = parseRadial(fillColor.getOFDElement("RadialShd"), resMgt, box, pathObject);
				if (radialShdColor != null) {
					pdfCanvas.setFillColor(radialShdColor);
				}
                Element patternElement = fillColor.getOFDElement("Pattern");
                if (patternElement != null) {
                    Color patternColor = parsePattern(patternElement, resMgt, box, pathObject, pdfCanvas, annotBox);
                    if (patternColor != null) {
                        pdfCanvas.setFillColor(patternColor);
                    }
                }

				
            } else {
//                pdfCanvas.setFillColor(defaultFillColor);
                // 未设置颜色时，以透明色填充（规范中有明确说明）
                PdfExtGState gs2 = new PdfExtGState();
                gs2.setFillOpacity(0);
                pdfCanvas.setExtGState(gs2);
            }
            path(pdfCanvas, box, sealBox, annotBox, pathObject, compositeObjectBoundary, compositeObjectCTM);
			if (null != pathObject.getRule() && pathObject.getRule().equals(Rule.Even_Odd)) {
				pdfCanvas.eoFill();
			} else {
				pdfCanvas.fill();
			}
            pdfCanvas.restoreState();
        }
    }
    private Color parsePattern(Element elePattern, ResourceManage resMgt, ST_Box box, PathObject pathObject,
                           PdfCanvas pdfCanvas, ST_Box annotBox) {
        // 返回一个默认值防止渲染出来的画面变成黑色
        // TODO 待完善支持Pattern渲染
        double width = Double.parseDouble(elePattern.attributeValue("Width"));
        double height = Double.parseDouble(elePattern.attributeValue("Height"));
        double xStep = Double.parseDouble(elePattern.attributeValue("XStep"));
        double yStep = Double.parseDouble(elePattern.attributeValue("YStep"));
        String relativeTo = elePattern.attributeValue("RelativeTo", "Object");
        String reflectMethod = elePattern.attributeValue("ReflectMethod", "Normal");

        PdfPattern.Tiling tilingPattern = new PdfPattern.Tiling(
            (float) converterDpi(width),
            (float) converterDpi(height),
            (float) converterDpi(xStep),
            (float) converterDpi(yStep),
            true // 使用彩色Pattern
            );
        return new PatternColor(tilingPattern);

    }

    private void path(PdfCanvas pdfCanvas, ST_Box box, ST_Box sealBox, ST_Box annotBox, PathObject pathObject, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM) {
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
            pathObject.setBoundary(
                    pathObject.getBoundary().getTopLeftX() + annotBox.getTopLeftX(),
                    pathObject.getBoundary().getTopLeftY() +annotBox.getTopLeftY(),
                    pathObject.getBoundary().getWidth(),
                    pathObject.getBoundary().getHeight());
        }

        // 裁剪
        clip(pdfCanvas, box, pathObject);
        
        List<PathPoint> listPoint = PointUtil.calPdfPathPoint(box.getWidth(), box.getHeight(), pathObject.getBoundary(), PointUtil.convertPathAbbreviatedDatatoPoint(pathObject.getAbbreviatedData()), pathObject.getCTM() != null, pathObject.getCTM(), compositeObjectBoundary, compositeObjectCTM, true, scale);
        for (int i = 0; i < listPoint.size(); i++) {
            if (listPoint.get(i).type.equals("M") || listPoint.get(i).type.equals("S")) {
                pdfCanvas.moveTo(listPoint.get(i).x1, listPoint.get(i).y1);
            } else if (listPoint.get(i).type.equals("L")) {
                pdfCanvas.lineTo(listPoint.get(i).x1, listPoint.get(i).y1);
            } else if (listPoint.get(i).type.equals("B")) {
                pdfCanvas.curveTo(listPoint.get(i).x1, listPoint.get(i).y1,
                        listPoint.get(i).x2, listPoint.get(i).y2,
                        listPoint.get(i).x3, listPoint.get(i).y3);
            } else if (listPoint.get(i).type.equals("Q")) {
                pdfCanvas.curveTo(listPoint.get(i).x1, listPoint.get(i).y1,
                        listPoint.get(i).x2, listPoint.get(i).y2);
            } else if (listPoint.get(i).type.equals("C")) {
                pdfCanvas.closePath();
            }
        }
    }

    private void clip(PdfCanvas pdfCanvas, ST_Box box, PathObject pathObject) {
		if (pathObject.getClips() == null) {
			return;
		}

		List<CT_Clip> clips = pathObject.getClips().getClips();
		for (int k = 0; k < clips.size(); k++) {
			CT_Clip clip = clips.get(k);
			for (Area area : clip.getAreas()) {
				Element elePath = area.getOFDElement("Path");
				CT_Path path = new CT_Path(elePath);
				List<PathPoint> points = PointUtil.calPdfPathPoint(box.getWidth(), box.getHeight(),
						pathObject.getBoundary(),
						PointUtil.convertPathAbbreviatedDatatoPoint(path.getAbbreviatedData()), area.getCTM() != null,
						area.getCTM(), null, null, true, 1.0);
				pdfCanvas.clip();
				for (int i = 0; i < points.size(); i++) {
					PathPoint pathPoint = points.get(i);
					if (pathPoint.type.equals("M") || pathPoint.type.equals("S")) {
						pdfCanvas.moveTo(pathPoint.x1, pathPoint.y1);
					} else if (pathPoint.type.equals("L")) {
						pdfCanvas.lineTo(pathPoint.x1, pathPoint.y1);
					} else if (pathPoint.type.equals("B")) {
						pdfCanvas.curveTo(pathPoint.x1, pathPoint.y1, pathPoint.x2, pathPoint.y2, pathPoint.x3,
								pathPoint.y3);
					} else if (pathPoint.type.equals("Q")) {
						pdfCanvas.curveTo(pathPoint.x1, pathPoint.y1, pathPoint.x2, pathPoint.y2);
					} else if (pathPoint.type.equals("C")) {
						pdfCanvas.closePath();
					}
				}
				pdfCanvas.endPath();
			}
		}

	}

    /**
     * 计算当前盒子到目标盒子的缩放比例
     * 
     * @param targetBox 目标区域大小
     * @param currentBox 图元大小
     * @return 缩放比例
     */
    private double scaling(ST_Box targetBox, ST_Box currentBox) {
        double scale = 1.0;
        if (targetBox != null && currentBox != null) {
            scale = Math.min(targetBox.getWidth() / currentBox.getWidth(), targetBox.getHeight() / currentBox.getHeight());
        }
        return scale;
    }

    /**
     * 计算图元到目标盒子的缩放比例
     * 
     * @param targetBox 目标区域大小
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

    private void writeImage(ResourceManage resMgt, PdfCanvas pdfCanvas, ST_Box box, ImageObject imageObject, ST_Box annotBox, Integer compositeObjectAlpha, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM) throws IOException {
        final ST_RefID resourceID = imageObject.getResourceID();
        if (resourceID == null) {
            return;
        }
        byte[] imageByteArray = null;
        try {
            imageByteArray = resMgt.getImageByteArray(resourceID.toString());
        } catch (Exception e) {
            System.err.println("图片ID: " + resourceID + " 解析失败 跳过，原因 " + e.getMessage());
        }
        if (imageByteArray == null) {
            return;
        }
        pdfCanvas.saveState();

        ImageData image = ImageDataFactory.create(imageByteArray);
        if (annotBox != null && !isSameBox(annotBox, imageObject.getBoundary())) {
            float x = annotBox.getTopLeftX().floatValue();
            float y = box.getHeight().floatValue() - (annotBox.getTopLeftY().floatValue() + annotBox.getHeight().floatValue());
            float width = annotBox.getWidth().floatValue();
            float height = annotBox.getHeight().floatValue();
            pdfCanvas.addImageWithTransformationMatrix(image, (float) converterDpi(width), 0, 0,
                (float) converterDpi(height), (float) converterDpi(x), (float) converterDpi(y));

        } else {
            org.apache.pdfbox.util.Matrix matrix = CommonUtil.toPFMatrix(CommonUtil.getImageMatrixFromOfd(imageObject, box, compositeObjectCTM));
            float a = matrix.getValue(0, 0);
            float b = matrix.getValue(0, 1);
            float c = matrix.getValue(1, 0);
            float d = matrix.getValue(1, 1);
            float e = matrix.getValue(2, 0);
            float f = matrix.getValue(2, 1);
            pdfCanvas.addImageWithTransformationMatrix(image, a, b, c, d, e, f);
        }
        pdfCanvas.restoreState();
    }

    private void writeSealImage(PdfDocument pdfDocument, PdfCanvas pdfCanvas, ST_Box box, byte[] image, ST_Box sealBox, ST_Box clipBox) throws IOException {
        if (image == null) {
            return;
        }
        float x = sealBox.getTopLeftX().floatValue();
        float y = box.getHeight().floatValue() - (sealBox.getTopLeftY().floatValue() + sealBox.getHeight().floatValue());
        float width = sealBox.getWidth().floatValue();
        float height = sealBox.getHeight().floatValue();
        Rectangle rect = new Rectangle((float) converterDpi(x), (float) converterDpi(y), (float) converterDpi(width), (float) converterDpi(height));
        // 将背景设置为透明，抠图阈值为 244（实践得到最佳）
        BufferedImage bImg = ImageUtils.clearWhiteBackground(ImageIO.read(new ByteArrayInputStream(image)), 244);
        ImageData img = ImageDataFactory.create(bImg, null);

        PdfFormXObject xObject = new PdfFormXObject(new Rectangle(rect.getWidth(), rect.getHeight()));
        PdfCanvas xObjectCanvas = new PdfCanvas(xObject, pdfDocument);
        if (clipBox != null) {
            xObjectCanvas.rectangle(converterDpi(clipBox.getTopLeftX()), rect.getHeight() - (converterDpi(clipBox.getTopLeftY()) + converterDpi(clipBox.getHeight())), converterDpi(clipBox.getWidth()), converterDpi(clipBox.getHeight()));
            xObjectCanvas.clip();
            xObjectCanvas.endPath();
        }
        xObjectCanvas.addImageWithTransformationMatrix(img, rect.getWidth(), 0, 0, rect.getHeight(), 0, 0);
        com.itextpdf.layout.element.Image clipped = new com.itextpdf.layout.element.Image(xObject);
        Canvas canvas = new Canvas(pdfCanvas, rect);
        canvas.add(clipped);
        canvas.close();
    }

    /**
     * 获取字号 ，若无法获取则设置为默认值 0.353。
     *
     * @param textObject 文字对象
     * @return 字号。
     */
    private double getTextObjectSize(TextObject textObject) {
        double fontSize = 0.353;
        if (textObject == null) {
            return fontSize;
        }
        try {
            fontSize = textObject.getSize();
        } catch (Exception e) {
            fontSize = 0.353;
        }
        return fontSize;
    }

    private void writeText(ResourceManage resMgt, PdfCanvas pdfCanvas, ST_Box box, ST_Box sealBox, ST_Box annotBox, TextObject textObject, Color fillColor, int alpha, Integer compositeObjectAlpha, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM) throws IOException {
        double scale = scaling(sealBox, textObject);
    	float fontSize = Double.valueOf(textObject.getSize() * scale).floatValue();
        CT_DrawParam ctDrawParam = resMgt.superDrawParam(textObject);
        // 使用绘制参数补充缺省的颜色
        if (ctDrawParam != null && textObject.getFillColor() == null
                && ctDrawParam.getFillColor() != null) {
            fillColor = ColorConvert.pdfRGB(resMgt, ctDrawParam.getFillColor());
        }
        pdfCanvas.setFillColor(fillColor);
        if (textObject.getFillColor() != null) {
            Element e = textObject.getFillColor().getOFDElement("AxialShd");
            if (e != null) {
                CT_AxialShd ctAxialShd = new CT_AxialShd(e);

                final CT_Color startColor = ctAxialShd.getSegments().get(0).getColor();
                final CT_Color endColor = ctAxialShd.getSegments().get(ctAxialShd.getSegments().size() - 1).getColor();

                ST_Array start = startColor.getValue();
                ST_Array end = endColor.getValue();
                ST_Pos startPos = ctAxialShd.getStartPoint();
                ST_Pos endPos = ctAxialShd.getEndPoint();
                double x1 = startPos.getX(), y1 = startPos.getY();
                double x2 = endPos.getX(), y2 = endPos.getY();
                if (textObject.getCTM() != null) {
                    double[] newPoint = PointUtil.ctmCalPoint(startPos.getX(), startPos.getY(), textObject.getCTM().toDouble());
                    x1 = newPoint[0];
                    y1 = newPoint[1];
                    newPoint = PointUtil.ctmCalPoint(x2, y2, textObject.getCTM().toDouble());
                    x2 = newPoint[0];
                    y2 = newPoint[1];
                }
                double[] realPos = PointUtil.adjustPos(box.getWidth(), box.getHeight(), x1, y1, textObject.getBoundary());
                x1 = realPos[0];
                y1 = box.getHeight() - realPos[1];
                realPos = PointUtil.adjustPos(box.getWidth(), box.getHeight(), x2, y2, textObject.getBoundary());
                x2 = realPos[0];
                y2 = box.getHeight() - realPos[1];
                PdfShading.Axial axial = new PdfShading.Axial(new PdfDeviceCs.Rgb(), (float) x1, (float) y1, ColorConvert.pdfRGB(resMgt, startColor).getColorValue(),
                        (float) x2, (float) y2, ColorConvert.pdfRGB(resMgt, endColor).getColorValue());
                PdfPattern.Shading shading = new PdfPattern.Shading(axial);
                pdfCanvas.setFillColorShading(shading);
            }
        }
        PdfExtGState gs1 = new PdfExtGState();
        gs1.setFillOpacity(alpha / 255f);
        pdfCanvas.setExtGState(gs1);

        if (sealBox != null && textObject.getBoundary() != null) {
            textObject.setBoundary(textObject.getBoundary().getTopLeftX() + sealBox.getTopLeftX(),
                    textObject.getBoundary().getTopLeftY() + sealBox.getTopLeftY(),
                    textObject.getBoundary().getWidth(),
                    textObject.getBoundary().getHeight());
        }
        if (annotBox != null && textObject.getBoundary() != null) {
            textObject.setBoundary(textObject.getBoundary().getTopLeftX() + annotBox.getTopLeftX(),
                    textObject.getBoundary().getTopLeftY() + annotBox.getTopLeftY(),
                    textObject.getBoundary().getWidth(),
                    textObject.getBoundary().getHeight());
        }
        // 修正线宽不受ctm影响的问题
        double lineWidth = 0.0;
        if (textObject.getLineWidth() != null) {
            lineWidth = textObject.getLineWidth();
        }
        if (textObject.getCTM() != null) {
            Double[] ctm = textObject.getCTM().toDouble();
            double a = ctm[0];
            double b = ctm[1];
            double c = ctm[2];
            double d = ctm[3];
            double e = ctm[4];
            double f = ctm[5];
            double sx = a > 0 ? Math.signum(a) * Math.sqrt(a * a + c * c) : Math.sqrt(a * a + c * c);
            double angel = Math.atan2(-b, d);
            if (!(angel == 0 && a != 0 && d == 1)) {
                fontSize = (float) (fontSize * sx);
                // 修正线宽不受ctm影响的问题
                lineWidth = lineWidth * sx;
            }
        }
        if (compositeObjectCTM != null) {
            Double[] ctm = compositeObjectCTM.toDouble();
            double a = ctm[0];
            double b = ctm[1];
            double c = ctm[2];
            double d = ctm[3];
            double e = ctm[4];
            double f = ctm[5];
            double sx = a > 0 ? Math.signum(a) * Math.sqrt(a * a + c * c) : Math.sqrt(a * a + c * c);
            double angel = Math.atan2(-b, d);
            if (!(angel == 0 && a != 0 && d == 1)) {
                fontSize = (float) (fontSize * sx);
                // 修正线宽不受ctm影响的问题
                lineWidth = lineWidth * sx;
            }
        }

        // 加载字体
        CT_Font ctFont = resMgt.getFont(textObject.getFont().toString());
        FontWrapper<PdfFont> pdfFontWrapper = getFont(resMgt.getOfdReader().getResourceLocator(), ctFont, textObject.getCGTransforms().isEmpty());
        PdfFont font = pdfFontWrapper.getFont();

        List<TextCodePoint> textCodePointList = PointUtil.calPdfTextCoordinate(box.getWidth(), box.getHeight(), textObject.getBoundary(), fontSize, textObject.getTextCodes(), textObject.getCGTransforms(), compositeObjectBoundary, compositeObjectCTM, textObject.getCTM() != null, textObject.getCTM(), true, scale);
        double rx = 0, ry = 0;
        for (int i = 0; i < textCodePointList.size(); i++) {
            TextCodePoint textCodePoint = textCodePointList.get(i);
            if (i == 0) {
                rx = textCodePoint.x;
                ry = textCodePoint.y;
            }
            pdfCanvas.saveState();

	        // 文字透明度
	        if (textObject.getFill()) {
		        pdfCanvas.setExtGState(new PdfExtGState().setFillOpacity(textObject.getAlpha() / 255f));
	        }

            pdfCanvas.beginText();
            if (textObject.getMiterLimit() > 0)
                pdfCanvas.setMiterLimit(textObject.getMiterLimit().floatValue());

            pdfCanvas.moveText((textCodePoint.getX()), (textCodePoint.getY()));
            if (textObject.getCTM() != null) {
                Double[] ctm = textObject.getCTM().toDouble();
                double a = ctm[0];
                double b = ctm[1];
                double c = ctm[2];
                double d = ctm[3];
                double e = ctm[4];
                double f = ctm[5];
                AffineTransform transform = new AffineTransform();
                double angel = Math.atan2(-b, d);
                transform.rotate(angel, rx, ry);
                pdfCanvas.concatMatrix(transform);
                if (angel == 0 && a != 0 && d == 1) {
                    textObject.setHScale(a);
                }
            }
            if (textObject.getHScale().floatValue() < 1) {
                AffineTransform transform = new AffineTransform();
                transform.setTransform(textObject.getHScale().floatValue(), 0, 0, 1, (1 - textObject.getHScale().floatValue()) * textCodePoint.getX(), 0);
                pdfCanvas.concatMatrix(transform);
            }
            pdfCanvas.setFontAndSize(font, (float) converterDpi(fontSize));

			// 设置线宽
            if (textObject.getLineWidth() != null) {
	            StrokeColor stroke = textObject.getStrokeColor();
	            if (stroke != null && textObject.getStroke()) {
		            Color strokeColor = ColorConstants.BLACK;
		            // 获取描边颜色
		            if (stroke.getValue() != null) {
			            strokeColor = ColorConvert.pdfRGB(resMgt, stroke);
		            } else if (stroke.getColorByType() != null) {
			            CT_AxialShd ctAxialShd = stroke.getColorByType();
			            strokeColor = ColorConvert.pdfRGB(resMgt, ctAxialShd.getSegments().get(0).getColor());
		            }
		            // 设置透明度
		            PdfExtGState gs2 = new PdfExtGState();
		            gs2.setFillOpacity(stroke.getAlpha() / 255f);
		            pdfCanvas.setExtGState(gs2);
		            // 设置描边颜色
		            pdfCanvas.setStrokeColor(strokeColor);
		            pdfCanvas.setLineWidth((float) converterDpi(lineWidth));
		            pdfCanvas.setTextRenderingMode(PdfCanvasConstants.TextRenderingMode.FILL_STROKE);
	            }
            }

            //设置字符方向
            if (textObject.getCharDirection() == Angle_90) {
                pdfCanvas.setTextMatrix(0, -1, 1, 0, (float) textCodePoint.getX(), (float) textCodePoint.getY());
            } else if (textObject.getCharDirection() == Angle_180) {
                pdfCanvas.setTextMatrix(-1, 0, 0, -1, (float) textCodePoint.getX(), (float) textCodePoint.getY());
            } else if (textObject.getCharDirection() == Angle_270) {
                pdfCanvas.setTextMatrix(0, 1, -1, 0, (float) textCodePoint.getX(), (float) textCodePoint.getY());
            }


            if (!StringUtils.isBlank(textCodePoint.getGlyph()) && !pdfFontWrapper.isEnableSimilarFontReplace()) {
                List<Glyph> glyphs = new ArrayList<>();
                String[] glys = textCodePoint.getGlyph().split(" ");
                for (String gly : glys) {
                    Glyph g = font.getFontProgram().getGlyphByCode(Integer.parseInt(gly));
                    if (g != null) {
                        glyphs.add(g);
                    }
                }
                if (glyphs.size() > 0) {
                    pdfCanvas.showText(new GlyphLine(glyphs));
                } else {
                    pdfCanvas.showText(textCodePoint.getText());
                }
            } else {
                pdfCanvas.showText(textCodePoint.getText());
            }

            pdfCanvas.endText();
            pdfCanvas.restoreState();
        }
    }

    /**
     * 加载字体
     *
     * @param rl     资源加载器
     * @param ctFont 字体对象
     * @param isNoGlyphs 是否不存在字符索引
     * @return 字体
     */
    private FontWrapper<PdfFont> getFont(ResourceLocator rl, CT_Font ctFont, boolean isNoGlyphs) {
        String key = String.format("%s_%s_%s", ctFont.getFamilyName(), ctFont.attributeValue("FontName"), ctFont.getFontFile());
        if (fontCache.containsKey(key)) {
            return fontCache.get(key);
        }
        FontWrapper<PdfFont> font = FontLoader.getInstance().loadPDFFontSimilar(rl, ctFont, isNoGlyphs);
        fontCache.put(key, font);
        return font;
    }
}
