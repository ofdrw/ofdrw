package org.ofdrw.layout.areaholder;

import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;


class AreaHolderBlocksTest {
    @Test
    public void testMarshall()throws Exception{
        AreaHolderBlocks arrs = new AreaHolderBlocks();
        CT_AreaHolderBlock obj = new CT_AreaHolderBlock("UserName");
        obj.setBoundary(new ST_Box(10, 10, 100, 100));
        obj.setPageBlockID(new ST_RefID(233));
        obj.setPageFile(ST_Loc.getInstance("/Doc_0/Pages/Page_0/Content.xml"));
        arrs.addAreaHolderBlock(obj);

        XMLWriter writerToSout = new XMLWriter(System.out, OutputFormat.createPrettyPrint());
        writerToSout.write(arrs);
        writerToSout.flush();
    }
}