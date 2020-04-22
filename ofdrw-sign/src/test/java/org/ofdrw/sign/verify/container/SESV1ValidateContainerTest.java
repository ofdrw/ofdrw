package org.ofdrw.sign.verify.container;

import org.junit.jupiter.api.Test;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.verify.OFDValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-04-22 23:37:16
 */
class SESV1ValidateContainerTest {
    @Test
    void validate() throws IOException, GeneralSecurityException {
        Path src = Paths.get("target/SESV1SignDoc.ofd");

        try (OFDReader reader = new OFDReader(src);
             OFDValidator validator = new OFDValidator(reader)) {
            validator.setValidator(new SESV1ValidateContainer());
            validator.exeValidate();
            System.out.println(">> 验证通过");
        }
    }
}