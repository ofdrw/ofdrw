package org.ofdrw.layout;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.element.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-03-22 11:38:48
 */
class OFDDocTest {


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
            ofdDoc.setDefaultPageLayout(PageLayout.A4.setMargin(0d));
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
}