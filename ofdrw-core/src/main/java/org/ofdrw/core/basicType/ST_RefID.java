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
public class ST_RefID extends STBase {

    private ST_ID ref;

    public ST_RefID(ST_ID ref) {
        this.ref = ref;
    }

    public ST_RefID(long ref) {
        this.ref = new ST_ID(ref);
    }
    /**
     * 获取 ST_RefID 实例如果参数非法则返还null
     *
     * @param idStr 被引用的ID字符串
     * @return 实例 或 null
     */
    public static ST_RefID getInstance(String idStr) {
        if (idStr == null || idStr.trim().length() == 0) {
            return null;
        }
        return new ST_RefID(ST_ID.getInstance(idStr.trim()));
    }

    public static ST_RefID getInstance(Long idStr) {
        return new ST_RefID(new ST_ID(idStr));
    }


    public ST_ID getRefId() {
        return ref;
    }

    public ST_RefID setRefId(ST_ID ref) {
        this.ref = ref;
        return this;
    }

    @Override
    public String toString() {
        return ref.toString();
    }
}
