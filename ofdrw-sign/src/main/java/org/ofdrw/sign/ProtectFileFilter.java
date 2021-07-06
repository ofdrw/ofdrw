package org.ofdrw.sign;

import org.jetbrains.annotations.NotNull;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * 在执行签章时会遍历OFD内的所有文件
 * <p>
 * 通过过滤器可以对需要或不需要的文件进行过滤
 *
 * @author 权观宇
 * @since 2021-06-22 19:41:27
 */
@FunctionalInterface
public interface ProtectFileFilter {

    /**
     * 执行过滤
     *
     * @param absPath 待保护文件在OFD容器内的绝对路径
     * @return true - 需要保护; false - 不需要保护
     */
    boolean filter(@NotNull ST_Loc absPath);
}
