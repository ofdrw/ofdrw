package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.core.signatures.Signatures;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则：数字签名/签章的去技术化（GB/T 42133-2022 6.13）
 * <p>
 * 检查文档中是否存在数字签名或签章。存在则提示需要去技术化处理。
 *
 * @author xxx
 * @since 2.3.9
 */
public class SignatureRule implements ArchiveRule {
    public static final String RULE_NAME = "SIGNATURE";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            Signatures signatures = reader.getDefaultSignatures();
            if (signatures == null) {
                return violations;
            }

            List<org.ofdrw.core.signatures.Signature> sigList = signatures.getSignatures();
            for (org.ofdrw.core.signatures.Signature sig : sigList) {
                SigType type = sig.getType();
                String sigId = sig.getID();

                if (type == SigType.Sign) {
                    violations.add(new ArchiveViolation(
                            RULE_NAME, ArchiveViolation.Severity.ERROR,
                            "存在数字签名(ID=" + sigId + ")，OFD-A 需去技术化: "
                                    + "提取签名信息→注释参数→删除SignRef→从签名列表移除",
                            ST_Loc.getInstance("Signs/Signatures.xml"),
                            "Sign", "去技术化"));
                } else {
                    // Seal (default)
                    violations.add(new ArchiveViolation(
                            RULE_NAME, ArchiveViolation.Severity.ERROR,
                            "存在签章(ID=" + sigId + ")，OFD-A 需去技术化: "
                                    + "外观→注释(BlendMode=Darken)→保留签章人/证书/时间/摘要→移除签名列表",
                            ST_Loc.getInstance("Signs/Signatures.xml"),
                            "Seal", "去技术化"));
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查签名时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }
}
