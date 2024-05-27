package org.ofdrw.layout.handler;

import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * OFDRW元素渲染结束时触发的回调函数
 * <p>
 * 用与获取OFDRW元素在转换为OFD元素生成的对象ID。
 *
 * @author 权观宇
 * @since 2024-5-27 20:10:18
 */
@FunctionalInterface
public interface ElementRenderFinishHandler {

    /**
     * OFDRW元素渲染结束时触发的回调函数
     *
     * @param loc          OFD元素所处页面在OFD容器内的绝对路径，不能为null。
     * @param contentObjId 内容对象绘制后产生的OFD元素ID序列，不能为null。
     * @param divObjIds    Div对象绘制后产生的OFD元素ID序列，不能为null。
     */
    void handle(ST_Loc loc, ST_ID[] contentObjId, ST_ID[] divObjIds);
}
