package org.ofdrw.reader;

import org.ofdrw.core.basicType.ST_Array;

import java.util.ArrayList;
import java.util.List;

/**
 * DeltaX和DeltaY工具
 *
 * @author minghu.zhang
 * @since 14:00 2020/11/14
 **/
public class DeltaTool {

    /**
     * 获取Delta数据
     *
     * @param delta         OFD数组对象
     * @param contentLength 文本长度
     * @return 一组坐标偏移值
     */
    public static List<Float> getDelta(ST_Array delta, int contentLength) {
        List<Float> list = new ArrayList<>();
        if (delta != null) {
            List<String> array = delta.getArray();
            for (int i = 0, len = array.size(); i < len; i++) {
                if ("g".equals(array.get(i))) {
                    for (int j = 0, len2 = Integer.parseInt(array.get(i + 1)); j < len2; j++) {
                        list.add(Float.valueOf(array.get(i + 2)));
                    }
                    i += 2;
                } else {
                    list.add(Float.valueOf(array.get(i)));
                }
            }
        }
        int deltaSize = list.size();
        if (deltaSize < contentLength && deltaSize > 0) {
            Float lastDelta = list.get(deltaSize - 1);
            for (int i = 0; i < contentLength - deltaSize; i++) {
                list.add(lastDelta);
            }
        }
        return list;
    }

}
