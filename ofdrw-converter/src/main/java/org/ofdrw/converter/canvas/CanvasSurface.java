package org.ofdrw.converter.canvas;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 离屏像素画布抽象。
 * <p>
 * 提供显式尺寸、显式背景、绘制后可编码的画布语义，不绑定窗口系统，
 * 可在 headless 环境使用。
 * <p>
 * 画布生命周期由调用方管理：绘制完成后可交由编码器输出字节，使用结束后应调用 {@link #flush()} 释放像素资源。
 */
public abstract class CanvasSurface {

    /**
     * 获取画布宽度。
     *
     * @return 画布宽度，单位：像素
     */
    public abstract int getWidth();

    /**
     * 获取画布高度。
     *
     * @return 画布高度，单位：像素
     */
    public abstract int getHeight();

    /**
     * 获取一个新建的绘制上下文。
     * <p>
     * 每次调用返回独立的 Graphics2D 副本，调用方负责 dispose。
     *
     * @return 绘制上下文
     */
    public abstract Graphics2D createGraphics();

    /**
     * 创建画布。
     *
     * @param width      宽度，单位：像素，必须大于 0
     * @param height     高度，单位：像素，必须大于 0
     * @param background 背景色，为 null 表示保持透明
     * @return 画布实例
     * @throws IllegalArgumentException 宽度或高度不大于 0
     */
    public static CanvasSurface create(int width, int height, Color background) {
        return new BufferedImageSurface(width, height, background);
    }

    /**
     * 释放画布持有的像素资源。
     */
    public abstract void flush();
}
