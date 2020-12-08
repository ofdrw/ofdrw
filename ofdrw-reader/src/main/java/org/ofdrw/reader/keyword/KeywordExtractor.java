package org.ofdrw.reader.keyword;

import org.dom4j.DocumentException;
import org.ofdrw.core.Const;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.pageObj.CT_TemplatePage;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.Template;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicStructure.res.resources.Fonts;
import org.ofdrw.core.basicType.*;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.core.text.text.CT_Text;
import org.ofdrw.reader.DeltaTool;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceLocator;
import sun.font.FontDesignMetrics;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关键字抽取
 *
 * @author minghu-zhang
 * @since 16:25 2020/9/26
 */
@SuppressWarnings("unchecked")
public class KeywordExtractor {

    /**
     * 每毫米的point单位
     * 1 point / 2.83464567 ≈ 0.35277778 mm
     */
    private static final float POINT_PER_MM = 72 / 25.4f;

    /**
     * 获取关键字坐标列表(坐标单位毫米mm)
     *
     * @param reader  OFD解析器
     * @param keyword 关键字
     * @return 关键字坐标列表
     * @throws FileNotFoundException 文件不存在异常
     * @throws DocumentException     文档解析异常
     */
    public static List<KeywordPosition> getKeyWordPositionList(OFDReader reader, String keyword) throws FileNotFoundException, DocumentException {
        return getKeyWordPositionList(reader, keyword, null);
    }

    /**
     * 获取关键字坐标列表(坐标单位毫米mm)
     *
     * @param reader   OFD解析器
     * @param keywords 关键字列表
     * @return 关键字坐标列表
     * @throws FileNotFoundException 文件不存在异常
     * @throws DocumentException     文档解析异常
     */
    public static List<KeywordPosition> getKeyWordPositionList(OFDReader reader, String[] keywords) throws FileNotFoundException, DocumentException {
        return getKeyWordPositionList(reader, keywords, null);
    }

    /**
     * 获取关键字坐标列表(坐标单位毫米mm)
     *
     * @param reader  OFD解析器
     * @param keyword 关键字
     * @param pages   要检索的页码，从1开始，不超过最大页码
     * @return 关键字坐标列表
     * @throws FileNotFoundException 文件不存在异常
     * @throws DocumentException     文档解析异常
     */
    public static List<KeywordPosition> getKeyWordPositionList(OFDReader reader, String keyword, int[] pages) throws FileNotFoundException, DocumentException {
        return getKeyWordPositionList(reader, new String[]{keyword}, pages);
    }

    /**
     * 获取关键字坐标列表(坐标单位毫米mm)
     *
     * @param reader   OFD解析器
     * @param keywords 关键字列表
     * @param pages    要检索的页码，从1开始，不超过最大页码
     * @return 关键字坐标列表
     * @throws FileNotFoundException 文件不存在异常
     * @throws DocumentException     文档解析异常
     */
    public static List<KeywordPosition> getKeyWordPositionList(OFDReader reader, String[] keywords, int[] pages) throws FileNotFoundException, DocumentException {
        Map<TextCode, KeywordResource> boundaryMapping = new HashMap<>(8);

        ResourceLocator locator = reader.getResourceLocator();
        //创建OFD和Document对象
        OFD ofd = locator.get(Const.INDEX_FILE, OFD::new);
        String docFile = ofd.getDocBody().getDocRoot().getLoc();
        Document document = locator.get(docFile, Document::new);
        //数据目录
        String dataDir = docFile.split("/")[0];

        //获取字体映射对象
        Map<ST_ID, CT_Font> fontMapping = getFontMapping(locator, dataDir, document);

        //获取模板字典
        Map<ST_ID, Page> templatePage = getTemplatePage(locator, dataDir, document);

        //创建文字定位列表
        List<TextCode> textCodeList = new ArrayList<>();
        int numberOfPages = reader.getNumberOfPages();

        boolean hasPageLimit = pages != null && pages.length > 0;
        if (hasPageLimit) {
            for (int page : pages) {
                if (page < 1 || page > numberOfPages) {
                    throw new IllegalArgumentException(String.format("页码不正确，支持范围[%d-%d]", 1, numberOfPages));
                }
            }
        }
        for (int page = 1; page <= numberOfPages; page++) {
            if (hasPageLimit) {
                for (int i : pages) {
                    if (i == page) {
                        preparedContextData(reader, textCodeList, boundaryMapping, fontMapping, templatePage, page);
                        break;
                    }
                }
            } else {
                preparedContextData(reader, textCodeList, boundaryMapping, fontMapping, templatePage, page);
            }
        }

        List<KeywordPosition> positionList = new ArrayList<>();
        //处理文字定位
        for (int i = 0; i < textCodeList.size(); i++) {
            TextCode textCode = textCodeList.get(i);
            if (textCode != null) {
                String content = textCode.getContent();
                if (content != null && !"".equals(content.trim())) {
                    for (String keyword : keywords) {
                        int textIndex = content.indexOf(keyword);
                        if (textIndex != -1) {
                            //完整包含关键字
                            addNormalKeyword(keyword, boundaryMapping, positionList, textCode, textIndex);
                        } else if (keyword.indexOf(content) == 0 && i != textCodeList.size() - 1) {
                            //前缀匹配关键字
                            addPrefixBreakTextCodeList(keyword, boundaryMapping, textCodeList, positionList, i, textCode);
                        } else {
                            int startIndex = checkPostfixMatch(content, keyword);
                            //后缀匹配关键字
                            if (startIndex != -1) {
                                addPostfixBreakTextCodeList(keyword, boundaryMapping, textCodeList, positionList, i, startIndex, textCode);
                            }
                        }
                    }
                }
            }
        }

        return positionList;
    }


