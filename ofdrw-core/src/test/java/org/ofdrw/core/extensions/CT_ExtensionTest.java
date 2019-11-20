package org.ofdrw.core.extensions;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_RefID;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class CT_ExtensionTest {

    public static CT_Extension extensionCase(){
        return new CT_Extension()
                .setAppName("ofdrw")
                .setCompany("ofdrw.org")
                .setAppVersion("1.0.0-SNAPSHOT")
                .setDate(LocalDate.now())
                .setRefId(ST_RefID.getInstance("724"))
                .addProperty(new Property("P1", "value1"))
                .addProperty(new Property("P2", "value2"));
    }
    
    @Test
    public void gen() throws Exception {
        TestTool.genXml("CT_Extension", extensionCase());
    }
}