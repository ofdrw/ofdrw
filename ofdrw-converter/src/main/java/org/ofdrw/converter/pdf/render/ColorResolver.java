package org.ofdrw.converter.pdf.render;

import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.colorSpace.BitsPerComponent;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.color.colorSpace.OFDColorSpaceType;
import org.ofdrw.core.pageDescription.color.colorSpace.Palette;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 颜色解析器（P3）：OFD 颜色/颜色空间 → 中间模型 {@link PdfColor}
 * <p>
 * 覆盖设计 3.4 节的全部颜色语义：
 * <ol>
 *     <li><b>颜色空间</b>：{@code ColorSpace} 引用指向的 GRAY / RGB / CMYK；无引用时按通道数量推断
 *     （1 → GRAY、3 → RGB、4 → CMYK）；</li>
 *     <li><b>通道值</b>：支持十进制与 {@code #RRGGBB} 十六进制；取值 &gt; 1 时按 0~255 归一化；
 *     <b>颜色空间声明了 {@code BitsPerComponent} 时按位宽归一化</b>（8 位为 /255，4 位为 /15，1 位为 /1）；</li>
 *     <li><b>调色板</b>：{@code Index} 属性按调色板索引取通道值，调色板缺失或越界时降级并告警；</li>
 *     <li><b>ICC 颜色空间</b>：{@code Profile} 指向 ICC 配置时，PDF 侧需要 ICCBased 颜色空间才能保真，
 *     当前实现按同一颜色空间的 Device 操作符输出（GRAY/RGB/CMYK），并输出 warning 标记偏差；</li>
 *     <li><b>颜色 Alpha</b>（表 26）：换算为 0~1 的不透明度写入 {@link PdfColor#getOpacity()}，
 *     缺省 255（不透明）；与图元 Alpha 的相乘由渲染器在生成 {@code SetAlpha} 时完成。</li>
 * </ol>
 * 颜色通道值一律保留原颜色空间：<b>CMYK 不转 RGB</b>（设计 3.4 节，否则黑色文字会发灰）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ColorResolver {

    private static final Logger logger = LoggerFactory.getLogger(ColorResolver.class);

    /**
     * 颜色空间查找函数
     */
    private final ColorSpaceLookup lookup;

    /**
     * 已告警原因（同一原因只告警一次，避免大文档刷屏）
     */
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    /**
     * 颜色空间查找函数：把颜色引用的 ID 解析为颜色空间定义（含调色板与 ICC 引用）
     */
    @FunctionalInterface
    public interface ColorSpaceLookup {

        /**
         * 按 ID 查找颜色空间
         *
         * @param id 颜色空间 ID（{@code ST_ID} 的字符串形式）
         * @return 颜色空间；{@code null} 表示不存在
         */
        CT_ColorSpace getColorSpace(String id);
    }

    /**
     * 构造颜色解析器
     *
     * @param lookup 颜色空间查找函数，可为空（此时按通道数量推断颜色空间，调色板颜色无法解析）
     */
    public ColorResolver(ColorSpaceLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * 解析 OFD 颜色
     *
     * @param color 颜色元素，可为空
     * @param usage 颜色用途（“填充”“描边”等），仅用于告警文案
     * @return 中间模型颜色；颜色为空、或颜色对象为渐变/底纹（由 {@link ShadingRenderer}/{@link PatternRenderer} 处理）时返回 {@code null}
     */
    public PdfColor resolve(CT_Color color, String usage) {
        if (color == null) {
            return null;
        }
        if (color.getColorByType() != null) {
            // 渐变/底纹属于复杂色对象，由渐变与底纹渲染器处理，此处不给出单色
            return null;
        }
        CT_ColorSpace space = colorSpace(color);
        OFDColorSpaceType type = colorSpaceType(color, space);
        ST_Array value = colorValue(color, space, usage);
        if (value == null || value.getArray() == null || value.getArray().isEmpty()) {
            warnOnce("empty:" + usage, "颜色缺少通道值（" + usage + "），按黑色处理");
            return null;
        }
        int count = type == OFDColorSpaceType.GRAY ? 1 : type == OFDColorSpaceType.CMYK ? 4 : 3;
        if (space != null && space.getProfile() != null) {
            // ICC 配置需要 PDF 侧的 ICCBased 颜色空间才能保真，当前按同颜色空间的 Device 操作符输出
            warnOnce("icc:" + usage, "颜色空间引用 ICC 配置（" + usage + "），已按 Device "
                    + type + " 输出，存在色彩偏差（后续可升级为 ICCBased）");
        }
        List<String> items = value.getArray();
        if (items.size() < count) {
            warnOnce("short:" + usage, type + " 颜色通道不足 " + count + " 个（" + usage + "），缺失通道按 0 处理");
        }
        int max = maxChannelValue(space);
        float[] channels = new float[count];
        for (int i = 0; i < count; i++) {
            channels[i] = channel(i < items.size() ? items.get(i) : null, max, usage);
        }
        float opacity = opacity(color.getAlpha());
        return new PdfColor(midModelSpace(type), opacity, channels);
    }

    /**
     * 解析渐变色段中的颜色
     * <p>
     * OFD 表 29/30 的颜色段（Segment）内部的颜色有两种写法：
     * <ul>
     *     <li>基本颜色：直接以 {@code Value} 属性与 {@code Alpha} 属性出现在颜色元素上；</li>
     *     <li>颜色族：子元素为 {@code Pattern} / {@code AxialShd} / {@code RadialShd} 等。</li>
     * </ul>
     * {@code CT_Color#getColor()} 只按子元素 {@code <Color>} 解析，两种写法都取不到，
     * 因此这里直接读取段的第一个子元素并以其为颜色元素解析。
     * GB/T 33190 11.2：颜色段中的颜色“应是基本颜色”，出现颜色族时按渐变嵌套不支持处理（降级为默认黑 + 告警）。
     *
     * @param segment OFD 颜色段，可为空
     * @param usage   用途文案
     * @return 中间模型颜色；颜色缺失或为颜色族时返回 {@code null}
     */
    public PdfColor resolveSegmentColor(org.ofdrw.core.pageDescription.color.color.Segment segment, String usage) {
        if (segment == null) {
            return null;
        }
        List<org.dom4j.Element> elements = segment.elements();
        if (elements.isEmpty()) {
            warnOnce("segment-empty:" + usage, usage + " 缺少颜色，按默认黑色处理");
            return null;
        }
        org.dom4j.Element element = elements.get(0);
        String name = element.getQualifiedName();
        if (name != null && !name.endsWith("FillColor") && !name.endsWith("StrokeColor")
                && !name.endsWith("Color")) {
            warnOnce("segment-cluster:" + usage, usage + " 使用了颜色族（" + name
                    + "），渐变内嵌渐变不受支持，按默认黑色处理");
            return null;
        }
        return resolve(new CT_Color((org.dom4j.Element) element.clone()), usage);
    }

    /**
     * 解析颜色并叠加图元 Alpha（决策 2：缺省不透明；颜色 Alpha 与图元 Alpha 相乘）
     *
     * @param color       颜色元素，可为空
     * @param unitAlpha   OFD 图元 Alpha，取值 0~255；{@code null} 表示属性缺失（按不透明处理）
     * @param usage       颜色用途，仅用于告警文案
     * @return 不透明度已乘上图元 Alpha 的颜色；颜色为空时返回 {@code null}
     */
    public PdfColor resolve(CT_Color color, Integer unitAlpha, String usage) {
        PdfColor base = resolve(color, usage);
        if (base == null) {
            return null;
        }
        float unit = opacity(unitAlpha);
        return base.withOpacity(base.getOpacity() * unit);
    }

    /**
     * 计算颜色与图元合成后的不透明度
     *
     * @param colorAlpha 颜色 Alpha，取值 0~255；{@code null} 表示缺省
     * @param unitAlpha  图元 Alpha，取值 0~255；{@code null} 表示缺省
     * @return 不透明度，0~1
     */
    public static float compositeOpacity(Integer colorAlpha, Integer unitAlpha) {
        return opacity(colorAlpha) * opacity(unitAlpha);
    }

    /**
     * 取已解析颜色的不透明度
     *
     * @param color 已解析颜色，可为空
     * @return 不透明度，0~1；颜色为空时返回 1（不透明）
     */
    public static float colorOpacity(org.ofdrw.converter.pdf.model.PdfColor color) {
        return color == null ? 1f : color.getOpacity();
    }

    /**
     * OFD Alpha（0~255）换算为不透明度（0~1）
     * <p>
     * 决策 2：属性缺失（{@code null}）按不透明处理；显式 0 才是完全透明。
     *
     * @param alpha0to255 OFD Alpha 值，可为空
     * @return 不透明度，0~1
     */
    public static float opacity(Integer alpha0to255) {
        if (alpha0to255 == null) {
            return 1f;
        }
        int value = Math.max(0, Math.min(255, alpha0to255));
        return value / 255f;
    }

    /**
     * 中间模型颜色空间
     *
     * @param type OFD 颜色空间类型
     * @return 中间模型颜色空间
     */
    private static PdfColor.ColorSpace midModelSpace(OFDColorSpaceType type) {
        if (type == OFDColorSpaceType.GRAY) {
            return PdfColor.ColorSpace.GRAY;
        }
        if (type == OFDColorSpaceType.CMYK) {
            return PdfColor.ColorSpace.CMYK;
        }
        return PdfColor.ColorSpace.RGB;
    }

    /**
     * 查找颜色的颜色空间定义
     *
     * @param color 颜色元素
     * @return 颜色空间；无引用或查找失败时返回 {@code null}
     */
    private CT_ColorSpace colorSpace(CT_Color color) {
        ST_RefID ref = color.getColorSpace();
        if (ref == null || lookup == null) {
            return null;
        }
        try {
            String id = GraphicsStateResolver.idOf(ref);
            if (id == null) {
                return null;
            }
            CT_ColorSpace space = lookup.getColorSpace(id);
            if (space == null) {
                warnOnce("space-missing:" + id, "颜色空间 " + id + " 不存在，按通道数量推断颜色空间");
            }
            return space;
        } catch (RuntimeException e) {
            warnOnce("space-error", "颜色空间解析失败：" + e.getMessage());
            return null;
        }
    }

    /**
     * 颜色空间类型：优先取颜色空间声明，其次按通道数量推断
     *
     * @param color 颜色元素
     * @param space 颜色空间，可为空
     * @return 颜色空间类型
     */
    private OFDColorSpaceType colorSpaceType(CT_Color color, CT_ColorSpace space) {
        if (space != null) {
            try {
                if (space.getType() != null) {
                    return space.getType();
                }
            } catch (RuntimeException e) {
                warnOnce("space-type", "颜色空间缺少类型声明，按通道数量推断");
            }
        }
        ST_Array value = color.getValue();
        int size = value == null || value.getArray() == null ? 0 : value.getArray().size();
        if (color.getIndex() != null || size == 1) {
            return OFDColorSpaceType.GRAY;
        }
        if (size == 4) {
            return OFDColorSpaceType.CMYK;
        }
        return OFDColorSpaceType.RGB;
    }

    /**
     * 取颜色的通道值：有 {@code Index} 时按调色板查表
     *
     * @param color 颜色元素
     * @param space 颜色空间，可为空
     * @param usage 颜色用途，仅用于告警文案
     * @return 通道值数组，可为空
     */
    private ST_Array colorValue(CT_Color color, CT_ColorSpace space, String usage) {
        Integer index = color.getIndex();
        if (index == null) {
            return color.getValue();
        }
        if (space == null) {
            warnOnce("palette-space:" + usage, "颜色使用调色板索引但缺少颜色空间定义（" + usage + "），按黑色处理");
            return null;
        }
        try {
            Palette palette = space.getPalette();
            if (palette == null) {
                warnOnce("palette-missing:" + usage, "颜色空间缺少调色板（" + usage + "），按黑色处理");
                return null;
            }
            ST_Array value = getPaletteColor(palette, index);
            if (value == null) {
                warnOnce("palette-range:" + usage, "调色板索引越界：" + index + "（" + usage + "），按黑色处理");
                return null;
            }
            return value;
        } catch (RuntimeException e) {
            warnOnce("palette-error:" + usage, "调色板解析失败：" + e.getMessage());
            return null;
        }
    }

    /**
     * 按索引读取调色板颜色
     *
     * @param palette 调色板
     * @param index   颜色索引，从 0 开始
     * @return 颜色通道值；越界时返回 {@code null}
     */
    private static ST_Array getPaletteColor(Palette palette, int index) {
        java.util.List<org.ofdrw.core.pageDescription.color.colorSpace.CV> cvs = palette.getCVs();
        if (cvs == null || index < 0 || index >= cvs.size()) {
            return null;
        }
        return cvs.get(index).getColor();
    }

    /**
     * 通道值的归一化上限
     * <p>
     * 设计 3.4 节：{@code BitsPerComponent} 声明时按位宽归一化（8 位 255、4 位 15、1 位 1），
     * 未声明时按 8 位（255）处理。
     *
     * @param space 颜色空间，可为空
     * @return 归一化上限
     */
    private static int maxChannelValue(CT_ColorSpace space) {
        if (space == null) {
            return 255;
        }
        try {
            /* BitsPerComponent 未声明时 getBitsPerComponent() 返回 BIT_1（规范要求的缺省值是 8 位），
               不能用于判断“是否显式声明”；显式访问器缺失返回 null，此时按 GB/T 33190 8.3.2 取 8 位（255）。 */
            BitsPerComponent bits = space.getBitsPerComponentExplicit();
            if (bits == null) {
                return 255;
            }
            int value = bits.getValue();
            return value > 0 && value < 32 ? (1 << value) - 1 : 255;
        } catch (RuntimeException e) {
            return 255;
        }
    }

    /**
     * 读取单个通道并归一化到 0~1
     *
     * @param text 通道原始文本，可为空
     * @param max  归一化上限（255 / 15 / 1 等）
     * @param usage 颜色用途，仅用于告警文案
     * @return 归一化通道值，0~1
     */
    private float channel(String text, int max, String usage) {
        if (text == null || text.trim().isEmpty()) {
            return 0f;
        }
        String value = text.trim();
        double raw;
        try {
            raw = value.startsWith("#") ? Integer.parseInt(value.substring(1), 16) : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            warnOnce("channel:" + usage, "颜色通道非法（" + value + "，" + usage + "），已按 0 处理");
            return 0f;
        }
        if (raw < 0) {
            warnOnce("channel-range:" + usage, "颜色通道越界（" + value + "，" + usage + "），已按边界截断");
            return 0f;
        }
        /*
         * GB/T 33190 8.3.2 的通道值有两种合法写法：
         * 1) 量化值（整数语义，"128"、"200"）：按颜色空间位宽归一化（8 位 /255、4 位 /15）；
         * 2) 归一化值（小数语义，"0.2"、"0.6"）：本身就是 0~1，直接使用。
         * 因此只有“整数且大于 1”才按量化值处理，其余一律视为已归一化并截断到 1。
         */
        if (raw == Math.rint(raw) && raw > 1) {
            return (float) Math.min(1d, raw / max);
        }
        return (float) Math.min(1d, raw);
    }

    /**
     * 输出一次告警（相同原因只告警一次）
     *
     * @param key     告警分类键
     * @param message 告警内容
     */
    private void warnOnce(String key, String message) {
        if (reported.add(key)) {
            logger.warn("[{}] {}", RenderContext.DIAGNOSTIC_TAG, message);
        }
    }
}
