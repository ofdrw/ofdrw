package org.ofdrw.converter;

import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.text.CT_CGTransform;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字形变换映射
 * <p>
 * 该映射用于处理一个TextObject的多个TextCode的映射关系。
 * <p>
 * 映射为： TextCode中字符处于整个TextObject的偏移量 字形
 *
 * @author 权观宇
 * @since 2021-10-12 19:42:39
 */
public class CGTransformMap {

    List<CT_CGTransform> data;

    private CGTransformMap() {
    }

    /**
     * 创建字形变换映射
     *
     * @param textObject 文字对象
     */
    public CGTransformMap(TextObject textObject) {
        data = textObject.getCGTransforms()
                .stream()
                .filter(item -> {
                    // 兼容非法情况
                    if (item.getCodePosition() == null) {
                        item.setCodePosition(0);
                    }
                    return true;
                })
                // 使用CodePosition递增排序
                .sorted(Comparator.comparingInt(CT_CGTransform::getCodePosition))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * 通过全局字符偏移量获取字形变换，如果字形变换不存在则返还空
     *
     * @param globalOffset 全局字符偏移量
     * @return 字形变换，或null
     */
    @Nullable
    public CT_CGTransform get(int globalOffset){
        if (globalOffset < 0){
            return null;
        }
        CT_CGTransform res = null;
        for (CT_CGTransform datum : data) {
            if (datum.getCodePosition() == globalOffset) {
                res = datum;
                // 不使用break，在出现多个字形变换Position相同时，取最后一个出现的字形变换。
            }
        }
        return res;
    }
}