    /**
     * 检查后缀匹配
     *
     * @param content 待匹配文本
     * @param keyword 关键字
     * @return 是/否 匹配
     */
    private static int checkPostfixMatch(String content, String keyword) {
        int startIndex;
        boolean match = true;
        if ((startIndex = content.lastIndexOf(keyword.charAt(0))) != -1) {
            for (int j = startIndex, k = 0; j < content.length(); j++, k++) {
                if (content.charAt(j) != keyword.charAt(k)) {
                    match = false;
                    break;
                }
            }
        }
        return match ? startIndex : -1;
    }

    /**
     * 处理后缀匹配断字断行文本定位关键字
     *
     * @param keyword         关键字字符串
     * @param boundaryMapping 映射对象
     * @param textCodeList    文本定位列表
     * @param positionList    关键字位置列表
     * @param textCodeIndex   TextCode位置
     * @param startIndex      TextCode文本起始位置
     * @param textCode        第一个文字定位
     */
    private static void addPostfixBreakTextCodeList(String keyword, Map<TextCode, KeywordResource> boundaryMapping, List<TextCode> textCodeList,
                                                    List<KeywordPosition> positionList, int textCodeIndex, int startIndex, TextCode textCode) {
        //文字定位合并列表
        List<TextCode> mergeTextCodeList = new ArrayList<>();
        //加入合并列表
        mergeTextCodeList.add(textCode);
        //匹配下一个字
        searchNextText(keyword, textCodeList, mergeTextCodeList, boundaryMapping, textCodeIndex, textCode.getContent().substring(startIndex), textCode);
        //判断是否包含
        StringBuilder builder = new StringBuilder();
        for (TextCode code : mergeTextCodeList) {
            builder.append(code.getContent());
        }
        if (builder.indexOf(keyword) != -1) {
            mergeKeywordPosition(keyword, startIndex, positionList, mergeTextCodeList, boundaryMapping);
        }

    }

    /**
     * 处理前缀匹配断字断行文本定位关键字
     *
     * @param keyword         关键字字符串
     * @param boundaryMapping 映射对象
     * @param textCodeList    文本定位列表
     * @param positionList    关键字位置列表
     * @param textCodeIndex   定位起始位置
     * @param textCode        第一个文字定位
     */
    private static void addPrefixBreakTextCodeList(String keyword, Map<TextCode, KeywordResource> boundaryMapping, List<TextCode> textCodeList,
                                                   List<KeywordPosition> positionList, int textCodeIndex, TextCode textCode) {
        //文字定位合并列表
        List<TextCode> mergeTextCodeList = new ArrayList<>();
        //加入合并列表
        mergeTextCodeList.add(textCode);
        //匹配下一个字
        searchNextText(keyword, textCodeList, mergeTextCodeList, boundaryMapping, textCodeIndex, textCode.getContent(), textCode);
        //判断是否包含
        StringBuilder builder = new StringBuilder();
        for (TextCode code : mergeTextCodeList) {
            builder.append(code.getContent());
        }
        if (builder.indexOf(keyword) != -1) {
            mergeKeywordPosition(keyword, 0, positionList, mergeTextCodeList, boundaryMapping);
        }
    }


