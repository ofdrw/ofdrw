package org.ofdrw.core.basicStructure.page.tree;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class PagesTest {

    public static Pages pagesCase() {
        return new Pages()
                .addPage(new Page(1, "Pages/Page_0/Content.xml"))
                .addPage(new Page(2, "Pages/Page_1/Content.xml"));
    }

    @Test
    public void gen() {
        TestTool.genXml("PagesTree", pagesCase());
    }
}