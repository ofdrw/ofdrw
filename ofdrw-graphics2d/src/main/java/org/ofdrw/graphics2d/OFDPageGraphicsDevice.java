package org.ofdrw.graphics2d;

import java.awt.*;

/**
 * OFD页面图形虚拟设备
 *
 * @author 权观宇
 * @since 2023-2-23 20:09:40
 */
public final class OFDPageGraphicsDevice extends GraphicsDevice {

    /**
     * 设备配置信息
     */
    private final GraphicsConfiguration defaultConfig;

    /**
     * 创建OFD页面图形虚拟设备
     *
     * @param defaultConfig 默认配置
     */
    public OFDPageGraphicsDevice(GraphicsConfiguration defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    /**
     * 获取设备类型
     *
     * @return 设备类型（虚拟打印机）
     */
    @Override
    public int getType() {
        return GraphicsDevice.TYPE_PRINTER;
    }

    /**
     * 获取设备ID
     *
     * @return 设备ID
     */
    @Override
    public String getIDstring() {
        return "OFD Reader & Writer Virtual OFD Page Printer";
    }

    /**
     * 获取所有关于设备的配置信息
     *
     * @return 获取所有关于设备的配置信息
     */
    @Override
    public GraphicsConfiguration[] getConfigurations() {
        return new GraphicsConfiguration[]{getDefaultConfiguration()};
    }

    /**
     * 获取默认配置信息
     *
     * @return 默认配置信息
     */
    @Override
    public GraphicsConfiguration getDefaultConfiguration() {
        return this.defaultConfig;
    }
}
