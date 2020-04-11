package org.ofdrw.core.basicType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-04-11 12:42:07
 */
class ST_LocTest {

    @Test
    void parent() {

        ST_Loc loc = new ST_Loc("Doc_9/Pages/Page_0/Content.xml");
        assertEquals("Doc_9/Pages/Page_0", loc.parent());

        loc = new ST_Loc("Content.xml");
        assertEquals("", loc.parent());
    }
}