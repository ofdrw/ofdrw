package org.ofdrw.archive.model;

import org.ofdrw.core.basicType.ST_Loc;

import java.util.Objects;

/**
 * OFD-A 不合规项描述
 * <p>
 * 检查器对单条规则检查后产生的结果，描述文档中一处不符合 GB/T 42133-2022 的位置和原因。
 *
 * @author xxx
 * @since 2.3.9
 */
public class ArchiveViolation {

    /**
     * 违规严重程度
     */
    public enum Severity {
        /** 错误：不符合 OFD-A 标准的核心约束，必须修复 */
        ERROR,
        /** 警告：不符合标准建议，但可接受 */
        WARN,
        /** 信息：提示性信息 */
        INFO
    }

    /** 规则标识，如 "DOC_TYPE"、"IMAGE_FORMAT" */
    private final String ruleName;

    /** 严重程度 */
    private final Severity severity;

    /** 人类可读的问题描述 */
    private final String description;

    /** 文档中的位置（OFD 容器内绝对路径），可为 null */
    private final ST_Loc location;

    /** 实际检测到的值，可为 null */
    private final String actualValue;

    /** 标准期望的值，可为 null */
    private final String expectedValue;

    /**
     * 创建不合规项描述
     *
     * @param ruleName      规则标识（如 "DOC_TYPE"）
     * @param severity      严重程度
     * @param description   问题描述
     * @param location      文档内位置（OFD 容器内路径），可为 null
     * @param actualValue   实际值，可为 null
     * @param expectedValue 期望值，可为 null
     */
    public ArchiveViolation(String ruleName, Severity severity, String description,
                            ST_Loc location, String actualValue, String expectedValue) {
        this.ruleName = Objects.requireNonNull(ruleName, "规则标识不能为 null");
        this.severity = Objects.requireNonNull(severity, "严重程度不能为 null");
        this.description = Objects.requireNonNull(description, "描述不能为 null");
        this.location = location;
        this.actualValue = actualValue;
        this.expectedValue = expectedValue;
    }

    public String getRuleName() {
        return ruleName;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public ST_Loc getLocation() {
        return location;
    }

    public String getActualValue() {
        return actualValue;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity).append("] ");
        sb.append(ruleName).append(": ").append(description);
        if (location != null) {
            sb.append(" (位置: ").append(location).append(")");
        }
        if (actualValue != null) {
            sb.append(" 实际: ").append(actualValue);
        }
        if (expectedValue != null) {
            sb.append(" 期望: ").append(expectedValue);
        }
        return sb.toString();
    }
}
