package org.ofdrw.core.basicType;

/**
 * 标识，无符号整数，应在文档内唯一。0标识无效标识符
 *
 * 示例：
 * <code>1000</code>
 *
 * ————《GB/T 33190-2016》 表 2 基本数据类型
 *
 * @author 权观宇
 * @since 2019-09-28 10:50:03
 */
public class IDString {

    /**
     * 标识符，默认为无符号标识符
     */
    private Long id = 0L;

    public IDString(long id) {
        this.id = id;
    }

    public IDString(String idStr) {
        this.id = Long.valueOf(idStr);
    }

    public Long getId() {
        return id;
    }

    public IDString setId(Long id) {
        this.id = id;
        return this;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
