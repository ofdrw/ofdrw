package org.ofdrw.layout;

import org.junit.jupiter.api.Test;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.element.*;

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
     * 测试段落内文字浮动
     */
    @Test
    void testParagraphInlineFloat() throws IOException {
        Path outP = Paths.get("target/testParagraphInlineFloat.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {

            // 向左浮动
            Paragraph p1 = new Paragraph("这是左浮动内容", 4d)
                    .setTextAlign(TextAlign.left);
            Paragraph p2 = new Paragraph("这是\n多行的文本\n\n我将采用向左浮动", 4d)
                    .setTextAlign(TextAlign.left);

            // 右浮动
            Paragraph p3 = new Paragraph("这是右浮动内容", 4d)
                    .setTextAlign(TextAlign.right);
            Paragraph p4 = new Paragraph("这是\n多行的文本\n\n我将采用向右浮动", 4d)
                    .setTextAlign(TextAlign.right);

            // 居中
            Paragraph p5 = new Paragraph("这是居中内容", 4d)
                    .setTextAlign(TextAlign.center);
            Paragraph p6 = new Paragraph("这是\n多行的文本\n\n我将采用向居中", 4d)
                    .setTextAlign(TextAlign.center);

            ofdDoc.add(p1)
                    .add(p2)
                    .add(p3)
                    .add(p4)
                    .add(p5)
                    .add(p6);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath());

    }

    /**
     * 段落布局 各种情况演示
     *
     * @author 权观宇
     * @since 2021-05-26 23:26:24
     */
    @Test
    void testParagraphLayout() throws IOException {
        Path outP = Paths.get("target/ParagraphLayoutDemo.ofd");
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
     * 段落首行缩进测试用例
     */
    @Test
    void testParagraphFirstLineIndent() throws IOException {
        Path outP = Paths.get("target/ParagraphFirstLineIndent.ofd");

        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            String titleStr = "济南的冬天";
            String p1Str = "对于一个在北平住惯的人，像我，冬天要是不刮风，便觉得是奇迹；济南的冬天是没有风声的。对于一个刚由伦敦回来的人，像我，冬天要能看得见日光，便觉得是怪事；济南的冬天是响晴的。自然，在热带的地方，日光是永远那么毒，响亮的天气，反有点叫人害怕。可是，在北中国的冬天，而能有温晴的天气，济南真得算个宝地。";
            String p2Str = "设若单单是有阳光，那也算不了出奇。请闭上眼睛想：一个老城，有山有水，全在天底下晒着阳光，暖和安适地睡着，只等春风来把它们唤醒，这是不是个理想的境界？";

            Span spTitle = new Span(titleStr)
                    .setFont(FontName.SimHei.font()) // 设置字体
                    .setBold(true)
                    .setFontSize(7d)
                    .setLetterSpacing(5d);
            Paragraph title = new Paragraph().add(spTitle);
            title.setFloat(AFloat.center).setMargin(5d);

            Span sp1 = new Span(p1Str)
                    .setFontSize(3d)        // 设置字体大小
                    .setLetterSpacing(3d);  // 设置字间距
            Paragraph p1 = new Paragraph()
                    .add(sp1)
                    .setFirstLineIndent(2); // 设置首行缩进

            Span sp2 = new Span(p2Str).setLetterSpacing(3d);
            Paragraph p2 = new Paragraph()
                    .add(sp2)
                    .setFirstLineIndentWidth((3d + 3d) * 2); // 设置首行缩进宽度，单位mm

            // 将段落加入文档
            ofdDoc.add(title).add(p1).add(p2);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

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
            ofdDoc.add(new Paragraph("青春 选节", 10d).setClear(Clear.none).setFloat(AFloat.center));
            ofdDoc.add(new Paragraph("李大钊", 3d).setFloat(AFloat.center).setClear(Clear.left));
            String[] pArr = new String[]{
                    "人类之成一民族一国家者，亦各有其生命焉。有青春之民族，斯有白首之民族，有青春之国家，斯有白首之国家。吾之民族若国家，果为青春之民族、青春之国家欤，抑为白首之民族、白首之国家欤？苟已成白首之民族、白首之国家焉，吾辈青年之谋所以致之回春为之再造者，又应以何等信力与愿力从事，而克以著效？此则系乎青年之自觉何如耳。异族之觇吾国者，辄曰：支那者老大之邦也。支那之民族，濒灭之民族也。支那之国家，待亡之国家也。洪荒而后，民族若国家之递兴递亡者，然其不可纪矣。粤稽西史，罗马、巴比伦之盛时，丰功伟烈，彪著寰宇，曾几何时，一代声华，都成尘土矣。祗今屈指，欧土名邦，若意大利，若法兰西，若西班牙，若葡萄牙，若和兰，若比利时，若丹马，若瑞典，若那威，乃至若英吉利，罔不有积尘之历史，以重累其国家若民族之生命。回溯往祀，是等国族，固皆尝有其青春之期，以其畅盛之生命，展其特殊之天才。而今已矣，声华渐落，躯壳空存，纷纷者皆成文明史上之过客矣。其校新者，惟德意志与勃牙利，此次战血洪涛中，又为其生命力之所注，勃然暴发，以挥展其天才矣。由历史考之，新兴之国族与陈腐之国族遇，陈腐者必败；朝气横溢之生命力与死灰沉滞之生命力遇，死灰沉滞者必败；青春之国民与白首之国民遇，白首者必败，此殆天演公例，莫或能逃者也。支那自黄帝以降，赫赫然树独立之帜于亚东大陆者，四千八百余年于兹矣。历世久远，纵观横览，罕有其伦。稽其民族青春之期，远在有周之世，典章文物，灿然大备，过此以往，渐向衰歇之运，然犹浸衰浸微，扬其余辉。以至于今日者，得不谓为其民族之光欤？夫人寿之永，不过百年，民族之命，垂五千载，斯亦寿之至也。印度为生释迦而兴，故自释迦生而印度死；犹太为生耶稣而立，故自耶稣生而犹太亡；支那为生孔子而建，故自孔子生而支那衰，陵夷至于今日，残骸枯骨，满目黤然，民族之精英，澌灭尽矣，而欲不亡，庸可得乎？吾青年之骤闻斯言者，未有不变色裂眦，怒其侮我之甚也。虽然，勿怒也。吾之国族，已阅长久之历史，而此长久之历史，积尘重压，以桎梏其生命而臻于衰敝者，又宁容讳？然而吾族青年所当信誓旦旦，以昭示于世者，不在龈龈辩证白首中国之不死，乃在汲汲孕育青春中国之再生。吾族今后之能否立足于世界，不在白首中国之苟延残喘，而在青春中国之投胎复活。盖尝闻之，生命者，死与再生之连续也。今后人类之问题，民族之问题，非苟生残存之问题，乃复活更生、回春再造之问题也。与吾并称为老大帝国之土耳其，则青年之政治运动，屡试不一试焉。巴尔干诸邦，则各谋离土自立，而为民族之运动，兵连祸结，干戈频兴，卒以酿今兹世界之大变焉。遥望喜马拉亚山之巅，恍见印度革命之烽烟一缕，引而弥长，是亦欲回其民族之青春也。吾华自辛亥首义，癸丑之役继之，喘息未安，风尘澒洞，又复倾动九服，是亦欲再造其神州也。而在是等国族，凡以冲决历史之桎梏，涤荡历史之积秽，新造民族之生命，挽回民族之青春者，固莫不惟其青年是望矣。建国伊始，肇锡嘉名，实维中华。中华之义，果何居乎？中者，宅中位正之谓也。吾辈青年之大任，不仅以于空间能致中华为天下之中而遂足，并当于时间而谛时中之旨也。旷观世界之历史，古往今来，变迁何极！吾人当于今岁之青春，画为中点，中以前之历史，不过如进化论仅于考究太阳地球动植各物乃至人类之如何发生、如何进化者，以纪人类民族国家之如何发生、如何进化也。中以后之历史，则以是为古代史之职，而别以纪人类民族国家之更生回春为其中心之的也。中以前之历史，封闭之历史，焚毁之历史，葬诸坟墓之历史也。中以后之历史，洁白之历史，新装之历史，待施绚绘之历史也。中以前之历史，白首之历史，陈死人之历史也。中以后之历史，青春之历史，活青年之历史也。青年乎！其以中立不倚之精神，肩兹砥柱中流之责任，即由今年今春之今日今刹那为时中之起点，取世界一切白首之历史，一火而摧焚之，而专以发挥青春中华之中，缀其一生之美于中以后历史之首页，为其职志，而勿逡巡不前。华者，文明开敷之谓也，华与实相为轮回，即开敷与废落相为嬗代。白首中华者，青春中华本以胚孕之实也。青春中华者，白首中华托以再生之华也。白首中华者，渐即废落之中华也。青春中华者，方复开敷之中华也。有渐即废落之中华，所以有方复开敷之中华。有前之废落以供今之开敷，斯有后之开敷以续今之废落，即废落，即开敷，即开敷，即废落，终竟如是废落，终竟如是开敷。宇宙有无尽之青春，斯宇宙有不落之华，而栽之、培之、灌之、溉之、赏玩之、享爱之者，舍青春中华之青年，更谁为归矣？青年乎，勿徒发愿，愿春常在华常好也，愿华常得青春，青春常在于华也。宜有即华不得青春，青春不在于华，亦必奋其回春再造之努力，使废落者复为开敷，开敷者终不废落，使华不能不得青春，青春不能不在于华之决心也。抑吾闻之化学家焉，土质虽腴，肥料虽多，耕种数载，地力必耗，砂土硬化，无能免也，将欲柔融之，俾再反于丰穰，惟有一种草木为能致之，为其能由空中吸收窒素肥料，注入土中而沃润之也。神州赤县，古称天府，胡以至今徒有万木秋声、萧萧落叶之悲，昔时繁华之盛，荒凉废落至于此极也！毋亦无此种草木为之交柔和润之耳。青年之于社会，殆犹此种草木之于田也。从此广植根蒂，深固不可复拔，不数年间，将见青春中华之参天蓊郁，错节盘根，树于世界，而神州之域，还其丰穰，复其膏腴矣。则谓此菁菁茁茁之青年，即此方复开敷之青春中华可也。",
                    "顾人之生也，苟不能窥见宇宙有无尽之青春，则自呱呱堕地，迄于老死，觉其间之春光，迅于电波石火，不可淹留，浮生若梦，直菌鹤马蜩之过乎前耳。是以川上尼父，有逝者如斯之嗟，湘水灵均，兴春秋代序之感。其他风骚雅士，或秉烛夜游，勤事劳人，或重惜分寸。而一代帝王，一时豪富，当其垂暮之年，绝诀之际，贪恋幸福，不忍离舍，每为咨嗟太息，尽其权力黄金之用，无能永一瞬之天年，而重留遗憾于长生之无术焉。秦政并吞八荒，统制四海，固一世之雄也，晚年畏死，遍遣羽客，搜觅神仙，求不老之药，卒未能获，一旦魂断，宫车晚出。汉武穷兵，蛮荒慑伏，汉代之英主也，暮年永叹，空有“欢乐极矣哀情多，少壮几时老奈何”之慨。最近美国富豪某，以毕生之奋斗，博得式之王冠，衰病相催，濒于老死，则抚枕而叹曰：“苟能延一月之命，报以千万金弗惜也。”然是又安可得哉？夫人之生也有限，其欲也无穷，以无穷之欲，逐有限之生，坐令似水年华，滔滔东去，红颜难再，白发空悲，其殆人之无奈无何者欤！涉念及此，灰肠断气，灰世之思，油然而生。贤者仁智俱穷，不肖者流连忘返，而人生之蕲向荒矣，是又岂青年之所宜出哉？人生兹世，更无一刹那不在青春，为其居无尽青春之一部，为无尽青春之过程也。顾青年之人，或不得常享青春之乐者，以其有黄金权力一切烦忧苦恼机械生活，为青春之累耳。谚云：“百金买骏马，千金买美人，万金买爵禄，何处买青春？”岂惟无处购买，邓氏铜山，郭家金穴，愈有以障青春之路俾无由达于其境也。罗马亚布达尔曼帝，位在皇极，富有四海，不可谓不尊矣，临终语其近侍，谓四十年间，真感愉快者，仅有三日。权力之不足福人，以视黄金，又无差等。而以四十年之青春，娱心不过三日，悼心悔憾，宁有穷耶？夫青年安心立命之所，乃在循今日主义以进，以吾人之生，洵如卡莱尔所云，特为时间所执之无限而已。无限现而为我，乃为现在，非为过去与将来也。苟了现在，即了无限矣。昔者圣叹作诗，有“何处谁人玉笛声”之句。释弓年小，窃以玉字为未安，而质之圣叹。圣叹则曰：“彼若说‘我所吹本是铁笛，汝何得用作玉笛’。我便云：”我已用作玉笛，汝何得更吹铁笛？‘天生我才，岂为汝铁笛作奴儿婢子来耶？“夫铁字与玉字，有何不可通融更易之处。圣叹顾与之争一字之短长而不惮烦者，亦欲与之争我之现在耳。诗人拜轮，放浪不羁，时人诋之，谓于来世必当酷受地狱之苦。拜轮答曰：”基督教徒自苦于现世，而欲祈福于来世。非基督教徒，则于现世旷逸自遣，来世之苦，非所辞也。“二者相校，但有先后之别，安有分量之差。拜轮此言，固甚矫激，且寓风刺之旨。以余观之，现世有现世之乐，来世有来世之乐。现世有现世之青春，来世有来世之青春。为贪来世之乐与青春，而迟吾现世之乐与青春，固所不许。而为贪现世之乐与青春，遽弃吾来世之乐与青春，亦所弗应也。人生求乐，何所不可，亦何必妄分先后，区异今来也？耶曼孙曰：”尔若爱千古，当利用现在。昨日不能呼还，明日尚未确实。尔能确有把握者，惟有今日。今日之一日，适当明晨之二日。“斯言足发吾人之深省矣。盖现在者吾人青春中之青春也。青春作伴以还于大漠之乡，无如而不自得，更何烦忧之有焉。烦忧既解，恐怖奚为？耶比古达士曰：”贫不足恐，流窜不足恐，囹圄不足恐，最可恐者，恐怖其物也。“美之政雄罗斯福氏，解政之后，游猎荒山，奋其銕腕，以与虎豹熊罴相搏战。一日猎白熊，险遭吞噬，自传其事，谓为不以恐怖误其稍纵即逝之机之效，始获免焉。于以知恐怖为物，决不能拯人于危。苟其明日将有大祸临于吾躬，无论如何恐怖，明日之祸万不能因是而减其豪末。而今日之我，则因是而大损其气力，俾不足以御明日之祸而与之抗也。艰虞万难之境，横于吾前，吾惟有我、有我之现在而足恃。堂堂七尺之躯，徘徊回顾，前不见古人，后不见来者，惟有昂头阔步，独往独来，何待他人之援手，始以遂其生者，更胡为乎念天地之悠悠，独怆然而涕下哉？惟足为累于我之现在及现在之我者，机械生活之重荷，与过去历史之积尘，殆有同一之力焉。今人之赴利禄之途也，如蚁之就膻，蛾之投火，究其所企，克致志得意满之果，而营营扰扰，已逾半生，以孑然之身，强负黄金与权势之重荷以趋，几何不为所重压而僵毙耶？盖其优于权富即其短于青春者也。耶经有云：”富人之欲入天国，犹之骆驼欲潜身于针孔。“此以喻重荷之与青春不并存也。总之，青年之自觉，一在冲决过去历史之网罗，破坏陈腐学说之囹圄，勿令僵尸枯骨，束缚现在活泼泼地之我，进而纵现在青春之我，扑杀过去青春之我，促今日青春之我，禅让明日青春之我。一在脱绝浮世虚伪之机械生活，以特立独行之我，立于行健不息之大机轴。祖裼裸裎，去来无罫，全其优美高尚之天，不仅以今日青春之我，追杀今日白首之我，并宜以今日青春之我，豫杀来日白首之我，此固人生唯一之蕲向，青年唯一之责任也矣。拉凯尔曰：”长保青春，为人生无上之幸福，尔欲享兹幸福，当死于少年之中。“吾愿吾亲爱之青年，生于青春死于青春，生于少年死于少年也。德国史家孟孙氏，评骘锡札曰：”彼由青春之杯，饮人生之水，并泡沫而干之。“吾愿吾亲爱之青年，擎此夜光之杯，举人生之醍醐浆液，一饮而干也。人能如是，方为不役于物，物莫之伤。大浸稽天而不溺，大旱金石流土山焦而不热，是其尘垢粃糠，将犹陶铸尧、舜。自我之青春，何能以外界之变动而改易，历史上残骸枯骨之灰，又何能塞蔽青年之聪明也哉？市南宜僚见鲁侯，鲁侯有忧色，市南子乃示以去累除忧之道，有曰，”吾愿君去国捐俗，与道相辅而行。“君曰：”彼其道远而险，又有江山，我无舟车，奈何？“市南子曰：”君无形倨，无留居，以为舟车。“君曰：”彼其道幽远而无人，吾谁与为邻？吾无粮，我无食，安得而至焉？“市南子曰：”少君之费，寡君之欲，虽无粮而乃足，君其涉于江而浮于海，望之而不见其崖，愈往而不知其所穷，送君者将自崖而反，君自此远矣。“此其谓道，殆即达于青春之大道。",
                    "青年循蹈乎此，本其理性，加以努力，进前而勿顾后，背黑暗而向光明，为世界进文明，为人类造幸福，以青春之我，创建青春之家庭，青春之国家，青春之民族，青春之人类，青春之地球，青春之宇宙，资以乐其无涯之生。乘风破浪，迢迢乎远矣，复何无计留春望尘莫及之忧哉？吾文至此，已嫌冗赘，请诵漆园之语，以终斯篇。"
            };
            for (String s : pArr) {
                ofdDoc.add(new Paragraph(s, 5d).setFirstLineIndent(2));
            }
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }
}
