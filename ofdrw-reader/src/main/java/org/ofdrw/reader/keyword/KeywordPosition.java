package org.ofdrw.reader.keyword;

import org.ofdrw.core.basicType.ST_Box;

/**
 * 关键字位置
 *
 * @author minghu-zhang
 * @since 16:25 2020/9/26
 */
public class KeywordPosition {
    /**
     * 关键字所在页码
     */
    private int page;
    /**
     * 矩形区域
     */
    private ST_Box box;
    /**
     * 所属关键字
     */
    private String keyword;

    public KeywordPosition(int page, ST_Box box) {
        this.page = page;
        this.box = box;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public ST_Box getBox() {
        return box;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public void setBox(ST_Box box) {
        this.box = box;
    }

    @Override
    public String toString() {
        return "KeywordPosition{" +
                "page=" + page +
                ", box=" + box +
                ", keyword='" + keyword + '\'' +
                '}';
    }
}
