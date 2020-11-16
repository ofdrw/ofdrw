package org.ofdrw.reader.keyword;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 关键字抽取 调用示例
 *
 * @author 权观宇
 * @since 2020-10-13 19:27:52
 */
class KeywordExtractorTest {

    /**
     * 获取关键字在文档中坐标
     */
    @Test
    void getKeyWordPositionList() throws IOException, DocumentException {

        Path src = Paths.get("src/test/resources/keyword.ofd");
        String keyword = "办理";

        try (OFDReader reader = new OFDReader(src)) {
            List<KeywordPosition> positionList = KeywordExtractor.getKeyWordPositionList(reader, keyword);
            assertEquals(positionList.size(), 1);
            KeywordPosition keywordPos = positionList.get(0);
            assertEquals(keywordPos.getPage(), 1);
            ST_Box box = keywordPos.getBox();
            // 误差保持在0.1
            assertEquals("131.9", String.format("%.1f", box.getTopLeftX()));
            assertEquals("87.9", String.format("%.1f", box.getTopLeftY()));
            assertEquals("22.3", String.format("%.1f", box.getWidth()));
            assertEquals("22.3", String.format("%.1f", box.getHeight()));
        }
    }

    /**
     * 获取关键字在文档中坐标
     */
    @Test
    void getMultiKeyWordPositionList() throws IOException, DocumentException {
        Path src = Paths.get("src/test/resources/keyword.ofd");
        String[] keywords = {"办理", "不动产权"};

        try (OFDReader reader = new OFDReader(src)) {
            List<KeywordPosition> positionList = KeywordExtractor.getKeyWordPositionList(reader, keywords);
            assertEquals(positionList.size(), 2);
            for (KeywordPosition keywordPosition : positionList) {
                System.out.println(keywordPosition);
            }
        }
    }
}