    /**
     * 检索下一个文本定位节点
     *
     * @param keyword           关键字字符串
     * @param textCodeList      文本定位列表
     * @param mergeTextCodeList 合并的TextCode列表
     * @param boundaryMapping   文本资源映射对象
     * @param textCodeIndex     TextCode位置
     * @param firstMatchString  最先匹配字符串
     * @param textCode          第一个匹配文字
     */
    private static void searchNextText(String keyword, List<TextCode> textCodeList, List<TextCode> mergeTextCodeList,
                                       Map<TextCode, KeywordResource> boundaryMapping, int textCodeIndex, String firstMatchString,
                                       TextCode textCode) {
        StringBuilder mergeText = new StringBuilder(firstMatchString);
        KeywordResource kr = boundaryMapping.get(textCode);
        if (kr != null) {
            int currentPage = kr.getPage();
            for (int j = textCodeIndex + 1; j < textCodeList.size(); j++) {
                TextCode next = textCodeList.get(j);
                if ("".equals(next.getContent().trim())) {
                    continue;
                }
                KeywordResource nextKr = boundaryMapping.get(next);
                if (nextKr != null) {
                    //不是同一页则不定位
                    if (currentPage != nextKr.getPage()) {
                        break;
                    }
                    //自然顺序检索
                    mergeText.append(next.getContent());
                    String mergeTextString = mergeText.toString();
                    if (mergeTextString.equals(keyword) || mergeTextString.startsWith(keyword)) {
                        mergeTextCodeList.add(next);
                        break;
                    }
                    if (keyword.startsWith(mergeTextString)) {
                        mergeTextCodeList.add(next);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    /**
     * 处理正常关键字
     *
     * @param keyword         关键字
     * @param boundaryMapping 映射对象
     * @param positionList    为转移列表
     * @param textCode        文字定位
     * @param textIndex       文本索引
     */
    private static void addNormalKeyword(String keyword, Map<TextCode, KeywordResource> boundaryMapping, List<KeywordPosition> positionList,
                                         TextCode textCode, int textIndex) {
        KeywordResource kr = boundaryMapping.get(textCode);
        if (kr != null) {
            CT_Text ctText = kr.getText();
            if (ctText.getBoundary() != null) {
                FontMetrics fontMetrics = FontDesignMetrics.getMetrics(getFont(ctText, kr.getFont()));

                List<Float> deltaX = DeltaTool.getDelta(textCode.getDeltaX(), textCode.getContent().length());
                List<Float> deltaY = DeltaTool.getDelta(textCode.getDeltaY(), textCode.getContent().length());

                KeywordPosition position;
                ST_Array ctm = ctText.getCTM();
                int keywordLength = keyword.length();
                if (ctm != null) {
                    position = getCtmKeywordPosition(textCode, textIndex, kr.getPage(), ctText, fontMetrics, ctm, deltaX, deltaY, keywordLength);
                } else {
                    position = getKeywordPosition(textCode, textIndex, kr.getPage(), ctText, fontMetrics, deltaX, deltaY, keywordLength);
                }

                position.setKeyword(keyword);
                positionList.add(position);
            }
        }
    }

    /**
     * 获取CTM后的关键字位置
     *
     * @param textCode      文字定位
     * @param textIndex     文本索引
     * @param page          文本资源
     * @param ctText        文字对象
     * @param fontMetrics   字体属性
     * @param ctm           CTM对象
     * @param deltaX        X偏移
     * @param deltaY        Y偏移
     * @param keywordLength 文本长度
     * @return 关键字位置
     */
    private static KeywordPosition getCtmKeywordPosition(TextCode textCode, int textIndex, int page, CT_Text ctText, FontMetrics fontMetrics,
                                                         ST_Array ctm, List<Float> deltaX, List<Float> deltaY, int keywordLength) {
        double height = (fontMetrics.getAscent() - fontMetrics.getDescent()) / POINT_PER_MM;

        double[] matrix = getMatrix(ctm);
        double x = textCode.getX();
        double y = textCode.getY();

        for (int i = 0; i < textIndex; i++) {
            if (deltaX.size() > i) {
                x += deltaX.get(i);
            }
            if (deltaY.size() > i) {
                y += deltaY.get(i);
            }
        }

        double stringWidth = getStringWidth(textIndex, keywordLength, deltaX, ctText.getSize());

        ST_Pos leftTop = transform(matrix, x, y - height);
        ST_Pos leftBottom = transform(matrix, x, y);
        ST_Pos rightTop = transform(matrix, x + stringWidth, y - height);
        ST_Pos rightBottom = transform(matrix, x + stringWidth, y);

        ST_Box ctmBox = mergePos(leftTop, leftBottom, rightTop, rightBottom);

        ctmBox.setTopLeftX(ctmBox.getTopLeftX() + ctText.getBoundary().getTopLeftX());
        ctmBox.setTopLeftY(ctmBox.getTopLeftY() + ctText.getBoundary().getTopLeftY());

        return new KeywordPosition(page, ctmBox);
    }

    /**
     * 合并坐标
     *
     * @param posList 坐标列表
     * @return 矩形框
     */
    private static ST_Box mergePos(ST_Pos... posList) {
        double topLeftX = 0, topLeftY = 0, bottomRightX = 0, bottomRightY = 0;

        for (int i = 0; i < posList.length; i++) {
            ST_Pos pos = posList[i];
            if (i == 0) {
                topLeftX = bottomRightX = pos.getX();
                topLeftY = bottomRightY = pos.getY();
            }
            if (topLeftX > pos.getX()) {
                topLeftX = pos.getX();
            }
            if (topLeftY > pos.getY()) {
                topLeftY = pos.getY();
            }
            if (bottomRightX < pos.getX()) {
                bottomRightX = pos.getX();
            }
            if (bottomRightY < pos.getY()) {
                bottomRightY = pos.getY();
            }
        }
        return new ST_Box(topLeftX, topLeftY, bottomRightX - topLeftX, bottomRightY - topLeftY);
    }

    /**
     * 坐标转换
     *
     * @param matrix 矩阵数组
     * @param sx     原始X
     * @param sy     原始Y
     * @return 计算后位置
     */
    private static ST_Pos transform(double[] matrix, double sx, double sy) {
        double x = matrix[0] * sx + matrix[2] * sy + matrix[4];
        double y = matrix[1] * sx + matrix[3] * sy + matrix[5];
        return new ST_Pos(x, y);
    }

    /**
     * 获取Matrix数据
     *
     * @param ctm ctm对象
     * @return 矩阵对象
     */
    private static double[] getMatrix(ST_Array ctm) {
        List<String> ctmArray = ctm.getArray();
        double[] matrix = new double[ctmArray.size()];
        for (int i = 0; i < ctmArray.size(); i++) {
            matrix[i] = Double.valueOf(ctmArray.get(i));
        }
        return matrix;
    }

    /**
     * 合并关键字位置对象
     *
     * @param keyword         关键字
     * @param firstStartIndex 第一个关键字起始匹配位置
     * @param positionList    检索到的关键字列表
     * @param textCodeList    合并列表
     * @param boundaryMapping 外接矩形映射
     */
    private static void mergeKeywordPosition(String keyword, int firstStartIndex, List<KeywordPosition> positionList, List<TextCode> textCodeList,
                                             Map<TextCode, KeywordResource> boundaryMapping) {
        List<ST_Box> boxList = new ArrayList<>();
        FontMetrics fontMetrics = null;
        int page = 0, totalLength = 0, keywordLength = keyword.length(), fontSize = 0;

        for (int i = 0; i < textCodeList.size(); i++) {
            TextCode textCode = textCodeList.get(i);
            int textLength = textCode.getContent().length();
            KeywordResource kr = boundaryMapping.get(textCode);
            if (kr != null) {
                CT_Text ctText = kr.getText();
                if (page == 0) {
                    page = kr.getPage();
                }
                if (ctText != null && ctText.getBoundary() != null) {
                    if (i == 0 && firstStartIndex > 0) {
                        textLength = totalLength = textCode.getContent().length() - firstStartIndex;
                    } else {
                        if ((totalLength + textLength) > keywordLength) {
                            textLength = keywordLength - totalLength;
                        } else {
                            totalLength += textCode.getContent().length();
                        }
                    }

                    List<Float> deltaX = DeltaTool.getDelta(textCode.getDeltaX(), textCode.getContent().length());
                    List<Float> deltaY = DeltaTool.getDelta(textCode.getDeltaY(), textCode.getContent().length());

                    double width;
                    if (i == 0 && firstStartIndex > 0) {
                        width = getStringWidth(firstStartIndex, textLength, deltaX, ctText.getSize());
                    } else {
                        width = getStringWidth(0, textLength, deltaX, ctText.getSize());
                    }

                    if (width == 0) {
                        width = kr.getText().getSize();
                    }

                    int size = ctText.getSize().intValue();
                    if (fontMetrics == null || fontSize != size) {
                        fontMetrics = FontDesignMetrics.getMetrics(getFont(ctText, kr.getFont()));
                        fontSize = size;
                    }
                    double height = (fontMetrics.getAscent() - fontMetrics.getDescent()) / POINT_PER_MM;
                    ST_Pos basePoint;
                    ST_Array ctm = ctText.getCTM();
                    if (ctm != null) {
                        double[] matrix = getMatrix(ctm);
                        double x = textCode.getX() == null ? 0 : textCode.getX();
                        double y = textCode.getY() == null ? 0 : textCode.getY();
                        if (i == 0 && firstStartIndex > 0 && deltaX.size() > 0) {
                            for (int j = 0; j < firstStartIndex; j++) {
                                x += deltaX.get(j);
                            }
                        }
                        if (i == 0 && firstStartIndex > 0 && deltaY.size() > 0) {
                            for (int j = 0; j < firstStartIndex; j++) {
                                y += deltaY.get(j);
                            }
                        }
                        ST_Pos leftBottom = transform(matrix, x, y);
                        ST_Pos rightTop = transform(matrix, x + width, y - height);

                        ST_Pos position = ctText.getBoundary().getTopLeftPos();

                        ST_Box box = mergePos(leftBottom, rightTop);
                        box.setTopLeftX(position.getX() + box.getTopLeftX());
                        box.setTopLeftY(position.getY() + box.getTopLeftY());

                        boxList.add(box);
                    } else {
                        basePoint = getLeftBottomPos(ctText.getBoundary(), textCode, deltaX, deltaY, 0);
                        if (i == 0 && firstStartIndex > 0 && deltaX.size() > 0) {
                            for (int j = 0; j < firstStartIndex; j++) {
                                basePoint.setX(basePoint.getX() + deltaX.get(j));
                            }
                        }
                        if (i == 0 && firstStartIndex > 0 && deltaY.size() > 0) {
                            for (int j = 0; j < firstStartIndex; j++) {
                                basePoint.setY(basePoint.getY() + deltaY.get(j));
                            }
                        }
                        boxList.add(new ST_Box(basePoint.getX(), basePoint.getY() - height, width, height));
                    }
                }
            }
        }
        if (boxList.size() > 0) {
            positionList.add(new KeywordPosition(page, mergeBox(boxList)));
        }
    }

    /**
     * 合并Box
     *
     * @param boxList 盒子列表
     */
    private static ST_Box mergeBox(List<ST_Box> boxList) {
        double topLeftX = 0, topLeftY = 0, bottomRightX = 0, bottomRightY = 0;

        for (int i = 0; i < boxList.size(); i++) {
            ST_Box box = boxList.get(i);
            if (i == 0) {
                topLeftX = box.getTopLeftX();
                topLeftY = box.getTopLeftY();
            } else {
                if (box.getTopLeftX() < topLeftX) {
                    topLeftX = box.getTopLeftX();
                }
                if (box.getTopLeftY() < topLeftY) {
                    topLeftY = box.getTopLeftY();
                }
            }
            if ((box.getTopLeftX() + box.getWidth()) > bottomRightX) {
                bottomRightX = box.getTopLeftX() + box.getWidth();
            }
            if ((box.getTopLeftY() + box.getHeight()) > bottomRightY) {
                bottomRightY = box.getTopLeftY() + box.getHeight();
            }
        }

        return new ST_Box(topLeftX, topLeftY, bottomRightX - topLeftX, bottomRightY - topLeftY);
    }

    /**
     * 获取获取模板字典
     *
     * @param locator  资源定位器
     * @param dataDir  Document File路径
     * @param document 文档对象
     * @throws FileNotFoundException 文件不存在异常
     * @throws DocumentException     文档解析异常
     */
    private static Map<ST_ID, Page> getTemplatePage(ResourceLocator locator, String dataDir, Document document)
            throws FileNotFoundException, DocumentException {
        Map<ST_ID, Page> templatePage = new HashMap<>(8);

        //创建模板字体对象
        for (CT_TemplatePage tp : document.getCommonData().getTemplatePages()) {
            String templateFile = dataDir + "/" + tp.getBaseLoc();
            Page page = locator.get(templateFile, Page::new);
            templatePage.put(tp.getID(), page);
        }

        return templatePage;
    }

    /**
     * 创建关键字位置对象
     *
     * @param textCode      文字定位对象
     * @param textIndex     文本索引
     * @param page          页码
     * @param ctText        文字对象
     * @param fontMetrics   字体属性
     * @param deltaX        X偏移
     * @param deltaY        Y偏移
     * @param keywordLength 文本长度
     * @return 关键字对象
     */
    private static KeywordPosition getKeywordPosition(TextCode textCode, int textIndex, int page, CT_Text ctText, FontMetrics fontMetrics,
                                                      List<Float> deltaX, List<Float> deltaY, int keywordLength) {
        double width = getStringWidth(textIndex, keywordLength, deltaX, ctText.getSize());
        double height = (fontMetrics.getAscent() - fontMetrics.getDescent()) / POINT_PER_MM;

        if (!deltaY.isEmpty()) {
            for (int i = 0; i < deltaY.size() && i < keywordLength - 1; i++) {
                height -= deltaY.get(i);
            }
        }

        ST_Pos basePoint = getLeftBottomPos(ctText.getBoundary(), textCode, deltaX, deltaY, textIndex);
        ST_Box box = new ST_Box(basePoint.getX(), basePoint.getY() - height, width, height);

        return new KeywordPosition(page, box);
    }

    /**
     * 获取文本宽度，单位毫米(mm)
     *
     * @param textIndex     文字索引
     * @param keywordLength 文本长度
     * @param deltaX        X偏移量
     * @param fontSize      文字字号
     * @return 文本宽度
     */
    private static double getStringWidth(int textIndex, int keywordLength, List<Float> deltaX, Double fontSize) {
        double width = fontSize;
        for (int i = textIndex; i < textIndex + keywordLength - 1 && i < deltaX.size(); i++) {
            width += deltaX.get(i);
        }
        return width;
    }

    /**
     * 获取字体
     *
     * @param ctText 文字对象
     * @param ctFont 字形对象
     * @return 字体对象
     */
    private static Font getFont(CT_Text ctText, CT_Font ctFont) {
        if (ctFont == null) {
            ctFont = new CT_Font("黑体");
        }

        Font font;
        int fontSize = (int) (ctText.getSize() * POINT_PER_MM);
        if (ctText.getItalic()) {
            font = new Font(ctFont.getFontName(), Font.ITALIC, fontSize);
        } else {
            font = new Font(ctFont.getFontName(), Font.PLAIN, fontSize);
        }

        Map attributes = font.getAttributes();
        if (ctText.getHScale() != null) {
            attributes.put(TextAttribute.WIDTH, ctText.getHScale());
        }

        if (ctText.getWeight() != null) {
            attributes.put(TextAttribute.WEIGHT, ctText.getWeight().getWeight() / 100);
        }

        return Font.getFont(attributes);
    }

    /**
     * 获取左下角位置
     *
     * @param boundary  矩形框
     * @param textCode  文字定位
     * @param deltaX    X偏移
     * @param deltaY    Y偏移
     * @param textIndex 文字索引
     * @return 左下角坐标
     */
    private static ST_Pos getLeftBottomPos(ST_Box boundary, TextCode textCode, List<Float> deltaX, List<Float> deltaY, int textIndex) {
        ST_Pos position = boundary.getTopLeftPos();

        double positionX = position.getX() == null ? 0 : position.getX();
        double positionY = position.getY() == null ? 0 : position.getY();

        double x = (textCode.getX() == null ? 0 : textCode.getX()) + positionX;
        double y = (textCode.getY() == null ? 0 : textCode.getY()) + positionY;

        for (int i = 0; i < textIndex; i++) {
            if (deltaX.size() > i) {
                x += deltaX.get(i);
            }
            if (deltaY.size() > i) {
                y += deltaY.get(i);
            }
        }

        return ST_Pos.getInstance(x, y);
    }

    /**
     * 预处理数据
     *
     * @param reader          OFD解析器
     * @param textCodeList    文本列表
     * @param boundaryMapping 外接矩形映射
     * @param fontMapping     字体映射对象
     * @param templatePageMap 模板数据
     * @param pageNumber      页码
     */
    private static void preparedContextData(OFDReader reader, List<TextCode> textCodeList, Map<TextCode, KeywordResource> boundaryMapping,
                                            Map<ST_ID, CT_Font> fontMapping, Map<ST_ID, Page> templatePageMap, int pageNumber) {
        Page page = reader.getPage(pageNumber);
        // 获取模板页正文层
        List<CT_Layer> layers = page.getContent().getLayers();

        for (Template tpl : page.getTemplates()) {
            //获取模板页
            Page templatePage = null;
            if (tpl != null) {
                ST_ID templateId = tpl.getTemplateID().getRefId();
                if (templatePageMap.containsKey(templateId)) {
                    templatePage = templatePageMap.get(templateId);
                }
            }

            //添加模板层
            if (templatePage != null) {
                layers.addAll(templatePage.getContent().getLayers());
            }
        }
        //创建字型映射关系
        for (CT_Layer layer : layers) {
            pageBlockHandle(textCodeList, boundaryMapping, fontMapping, pageNumber, layer.getPageBlocks());
        }
    }

    /**
     * 页面块处理
     *
     * @param textCodeList    文本列表
     * @param boundaryMapping 外接矩形映射
     * @param fontMapping     字体映射对象
     * @param pageNumber      页码
     * @param pageBlocks      页块列表
     */
    private static void pageBlockHandle(List<TextCode> textCodeList, Map<TextCode, KeywordResource> boundaryMapping, Map<ST_ID, CT_Font> fontMapping,
                                        int pageNumber, List<PageBlockType> pageBlocks) {
        for (PageBlockType block : pageBlocks) {
            if (block instanceof TextObject) {
                TextObject text = (TextObject) block;
                CT_Font font = fontMapping.get(text.getFont().getRefId());
                KeywordResource kr;

                for (TextCode code : text.getTextCodes()) {
                    kr = new KeywordResource();
                    kr.setPage(pageNumber);
                    kr.setFont(font);
                    kr.setText(text);

                    textCodeList.add(code);
                    boundaryMapping.put(code, kr);
                }
            } else if (block instanceof CT_PageBlock) {
                CT_PageBlock ctPageBlock = (CT_PageBlock) block;
                pageBlockHandle(textCodeList, boundaryMapping, fontMapping, pageNumber, ctPageBlock.getPageBlocks());
            }
        }
    }

    /**
     * 获取字体映射对象
     *
     * @param locator  资源定位器
     * @param dataDir  Document File路径
     * @param document 文档对象
     * @throws FileNotFoundException 文件不存在异常
     * @throws DocumentException     文档解析异常
     */
    private static Map<ST_ID, CT_Font> getFontMapping(ResourceLocator locator, String dataDir, Document document)
            throws FileNotFoundException, DocumentException {
        Map<ST_ID, CT_Font> fontMapping = new HashMap<>(8);
        //资源位置
        ST_Loc publicRes = document.getCommonData().getPublicRes();
        if (publicRes != null) {
            String resFile = dataDir + "/" + publicRes.getLoc();
            Res res = locator.get(resFile, Res::new);
            List<Fonts> fontsList = res.getFonts();
            for (Fonts font : fontsList) {
                List<CT_Font> ctFontList = font.getFonts();
                for (CT_Font ctFont : ctFontList) {
                    fontMapping.put(ctFont.getID(), ctFont);
                }
            }
        }
        return fontMapping;
    }

}
