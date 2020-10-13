package org.ofdrw.core;

import org.dom4j.Namespace;

import java.time.format.DateTimeFormatter;

/**
 * 静态变量
 *
 * @author 权观宇
 * @since 2019-09-27 10:22:57
 */
public class Const {

    /**
     * 命名空间 URI,《GB/T_33190-2016》 7.1 命名空间
     */
    public static final String OFD_NAMESPACE_URI = "http://www.ofdspec.org/2016";
    /**
     * 元素节点应使用命名空间标识符
     * ————《GB/T 33190-2016》 7.1 命名空间
     */
    public static final String OFD_VALUE = "ofd";
    /**
     * OFD命名空间
     */
    public static final String OFD_Q = "ofd:";

    /**
     * 使用命名空间为 http://www.ofdspec.org/2016，其表示符应为 ofd。
     * ————《GB/T 33190-2016》 7.1 命名空间
     */
    public static final Namespace OFD_NAMESPACE = new Namespace("ofd", OFD_NAMESPACE_URI);
    public static final Namespace OFD_NAMESPACE_DEFAULT = new Namespace("", OFD_NAMESPACE_URI);

    /**
     * xs:date 类型日期格式化
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * xs:dateTime 类型时间日期格式化
     */
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss");

    /**
     * OFD索引文件
     */
    public static final String INDEX_FILE = "OFD.xml";


}
