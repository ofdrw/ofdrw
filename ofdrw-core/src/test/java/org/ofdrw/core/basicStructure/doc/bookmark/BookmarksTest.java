package org.ofdrw.core.basicStructure.doc.bookmark;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.action.actionType.actionGoto.CT_Dest;
import org.ofdrw.core.action.actionType.actionGoto.CT_DestTest;

import static org.junit.jupiter.api.Assertions.*;

public class BookmarksTest {

    public static Bookmarks bookmarksCase() {
        return new Bookmarks()
                .addBookmark(new Bookmark("Mark1", CT_DestTest.destCase()))
                .addBookmark(new Bookmark("Mark2", CT_DestTest.destCase()));
    }


    @Test
    public void gen() throws Exception {
        TestTool.genXml("Bookmarks", bookmarksCase());
    }
}