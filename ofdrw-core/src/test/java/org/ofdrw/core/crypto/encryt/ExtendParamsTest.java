package org.ofdrw.core.crypto.encryt;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-06-23 19:50:04
 */
class ExtendParamsTest {

    @Test
    void addParameter() {
        ExtendParams extendParams = new ExtendParams();
        extendParams.add(new Parameter("KEY", "VALUE"));
        extendParams.add(new Parameter("KEY2"));
        extendParams.add(new Parameter("KEY3", "333"));
        TestTool.genXml("ExtendParams",extendParams);
    }
}