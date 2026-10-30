package org.ofdrw.converter;

import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.util.Matrix;
import org.dom4j.Element;
import org.ofdrw.converter.font.ItextTrueTypeFont;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.ofdrw.converter.utils.CommonUtil.convertPDColor;
import static org.ofdrw.converter.utils.CommonUtil.converterDpi;
import static org.ofdrw.core.text.text.Direction.Angle_180;
import static org.ofdrw.core.text.text.Direction.Angle_270;
import static org.ofdrw.core.text.text.Direction.Angle_90;

/**
 * pdf生成器
 * <p>
 * 该实现基于 PDFBox 生成 PDF，对外接口与历史 iText 实现保持一致：
 * 页面坐标系原点位于左下角，页面内坐标由 OFD 坐标逐点换算得到，
 * 不做整体坐标变换，以保证各 PDF 渲染器输出一致。
 *
 * @author dltech21
 * @since 2020.08.11
 * @deprecated 类名保留历史命名（iText），实现已迁移为 PDFBox；新代码请使用
 * {@link org.ofdrw.converter.export.PDFExporterPDFBox} 或 {@link PdfboxMaker}。
 */
@Deprecated
public class ItextMaker {

    private static final Logger logger = LoggerFactory.getLogger(ItextMaker.class);

    /**
     * 文字裁剪区域在文字图元水平与垂直方向上的扩展量，单位：毫米
     * <p>
     * 与 AWT 渲染保持一致，避免倾斜或描边字形超出紧边界时被裁切。
     */
    private static final double TEXT_CLIP_MARGIN = 1d;

    /**
     * 内容流指令换行符
     */
    private static final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * 默认字体，当无法加载 OFD 字体时使用
     */
    private PDFont defaultFont = PDType1Font.HELVETICA;

    /**
     * 字体缓存，防止重复加载字体
     * <p>
     * KEY: 资源加载器标识_字族名_字体名_字体文件路径
     */
    private final Map<String, FontHolder> fontCache = new HashMap<>();

    /**
     * 字体池所属的 PDF 文档对象
     */
    private PDDocument fontPoolDocument;

    /**
     * 字体池
     * <p>
     * PDFBox 不允许同一字体实例被写入不同文档，因此同一文档内按字体内容哈希共用字体对象。
     * KEY: 字体文件 SHA-256；VALUE: 字体对象
     */
    private final Map<String, PDType0Font> fontPool = new HashMap<>();

    /**
     * 字体文件数据缓存
     * <p>
     * KEY: 字体文件绝对路径；VALUE: 字体文件数据
     */
    private final Map<String, byte[]> fontBytesCache = new HashMap<>();

    private final OFDReader ofdReader;

    /**
     * 资源加载器
     * <p>
     * 用于获取OFD内资源
     */
    private final ResourceManage resMgt;

