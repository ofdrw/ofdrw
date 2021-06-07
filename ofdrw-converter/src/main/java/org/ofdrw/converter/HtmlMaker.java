package org.ofdrw.converter;


import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.ofdrw.converter.html.Element;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CompositeObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.text.Weight;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;
import org.ofdrw.reader.ResourceManage;
import org.ofdrw.reader.model.AnnotionEntity;
import org.ofdrw.reader.model.StampAnnotEntity;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.ofdrw.converter.AWTMaker.parseDelta;

/**
 * @author yuanfang
 * @since 2021 2021-6-7 9:35
 */
public class HtmlMaker {

    private final OFDReader ofdReader;

    private final ResourceManage resMgt;

    private int ppm = 15;

    private int screenWidth = 1000;

    float scale;

    Path inputFile;

    public HtmlMaker(OFDReader ofdReader, Path inputFile) {
        this.ofdReader = ofdReader;
        this.inputFile = inputFile;
        this.resMgt = ofdReader.getResMgt();
    }

    public void writeToFile(String filepath, String content) throws IOException {

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filepath))) {
            bufferedWriter.write(content);
            bufferedWriter.flush();
        }

    }

    public static Document doc;

    public static org.dom4j.Element ofdElementToXmlNode(Element element, org.dom4j.Element root) {

        if (doc == null || root == null)
            doc = DocumentHelper.createDocument();

        // 添加根节点
        if (root == null)
            root = doc.addElement(element.getTagName());
        else
            root = root.addElement(element.getTagName());

//        System.out.println("=====");
//        System.out.println(element.getTagName());

//
//            if (((Element) root).getVersion() != null) {
//                root.addAttribute("version", ((ElementNS) root).getVersion());
//            }


        Map<String, String> attribute = element.getAttribute();
        if (attribute != null) {
            for (String name : attribute.keySet()) {
                root.addAttribute(name, attribute.get(name));
//                System.out.print(name + "=" + attribute.get(name));
            }
        }

        if (element.getInnerHTML() != null) {
            root.setText(element.getInnerHTML());
//            System.out.println("\ntext=" + element.getInnerHTML());
        }

        List<Element> children = element.getChildren();
        if (children != null) {
//            System.out.println("hasChild=" + children.size());

            for (Element child : children) {

                ofdElementToXmlNode(child, root);
//            Element childNode = root.addElement(child.getTagName());
//
//            Map<String, String> childAttribute = child.getAttribute();
//            if(childAttribute!=null) {
//                for (String name : childAttribute.keySet()) {
//                    childNode.addAttribute(name,childAttribute.get(name));
//                }
//            }
//            if(child.getInnerHTML()!=null)
//            childNode.setText(child.getInnerHTML());

            }
        }

        return root;


    }

    /**
     * ofd每页的object画到pdf
     *
     * @param pageInfo 页面信息
     * @return PDF页面
     */
    public List<Element> makePage(PageInfo pageInfo) {

        final List<AnnotionEntity> annotationEntities = ofdReader.getAnnotationEntities();
        final List<StampAnnotEntity> stampAnnots = ofdReader.getStampAnnots();

        // 获取页面内容出现的所有图层，包含模板页（所有页面均按照定义ZOrder排列）
        List<CT_Layer> layerList = pageInfo.getAllLayer();


//        Div pageDiv = new Div();
//
//        ST_Box pageBox = pageInfo.getSize();
//
//        int pageWidthPixel = (int) Math.round(ppm * pageBox.getWidth());
//        int pageHeightPixel = (int) Math.round(ppm * pageBox.getHeight());
//
//        pageDiv.setPosition(Position.Relative);
//        pageDiv.setWidth((double) pageWidthPixel);
//        pageDiv.setHeight((double) pageHeightPixel);
//        pageDiv.setBackgroundColor(255, 255, 255);
//        pageDiv.setAttribute("style", String.format("margin-bottom: 20px;position: relative;width:%.2fpx;height:%.2fpx;background: white;", pageBox.getW(), pageBox.getH()));


        List<Element> elements = renderLayer(layerList);


        List<Element> elements2 = writeAnnoAppearance(pageInfo, annotationEntities);

//        // 绘制 模板层 和 页面内容层
//        writeLayer(resMgt, pdfCanvas, layerList, pageBox, null);
//        // 绘制电子印章
//        writeStamp(pdf, pdfCanvas, pageInfo, stampAnnots);
//        // 绘制注释
//        writeAnnoAppearance(resMgt, pdfCanvas, pageInfo, annotationEntities, pageBox);
//        return pdfPage;
        elements.addAll(elements2);
        return elements;

    }

    private List<Element> renderLayer(List<CT_Layer> layerList) {

        List<Element> pageDivs = new ArrayList<>();

        for (CT_Layer layer : layerList) {

            Element pageDiv = new Element();
            pageDiv.setTagName("div");


            List<Element> pageElements = writePageBlock(layer.getPageBlocks(), null, null);

            for (Element ele : pageElements) {
                pageDiv.appendChild(ele);
            }

            pageDivs.add(pageDiv);

        }


        return pageDivs;
    }

    private List<Element> writePageBlock(List<PageBlockType> layerList,
                                         CompositeObject compositeObject,
                                         ST_Box annotBox) {
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
//                     new ArrayList<>(Collections.singletonList(element));
            } else if (block instanceof CompositeObject) {
//                CompositeObject compositeObject_ = (CompositeObject) block;

//                CT_VectorG vectorG = resMgt.getCompositeGraphicUnit(compositeObject.getResourceID().toString());

//               return writePageBlock(vectorG.getContent().getPageBlocks(),compositeObject_,null);
            }
        }
        return pageElements;
    }

    private List<Element> writeAnnoAppearance(PageInfo pageInfo,
                                              List<AnnotionEntity> annotionEntities
    ) {
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
                List<Element> elements = writePageBlock(pageBlockTypeList, null, annotBox);
                pageElements.addAll(elements);
            }
        }
        return pageElements;
    }

    private Element renderText(TextObject textObject) {

//        float fontSize = textObject.getSize().floatValue();

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

        TextCode textCode = textCodes.get(0);

        char[] chars = textCode.getText().toCharArray();
        float len = 0;
        for (char c : chars) {
//            if (Character.isDigit(c) ||  Character.isUpperCase(c) ||  Character.isLowerCase(c))
            if (isChineseByBlock(c))
                len += 1;
            else
                len += 0.5;
        }

        //考虑到字符大小写导致的字体绝对宽度不一样，但是渲染的时候不必分隔开。
        double fontp = boundary.getWidth() / fontSize / len;

        List<TextCodePoint> textCodePointList = calTextPoint(textCode, scale, (fontp > 1.15));

        String letter = "";

        int index = -1;

        if (textCode.getText().equals("2011年3月10日印发")) {
            System.out.println(1);
        }

        for (TextCodePoint textCodePoint : textCodePointList) {

            index++;
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


//                    if (index + 1 < textCodePointList.size() && converterDpi(textCodePoint.getFirstDeltaX() * (ctm[0])) / fontSize > 1.5) {
//
//                        double w1 = (textCodePointList.get(index + 1).x - textCodePoint.x) * (ctm[0]);
//                        System.out.println("letter-spacing");
//                        letter = "letter-spacing:" + (w1 / fontSize - textCodePoint.getText().length()) * fontSize / (textCodePoint.getText().length() - 1) + "px";
//
//                    }


                } else {


                    text.setAttribute("transform", String.format(
                        "matrix(%s %s %s %s %.2f %.2f)",
                        ctm[0], ctm[1], ctm[2], ctm[3], converterDpi(ctm[4]), converterDpi(ctm[5])
                    ));


                }

            } else {

//                if (index + 1 < textCodePointList.size() && converterDpi(textCodePoint.getFirstDeltaX()) / fontSize > 1.5) {
//
//                    double w1 = ((textCodePointList.get(index + 1).x - textCodePoint.x) / fontSize - textCodePoint.getText().length()) * fontSize / (textCodePoint.getText().length() - 1);
//                    System.out.println("letter-spacing");
//                    if (w1 / fontSize >= 0.15)
//                        letter = "letter-spacing:" + (w1) + "px";
//
//                } else if (textCodePointList.size() == 1) {
//                    double v = (converterDpi(boundary.getWidth()) / fontSize - textCodePoint.getText().length()) * fontSize / (textCodePoint.getText().length() - 1);
//                    if (v / fontSize >= 0.15)
//                        letter = "letter-spacing:" + (v) + "px";
//
//                }

            }
            if (hScale != 1) {
                hScale = textCodePoint.getFirstDeltaX() / textObject.getSize();
                text.setAttribute("transform", String.format("matrix(%.1f,0,0,1, %.1f, 0)",
                    hScale, (1 - hScale) * textCodePoint.getX()));

            }

            text.setAttribute("fill", "transparent");


            text.setAttribute("style", String.format("font-weight: %d;font-size:%dpx;font-family: %s;%s",
                fontWeight.getWeight(), fontSize, "simSum", letter));


            svg.appendChild(text);

        }


        svg.setAttribute("style", String.format(
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
            textObject.getID())
        );
        return svg;


    }

    public float toFixed2(double num) {
        return (float) Math.round(num * 10) / 10;
    }

    public float converterDpi(double num) {
        return toFixed2(millimetersToPixel(num, scale * 25.4f));
    }

    public float millimetersToPixel(double mm, float dpi) {
        return (float) ((mm * dpi / 25.4));
    }

    public void parse() {

        long start;
        long end;
        int pageNum = 1;

        List<Element> pageDivs = new ArrayList<>();

//        List<Element> svgWrapper = new ArrayList<>();

        double paperWidth = 210;
        double paperPixels = screenWidth;

        List<ST_Box> boxs = new ArrayList<>();
        for (PageInfo pageInfo : ofdReader.getPageList()) {
            start = System.currentTimeMillis();
//            pdfMaker.makePage(pdfDocument, pageInfo);

            ST_Box pageBox = pageInfo.getSize();
            paperWidth = pageBox.getWidth();

            scale = (float) Math.round(((screenWidth - 10) / pageBox.getWidth()) * 10) / 10;

            int pageWidthPixel = (int) converterDpi(pageBox.getWidth());
            int pageHeightPixel = (int) converterDpi(pageBox.getHeight());
            paperPixels = pageWidthPixel;

            ST_Box st_box = new ST_Box(0, 0, (double) pageWidthPixel, (double) pageHeightPixel);
//            st_box.setWidth();
//            st_box.setHeight();
            boxs.add(st_box);

            Element pageDiv = new Element();
            pageDiv.setTagName("div");
            pageDiv.setAttribute("style", String.format("margin-bottom: 20px;position: relative;width:%dpx;height:%dpx;;",
                pageWidthPixel,
                pageHeightPixel));


            List<Element> elements = makePage(pageInfo);

            for (Element ele : elements) {
                pageDiv.appendChild(ele);
            }

            pageDivs.add(pageDiv);
            end = System.currentTimeMillis();
//            logger.debug(String.format("page %d speed time %d", pageNum++, end - start));
        }

        double ppm = paperPixels / paperWidth;

        SVGMaker svgMaker = null;
        try {
            svgMaker = new SVGMaker(new OFDReader(inputFile), ppm);
        } catch (IOException e) {
            e.printStackTrace();
        }
        svgMaker.config.setDrawBoundary(false);
        svgMaker.config.setClip(false);

        List<String> svgs = new ArrayList<>();
        for (int i = 0; i < svgMaker.pageSize(); i++) {
            String svg = svgMaker.makePage(i);
            svgs.add(svg);
        }

        String ofdHtml = displayOfdDiv(pageDivs, svgs, boxs);

        try {
            writeToFile("D:/ofd/mk.html", ofdHtml);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        String prefix = "                <!DOCTYPE html>\n" +
            "                <html lang=\"en\">\n" +
            "                  <head>\n" +
            "                    <meta charset=\"utf-8\">\n" +
            "                    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
            "                    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">";
        String titleElement = "<title>" + title + "</title>";
        String ele1 = "</head><body>";

        String svgWrapper = "<div class=\"svgWrapper\"> " + svgss + "</div>";

        String main = "                <div class=\"main-section\"id=\"content\" ref=\"contentDiv\" @mousewheel=\"scrool\">";

        String ele2 = "</div></body></html>";
        String style = "<style>\n" +
            "            .svgWrapper{\n" +
            "           margin:0 auto;left:0;right:0;text-align:center;position:absolute;z-index:1" +
            "        }\n" +
            "                    html,body{\n" +
            "                        margin:0;background: #808080;height:100%\n" +
            "                    }\n" +
            "                    .main-section {\n" +
            "                        padding-top: 20px;\n" +
            "                        display: flex;\n" +
            "                        flex-direction: column;\n" +
            "                        align-items: center;\n" +
            "                        justify-content: center;\n" +
            "                        background: #808080;\n" +
            "                        overflow: hidden;\n" +
            "                        position: relative;\n" +
            "                    }\n" +
            "                </style>";
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


        return sb.append(prefix).append(titleElement).append(ele1).
            append(svgWrapper).

            append(main).append(divs).append(ele2).append(style).append(js).toString();

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


    public List<TextCodePoint> calTextPoint(TextCode textCode, float scale, boolean useDiff) {
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


//
//        List<Integer> diffDeltaY = new ArrayList<>();
//
//        for (int i = 0; i < deltaYList.size() - 1; i++) {
//            if (diffDeltaY.size() > 0) {
//                if ((i - 1) > diffDeltaY.get(diffDeltaY.size() - 1) && !deltaYList.get(i).equals(deltaYList.get(i + 1))) {
//                    diffDeltaY.add(i + 1);
//                }
//            } else if (!deltaYList.get(i).equals(deltaYList.get(i + 1))) {
//                diffDeltaY.add(i + 1);
//            }
//        }
//
//        if (textStr.equals("秘书股李冠标20")) {
//            System.out.println("test calc text");
//        }
//
//
//        List<Integer> diffDeltaX = new ArrayList<>();
////        System.out.println(textCode.get_DeltaX());
//
//        diffDeltaX.add(0);
//        if (useDiff)
//            for (int i = 0; i < deltaXList.size() - 1; i++) {
//
//
//                if (diffDeltaX.size() > 1) {
//
//                    if (i > diffDeltaX.get(diffDeltaX.size() - 1) && !deltaXList.get(i).equals(deltaXList.get(i + 1))) {
////                    System.out.println("-"+(i+1));
////                    diffDeltaX.add(i + 1);
//                        if (deltaXList.get(i).equals(deltaXList.get(i - 1))) {
//                            diffDeltaX.add(i + 2);
////                        System.out.println("-"+(i+2));
//                        } else {
//                            diffDeltaX.add(i + 1);
////                        System.out.println("-"+(i+1));
//                        }
//                    }
//
//                } else if (!deltaXList.get(i).equals(deltaXList.get(i + 1))) {
//
//                    if (i == 0) {
////                    System.out.println(i+1);
//                        diffDeltaX.add(i + 1);
//                    } else {
////                    System.out.println(i+2);
//                        diffDeltaX.add(i + 2);
//                    }
//                }
////            if (diffDeltaX.size() > 0) {
////                if ((i - 1) > diffDeltaX.get(diffDeltaX.size() - 1) && !deltaXList.get(i).equals(deltaXList.get(i + 1))) {
////                    diffDeltaX.add(i + 1);
////                    System.out.println(i+1);
////                }
////            } else  if (!deltaXList.get(i).equals(deltaXList.get(i + 1))) {
////                diffDeltaX.add(i + 2);
////                System.out.println(i+2);
////            }
//
//            }
//        diffDeltaX.add(textStr.length());
//
//
////        textStr = decodeHtml(textStr);
//        textStr = textStr.replace("&#x20;", " ");
//
//        //y轴方向上的换行
////        if (diffDeltaY.size() > 1) {
////
////            for (int i = 0; i < diffDeltaY.size(); i++) {
////                if (i > 0)
////                    y += deltaYList.get(diffDeltaY.get(i));
////
////                String text = textStr.substring(i == 0 ? 0 : diffDeltaY.get(i - 1) + 1, i == 0 ? diffDeltaY.get(0) + 1 : diffDeltaY.get(i) + 1);
////
////                TextCodePoint point = new TextCodePoint(
////                        converterDpi(x, scale),
////                        converterDpi(y, scale),
////                        text,
////                        deltaXList.size() > 0 ? deltaXList.get(0) : -1f,
////                        deltaYList.size() > 0 ? deltaYList.get(0) : -1f,
////                        deltaYList.size() > 0 && deltaXList.size() == 0
////                );
////
////                textCodePointList.add(point);
////            }
////
////        } else
//        if (diffDeltaX.size() > 2) {
//
//
//            for (int i = 0; i < diffDeltaX.size() - 1; i++) {
//                double x2 = 0;
//                if (i > 0) {
//
////                    x += deltaXList.get(diffDeltaX.get(i));
//
//                    Integer index = diffDeltaX.get(i);
////                    System.out.println("index="+index);
//                    for (int j = 0; j < index; j++) {
//                        x2 += deltaXList.get(j);
//                    }
//                    x2 = x + x2;
//
//                } else {
//                    x2 = x;
//                }
////                System.out.println("x="+x2);
//
//                String text = textStr.substring(
//                    i == 0 ? 0 : diffDeltaX.get(i),
//                    i == 0 ? diffDeltaX.get(1) : diffDeltaX.get(i + 1)
//                );
////                System.out.println("text="+text);
//
//
//                TextCodePoint point = new TextCodePoint(
//                    converterDpi(x2),
//                    converterDpi(y),
//                    text,
//                    deltaXList.size() > 0 ? deltaXList.get(0) : -1d,
//                    deltaYList.size() > 0 ? deltaYList.get(0) : -1d,
//                    deltaYList.size() > 0 && deltaXList.size() == 0
//                );
//
//
////                point.setPercent();
//
////                float textDXLen=0;
////                float total=0;
////                for (int j = 0; j < deltaXList.size(); j++) {
////                    if(j<text.length()){
////                        textDXLen+=deltaXList.get(j);
////                    }
////                    total+=deltaXList.get(j);
////                }
////
////                point.setPercent(textDXLen/total);
//                if (text.trim().length() != 0)
//                    textCodePointList.add(point);
//            }
//
//        } else {
//
//            //TODO 优化不同字距的显示
//            //diffDeltaX
//
//
//            TextCodePoint point = new TextCodePoint(
//                converterDpi(x),
//                converterDpi(y),
//                textStr,
//                deltaXList.size() > 0 ? deltaXList.get(0) : -1d,
//                deltaYList.size() > 0 ? deltaYList.get(0) : -1d,
//                deltaYList.size() > 0 && deltaXList.size() == 0
//            );
//
//            textCodePointList.add(point);
//        }

//        String textStr = textCode.getText();

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
                -1d,
                -1d,
                deltaYList.size() > 0 && deltaXList.size() == 0
            );

//            let textCodePoint = {'x': converterDpi(x), 'y': converterDpi(y), 'text': text};
            textCodePointList.add(point);
        }

        return textCodePointList;
    }


    public static class TextCodePoint {
        double x;
        double y;
        String text;
        Double firstDeltaX;
        Double firstDeltaY;
        boolean isVerticalDirection;
        float percent;

        public TextCodePoint() {
        }

        public TextCodePoint(double x, double y, String text, Double firstDeltaX, Double firstDeltaY, boolean isVerticalDirection) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.firstDeltaX = firstDeltaX;
            this.firstDeltaY = firstDeltaY;
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

        public Double getFirstDeltaX() {
            return firstDeltaX;
        }

        public void setFirstDeltaX(Double firstDeltaX) {
            this.firstDeltaX = firstDeltaX;
        }

        public Double getFirstDeltaY() {
            return firstDeltaY;
        }

        public void setFirstDeltaY(Double firstDeltaY) {
            this.firstDeltaY = firstDeltaY;
        }

        public boolean isVerticalDirection() {
            return isVerticalDirection;
        }

        public void setVerticalDirection(boolean verticalDirection) {
            isVerticalDirection = verticalDirection;
        }

        public float getPercent() {
            return percent;
        }

        public void setPercent(float percent) {
            this.percent = percent;
        }
    }

}
