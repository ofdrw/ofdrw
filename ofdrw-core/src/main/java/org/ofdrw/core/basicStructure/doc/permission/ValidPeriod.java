package org.ofdrw.core.basicStructure.doc.permission;

import org.dom4j.Element;
import org.ofdrw.core.Const;
import org.ofdrw.core.basicStructure.OFDElement;

import java.time.LocalDateTime;

/**
 * 有效期
 * <p>
 * 该文档允许访问的期限，其具体期限取决于开始日期和
 * 结束日期，其中开始日期不能晚于结束日期，并且开始日期和结束
 * 日期至少出现一个。当不设置开始日期时，代表不限定开始日期，
 * 当不设置结束日期时代表不限定结束日期；当此不设置此节点时，
 * 表示开始和结束日期均不受限
 * <p>
 * 7.5 图 9 文档权限声明结构
 *
 * @author 权观宇
 * @since 2019-10-07 05:21:06
 */
public class ValidPeriod extends OFDElement {
    public ValidPeriod(Element proxy) {
        super(proxy);
    }

    public ValidPeriod() {
        super("ValidPeriod");
    }

    public ValidPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        this();
        this.setStartDate(startDate)
                .setEndDate(endDate);
    }

    /**
     * 【可选 属性】
     * 设置 有效期开始日期
     *
     * @param startDate 有效期开始日期
     * @return this
     */
    public ValidPeriod setStartDate(LocalDateTime startDate) {
        this.addAttribute("StartDate", startDate.format(Const.DATETIME_FORMATTER));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 有效期开始日期
     *
     * @return 有效期开始日期
     */
    public LocalDateTime getStartDate() {
        String str = this.attributeValue("StartDate");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        return LocalDateTime.parse(str, Const.DATETIME_FORMATTER);
    }


    /**
     * 【可选 属性】
     * 设置 有效期结束日期
     *
     * @param endDate 有效期结束日期
     * @return this
     */
    public ValidPeriod setEndDate(LocalDateTime endDate) {
        this.addAttribute("EndDate", endDate.format(Const.DATETIME_FORMATTER));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 有效期结束日期
     *
     * @return 有效期结束日期
     */
    public LocalDateTime getEndDate() {
        String str = this.attributeValue("EndDate");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        return LocalDateTime.parse(str, Const.DATETIME_FORMATTER);
    }
}