    /**
     * 创建 PDF 转换器
     *
     * @param ofdReader OFD解析器
     * @throws IOException 文档读取异常
     * @deprecated 请使用 {@link PdfboxMaker#PdfboxMaker(OFDReader, PDDocument)}
     */
    @Deprecated
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
     * @deprecated 请使用 {@link PdfboxMaker#makePage(PageInfo)}
     */
    @Deprecated
    public PDPage makePage(PDDocument pdf, PageInfo pageInfo) throws IOException {
        ST_Box pageBox = pageInfo.getSize();
        // 页面坐标原点位于 OFD 页面左上角，Y 轴向上
        ST_Box drawBox = new ST_Box(0, 0, pageBox.getWidth(), pageBox.getHeight());
        PDPage pdfPage = new PDPage(new PDRectangle((float) converterDpi(pageBox.getWidth()),
                (float) converterDpi(pageBox.getHeight())));
        pdf.addPage(pdfPage);

        final List<AnnotionEntity> annotationEntities = ofdReader.getAnnotationEntities();
        final List<StampAnnotEntity> stampAnnots = ofdReader.getStampAnnots();
        try (PDPageContentStream pdfCanvas = new PDPageContentStream(pdf, pdfPage)) {
            PageContext ctx = new PageContext(pdfCanvas, pdfPage.getCOSObject()
                    .getCOSDictionary(COSName.RESOURCES));
            // 获取页面内容出现的所有图层，包含模板页（所有页面均按照定义ZOrder排列）
            List<CT_Layer> layerList = pageInfo.getAllLayer();
            // 绘制 模板层 和 页面内容层
            writeLayer(resMgt, pdf, ctx, layerList, drawBox, null);
            // 绘制电子印章
            writeStamp(pdf, ctx, pageInfo, stampAnnots);
            // 绘制注释
            writeAnnoAppearance(resMgt, pdf, ctx, pageInfo, annotationEntities, drawBox);
        }
        return pdfPage;
    }

    /**
     * 添加附件
     *
     * @param pdf       PDF文档对象
     * @param ofdReader OFD解析器
     * @throws IOException IO异常
     * @deprecated 请使用 {@link PdfboxMaker#addAttachments(OFDReader)}
     */
    @Deprecated
    public void addAttachments(PDDocument pdf, OFDReader ofdReader) throws IOException {
        // 获取OFD中所有附件
        List<CT_Attachment> attachmentList = ofdReader.getAttachmentList();
        if (attachmentList == null || attachmentList.isEmpty()) {
            return;
        }
        PDDocumentNameDictionary names = new PDDocumentNameDictionary(pdf.getDocumentCatalog());
        PDEmbeddedFilesNameTreeNode efTree = names.getEmbeddedFiles();
        if (efTree == null) {
            efTree = new PDEmbeddedFilesNameTreeNode();
            names.setEmbeddedFiles(efTree);
            pdf.getDocumentCatalog().setNames(names);
        }
        Map<String, PDComplexFileSpecification> efMap = efTree.getNames();
        efMap = efMap == null ? new HashMap<>() : new HashMap<>(efMap);

        for (CT_Attachment attachment : attachmentList) {
            Path attFile = ofdReader.getAttachmentFile(attachment);
            byte[] fileBytes = Files.readAllBytes(attFile);
            String fileName = attFile.getFileName().toString();
            final String attachmentName = attachment.getAttachmentName();
            String displayFileName = StringUtils.isBlank(attachmentName) ? fileName :
                    attachmentName.concat(fileName.contains(".") ?
                            fileName.substring(fileName.lastIndexOf(".")) : "");

            // 附件内容流，流由 PDEmbeddedFile 负责关闭
            PDEmbeddedFile ef = new PDEmbeddedFile(pdf, new ByteArrayInputStream(fileBytes));
            ef.setSubtype(attachment.getFormat());
            ef.setSize(fileBytes.length);
            try {
                Date date = Date.from(attachment.getCreationDateTime()
                        .atZone(ZoneId.systemDefault()).toInstant());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                ef.setModDate(calendar);
            } catch (Exception e) {
                logger.debug("无法获取附件创建时间 {} : {}", attachment.attributeValue("CreationDate"),
                        attachment.getAttachmentName());
            }

            PDComplexFileSpecification fs = new PDComplexFileSpecification();
            fs.setFile(displayFileName);
            fs.setFileUnicode(displayFileName);
            fs.setEmbeddedFile(ef);
            efMap.put(displayFileName, fs);
        }
        efTree.setNames(efMap);
    }

    /**
     * 绘制印章
     *
     * @param pdf                  PDF文档对象
     * @param ctx                  页面绘制状态
     * @param parent               OFD页面信息
     * @param stampAnnotEntityList 印章列表
     * @throws IOException 文件读写异常
     */
    private void writeStamp(PDDocument pdf, PageContext ctx,
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
                            writeLayer(sealResMgt, pdf, ctx, layerList, pageBox, sealBox);
                            // 绘制注释
                            writeAnnoAppearance(sealResMgt, pdf, ctx,
                                    ofdPageVo,
                                    sealOfdReader.getAnnotationEntities(),
                                    pageBox);
                        }
                    }
                } else {
                    // 绘制图片印章内容
                    writeSealImage(pdf, ctx, pageBox, stampAnnotVo.getImageByte(), sealBox, clipBox);
                }
            }
        }
    }

    /**
     * 绘制 图层
     *
     * @param resMgt    资源管理器
     * @param pdf       PDF文档对象
     * @param ctx 页面绘制状态
     * @param layerList 图层
     * @param box       页面区域
     * @param sealBox   印章区域
     * @throws IOException 绘制异常
     */
    private void writeLayer(ResourceManage resMgt,
                            PDDocument pdf,
                            PageContext ctx,
                            List<CT_Layer> layerList,
                            ST_Box box,
                            ST_Box sealBox) throws IOException {
        for (CT_Layer layer : layerList) {
            writePageBlock(resMgt,
                    pdf,
                    ctx,
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
                                     PDDocument pdf,
                                     PageContext ctx,
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
                writePageBlock(resMgt, pdf, ctx, box, null, pageBlockTypeList, null, annotBox, null, null, null);
            }
        }
    }

    /**
     * 绘制 页块
     *
     * @param resMgt                  资源管理器
     * @param pdf                     PDF文档对象
     * @param ctx                     页面绘制状态
     * @param box                     页面区域
     * @param sealBox                 印章区域
     * @param pageBlockTypeList       需要绘制的页块
     * @param drawparam               绘制参数ID
     * @param annotBox                注释区域
     * @param compositeObjectAlpha    透明度
     * @param compositeObjectBoundary 符合对象区域
     * @param compositeObjectCTM      符合对象变换矩阵
     * @throws IOException 绘制异常
     */
    private void writePageBlock(ResourceManage resMgt,
                                PDDocument pdf,
                                PageContext ctx,
                                ST_Box box, ST_Box sealBox,
                                List<PageBlockType> pageBlockTypeList,
                                ST_RefID drawparam,
                                ST_Box annotBox,
                                Integer compositeObjectAlpha,
                                ST_Box compositeObjectBoundary,
                                ST_Array compositeObjectCTM) throws IOException {
        PDColor defaultStrokeColor = new PDColor(new float[]{0.0f, 0.0f, 0.0f}, PDDeviceRGB.INSTANCE);
        PDColor defaultFillColor = new PDColor(new float[]{0.0f, 0.0f, 0.0f}, PDDeviceRGB.INSTANCE);
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
                int alpha = 255;
                final FillColor ctFillColor = textObject.getFillColor();
                if (ctFillColor != null) {
                    if (ctFillColor.getValue() != null) {
                        fillColor = convertPDColor(ctFillColor.getValue());
                    } else if (ctFillColor.getColorByType() != null) {
                        // todo
                        CT_AxialShd ctAxialShd = ctFillColor.getColorByType();
                        fillColor = convertPDColor(ctAxialShd.getSegments().get(0).getColor().getValue());
                    }
                    alpha = ctFillColor.getAlpha();
                }
                //TODO 修复annot中的文字注解的定位
                writeText(resMgt, pdf, ctx, box, sealBox, annotBox, textObject, fillColor, alpha, compositeObjectAlpha, compositeObjectBoundary, compositeObjectCTM);
            } else if (block instanceof ImageObject) {
                ImageObject imageObject = (ImageObject) block;
                resMgt.superDrawParam(imageObject); // 补充图元参数
                writeImage(resMgt, pdf, ctx, box, imageObject, annotBox, compositeObjectAlpha, compositeObjectBoundary, compositeObjectCTM);
            } else if (block instanceof PathObject) {
                PathObject pathObject = (PathObject) block;
                resMgt.superDrawParam(pathObject); // 补充图元参数
                writePath(resMgt, ctx, box, sealBox, annotBox, pathObject, defaultFillColor, defaultStrokeColor, defaultLineWidth, compositeObjectAlpha, compositeObjectBoundary, compositeObjectCTM);
            } else if (block instanceof CompositeObject) {
                CompositeObject compositeObject = (CompositeObject) block;
                // 获取引用的矢量资源
                CT_VectorG vectorG = resMgt.getCompositeGraphicUnit(compositeObject.getResourceID().toString());
                Integer currentCompositeObjectAlpha = compositeObject.getAlpha();
                ST_Box currentCompositeObjectBoundary = compositeObject.getBoundary();
                ST_Array currentCompositeObjectCTM = compositeObject.getCTM();
                writePageBlock(resMgt, pdf, ctx, box, sealBox, vectorG.getContent().getPageBlocks(), drawparam, annotBox, currentCompositeObjectAlpha, currentCompositeObjectBoundary, currentCompositeObjectCTM);
            } else if (block instanceof CT_PageBlock) {
                writePageBlock(resMgt, pdf, ctx, box, sealBox, ((CT_PageBlock) block).getPageBlocks(), drawparam, annotBox, compositeObjectAlpha, compositeObjectBoundary, compositeObjectCTM);
            }
        }
    }

    /**
     * 构建两色渐变函数（PDF 类型 2 函数，线性插值）
     *
     * @param startColor 起始颜色
     * @param endColor   结束颜色
     * @return 渐变函数
     */
    private static PDFunctionType2 createGradientFunction(PDColor startColor, PDColor endColor) {
        COSDictionary fdict = new COSDictionary();
        fdict.setInt(COSName.FUNCTION_TYPE, 2);
        COSArray domain = new COSArray();
        domain.add(COSInteger.ZERO);
        domain.add(COSInteger.ONE);
        fdict.setItem(COSName.DOMAIN, domain);
        fdict.setItem(COSName.C0, startColor.toCOSArray());
        fdict.setItem(COSName.C1, endColor.toCOSArray());
        fdict.setInt(COSName.N, 1);
        return new PDFunctionType2(fdict);
    }

    /**
     * 构建线性（轴向）渐变着色
     *
     * @param startColor 起始颜色
     * @param endColor   结束颜色
     * @param x1         起始点 X 坐标，单位：毫米
     * @param y1         起始点 Y 坐标，单位：毫米
     * @param x2         结束点 X 坐标，单位：毫米
     * @param y2         结束点 Y 坐标，单位：毫米
     * @return 渐变着色
     */
    private static PDShadingType2 createAxialShading(PDColor startColor, PDColor endColor,
                                                     double x1, double y1, double x2, double y2) {
        PDShadingType2 axialShading = new PDShadingType2(new COSDictionary());
        axialShading.setColorSpace(PDDeviceRGB.INSTANCE);
        axialShading.setShadingType(PDShading.SHADING_TYPE2);
        COSArray coords = new COSArray();
        coords.add(new COSFloat((float) x1));
        coords.add(new COSFloat((float) y1));
        coords.add(new COSFloat((float) x2));
        coords.add(new COSFloat((float) y2));
        axialShading.setCoords(coords);
        axialShading.setFunction(createGradientFunction(startColor, endColor));
        return axialShading;
    }

    /**
     * 将渐变着色注册为页面资源中的图案
     *
     * @param ctx     页面绘制状态
     * @param shading 渐变着色
     * @return 图案在资源字典中的名称
     */
    private static COSName registerPattern(PageContext ctx, PDShading shading) {
        // 图案字典以其着色对象作为内容（图案类型 2 为渐变图案）
        COSDictionary patternDict = new COSDictionary();
        patternDict.setItem(COSName.PATTERN_TYPE, COSInteger.get(2));
        patternDict.setItem(COSName.SHADING, shading);
        COSName patternName = COSName.getPDFName("P" + ctx.patterns.size());
        ctx.patterns.setItem(patternName, patternDict);
        return patternName;
    }

    /**
     * 构建已注册图案对应的颜色
     * <p>
     * 图案颜色由图案名称与图案色彩空间组成，PDFBox 在该颜色上绘制时写出着色命令（scn）。
     *
     * @param resources   页面资源字典
     * @param patternName 图案在资源字典中的名称
     * @return 图案颜色
     */
    private static PDColor patternColor(COSDictionary resources, COSName patternName) {
        COSArray colorArray = new COSArray();
        colorArray.add(patternName);
        return new PDColor(colorArray, new PDPattern(new PDResources(resources)));
    }

    /**
     * 解析轴向渐变
     *
     * @param eleAxialShd 渐变元素
     * @param box         页面区域，单位：毫米
     * @param pathObject  路径对象
     * @return 渐变着色，无法解析时返回 null
     */
    private PDShading parseAxial(Element eleAxialShd, ST_Box box, PathObject pathObject) {
        if (eleAxialShd == null) {
            return null;
        }
        CT_AxialShd ctAxialShd = new CT_AxialShd(eleAxialShd);
        PDColor startColor = convertPDColor(ctAxialShd.getSegments().get(0).getColor().getValue());
        PDColor endColor = convertPDColor(
                ctAxialShd.getSegments().get(ctAxialShd.getSegments().size() - 1).getColor().getValue());
        if (startColor == null || endColor == null) {
            return null;
        }
        ST_Pos startPos = ctAxialShd.getStartPoint();
        ST_Pos endPos = ctAxialShd.getEndPoint();
        double[] start = convertToPdfPos(box, startPos, pathObject.getBoundary());
        double[] end = convertToPdfPos(box, endPos, pathObject.getBoundary());
        return createAxialShading(startColor, endColor, start[0], start[1], end[0], end[1]);
    }

    /**
     * 解析径向渐变
     *
     * @param eleRadialShd 渐变元素
     * @param box          页面区域，单位：毫米
     * @param pathObject   路径对象
     * @return 渐变着色，无法解析时返回 null
     */
    private PDShading parseRadial(Element eleRadialShd, ST_Box box, PathObject pathObject) {
        if (eleRadialShd == null) {
            return null;
        }
        CT_RadialShd ctRadialShd = new CT_RadialShd(eleRadialShd);
        PDColor startColor = convertPDColor(ctRadialShd.getSegments().get(0).getColor().getValue());
        PDColor endColor = convertPDColor(
                ctRadialShd.getSegments().get(ctRadialShd.getSegments().size() - 1).getColor().getValue());
        if (startColor == null || endColor == null) {
            return null;
        }
        double[] start = convertToPdfPos(box, ctRadialShd.getStartPoint(), pathObject.getBoundary());
        double[] end = convertToPdfPos(box, ctRadialShd.getEndPoint(), pathObject.getBoundary());

        PDShadingType3 radialShading = new PDShadingType3(new COSDictionary());
        radialShading.setColorSpace(PDDeviceRGB.INSTANCE);
        radialShading.setShadingType(PDShading.SHADING_TYPE3);
        COSArray coords = new COSArray();
        coords.add(new COSFloat((float) start[0]));
        coords.add(new COSFloat((float) start[1]));
        coords.add(new COSFloat((float) converterDpi(ctRadialShd.getStartRadius())));
        coords.add(new COSFloat((float) end[0]));
        coords.add(new COSFloat((float) end[1]));
        coords.add(new COSFloat((float) converterDpi(ctRadialShd.getEndRadius())));
        radialShading.setCoords(coords);
        radialShading.setFunction(createGradientFunction(startColor, endColor));
        return radialShading;
    }

    /**
     * 将 OFD 内容坐标系下的图片变换矩阵转换为 PDF 页面坐标系下的矩阵
     * <p>
     * CommonUtil 计算得到的矩阵把图片空间映射到 OFD 内容坐标系（原点位于页面左上角、Y 轴向下），
     * 线性部分与平移量均为毫米；此处在其中补上平移页面高度的一步，并换算为磅。
     *
     * @param box    页面区域，单位：毫米
     * @param matrix CommonUtil 计算得到的图片矩阵，单位：毫米
     * @return PDF 页面坐标系下的图片矩阵，单位：磅
     */
    private static Matrix toPdfMatrix(ST_Box box, org.ujmp.core.Matrix matrix) {
        org.ujmp.core.Matrix result = matrix.mtimes(org.ofdrw.converter.utils.MatrixUtils.create(
                1, 0, 0, 1, 0, box.getHeight()));
        result = result.mtimes(org.ofdrw.converter.utils.MatrixUtils.create(
                converterDpi(1), 0, 0, converterDpi(1), 0, 0));
        return new Matrix((float) result.getAsDouble(0, 0), (float) result.getAsDouble(0, 1),
                (float) result.getAsDouble(1, 0), (float) result.getAsDouble(1, 1),
                (float) result.getAsDouble(2, 0), (float) result.getAsDouble(2, 1));
    }

    /**
     * 将 OFD 坐标的 Y 值转换为 PDF 页面坐标
     * <p>
     * OFD 坐标原点位于页面左上角且 Y 轴向下，PDF 页面坐标原点位于左下角且 Y 轴向上。
     *
     * @param box 页面区域，单位：毫米
     * @param y   OFD 坐标 Y 值，单位：毫米
     * @return PDF 页面坐标 Y 值，单位：磅（1/72 英寸）
     */
    private static float toPdfY(ST_Box box, double y) {
        return (float) converterDpi(box.getHeight() - y);
    }

    /**
     * 将 OFD 坐标转换为页面绘制坐标
     * <p>
     * OFD 页面原点位于左上角且 Y 轴向下，绘制坐标系原点位于页面左上角且 Y 轴向上。
     *
     * @param box      页面区域，单位：毫米
     * @param pos      OFD 坐标点，单位：毫米
     * @param boundary 图元区域，单位：毫米
     * @return 页面绘制坐标 {x, y}，单位：毫米
     */
    private static double[] convertToPdfPos(ST_Box box, ST_Pos pos, ST_Box boundary) {
        double[] realPos = PointUtil.adjustPos(box.getWidth(), box.getHeight(),
                pos.getX(), pos.getY(), boundary);
        return new double[]{realPos[0], box.getHeight() - realPos[1]};
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

    private void writeText(ResourceManage resMgt, PDDocument pdf, PageContext ctx, ST_Box box, ST_Box sealBox, ST_Box annotBox, TextObject textObject, PDColor fillColor, int alpha, Integer compositeObjectAlpha, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM) throws IOException {
        // 页面与模板层中的图元坐标已是页面坐标，仅印章内的图元需要按印章区域缩放
        double scale = scaling(sealBox, textObject.getBoundary());
        float fontSize = Double.valueOf(textObject.getSize() * scale).floatValue();
        CT_DrawParam ctDrawParam = resMgt.superDrawParam(textObject);
        // 使用绘制参数补充缺省的颜色
        if (ctDrawParam != null && textObject.getFillColor() == null
                && ctDrawParam.getFillColor() != null) {
            fillColor = convertPDColor(ctDrawParam.getFillColor().getValue());
        }
        setNonStrokingColor(ctx, null, fillColor);

        // 文字填色为轴向渐变时，以渐变着色填充文字
        PDShading textShading = null;
        if (textObject.getFillColor() != null) {
            Element e = textObject.getFillColor().getOFDElement("AxialShd");
            if (e != null) {
                CT_AxialShd ctAxialShd = new CT_AxialShd(e);
                final CT_Color startColor = ctAxialShd.getSegments().get(0).getColor();
                final CT_Color endColor = ctAxialShd.getSegments().get(ctAxialShd.getSegments().size() - 1).getColor();
                PDColor startPDColor = convertPDColor(startColor.getValue());
                PDColor endPDColor = convertPDColor(endColor.getValue());
                if (startPDColor != null && endPDColor != null) {
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
                    double[] start = convertToPdfPos(box, new ST_Pos(x1, y1), textObject.getBoundary());
                    double[] end = convertToPdfPos(box, new ST_Pos(x2, y2), textObject.getBoundary());
                    textShading = createAxialShading(startPDColor, endPDColor, start[0], start[1], end[0], end[1]);
                } else if (FontLoader.DEBUG) {
                    logger.info("文字渐变颜色无法解析: {}", e);
                }
            }
        }
        PDExtendedGraphicsState gs1 = new PDExtendedGraphicsState();
        gs1.setNonStrokingAlphaConstant(alpha / 255f);
        ctx.pdfCanvas.setGraphicsStateParameters(gs1);

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
        FontHolder fontHolder = getFont(pdf, resMgt.getOfdReader().getResourceLocator(), ctFont, textObject.getCGTransforms().isEmpty());
        PDFont font = fontHolder.font;

        List<TextCodePoint> textCodePointList = PointUtil.calPdfTextCoordinate(box.getWidth(), box.getHeight(), textObject.getBoundary(), fontSize, textObject.getTextCodes(), textObject.getCGTransforms(), compositeObjectBoundary, compositeObjectCTM, textObject.getCTM() != null, textObject.getCTM(), true, scale);

        // 创建矩形对象, 指定文字绘制区域
        ST_Box boundary = textObject.getBoundary();
        PDRectangle rectangle = textClipRectangle(box, boundary);

        double rx = 0, ry = 0;
        for (int i = 0; i < textCodePointList.size(); i++) {
            TextCodePoint textCodePoint = textCodePointList.get(i);
            if (i == 0) {
                rx = textCodePoint.x;
                ry = textCodePoint.y;
            }
            ctx.pdfCanvas.saveGraphicsState();

            // 文字透明度
            if (textObject.getFill()) {
                PDExtendedGraphicsState textGState = new PDExtendedGraphicsState();
                textGState.setNonStrokingAlphaConstant(textObject.getAlpha() / 255f);
                ctx.pdfCanvas.setGraphicsStateParameters(textGState);
            }

            // 剪裁文字实际绘制区域
            ctx.pdfCanvas.addRect(rectangle.getLowerLeftX(), rectangle.getLowerLeftY(),
                    rectangle.getWidth(), rectangle.getHeight());
            ctx.pdfCanvas.clip();    // 通过将当前剪切路径与当前路径相交来修改当前剪切路径

            ctx.pdfCanvas.beginText();
            if (textObject.getMiterLimit() > 0) {
                ctx.pdfCanvas.setMiterLimit(textObject.getMiterLimit().floatValue());
            }

            Matrix textMatrix = new Matrix(1, 0, 0, 1, (float) converterDpi(textCodePoint.getX()), toPdfY(box, textCodePoint.getY()));
            ctx.pdfCanvas.setTextMatrix(textMatrix);
            if (textObject.getCTM() != null) {
                Double[] ctm = textObject.getCTM().toDouble();
                double a = ctm[0];
                double b = ctm[1];
                double c = ctm[2];
                double d = ctm[3];
                double angel = Math.atan2(-b, d);
                Matrix rotate = Matrix.getRotateInstance(angel, (float) converterDpi(rx), toPdfY(box, ry));
                textMatrix = textMatrix.multiply(rotate);
                ctx.pdfCanvas.setTextMatrix(textMatrix);
                if (angel == 0 && a != 0 && d == 1) {
                    textObject.setHScale(a);
                }
            }
            if (textObject.getHScale().floatValue() < 1) {
                Matrix hScale = new Matrix(textObject.getHScale().floatValue(), 0, 0, 1,
                        (float) ((1 - textObject.getHScale().floatValue()) * textCodePoint.getX()), 0);
                textMatrix = textMatrix.multiply(hScale);
                ctx.pdfCanvas.setTextMatrix(textMatrix);
            }
            ctx.pdfCanvas.setFont(font, (float) converterDpi(fontSize));

            // 设置线宽
            if (textObject.getLineWidth() != null) {
                StrokeColor stroke = textObject.getStrokeColor();
                if (stroke != null && textObject.getStroke()) {
                    PDColor strokeColor = new PDColor(new float[]{0.0f, 0.0f, 0.0f}, PDDeviceRGB.INSTANCE);
                    // 获取描边颜色
                    if (stroke.getValue() != null) {
                        strokeColor = convertPDColor(stroke.getValue());
                    } else if (stroke.getColorByType() != null) {
                        CT_AxialShd ctAxialShd = stroke.getColorByType();
                        strokeColor = convertPDColor(ctAxialShd.getSegments().get(0).getColor().getValue());
                    }
                    // 设置透明度
                    PDExtendedGraphicsState gs2 = new PDExtendedGraphicsState();
                    gs2.setNonStrokingAlphaConstant(stroke.getAlpha() / 255f);
                    ctx.pdfCanvas.setGraphicsStateParameters(gs2);
                    // 设置描边颜色
                    ctx.pdfCanvas.setStrokingColor(strokeColor);
                    ctx.pdfCanvas.setLineWidth((float) converterDpi(lineWidth));
                    ctx.pdfCanvas.setRenderingMode(RenderingMode.FILL_STROKE);
                }
            }

            // 设置文字斜体
            if (textObject.getItalic()) {
                // 计算剪切因子（角度=10°）
                double shearFactor = Math.tan(Math.toRadians(10));

                // 定义补偿量：补偿量 = 剪切因子 * 字体高度
                double compensation = shearFactor * toPdfY(box, textCodePoint.getY());

                // 构建组合变换矩阵（反向平移且剪切）
                Matrix shear = new Matrix(1, 0, (float) shearFactor, 1, (float) -compensation, 0);
                textMatrix = textMatrix.multiply(shear);
                ctx.pdfCanvas.setTextMatrix(textMatrix);
            }

            //设置字符方向
            float pdfTextX = (float) converterDpi(textCodePoint.getX());
            float pdfTextY = toPdfY(box, textCodePoint.getY());
            if (textObject.getCharDirection() == Angle_90) {
                // OFD 中 90° 为字符顺时针旋转，PDF 坐标 Y 轴向上，对应逆时针 90°
                ctx.pdfCanvas.setTextMatrix(0, 1, -1, 0, pdfTextX, pdfTextY);
            } else if (textObject.getCharDirection() == Angle_180) {
                ctx.pdfCanvas.setTextMatrix(-1, 0, 0, -1, pdfTextX, pdfTextY);
            } else if (textObject.getCharDirection() == Angle_270) {
                ctx.pdfCanvas.setTextMatrix(0, -1, 1, 0, pdfTextX, pdfTextY);
            }

            // 字形索引与字符文本均可能包含字体无法编码的字符（如制表符、控制符），逐字输出并跳过这些字符
            List<String> candidates = new ArrayList<>(2);
            if (!StringUtils.isBlank(textCodePoint.getGlyph()) && !fontHolder.enableSimilarFontReplace) {
                // 原字体可用时按字形索引输出，保证文字内容与 OFD 中的字形一致
                candidates.add(toGlyphText(textCodePoint));
            }
            if (!StringUtils.isBlank(textCodePoint.getText())) {
                candidates.add(textCodePoint.getText());
            }
            for (String candidate : candidates) {
                if (showText(ctx.pdfCanvas, fontHolder.font, candidate)) {
                    break;
                }
            }

            ctx.pdfCanvas.endText();
            ctx.pdfCanvas.restoreGraphicsState();
        }
    }

    /**
     * 输出文字，自动跳过字体无法编码的字符
     *
     * @param pdfCanvas 绘制上下文
     * @param font      当前使用的字体
     * @param text      待输出文字
     * @return true - 已输出文字；false - 文字中没有字体可以编码的字符
     * @throws IOException 文字输出异常
     */
    private static boolean showText(PDPageContentStream pdfCanvas, PDFont font, String text) throws IOException {
        StringBuilder encodable = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            try {
                font.encode(String.valueOf(c));
                encodable.append(c);
            } catch (Exception e) {
                // 字体无法编码该字符（控制符或字体缺少对应字形），跳过
            }
        }
        if (encodable.length() == 0) {
            return false;
        }
        int[] codePoints = encodable.toString().codePoints().toArray();
        if (codePoints.length != encodable.length()) {
            pdfCanvas.showTextWithPositioning(toPositioningArray(codePoints, font));
        } else {
            pdfCanvas.showText(encodable.toString());
        }
        return true;
    }

    /**
     * 将码点序列转换为带定位的文本元素数组，代理对按完整码点输出
     *
     * @param codePoints 码点序列
     * @param font      当前使用的字体
     * @return 带定位的文本元素数组
     */
    private static Object[] toPositioningArray(int[] codePoints, PDFont font) {
        Object[] elements = new Object[codePoints.length];
        for (int k = 0; k < codePoints.length; k++) {
            elements[k] = new String(codePoints, k, 1);
        }
        return elements;
    }

    /**
     * 设置描边颜色
     *
     * @param ctx         页面绘制状态
     * @param patternName 图案名称，为空表示使用 RGB 颜色
     * @param color       RGB 颜色，图案颜色时可为空
     * @throws IOException 内容流写入异常
     */
    private static void setStrokingColor(PageContext ctx, COSName patternName, PDColor color) throws IOException {
        if (patternName != null) {
            ctx.pdfCanvas.setStrokingColor(patternColor(ctx.resources, patternName));
            ctx.strokingPatternName = patternName;
            ctx.strokingColor = null;
        } else {
            ctx.pdfCanvas.setStrokingColor(color);
            ctx.strokingPatternName = null;
            ctx.strokingColor = color;
        }
    }

    /**
     * 设置填充颜色
     *
     * @param ctx         页面绘制状态
     * @param patternName 图案名称，为空表示使用 RGB 颜色
     * @param color       RGB 颜色，图案颜色时可为空
     * @throws IOException 内容流写入异常
     */
    private static void setNonStrokingColor(PageContext ctx, COSName patternName, PDColor color) throws IOException {
        if (patternName != null) {
            ctx.pdfCanvas.setNonStrokingColor(patternColor(ctx.resources, patternName));
            ctx.nonStrokingPatternName = patternName;
            ctx.nonStrokingColor = null;
        } else {
            ctx.pdfCanvas.setNonStrokingColor(color);
            ctx.nonStrokingPatternName = null;
            ctx.nonStrokingColor = color;
        }
    }

    /**
     * 描边当前路径
     * <p>
     * PDFBox 执行操作符出现异常会中断后续内容流的执行，状态恢复也会随之失效，
     * 因此填充与描边失败时重新设置图形状态并直接写入绘制指令。
     *
     * @param ctx 页面绘制状态
     * @throws IOException 内容流写入异常
     */
    private static void strokePath(PageContext ctx) throws IOException {
        try {
            ctx.pdfCanvas.stroke();
        } catch (Exception e) {
            logger.debug("PDFBox 执行描边失败，已直接写入描边指令: {}", e.getMessage());
            reapplyGraphicsState(ctx);
        }
    }

    /**
     * 填充当前路径
     *
     * @param ctx     页面绘制状态
     * @param evenOdd true - 奇偶规则填充；false - 非零环绕规则填充
     * @throws IOException 内容流写入异常
     */
    private static void fillPath(PageContext ctx, boolean evenOdd) throws IOException {
        try {
            if (evenOdd) {
                ctx.pdfCanvas.fillEvenOdd();
            } else {
                ctx.pdfCanvas.fill();
            }
        } catch (Exception e) {
            logger.debug("PDFBox 执行填充失败，已直接写入填充指令: {}", e.getMessage());
            reapplyGraphicsState(ctx);
        }
    }

    /**
     * 重新设置当前图形状态
     * <p>
     * 用于在填充或描边执行失败后恢复颜色与线宽等参数，后续绘制不再依赖已失效的操作符状态。
     *
     * @param ctx 页面绘制状态
     * @throws IOException 内容流写入异常
     */
    private static void reapplyGraphicsState(PageContext ctx) throws IOException {
        ctx.pdfCanvas.appendRawCommands("/DeviceRGB CS" + LINE_SEPARATOR + "/DeviceRGB cs" + LINE_SEPARATOR);
        if (ctx.strokingColor != null) {
            appendColor(ctx.pdfCanvas, "SC", ctx.strokingPatternName, ctx.strokingColor);
        }
        if (ctx.nonStrokingColor != null) {
            appendColor(ctx.pdfCanvas, "sc", ctx.nonStrokingPatternName, ctx.nonStrokingColor);
        }
        ctx.pdfCanvas.appendRawCommands(formatNumber(ctx.lineWidth) + " w" + LINE_SEPARATOR);
    }

    /**
     * 写入颜色参数
     *
     * @param pdfCanvas  绘制上下文
     * @param operator   RGB 颜色操作符，图案颜色使用对应的着色操作符
     * @param patternName 图案名称，为空表示 RGB 颜色
     * @param color      颜色
     * @throws IOException 内容流写入异常
     */
    private static void appendColor(PDPageContentStream pdfCanvas, String operator, COSName patternName,
                                    PDColor color) throws IOException {
        if (patternName != null) {
            String patternOperator = "SC".equals(operator) ? "SCN" : "scn";
            pdfCanvas.appendRawCommands("/" + patternName.getName() + " " + patternOperator + LINE_SEPARATOR);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (float component : color.getComponents()) {
            sb.append(formatNumber(component)).append(' ');
        }
        pdfCanvas.appendRawCommands(sb.append(operator).append(LINE_SEPARATOR).toString());
    }

    /**
     * 格式化数值，保留 5 位小数并去除多余的 0
     *
     * @param value 数值
     * @return 格式化后的数值
     */
    private static String formatNumber(float value) {
        BigDecimal decimal = BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP);
        return decimal.stripTrailingZeros().toPlainString();
    }

    static PDRectangle textClipRectangle(ST_Box pageBox, ST_Box boundary) {
        return new PDRectangle(
                (float) converterDpi(boundary.getTopLeftX() - TEXT_CLIP_MARGIN),
                (float) converterDpi(pageBox.getHeight() - boundary.getTopLeftY()
                        - boundary.getHeight() - TEXT_CLIP_MARGIN),
                (float) converterDpi(boundary.getWidth() + TEXT_CLIP_MARGIN * 2),
                (float) converterDpi(boundary.getHeight() + TEXT_CLIP_MARGIN * 2));
    }

    /**
     * 将字形索引列表转换为可输出文字
     *
     * @param textCodePoint 文字坐标信息
     * @return 字形索引对应的文字
     */
    private static String toGlyphText(TextCodePoint textCodePoint) {
        List<String> glyphs = new ArrayList<>();
        for (String gly : textCodePoint.getGlyph().split(" ")) {
            try {
                int gid = Integer.parseInt(gly);
                if (gid > 0 && gid <= 0xFFFF) {
                    glyphs.add(String.valueOf((char) gid));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return String.join("", glyphs);
    }

    /**
     * 加载字体
     *
     * @param pdf        PDF文档对象
     * @param rl         资源加载器
     * @param ctFont     字体对象
     * @param isNoGlyphs 是否不存在字符索引
     * @return 字体
     */
    private FontHolder getFont(PDDocument pdf, ResourceLocator rl, CT_Font ctFont, boolean isNoGlyphs) {
        String key = String.format("%s_%s_%s_%s", System.identityHashCode(rl), ctFont.getFamilyName(),
                ctFont.attributeValue("FontName"), ctFont.getFontFile());
        FontHolder holder = fontCache.get(key);
        if (holder != null) {
            return holder;
        }
        holder = loadPDFFontSimilar(pdf, rl, ctFont, isNoGlyphs);
        fontCache.put(key, holder);
        return holder;
    }

    /**
     * 尽可能的加载字体
     * <p>
     * 依次尝试 OFD 内嵌字体、系统同名或相近字体，全部失败时使用默认字体。
     *
     * @param pdf        PDF文档对象
     * @param rl         资源加载器
     * @param ctFont     字体对象
     * @param isNoGlyphs 是否不存在字符索引
     * @return 字体
     */
    private FontHolder loadPDFFontSimilar(PDDocument pdf, ResourceLocator rl, CT_Font ctFont, boolean isNoGlyphs) {
        final String fontName = ctFont.getFontName();
        final String familyName = ctFont.getFamilyName();
        byte[] fontData = null;
        try {
            // 1、尝试加载内嵌字体
            if (ctFont.getFontFile() != null) {
                String fontAbsPath = rl.getFile(ctFont.getFontFile()).toAbsolutePath().toString();
                fontData = readFontBytes(fontAbsPath);
            }
            // 源文件不存在索引标签且字体映射表为空时，需要选择操作系统字体
            boolean isLoadSystemFont = isNoGlyphs && fontData != null && !hasCmap(fontData);
            if (FontLoader.DEBUG) {
                logger.info("是否加载系统字体 {} {} {}", isLoadSystemFont, isNoGlyphs, fontData == null);
            }
            // 2、尝试根据名字从操作系统加载字体，只要替换了内嵌字体，就需要设置为需要替换
            boolean hasReplace = isLoadSystemFont || fontData == null;
            if (hasReplace) {
                String fontAbsPath = FontLoader.getInstance().getSystemFontPath(familyName, fontName);
                if (fontAbsPath == null) {
                    // 操作系统中不存在，那么尝试使用近似的字体替换
                    fontAbsPath = FontLoader.getInstance().getReplaceSimilarFontPath(familyName, fontName);
                }
                fontData = fontAbsPath == null ? null : readFontBytes(fontAbsPath);
            }
            if (FontLoader.DEBUG) {
                logger.info("加载PDF中的字体 status=加载{}, {}, {}, {}", fontData == null ? "失败" : "成功",
                        familyName, fontName, ctFont.getFontFile());
            }
            // 3、前面两种加载机制都失效时，使用默认字体
            if (fontData == null) {
                Path defaultFontPath = FontLoader.getInstance().getDefaultFontPath();
                fontData = defaultFontPath == null ? null : readFontBytes(defaultFontPath.toString());
                hasReplace = true;
            }
            if (fontData == null) {
                return new FontHolder(defaultFont, true);
            }
            return new FontHolder(getPooledFont(pdf, fontData), hasReplace);
        } catch (Exception e) {
            if (FontLoader.DEBUG) {
                logger.warn("加载字体异常 " + familyName + " " + fontName + " " + ctFont.getFontFile(), e);
            }
            return new FontHolder(defaultFont, true);
        }
    }

    /**
     * 读取字体文件数据
     * <p>
     * 字体集合（TTC）无法直接嵌入 PDF，会转换为集合中的第一个字体。
     *
     * @param fontAbsPath 字体文件绝对路径
     * @return 单个字体的文件数据
     * @throws IOException 文件读写异常
     */
    private byte[] readFontBytes(String fontAbsPath) throws IOException {
        byte[] data = fontBytesCache.get(fontAbsPath);
        if (data != null) {
            return data;
        }
        try (InputStream in = Files.newInputStream(Paths.get(fontAbsPath))) {
            data = toSingleFontBytes(org.apache.commons.io.IOUtils.toByteArray(in));
        }
        fontBytesCache.put(fontAbsPath, data);
        return data;
    }

    /**
     * 字体数据转换为单个字体文件数据
     *
     * @param data 字体文件数据
     * @return 非字体集合时为原始数据；字体集合时为集合中第一个字体的数据
     * @throws IOException 字体解析异常
     */
    private static byte[] toSingleFontBytes(byte[] data) throws IOException {
        if (!ItextTrueTypeFont.isTrueTypeCollection(data)) {
            return data;
        }
        if (ItextTrueTypeFont.getTrueTypeCollectionFontCount(data) <= 0) {
            throw new IOException("字体集合中没有可用字体");
        }
        return buildSingleFont(data);
    }

    /**
     * 判断字体数据是否包含 cmap 表
     *
     * @param data 字体文件数据
     * @return true - 包含可用的 cmap；false - 不包含
     */
    private static boolean hasCmap(byte[] data) {
        if (ItextTrueTypeFont.isTrueTypeCollection(data)) {
            // 字体集合在读取时已转换为第一个字体
            return true;
        }
        try (InputStream in = new ByteArrayInputStream(data)) {
            TrueTypeFont font = new TTFParser(true).parse(in);
            return font.getTableMap().containsKey("cmap");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取字体对象
     * <p>
     * PDFBox 不允许同一字体实例被写入不同文档，因此同一文档内按字体内容共用字体对象。
     *
     * @param pdf  PDF文档对象
     * @param data 字体文件数据
     * @return 字体对象
     * @throws IOException 字体加载异常
     */
    private PDType0Font getPooledFont(PDDocument pdf, byte[] data) throws IOException {
        if (fontPoolDocument != pdf) {
            fontPool.clear();
            fontPoolDocument = pdf;
        }
        String hash = hash(data);
        PDType0Font font = fontPool.get(hash);
        if (font != null) {
            return font;
        }
        try (InputStream in = new ByteArrayInputStream(data)) {
            font = PDType0Font.load(pdf, in, true);
        }
        fontPool.put(hash, font);
        return font;
    }

    /**
     * 计算字体数据的摘要，用于在字体池中区分字体
     *
     * @param data 字体文件数据
     * @return 字体数据摘要
     */
    private static String hash(byte[] data) {
        try {
            byte[] sha = java.security.MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(sha.length * 2);
            for (byte b : sha) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(Arrays.hashCode(data)) + "_" + data.length;
        }
    }

    /**
     * sfnt 文件头与表目录的长度，单位：字节
     * <p>
     * 12 字节文件头 + 每个字体表 16 字节目录项
     */
    private static final int SFNT_HEADER_SIZE = 12;

    /**
     * sfnt 表目录项长度，单位：字节
     */
    private static final int SFNT_TABLE_RECORD_SIZE = 16;

    /**
     * sfnt 字体表的 4 字节对齐填充
     */
    private static final int SFNT_TABLE_ALIGNMENT = 4;

    /**
     * 由字体读取器构建单个字体文件数据
     * <p>
     * 按 sfnt 结构逐表写出字体：文件头、表目录、表数据，用于将字体集合（TTC）中的单个字体
     * 转换为可嵌入 PDF 的独立字体文件。
     *
     * @param font 字体集合中的字体
     * @return 单个字体的文件数据
     * @throws IOException 字体数据读取异常
     */
    private static byte[] buildSingleFont(byte[] ttcData) throws IOException {
        List<String> tags = new ArrayList<>();
        List<byte[]> records = new ArrayList<>();
        int sfntVersion;
        int tableCount;
        int fontOffset;
        try (InputStream in = new ByteArrayInputStream(ttcData)) {
            // TTC 文件头：标识(4)、版本(4)、字体数量(4)，其后依次为各字体表目录的偏移量
            readBytes(in, 4);
            readInt(in);
            readInt(in);
            fontOffset = readInt(in);
        }
        try (InputStream in = skipTo(ttcData, fontOffset)) {
            sfntVersion = readInt(in);
            readBytes(in, 6);
            tableCount = readUnsignedShort(in);
            for (int i = 0; i < tableCount; i++) {
                byte[] record = readBytes(in, SFNT_TABLE_RECORD_SIZE);
                records.add(record);
                tags.add(new String(record, 0, 4, StandardCharsets.ISO_8859_1));
            }
        }
        int offset = SFNT_HEADER_SIZE + tableCount * SFNT_TABLE_RECORD_SIZE;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeInt(bos, sfntVersion);
        writeUnsignedShort(bos, tableCount);
        // searchRange、entrySelector、rangeShift 由表数量决定
        int searchRange = 1;
        int entrySelector = 0;
        while (searchRange * 2 <= tableCount) {
            searchRange *= 2;
            entrySelector++;
        }
        searchRange *= SFNT_TABLE_RECORD_SIZE;
        writeUnsignedShort(bos, searchRange);
        writeUnsignedShort(bos, entrySelector);
        writeUnsignedShort(bos, tableCount * SFNT_TABLE_RECORD_SIZE - searchRange);

        List<byte[]> tableData = new ArrayList<>();
        for (int i = 0; i < tableCount; i++) {
            byte[] table = readTable(ttcData, fontOffset, tags.get(i));
            tableData.add(table);
            writeInt(bos, readInt(records.get(i), 0));
            // 部分字体集合中的表记录校验和不准确，这里按表数据重新计算
            writeInt(bos, checksum(table));
            writeInt(bos, offset);
            writeInt(bos, table.length);
            offset += align(table.length);
        }
        for (byte[] table : tableData) {
            bos.write(table, 0, table.length);
            for (int p = table.length; p < align(table.length); p++) {
                bos.write(0);
            }
        }
        return bos.toByteArray();
    }

    /**
     * 读取字体中指定标签的字体表数据
     *
     * @param ttcData    字体集合（TTC）数据
     * @param fontOffset 字体表目录在数据中的偏移量，单位：字节
     * @param tag        字体表标签
     * @return 字体表数据
     * @throws IOException 字体数据读取异常
     */
    private static byte[] readTable(byte[] ttcData, int fontOffset, String tag) throws IOException {
        try (InputStream in = skipTo(ttcData, fontOffset)) {
            readInt(in);
            readBytes(in, 6);
            int tableCount = readUnsignedShort(in);
            for (int i = 0; i < tableCount; i++) {
                byte[] record = readBytes(in, SFNT_TABLE_RECORD_SIZE);
                if (tag.equals(new String(record, 0, 4, StandardCharsets.ISO_8859_1))) {
                    byte[] table = new byte[readInt(record, 12)];
                    System.arraycopy(ttcData, readInt(record, 8), table, 0, table.length);
                    return table;
                }
            }
        }
        throw new IOException("字体中不存在字体表: " + tag);
    }

    /**
     * 定位到字体数据中的指定偏移量
     *
     * @param data   字体数据
     * @param offset 偏移量，单位：字节
     * @return 从指定位置开始读取的输入流
     * @throws IOException 偏移量超出数据范围
     */
    private static InputStream skipTo(byte[] data, int offset) throws IOException {
        InputStream in = new ByteArrayInputStream(data);
        if (in.skip(offset) != offset) {
            throw new IOException("字体数据不完整");
        }
        return in;
    }

    /**
     * 计算字体表长度按 4 字节对齐后的长度
     *
     * @param length 字体表长度，单位：字节
     * @return 对齐后的长度，单位：字节
     */
    private static int align(int length) {
        return (length + SFNT_TABLE_ALIGNMENT - 1) / SFNT_TABLE_ALIGNMENT * SFNT_TABLE_ALIGNMENT;
    }

    /**
     * 计算字体表数据的校验和
     *
     * @param table 字体表数据
     * @return 校验和
     */
    private static int checksum(byte[] table) {
        int sum = 0;
        for (int i = 0; i < table.length; i += 4) {
            int value = 0;
            for (int k = 0; k < 4; k++) {
                int index = i + k;
                value = (value << 8) | (index < table.length ? (table[index] & 0xFF) : 0);
            }
            sum += value;
        }
        return sum;
    }

    /**
     * 读取大端无符号 32 位整数
     *
     * @param in 输入流
     * @return 读取的数值
     * @throws IOException 数据不足时抛出
     */
    private static int readInt(InputStream in) throws IOException {
        return readInt(readBytes(in, 4), 0);
    }

    /**
     * 读取字节数组中的大端无符号 32 位整数
     *
     * @param data   字节数组
     * @param offset 起始位置
     * @return 读取的数值
     */
    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }

    /**
     * 读取大端无符号 16 位整数
     *
     * @param in 输入流
     * @return 读取的数值
     * @throws IOException 数据不足时抛出
     */
    private static int readUnsignedShort(InputStream in) throws IOException {
        byte[] data = readBytes(in, 2);
        return ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
    }

    /**
     * 从输入流读取指定长度的数据
     *
     * @param in     输入流
     * @param length 读取长度，单位：字节
     * @return 读取的数据
     * @throws IOException 数据不足时抛出
     */
    private static byte[] readBytes(InputStream in, int length) throws IOException {
        if (length < 0) {
            throw new IOException("字体数据长度非法");
        }
        byte[] data = new byte[length];
        int read = 0;
        while (read < length) {
            int n = in.read(data, read, length - read);
            if (n < 0) {
                throw new IOException("字体数据不完整");
            }
            read += n;
        }
        return data;
    }

    /**
     * 写出大端 32 位整数
     *
     * @param out   输出流
     * @param value 数值
     */
    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    /**
     * 写出大端 16 位整数
     *
     * @param out   输出流
     * @param value 数值
     */
    private static void writeUnsignedShort(ByteArrayOutputStream out, int value) {
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    /**
     * 字体加载结果
     */
    private static final class FontHolder {
        /**
         * PDF 字体
         */
        private final PDFont font;
        /**
         * 是否使用了相近字体替换（此时字形索引可能失去原字体含义）
         */
        private final boolean enableSimilarFontReplace;
        /**
         * 字体无法编码时用于占位的字符
         */
        private final String placeholder;

        private FontHolder(PDFont font, boolean enableSimilarFontReplace) {
            this.font = font;
            this.enableSimilarFontReplace = enableSimilarFontReplace;
            this.placeholder = createPlaceholder(font);
        }

        /**
         * 创建字体占位字符
         * <p>
         * 字体可以编码空格时使用空格，否则使用字体能够编码的第一个字符。
         *
         * @param font 字体
         * @return 占位字符，无法编码任何字符时为空字符串
         */
        private static String createPlaceholder(PDFont font) {
            for (char c : new char[]{'\t', ' ', 'A', '0'}) {
                try {
                    font.encode(String.valueOf(c));
                    return String.valueOf(c);
                } catch (Exception ignored) {
                }
            }
            return "";
        }
    }

    private void writePath(ResourceManage resMgt,
                           PageContext ctx,
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
        ctx.pdfCanvas.saveGraphicsState();

        // 处理透明度
        Integer alpha = pathObject.getAlpha();
        if (alpha != null) {
            PDExtendedGraphicsState gs0 = new PDExtendedGraphicsState();
            gs0.setNonStrokingAlphaConstant(alpha / 255f);
            ctx.pdfCanvas.setGraphicsStateParameters(gs0);
        }

        // 页面与模板层中的图元坐标已是页面坐标，仅印章内的图元需要按印章区域缩放
        double scale = scaling(sealBox, pathObject.getBoundary());
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
                setStrokingColor(ctx, null, convertPDColor(strokeColor.getValue()));
            }
            PDShading axialShdColor = parseAxial(strokeColor.getOFDElement("AxialShd"), box, pathObject);
            if (axialShdColor != null) {
                setStrokingColor(ctx, registerPattern(ctx, axialShdColor), null);
            }
            PDShading radialShdColor = parseRadial(strokeColor.getOFDElement("RadialShd"), box, pathObject);
            if (radialShdColor != null) {
                setStrokingColor(ctx, registerPattern(ctx, radialShdColor), null);
            }
        } else {
            setStrokingColor(ctx, null, defaultStrokeColor);
        }

        boolean legacyAbsolutePath = sealBox == null && annotBox == null && compositeObjectBoundary == null
                && PointUtil.isLegacyAbsolutePath(box.getWidth(), box.getHeight(), pathObject.getBoundary(),
                PointUtil.convertPathAbbreviatedDatatoPoint(pathObject.getAbbreviatedData()),
                pathObject.getCTM() != null, pathObject.getCTM());
        float lineWidth = defaultLineWidth;
        if (pathObject.getLineWidth() != null && pathObject.getLineWidth() > 0) {
            lineWidth = (float) PointUtil.calPdfPathLineWidth(pathObject.getLineWidth(), scale,
                    legacyAbsolutePath, pathObject.getCTM());
        }
        ctx.lineWidth = lineWidth;
        ctx.pdfCanvas.setLineWidth(lineWidth);
        if (pathObject.getStroke()) {
            if (pathObject.getDashPattern() != null) {
                float unitsOn = (float) converterDpi(pathObject.getDashPattern().toDouble()[0].floatValue());
                float unitsOff = (float) converterDpi(pathObject.getDashPattern().toDouble()[1].floatValue());
                float phase = (float) converterDpi(pathObject.getDashOffset().floatValue());
                ctx.pdfCanvas.setLineDashPattern(new float[]{unitsOn, unitsOff}, phase);
            }
            ctx.pdfCanvas.setLineJoinStyle(pathObject.getJoin().ordinal());
            ctx.pdfCanvas.setLineCapStyle(pathObject.getCap().ordinal());
            ctx.pdfCanvas.setMiterLimit(pathObject.getMiterLimit().floatValue());
            path(ctx, box, sealBox, annotBox, pathObject, compositeObjectBoundary, compositeObjectCTM);

            strokePath(ctx);
            ctx.pdfCanvas.restoreGraphicsState();
        }
        if (pathObject.getFill()) {
            ctx.pdfCanvas.saveGraphicsState();
            if (compositeObjectAlpha != null) {
                PDExtendedGraphicsState gs1 = new PDExtendedGraphicsState();
                gs1.setNonStrokingAlphaConstant(compositeObjectAlpha * 1.0f / 255f);
                ctx.pdfCanvas.setGraphicsStateParameters(gs1);
            }
            FillColor fillColor = (FillColor) pathObject.getFillColor();
            if (fillColor != null) {
                if (fillColor.getValue() != null) {
                    setNonStrokingColor(ctx, null, convertPDColor(fillColor.getValue()));
                }
                PDShading axialShdColor = parseAxial(fillColor.getOFDElement("AxialShd"), box, pathObject);
                if (axialShdColor != null) {
                    setNonStrokingColor(ctx, registerPattern(ctx, axialShdColor), null);
                }
                PDShading radialShdColor = parseRadial(fillColor.getOFDElement("RadialShd"), box, pathObject);
                if (radialShdColor != null) {
                    setNonStrokingColor(ctx, registerPattern(ctx, radialShdColor), null);
                }
            } else {
                // 未设置颜色时，以透明色填充（规范中有明确说明）
                PDExtendedGraphicsState gs2 = new PDExtendedGraphicsState();
                gs2.setNonStrokingAlphaConstant(0f);
                ctx.pdfCanvas.setGraphicsStateParameters(gs2);
            }
            path(ctx, box, sealBox, annotBox, pathObject, compositeObjectBoundary, compositeObjectCTM);
            if (null != pathObject.getRule() && pathObject.getRule().equals(Rule.Even_Odd)) {
                fillPath(ctx, true);
            } else {
                fillPath(ctx, false);
            }
            ctx.pdfCanvas.restoreGraphicsState();
        }
    }

    private void path(PageContext ctx, ST_Box box, ST_Box sealBox, ST_Box annotBox, PathObject pathObject, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM) throws IOException {
        if (pathObject.getBoundary() == null) {
            return;
        }
        double scale = scaling(sealBox, pathObject.getBoundary());
        if (sealBox != null) {
            pathObject.setBoundary(pathObject.getBoundary().getTopLeftX() + sealBox.getTopLeftX(),
                    pathObject.getBoundary().getTopLeftY() + sealBox.getTopLeftY(),
                    pathObject.getBoundary().getWidth(),
                    pathObject.getBoundary().getHeight());
        }
        if (annotBox != null) {
            pathObject.setBoundary(
                    pathObject.getBoundary().getTopLeftX() + annotBox.getTopLeftX(),
                    pathObject.getBoundary().getTopLeftY() + annotBox.getTopLeftY(),
                    pathObject.getBoundary().getWidth(),
                    pathObject.getBoundary().getHeight());
        }

        // 裁剪
        clip(ctx, box, pathObject);

        List<PathPoint> listPoint = PointUtil.calPdfPathPoint(box.getWidth(), box.getHeight(), pathObject.getBoundary(), PointUtil.convertPathAbbreviatedDatatoPoint(pathObject.getAbbreviatedData()), pathObject.getCTM() != null, pathObject.getCTM(), compositeObjectBoundary, compositeObjectCTM, true, scale);
        for (int i = 0; i < listPoint.size(); i++) {
            PathPoint point = listPoint.get(i);
            // 路径点已换算为 PDF 页面坐标且单位已转换为磅
            if (point.type.equals("M") || point.type.equals("S")) {
                ctx.pdfCanvas.moveTo(point.x1, point.y1);
            } else if (point.type.equals("L")) {
                ctx.pdfCanvas.lineTo(point.x1, point.y1);
            } else if (point.type.equals("B")) {
                ctx.pdfCanvas.curveTo(point.x1, point.y1, point.x2, point.y2, point.x3, point.y3);
            } else if (point.type.equals("Q")) {
                // 二次贝塞尔曲线：以起点为第一控制点转换为三次贝塞尔曲线
                ctx.pdfCanvas.curveTo1(point.x1, point.y1, point.x2, point.y2);
            } else if (point.type.equals("C")) {
                ctx.pdfCanvas.closePath();
            }
        }
        // 闭合当前子路径，确保填充与描边按闭合区域处理
        ctx.pdfCanvas.closePath();
    }

    private void clip(PageContext ctx, ST_Box box, PathObject pathObject) throws IOException {
        if (pathObject.getClips() == null) {
            return;
        }

        List<CT_Clip> clips = pathObject.getClips().getClips();
        for (int k = 0; k < clips.size(); k++) {
            CT_Clip clip = clips.get(k);
            boolean hasPath = false;
            for (Area area : clip.getAreas()) {
                Element elePath = area.getOFDElement("Path");
                if (elePath == null) {
                    continue;
                }
                CT_Path path = new CT_Path(elePath);
                List<PathPoint> points = PointUtil.calPdfPathPoint(box.getWidth(), box.getHeight(),
                        PointUtil.combineBoundary(pathObject.getBoundary(), path.getBoundary()),
                        PointUtil.convertPathAbbreviatedDatatoPoint(path.getAbbreviatedData()), area.getCTM() != null,
                        area.getCTM(), null, null, true, 1.0);
                for (int i = 0; i < points.size(); i++) {
                    PathPoint pathPoint = points.get(i);
                    if (pathPoint.type.equals("M") || pathPoint.type.equals("S")) {
                        ctx.pdfCanvas.moveTo(pathPoint.x1, pathPoint.y1);
                    } else if (pathPoint.type.equals("L")) {
                        ctx.pdfCanvas.lineTo(pathPoint.x1, pathPoint.y1);
                    } else if (pathPoint.type.equals("B")) {
                        ctx.pdfCanvas.curveTo(pathPoint.x1, pathPoint.y1, pathPoint.x2, pathPoint.y2,
                                pathPoint.x3, pathPoint.y3);
                    } else if (pathPoint.type.equals("Q")) {
                        // 二次贝塞尔曲线：以终点为第二控制点转换为三次贝塞尔曲线
                        ctx.pdfCanvas.curveTo2(pathPoint.x1, pathPoint.y1, pathPoint.x2, pathPoint.y2);
                    } else if (pathPoint.type.equals("C")) {
                        ctx.pdfCanvas.closePath();
                    }
                }
                hasPath = true;
            }
            if (hasPath) {
                ctx.pdfCanvas.closePath();
                // 先写入路径，裁剪执行出现异常时清除路径，避免后续填充命中残留路径
                ctx.pdfCanvas.appendRawCommands("W" + System.lineSeparator());
                try {
                    ctx.pdfCanvas.clip();
                } catch (Exception e) {
                    logger.debug("裁剪路径执行失败: {}", e.getMessage());
                    ctx.pdfCanvas.appendRawCommands("n" + System.lineSeparator());
                }
            }
        }

    }

    /**
     * 计算当前盒子到目标盒子的缩放比例
     *
     * @param targetBox  目标区域大小
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

    private void writeImage(ResourceManage resMgt, PDDocument pdf, PageContext ctx, ST_Box box, ImageObject imageObject, ST_Box annotBox, Integer compositeObjectAlpha, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM) throws IOException {
        final ST_RefID resourceID = imageObject.getResourceID();
        if (resourceID == null) {
            return;
        }
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = resMgt.getImage(resourceID.toString());
        } catch (Exception e) {
            logger.warn("图片ID: {} 解析失败 跳过，原因 {}", resourceID, e.getMessage());
        }
        if (bufferedImage == null) {
            return;
        }
        ctx.pdfCanvas.saveGraphicsState();

        // OFD image objects use normal source-over compositing. Applying Multiply
        // changes opaque white pixels into transparent-looking pixels and exposes
        // content that should have been covered by the image.
        Integer alpha = imageObject.getAlpha();
        if (alpha != null && alpha < 255) {
            PDExtendedGraphicsState extGState = new PDExtendedGraphicsState();
            extGState.setNonStrokingAlphaConstant(alpha * 1.0f / 255);
            ctx.pdfCanvas.setGraphicsStateParameters(extGState);
        }

        PDImageXObject pdfImageObject = createImageObject(pdf, resMgt, resourceID, bufferedImage);
        if (annotBox != null && !isSameBox(annotBox, imageObject.getBoundary())) {
            float x = annotBox.getTopLeftX().floatValue();
            float width = annotBox.getWidth().floatValue();
            float height = annotBox.getHeight().floatValue();
            ctx.pdfCanvas.drawImage(pdfImageObject, (float) converterDpi(x),
                    toPdfY(box, annotBox.getTopLeftY().floatValue() + height),
                    (float) converterDpi(width), (float) converterDpi(height));
        } else {
            Matrix matrix = toPdfMatrix(box, CommonUtil.getImageMatrixFromOfd(
                    imageObject, box, compositeObjectCTM, false));
            ctx.pdfCanvas.drawImage(pdfImageObject, matrix);
        }
        ctx.pdfCanvas.restoreGraphicsState();
    }

    /**
     * 根据图片格式创建 PDF 图像对象
     *
     * @param pdf           PDF文档对象
     * @param resMgt        资源管理器
     * @param resourceID    图片资源ID
     * @param bufferedImage 图片
     * @return PDF图像对象
     * @throws IOException 图像资源写入异常
     */
    private PDImageXObject createImageObject(PDDocument pdf, ResourceManage resMgt, ST_RefID resourceID,
                                             BufferedImage bufferedImage) throws IOException {
        // 根据图片格式决定图片使用哪种创建方式
        CT_MultiMedia multiMedia = resMgt.getMultiMedia(resourceID.toString());
        if (multiMedia != null && "JPEG".equals(multiMedia.getFormat())) {
            return JPEGFactory.createFromImage(pdf, bufferedImage);
        }
        return LosslessFactory.createFromImage(pdf, bufferedImage);
    }

    private void writeSealImage(PDDocument pdfDocument, PageContext ctx, ST_Box box, byte[] image, ST_Box sealBox, ST_Box clipBox) throws IOException {
        if (image == null) {
            return;
        }
        float width = sealBox.getWidth().floatValue();
        float height = sealBox.getHeight().floatValue();
        // 表单坐标原点位于印章区域左下角，并以该位置绘制到页面上
        float pdfX = (float) converterDpi(sealBox.getTopLeftX().floatValue());
        float pdfY = toPdfY(box, sealBox.getTopLeftY().floatValue() + height);

        ctx.pdfCanvas.saveGraphicsState();
        // 印章图片及其裁剪区域绘制在独立的表单中，再按印章位置整体绘制到页面
        PDRectangle rect = new PDRectangle((float) converterDpi(width), (float) converterDpi(height));
        PDFormXObject xObject = new PDFormXObject(new PDStream(pdfDocument));
        xObject.setBBox(rect);
        xObject.setResources(new PDResources());
        try (PDPageContentStream xObjectCanvas = new PDPageContentStream(pdfDocument, xObject, new ByteArrayOutputStream())) {
            if (clipBox != null) {
                xObjectCanvas.addRect((float) converterDpi(clipBox.getTopLeftX()),
                        rect.getHeight() - (float) (converterDpi(clipBox.getTopLeftY()) + converterDpi(clipBox.getHeight())),
                        (float) converterDpi(clipBox.getWidth()), (float) converterDpi(clipBox.getHeight()));
                xObjectCanvas.clip();
            }
            // 将背景设置为透明，抠图阈值为 244（实践得到最佳）
            BufferedImage bImg = ImageUtils.clearWhiteBackground(ImageIO.read(new ByteArrayInputStream(image)), 244);
            PDImageXObject img = LosslessFactory.createFromImage(pdfDocument, bImg);
            xObjectCanvas.drawImage(img, 0, 0, rect.getWidth(), rect.getHeight());
        }
        ctx.pdfCanvas.drawXObject(xObject, java.awt.geom.AffineTransform.getTranslateInstance(pdfX, pdfY));
        ctx.pdfCanvas.restoreGraphicsState();
    }
    /**
     * 页面绘制状态
     * <p>
     * 记录页面内容流及其资源字典，用于在同一页面内注册图案等绘制资源。
     */
    private static final class PageContext {
        /**
         * 页面内容流
         */
        private final PDPageContentStream pdfCanvas;

        /**
         * 页面资源字典
         */
        private final COSDictionary resources;

        /**
         * 页面资源中的图案字典
         */
        private final COSDictionary patterns;

        /**
         * 当前描边颜色
         */
        private PDColor strokingColor;

        /**
         * 当前描边颜色使用的图案名称，为空表示 RGB 颜色
         */
        private COSName strokingPatternName;

        /**
         * 当前填充颜色
         */
        private PDColor nonStrokingColor;

        /**
         * 当前填充颜色使用的图案名称，为空表示 RGB 颜色
         */
        private COSName nonStrokingPatternName;

        /**
         * 当前线宽，单位：磅
         */
        private float lineWidth = 0.9978f;


        /**
         * @param pdfCanvas 页面内容流
         * @param resources 页面资源字典
         */
        private PageContext(PDPageContentStream pdfCanvas, COSDictionary resources) {
            this.pdfCanvas = pdfCanvas;
            this.resources = resources;
            COSDictionary patternDict = (COSDictionary) resources.getDictionaryObject(COSName.PATTERN);
            if (patternDict == null) {
                patternDict = new COSDictionary();
                resources.setItem(COSName.PATTERN, patternDict);
            }
            this.patterns = patternDict;
        }
    }

}
