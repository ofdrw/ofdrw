package org.ofdrw.crypto.integrity;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试验证OFD完整性
 *
 * @author 权观宇
 * @since 2021-08-24 19:45:25
 */
class OFDIntegrityVerifierTest {

    /**
     * 完整性验证测试用例
     */
    @Test
    void integrity() throws GeneralSecurityException, IOException, DocumentException {
        Path in = Paths.get("src/test/resources/hello-integrity.ofd");
        // 1. 创建OFD完整性验证器。
        OFDIntegrityVerifier ofdIntegrityVerifier = new OFDIntegrityVerifier();
        // 2. 创建用于验证签名值的验证器。
        GMProtectVerifier gmProtectVerifier = new GMProtectVerifier();
        // 3. 执行完整性验证，获取验证结果。
        final boolean integrity = ofdIntegrityVerifier.integrity(in, gmProtectVerifier);
        System.out.println(">> OFD完整性校验: " + integrity);
        assertTrue(integrity);
    }

}