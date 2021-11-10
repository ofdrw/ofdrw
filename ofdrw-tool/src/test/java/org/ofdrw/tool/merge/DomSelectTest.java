package org.ofdrw.tool.merge;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.pageObj.Page;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author 权观宇
 * @since 2021-11-10 21:18:24
 */
public class DomSelectTest {

    @Test
    public void testSelectAttrID() throws Exception {
        Path src = Paths.get("src/test/resources/page_content.xml");
        SAXReader reader = new SAXReader();
        try ( InputStream in = Files.newInputStream(src)) {
            Document document = reader.read(in);
            Page page = new Page(document.getRootElement());
            List<Node> nodes = page.selectNodes("//*[@ResourceID]");
            assertEquals(2, nodes.size());
            nodes = page.selectNodes("//*[@Font]");
            assertEquals(13, nodes.size());
        }

    }
}
