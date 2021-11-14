package org.ofdrw.tool.merge;

import org.dom4j.*;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.res.OFDResource;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicStructure.res.resources.CompositeGraphicUnits;
import org.ofdrw.core.compositeObj.CT_VectorG;
import org.ofdrw.pkg.tool.ElemCup;

import static org.junit.jupiter.api.Assertions.*;

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
        Path src = Paths.get("src/test/resources/Content.xml");
        final Element element = ElemCup.inject(src);
        Res page = new Res(element);
        final List<OFDResource> resources = page.getResources();
        final CompositeGraphicUnits cgArr = (CompositeGraphicUnits) resources.get(1);
        final CT_VectorG g = new CT_VectorG((Element) cgArr.getCompositeGraphicUnits().get(0).clone());
        final Document document = DocumentHelper.createDocument();
        document.add(g);
        XPath xpathSelector = DocumentHelper.createXPath("//*[@Font]");
        List<Node> nodes = xpathSelector.selectNodes(g);
        assertEquals(1, nodes.size());
//            nodes = page.selectNodes("//*[@Font]");
//            final Node node = nodes.get(0);
//            if (node instanceof Element) {
//                System.out.println(node);
//            }
//            assertEquals(13, nodes.size());
//            nodes = page.selectNodes("//*[@DrawParam]");
//            assertEquals(0, nodes.size());


    }
}
