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
        TextCode tcENTxt = new TextCode()
                .setCoordinate(0d, 25d)
                .setDeltaX(14d, 14d, 14d)
                .setContent("Font");
        TextCode tcSTTxt = new TextCode()
                .setCoordinate(60d, 25d)
                .setDeltaX(25d)
                .setContent("字形");
        TextObject textObj = new TextObject(3);
        textObj.setBoundary(50, 20, 112, 26)
                .setFont(4)
                .setSize(25.4)
                .addTextCode(tcENTxt)
                .addTextCode(tcSTTxt);
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
