package org.ofdrw.reader;

import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;

/**
 * 页面信息
 *
 * @author 权观宇
 * @since 2020-05-13 19:10:02
 */
public class PageInfo {

    /**
     * 页面的物理大小
     */
    private ST_Box size;
    /**
     * 页面底层对象
     */
    private Page obj;
    /**
     * 页面在OFD中的对象ID
     */
    private ST_ID id;
    /**
     * 页码，从1起
     */
    private Integer index;

    public PageInfo() {
    }

    public ST_Box getSize() {
        return size;
    }

    public PageInfo setSize(ST_Box size) {
        this.size = size;
        return this;
    }

    public Page getObj() {
        return obj;
    }

    public PageInfo setObj(Page obj) {
        this.obj = obj;
        return this;
    }

    public ST_ID getId() {
        return id;
    }

    public PageInfo setId(ST_ID id) {
        this.id = id;
        return this;
    }

    public Integer getIndex() {
        return index;
    }

    public PageInfo setIndex(Integer index) {
        this.index = index;
        return this;
    }
}
