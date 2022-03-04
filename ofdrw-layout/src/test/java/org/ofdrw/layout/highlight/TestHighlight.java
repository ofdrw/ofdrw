package org.ofdrw.layout.highlight;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.edit.AdditionVPage;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.Position;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.keyword.KeywordExtractor;
import org.ofdrw.reader.keyword.KeywordPosition;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 高亮 显示关键字定位
 *
 * @author 权观宇
 * @since 2022-03-04 23:02:08
 */
public class TestHighlight {

    /**
     * 关键自定位 绘制图形
     */
    @Test
    public void testLocKW() throws IOException, DocumentException {

        //签署文档路径
        Path src = Paths.get("src/test/resources/keyword2.ofd");
        // 签署输出路径
        Path out = Paths.get("target/put_rect.ofd");

        String[] keyword = {"销售方", "价金", "项目名称"};
        try (OFDReader reader = new OFDReader(src)) {
            List<KeywordPosition> positionList = KeywordExtractor.getKeyWordPositionList(reader, keyword);
            try (OFDDoc ofdDoc = new OFDDoc(reader, out)) {
                final AdditionVPage vPage = ofdDoc.getAVPage(1);
                for (KeywordPosition pos : positionList) {
                    Div e = new Div(pos.getBox().getWidth(), pos.getBox().getHeight())
                            .setPosition(Position.Absolute)
                            .setX(pos.getBox().getTopLeftX()).setY(pos.getBox().getTopLeftY())
                            .setBackgroundColor(0, 255, 0)
                            .setOpacity(0.5d);
                    vPage.add(e);
                }
            }
        }
        System.out.println(">> " + out.toAbsolutePath());
    }

    /**
     * 关键自定位 绘制图形
     */
    @Test
    public void testLocKW2() throws IOException, DocumentException {

        //签署文档路径
        Path src = Paths.get("src/test/resources/keyword.ofd");
        // 签署输出路径
        Path out = Paths.get("target/put_rect_2.ofd");

        String[] keywords = {"办理", "不动产权"};
        try (OFDReader reader = new OFDReader(src)) {
            List<KeywordPosition> positionList = KeywordExtractor.getKeyWordPositionList(reader, keywords);
            try (OFDDoc ofdDoc = new OFDDoc(reader, out)) {
                final AdditionVPage vPage = ofdDoc.getAVPage(1);
                for (KeywordPosition pos : positionList) {
                    Div e = new Div(pos.getBox().getWidth(), pos.getBox().getHeight())
                            .setPosition(Position.Absolute)
                            .setX(pos.getBox().getTopLeftX()).setY(pos.getBox().getTopLeftY())
                            .setBackgroundColor(0, 255, 0)
                            .setOpacity(0.5d);
                    vPage.add(e);
                }
            }
        }
        System.out.println(">> " + out.toAbsolutePath());
    }
}
