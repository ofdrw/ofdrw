package org.ofdrw.converter.pdf.render.text;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文字布局计算：把 OFD 文字对象转换为逐字形位置（表 46、表 47、表 48）
 * <p>
 * 计算规则：
 * <ol>
 *     <li><b>首个字形的原点</b>（表 46 备注）：字符方向的旋转原点位于文字外接矩形的竖直中线；
 *     0° 取左上角，90° 取右上角，180° 取右下角，270° 取左下角；各 TextCode 再叠加自身的 X/Y 偏移
 *     （表 46：X/Y 为字形在对象坐标系中的位置）；</li>
 *     <li><b>Delta 双队列</b>：第 i 个文字的偏移取 DeltaX[i-1]、DeltaY[i-1]，队列长度不足时取最后一个值（末位尾部增量），
 *     见 {@link DeltaQueue#deltaAt(int)}；DeltaX 与 DeltaY 同属一条沿阅读方向排布的文字流；</li>
 *     <li><b>字形步进</b>：文字的排列由 Delta 决定；Delta 之外，<b>只</b>为多对一（连字）映射的后续字符补足字宽，
 *     因为该字符与前一字符共享字形、若不补足会导致后续文字整体左移；其余情形不得额外叠加字宽，
 *     否则会与 Delta 重复计算（现有实现“按字宽推进”造成的双倍位移即源于此）；</li>
 *     <li><b>CGTransform</b>（表 48）：CodePosition 起的 CodeCount 个字符对应 Glyphs 中的字形；
 *     {@link DeltaQueue#deltaAt(int)} 的跳过规则同现有实现：Glyphs 数量少于 CodeCount 时，
 *     区间内第 2 个字符起不产生位移。</li>
 * </ol>
 * 本类为无状态纯函数，不依赖任何渲染库。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class TextLayoutEngine {

    /**
     * 进行布局计算
     *
     * @param text       文字图元视图
     * @param nominalAdvanceMm 每个字形按字体度量换算的标称步进，单位 mm（多对一补足位移时使用）
     * @return 逐字布局结果
     */
    public TextLayout layout(OfdText text, double nominalAdvanceMm) {
        if (text == null) {
            throw new IllegalArgumentException("文字图元（text）不能为空");
        }
        List<TextLayout.PlacedGlyph> glyphs = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        /*
         * 图元 CTM 不在此处叠加：布局只输出**对象空间**坐标（Boundary 角点 + TextCode X/Y + Delta 累积），
         * 由写入侧 baseMatrix（含图元 CTM 与容器 CTM）统一乘一次，避免双重缩放。
         * CTM 的线性部分同时决定字号（见 TextRendererImpl 的 fontSizePt），两处口径必须一致。
         */
        double advance = nominalAdvanceMm > 0 ? nominalAdvanceMm : 0d;
        // 外接矩形：每个 TextCode 的 X/Y 都以它（按 CharDirection 取的角）为原点
        OfdBox boundary = text.getBoundary() == null ? new OfdBox(0, 0, 0, 0) : text.getBoundary();

        for (TextCodeRun code : text.getTextCodes()) {
            String content = code.getText();
            if (content.isEmpty()) {
                continue;
            }
            /*
             * GB/T 33190-2016 表 46：TextCode 的 X/Y 是**对象空间**中相对外接矩形左上角（按 CharDirection
             * 取对应的角）的偏移，不是页内绝对坐标。
             * 实测佐证：helloworld.ofd 的 TextObject Boundary="31.7 25.4 40.5 5"、TextCode X="0" Y="3"，
             * 基准 PDF 中该行文字 xMin = 89.86pt = 31.7mm（正是 Boundary 的 x），
             * 若把 X/Y 当绝对坐标会得到 0pt，文字整体错位到页角。
             */
            double[] origin = startOrigin(text, boundary);
            double cursorX = origin[0] + code.getX();
            double cursorY = origin[1] + code.getY();
            Map<Integer, CgTransform> transforms = indexTransforms(code.getCgTransforms());
            int codeStart = glyphs.size();

            int charIndex = 0;
            // 当前 TextCode 内的字符序号（从 0 起），DeltaX/DeltaY 队列按它取值
            int codeCharIndex = 0;
            for (int offset = 0; offset < content.length(); ) {
                int codePoint = content.codePointAt(offset);
                int charLength = Character.charCount(codePoint);
                String charText = content.substring(offset, offset + charLength);
                offset += charLength;

                // =========> CGTransform：显式字形索引与多对一跳过
                CgTransform transform = findTransform(transforms, charIndex);
                boolean skipDelta = transform != null && isSkipPosition(transform, charIndex);
                int[] explicitGids = explicitGlyphs(transform, charIndex);
                /*
                 * 多对一：CodeCount 个字符对应 1 个字形，该字形已经代表整个字符簇，
                 * 簇内其余字符不再生成字形（否则会按替代码位画出多余字形并覆盖同一 GID 的 Unicode，
                 * 提取文本出现重复字符或乱码），也不推进游标。
                 */
                if (transform != null && transform.getGlyphs().size() == 1 && transform.getCodeCount() > 1
                        && charIndex > transform.getCodePosition()) {
                    charIndex++;
                    continue;
                }

                // =========> 位置推进（DeltaX/DeltaY 队列按“所属 TextCode 内字符序号”取值，表 46：首字符不偏移；
                // 不同 TextCode 的队列互不共享，charIndex 仅用于 CGTransform 查找，不能参与 Delta 取值）
                int currentIndex = codeCharIndex++;
                if (currentIndex > 0) {
                    double dx = skipDelta ? 0 : code.getDeltaX().deltaAt(currentIndex);
                    double dy = skipDelta ? 0 : code.getDeltaY().deltaAt(currentIndex);
                    cursorX += dx;
                    cursorY += dy;
                    if (skipDelta && dx == 0 && dy == 0) {
                        double[] moved = advanceAlongReading(text, new double[]{cursorX, cursorY}, advance);
                        cursorX = moved[0];
                        cursorY = moved[1];
                    }
                }
                // 输出对象空间坐标（mm），图元 CTM 由写入侧统一乘一次
                double localX = cursorX;
                double localY = cursorY;

                // =========> 生成字形（CGTransform 一对多时展开为多个附属字形）
                /*
                 * 一对多 / 多对多（GlyphCount > CodeCount）没有“字形→码位”的反函数，
                 * 参考实现直接把这些簇的源码元写进 UniGB-UCS2-H 内容串、且不生成 ToUnicode，
                 * 文本层提取为空。这里同样不给簇内字形登记 Unicode，避免凭空多出泰文/泰米尔字符。
                 * 多对一（GlyphCount < CodeCount）仍保留首字符，与参考“fi 连字提取为 f”一致。
                 */
                boolean expandedCluster = isExpandedCluster(transform, charIndex);
                String unicode = expandedCluster ? "" : unescape(charText);
                if (explicitGids.length == 0) {
                    glyphs.add(new TextLayout.PlacedGlyph(unicode, placeholder(charText, codePoint),
                            -1, localX, localY, advance, false));
                } else {
                    for (int i = 0; i < explicitGids.length; i++) {
                        if (i == 0) {
                            // 展开簇不登记 Unicode（参考实现同样不生成 ToUnicode）
                            int[] codePoints = expandedCluster ? new int[0] : new int[]{codePoint};
                            glyphs.add(new TextLayout.PlacedGlyph(unicode, codePoints, explicitGids[i],
                                    localX, localY, advance, false));
                        } else {
                            // 一对多：附属字形与首字形同址，不携带文字
                            glyphs.add(new TextLayout.PlacedGlyph("", new int[0], explicitGids[i],
                                    localX, localY, 0d, true));
                        }
                    }
                }
                charIndex++;
            }
            /*
             * 字形步进 = 到下一个字形的实际位移（DeltaX/DeltaY 的结果）。
             * 该值会作为 /W 与 TJ 修正量的统一口径：若用字体标称步进代替，
             * 阅读器按标称字宽推进、实际字距却由 Delta 决定，提取文本会在字间插入空格/拆词。
             */
            for (int gi = codeStart; gi < glyphs.size(); gi++) {
                TextLayout.PlacedGlyph glyph = glyphs.get(gi);
                if (glyph.isShared()) {
                    continue;
                }
                double step = nominalAdvanceMm;
                for (int gj = gi + 1; gj < glyphs.size(); gj++) {
                    TextLayout.PlacedGlyph next = glyphs.get(gj);
                    if (next.isShared()) {
                        continue;
                    }
                    step = Math.hypot(next.getX() - glyph.getX(), next.getY() - glyph.getY());
                    break;
                }
                if (step > 0 && step != glyph.getAdvanceMm()) {
                    glyphs.set(gi, new TextLayout.PlacedGlyph(glyph.getText(), glyph.getCodePoints(),
                            glyph.getExplicitGlyphId(), glyph.getX(), glyph.getY(), step, false));
                }
            }
        }
        return new TextLayout(glyphs, warnings);
    }

    /**
     * 占位符 {@code ¤}（U+00A4）不携带 Unicode
     * <p>
     * GB/T 33190-2016 11.2：TextCode 作为占位符使用时一律采用 {@code ¤} 占位。
     *
     * @param charText  字符文本
     * @param codePoint 码位
     * @return 码位数组；占位符返回空数组
     */
    private static int[] placeholder(String charText, int codePoint) {
        return "\u00A4".equals(charText) ? new int[0] : new int[]{codePoint};
    }

    /**
     * 计算文字流首个字形的原点（表 46：旋转原点位于外接矩形竖直中线）
     *
     * @param text     文字图元
     * @param boundary 外接矩形，单位 mm
     * @return 原点 {@code [x, y]}，单位 mm（对象空间）
     */
    double[] startOrigin(OfdText text, OfdBox boundary) {
        double charDir = text.getCharDirectionDeg();
        double x;
        double y;
        if (charDir == 90) {
            // 顺时针 90°：原点移到右上角，文字沿竖直中线向下延伸
            x = boundary.getX() + boundary.getWidth();
            y = boundary.getY();
        } else if (charDir == 180) {
            x = boundary.getX() + boundary.getWidth();
            y = boundary.getY() + boundary.getHeight();
        } else if (charDir == 270) {
            x = boundary.getX();
            y = boundary.getY() + boundary.getHeight();
        } else {
            x = boundary.getX();
            y = boundary.getY();
        }
        return new double[]{x, y};
    }

    /**
     * 沿阅读方向推进一个字形步进
     *
     * @param text    文字图元
     * @param origin  当前原点 {@code [x, y]}，单位 mm
     * @param advance 步进，单位 mm
     * @return 推进后的原点
     */
    private static double[] advanceAlongReading(OfdText text, double[] origin, double advance) {
        // 阅读方向决定文字流的推进方向：0° → +x，90° → +y（OFD 左上角原点、Y 轴向下），
        // 180° → -x，270° → -y；HScale 只压缩字形宽度，不改变 Delta 语义，因此不参与推进
        double angle = text.getReadDirectionDeg();
        if (angle == 90) {
            return new double[]{origin[0], origin[1] + advance};
        }
        if (angle == 180) {
            return new double[]{origin[0] - advance, origin[1]};
        }
        if (angle == 270) {
            return new double[]{origin[0], origin[1] - advance};
        }
        return new double[]{origin[0] + advance, origin[1]};
    }

    /**
     * 建立字符位置到字形变换的索引
     *
     * @param transforms 字形变换列表
     * @return 字符位置到变换的映射（同一位置以首个变换为准）
     */
    private static Map<Integer, CgTransform> indexTransforms(List<CgTransform> transforms) {
        Map<Integer, CgTransform> map = new HashMap<>(transforms.size() * 2);
        for (CgTransform transform : transforms) {
            for (int i = 0; i < transform.getCodeCount(); i++) {
                map.putIfAbsent(transform.getCodePosition() + i, transform);
            }
        }
        return map;
    }

    /**
     * 查找字符所属的字形变换
     *
     * @param transforms 字符位置到变换的映射
     * @param charIndex  字符位置
     * @return 变换；不属于任何变换时返回 null
     */
    private static CgTransform findTransform(Map<Integer, CgTransform> transforms, int charIndex) {
        return transforms.get(charIndex);
    }

    /**
     * 判断该字符是否因多对一映射而跳过 Delta 位移
     *
     * @param transform 字形变换
     * @param charIndex 字符位置
     * @return true 表示不产生位移
     */
    private static boolean isSkipPosition(CgTransform transform, int charIndex) {
        return transform.getGlyphCount() < transform.getCodeCount()
                && transform.contains(charIndex) && charIndex > transform.getCodePosition();
    }

    /**
     * 判断字符是否属于“字形多于字符”的展开簇（一对多 / 多对多）
     *
     * <p>这类簇没有“字形→码位”的反函数，参考实现不为其生成 ToUnicode，文本层提取为空；
     * 若按字形逐个登记 Unicode，会在文本层凭空多出原文档没有的字符（如泰文、泰米尔文）。</p>
     *
     * @param transform 字形变换，可为 null
     * @param charIndex 字符位置
     * @return true 表示该字符所在簇的字形数多于字符数
     */
    private static boolean isExpandedCluster(CgTransform transform, int charIndex) {
        return transform != null
                && transform.getGlyphs().size() > transform.getCodeCount()
                && transform.contains(charIndex);
    }

    /**
     * 获取该字符对应的显式字形索引
     *
     * @param transform 字形变换，可为 null
     * @param charIndex 字符位置
     * @return 字形索引数组；无显式索引时为空数组
     */
    private static int[] explicitGlyphs(CgTransform transform, int charIndex) {
        if (transform == null || transform.getGlyphs().isEmpty()) {
            return new int[0];
        }
        List<Integer> glyphs = transform.getGlyphs();
        int count = transform.getCodeCount();
        int index = charIndex - transform.getCodePosition();
        if (index < 0) {
            return new int[0];
        }
        if (count == glyphs.size()) {
            // 一对一 / 多对多：逐字符对应
            return index < glyphs.size() ? new int[]{glyphs.get(index)} : new int[0];
        }
        if (glyphs.size() == 1) {
            // 多对一：首个字符使用该字形，后续字符共享
            return index == 0 ? new int[]{glyphs.get(0)} : new int[0];
        }
        if (index == 0) {
            // 一对多 / 多对多（字符少字形多）：首个字符展开全部字形
            int[] result = new int[glyphs.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = glyphs.get(i);
            }
            return result;
        }
        return new int[0];
    }

    /**
     * 还原 OFD 文字内容中的转义与 XML 实体
     * <p>
     * GB/T 33190-2016 11.2：不在 XML 编码范围内的字符以 {@code \} 加四位十六进制表示，空格同样需要转义。
     *
     * @param text 原始文字
     * @return 还原后的文字
     */
    static String unescape(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String value = text
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&copy;", "")
                .replace("&apos;", "'")
                .replace("\n", "");
        if (value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' && i + 4 < value.length()) {
                try {
                    int code = Integer.parseInt(value.substring(i + 1, i + 5), 16);
                    sb.append((char) code);
                    i += 4;
                    continue;
                } catch (NumberFormatException e) {
                    // 不是合法的四位十六进制转义，按原样保留
                }
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}
