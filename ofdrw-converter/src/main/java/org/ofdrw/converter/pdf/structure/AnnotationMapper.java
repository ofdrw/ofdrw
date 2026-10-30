package org.ofdrw.converter.pdf.structure;

import org.ofdrw.core.action.Actions;
import org.ofdrw.core.action.CT_Action;
import org.ofdrw.core.action.actionType.OFDAction;
import org.ofdrw.core.action.actionType.GotoA;
import org.ofdrw.core.action.actionType.Sound;
import org.ofdrw.core.action.actionType.URI;
import org.ofdrw.core.action.actionType.actionGoto.Bookmark;
import org.ofdrw.core.action.actionType.actionGoto.CT_Dest;
import org.ofdrw.core.action.actionType.actionGoto.Goto;
import org.ofdrw.core.action.actionType.actionGoto.OFDGotoTarget;
import org.ofdrw.core.action.actionType.actionMovie.Movie;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_RefID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 注释映射器：OFD Annots → PDF 页面 /Annots（设计 3.8 节）
 * <p>
 * <b>映射规则</b>：
 * <ul>
 *     <li>视觉：所有注释一律<b>以 Appearance 重绘</b>其静态外观（路径/文字/图像），不依赖阅读器对注释类型的二次解释；</li>
 *     <li>交互：{@code Link} 注释若带 {@code Goto}/{@code URI}/{@code GotoA} 动作，额外生成一条链接注释，
 *         热区取 Appearance 外接矩形（缺省时取动作区域）；</li>
 *     <li>类型映射：Link → /Link，Highlight → /Highlight（Quads 取外接矩形），
 *         Path/Stamp/Watermark → /Stamp（其语义是“固定外观的图章/水印”，用 /Stamp 承载可被阅读器识别为注释）；</li>
 *     <li>动作：{@code Sound}/{@code Movie} 按<b>决策 18</b> 跳过并输出 warning，注释外观仍照常绘制；</li>
 *     <li>不可见注释（{@code Visible="false"}）不生成 PDF 注释，仅告警。</li>
 * </ul>
 * <p>
 * 坐标换算与页面图元一致：注释外观位于页面坐标系，需经页面框偏移与 Y 翻转（决策 26 + 决策 1），
 * 由 {@link AnnotationTarget#toPdfRect(double, double)} 在拿到页面框后完成。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class AnnotationMapper implements StructureMapper {

    /**
     * OFD 页对象 ID → 页码（从 0 起）
     */
    private final Map<String, Integer> pageIndexById;

    /**
     * 书签名 → 目标，用于 Goto 的 {@code Bookmark} 名前向引用
     */
    private final Map<String, PdfDestination> bookmarkTargets;

    /**
     * 构造注释映射器
     *
     * @param pageIndexById   OFD 页对象 ID 到页码（从 0 起）的映射，不能为空
     * @param bookmarkTargets 书签名到目标的映射（来自 {@link OutlineMapper}），可为空
     */
    public AnnotationMapper(Map<String, Integer> pageIndexById, Map<String, PdfDestination> bookmarkTargets) {
        if (pageIndexById == null) {
            throw new IllegalArgumentException("页对象 ID 到页码的映射不能为空");
        }
        this.pageIndexById = pageIndexById;
        this.bookmarkTargets = bookmarkTargets == null
                ? java.util.Collections.<String, PdfDestination>emptyMap() : bookmarkTargets;
    }

    /**
     * @return 映射器名称
     */
    @Override
    public String getName() {
        return "AnnotationMapper";
    }

    /**
     * 映射单个页面上的全部注释
     *
     * @param annots OFD 注释列表，可为空
     * @return 注释目标描述；无注释时返回空结果
     */
    public Result<AnnotationTarget> map(List<Annot> annots) {
        if (annots == null || annots.isEmpty()) {
            return Result.empty();
        }
        List<AnnotationTarget> items = new ArrayList<>(annots.size());
        List<String> warnings = new ArrayList<>();
        for (Annot annot : annots) {
            if (annot == null) {
                continue;
            }
            String id = annot.getID() == null ? "?" : annot.getID().toString();
            if (Boolean.FALSE.equals(annot.getVisible())) {
                warnings.add("注释声明为不可见，已跳过：ID=" + id);
                continue;
            }
            AnnotType type = annot.getType();
            if (type == null) {
                warnings.add("注释缺少类型，按 Watermark 外观处理：ID=" + id);
                type = AnnotType.Watermark;
            }
            ST_Box boundary = boundaryOf(annot);
            PdfAction action = mapAction(annot, id, warnings);
            items.add(new AnnotationTarget(id, type, boundary, annot.getAppearance(), action, annot.getRemark()));
        }
        return new Result<>(items, warnings);
    }

    /**
     * 取注释外观的外接矩形
     *
     * @param annot 注释
     * @return 外接矩形；缺失时退化到 {@code (0,0,0,0)}（写入侧会跳过零面积热区）
     */
    static ST_Box boundaryOf(Annot annot) {
        Appearance appearance = annot.getAppearance();
        if (appearance != null && appearance.getBoundary() != null) {
            return appearance.getBoundary();
        }
        // 无外观时，退化到动作区域（交互动作通常带区域）——语义上近似，告警由调用方处理
        Actions actions = actionsOf(annot);
        if (actions != null) {
            for (CT_Action ctAction : actions.getActions()) {
                if (ctAction == null || ctAction.getRegion() == null || ctAction.getRegion().getAreas() == null) {
                    continue;
                }
                for (org.ofdrw.core.graph.tight.CT_Area area : ctAction.getRegion().getAreas()) {
                    ST_Box box = boundaryOf(area);
                    if (box != null) {
                        return box;
                    }
                }
            }
        }
        return new ST_Box(0, 0, 0, 0);
    }

    /**
     * 取动作区域的外接矩形
     * <p>
     * {@code CT_Area} 只给出起始点与绘制指令，没有现成的外接矩形；
     * 这里按“起始点 + 各指令端点”的包围盒近似（动作热区只需覆盖交互范围，近似即可）。
     *
     * @param area 区域，可为空
     * @return 外接矩形（mm）；无有效几何时返回 null
     */
    static ST_Box boundaryOf(org.ofdrw.core.graph.tight.CT_Area area) {
        if (area == null || area.getStart() == null || area.getStart().getX() == null) {
            return null;
        }
        double minX = area.getStart().getX();
        double maxX = minX;
        double minY = area.getStart().getY() == null ? 0 : area.getStart().getY();
        double maxY = minY;
        try {
            for (org.ofdrw.core.graph.tight.method.Command command : area.getCommands()) {
                for (double[] point : commandPoint(command)) {
                    minX = Math.min(minX, point[0]);
                    maxX = Math.max(maxX, point[0]);
                    minY = Math.min(minY, point[1]);
                    maxY = Math.max(maxY, point[1]);
                }
            }
        } catch (RuntimeException ignored) {
            // 区域指令畸形：按起始点退化
        }
        if (maxX - minX <= 0 || maxY - minY <= 0) {
            return null;
        }
        return new ST_Box(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * 取绘制指令的端点
     *
     * @param command 绘制指令
     * @return 端点数组，每项为 {@code [x, y]}
     */
    private static double[][] commandPoint(org.ofdrw.core.graph.tight.method.Command command) {
        if (command instanceof org.ofdrw.core.graph.tight.method.Line) {
            org.ofdrw.core.graph.tight.method.Line line = (org.ofdrw.core.graph.tight.method.Line) command;
            return new double[][]{{line.getPoint1().getX(), line.getPoint1().getY()}};
        }
        if (command instanceof org.ofdrw.core.graph.tight.method.CubicBezier) {
            org.ofdrw.core.graph.tight.method.CubicBezier curve = (org.ofdrw.core.graph.tight.method.CubicBezier) command;
            return new double[][]{
                    {curve.getPoint1().getX(), curve.getPoint1().getY()},
                    {curve.getPoint2().getX(), curve.getPoint2().getY()},
                    {curve.getPoint3().getX(), curve.getPoint3().getY()}};
        }
        if (command instanceof org.ofdrw.core.graph.tight.method.QuadraticBezier) {
            org.ofdrw.core.graph.tight.method.QuadraticBezier curve =
                    (org.ofdrw.core.graph.tight.method.QuadraticBezier) command;
            return new double[][]{
                    {curve.getPoint1().getX(), curve.getPoint1().getY()},
                    {curve.getPoint2().getX(), curve.getPoint2().getY()}};
        }
        if (command instanceof org.ofdrw.core.graph.tight.method.Move) {
            org.ofdrw.core.graph.tight.method.Move move = (org.ofdrw.core.graph.tight.method.Move) command;
            return new double[][]{{move.getPoint1().getX(), move.getPoint1().getY()}};
        }
        return new double[0][];
    }

    /**
     * 取注释的动作列表：优先注释自身的 {@code Actions} 元素
     *
     * @param annot 注释
     * @return 动作列表，可为空
     */
    static Actions actionsOf(Annot annot) {
        Object value = annot.getOFDElement("Actions");
        if (value instanceof org.dom4j.Element) {
            return new Actions((org.dom4j.Element) value);
        }
        return null;
    }

    /**
     * 把注释动作映射为 PDF 动作
     * <p>
     * 决策 18：Sound/Movie 跳过并 warning；Goto 目标无法解析时告警并返回 null。
     *
     * @param annot    注释
     * @param id       注释 ID（用于告警）
     * @param warnings 告警收集器
     * @return PDF 动作；无动作或全部不支持时返回 null
     */
    private PdfAction mapAction(Annot annot, String id, List<String> warnings) {
        Actions actions = actionsOf(annot);
        if (actions == null) {
            return null;
        }
        for (CT_Action ctAction : actions.getActions()) {
            if (ctAction == null) {
                continue;
            }
            OFDAction action = ctAction.getAction();
            if (action instanceof Sound) {
                warnings.add("不支持 Sound 动作，已跳过（决策 18）：注释 ID=" + id);
                continue;
            }
            if (action instanceof Movie) {
                warnings.add("不支持 Movie 动作，已跳过（决策 18）：注释 ID=" + id);
                continue;
            }
            if (action instanceof URI) {
                String uri = ((URI) action).getURI();
                return uri == null || uri.trim().isEmpty() ? null : PdfAction.uri(uri);
            }
            if (action instanceof GotoA) {
                String attachId = ((GotoA) action).getAttachID();
                return attachId == null || attachId.trim().isEmpty() ? null : PdfAction.gotoAttachment(attachId);
            }
            if (action instanceof Goto) {
                PdfDestination dest = mapGoto((Goto) action, id, warnings);
                return dest == null ? null : PdfAction.gotoDestination(dest);
            }
            warnings.add("未知动作类型，已跳过：注释 ID=" + id);
        }
        return null;
    }

    /**
     * 解析 Goto 动作的目标
     *
     * @param goto     Goto 动作
     * @param id       注释 ID
     * @param warnings 告警收集器
     * @return 目标；无法解析时返回 null 并告警
     */
    private PdfDestination mapGoto(Goto gotoAction, String id, List<String> warnings) {
        try {
            OFDGotoTarget target = gotoAction.getTarget();
            if (target instanceof CT_Dest) {
                CT_Dest dest = (CT_Dest) target;
                Integer pageIndex = dest.getPageID() == null ? null : pageIndexById.get(dest.getPageID().toString());
                PdfDestination.Type type = OutlineMapper.toPdfType(dest.getType());
                if (pageIndex == null || type == null) {
                    warnings.add("Goto 目标缺少页对象或类型无法识别，已跳过：注释 ID=" + id);
                    return null;
                }
                return PdfDestination.ofdMm(type, pageIndex, dest.getLeft(), dest.getTop(),
                        dest.getRight(), dest.getBottom(), dest.getZoom());
            }
            if (target instanceof Bookmark) {
                PdfDestination referenced = bookmarkTargets.get(((Bookmark) target).getBookmarkName());
                if (referenced == null) {
                    warnings.add("Goto 引用的书签不存在，已跳过：注释 ID=" + id);
                }
                return referenced;
            }
        } catch (RuntimeException e) {
            warnings.add("Goto 动作解析失败，已跳过：注释 ID=" + id + "，原因：" + e.getMessage());
        }
        return null;
    }

    /**
     * 注释目标描述（库无关）
     */
    public static final class AnnotationTarget {

        /**
         * OFD 注释 ID
         */
        private final String annotId;

        /**
         * OFD 注释类型
         */
        private final AnnotType ofdType;

        /**
         * 外观外接矩形（OFD 页面坐标，mm）
         */
        private final ST_Box boundary;

        /**
         * OFD 外观内容，可为空
         */
        private final Appearance appearance;

        /**
         * PDF 动作，可为空
         */
        private final PdfAction action;

        /**
         * 注释备注，可为空
         */
        private final String remark;

        /**
         * @param annotId    OFD 注释 ID
         * @param ofdType    OFD 注释类型
         * @param boundary   外观外接矩形（mm）
         * @param appearance OFD 外观内容，可为空
         * @param action     PDF 动作，可为空
         * @param remark     备注，可为空
         */
        public AnnotationTarget(String annotId, AnnotType ofdType, ST_Box boundary, Appearance appearance,
                                PdfAction action, String remark) {
            this.annotId = annotId;
            this.ofdType = ofdType;
            this.boundary = boundary;
            this.appearance = appearance;
            this.action = action;
            this.remark = remark;
        }

        /**
         * @return OFD 注释 ID
         */
        public String getAnnotId() {
            return annotId;
        }

        /**
         * @return OFD 注释类型
         */
        public AnnotType getOfdType() {
            return ofdType;
        }

        /**
         * @return 外观外接矩形（OFD 页面坐标，mm）
         */
        public ST_Box getBoundary() {
            return boundary;
        }

        /**
         * @return OFD 外观内容，可为空
         */
        public Appearance getAppearance() {
            return appearance;
        }

        /**
         * @return PDF 动作，可为空
         */
        public PdfAction getAction() {
            return action;
        }

        /**
         * @return 备注，可为空
         */
        public String getRemark() {
            return remark;
        }

        /**
         * 映射为 PDF 注释类型名
         * <p>
         * Link → /Link；Highlight → /Highlight；Path/Stamp/Watermark → /Stamp
         * （后三者是“固定外观”类注释，用 /Stamp 承载，外观由 Appearance 重绘保证视觉一致）。
         *
         * @return PDF 注释子类型名（不含前缀 {@code /}）
         */
        public String getPdfAnnotationType() {
            if (ofdType == null) {
                return "Stamp";
            }
            switch (ofdType) {
                case Link:
                    return "Link";
                case Highlight:
                    return "Highlight";
                case Path:
                case Stamp:
                case Watermark:
                default:
                    return "Stamp";
            }
        }

        /**
         * 换算热区矩形到 PDF 页面坐标
         * <p>
         * 结果为 {@code [llx, lly, urx, ury]}，单位 pt，原点左下、Y 向上。
         *
         * @param pageY0Mm     页面框左上角纵坐标，单位 mm
         * @param pageHeightMm 页面框高度，单位 mm
         * @return 长度 4 的数组；外接矩形缺失或零面积时返回 null
         */
        public double[] toPdfRect(double pageY0Mm, double pageHeightMm) {
            if (boundary == null || boundary.getWidth() == null || boundary.getHeight() == null
                    || boundary.getWidth() <= 0 || boundary.getHeight() <= 0) {
                return null;
            }
            double x0 = boundary.getTopLeftX() == null ? 0 : boundary.getTopLeftX();
            double y0 = boundary.getTopLeftY() == null ? 0 : boundary.getTopLeftY();
            double llx = org.ofdrw.converter.pdf.CoordSystem.mmToPt(x0);
            double urx = org.ofdrw.converter.pdf.CoordSystem.mmToPt(x0 + boundary.getWidth());
            double ury = org.ofdrw.converter.pdf.CoordSystem.yToPt(y0 - pageY0Mm, pageHeightMm);
            double lly = org.ofdrw.converter.pdf.CoordSystem.yToPt(y0 + boundary.getHeight() - pageY0Mm, pageHeightMm);
            return new double[]{llx, lly, urx, ury};
        }

        @Override
        public String toString() {
            return "AnnotationTarget{" + annotId + ", " + ofdType + " -> " + getPdfAnnotationType()
                    + ", action=" + action + '}';
        }
    }
}
