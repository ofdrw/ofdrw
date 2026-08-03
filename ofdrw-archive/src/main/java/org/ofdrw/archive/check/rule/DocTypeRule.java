package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;

import org.dom4j.DocumentException;

/**
 * 规则 1：DocType 必须为 "OFD-A"（GB/T 42133-2022 6.2.1a）
 * <p>
 * 检查 OFD.xml 根节点的 DocType 属性是否为 "OFD-A"。
 *
 * @author 权观宇
 * @since 2.4.0
 */
public class DocTypeRule implements ArchiveRule {

    /** 规则标识 */
    public static final String RULE_NAME = "DOC_TYPE";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        try {
            OFD ofd = ofdDir.getOfd();
            String docType = ofd.getDocType();

            if (!"OFD-A".equals(docType)) {
                return Collections.singletonList(new ArchiveViolation(
                        RULE_NAME,
                        ArchiveViolation.Severity.ERROR,
                        "DocType 必须为 \"OFD-A\"，当前为: " + (docType == null ? "缺失" : "\"" + docType + "\""),
                        ST_Loc.getInstance("OFD.xml"),
                        docType,
                        "OFD-A"));
            }
            return Collections.emptyList();
        } catch (FileNotFoundException | DocumentException e) {
            return Collections.singletonList(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "无法读取 OFD.xml: " + e.getMessage(), null, null, null));
        }
    }
}
