package org.ofdrw.core.signatures.sig;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-06-19 13:48:56
 */
class ParametersTest {
    public static Parameters ParametersCase(){
        return new Parameters()
        .addParameter(new Parameter("KEY1", "太棒了"))
        .addParameter(new Parameter("KEY2", "Number", "777"));

    }

    @Test
    public void testRemoveParameter(){
        Parameters p = ParametersCase();
        Parameter res = p.removeParameter(null);
        assertNull(res);
        res = p.removeParameter("KEY_NOT_EXIST");
        assertNull(res);
        res = p.removeParameter("KEY2");
        assertNotNull(res);
    }

    @Test
    public void testMarshall() throws Exception {
        Parameters p = ParametersCase();
        TestTool.genXml("Parameters", p);

        p = ParametersCase();
        p.addParameter(new Parameter("KEY1", "GREAT"));
        TestTool.genXml("Parameters", p);

    }
}