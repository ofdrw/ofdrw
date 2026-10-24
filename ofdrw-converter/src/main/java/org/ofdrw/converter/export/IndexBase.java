package org.ofdrw.converter.export;

/**
 * 导出文件名占位符的取值基准。
 * <p>
 * 与 {@link CanvasImageExporter#setFilenamePattern(String)} 配合使用，
 * 决定 {@code String.format} 占位符代入的是页码还是本次导出的序号。
 *
 * @author 权观宇
 * @since 2024-10-24
 */
public enum IndexBase {

    /**
     * 页码（从 0 起）
     */
    PAGE,

    /**
     * 本次导出的序号（从 0 起）
     */
    SEQUENCE
}
