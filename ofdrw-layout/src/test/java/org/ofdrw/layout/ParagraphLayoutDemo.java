package org.ofdrw.layout;

import org.junit.jupiter.api.Test;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Clear;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.layout.element.Span;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 段落布局演示
 *
 * @author 权观宇
 * @since 2021-05-26 23:11:21
 */
public class ParagraphLayoutDemo {

    /**
     * 段落布局模式
     */
    @Test
     void testPLayoutPatten() throws IOException {
        Path outP = Paths.get("target/TestPLayoutPatten.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            // - 独占整个行
            Paragraph p1 = new Paragraph();
            p1.add("独占整个行").setBorder(0.1d).setMargin(3d, 0d);
            // - 共享模式(inline-block)
            Paragraph p2 = new Paragraph();
            p2.add("共享模式(inline-block),元素等同于内容宽度");
            p2.setClear(Clear.none).setBorder(0.1d).setMargin(3d, 0d);
            // 结束共享模式
            Paragraph hit1 = new Paragraph("插入一个Clear=both的元素中断共享模式");
            // - 手动指定宽度，仅在 Clear != both 模式有效
            Paragraph p3 = new Paragraph();
            p3.add("手动指定宽度，仅在 Clear != both 模式有效");
            p3.setWidth(55d);
            p3.setClear(Clear.none).setBorder(0.1d).setMargin(3d, 0d);
            Paragraph hit2 = new Paragraph("插入一个Clear=both的元素中断共享模式");

            // 居中
            Paragraph pn = new Paragraph();
            pn.setWidth(80d);
            pn.setHeight(20d);
            pn.setBorder(0.4d);
            pn.setPadding(5d);
            pn.setClear(Clear.none).setIntegrity(true);
            Span sp = new Span("序号")
                    .setFont(FontName.SimHei.font())
                    .setFontSize(4d);
            pn.add(sp);
            pn.setFloat(AFloat.center);

            Paragraph hit3 = new Paragraph("插入一个Clear=both的元素中断共享模式");

            // 共享模式下 多元素同行
            Paragraph p4A = new Paragraph("这是A部分");
            p4A.setClear(Clear.none).setFloat(AFloat.center).setBorder(0.1d).setPadding(3d).setMargin(1d);
            Paragraph p4B = new Paragraph("这是B部分");
            p4B.setClear(Clear.none).setFloat(AFloat.center).setBorder(0.1d).setPadding(3d).setMargin(1d);
            ofdDoc.add(p1);
            ofdDoc.add(p2);
            ofdDoc.add(hit1);
            ofdDoc.add(p3);
            ofdDoc.add(hit2);
            ofdDoc.add(pn);
            ofdDoc.add(hit3);
            ofdDoc.add(p4A).add(p4B);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 文档示例
     */
    @Test
    void testPLayoutDoc() throws IOException {
        Path path = Paths.get("target/TestPLayoutDoc.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            ofdDoc.add(new Paragraph("拿来主义", 10d).setClear(Clear.none).setFloat(AFloat.center));
            ofdDoc.add(new Paragraph("鲁迅", 3d).setFloat(AFloat.center).setClear(Clear.left));
            String[] pArr = new String[]{
                    "中国一向是所谓“闭关主义”，自己不去，别人也不许来。自从给枪炮打破了大门之后，又碰了一串钉子，到现在，成了什么都是“送去主义”了。别的且不说罢，单是学艺上的东西，近来就先送一批古董到巴黎去展览，但终“不知后事如何”；还有几位“大师”们捧着几张古画和新画，在欧洲各国一路的挂过去，叫作“发扬国光”。听说不远还要送梅兰芳博士到苏联去，以催进“象征主义”，此后是顺便到欧洲传道。我在这里不想讨论梅博士演艺和象征主义的关系，总之，活人替代了古董，我敢说，也可以算得显出一点进步了。",
                    "但我们没有人根据了“礼尚往来”的仪节，说道：拿来！",
                    "当然，能够只是送出去，也不算坏事情，一者见得丰富，二者见得大度。尼采就自诩过他是太阳，光热无穷，只是给与，不想取得。然而尼采究竟不是太阳，他发了疯。中国也不是，虽然有人说，掘起地下的煤来，就足够全世界几百年之用，但是，几百年之后呢？几百年之后，我们当然是化为魂灵，或上天堂，或落了地狱，但我们的子孙是在的，所以还应该给他们留下一点礼品。要不然，则当佳节大典之际，他们拿不出东西来，只好磕头贺喜，讨一点残羹冷炙做奖赏。",
                    "这种奖赏，不要误解为“抛来”的东西，这是“抛给”的，说得冠冕些，可以称之为“送来”，我在这里不想举出实例。",
                    "我在这里也并不想对于“送去”再说什么，否则太不“摩登”了。我只想鼓吹我们再吝啬一点，“送去”之外，还得“拿来”，是为“拿来主义”。",
                    "但我们被“送来”的东西吓怕了。先有英国的鸦片，德国的废枪炮，后有法国的香粉，美国的电影，日本的印着“完全国货”的各种小东西。于是连清醒的青年们，也对于洋货发生了恐怖。其实，这正是因为那是“送来”的，而不是“拿来”的缘故。",
                    "所以我们要运用脑髓，放出眼光，自己来拿！",
                    "譬如罢，我们之中的一个穷青年，因为祖上的阴功（姑且让我这么说说罢），得了一所大宅子，且不问他是骗来的，抢来的，或合法继承的，或是做了女婿换来的。那么，怎么办呢？我想，首先是不管三七二十一，“拿来”！但是，如果反对这宅子的旧主人，怕给他的东西染污了，徘徊不敢走进门，是孱头；勃然大怒，放一把火烧光，算是保存自己的清白，则是昏蛋。不过因为原是羡慕这宅子的旧主人的，而这回接受一切，欣欣然的蹩进卧室，大吸剩下的鸦片，那当然更是废物。“拿来主义”者是全不这样的。",
                    "他占有，挑选。看见鱼翅，并不就抛在路上以显其“平民化”，只要有养料，也和朋友们像萝卜白菜一样的吃掉，只不用它来宴大宾；看见鸦片，也不当众摔在茅厕里，以见其彻底革命，只送到药房里去，以供治病之用，却不弄“出售存膏，售完即止”的玄虚。只有烟枪和烟灯，虽然形式和印度，波斯，阿剌伯的烟具都不同，确可以算是一种国粹，倘使背着周游世界，一定会有人看，但我想，除了送一点进博物馆之外，其余的是大可以毁掉的了。还有一群姨太太，也大以请她们各自走散为是，要不然，“拿来主义”怕未免有些危机。",
                    "总之，我们要拿来。我们要或使用，或存放，或毁灭。那么，主人是新主人，宅子也就会成为新宅子。然而首先要这人沉着，勇猛，有辨别，不自私。没有拿来的，人不能自成为新人，没有拿来的，文艺不能自成为新文艺。"
            };
            for (String s : pArr) {
                ofdDoc.add(new Paragraph(s, 5d).setFirstLineIndent(2));
            }
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }
}
