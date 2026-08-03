package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;

import org.dom4j.DocumentException;

/**
 * 规则 2：OFD 文件只能包含一个文档（GB/T 42133-2022 6.2.1c）
 * <p>
 * 检查 OFD.xml 中是否存在多个 DocBody。
 *
 * @author 权观宇
 * @since 2.4.0
 */
public class SingleDocRule implements ArchiveRule {

    public static final String RULE_NAME = "SINGLE_DOC";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        try {
            OFD ofd = ofdDir.getOfd();
            List<DocBody> docBodies = ofd.getDocBodies();

            if (docBodies.size() > 1) {
                return Collections.singletonList(new ArchiveViolation(
                        RULE_NAME,
                        ArchiveViolation.Severity.ERROR,
                        "OFD 文件包含 " + docBodies.size() + " 个文档体，OFD-A 只允许单个文档",
                        ST_Loc.getInstance("OFD.xml"),
                        String.valueOf(docBodies.size()),
                        "1"));
            }
            return Collections.emptyList();
        } catch (FileNotFoundException | DocumentException e) {
            return Collections.singletonList(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "无法读取 OFD.xml: " + e.getMessage(), null, null, null));
        }
    }
}
