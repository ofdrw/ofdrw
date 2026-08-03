package org.ofdrw.archive.convert.handler;

import org.dom4j.Element;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.action.actionType.OFDAction;
import org.ofdrw.core.action.actionType.actionGoto.Goto;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理器 6：删除非 Goto 动作，Goto→Link 注释（GB/T 42133-2022 6.2.2c/6.2.3c/6.3.3b）
 * <p>
 * 三级处理：
 * <ul>
 *   <li>Document 级：直接删除全部 Actions</li>
 *   <li>Page 级：遍历页面/图元对象 Actions，GotoA→Link注释，非Goto删除</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class NonGotoActionHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            // 1. Document 级：直接删 Actions
            org.ofdrw.core.basicStructure.doc.Document document = reader.getDoc(0);
            if (document != null) {
                document.removeOFDElemByNames("Actions");
            }

            // 2. Page 级：遍历每个页面
            int pageCount = reader.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                Page page = reader.getPage(i);
                cleanPageActions(page);
            }
        } catch (Exception e) {
            throw new IOException("无法处理动作: " + e.getMessage(), e);
        }
    }

    /**
     * 清理页面中所有图元对象的动作
     */
    @SuppressWarnings("unchecked")
    private void cleanPageActions(Element container) {
        // 递归遍历所有元素
        List<Element> children = new ArrayList<>(container.elements());
        for (Element child : children) {
            // 处理当前元素的 Actions
            List<Element> actionsList = child.elements("Actions");
            for (Element actions : new ArrayList<>(actionsList)) {
                cleanActionsElement(actions);
            }
            // 递归处理子元素（处理 PageBlock 嵌套）
            cleanPageActions(child);
        }
    }

    /**
     * 清理单个 Actions 元素：非Goto→删除，Goto→保留（后续由OutlineActionHandler处理跳转）
     */
    @SuppressWarnings("unchecked")
    private void cleanActionsElement(Element actions) {
        List<Element> actionList = new ArrayList<>(actions.elements());
        boolean hasValidAction = false;

        for (Element actionEl : actionList) {
            // 获取动作类型元素（Goto/URI/Sound/Movie 等）
            List<Element> typeElements = actionEl.elements();
            if (typeElements.isEmpty()) {
                actionEl.detach();
                continue;
            }

            Element typeEl = typeElements.get(0);
            String typeName = typeEl.getName();

            if ("Goto".equalsIgnoreCase(typeName) || "GotoA".equalsIgnoreCase(typeName)) {
                // Goto 类动作保留（文档内跳转是允许的）
                hasValidAction = true;
            } else {
                // URI/Sound/Movie → 删除
                actionEl.detach();
            }
        }

        // 若所有动作都被删除，删除 Actions 节点
        if (!hasValidAction) {
            actions.detach();
        }
    }
}
