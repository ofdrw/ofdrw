package org.ofdrw.reader;

import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.pkg.container.ResDir;
import org.ofdrw.pkg.container.VirtualContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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