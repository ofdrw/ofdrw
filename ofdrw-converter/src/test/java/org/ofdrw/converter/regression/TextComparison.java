package org.ofdrw.converter.regression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PDF 文本提取比较结果
 * <p>
 * 比较前会统一做规范化：按 NFKC 归一（连字、全角/半角差异）、去掉全部空白字符。
 * 这样只比较“文字内容”本身，不受换行、字距、文本块顺序带来的空格差异影响。
 * <p>
 * 文本按 UTF-8 编码的字节序列计算相似度（字节级最长公共子序列），既能反映字符缺失，
 * 也能反映字符错误（汉字乱码时字节不同）。相似度 = 2 × LCS / (基准长度 + 实际长度)。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class TextComparison {

    /**
     * 规范化后的基准文本
     */
    private final String expected;

    /**
     * 规范化后的实际文本
     */
    private final String actual;

    /**
     * 最长公共子序列长度（字节）
     */
    private final int commonLength;

    /**
     * 不排序口径下的最长公共子序列长度（字节），用于覆盖率
     */
    private int coverageCommonLength;

    /**
     * 不排序口径下的基准文本字节数，用于覆盖率
     */
    private int coverageExpectedBytes = -1;

    /**
     * 差异片段（最多保留 {@link #MAX_FRAGMENT} 条）
     */
    private final List<String> fragments;

    /**
     * 保留的差异片段最大条数
     */
    private static final int MAX_FRAGMENT = 10;

    /**
     * @param expected     规范化后的基准文本
     * @param actual       规范化后的实际文本
     * @param commonLength 最长公共子序列长度（字节）
     * @param fragments    差异片段
     */
    private TextComparison(String expected, String actual, int commonLength, List<String> fragments) {
        this.expected = expected;
        this.actual = actual;
        this.commonLength = commonLength;
        this.fragments = Collections.unmodifiableList(fragments);
    }

    /**
     * 比较两段文本
     *
     * @param expectedText 基准 PDF 提取的文本
     * @param actualText   转换得到的 PDF 提取的文本
     * @return 比较结果
     */
    public static TextComparison compare(String expectedText, String actualText) {
        return compare(expectedText, actualText, expectedText, actualText);
    }

    /**
     * 比较两段文本，覆盖率与相似度分别使用不同的提取口径
     * <p>
     * 背景（captain 裁决 B）：相似度要反映真实阅读顺序，用「按位置排序」的提取结果；
     * 覆盖率只关心“基准字符是否齐全”，用「不排序」的提取结果，
     * 避免 PDFBox 按字号做行合并时因字号口径不同（OFD 的 Size 是 mm，换算 pt 后数值大于基准标注）
     * 而把字符重排、误扣覆盖率。
     *
     * @param expectedText        基准文本（按位置排序）
     * @param actualText          实际文本（按位置排序）
     * @param expectedUnordered   基准文本（不排序）
     * @param actualUnordered     实际文本（不排序）
     * @return 比较结果
     */
    public static TextComparison compare(String expectedText, String actualText,
                                         String expectedUnordered, String actualUnordered) {
        String e = normalize(expectedText);
        String a = normalize(actualText);
        byte[] eb = e.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] ab = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int lcs = lcs(eb, ab);
        TextComparison result = new TextComparison(e, a, lcs, fragments(eb, ab));
        byte[] eub = normalize(expectedUnordered).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] aub = normalize(actualUnordered).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        result.coverageCommonLength = lcs(eub, aub);
        result.coverageExpectedBytes = eub.length;
        return result;
    }

    /**
     * 文本规范化：NFKC 归一（兼容连字与全半角）+ 去除空白
     *
     * @param text 原始文本，可为空
     * @return 规范化文本，入参为空时返回空串
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String nfkc = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC);
        StringBuilder sb = new StringBuilder(nfkc.length());
        for (int i = 0; i < nfkc.length(); i++) {
            char c = nfkc.charAt(i);
            if (!Character.isWhitespace(c) && !Character.isSpaceChar(c) && c != '\u00A0') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * @return 规范化后的基准文本
     */
    public String getExpected() {
        return expected;
    }

    /**
     * @return 规范化后的实际文本
     */
    public String getActual() {
        return actual;
    }

    /**
     * 文本相似度
     *
     * @return 取值 0~1，1 表示规范化后完全一致；两侧都为空时返回 1
     */
    public double getSimilarity() {
        if (expected.isEmpty() && actual.isEmpty()) {
            return 1d;
        }
        // LCS 在 UTF-8 字节上进行：两侧长度与公共长度单位一致，比例与按字符计算等价
        int expectedBytes = expected.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        int actualBytes = actual.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        return 2d * commonLength / (expectedBytes + actualBytes);
    }

    /**
     * 文本覆盖率：基准文本被实际文本覆盖的比例
     * <p>
     * 与相似度的区别：相似度对“多出来的文字”同样扣分，而覆盖率只看基准是否被完整还原。
     * 设计 3.8 要求注释外观（水印/图章）重绘进页面内容流，这类文字会出现在实际文本中但不在基准里，
     * 用相似度会误判为回归；覆盖率则是 1.0，正确反映“正文无丢失”。
     * <p>
     * 若实际文本比基准短（丢字），覆盖率会下降，因此它能抓住真正的文字缺失——
     * 这正是“防丢字”的指标，相似度则用于“防乱码/防多画”。
     *
     * @return 取值 0~1，1 表示基准文本被完全覆盖；基准为空时返回 1
     */
    public double getCoverage() {
        int expectedBytes = coverageExpectedBytes >= 0
                ? coverageExpectedBytes
                : expected.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (expectedBytes == 0) {
            return 1d;
        }
        int common = coverageExpectedBytes >= 0 ? coverageCommonLength : commonLength;
        return (double) common / expectedBytes;
    }

    /**
     * @return 差异片段（每条以 “-基准 / +实际” 成对描述）
     */
    public List<String> getFragments() {
        return fragments;
    }

    /**
     * @return 便于日志输出的单行描述
     */
    @Override
    public String toString() {
        return String.format("textSimilarity=%.4f expectedLen=%d actualLen=%d%s",
                getSimilarity(), expected.length(), actual.length(),
                fragments.isEmpty() ? "" : " fragments=" + fragments);
    }

    /**
     * 字节级最长公共子序列长度
     *
     * @param a 基准字节序列
     * @param b 实际字节序列
     * @return 公共子序列长度（字节）
     */
    private static int lcs(byte[] a, byte[] b) {
        if (a.length == 0 || b.length == 0) {
            return 0;
        }
        int[] prev = new int[b.length + 1];
        int[] cur = new int[b.length + 1];
        for (int i = 1; i <= a.length; i++) {
            cur[0] = 0;
            byte ai = a[i - 1];
            for (int j = 1; j <= b.length; j++) {
                if (ai == b[j - 1]) {
                    cur[j] = prev[j - 1] + 1;
                } else {
                    cur[j] = Math.max(prev[j], cur[j - 1]);
                }
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[b.length];
    }

    /**
     * 抽取前若干处差异片段，便于定位问题
     *
     * @param a 基准字节序列
     * @param b 实际字节序列
     * @return 差异片段
     */
    private static List<String> fragments(byte[] a, byte[] b) {
        List<String> out = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < a.length && j < b.length && out.size() < MAX_FRAGMENT) {
            if (a[i] == b[j]) {
                i++;
                j++;
                continue;
            }
            int startA = i;
            int startB = j;
            // 各自向前推进到下一个能重新对齐的位置（简单回溯：找最近的相同字节）
            while (i < a.length && (j >= b.length || a[i] != b[j])) {
                i++;
            }
            while (j < b.length && (i >= a.length || a[i] != b[j])) {
                j++;
            }
            String ea = new String(a, startA, Math.min(i - startA, 24), java.nio.charset.StandardCharsets.UTF_8);
            String eb = new String(b, startB, Math.min(j - startB, 24), java.nio.charset.StandardCharsets.UTF_8);
            out.add("-[" + ea + "] +[" + eb + "]");
        }
        return out;
    }
}
