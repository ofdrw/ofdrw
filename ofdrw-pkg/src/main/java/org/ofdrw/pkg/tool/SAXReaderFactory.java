package org.ofdrw.pkg.tool;

import java.util.function.Supplier;

import org.dom4j.io.SAXReader;

/**
 * SAXReader 工厂
 *
 * @author gongxiangyang
 * @since 2022/5/7 11:29
 */
public class SAXReaderFactory {

    /**
     * 用户自定义生成器
     */
    private static Supplier<SAXReader> CustomizeProducer = null;


    /**
     * 设置用户自定义的 SAXReader 生成器
     *
     * @param SAXReaderProducer 自定义工厂对象
     */
    public static synchronized void SetCustomizedProducer(Supplier<SAXReader> SAXReaderProducer) {
        if (null == CustomizeProducer) {
            SAXReaderFactory.CustomizeProducer = SAXReaderProducer;
        }
    }


    /**
     * 创建 SAXReader 实例
     *
     * @return SAXReader 对象
     * 若用户未配置，则调用 SAXReader.createDefault()创建，否则调用用户定义的生成器逻辑
     */
    public static SAXReader create() {
        if (null == CustomizeProducer) {
            return SAXReader.createDefault();
        } else {
            return CustomizeProducer.get();
        }
    }
}