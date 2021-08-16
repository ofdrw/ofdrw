package org.ofdrw.reader;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.reader.extractor.ExtractorFilter;
import org.ofdrw.reader.extractor.RegionTextExtractorFilter;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author 权观宇
 * @since 2020-09-21 23:09:24
 */
class ContentExtractorTest {
    private Path src = Paths.get("src/test/resources/helloworld.ofd");

    @Test
    void getPageContent() throws IOException {
        try (OFDReader reader = new OFDReader(src)) {
            ContentExtractor extractor = new ContentExtractor(reader);

            List<String> pageContent = extractor.getPageContent(1);
            System.out.println(pageContent);
            assertEquals(pageContent.size(), 1);
            assertEquals("你好呀，OFD Reader&Writer！", pageContent.get(0));
        }
    }

    @Test
    void extractByFilter() throws IOException {
        try (OFDReader reader = new OFDReader("src/test/resources/keyword.ofd")) {
            CT_PageArea area = reader.getPage(1).getArea();
            System.out.println(area.getPhysicalBox());
            Rectangle rectangle = new Rectangle(0, 0, 283, 120);
            ExtractorFilter filter = new RegionTextExtractorFilter(rectangle);
            ContentExtractor extractor = new ContentExtractor(reader, filter);

            List<String> pageContent = extractor.getPageContent(1);
            System.out.println(pageContent);
        }
    }

    @Test
    void extractAll() throws IOException {
        try (OFDReader reader = new OFDReader(src)) {
            ContentExtractor extractor = new ContentExtractor(reader);

            List<String> pageContent = extractor.extractAll();
            System.out.println(pageContent);
            assertEquals(pageContent.size(), 1);
            assertEquals("你好呀，OFD Reader&Writer！", pageContent.get(0));
        }
    }

    /**
     * 含有PageBlock包裹的对象的文字提取测试
     *
     * @throws IOException
     */
    @Test
    void extractAllPageBlock() throws IOException {
        Path src = Paths.get("src/test/resources/helloworld_with_pageblock.ofd");
        try (OFDReader reader = new OFDReader(src)) {
            ContentExtractor extractor = new ContentExtractor(reader);

            List<String> pageContent = extractor.extractAll();
            System.out.println(pageContent);
            assertEquals(pageContent.size(), 1);
            assertEquals("你好呀，OFD Reader&Writer！", pageContent.get(0));
        }
    }


    @Test
    void traverse() throws IOException {
        try (OFDReader reader = new OFDReader(src)) {
            ContentExtractor extractor = new ContentExtractor(reader);
            extractor.traverse((pageNum, contents) -> {
                // 在这里你可以做些你喜欢的事情
                assertEquals(contents.size(), 1);
                assertEquals("你好呀，OFD Reader&Writer！", contents.get(0));
            });
        }
    }
}