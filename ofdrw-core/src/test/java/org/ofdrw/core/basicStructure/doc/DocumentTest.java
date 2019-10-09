package org.ofdrw.core.basicStructure.doc;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.action.ActionsTest;
import org.ofdrw.core.basicStructure.doc.bookmark.BookmarksTest;
import org.ofdrw.core.basicStructure.doc.permission.CT_PermissionTest;
import org.ofdrw.core.basicStructure.doc.vpreferences.CT_VPreferencesTest;
import org.ofdrw.core.basicStructure.outlines.OutlinesTest;
import org.ofdrw.core.basicStructure.pageTree.tree.PagesTest;
import org.ofdrw.core.basicType.ST_Loc;

public class DocumentTest {
    public static Document documentCase() {
        return new Document()
                .setCommonData(CT_CommonDataTest.commonDataCase())
                .setPages(PagesTest.pagesCase())
                .setOutlines(OutlinesTest.outlinesCase())
                .setPermissions(CT_PermissionTest.permissionCase())
                .setActions(ActionsTest.actionsCase())
                .setVPreferences(CT_VPreferencesTest.vPreferencesCase())
                .setBookmarks(BookmarksTest.bookmarksCase())
                .setAnnotations(ST_Loc.getInstance("./Res/Annotations.xml"))
                .setCustomTags(ST_Loc.getInstance("./Res/CustomTags.xml"))
                .setAttachments(ST_Loc.getInstance("./Attachments.xml"))
                .setExtensions(ST_Loc.getInstance("./Res/Extensions.xml"));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("Document", documentCase());
    }

}