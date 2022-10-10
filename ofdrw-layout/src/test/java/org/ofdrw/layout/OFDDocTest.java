package org.ofdrw.layout;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.action.Actions;
import org.ofdrw.core.action.CT_Action;
import org.ofdrw.core.action.EventType;
import org.ofdrw.core.action.actionType.URI;
import org.ofdrw.core.annotation.pageannot.*;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.*;
import org.ofdrw.core.text.CT_CGTransform;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.font.Font;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.edit.Annotation;
import org.ofdrw.layout.element.*;
import org.ofdrw.layout.element.canvas.Canvas;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OFD 功能测试
 *
 * @author 权观宇
 * @since 2020-03-22 11:38:48
 */
class OFDDocTest {

    /**
     * 测试Div图层属性
     */
    @Test
    void vPageLayerTest() throws IOException {
        Path path = Paths.get("target/MutiLayer.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);

            final Span span = new Span("你好")
                    .setFontSize(15d)
                    .setColor(255, 0, 0);
            Paragraph p = new Paragraph().add(span);
            p.setPosition(Position.Absolute)
                    .setXY(pageLayout.getWidth() / 2, pageLayout.getHeight() / 2)
                    .setWidth(40d)
                    .setLayer(Type.Body);

            Path imgPath = Paths.get("src/test/resources", "eg_tulip.jpg");
            Img img = new Img(80, 53, imgPath);
            double x = (pageLayout.getWidth() - img.getWidth()) / 2;
            double y = (pageLayout.getHeight() - img.getHeight()) / 2;
            img.setPosition(Position.Absolute)
                    .setX(x).setY(y);
            img.setBorder(0.1d);
            img.setLayer(Type.Background);

            // 先添加的文字
            vPage.add(p);
            // 后添加图片，由于图片处于背景层，不会覆盖文字
            vPage.add(img);


            ofdDoc.addVPage(vPage);
            System.out.println(">> " + path.toAbsolutePath());
        }
    }

