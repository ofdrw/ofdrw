package org.ofdrw.layout.element;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.font.Font;
import org.ofdrw.layout.Rectangle;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

public class SpanTest {

    private static final Font DEFAULT_FONT = new Font("宋体", "宋体",
            Paths.get("src/test/resources/simhei-cut1.ttf").toAbsolutePath());

    @Test
    public void split() {
        Span span = new Span("你好OFD R&W");
        Span[] split = span.split(2);
        System.out.println();
        Assertions.assertEquals(split[0].getText(), "你好");
        Assertions.assertEquals(split[1].getText(), "OFD R&W");
    }

    @Test
    public void blockSize() {
        Span span = new Span("你好OFD R&W")
                .setFont(DEFAULT_FONT)
                .setFontSize(10d);
        Rectangle rectangle = span.blockSize();
        // 高度取字号（字体已绑定文件且带字符宽度映射表时按字号计算）
        Assertions.assertEquals(11.40625, rectangle.getHeight(), 0.01);
        // 宽度 = 各字符字宽之和，结果缓存，重复调用应一致
        double width = rectangle.getWidth();
        Assertions.assertTrue(width > 0, "字宽应为正数");
        Assertions.assertEquals(width, span.blockSize().getWidth(), 1e-9);

        // 设置字间距后，整行宽度增加 字符数 × 字间距（9 个字符）
        span.setLetterSpacing(3d);
        Assertions.assertEquals(width + 9 * 3, span.blockSize().getWidth(), 1e-9);

    }

    @Test
    public void getDeltaX() {
        // 显式绑定仓库内字体：不绑定字体文件时会按字体名查找系统字体，结果随运行环境而变
        Span span = new Span("你好OFD R&W")
                .setFont(DEFAULT_FONT)
                .setFontSize(10d);
        Double[] deltaX = span.getDeltaX();
        System.out.println(Arrays.toString(deltaX));
        // "你好OFD R&W" 共 9 个字符，产生 8 个偏移值（忽略最后一个字符）
        Assertions.assertEquals(8, deltaX.length);
        // 每个字符都有正的排版宽度（有字形时取真实字形宽度，缺字形时回退整字宽度）
        for (int i = 0; i < deltaX.length; i++) {
            Assertions.assertTrue(deltaX[i] > 0, "第 " + i + " 个字符应有正的字宽：" + Arrays.toString(deltaX));
        }
        // 与 blockSize 的宽度口径一致：偏移值之和 = 除末字符外的宽度累计
        double sum = 0;
        for (Double v : deltaX) {
            sum += v;
        }
        double blockWidth = new Span("你好OFD R&W").setFont(DEFAULT_FONT).setFontSize(10d).blockSize().getWidth();
        Assertions.assertTrue(sum < blockWidth, "末字符的宽度不应计入偏移值");
    }

    @Test
    public void splitLineBreak() {
        Span span = new Span("line 1\n");
        LinkedList<Span> spans = span.splitLineBreak();
        Assertions.assertEquals(spans.size(), 1);

        span = new Span("line 1\nline 2");
        spans = span.splitLineBreak();
        Assertions.assertEquals(spans.size(), 2);

        span = new Span("line 1\n\nline 3");
        spans = span.splitLineBreak();
        Assertions.assertEquals(spans.size(), 3);
        span = new Span("line 1\n\n\nline 4");
        spans = span.splitLineBreak();
        Assertions.assertEquals(spans.size(), 4);

    }
}
