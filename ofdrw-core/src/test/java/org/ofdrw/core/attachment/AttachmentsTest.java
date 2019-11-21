package org.ofdrw.core.attachment;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class AttachmentsTest {
    public static Attachments attachmentsCase(){
        return new Attachments()
                .addAttachment(CT_AttachmentTest.attachmentCase());
    }
    
    @Test
    public void gen() throws Exception {
        TestTool.genXml("Attachments", attachmentsCase());
    }
}