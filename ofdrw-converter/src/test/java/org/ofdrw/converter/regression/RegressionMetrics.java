package org.ofdrw.converter.regression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 单个样本的回归指标汇总
 * <p>
 * 汇总逐页的像素比较结果与全文的文本提取比较结果，并给出是否满足通过阈值。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class RegressionMetrics {

    /**
     * 样本名（不含扩展名）
     */
    private final String sample;

    /**
     * 渲染分辨率（dpi）
     */
    private final float dpi;

    /**
     * 逐页像素比较结果，下标对应页码（从 0 起）
     */
    private final List<PageComparison> pages;

    /**
     * 基准 PDF 页数
     */
    private final int expectedPageCount;

    /**
     * 转换得到的 PDF 页数
     */
    private final int actualPageCount;

    /**
     * 文本比较结果
     */
    private final TextComparison text;

    /**
     * 未参与比较的原因（如基准 PDF 缺失），为空表示已比较
     */
    private final String skippedReason;

    /**
     * 单个页面的比较结果
     */
    public static final class PageComparison {

        /**
         * 页码，从 0 起
         */
        private final int index;

        /**
         * 像素比较结果，为空表示该页未比较（基准缺页）
         */
        private final ImageDiff diff;

        /**
         * 构造逐页比较结果
         *
         * @param index 页码，从 0 起
         * @param diff  像素比较结果，可为空
         */
        public PageComparison(int index, ImageDiff diff) {
            this.index = index;
            this.diff = diff;
        }

        /**
         * @return 页码，从 0 起
         */
        public int getIndex() {
            return index;
        }

        /**
         * @return 像素比较结果，可为空
         */
        public ImageDiff getDiff() {
            return diff;
        }
    }

    /**
     * @param sample            样本名
     * @param dpi               渲染分辨率（dpi）
     * @param pages             逐页比较结果
     * @param expectedPageCount 基准页数
     * @param actualPageCount   实际页数
     * @param text              文本比较结果，可为空
     * @param skippedReason     跳过原因，可为空
     */
    /**
     * 是否属于“已知差异”样本（纯字体替换导致像素差异，决策 7：替换只告警、不阻断）
     */
    private boolean knownDifference;

    public RegressionMetrics(String sample, float dpi, List<PageComparison> pages,
                             int expectedPageCount, int actualPageCount,
                             TextComparison text, String skippedReason) {
        this.sample = sample;
        this.dpi = dpi;
        this.pages = Collections.unmodifiableList(new ArrayList<>(pages));
        this.expectedPageCount = expectedPageCount;
        this.actualPageCount = actualPageCount;
        this.text = text;
        this.skippedReason = skippedReason;
    }

    /**
     * @return 样本名（不含扩展名）
     */
    public String getSample() {
        return sample;
    }

    /**
     * @return 渲染分辨率（dpi）
     */
    public float getDpi() {
        return dpi;
    }

    /**
     * @return 逐页像素比较结果
     */
    public List<PageComparison> getPages() {
        return pages;
    }

    /**
     * @return 基准 PDF 页数
     */
    public int getExpectedPageCount() {
        return expectedPageCount;
    }

    /**
     * @return 转换得到的 PDF 页数
     */
    public int getActualPageCount() {
        return actualPageCount;
    }

    /**
     * @return 文本比较结果，可为空
     */
    public TextComparison getText() {
        return text;
    }

    /**
     * @return 未参与比较的原因，为空表示已比较
     */
    public String getSkippedReason() {
        return skippedReason;
    }

    /**
     * 最差的差异像素比例（全部页取最大值）
     *
     * @return 取值 0~1；没有可比页面时返回 0
     */
    public double getWorstDiffRatio() {
        double worst = 0d;
        for (PageComparison page : pages) {
            if (page.diff != null) {
                worst = Math.max(worst, page.diff.getDiffRatio());
            }
        }
        return worst;
    }

    /**
     * 最差的平均绝对误差（全部页取最大值）
     *
     * @return 取值 0~1；没有可比页面时返回 0
     */
    public double getWorstMeanAbsoluteError() {
        double worst = 0d;
        for (PageComparison page : pages) {
            if (page.diff != null) {
                worst = Math.max(worst, page.diff.getMeanAbsoluteError());
            }
        }
        return worst;
    }

    /**
     * 判断像素指标是否满足阈值
     *
     * @param maxDiffRatio 允许的最大差异像素比例，取值 0~1
     * @param maxMae       允许的最大平均绝对误差，取值 0~1（2/255 写作 2d/255d）
     * @return 所有可比页面均满足阈值返回 true
     */
    public boolean isPixelPassed(double maxDiffRatio, double maxMae) {
        for (PageComparison page : pages) {
            if (page.diff == null) {
                continue;
            }
            if (page.diff.getDiffRatio() > maxDiffRatio || page.diff.getMeanAbsoluteError() > maxMae) {
                return false;
            }
        }
        return true;
    }

    /**
     * 页数是否与基准一致
     *
     * @return 一致返回 true
     */
    public boolean isPageCountPassed() {
        return expectedPageCount == actualPageCount;
    }

    /**
     * 文本比较是否满足阈值
     *
     * @param minSimilarity 允许的最小文本相似度，取值 0~1
     * @return 满足返回 true；无文本比较结果时返回 true
     */
    /**
     * 判定并标记“已知差异”
     * <p>
     * 条件同时满足才标记（captain 裁决）：
     * <ol>
     *     <li>导出过程中发生了 <b>字体替换</b>（决策 7：替换只告警、不阻断）；</li>
     *     <li>页数一致且<b>文本达标</b>（覆盖率 ≥ 阈值），即内容没有丢失；</li>
     *     <li><b>仅像素指标</b>超阈值——差异由替换字体的字形/字宽不同造成。</li>
     * </ol>
     * 任一条件不满足（尤其是文本覆盖不足：说明是真实缺陷）都不得标记，
     * 避免把真实缺陷藏进“已知差异”。
     *
     * @param fontSubstitutionCount 本次导出发生替换的字体数量
     * @param maxDiff               差异像素比例阈值（0~1）
     * @param maxMae                平均绝对误差阈值（0~1）
     * @param minText               文本覆盖率阈值（0~1）
     * @return this，便于链式调用
     */
    public RegressionMetrics markKnownDifferenceIfFontSubstituted(int fontSubstitutionCount,
                                                                  double maxDiff, double maxMae, double minText) {
        this.knownDifference = fontSubstitutionCount > 0
                && skippedReason == null
                && isPageCountPassed()
                && isTextPassed(minText)
                && !isPixelPassed(maxDiff, maxMae);
        return this;
    }

    /**
     * @return 是否标记为“已知差异”（纯字体替换导致）
     */
    public boolean isKnownDifference() {
        return knownDifference;
    }

    /**
     * 相似度预警线：覆盖率达标但相似度低于该值时在报告中警示
     */
    public static final double SUSPICIOUS_SIMILARITY = 0.5d;

    public boolean isTextPassed(double minSimilarity) {
        /*
         * 文本达标只看覆盖率（基准文本是否被完整还原），相似度降为辅助指标（captain 裁决 A）：
         * 设计 3.8 要求注释外观（水印/图章）重绘进页面内容流，其文字必然出现在实际文本中、
         * 却不在基准 PDF 的文本层里，用相似度会把这类情况误判为回归；
         * 覆盖率对「多出文字」不扣分、对「丢失文字」严格扣分，语义与质量目标 2 一致。
         * 相似度低于预警线时由 {@link #isTextSuspicious(double)} 单独警示，不参与通过判定。
         */
        return text == null || text.getCoverage() >= minSimilarity;
    }

    /**
     * 文本异常预警：基准文本完整，但实际文本严重膨胀或乱码
     * <p>
     * 覆盖率高说明基准文字都在，但相似度异常低意味着实际文本多出大量无关内容或编码错乱，
     * 这通常不是注释外观重绘造成的，需要在报告中单独提示（不作为通过判定）。
     *
     * @param minSimilarity 覆盖率阈值
     * @return true 表示需要警示
     */
    public boolean isTextSuspicious(double minSimilarity) {
        return text != null && text.getCoverage() >= minSimilarity && text.getSimilarity() < SUSPICIOUS_SIMILARITY;
    }

    /**
     * @return 便于日志与报告输出的单行描述
     */
    @Override
    public String toString() {
        if (skippedReason != null) {
            return sample + " SKIP(" + skippedReason + ")";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(sample)
                .append(" dpi=").append(dpi)
                .append(" pages=").append(actualPageCount).append("/").append(expectedPageCount)
                .append(String.format(" worstDiff=%.4f%% worstMAE=%.4f/255", getWorstDiffRatio() * 100d, getWorstMeanAbsoluteError() * 255d));
        if (text != null) {
            sb.append(String.format(" text=%.4f", text.getSimilarity()));
        }
        return sb.toString();
    }
}
