package org.ofdrw.converter.pdf.structure;

import org.ofdrw.core.action.Actions;
import org.ofdrw.core.action.CT_Action;
import org.ofdrw.core.action.actionType.OFDAction;
import org.ofdrw.core.action.actionType.actionGoto.Bookmark;
import org.ofdrw.core.action.actionType.actionGoto.CT_Dest;
import org.ofdrw.core.action.actionType.actionGoto.DestType;
import org.ofdrw.core.action.actionType.actionGoto.Goto;
import org.ofdrw.core.action.actionType.actionGoto.OFDGotoTarget;
import org.ofdrw.core.basicStructure.outlines.CT_OutlineElem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 书签映射器：OFD Outlines → PDF /Outlines
 * <p>
 * <b>决策 25</b>：仅映射<b>标题</b>与<b>目标</b>（OFD 表 54 的 XYZ/Fit/FitH/FitV/FitR 对应 PDF 同名目标），
 * <b>不做</b>大纲样式；{@code Expanded}/{@code Count} 只作为结构信息保留，不写入任何 PDF 样式属性。
 * <p>
 * 目标解析：取大纲项自身的 {@code Goto} 动作，其目标可以是 {@code Dest}，
 * 也可以是 {@code Bookmark} 名（前向引用，需要先建立“书签名 → 目标”映射）。
 * 无法解析目标的大纲项<b>仍保留标题</b>（PDF 允许无目标书签），并记录 warning。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class OutlineMapper implements StructureMapper {

    /**
     * 最大递归深度，防止畸形文档的深层嵌套导致栈溢出（与绘制参数继承链上限 32 一致）
     */
    public static final int MAX_DEPTH = 32;

    /**
     * OFD 页对象 ID → 页码（从 0 起）
     */
    private final Map<String, Integer> pageIndexById;

    /**
     * 书签名 → 目标，用于 Goto 的 {@code Bookmark} 名前向引用
     */
    private final Map<String, PdfDestination> bookmarkTargets = new HashMap<>();

    /**
     * 构造书签映射器
     *
     * @param pageIndexById OFD 页对象 ID（{@code ST_ID} 的字符串形式）到页码（从 0 起）的映射，不能为空
     */
    public OutlineMapper(Map<String, Integer> pageIndexById) {
        if (pageIndexById == null) {
            throw new IllegalArgumentException("页对象 ID 到页码的映射不能为空");
        }
        this.pageIndexById = pageIndexById;
    }

    /**
     * @return 映射器名称
     */
    @Override
    public String getName() {
        return "OutlineMapper";
    }

    /**
     * 把 OFD 大纲树映射为 PDF 书签树
     *
     * @param outlineElems OFD 大纲元素列表，可为空
     * @return 第一层书签节点；无大纲时返回空结果
     */
    public Result<BookmarkNode> map(List<CT_OutlineElem> outlineElems) {
        if (outlineElems == null || outlineElems.isEmpty()) {
            return Result.empty();
        }
        bookmarkTargets.clear();
        collectBookmarkTargets(outlineElems);

        List<String> warnings = new ArrayList<>();
        List<BookmarkNode> nodes = new ArrayList<>(outlineElems.size());
        for (CT_OutlineElem elem : outlineElems) {
            BookmarkNode node = toNode(elem, 0, warnings);
            if (node != null) {
                nodes.add(node);
            }
        }
        Result<BookmarkNode> result = new Result<>(nodes, warnings);
        return result.isEmpty() ? new Result<>(Collections.<BookmarkNode>emptyList(), warnings) : result;
    }

    /**
     * 第一遍：收集“书签名 → 目标”，供 Goto 的 Bookmark 名前向引用使用
     *
     * @param elems 大纲元素
     */
    private void collectBookmarkTargets(List<CT_OutlineElem> elems) {
        for (CT_OutlineElem elem : elems) {
            if (elem == null) {
                continue;
            }
            String title = elem.getTitle();
            PdfDestination dest = resolveActionTarget(elem.getActions(), false);
            if (title != null && dest != null) {
                bookmarkTargets.put(title, dest);
            }
            collectBookmarkTargets(elem.getOutlineElems());
        }
    }

    /**
     * 递归映射单个大纲项
     *
     * @param elem     大纲项，可为空
     * @param depth    当前深度，从 0 起
     * @param warnings 告警收集器
     * @return 书签节点；输入为空或超过深度上限时返回 null
     */
    private BookmarkNode toNode(CT_OutlineElem elem, int depth, List<String> warnings) {
        if (elem == null) {
            return null;
        }
        if (depth >= MAX_DEPTH) {
            warnings.add("大纲嵌套深度超过 " + MAX_DEPTH + "，已截断：" + elem.getTitle());
            return null;
        }
        String title = elem.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = "未命名书签";
            warnings.add("大纲项缺少标题，已使用默认标题");
        }
        PdfDestination dest = resolveActionTarget(elem.getActions(), true);
        if (dest == null) {
            warnings.add("大纲项未提供可映射的目标，书签仅保留标题：" + title);
        }
        List<BookmarkNode> children = new ArrayList<>();
        for (CT_OutlineElem child : elem.getOutlineElems()) {
            BookmarkNode childNode = toNode(child, depth + 1, warnings);
            if (childNode != null) {
                children.add(childNode);
            }
        }
        return new BookmarkNode(title, dest, elem.getExpanded(), children);
    }

    /**
     * 从动作列表中解析 Goto 目标
     *
     * @param actions 动作列表，可为空
     * @param once    保留参数（便于后续区分“仅第一次解析”的告警策略）
     * @return 目标；无 Goto 或无法解析时返回 null
     */
    private PdfDestination resolveActionTarget(Actions actions, boolean once) {
        if (actions == null) {
            return null;
        }
        for (CT_Action ctAction : actions.getActions()) {
            if (ctAction == null) {
                continue;
            }
            OFDAction action = ctAction.getAction();
            if (!(action instanceof Goto)) {
                continue;
            }
            try {
                OFDGotoTarget target = ((Goto) action).getTarget();
                if (target instanceof CT_Dest) {
                    return mapDest((CT_Dest) target);
                }
                if (target instanceof Bookmark) {
                    PdfDestination referenced = bookmarkTargets.get(((Bookmark) target).getBookmarkName());
                    if (referenced != null) {
                        return referenced;
                    }
                }
            } catch (RuntimeException ignored) {
                // Goto 目标畸形（多个目标 / 未知类型）：按“无目标”处理，由调用方决定是否告警
            }
        }
        return null;
    }

    /**
     * OFD 目标区域（表 54）→ PDF 目标描述
     * <p>
     * OFD 的 Left/Top/Right/Bottom 单位为 mm、位于页面坐标系（原点左上、Y 向下），
     * 转换为 PDF 坐标需要目标页高度做 Y 轴翻转；此处只保留 mm 值，
     * 由 {@link PdfDestination#ofdMm} 标注坐标来源，写入侧在拿到目标页高度后完成翻转。
     *
     * @param dest OFD 目标区域
     * @return 目标；类型或页码缺失/不支持时返回 null
     */
    private PdfDestination mapDest(CT_Dest dest) {
        if (dest == null || dest.getType() == null) {
            return null;
        }
        Integer pageIndex = dest.getPageID() == null ? null : pageIndexById.get(dest.getPageID().toString());
        if (pageIndex == null) {
            return null;
        }
        PdfDestination.Type pdfType = toPdfType(dest.getType());
        if (pdfType == null) {
            return null;
        }
        return PdfDestination.ofdMm(pdfType, pageIndex, dest.getLeft(), dest.getTop(),
                dest.getRight(), dest.getBottom(), dest.getZoom());
    }

    /**
     * OFD 目标类型 → PDF 目标类型
     *
     * @param type OFD 目标类型
     * @return PDF 目标类型；不支持时返回 null
     */
    public static PdfDestination.Type toPdfType(DestType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case XYZ:
                return PdfDestination.Type.XYZ;
            case Fit:
                return PdfDestination.Type.FIT;
            case FitH:
                return PdfDestination.Type.FIT_H;
            case FitV:
                return PdfDestination.Type.FIT_V;
            case FitR:
                return PdfDestination.Type.FIT_R;
            default:
                return null;
        }
    }

    /**
     * 书签节点
     * <p>
     * 决策 25：只承载标题、目标与子节点；{@code expanded} 仅作结构信息保留，不写入 PDF 样式。
     */
    public static final class BookmarkNode {

        /**
         * 标题
         */
        private final String title;

        /**
         * 目标，可为空（无目标书签）
         */
        private final PdfDestination destination;

        /**
         * OFD 展开标记，可为空
         */
        private final Boolean expanded;

        /**
         * 子节点
         */
        private final List<BookmarkNode> children;

        /**
         * @param title       标题
         * @param destination 目标，可为空
         * @param expanded    OFD 展开标记，可为空
         * @param children    子节点
         */
        public BookmarkNode(String title, PdfDestination destination, Boolean expanded, List<BookmarkNode> children) {
            this.title = title;
            this.destination = destination;
            this.expanded = expanded;
            this.children = children == null ? new ArrayList<BookmarkNode>() : new ArrayList<>(children);
        }

        /**
         * @return 标题
         */
        public String getTitle() {
            return title;
        }

        /**
         * @return 目标，可为空
         */
        public PdfDestination getDestination() {
            return destination;
        }

        /**
         * @return OFD 展开标记，可为空；不参与 PDF 输出（决策 25）
         */
        public Boolean getExpanded() {
            return expanded;
        }

        /**
         * @return 子节点
         */
        public List<BookmarkNode> getChildren() {
            return Collections.unmodifiableList(children);
        }

        /**
         * 展开为“深度:标题->页码”的扁平列表，便于断言
         *
         * @param depth 当前深度，从 0 起
         * @param out   输出列表
         */
        public void flatten(int depth, List<String> out) {
            out.add(depth + ":" + title + (destination == null ? "" : "->p" + destination.getPageIndex()));
            for (BookmarkNode child : children) {
                child.flatten(depth + 1, out);
            }
        }

        @Override
        public String toString() {
            return "BookmarkNode{" + title + ", dest=" + destination + ", children=" + children.size() + '}';
        }
    }
}
