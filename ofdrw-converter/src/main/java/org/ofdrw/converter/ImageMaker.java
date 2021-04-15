package org.ofdrw.converter;

import org.apache.fontbox.ttf.GlyphData;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.ofdrw.converter.point.Tuple2;
import org.ofdrw.reader.tools.ImageUtils;
import org.ofdrw.converter.utils.MatrixUtils;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.*;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.CT_GraphicUnit;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.color.colorSpace.OFDColorSpaceType;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.core.text.CT_CGTransform;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.DLOFDReader;
import org.ofdrw.reader.ResourceManage;
import org.ofdrw.reader.model.AnnotionVo;
import org.ofdrw.reader.model.OfdPageVo;
import org.ofdrw.reader.model.StampAnnotVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ujmp.core.Matrix;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static org.ofdrw.converter.utils.CommonUtil.getPageBox;

/**
 * 图片转换类
 *
 * @author qaqtutu
 * @since 2021-03-13 10:00:01
 */
public class ImageMaker {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    private DLOFDReader ofdReader;

    /**
     * 每毫米像素数量(Pixels per millimeter)
     * <p>
     * 默认为： 8像素/毫米
     */
    private int ppm = 8;

    public final Config config = new Config();

    /**
     * 是否是印章，用于渲染印章
     */
    private boolean isStamp = false;


    private List<OfdPageVo> pages = null;
    private final ResourceManage resourceManage;
    /**
     * 加载后的字体缓存
     * <p>
     * 防止重复加载文件读写和解析带来耗时
     * <p>
     * KEY: 字族名_字体名_字体路径
     */
    private final Map<String, TrueTypeFont> fontCache = new HashMap<>();

    /**
     * 创建图片转换对象实例
     * <p>
     * OFD内部使用毫米作为基本单位
     *
     * @param ofdReader OFD解析器
     * @param ppm       每毫米像素数量(Pixels per millimeter)
     */
    public ImageMaker(DLOFDReader ofdReader, int ppm) {
        this.ofdReader = ofdReader;
        this.resourceManage = ofdReader.getResMgt();
        this.pages = ofdReader.getOFDDocumentVo().getOfdPageVoList();
        if (this.ppm > 0) {
            this.ppm = ppm;
        }
    }

    public int pageSize() {
        return pages.size();
    }

    public BufferedImage makePage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new GeneralConvertException(String.format("%s 不是有效索引", pageIndex));
        }
        OfdPageVo pageVo = pages.get(pageIndex);
        Page contentPage = pageVo.getContentPage();
