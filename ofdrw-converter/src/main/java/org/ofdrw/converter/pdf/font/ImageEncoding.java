package org.ofdrw.converter.pdf.font;

import java.util.Locale;

/**
 * 图像编码类型
 * <p>
 * 设计 3.7 节与决策 23（保真优先）：
 * <ul>
 *     <li>{@link #JPEG}：原始 JPEG 字节<b>原样嵌入</b>（{@code DCTDecode}），不重新编码；</li>
 *     <li>其余编码统一为无损嵌入，透明通道通过 Alpha / Soft Mask 表达。</li>
 * </ul>
 * 本枚举不依赖任何渲染库，可由渲染器无关的单元测试直接断言。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public enum ImageEncoding {
    /**
     * JPEG（有损），PDF 过滤器 {@code DCTDecode}
     */
    JPEG(true),
    /**
     * PNG（无损），PDF 过滤器 {@code FlateDecode} + PNG 预测器
     */
    PNG(false),
    /**
     * 原始像素数据（无损），PDF 过滤器 {@code FlateDecode}
     */
    RAW(false),
    /**
     * JPEG 2000（有损），PDF 过滤器 {@code JPXDecode}
     */
    JPEG2000(true),
    /**
     * JBIG2（无损），PDF 过滤器 {@code JBIG2Decode}
     */
    JBIG2(false),
    /**
     * CCITT G4（无损），PDF 过滤器 {@code CCITTFaxDecode}
     */
    CCITT(false);

    /**
     * 是否为有损编码
     */
    private final boolean lossy;

    ImageEncoding(boolean lossy) {
        this.lossy = lossy;
    }

    /**
     * @return true 表示有损编码，原始数据必须原样嵌入、禁止重新编码
     */
    public boolean isLossy() {
        return lossy;
    }

    /**
     * 由 OFD 图像 Format 属性解析编码类型
     *
     * @param format OFD ImageObject 的 Format 属性，如 {@code JPG}、{@code JPEG}、{@code PNG}，可为空
     * @return 图像编码，无法识别时返回 {@link #RAW}
     */
    public static ImageEncoding ofFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            return RAW;
        }
        String name = format.trim().toUpperCase(Locale.ROOT);
        if ("JPG".equals(name) || "JPEG".equals(name)) {
            return JPEG;
        }
        if ("PNG".equals(name)) {
            return PNG;
        }
        if ("JPX".equals(name) || "JP2".equals(name) || "JPEG2000".equals(name)) {
            return JPEG2000;
        }
        if ("JBIG2".equals(name)) {
            return JBIG2;
        }
        if ("CCITT".equals(name) || "CCITTFAX".equals(name)) {
            return CCITT;
        }
        return RAW;
    }
}
