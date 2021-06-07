package org.ofdrw.converter;


import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.ofdrw.converter.html.Element;
import org.ofdrw.converter.utils.CommonUtil;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.text.Weight;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;
import org.ofdrw.reader.model.AnnotionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.ofdrw.converter.AWTMaker.parseDelta;

/**
 * ofd转换为html
 *
 * @author yuanfang
 * @since 2021 2021-6-7 9:35
 */
public class HtmlMaker {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static Document doc;

    private final OFDReader ofdReader;

    private int screenWidth = 1000;

    private String outputFile;

    private float scale;


    /**
     * 转HTML构造方法
     *
     * @param ofdReader   OFD输入文件
     * @param outputFile  HTML输出文件路径
     * @param screenWidth 页面宽度，或者屏幕宽度
     */
    public HtmlMaker(OFDReader ofdReader, String outputFile, int screenWidth) {
        this.ofdReader = ofdReader;
        this.outputFile = outputFile;
        this.screenWidth = screenWidth;
    }

    /**
     * 将字符串写入文件
     *
     * @param filepath 文件路径
     * @param content  文件内容
     * @throws IOException IO异常
     */
    public void writeToFile(String filepath, String content) throws IOException {

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filepath))) {
            bufferedWriter.write(content);
            bufferedWriter.flush();
        }

    }

    /**
     * 开始转换
     */
    public void parse() {

        long start;
        long end;
        int pageNum = 1;

        List<Element> pageDivs = new ArrayList<>();

        double paperWidth = 210;
        double paperPixels = screenWidth;

        List<ST_Box> boxs = new ArrayList<>();
        for (PageInfo pageInfo : ofdReader.getPageList()) {
            start = System.currentTimeMillis();

            ST_Box pageBox = pageInfo.getSize();
            paperWidth = pageBox.getWidth();

            scale = (float) Math.round(((screenWidth - 10) / pageBox.getWidth()) * 10) / 10;

            int pageWidthPixel = (int) converterDpi(pageBox.getWidth());
            int pageHeightPixel = (int) converterDpi(pageBox.getHeight());
            paperPixels = pageWidthPixel;

            ST_Box st_box = new ST_Box(0, 0, pageWidthPixel, pageHeightPixel);

            boxs.add(st_box);

            Element pageDiv = new Element();
            pageDiv.setTagName("div");
            pageDiv.setAttribute(
                "style",
                String.format(
                    "margin-bottom: 20px;position: relative;width:%dpx;height:%dpx;;",
                    pageWidthPixel, pageHeightPixel
                )
            );

            List<Element> elements = makePage(pageInfo);

            for (Element ele : elements) {
                pageDiv.appendChild(ele);
            }

            pageDivs.add(pageDiv);
            end = System.currentTimeMillis();
            logger.info(String.format("page %d speed time %d", pageNum++, end - start));
        }

        double ppm = paperPixels / paperWidth;

        SVGMaker svgMaker = new SVGMaker(ofdReader, ppm);
        svgMaker.config.setDrawBoundary(false);
        svgMaker.config.setClip(false);

        List<String> svgs = new ArrayList<>();
        for (int i = 0; i < svgMaker.pageSize(); i++) {
            String svg = svgMaker.makePage(i);
            svgs.add(svg);
        }

        String ofdHtml = displayOfdDiv(pageDivs, svgs, boxs);

        try {
            writeToFile(outputFile, ofdHtml);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ofd每页的object画到pdf
     *
     * @param pageInfo 页面信息
     * @return PDF页面，只含文字节点
     */
    private List<Element> makePage(PageInfo pageInfo) {

        final List<AnnotionEntity> annotationEntities = ofdReader.getAnnotationEntities();

        // 获取页面内容出现的所有图层，包含模板页（所有页面均按照定义ZOrder排列）
        List<CT_Layer> layerList = pageInfo.getAllLayer();

        // 绘制 模板层 和 页面内容层
        List<Element> elements = renderLayer(layerList);

        // 绘制注释
        List<Element> elements2 = writeAnnoAppearance(pageInfo, annotationEntities);

        elements.addAll(elements2);
        return elements;

    }

    public String displayOfdDiv(List<Element> pageDivs, List<String> svgs, List<ST_Box> boxs) {

        StringBuilder sb = new StringBuilder();

        String svgss = "";
        for (int i = 0; i < svgs.size(); i++) {
            svgss = svgss +
                "<div style=\"margin:0 auto;margin-top: 20px;text-align:center;background:white;" +
                "height:" + boxs.get(i).getHeight() + "px;" +
                "width:" + boxs.get(i).getWidth() + "px;" +
                "\" class=\"svgItem\">" + svgs.get(i) + "</div>";
        }


        String title = "文件预览";
        String htmlWrapper = "";
        String prefix = " <!DOCTYPE html>\n" +
            "                <html lang=\"en\">\n" +
            "                  <head>\n" +
            "                    <meta charset=\"utf-8\">\n" +
            "                    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
            "                    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">";
        String titleElement = "<title>" + title + "</title>";
        String ele1 = "</head><body>";

        String svgWrapper = "<div class=\"svgWrapper\"> " + svgss + "</div>";

        String main = "  <div class=\"main-section\"id=\"content\" ref=\"contentDiv\" @mousewheel=\"scrool\">";

        String ele2 = "</div></body></html>";
        String style = "<style>" +
            "            .svgWrapper {" +
            "                margin: 0 auto;" +
            "                left: 0;" +
            "                right: 0;" +
            "                text-align: center;" +
            "                position: absolute;" +
            "                z-index: 1" +
            "            }" +
            "            html," +
            "            body {" +
            "                margin: 0;" +
            "                background: #808080;" +
            "                height: 100%" +
            "            }" +
            "            .main-section {" +
            "                padding-top: 20px;" +
            "                display: flex;" +
            "                flex-direction: column;" +
            "                align-items: center;" +
            "                justify-content: center;" +
            "                background: #808080;" +
            "                overflow: hidden;" +
            "                position: relative;" +
            "            }"+
            "          </style>";
        String js = "";
//                """
//                <script>
//                window.onload = function(){
//
//                 var annots =  document.getElementsByClassName("ofd_annotation");
//                 console.log(annots)
//                 for(var i=0;i<annots.length;i++){
//                      annots[i].onclick = function() { console.log(this) }
//                 }
//
//                }
//                </script>
//                """;

        StringBuilder divs = new StringBuilder();
        for (Element div : pageDivs) {

            org.dom4j.Element element = ofdElementToXmlNode(div, null);

            divs.append(element.asXML());
//            System.out.println(element.asXML());
        }

        return sb.
            append(prefix).
            append(titleElement).
            append(ele1).
            append(svgWrapper).
            append(main).
            append(divs).
            append(ele2).
            append(style).
            append(js)
            .toString();
    }

    private List<Element> renderLayer(List<CT_Layer> layerList) {

        List<Element> pageDivs = new ArrayList<>();

        for (CT_Layer layer : layerList) {

            Element pageDiv = new Element();
            pageDiv.setTagName("div");

            List<Element> pageElements = writePageBlock(layer.getPageBlocks(), null);

            for (Element ele : pageElements) {
                pageDiv.appendChild(ele);
            }

            pageDivs.add(pageDiv);

        }

        return pageDivs;
    }


    private List<Element> writePageBlock(List<PageBlockType> layerList, ST_Box annotBox) {
        List<Element> pageElements = new ArrayList<>();
        for (PageBlockType block : layerList) {

            if (block instanceof TextObject) {
                TextObject textObject = (TextObject) block;
                if (annotBox != null) {
                    ST_Box boundary = textObject.getBoundary();
                    boundary.setTopLeftX(boundary.getTopLeftX() + annotBox.getTopLeftX());
                    boundary.setTopLeftY(boundary.getTopLeftY() + annotBox.getTopLeftY());
                    textObject.setBoundary(boundary);
                }

                Element element = renderText(textObject);

                pageElements.addAll(new ArrayList<>(Collections.singletonList(element)));

            }
        }
        return pageElements;
    }


    private List<Element> writeAnnoAppearance(PageInfo pageInfo, List<AnnotionEntity> annotionEntities) {
        String pageId = pageInfo.getId().toString();
        List<Element> pageElements = new ArrayList<>();
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
                List<Element> elements = writePageBlock(pageBlockTypeList, annotBox);
                pageElements.addAll(elements);
            }
        }
        return pageElements;
    }


    private Element renderText(TextObject textObject) {

        ST_Box boundary = textObject.getBoundary();

        Double hScale = textObject.getHScale();

        Weight fontWeight = textObject.getWeight();

        int fontSize = (int) converterDpi(textObject.getSize());

        Double[] ctm = null;

        if (textObject.getCTM() != null) {
            ctm = textObject.getCTM().toDouble();
            if (ctm[0].equals(ctm[3])) {
                fontSize = (int) (ctm[0] * fontSize);
            }
        }

        Element svg = new Element();
        svg.setTagName("svg");
        svg.setNamespaceURI("http://www.w3.org/2000/svg");

        svg.setAttribute("version", "1.1");

        List<TextCode> textCodes = textObject.getTextCodes();

        List<TextCodePoint> textCodePointList = calTextPoint(textCodes.get(0));

        for (TextCodePoint textCodePoint : textCodePointList) {

            if (textCodePoint == null || textCodePoint.x == -1)
                continue;

//            System.out.println(textCodePoint.getText());
            Element text = new Element();
            text.setTagName("text");
            text.setX((textCodePoint.getX()));
            text.setY((textCodePoint.getY()));
            text.setInnerHTML(textCodePoint.getText());

            if (ctm != null) {
                if (ctm[0].equals(ctm[3])) {
                    text.setX((textCodePoint.getX() * ctm[0]));
                    text.setY((textCodePoint.getY() * ctm[0]));
                } else {
                    text.setAttribute(
                        "transform",
                        String.format(
                            "matrix(%s %s %s %s %.2f %.2f)",
                            ctm[0], ctm[1], ctm[2], ctm[3], converterDpi(ctm[4]), converterDpi(ctm[5])
                        )
                    );
                }
            } else {

            }
            if (hScale != 1) {
//                hScale = textCodePoint.getX() / textObject.getSize();
//                text.setAttribute(
//                    "transform",
//                    String.format(
//                        "matrix(%.1f,0,0,1, %.1f, 0)",
//                        hScale, (1 - hScale) * textCodePoint.getX()
//                    )
//                );
            }

            text.setAttribute("fill", "transparent");

            text.setAttribute(
                "style",
                String.format(
                    "font-weight: %d;font-size:%dpx;font-family: %s;%s",
                    fontWeight.getWeight(), fontSize, "simSum", ""
                )
            );
            svg.appendChild(text);
        }


        svg.setAttribute("style",
            String.format(
                "overflow:visible;" +
                    "position:absolute;" +
                    "width:%.1fpx;" +
                    "height:%.1fpx;" +
                    "left:%.1fpx;" +
                    "top:%.1fpx;" +
                    "z-index:%s",
                converterDpi(boundary.getWidth()),
                converterDpi(boundary.getHeight()),
                converterDpi(boundary.getTopLeftX()),
                converterDpi(boundary.getTopLeftY()),
                textObject.getID()
            )
        );
        return svg;
    }

    private float toFixed2(double num) {
        return (float) Math.round(num * 10) / 10;
    }

    private double converterDpi(double num) {
        return CommonUtil.millimetersToPixel(num, scale * 25.4f);
    }


    private static org.dom4j.Element ofdElementToXmlNode(Element element, org.dom4j.Element root) {

        if (doc == null || root == null) {
            doc = DocumentHelper.createDocument();
        }

        // 添加根节点
        if (root == null) {
            root = doc.addElement(element.getTagName());
        } else {
            root = root.addElement(element.getTagName());
        }

        Map<String, String> attribute = element.getAttribute();
        if (attribute != null) {
            for (String name : attribute.keySet()) {
                root.addAttribute(name, attribute.get(name));
            }
        }

        if (element.getInnerHTML() != null) {
            root.setText(element.getInnerHTML());
        }

        List<Element> children = element.getChildren();
        if (children != null) {
            for (Element child : children) {
                ofdElementToXmlNode(child, root);
            }
        }

        return root;
    }


    private List<TextCodePoint> calTextPoint(TextCode textCode) {
        double x = 0;
        double y = 0;

        List<TextCodePoint> textCodePointList = new ArrayList<>();
        if (textCode == null) {
            return textCodePointList;
        }
        String textStr = textCode.getText();
        if (textStr == null) {
            return textCodePointList;
        }
        x = textCode.getX();
        y = textCode.getY();


        List<Double> deltaXList = new ArrayList<>();
        List<Double> deltaYList = new ArrayList<>();

        if (textCode.getDeltaX() != null && textCode.getDeltaX().getArray() != null) {
            deltaXList = parseDelta(textCode.getDeltaX());
        }

        if (textCode.getDeltaY() != null && textCode.getDeltaY().getArray() != null) {
            deltaYList = parseDelta(textCode.getDeltaY());
        }


        for (int i = 0; i < textStr.length(); i++) {

            if (i > 0 && deltaXList.size() > 0) {
                x += deltaXList.get(i - 1);
            }
            if (i > 0 && deltaYList.size() > 0) {
                y += deltaYList.get(i - 1);
            }
            String text = textStr.substring(i, i + 1);

            TextCodePoint point = new TextCodePoint(
                converterDpi(x),
                converterDpi(y),
                text,
                deltaYList.size() > 0 && deltaXList.size() == 0
            );

            textCodePointList.add(point);
        }

        return textCodePointList;
    }

    //使用UnicodeBlock方法判断
    public static boolean isChineseByBlock(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D
            || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT) {
            return true;
        } else {
            return false;
        }
    }

    static class TextCodePoint {
        double x;
        double y;
        String text;
        boolean isVerticalDirection;

        public TextCodePoint() {
        }

        public TextCodePoint(double x, double y, String text, boolean isVerticalDirection) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.isVerticalDirection = isVerticalDirection;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean isVerticalDirection() {
            return isVerticalDirection;
        }

        public void setVerticalDirection(boolean verticalDirection) {
            isVerticalDirection = verticalDirection;
        }

    }

}
