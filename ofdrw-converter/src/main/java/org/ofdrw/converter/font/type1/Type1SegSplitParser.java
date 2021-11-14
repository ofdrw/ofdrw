package org.ofdrw.converter.font.type1;

import org.apache.commons.io.IOUtils;
import org.apache.fontbox.pfb.PfbParser;
import org.apache.fontbox.type1.Type1Font;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 将Type1 字体分段
 * <p>
 * https://adobe-type-tools.github.io/font-tech-notes/pdfs/T1_SPEC.pdf
 *
 * @author 权观宇
 * @since 2021-10-24 16:44:53
 */
public class Type1SegSplitParser {
    /**
     * ASCII 段 结束标志
     * <p>
     * eexec\n
     */
    private final static byte[] EEXEC = new byte[]{
            0x65, 0x65, 0x78, 0x65, 0x63, 0x0d
    };

    /**
     * 尝试解析Type1 字体
     *
     * @param raw 字体
     * @return Type1 字体或 null
     * @throws IOException IOE
     */
    public static Type1Font parse(byte[] raw) throws IOException {
        final byte[][] segments = split(raw);
        if (segments == null) {
            return null;
        }
        return Type1Font.createWithSegments(segments[0], segments[1]);
    }

    /**
     * 尝试解析Type1 字体
     *
     * @param in 字体流
     * @return Type1 字体或 null
     * @throws IOException IOE
     */
    public static Type1Font parse(InputStream in) throws IOException {
        final byte[] buf = IOUtils.toByteArray(in);
        return parse(buf);
    }

    /**
     * 判断是否是Type1 字体
     *
     * @param raw 字体内容
     * @return true - type1 字体；false - 非 type1
     */
    public static boolean isType1(byte[] raw) {
        if (raw == null || raw.length <= 2) {
            return false;
        }
        if ((raw[0] & 0xff) == 0x80 && (raw[1] & 0xff) == 0x01) {
            return true;
        }
        if (raw[0] == '%' && raw[1] == '!') {
            return true;
        }
        return false;
    }

    /**
     * Tyep1 字体 格式分段
     *
     * @param raw 字体字节
     * @return 分段或null
     * @throws IOException IO异常
     */
    public static byte[][] split(byte[] raw) throws IOException {
        if (raw == null || raw.length <= 2) {
            return null;
        }

        // 兼容原有解析方式
        if ((raw[0] & 0xff) == 0x80 && (raw[1] & 0xff) == 0x01) {
            PfbParser pfbParser = new PfbParser(raw);
            return new byte[][]{
                    pfbParser.getSegment1(), pfbParser.getSegment2()
            };
        }


        int off = indexOf(raw, EEXEC);
        if (off == -1) {
            return null;
        }

        off += EEXEC.length;
        byte[] asciiSeg = Arrays.copyOf(raw, off);
        byte[] binSeg = Arrays.copyOfRange(raw, off, raw.length);
        return new byte[][]{
                asciiSeg, binSeg
        };

    }


    /**
     * 查找目标字节串偏移量
     * <p>
     * copy from guava from com.google.common.primitives.Bytes
     *
     * @param array  数组
     * @param target 查找目标
     * @return 偏移量
     */
    public static int indexOf(byte[] array, byte[] target) {
        if (target.length == 0) {
            return 0;
        }

        outer:
        for (int i = 0; i < array.length - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
