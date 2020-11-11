package org.ofdrw.layout.cases.content;

import org.junit.jupiter.api.Test;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.layout.element.Span;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 段落测试用例
 *
 * @author 权观宇
 * @since 2020-11-09 21:40:04
 */
public class ParagraphCase {

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


}
