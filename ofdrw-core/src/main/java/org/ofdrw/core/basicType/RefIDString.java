package org.ofdrw.core.basicType;


/**
 * 标识符引用，无符号整数，此标识符应为文档内已定义的标识符
 * <p>
 * <p>
 * 示例：
 * <code>1000</code>
 * <p>
 * ————《GB/T 33190-2016》 表 2 基本数据类型
 *
 * @author 权观宇
 * @since 2019-09-28 10:52:01
 */
public class RefIDString {

    private IDString ref;

    public RefIDString(IDString ref) {
        this.ref = ref;
    }

    public IDString getRefId() {
        return ref;
    }

    public RefIDString setRefId(IDString ref) {
        this.ref = ref;
        return this;
    }

    @Override
    public String toString() {
        return ref.toString();
    }
}