//        Page templatePage = pageVo.getTemplatePage();
        ST_Box pageBox = getPageBox(contentPage.getArea(), ofdReader.getOFDDocumentVo().getPageWidth(), ofdReader.getOFDDocumentVo().getPageHeight());
        double pageWidthPixel = ppm * pageBox.getWidth();
        double pageHeightPixel = ppm * pageBox.getHeight();

        BufferedImage image = createImage((int) pageWidthPixel, (int) pageHeightPixel);
        Graphics2D graphics = (Graphics2D) image.getGraphics();

        writePage(graphics, pageVo, null);

        // 绘制电子印章图片
        for (int i = 0; i < ofdReader.getOFDDocumentVo().getStampAnnotVos().size(); i++) {
            StampAnnotVo stampAnnotVo = ofdReader.getOFDDocumentVo().getStampAnnotVos().get(i);
            List<StampAnnot> stampAnnots = stampAnnotVo.getStampAnnots();
            for (StampAnnot stampAnnot : stampAnnots) {
                if (stampAnnot.getPageRef().toString().equals(contentPage.getObjID().toString())) {
                    writeStampAnnot(graphics, stampAnnotVo, stampAnnot);
                }
            }
        }

        // 绘制注解对象
        for (AnnotionVo annotionVo : ofdReader.getOFDDocumentVo().getAnnotaions()) {
            if (pageVo.getContentPage().getObjID().toString().equals(annotionVo.getPageId()) && null != annotionVo.getAnnots()) {
                for (Annot annot : annotionVo.getAnnots()) {
                    Appearance appearance = annot.getAppearance();
                    writeContent(graphics, appearance, null, null);
                }
            }
        }

        return image;
    }

    /**
     * 创建图片
     *
     * @param pageWidthPixel  图形宽度
     * @param pageHeightPixel 图像高度
     */
    private BufferedImage createImage(int pageWidthPixel, int pageHeightPixel) {
        return ImageUtils.createImage(pageWidthPixel, pageHeightPixel, isStamp);
    }

    private void writeStampAnnot(Graphics2D graphics, StampAnnotVo stampAnnotVo, StampAnnot stampAnnot) {
        BufferedImage stampImage = null;
        graphics = (Graphics2D) graphics.create();
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(stampAnnotVo.getImgByte())) {
            if (stampAnnotVo.getType().equals("ofd")) {
                ImageMaker imageMaker = new ImageMaker(new DLOFDReader(inputStream), ppm);
                imageMaker.isStamp = true;
                imageMaker.config.setDrawBoundary(config.drawBoundary);
                if (imageMaker.pageSize() > 0) {
                    logger.debug("渲染ofd格式印章");
                    stampImage = imageMaker.makePage(0);
                }
            } else {
                stampImage = ImageIO.read(inputStream);
            }
            if (stampImage != null) {
                if (config.clearStampBackground) {
                    stampImage = ImageUtils.clearWhiteBackground(stampImage, config.stampBackgroundGray);
                }

                ST_Box stBox = stampAnnot.getBoundary();
                Matrix m = MatrixUtils.base();
                graphics.setTransform(MatrixUtils.createAffineTransform(m));

                // 调整比例
                final double fx = stBox.getWidth() / stampImage.getWidth();
                final double fy = stBox.getHeight() / stampImage.getHeight();
                m = MatrixUtils.scale(m, fx, fy);
                // 首先平移到指定位置
                m = MatrixUtils.move(m, stBox.getTopLeftX(), stBox.getTopLeftY());
                // 缩放适应
                m = MatrixUtils.scale(m, ppm, ppm);

                graphics.setClip(null);
                graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, config.stampOpacity));
                graphics.drawImage(stampImage, MatrixUtils.createAffineTransform(m), null);
            }
        } catch (Exception e) {
            logger.error("印章绘制异常", e);
        } finally {
            graphics.dispose();
        }
    }

    private void writePage(Graphics2D graphics, OfdPageVo page, Matrix matrix) {
        Page contentPage = page.getContentPage();
        Page templatePage = page.getTemplatePage();
        List<CT_Layer> templateLayers = new ArrayList<>();
        List<CT_Layer> layers = new ArrayList<>(contentPage.getContent().getLayers());
        if (templatePage != null && templatePage.getContent() != null) {
            templateLayers.addAll(templatePage.getContent().getLayers());
        }

        Comparator<CT_Layer> comparator = Comparator.comparing(CT_Layer::getType);
        layers.sort(comparator);
        templateLayers.sort(comparator);

        for (CT_Layer layer : templateLayers) {
            writeContent(graphics, layer, null, matrix);
        }
        for (CT_Layer layer : layers) {
            writeContent(graphics, layer, null, matrix);
        }

    }


    private void writeContent(Graphics2D graphics, CT_PageBlock pageBlock, List<CT_DrawParam> drawParams, Matrix parentMatrix) {
        graphics = (Graphics2D) graphics.create();
        try {
            if (drawParams == null) drawParams = new ArrayList<>();
            if (parentMatrix == null) parentMatrix = MatrixUtils.base();

            if (pageBlock instanceof CT_Layer) {
                ST_RefID drawParamRef = ((CT_Layer) pageBlock).getDrawParam();
                if (drawParamRef != null) {
                    CT_DrawParam ctDrawParam = resourceManage.getDrawParam(drawParamRef.getRefId().toString());
                    if (ctDrawParam != null) {
                        drawParams.add(ctDrawParam);
                    }
                }
            }

            if (pageBlock.attribute("Boundary") != null) {
                String data = (String) pageBlock.attribute("Boundary").getData();
                ST_Box stBox = ST_Box.getInstance(data);
                parentMatrix = MatrixUtils.move(parentMatrix, stBox.getTopLeftX(), stBox.getTopLeftY());
            }

            for (PageBlockType object : pageBlock.getPageBlocks()) {
                try {
                    if (object instanceof TextObject) {
                        TextObject textObject = (TextObject) object;
                        writeText(graphics, textObject, drawParams, parentMatrix);
                    } else if (object instanceof ImageObject) {
                        ImageObject imageObject = (ImageObject) object;
                        writeImage(graphics, imageObject, drawParams, parentMatrix);
                    } else if (object instanceof PathObject) {
                        PathObject pathObject = (PathObject) object;
                        writePath(graphics, pathObject, drawParams, parentMatrix);
                    } else if (object instanceof CompositeObject) {
                        logger.info("暂不支持复合对象");
                    } else if (object instanceof CT_PageBlock) {
                        CT_PageBlock block = (CT_PageBlock) object;
                        writeContent(graphics, block, drawParams, parentMatrix);
                    } else if (object instanceof CT_Layer) {
                        CT_Layer layer = (CT_Layer) object;
                        ST_RefID drawParamRef = layer.getDrawParam();
                        CT_DrawParam ctDrawParam = resourceManage.getDrawParam(drawParamRef.getRefId().toString());
                        drawParams.add(ctDrawParam);
                        writeContent(graphics, layer, drawParams, parentMatrix);
                    }
                } catch (Exception e) {
                    logger.warn("PageBlock无法渲染:", e);
                }
            }
        } finally {
            graphics.dispose();
        }
    }

    private void writePath(Graphics2D graphics, PathObject pathObject, List<CT_DrawParam> drawParams, Matrix parentMatrix) {
        ST_Box boundary = pathObject.getBoundary();
        Matrix baseMatrix = renderBoundaryAndSetClip(graphics, boundary, parentMatrix);
        Matrix matrix = MatrixUtils.base();
        if (pathObject.getCTM() != null) {
            matrix = matrix.mtimes(MatrixUtils.ctm(pathObject.getCTM().toDouble()));
        }

        if (boundary != null) {
            matrix = MatrixUtils.move(matrix, boundary.getTopLeftX(), boundary.getTopLeftY());
        }
        matrix = matrix.mtimes(baseMatrix);
        graphics.transform(MatrixUtils.createAffineTransform(matrix));

        Path2D path2D = buildPath(pathObject.getAbbreviatedData());


        if (pathObject.getStroke() == null || pathObject.getStroke()) {
            graphics.setStroke(new BasicStroke(getLineWidth(pathObject, drawParams).floatValue()));
            Color strokeColor = getStrokeColor(pathObject.getStrokeColor(), CT_Color.rgb(0, 0, 0), drawParams);
            if (strokeColor != null) {
                graphics.setColor(strokeColor);
                graphics.draw(path2D);
            }
        }
        if (pathObject.getFill() != null && pathObject.getFill()) {
            Color fillColor = getFillColor(pathObject.getFillColor(), null, drawParams);
            if (fillColor != null) {
                graphics.setColor(fillColor);
                graphics.fill(path2D);
            }
        }
    }

    private void writeImage(Graphics2D graphics, ImageObject imageObject, List<CT_DrawParam> drawParams, Matrix parentMatrix) {
        ST_Box boundary = imageObject.getBoundary();
        Matrix baseMatrix = renderBoundaryAndSetClip(graphics, boundary, parentMatrix);

        BufferedImage image = null;
        try {
            // 解析图片对象获取图片
            image = resourceManage.getImage(imageObject);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Matrix m = MatrixUtils.base();
        // 把图片还原成1*1
        m = MatrixUtils.scale(m, Double.valueOf(1.0 / image.getWidth()).floatValue(), Double.valueOf(1.0 / image.getHeight()).floatValue());

        if (imageObject.getCTM() != null) {
            m = m.mtimes(MatrixUtils.ctm(imageObject.getCTM().toDouble()));
        }
        if (boundary != null) {
            m = MatrixUtils.move(m, boundary.getTopLeftX(), boundary.getTopLeftY());
        }
        m = m.mtimes(baseMatrix);

        graphics.setTransform(MatrixUtils.createAffineTransform(MatrixUtils.base()));

        float alpha = 1;
        if (imageObject.attributeValue("Alpha") != null) {
            alpha = imageObject.getAlpha() / 255.0f;
        }
        Composite oldComposite = graphics.getComposite();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        graphics.setTransform(MatrixUtils.createAffineTransform(MatrixUtils.base()));
        graphics.drawImage(image, MatrixUtils.createAffineTransform(m), null);
        graphics.setComposite(oldComposite);

    }

    private void writeText(Graphics2D graphics, TextObject textObject, List<CT_DrawParam> drawParams, Matrix parentMatrix) {
        logger.debug("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━TextObject━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓");

        Color strokeColor = getStrokeColor(textObject.getStrokeColor(), null, drawParams);
        Color fillColor = getFillColor(textObject.getFillColor(), null, drawParams);
        if (fillColor == null) fillColor = Color.black;

        ST_Box boundary = textObject.getBoundary();
        Matrix baseMatrix = renderBoundaryAndSetClip(graphics, boundary, parentMatrix);

        // 读取字体
        TrueTypeFont typeFont = getFont(textObject);
        List<Number> fontMatrix = null;

        if (typeFont == null) {
            logger.error("加载字体失败：" + textObject.getFont());
//            typeFont = FontLoader.getInstance().loadDefaultFont();
            return;
        } else {
            try {
//                logger.debug("字体名：" + typeFont.getName());
//                logger.debug("字体表：" + typeFont.getTables().stream().map(ttfTable -> ttfTable.getTag() + " ").collect(Collectors.joining()));
                fontMatrix = typeFont.getFontMatrix();
            } catch (Exception e) {
                logger.warn("解析加载异常", e);
            }
        }

        List<CT_CGTransform> transforms = textObject.getCGTransforms();
        int globalPoint = 0; // 字符计数，用来比较是否跟某个Transforms起始点重合
        int transPoint = -1; // 下一个Transforms，-1表示不存在
        if (transforms != null && transforms.size() >= 1) {
            transPoint = 0;
            transforms.stream().forEach(transform -> {
                if (transform.getCodePosition() == null) {
                    transform.setCodePosition(0);
                }
            });
            transforms.sort((t1, t2) -> {
                return t1.getCodePosition() - t2.getCodePosition();
            });
        }
        for (TextCode textCode : textObject.getTextCodes()) {
            int deltaOffset = -1;
            logger.debug("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━TextCode━━━━━━━━━━━━━━━━━━━━━━━━━━━┓");
            logger.debug("TextCode: " + textCode.getContent());
            logger.debug("DeltaX:" + textCode.getDeltaX());
            logger.debug("DeltaY:" + textCode.getDeltaY());

            List<Double> deltaX = parseDelta(textCode.getDeltaX());
            List<Double> deltaY = parseDelta(textCode.getDeltaY());
            Double x = textCode.getX();
            if (x == null) x = 0.0;
            Double y = textCode.getY();
            if (y == null) y = 0.0;

            for (int j = 0; j < textCode.getContent().length(); j++) {
                if (transPoint == -1 || globalPoint < transforms.get(transPoint).getCodePosition()) {
                    if (deltaOffset != -1) {
                        x += (deltaX == null || deltaX.size() < 0) ? 0.0 : (deltaOffset < deltaX.size() ? deltaX.get(deltaOffset) : deltaX.get(deltaX.size() - 1));
                        y += (deltaY == null || deltaY.size() < 0) ? 0.0 : (deltaOffset < deltaY.size() ? deltaY.get(deltaOffset) : deltaY.get(deltaY.size() - 1));
                    }
                    char c = textCode.getContent().charAt(j);
                    logger.debug(String.format("编码索引 <%s> DeltaX:%s DeltaY:%s", c, x, y));
                    try {
                        int gid = typeFont.getUnicodeCmap().getGlyphId((int) c);
                        typeFont.getFontMatrix();
                        GlyphData glyphData = typeFont.getGlyph().getGlyph(gid);
                        if (glyphData == null) {
                            logger.debug(String.format("找不到字形 %s", c));
                        } else {
                            Shape shape = glyphData.getPath();
                            logger.debug(String.format("字形Shape %s", shape));
                            Matrix matrix = chatMatrix(textObject, x, y, textObject.getSize(), fontMatrix, baseMatrix);
                            renderChar(graphics, shape, matrix, strokeColor, fillColor);
                        }
                    } catch (IOException e) {
                        logger.warn("文字渲染异常：", e);
                    }
                    globalPoint++;
                    deltaOffset++;
                } else {
                    CT_CGTransform transform = transforms.get(transPoint);
                    List<String> glyphs = transform.getGlyphs().getArray();
                    logger.debug("字形变换：" + transform);
                    for (String glyphStr : glyphs) {
                        Integer glyph = new Integer(glyphStr);
                        if (deltaOffset != -1) {
                            x += (deltaX == null || deltaX.size() < 0) ? 0.0 : (deltaOffset < deltaX.size() ? deltaX.get(deltaOffset) : deltaX.get(deltaX.size() - 1));
                            y += (deltaY == null || deltaY.size() < 0) ? 0.0 : (deltaOffset < deltaY.size() ? deltaY.get(deltaOffset) : deltaY.get(deltaY.size() - 1));
                        }
                        logger.debug(String.format("字形索引 <%s> DeltaX:%s DeltaY:%s", glyph, x, y));
                        try {
//                            typeFont.getGlyph().getGlyphs();
                            GlyphData glyphData = typeFont.getGlyph().getGlyph(glyph);
                            if (glyphData != null) {
                                Shape shape = glyphData.getPath();
                                Matrix matrix = chatMatrix(textObject, x, y, textObject.getSize(), fontMatrix, baseMatrix);
                                renderChar(graphics, shape, matrix, strokeColor, fillColor);
                            }
                        } catch (IOException e) {
                            logger.warn("文字渲染异常：", e);
                        }
                        deltaOffset++;
                    }
                    if (transPoint + 1 >= transforms.size()) {
                        transPoint = -1;
                    } else {
                        transPoint++;
                    }
                    globalPoint += (transform.getCodeCount() != null ? transform.getCodeCount() : glyphs.size());
                    j += (transform.getCodeCount() != null ? transform.getCodeCount() : glyphs.size());
                }

            }
            logger.debug("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛");
            globalPoint += textCode.getContent().length();
        }
        logger.debug("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛");
    }

    private Matrix chatMatrix(TextObject ctText, Double deltaX, Double deltaY, Double fontSize, List<Number> fontMatrix, Matrix baseMatrix) {
        Matrix m = MatrixUtils.base();
        m = MatrixUtils.imageMatrix(m, 0, 1, 0);
        if (ctText.getHScale() != null) {
            m = MatrixUtils.scale(m, ctText.getHScale(), 1);
        }
        m = m.mtimes(MatrixUtils.create(fontMatrix.get(0).doubleValue(), fontMatrix.get(1).doubleValue(),
                fontMatrix.get(2).doubleValue(), fontMatrix.get(3).doubleValue(),
                fontMatrix.get(4).doubleValue(), fontMatrix.get(5).doubleValue()));
        m = MatrixUtils.scale(m, fontSize, fontSize);
        m = MatrixUtils.move(m, deltaX, deltaY);
        if (ctText.getCTM() != null) {
            m = m.mtimes(MatrixUtils.ctm(ctText.getCTM().toDouble()));
        }
        if (ctText.getBoundary() != null) {
            m = MatrixUtils.move(m, ctText.getBoundary().getTopLeftX(), ctText.getBoundary().getTopLeftY());
        }
        m = m.mtimes(baseMatrix);
        return m;
    }

    private void renderChar(Graphics2D graphics, Shape shape, Matrix m, Color stroke, Color fill) {
        graphics.setClip(null);
        graphics.setTransform(MatrixUtils.createAffineTransform(m));
        graphics.setStroke(new BasicStroke(0.1f));
        graphics.setColor(Color.BLACK);
        graphics.setBackground(Color.white);
        if (stroke != null) {
            graphics.setColor(stroke);
            graphics.draw(shape);
        }
        if (fill != null) {
            graphics.setColor(fill);
            graphics.fill(shape);
        }
    }

    private String getResFilePath(String filename) {
        String srcPath = ofdReader.getOFDDir().getSysAbsPath() + "/" + ofdReader.getOFDDocumentVo().getDocPath() + "/Res/" + filename;
        if (!new File(srcPath).exists()) {
            if (filename.indexOf("/") >= 0) {
                filename = filename.substring(filename.lastIndexOf("/") + 1);
            }
            File file = findFile(new File(ofdReader.getOFDDir().getSysAbsPath()), filename);
            if (file != null) return file.getAbsolutePath();
        }
        return srcPath;
    }

    private static File findFile(File dir, String filename) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                File res = findFile(file, filename);
                if (res != null) return res;
            } else {
                if (file.getName().toLowerCase().equals(filename.toLowerCase())) {
                    return file;
                }
            }
        }
        return null;
    }

    /**
     * 解析字体对象获取字体
     *
     * @param textObject 字体对象
     * @return 字体
     */
    private TrueTypeFont getFont(TextObject textObject) {
        ST_RefID stRefID = textObject.getFont();
        if (stRefID == null) return null;

        CT_Font ctFont = resourceManage.getFont(stRefID.toString());
        if (ctFont == null) {
            return null;
        }

        String key = String.format("%s_%s_%s", ctFont.getFamilyName(), ctFont.getFontName(), ctFont.getFontFile());
        if (fontCache.containsKey(key)) {
            // 命中缓存，直接返还已经缓存的字体对象
            return fontCache.get(key);
        }

        TrueTypeFont trueTypeFont = null;
        if (ctFont.getFontFile() != null) {
            String absFontPath = getResFilePath(ctFont.getFontFile().toString());
            logger.debug("加载内嵌字体：" + absFontPath);
            trueTypeFont = FontLoader.getInstance().loadExternalFont(absFontPath);
        } else {
            logger.debug("加载系统字体：" + ctFont.getFamilyName() + " " + ctFont.getFontName());
            trueTypeFont = FontLoader.getInstance().loadSystemFont(ctFont.getFamilyName(), ctFont.getFontName());
        }
        // 更新缓存 即便 trueTypeFont 也设置，不存在字体时(null)重复加载问题。
        fontCache.put(key, trueTypeFont);
        return trueTypeFont;
    }

    private Matrix renderBoundaryAndSetClip(Graphics2D graphics, ST_Box boundary, Matrix parentMatrix) {
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(0.1f * ppm));
        Matrix m = MatrixUtils.base().mtimes(parentMatrix);


        graphics.setTransform(MatrixUtils.createAffineTransform(m));

        if (boundary != null) {

            Polygon shape = new Polygon();

            Tuple2<Double, Double> p00 = MatrixUtils.pointTransform(m, boundary.getTopLeftX(), boundary.getTopLeftY() - 1);
            Tuple2<Double, Double> p01 = MatrixUtils.pointTransform(m, boundary.getTopLeftX() + boundary.getWidth() + 2, boundary.getTopLeftY() - 1);
            Tuple2<Double, Double> p10 = MatrixUtils.pointTransform(m, boundary.getTopLeftX(), boundary.getTopLeftY() + boundary.getHeight() + 2);
            Tuple2<Double, Double> p11 = MatrixUtils.pointTransform(m, boundary.getTopLeftX() + boundary.getWidth() + 2, boundary.getTopLeftY() + boundary.getHeight() + 2);

            shape.addPoint(Double.valueOf(p00.getFirst() * ppm).intValue(), Double.valueOf(p00.getSecond() * ppm).intValue());
            shape.addPoint(Double.valueOf(p01.getFirst() * ppm).intValue(), Double.valueOf(p01.getSecond() * ppm).intValue());
            shape.addPoint(Double.valueOf(p11.getFirst() * ppm).intValue(), Double.valueOf(p11.getSecond() * ppm).intValue());
            shape.addPoint(Double.valueOf(p10.getFirst() * ppm).intValue(), Double.valueOf(p10.getSecond() * ppm).intValue());

            if (config.drawBoundary) {
                graphics.setClip(null);
                graphics.drawPolygon(shape);
            }
            graphics.setClip(shape);
        }

        m = MatrixUtils.scale(m, ppm, ppm);
        return m;
    }

    private Path2D buildPath(String abbreviatedData) {
        Path2D path = new Path2D.Double();
        path.moveTo(0, 0);
        String[] s = abbreviatedData.split("\\s+");
        int i = 0;
        while (i < s.length) {
            String operator = s[i];
            switch (operator) {
                case "S":
                case "M":
                    path.moveTo(Double.valueOf(s[i + 1]), Double.valueOf(s[i + 2]));
                    i += 3;
                    break;
                case "L":
                    path.lineTo(Double.valueOf(s[i + 1]), Double.valueOf(s[i + 2]));
                    i += 3;
                    break;
                case "Q":
                    path.quadTo(Double.valueOf(s[i + 1]), Double.valueOf(s[i + 2]), Double.valueOf(s[i + 3]),
                            Double.valueOf(s[i + 4]));
                    i += 5;
                    break;
                case "B":
                    path.curveTo(Double.valueOf(s[i + 1]), Double.valueOf(s[i + 2]), Double.valueOf(s[i + 3]),
                            Double.valueOf(s[i + 4]), Double.valueOf(s[i + 5]),
                            Double.valueOf(s[i + 6]));
                    i += 7;
                    break;
                case "A":
                    // path.append(new
                    // Arc2D.Double(Double.valueOf(s[i+1]),Double.valueOf(s[i+2]),Double.valueOf(s[i+3]),Double.valueOf(s[i+4]),Double.valueOf(s[i+5]),Double.valueOf(s[i+3]),-1),true);
                    i += 7;
                    break;
                case "C":
                    path.closePath();
                    i++;
                    break;
                default:
                    i++;
            }
        }
        return path;
    }

    private Double getLineWidth(CT_GraphicUnit graphicUnit, List<CT_DrawParam> drawParams) {
        Double lineWidth = graphicUnit.getLineWidth();
        if (lineWidth != null) return lineWidth;
        logger.debug("LineWidth 为空，使用默认值0.4毫米");
        return 0.4;
    }

    private Color getStrokeColor(CT_Color color, CT_Color defaultColor, List<CT_DrawParam> drawParams) {
        CT_Color c = color;
        if (c == null) {
            for (CT_DrawParam drawParam : drawParams) {
                c = drawParam.getStrokeColor();
                if (c != null)
                    break;
            }
        }
        if (c == null) {
            c = defaultColor;
        }
        return getColor(c);
    }

    private Color getFillColor(CT_Color color, CT_Color defaultColor, List<CT_DrawParam> drawParams) {
        CT_Color c = color;
        if (c == null) {
            for (CT_DrawParam drawParam : drawParams) {
                c = drawParam.getFillColor();
                if (c != null)
                    break;
            }
        }
        if (c == null) {
            c = defaultColor;
        }
        return getColor(c);
    }

    public Color getColor(CT_Color ctColor) {
        if (ctColor == null) return null;
        ST_Array array = ctColor.getValue();


        OFDColorSpaceType type = OFDColorSpaceType.RGB;
        ST_RefID refID = ctColor.getColorSpace();
        CT_ColorSpace ctColorSpace = null;
        if (refID != null) {
            resourceManage.getColorSpace(refID.toString());
        }
        if (ctColorSpace != null) {
            if (ctColorSpace.getType() != null) {
                type = ctColorSpace.getType();
            }
            if (array == null && ctColor.getIndex() != null) {
                array = ctColorSpace.getPalette().getColorByIndex(ctColor.getIndex());
            }

        }
        if (array == null) return null;
        int[] color = new int[array.size()];
        for (int i = 0; i < array.size(); i++) {
            String s = array.getArray().get(i);
            if (s.startsWith("#")) {
                color[i] = Integer.parseInt(s.replaceAll("#", ""), 16);
            } else {
                if ("0.0".equals(s))
                    color[i] = 0;
                else
                    color[i] = Integer.valueOf(s);
            }
        }
        switch (type) {
            case GRAY:
                return new Color(color[0], color[0], color[0]);
            case CMYK:
                int r = 255 * (100 - color[0]) * (100 - color[3]) / 10000;
                int g = 255 * (100 - color[1]) * (100 - color[3]) / 10000;
                int b = 255 * (100 - color[2]) * (100 - color[3]) / 10000;
                return new Color(r, g, b);
            case RGB:
            default:
                return new Color(color[0], color[1], color[2]);
        }
    }

    public static List<Double> parseDelta(ST_Array array) {
        if (array == null) return null;
        List<Double> arr = new ArrayList<>();

        int i = 0;
        int counter = 0;
        while (i < array.size()) {
            String current = array.getArray().get(i);
            if ("g".equals(current)) {
                Integer num = Integer.valueOf(array.getArray().get(i + 1));
                Double delta = Double.valueOf(array.getArray().get(i + 2));
                for (int j = 1; j <= num; j++) {
                    arr.add(delta);
                    counter++;
                }
                i += 3;
            } else {
                Double delta = Double.valueOf(current);
                arr.add(delta);
                counter++;
                i++;
            }

        }
        return arr;
    }

    public static class Config {
        /*
         * 印章透明度
         * */
        private float stampOpacity = 0.75f;
        /*
         * 是否清除印章背景色
         * */
        private boolean clearStampBackground = true;
        /*
         * 印章背景灰度阈值，高于此值被设置为透明
         * */
        private int stampBackgroundGray = 255;

        /*
         * 是否绘制元素边框 （调试使用）
         * */
        private boolean drawBoundary;

        public float getStampOpacity() {
            return stampOpacity;
        }

        public void setStampOpacity(float stampOpacity) {
            if (stampOpacity >= 0 && stampOpacity <= 1) {
                this.stampOpacity = stampOpacity;
            }
        }

        public boolean isDrawBoundary() {
            return drawBoundary;
        }

        public void setDrawBoundary(boolean drawBoundary) {
            this.drawBoundary = drawBoundary;
        }

        public boolean isClearStampBackground() {
            return clearStampBackground;
        }

        public void setClearStampBackground(boolean clearStampBackground) {
            this.clearStampBackground = clearStampBackground;
        }

        public int getStampBackgroundGray() {
            return stampBackgroundGray;
        }

        /**
         * 印章背景灰度阈值，高于此值被设置为透明
         *
         * @param stampBackgroundGray 灰度阈值
         */
        public void setStampBackgroundGray(int stampBackgroundGray) {
            if (stampBackgroundGray < 0 || stampBackgroundGray > 255) {
                stampBackgroundGray = 255;
            }
            this.stampBackgroundGray = stampBackgroundGray;
        }
    }
}
