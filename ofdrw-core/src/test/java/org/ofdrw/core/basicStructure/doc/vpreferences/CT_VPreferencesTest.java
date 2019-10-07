package org.ofdrw.core.basicStructure.doc.vpreferences;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicStructure.doc.vpreferences.zoom.ZoomMode;

public class CT_VPreferencesTest {
    public static CT_VPreferences vPreferencesCase() {
        return new CT_VPreferences()
                .setPageMode(PageMode.None)
                .setPageLayout(PageLayout.OneColumn)
                .setTabDisplay(TabDisplay.FileName)
                .setHideToolbar(false)
                .setHideMenubar(false)
                .setHideWindowUI(false)
                .setZoomMode(ZoomMode.getInstance(ZoomMode.Type.FitWidth));
    }

    @Test
    public void gen(){
        TestTool.genXml("VPreferences", vPreferencesCase());
        TestTool.genXml("VPreferences2", vPreferencesCase().setZoom(3.5));

    }
}