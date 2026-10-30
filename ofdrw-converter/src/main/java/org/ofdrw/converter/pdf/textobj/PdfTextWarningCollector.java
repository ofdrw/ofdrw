package org.ofdrw.converter.pdf.textobj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 文字对象化降级告警聚合器
 * <p>
 * 按“原因 + 字体 + 页”聚合降级告警：同一页、同一字体、同一原因只记录一次，
 * 避免逐字打印日志；转换结束时通过 {@link #flush()} 输出汇总。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfTextWarningCollector {

    private static final Logger logger = LoggerFactory.getLogger(PdfTextWarningCollector.class);

    /**
     * 告警聚合表：key = 原因|字体|页码，value = 命中次数
     */
    private final Map<String, Integer> counters = new HashMap<>();

    /**
     * 告警聚合表：key = 原因|字体|页码，value = 首次出现时的页码（用于输出）
     */
    private final Map<String, String> firstSeen = new HashMap<>();

    /**
     * 记录一次降级
     *
     * @param reason 降级原因（固定短语，便于聚合）
     * @param font   所属 PDF 字体的名称，可为 null
     * @param page   所在页码（从 1 起），小于 0 表示未知
     */
    public synchronized void record(String reason, String font, int page) {
        String key = reason + '|' + (font == null || font.isEmpty() ? "unknown" : font) + '|' + page;
        counters.merge(key, 1, Integer::sum);
        firstSeen.putIfAbsent(key, reason + '|' + (font == null || font.isEmpty() ? "unknown" : font) + '|' + page);
    }

    /**
     * 已聚合的告警条目数
     *
     * @return 不同“原因+字体+页”组合的数量
     */
    public synchronized int size() {
        return counters.size();
    }

    /**
     * 输出并清空全部聚合告警
     */
    public synchronized void flush() {
        if (counters.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : counters.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 3);
            logger.warn("PDF 文字对象化降级：原因={}，字体={}，页码={}，命中 {} 次",
                    parts[0], parts[1], parts[2], entry.getValue());
        }
        counters.clear();
        firstSeen.clear();
    }
}
