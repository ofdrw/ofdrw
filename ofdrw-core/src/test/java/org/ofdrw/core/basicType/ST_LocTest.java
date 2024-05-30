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

    @Test
    void getFileName(){
        ST_Loc loc = new ST_Loc("Doc_9/Pages/Page_0/Content.xml");
        assertEquals("Content.xml", loc.getFileName());

        loc = new ST_Loc("Content.xml");
        assertEquals("Content.xml", loc.getFileName());

        loc = new ST_Loc("Doc_9/");
        assertEquals("", loc.getFileName());
    }

    @Test
    void cat(){
        ST_Loc part1 = new ST_Loc("Doc_9/Pages/Page_0/");
        ST_Loc part2 = new ST_Loc("/Content.xml");
        assertEquals("Doc_9/Pages/Page_0/Content.xml", part1.cat(part2).getLoc());

        part1 = new ST_Loc("Doc_9/Pages/Page_0");
        part2 = new ST_Loc("Content.xml");
        assertEquals("Doc_9/Pages/Page_0/Content.xml", part1.cat(part2).toString());

        part1 = new ST_Loc("Doc_9/Pages/Page_0/");
        part2 = new ST_Loc("Content.xml");
        assertEquals("Doc_9/Pages/Page_0/Content.xml", part1.cat(part2).toString());

        part1 = new ST_Loc("Doc_9/Pages/Page_0");
        part2 = new ST_Loc("/Content.xml");
        assertEquals("Doc_9/Pages/Page_0/Content.xml", part1.cat(part2).toString());

        part1 = new ST_Loc("/");
        part2 = new ST_Loc("Content.xml");
        assertEquals("/Content.xml", part1.cat(part2).toString());
    }

    @Test
    void parts() {

        ST_Loc loc = new ST_Loc("Doc_9/Pages/Page_0/Content.xml");
        String[] parts = loc.parts();
        assertArrayEquals(new String[]{"Doc_9", "Pages", "Page_0", "Content.xml"}, parts);

        loc = new ST_Loc("Content.xml");
        parts = loc.parts();
        assertArrayEquals(new String[]{"Content.xml"}, parts);

        loc = new ST_Loc("Doc_9/");
        parts = loc.parts();
        assertArrayEquals(new String[]{"Doc_9"}, parts);


        loc = new ST_Loc("/Doc_0/Pages/Page_0/Content.xml");
        parts = loc.parts();
        assertArrayEquals(new String[]{"Doc_0", "Pages", "Page_0", "Content.xml"}, parts);


        loc = new ST_Loc("/Doc_0//Pages/   /Page_0/Content.xml//");
        parts = loc.parts();
        assertArrayEquals(new String[]{"Doc_0", "Pages", "Page_0", "Content.xml"}, parts);

        loc = new ST_Loc("/// ///      //////   /////");
        parts = loc.parts();
        assertArrayEquals(new String[]{}, parts);
    }
}