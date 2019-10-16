package org.ofdrw.core.graph.tight.method;

import org.dom4j.Element;

/**
 * 自动闭合到当前路径的起始点，并以该点为当前点
 * <p>
 * 表 37 图形对象描述方法
 *
 * @author 权观宇
 * @since 2019-10-05 06:21:39
 */
public class Close extends Command {
    public Close(Element proxy) {
        super(proxy);
    }

    public Close() {
        super("Close");
    }
}
