package org.ofdrw.converter.pdf.render.text;

import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.text.CT_CGTransform;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.text.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * OFD 文字对象适配器：把 {@link TextObject} 转换为库无关的 {@link OfdText}
 * <p>
 * 本类是 {@code org.ofdrw.converter.pdf.render.text} 包中<b>唯一</b>接触 OFD 核心模型的类，
 * 布局与字形映射逻辑全部位于不依赖外部模型的类中，便于单独回归。
 * <p>
 * 适配内容：
 * <ul>
 *     <li>外接矩形、字号、HScale、CharDirection、ReadDirection、CTM 的取值与缺省值（表 45）；</li>
 *     <li>TextCode 的 X/Y 缺失时继承上一个 TextCode（表 46 备注）；</li>
 *     <li>DeltaX/DeltaY 中的 {@code g} 重复语法展开为数值队列（含末位尾部增量语义，见 {@link DeltaQueue}）；</li>
 *     <li>CGTransform 的 CodePosition/CodeCount/Glyphs（表 48）。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class OfdTextAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OfdTextAdapter.class);

    private OfdTextAdapter() {
    }

    /**
     * 把 OFD 文字对象转换为文字图元视图
     *
     * @param textObject OFD 文字对象，不能为空
     * @return 文字图元视图
     */
    public static OfdText adapt(TextObject textObject) {
        if (textObject == null) {
            throw new IllegalArgumentException("文字对象不能为空");
        }
        ST_Box boundary = textObject.getBoundary();
        OfdBox box = boundary == null ? new OfdBox(0, 0, 0, 0) : new OfdBox(
                value(boundary.getTopLeftX()), value(boundary.getTopLeftY()),
                value(boundary.getWidth()), value(boundary.getHeight()));

        double size = textObject.getSize() == null ? 0 : textObject.getSize();
        double hScale = textObject.getHScale() == null ? 1.0 : textObject.getHScale();
        double charDir = angleOf(textObject.getCharDirection());
        double readDir = angleOf(textObject.getReadDirection());
        double[] ctm = toCtm(textObject.getCTM());

        List<CgTransform> transforms = adaptTransforms(textObject.getCGTransforms());
        List<TextCodeRun> runs = new ArrayList<>();
        double prevX = 0;
        double prevY = 0;
        for (TextCode textCode : textObject.getTextCodes()) {
            // 表 46 备注：X/Y 不出现时采用上一个 TextCode 的值（相对外接矩形角点的偏移）
            double x = textCode.getX() == null ? prevX : textCode.getX();
            double y = textCode.getY() == null ? prevY : textCode.getY();
            prevX = x;
            prevY = y;
            runs.add(new TextCodeRun(textCode.getContent(), x, y,
                    parseDelta(textCode.getDeltaX(), "DeltaX"), parseDelta(textCode.getDeltaY(), "DeltaY"),
                    transforms));
        }
        return new OfdText(box, size, hScale, charDir, readDir, ctm, runs);
    }

    /**
     * 解析 Delta 队列：展开 {@code g <重复次数> <值>} 语法
     *
     * @param array OFD 数组，可为 null
     * @param name  属性名，用于日志
     * @return 偏移队列，未声明时返回空队列
     */
    static DeltaQueue parseDelta(ST_Array array, String name) {
        if (array == null) {
            return DeltaQueue.EMPTY;
        }
        List<String> raw = array.getArray();
        if (raw == null || raw.isEmpty()) {
            return DeltaQueue.EMPTY;
        }
        List<Double> values = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            String token = raw.get(i) == null ? "" : raw.get(i).trim();
            if ("g".equals(token)) {
                // g 重复语法：g <重复次数> <值>
                if (i + 2 >= raw.size()) {
                    logger.warn("{} 中的 g 重复语法不完整，已忽略：{}", name, raw);
                    break;
                }
                int repeat;
                try {
                    repeat = Integer.parseInt(raw.get(i + 1).trim());
                } catch (NumberFormatException e) {
                    logger.warn("{} 中的 g 重复次数非法，已忽略：{}", name, raw);
                    break;
                }
                double value = parseNumber(raw.get(i + 2), name);
                for (int k = 0; k < repeat; k++) {
                    values.add(value);
                }
                i += 2;
            } else if (!token.isEmpty()) {
                values.add(parseNumber(token, name));
            }
        }
        return values.isEmpty() ? DeltaQueue.EMPTY : new DeltaQueue(values);
    }

    /**
     * 解析数值，失败时按 0 处理
     *
     * @param text 数值文本
     * @param name 属性名，用于日志
     * @return 数值
     */
    private static double parseNumber(String text, String name) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            logger.warn("{} 中的数值非法，按 0 处理：{}", name, text);
            return 0d;
        }
    }

    /**
     * 转换 CGTransform 列表
     *
     * @param transforms OFD 字形变换列表，可为 null
     * @return 字形变换列表
     */
    private static List<CgTransform> adaptTransforms(List<CT_CGTransform> transforms) {
        if (transforms == null || transforms.isEmpty()) {
            return new ArrayList<>();
        }
        List<CgTransform> result = new ArrayList<>(transforms.size());
        for (CT_CGTransform transform : transforms) {
            Integer position;
            try {
                position = transform.getCodePosition();
            } catch (Exception e) {
                logger.warn("CGTransform 缺少 CodePosition，已忽略该变换", e);
                continue;
            }
            int codeCount = transform.getCodeCount() == null ? 1 : transform.getCodeCount();
            List<Integer> glyphs = new ArrayList<>();
            ST_Array glyphArray = transform.getGlyphs();
            if (glyphArray != null && glyphArray.getArray() != null) {
                for (String item : glyphArray.getArray()) {
                    try {
                        glyphs.add(Integer.parseInt(item.trim()));
                    } catch (NumberFormatException e) {
                        logger.warn("CGTransform 中的字形索引非法，已忽略：{}", item);
                    }
                }
            }
            result.add(new CgTransform(position, codeCount, glyphs));
        }
        return result;
    }

    /**
     * 获取方向角度
     *
     * @param direction 方向，可为 null
     * @return 角度，单位 °，缺省 0
     */
    private static double angleOf(Direction direction) {
        if (direction == null) {
            return 0;
        }
        try {
            return Double.parseDouble(direction.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 转换 CTM 数组
     *
     * @param ctm OFD CTM 数组，可为 null
     * @return {@code [a b c d e f]}，无 CTM 时返回 null
     */
    private static double[] toCtm(ST_Array ctm) {
        if (ctm == null || ctm.getArray() == null || ctm.getArray().size() != 6) {
            return null;
        }
        double[] result = new double[6];
        for (int i = 0; i < 6; i++) {
            result[i] = parseNumber(ctm.getArray().get(i), "CTM");
        }
        return result;
    }

    /**
     * 空值安全取值
     *
     * @param value 可能为 null 的值
     * @return 数值，null 按 0 处理
     */
    private static double value(Double value) {
        return value == null ? 0d : value;
    }
}