    /**
     * 在生成文档的过程中获取文档信息
     */
    @Test
    void onRenderFinished() throws IOException {
        Path path = Paths.get("target/AddInfoAfterRender.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Paragraph p = new Paragraph("你好呀，OFD Reader&Writer！", 8d);
            ofdDoc.add(p);
            // 通过回调函数向文档加入一个点击动作
            ofdDoc.onRenderFinish(((maxUnitID, ofdDir, index) -> {
                try {
                    final DocDir docDir = ofdDir.getDocByIndex(index);
                    final Document document = docDir.getDocument();
                    Actions actions = new Actions();
                    CT_Action myAction = new CT_Action(EventType.DO, new URI("https://gitee.com/ofdrw/ofdrw"));
                    myAction.setObjID(maxUnitID.incrementAndGet());
                    actions.addAction(myAction);
                    document.setActions(actions);
                } catch (IOException | DocumentException e) {
                    e.printStackTrace();
                }
            }));
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }


    /**
     * 在生成文档的过程中获取文档信息
     */
    @Test
    void genDocAndGetDocInfo() throws IOException {
        Path path = Paths.get("target/doc-my-info.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Paragraph p = new Paragraph("你好呀，OFD Reader&Writer！", 8d);
            ofdDoc.add(p);
            String boxSize = ofdDoc.getOfdDocument().getCommonData().getPageArea().getApplicationBox().toString();
            p = new Paragraph(String.format("页面尺寸为 [%s]", boxSize), 5d);
            ofdDoc.add(p);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    /**
     * 字体宽度溢出可用最大宽度测试
     */
    @Test
    void fontSizeOverflow() throws IOException {
        Path outP = Paths.get("target/FontSizeOverflow.ofd");

        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            Paragraph p = new Paragraph(10d, 20d).setFontSize(15d);
            p.add("l我l");
            p.setBorder(0.1d);
            ofdDoc.add(p);
            // Expect: 只显示 "l"
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 加入印章类型注释对象
     */
    @Test
    void addAnnotationStamp() throws IOException {
        Path srcP = Paths.get("src/test/resources", "AddWatermarkAnnot.ofd");
        Path outP = Paths.get("target/AddAnnotationStamp.ofd");
        Path imgPath = Paths.get("src/test/resources", "StampImg.png");

        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            Annotation annotation = new Annotation(70d, 100d, 60d, 60d, AnnotType.Stamp, ctx -> {
                ctx.setGlobalAlpha(0.53);
                ctx.drawImage(imgPath, 0, 0, 40d, 40d);
            });
            ofdDoc.addAnnotation(1, annotation);

        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath());
    }


    /**
     * 加入水印类型注释对象
     */
    @Test
    void addAnnotation() throws IOException {
        Path srcP = Paths.get("src/test/resources", "拿来主义_page6.ofd");
        Path outP = Paths.get("target/AddWatermarkAnnot.ofd");
        Path imgPath = Paths.get("src/test/resources", "eg_tulip.jpg");

        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            ST_Box boundary = new ST_Box(50d, 50d, 60d, 60d);
            Annotation annotation = new Annotation(boundary, AnnotType.Watermark, ctx -> {
                ctx.setGlobalAlpha(0.53);
                ctx.drawImage(imgPath, 0, 0, 40d, 30d);
            });

            ofdDoc.addAnnotation(1, annotation);
            ofdDoc.addAnnotation(2, annotation);
            ofdDoc.addAnnotation(2, annotation);

        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 加入水印类型注释对象
     */
    @Test
    void addAnnotationExist() throws IOException {
        Path srcP = Paths.get("src/test/resources", "ano.ofd");
        Path outP = Paths.get("target/ano_two_mark.ofd");
        Path imgPath = Paths.get("src/test/resources", "eg_tulip.jpg");

        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            ST_Box boundary = new ST_Box(50d, 50d, 60d, 60d);
            Annotation annotation = new Annotation(boundary, AnnotType.Watermark, ctx -> {
                ctx.setGlobalAlpha(0.53);
                ctx.drawImage(imgPath, 0, 0, 40d, 30d);
            });
            ofdDoc.addAnnotation(1, annotation);
            ofdDoc.addAnnotation(2, annotation);

        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 测试加入操作系统中的字体
     */
    @Test
    void testAddSysFont() throws IOException {
        Path outP = Paths.get("target/SystemFont.ofd");
        try (OFDDoc doc = new OFDDoc(outP)) {
            Font font = new Font("等线 Light", "等线 Light");
            Paragraph p = new Paragraph()
                    .setDefaultFont(font)
                    .setFontSize(10d)
                    .add("字体名称：等线 Light");

            doc.add(p);

            font = FontName.MSYahei.font();
            p = new Paragraph()
                    .setDefaultFont(font)
                    .setFontSize(10d)
                    .add("字体名称：微软雅黑");
            doc.add(p);

            // 注意：在使用操作系统字体时，默认采用ACSII 0.5 其余1的比例计算宽度，因此可能需要手动设置宽度比例才可以达到相应的效果
            font = new Font("Times New Roman", "Times New Roman")
                    .setPrintableAsciiWidthMap(FontName.TIMES_NEW_ROMAN_PRINTABLE_ASCII_MAP);
            p = new Paragraph()
                    .setDefaultFont(font)
                    .setFontSize(10d)
                    .add("Font Name: Time New Roman");
            doc.add(p);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath());
    }


    @Test
    void divBoxTest() throws IOException {
        Path path = Paths.get("target/VPage1.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);
            Div e = new Div(10d, 10d)
                    .setPosition(Position.Absolute)
                    .setX(70d).setY(113.5)
                    .setBackgroundColor(30, 144, 255)
                    .setMargin(10d)
                    .setBorder(10d)
                    .setPadding(10d);
            vPage.add(e);
            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void divBoxDiffBorderTest() throws IOException {
        Path path = Paths.get("target/VPage2.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);
            Div e = new Div(10d, 10d)
                    .setPosition(Position.Absolute)
                    .setX(70d).setY(113.5)
                    .setBackgroundColor(30, 144, 255)
                    .setBorderColor(255, 0, 0)
                    .setMargin(10d)
                    .setBorderTop(10d).setBorderRight(7d).setBorderBottom(3d).setBorderLeft(0.5d)
                    .setPadding(10d);
            vPage.add(e);
            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    /**
     * 测试添加图片
     */
    @Test
    void imgTest() throws IOException {
        Path path = Paths.get("target/VPageOfPNG.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);
            Path imgPath = Paths.get("src/test/resources", "testimg.png");
            Img img = new Img(imgPath);

            double x = (pageLayout.getWidth() - img.getWidth()) / 2;
            double y = (pageLayout.getHeight() - img.getHeight()) / 2;
            img.setPosition(Position.Absolute)
                    .setX(x).setY(y);
            img.setBorder(3d);
            img.setPadding(3d);
            vPage.add(img);
            ofdDoc.addVPage(vPage);
        }
    }

    @Test
    void imgTestTif() throws IOException {
        Path path = Paths.get("target/VPageOfTIFF.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);
            Path imgPath = Paths.get("src/test/resources", "asf-logo.tif");
            Img img = new Img(imgPath);

            double x = (pageLayout.getWidth() - img.getWidth()) / 2;
            double y = (pageLayout.getHeight() - img.getHeight()) / 2;
            img.setPosition(Position.Absolute)
                    .setX(x).setY(y);
            img.setBorder(3d);
            img.setPadding(3d);
            vPage.add(img);
            ofdDoc.addVPage(vPage);
        }
    }

    @Test
    void paragraphTest() throws IOException {
        Path path = Paths.get("target/VPage4.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {

            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);

            Paragraph p = new Paragraph(120d, 10d);
            p.add("我们无论遇到什么困难也不要怕，微笑着面对它，消除恐惧的最好办法就是直面恐惧，坚持就是胜利，加油！奥力给！ （OFD Reader & Write）");
            p.setPosition(Position.Absolute)
                    .setPadding(3d)
                    .setBorder(0.353);
            double x = pageLayout.getWidth() / 2 - p.getMarginLeft() - p.getWidth() / 2;
            p.setX(x).setY(pageLayout.getMarginTop());

            vPage.add(p);
            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void paragraphTest2() throws IOException {
        Path path = Paths.get("target/VPage5.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);

            Paragraph p = new Paragraph(120d, 10d);
            StringBuilder txt = new StringBuilder();
            for (char ch = 32; ch <= 126; ch++) {
                txt.append(ch);
            }
            p.add(txt.toString());
            p.setPosition(Position.Absolute)
                    .setPadding(3d)
                    .setBorder(0.353);
            double x = pageLayout.getWidth() / 2 - p.getMarginLeft() - p.getWidth() / 2;
            p.setX(x).setY(pageLayout.getMarginTop());
            vPage.add(p);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());

    }


    @Test
    void streamTestFloat() throws IOException {
        Path path = Paths.get("target/VPage6.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Div eLeft = new Div(10d, 10d)
                    .setBackgroundColor(30, 144, 255)
                    .setFloat(AFloat.left)
                    .setBorder(10d)
                    .setPadding(10d);
            Div eCenter = new Div(10d, 10d)
                    .setBackgroundColor(30, 144, 255)
                    .setFloat(AFloat.center)
                    .setBorder(10d)
                    .setPadding(10d);
            Div eRight = new Div(10d, 10d)
                    .setBackgroundColor(30, 144, 255)
                    .setFloat(AFloat.right)
                    .setBorder(10d)
                    .setPadding(10d);

            ofdDoc.add(eLeft);
            ofdDoc.add(eCenter);
            ofdDoc.add(eRight);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void streamTestDivPageSplit() throws IOException {
        Path path = Paths.get("target/VPage7.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            ofdDoc.setDefaultPageLayout(PageLayout.A4().setMargin(0d));
            for (int i = 0; i < 500; i++) {
                Div e = new Div(70d, 30d)
                        .setBackgroundColor(255, 0, 0)
                        .setFloat(AFloat.center)
                        .setMargin(5d)
                        .setBorder(0.353d);
                ofdDoc.add(e);
            }

        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void streamTestParagraphPageSplit() throws IOException {
        Path path = Paths.get("target/VPage8.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Span title = new Span("看云识天气").setBold(true).setFontSize(13d).setLetterSpacing(10d);
            Paragraph p = new Paragraph().add(title);
            p.setFloat(AFloat.center).setMargin(5d);
            ofdDoc.add(p);

            Span author = new Span("朱泳燚").setBold(true).setFontSize(3d);
            p = new Paragraph().add(author);
            p.setFloat(AFloat.center).setMargin(5d);
            ofdDoc.add(p);

            Paragraph p1 = new Paragraph("天上的云，姿态万千，变化无常：有的像羽毛，轻轻地飘在空中；有的像鱼鳞，一片片整整齐齐地排列着；有的像羊群，来来去去；有的像一张大棉絮，满满地盖住了天空；还有的像峰峦，像河川，像雄狮，像奔马……它们有时把天空点缀得很美丽，有时又把天空笼罩得很阴森。刚才还是白云朵朵，阳光灿烂；一霎间却又是乌云密布，大雨倾盆。云就像是天气的“招牌”，天上挂什么云，就将出现什么样的天气。 ")
                    .setFirstLineIndent(2);
            Paragraph p2 = new Paragraph("经验告诉我们：天空的薄云，往往是天气晴朗的象征；那些低而厚密的云层，常常是阴雨风雪的预兆。")
                    .setFirstLineIndent(2);
            Paragraph p3 = new Paragraph("那最轻盈、站得最高的云，叫卷云。这种云很薄，阳光可以透过云层照到地面，房屋和树木的影子依然很清晰。卷云丝丝缕缕地飘浮着，有时像一片白色的羽毛，有时像一块洁白的绫纱。如果卷云成群成行地排列在空中，好像微风吹过水面引起的粼波，这就成了卷积云。卷云和卷积云的位置很高，那里水分少，它们一般不会带来雨雪。还有一种像棉花团似的白云，叫积云，常在两千米左右的天空，一朵朵分散着，映着温和的阳光，云块四周散发出金黄的光辉。积云都在上午开始出现，午后最多，傍晚渐渐消散。在晴天，我们还会遇见一种高积云。这是成群的扁球状的云块，排列得很匀称，云块间露出碧蓝的天幕，远远望去，就像草原上雪白的羊群。卷云、卷积云、积云和高积云，都是很美丽的。 ")
                    .setFirstLineIndent(2);
            Paragraph p4 = new Paragraph("当那连绵的雨雪要来临的时候，卷云聚集着，天空渐渐出现一层薄云，仿佛蒙上了白色的绸幕。这种云叫卷层云。卷层云慢慢地向前推进，天气就要转阴。接着，云越来越低，越来越厚，隔着云看太阳和月亮，就像隔了一层毛玻璃，朦胧不清。这时的卷层云得改名换姓，该叫它高层云了。出现了高层云，往往在几个钟头内便要下雨或者下雪。最后，云压得更低，变得更厚，太阳和月亮都躲藏了起来，天空被暗灰色的云块密密层层地布满了。这种新的云叫雨层云。雨层云一形成，连绵不断的雨雪也就开始下降。 ")
                    .setFirstLineIndent(2);
            Paragraph p5 = new Paragraph("夏天，雷雨到来之前，在天空先会出现积云。积云如果迅速向上凸起，形成高大的云山，群峰争奇，耸入天顶，就变成了积雨云。积雨云越长越高，云底慢慢变黑，云峰渐渐模糊，不一会儿，整座云山崩塌了，乌云弥漫着天空，顷刻间，雷声隆隆，电光闪闪，就会哗啦哗啦地下起暴雨来，有时竟会带来冰冰雹或者龙卷风。")
                    .setFirstLineIndent(2);
            Paragraph p6 = new Paragraph("我们还可以根据云上的光彩，推测天气的情况。在太阳和月亮的周围，有时会出现一种美丽的七彩光圈，里层是红色的，外层是紫色的。这种光圈叫做晕。日晕和月晕常常出现在卷层云上，当卷层云后面有一大片高层云和雨层云时，是大风雨的征兆。所以有“日晕三更雨，月晕午时风”的说法。说明出现卷层云，并且伴有晕，天气就会变坏。另有一种比晕小的彩色光环，叫做华。颜色的排列是里紫外红，跟晕刚好相反。日华和月华大多出现在高积云的边缘。华环由小变大，天气将趋向晴好。华环由大变小，天气可能转为阴雨。夏天，雨过天晴，太阳对面的云幕上，常会挂上一条彩色的圆弧，这就是虹。人们常说：“东虹轰隆西虹雨。”意思说，虹在东方，就有雷无雨；虹在西方，将会有大雨。还有一种云彩常出现在清晨或傍晚。太阳照到天空，使云层变成红色，这种云彩叫做霞。出现朝霞，表明阴雨天气就要到来；出现晚霞，表示最近几天里天气晴朗。所以有“朝霞不出门，晚霞行千里”的谚语。")
                    .setFirstLineIndent(2);
            Paragraph p7 = new Paragraph("云能够帮助我们识别阴晴风雨，预知天气变化，这对工农业生产有着重要的意义。我们要学会看云识天气，就要虚心向有经验的人学习，留心观察云的变化，在反复实践中掌握它们的规律。但是，天气变化异常复杂，看云识天气有一定的限度。我们要准确地掌握天气变化的情况 ")
                    .setFirstLineIndent(2);
            ofdDoc.add(p1);
            ofdDoc.add(p2);
            ofdDoc.add(p3);
            ofdDoc.add(p4);
            ofdDoc.add(p5);
            ofdDoc.add(p6);
            ofdDoc.add(p7);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void streamTestLineDiffPageSplit() throws IOException {
        Path path = Paths.get("target/VPage9.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Paragraph p = new Paragraph();
            Span sp0 = new Span("我们无论遇到什么困难也不要怕，微笑着面对它，消除恐惧的最好办法就是直面恐惧，")
                    .setFontSize(5d).setItalic(true);
            p.add(sp0);
            ofdDoc.add(p);

            p = new Paragraph();
            Span sp1 = new Span("坚持就是胜利，")
                    .setFontSize(5d)
                    .setUnderline(true);
            Span sp2 = new Span("加油！")
                    .setFontSize(10d)
                    .setUnderline(true);
            Span sp3 = new Span("奥力给！")
                    .setColor(255, 0, 0)
                    .setBold(true)
                    .setFontSize(15d);

            p.add(sp1).add(sp2).add(sp3);
            ofdDoc.add(p);

        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }


    @Test
    void elementOpacityTest() throws IOException {
        Path path = Paths.get("target/ElementOpacityDoc.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {

            Div div = new Div(30d, 30d)
                    .setBackgroundColor(255, 0, 0)
                    .setMargin(3d)
                    .setBorder(1d)
                    .setOpacity(0.5)
                    .setFloat(AFloat.center);

            Path imgPath = Paths.get("src/test/resources", "asf-logo.tif");
            Img img = new Img(imgPath);
            img.setOpacity(0.3)
                    .setMargin(3d)
                    .setBorder(1d);

            Paragraph p = new Paragraph().setFontSize(10d)
                    .add("我们无论遇到什么困难也不要怕，微笑着面对它，" +
                            "消除恐惧的最好办法就是直面恐惧，坚持就是胜利，加油！奥力给！");
            p.setPadding(3d)
                    .setOpacity(0.2d)
                    .setMargin(3d)
                    .setBorder(1d, 2d, 3d, 4d);

            ofdDoc.add(div).add(img).add(p);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void splitStrToParagraph() throws IOException {
        String plaintext = "只……只要我把那家伙给拖进来……\n交给你的话……\n你……你真的会……饶我一命吗？" +
                "嘻嘻~当然\n我可是说话算话的啦~\n这算是以他的养分为筹码的交易Give&Take啦……快……快点叫吧！\n" +
                "但是我拒绝！\n——JOJO 岸边露伴";

        Path path = Paths.get("target/SplitStrToParagraphDoc.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            for (String pTxt : plaintext.split("\\\n")) {
                Paragraph p = new Paragraph(pTxt);
                ofdDoc.add(p);
            }
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void splitStrToParagraph2() throws IOException {
        String plaintext = "只……只要我把那家伙给拖进来……\n交给你的话……\n你……你真的会……饶我一命吗？\n\n" +
                "嘻嘻~当然\n我可是说话算话的啦~\n这算是以他的养分为筹码的交易Give&Take啦……快……快点叫吧！\n\n\n\n" +
                "但是我拒绝！\n——JOJO 岸边露伴";

        Path path = Paths.get("target/SplitStrToParagraphDoc2.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Paragraph p = new Paragraph(plaintext);
            ofdDoc.add(p);
            final Paragraph item = new Paragraph()
                    .add(new Span("\nOFD R&W").setLinebreak(true))
                    .add(new Span("Nice Day!"));

            ofdDoc.add(item);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    public void fillPageParagraph() throws IOException {
        Path path = Paths.get("target/FillPageParagraphDoc.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            VirtualPage virtualPage = new VirtualPage(210.0, 140.0);
            virtualPage.getStyle().setMargin(0d);
            Paragraph p = new Paragraph("helloword");
            p.setPosition(Position.Absolute);
            p.setX(0.0);
            p.setY(0.0);
            p.setWidth(210.0 - 0.5);
            p.setHeight(140.0 - 0.5);
            p.setBorder(0.25);
            p.setPadding(0.0);
            p.setMargin(0.0);
            virtualPage.add(p);
            ofdDoc.addVPage(virtualPage);
        }
        System.out.println("生成文档位置：" + path.toAbsolutePath());
    }

    @Test
    public void canvasInflow() throws IOException {
        Path path = Paths.get("target/CanvasInflow.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Paragraph p = new Paragraph("这是一个圆形哦");
            p.setClear(Clear.none);
            Canvas canvas = new Canvas(20d, 20d);
            canvas.setClear(Clear.none);
            canvas.setDrawer(ctx -> {
                ctx.beginPath();
                ctx.arc(10, 10, 5, 0, 360);
                ctx.stroke();
            });
            Paragraph p2 = new Paragraph("是不是很好看");
            p2.setClear(Clear.none);
            ofdDoc.add(p)
                    .add(canvas)
                    .add(p2);
        }
        System.out.println("生成文档位置：" + path.toAbsolutePath());
    }

    @Test
    public void virtualPagePositioning() throws IOException {
        Path path = Paths.get("target/virtualPagePositioning.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {

            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(0d)
                    .setY(0d)
                    .setBorder(1d);
            canvas.setDrawer(ctx -> {
                ctx.beginPath();
                ctx.arc(10, 10, 5, 0, 360);
                ctx.stroke();
            });

            vPage.add(canvas);
            ofdDoc.addVPage(vPage);

            Paragraph p = new Paragraph("是不是很好看是不是很好看是不是很好看是不是很好看");
            p.setPosition(Position.Absolute)
                    .setWidth(30d)
                    .setX(50d).setY(100d);
            vPage.add(p);
        }
        System.out.println("生成文档位置：" + path.toAbsolutePath());
    }

    /**
     * 添加图片+特定位置文本
     *
     * @author zwd
     */
    @Test
    void test05() throws IOException {
        // 页面宽高设置
        Double widthZb = 210d;
        Double heightZb = 156d;

        Path path = Paths.get("target", "addCMYKImage.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            // 设置最外层样式
            VirtualPage vPage = new VirtualPage(pageLayout);
            PageLayout style = new PageLayout(widthZb, heightZb);
            vPage.setStyle(style);
            // 设置图片相关信息
            Path imgPath = Paths.get("src/test/resources", "img-CMYK.jpg");
            // img部分代码有修改，具体使用要注意
            Img img = new Img(imgPath);
            img.setPosition(Position.Absolute).setX(0d).setY(0d);
            img.setBorder(0d);
            img.setPadding(0d);
            img.setWidth(210d);
            img.setHeight(156d);
            // 添加图片
            vPage.add(img);
            // 设置文本相关信息
            Paragraph p = new Paragraph(100d, 30d).setFontSize(4d);
            p.add("zwd");
            p.setPosition(Position.Absolute).setX(62d).setY(70d);
            // 添加文本
            vPage.add(p);
            // 往ofd中添加页面一
            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + path.toAbsolutePath());
    }

    /**
     * 替换文字内容
     *
     * @author shanhy
     */
    @Test
    void testReplaceText() throws IOException {
        // 随便找一张电子发票，例如滴滴打车发票的ofd格式，即可测试
        Path srcP = Paths.get("src/test/resources", "滴滴电子发票 (11).ofd");
        Path outP = Paths.get("target", "test-reaplaced.ofd");
        Path fontFile = Paths.get("src/test/resources", "simhei-cut1.ttf");

        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {

            Font newFont = new Font("simsun-cut1","simsun-cut1",fontFile);
            ST_ID newFontID = ofdDoc.getResManager().addFont(newFont);

            DocContentReplace docContentReplace = new DocContentReplace(ofdDoc);
            docContentReplace.setReplaceTextHandler(new DocContentReplace.ReplaceTextHandler() {
                @Override
                public CT_CGTransform handleCgTransform(TextObject textObject, String newText, CT_Font beforeCtFont) {
                    // 这里构造文字的CgTransform
                    if (newText.equals("杭州钧硕科技有限公司"))
                        return new CT_CGTransform().setCodeCount(10).setCodePosition(0).setGlyphCount(10).setGlyphs(ST_Array.getInstance("25 26 27 28 29 30 31 32 33 34"));
                    return null;
                }
                @Override
                public Font handleNewFont(TextObject textObject, String newText, CT_Font beforeCtFont){
                    if (newText.equals("红宇测试有限公司")) {
                        textObject.setFont(new ST_RefID(newFontID));
                        return newFont;
                    }
                    return null;
                }
            });

            Map<String, String> map = new HashMap<>();
            map.put("Evaluation Warning", "Hi, Tom. Welcome to China.");
            map.put("杭州钧硕科技有限公司", "红宇测试有限公司");
            map.put("滴滴出行科技有限公司", "杭州钧硕科技有限公司");
            map.put("滴滴信息服务有限公司", "红宇测试有限公司");
            map.put("重庆呼我出行网络科技有限公司", "红宇测试有限公司");
            map.put("赵笑林", "单红宇");
            docContentReplace.replaceText(map);
            System.out.println("生成文档位置：" + outP.toAbsolutePath());
        }
    }

    /**
     * 添加字体
     */
    @Test
    public void testFont() throws IOException {
        Path srcP = Paths.get("src/test/resources", "AddWatermarkAnnot.ofd");
        Path outP = Paths.get("target/AddWatermarkAnnot-addfont.ofd");
        Path fontFile = Paths.get("src/test/resources", "simhei-cut1.ttf");

        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            Font font = new Font("simsun-cut1","simsun-cut1",fontFile);
            ofdDoc.getResManager().addFont(font);

            Paragraph p = new Paragraph("国庆节普天同庆", 8d, font);
            ofdDoc.add(p);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath());
    }
}