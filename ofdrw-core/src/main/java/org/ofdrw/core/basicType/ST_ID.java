package org.ofdrw.core.basicType;

import java.util.Objects;

/**
 * 标识，无符号整数，应在文档内唯一。0标识无效标识符
 * <p>
 * 示例：
 * <code>1000</code>
 * <p>
 * ————《GB/T 33190-2016》 表 2 基本数据类型
 *
 * @author 权观宇
 * @since 2019-09-28 10:50:03
 */
public class ST_ID extends STBase {

    /**
     * 标识符，默认为无符号标识符
     */
    private Long id = 0L;

    public ST_ID(long id) {
        if (id <= 0) {
            throw new NumberFormatException("ID 必须大于 0");
        }
        this.id = id;
    }

    public ST_ID(String idStr) {
        this.id = Long.parseLong(idStr);
    }

    /**
     * 获取 ST_ID 实例如果参数非法则返还null
     *
     * @param idStr ID字符串
     * @return 实例 或 null
     */
    public static ST_ID getInstance(String idStr) {
        if (idStr == null || idStr.trim().length() == 0) {
            return null;
        }
        return new ST_ID(idStr.trim());
    }


    public Long getId() {
        return id;
    }

    public ST_ID setId(Long id) {
        this.id = id;
        return this;
    }

    /**
     * 获取引用ID
     * @return 引用ID
     */
    public ST_RefID ref(){
        return new ST_RefID(this);
    }


    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ST_ID)) {
            return false;
        }
        ST_ID stId = (ST_ID) o;
        return Objects.equals(getId(), stId.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
