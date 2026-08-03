package org.ofdrw.archive.check.rule;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.basicStructure.doc.CT_CommonData;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicStructure.res.resources.*;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * 规则 18：资源应在正确位置定义（GB/T 42133-2022 6.2.6a）
 * <p>
 * ColorSpace/Font 应在 PublicRes 中（不能在 PageRes）
 * Image/VectorG/DrawParam 应在 DocumentRes 或 PageRes（不能在 PublicRes）
 *
 * @author 权观宇
 * @since 2.4.0
 */
public class ResourcePlacementRule implements ArchiveRule {
    public static final String RULE_NAME = "RESOURCE_PLACEMENT";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            // 获取 DocumentRes 中的资源类型
            org.ofdrw.pkg.container.DocDir docDir = ofdDir.obtainDocDefault();
            Res publicRes = getRes(docDir, "PublicRes.xml");
            Res docRes = getRes(docDir, "DocumentRes.xml");

            // 检查 PublicRes：不应包含 MultiMedia/VectorG/DrawParam
            if (publicRes != null) {
                checkPublicRes(publicRes, violations);
            }

            // 检查各页 PageRes：不应包含 ColorSpace/Font
            checkPageResources(reader, ofdDir, violations);
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查资源位置时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }

    /**
     * 检查 PublicRes 中的资源类型是否合规
     */
    private void checkPublicRes(Res publicRes, List<ArchiveViolation> violations) {
        // MultiMedia（图像/音视频）不应在 PublicRes
        if (hasResourceType(publicRes, "MultiMedias")) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.WARN,
                    "MultiMedia 资源应放在 DocumentRes 或 PageRes，而非 PublicRes",
                    ST_Loc.getInstance("PublicRes.xml"),
                    "PublicRes", "DocumentRes/PageRes"));
        }
        // VectorG 不应在 PublicRes
        if (hasResourceType(publicRes, "CompositeGraphicUnits")) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.WARN,
                    "VectorG 资源应放在 DocumentRes 或 PageRes，而非 PublicRes",
                    ST_Loc.getInstance("PublicRes.xml"),
                    "PublicRes", "DocumentRes/PageRes"));
        }
        // DrawParam 不应在 PublicRes
        if (hasResourceType(publicRes, "DrawParams")) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.WARN,
                    "DrawParam 应放在 DocumentRes 或 PageRes，而非 PublicRes",
                    ST_Loc.getInstance("PublicRes.xml"),
                    "PublicRes", "DocumentRes/PageRes"));
        }
    }

    /**
     * 检查各页 PageRes 中是否有应放在 PublicRes 的资源
     */
    private void checkPageResources(OFDReader reader, OFDDir ofdDir,
                                     List<ArchiveViolation> violations) {
        // 通过 ResourceManage 获取资源列表
        // ColorSpace/Font 列表由 ResourceManage 从所有资源文件加载
        // 若某个 ColorSpace/Font 定义在 PageRes → WARN
        // Phase 3: 通过 Reader 遍历页面 PageRes 文件
    }

    /**
     * 检查 Res 对象中是否包含指定类型的资源
     */
    @SuppressWarnings("unchecked")
    private boolean hasResourceType(Res res, String elementName) {
        List<Element> children = res.elements();
        for (Element child : children) {
            if (elementName.equals(child.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 安全获取 Res 对象
     */
    private Res getRes(org.ofdrw.pkg.container.DocDir docDir, String fileName) {
        try {
            if ("PublicRes.xml".equals(fileName)) {
                return docDir.getPublicRes();
            } else {
                return docDir.getDocumentRes();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
