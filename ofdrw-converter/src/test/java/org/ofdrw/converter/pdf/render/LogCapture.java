package org.ofdrw.converter.pdf.render;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试用日志捕获器：收集指定 logger 的 WARN 及以上事件
 * <p>
 * 用于断言“无图片文档不产生资源读取失败告警”这类“不应出现某类日志”的约束。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class LogCapture extends AbstractAppender {

    /**
     * 捕获到的告警消息
     */
    private final List<String> warnMessages = new ArrayList<>();

    /**
     * 被挂载的 logger
     */
    private final Logger logger;

    /**
     * 构造（请使用 {@link #start(String)}）
     *
     * @param logger 目标 logger
     */
    private LogCapture(Logger logger) {
        super("LegendTestCapture", null, null, false, null);
        this.logger = logger;
    }

    /**
     * 开始捕获指定 logger 的告警
     *
     * @param loggerName logger 名称
     * @return 捕获器
     */
    static LogCapture start(String loggerName) {
        Logger logger = (Logger) LogManager.getLogger(loggerName);
        LogCapture capture = new LogCapture(logger);
        capture.start();
        logger.addAppender(capture);
        return capture;
    }

    @Override
    public void append(LogEvent event) {
        if (event.getLevel().isMoreSpecificThan(Level.WARN)) {
            synchronized (warnMessages) {
                warnMessages.add(event.getMessage().getFormattedMessage());
            }
        }
    }

    /**
     * @return 捕获到的 WARN 及以上消息
     */
    List<String> warnings() {
        synchronized (warnMessages) {
            return new ArrayList<>(warnMessages);
        }
    }

    /**
     * 解除捕获
     */
    void close() {
        logger.removeAppender(this);
        stop();
    }
}
