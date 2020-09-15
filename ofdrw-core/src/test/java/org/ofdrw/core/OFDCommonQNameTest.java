package org.ofdrw.core;

import org.dom4j.Element;
import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试通用命名空间
 *
 * @author 权观宇
 * @since 2020-09-15 21:21:08
 */
class OFDCommonQNameTest {


    private static final String CASE1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ofd:OFD xmlns:ofd=\"http://www.ofdspec.org/2016\" Version=\"1.0\" DocType=\"OFD\"><ofd:DocBody></ofd:DocBody></ofd:OFD>";
    private static final String CASE2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ofd:OFD xmlns:ofd=\"http://www.ofdspec.com/2016\" Version=\"1.0\" DocType=\"OFD\"><ofd:DocBody></ofd:DocBody></ofd:OFD>";
    private static final String CASE3 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ofd:OFD xmlns:ofd=\"http://www.ofdspec.org\" Version=\"1.0\" DocType=\"OFD\"><ofd:DocBody></ofd:DocBody></ofd:OFD>";
    private static final String CASE4 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ofd:OFD xmlns:ofd=\"http://www.ofdspec.org/\" Version=\"1.0\" DocType=\"OFD\"><ofd:DocBody></ofd:DocBody></ofd:OFD>";
    private static final String CASE5 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ofd:OFD xmlns:ofd=\"http://www.ofdspec.org/2020\" Version=\"1.0\" DocType=\"OFD\"><ofd:DocBody></ofd:DocBody></ofd:OFD>";


    @Test
    public void testParse() {
        TestTool.validateWithXML(CASE1, root -> {
            OFDCommonQName qName = new OFDCommonQName("DocBody");
            List<Element> elements = root.elements(qName);
            assertFalse(elements.isEmpty());
        });

        TestTool.validateWithXML(CASE2, root -> {
            OFDCommonQName qName = new OFDCommonQName("DocBody");
            List<Element> elements = root.elements(qName);
            assertTrue(elements.isEmpty());
        });

        TestTool.validateWithXML(CASE3, root -> {
            OFDCommonQName qName = new OFDCommonQName("DocBody");
            List<Element> elements = root.elements(qName);
            assertFalse(elements.isEmpty());
        });

        TestTool.validateWithXML(CASE4, root -> {
            OFDCommonQName qName = new OFDCommonQName("DocBody");
            List<Element> elements = root.elements(qName);
            assertFalse(elements.isEmpty());
        });

        TestTool.validateWithXML(CASE5, root -> {
            OFDCommonQName qName = new OFDCommonQName("DocBody");
            List<Element> elements = root.elements(qName);
            assertFalse(elements.isEmpty());
        });
    }
}