package org.ofdrw.sign.verify.container;

import org.junit.jupiter.api.Test;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.verify.OFDValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

class SESV5ValidateContainerTest {

    @Test
    void validate() throws IOException, GeneralSecurityException {
        Path src = Paths.get("target/SESV5SignDoc.ofd");

        try (OFDReader reader = new OFDReader(src);
             OFDValidator validator = new OFDValidator(reader)) {
            validator.setValidator(new SESV5ValidateContainer());
            validator.exeValidate();
            System.out.println(">> V5验签通过");
        }
        // 如果没抛异常就是验签通过
    }
}
