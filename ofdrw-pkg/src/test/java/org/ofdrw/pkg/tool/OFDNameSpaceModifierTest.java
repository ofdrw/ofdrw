package org.ofdrw.pkg.tool;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-07-27 20:01:26
 */
class OFDNameSpaceModifierTest {

    @Test
    void visit() throws DocumentException {
        String src = "src/test/resources/namespace_case.xml";
        SAXReader reader = new SAXReader();
        Document document = reader.read(src);

        // 修改已经存在的命名空间为指定命名空间
        document.accept(new OFDNameSpaceModifier());
        System.out.println(document.asXML());
    }
}