package org.ofdrw.core.attachment;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Loc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class CT_AttachmentTest {
    public static CT_Attachment attachmentCase(){
        return new CT_Attachment()
                .setID("10004")
                .setAttachmentName("File.txt")
                .setFormat("Text")
                .setCreationDate(LocalDateTime.now())
                .setSize(4D)
                .setFileLoc(new ST_Loc("/Doc_0/Res/File.txt"));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("CT_Attachment", attachmentCase());
    }

}