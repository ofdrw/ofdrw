package org.ofdrw.core.pageDescription.clips;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.text.text.CT_Text;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-10-15 19:13:06
 */
class ClipAbleTest {

    @Test
    void getInstance() {
        CT_Path ctPath = new CT_Path();
        ClipAble ca = ClipAble.getInstance(ctPath);
        assertNotNull(ca);

        CT_Text ctText = new CT_Text();
        ca = ClipAble.getInstance(ctText);
        assertNotNull(ca);
    }
}