package org.ofdrw.pkg.dir.content;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.pageObj.Content;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.pkg.dir.TT;
import org.ofdrw.pkg.tool.DocObjDump;

import java.nio.file.Files;
import java.nio.file.Path;

public class PageContent {

    private static Content content() {
        TextCode hello = new TextCode()
                .setCoordinate(0.6747, 3.5101)
                .setDeltaX(1.95, 1.95, 0.8, 0.8, 1.95, 1.95, 1.95, 1.95, 1.95, 1.95, 1.95,0.8)
                .setContent("hello ofdrw!");
        TextObject textObj = new TextObject(3);
        textObj.setBoundary(31.0753, 25.9115, 25, 5)
                .setFont(4)
                .setSize(3.6865)
                .addTextCode(hello);
        CT_Layer layer = new CT_Layer();
        layer.addPageBlock(textObj)
                .setObjID(2);

        return new Content()
                .addLayer(layer);
    }

    public static Page page() {
        return new Page()
                .setArea(new CT_PageArea(0, 0, 210, 297))
                .setContent(content());
    }


    @Test
    public void printReview() throws Exception {
        Page page = page();
        TT.dumpToTmpReview(page);
    }
}
