package org.ofdrw.core.signatures.sig;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-06-19 13:46:41
 */
class ParameterTest {

    @Test
    public void testMarshal() throws Exception {
        Parameter p = new Parameter("Key", "string", "Value");
        TestTool.genXml("Parameter", p);
        p = new Parameter("Key", "Value");
        TestTool.genXml("Parameter", p);
    }
}