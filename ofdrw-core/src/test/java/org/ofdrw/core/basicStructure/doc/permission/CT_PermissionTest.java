package org.ofdrw.core.basicStructure.doc.permission;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class CT_PermissionTest {
    public static CT_Permission permissionCase() {
        return new CT_Permission()
                .setEdit(true)
                .setAnnot(true)
                .setEdit(true)
                .setSignature(true)
                .setWatermark(true)
                .setPrintScreen(true)
                .setPrint(new Print(true, 777))
                .setValidPeriod(
                        new ValidPeriod(LocalDateTime.now().minusDays(30),
                                LocalDateTime.now()));
    }


    @Test
    public void gen(){
        TestTool.genXml("Permission", permissionCase());
    }
}