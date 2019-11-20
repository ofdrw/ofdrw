package org.ofdrw.core.extensions;

import org.dom4j.Element;
import org.ofdrw.core.Const;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;

import java.time.LocalDate;
import java.util.List;

/**
 * 扩展信息节点
 * <p>
 * 17 扩展信息 图 83 表 6
 *
 * @author 权观宇
 * @since 2019-11-20 06:09:49
 */
public class CT_Extension extends OFDElement {
    public CT_Extension(Element proxy) {
        super(proxy);
    }

    public CT_Extension() {
        super("Extension");
    }

    /**
     * 【必选 属性】
     * 设置 用于生成或解释该自定义对象数据的扩展应用程序名称
     *
     * @param appName 用于生成或解释该自定义对象数据的扩展应用程序名称
     * @return this
     */
    public CT_Extension setAppName(String appName) {
        if (appName == null || appName.trim().length() == 0) {
            throw new IllegalArgumentException("用于生成或解释该自定义对象数据的扩展应用程序名称（AppName）不能为空");
        }
        this.addAttribute("AppName", appName);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 用于生成或解释该自定义对象数据的扩展应用程序名称
     *
     * @return 用于生成或解释该自定义对象数据的扩展应用程序名称
     */
    public String getAppName() {
        String str = this.attributeValue("AppName");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("用于生成或解释该自定义对象数据的扩展应用程序名称（AppName）不能为空");
        }
        return str;
    }

    /**
     * 【可选 属性】
     * 设置 形成此扩展信息的软件厂商标识
     *
     * @param company 形成此扩展信息的软件厂商标识
     * @return this
     */
    public CT_Extension setCompany(String company) {
        if (company == null || company.trim().length() == 0) {
            this.removeAttr("Company");
            return this;
        }
        this.addAttribute("Company", company);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 形成此扩展信息的软件厂商标识
     *
     * @return 形成此扩展信息的软件厂商标识
     */
    public String getCompany() {
        return this.attributeValue("Company");
    }

    /**
     * 【可选 属性】
     * 设置 形成此扩展信息的软件版本
     *
     * @param appVersion 形成此扩展信息的软件版本
     * @return this
     */
    public CT_Extension setAppVersion(String appVersion) {
        if (appVersion == null || appVersion.trim().length() == 0) {
            this.removeAttr("AppVersion");
            return this;
        }
        this.addAttribute("AppVersion", appVersion);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 形成此扩展信息的软件版本
     *
     * @return 形成此扩展信息的软件版本
     */
    public String getAppVersion() {
        return this.attributeValue("AppVersion");
    }

    /**
     * 【可选 属性】
     * 设置 形成此扩展信息的日期时间
     *
     * @param date 形成此扩展信息的日期时间
     * @return this
     */
    public CT_Extension setDate(LocalDate date) {
        if (date == null) {
            this.removeAttr("Date");
            return this;
        }
        this.addAttribute("Date", date.format(Const.DATE_FORMATTER));
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 形成此扩展信息的日期时间
     *
     * @return 形成此扩展信息的日期时间
     */
    public LocalDate getDate() {
        String str = this.attributeValue("Date");
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        return LocalDate.parse(str, Const.DATE_FORMATTER);
    }

    /**
     * 【可选 属性】
     * 设置 引用扩展项针对的文档项目的标识
     *
     * @param refId 引用扩展项针对的文档项目的标识
     * @return this
     */
    public CT_Extension setRefId(ST_RefID refId) {
        if (refId == null) {
            this.removeAttr("RefId");
            return this;
        }
        this.addAttribute("RefId", refId.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 引用扩展项针对的文档项目的标识
     *
     * @return 引用扩展项针对的文档项目的标识
     */
    public ST_RefID getRefId() {
        return ST_RefID.getInstance(this.attributeValue("RefId"));
    }

    /**
     * 【必选 属性】
     * 增加 属性
     *
     * @param property 属性
     * @return this
     */
    public CT_Extension addProperty(Property property) {
        if (property == null) {
            return this;
        }
        this.add(property);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 属性列表
     *
     * @return 属性列表
     */
    public List<Property> getPropertys() {
        return this.getOFDElements("Property", Property::new);
    }

    /**
     * 【必选】
     * 增加 扩展数据文件所在位置
     * <p>
     * 用于扩展大量信息
     *
     * @param extendData 扩展数据文件所在位置
     * @return this
     */
    public CT_Extension addExtendData(ST_Loc extendData) {
        if (extendData == null) {
            return this;
        }
        this.addOFDEntity("ExtendData", extendData);
        return this;
    }

    /**
     * 【必选】
     * 获取 扩展数据文件所在位置序列
     * <p>
     * 用于扩展大量信息
     *
     * @return 扩展数据文件所在位置序列
     */
    public List<ST_Loc> getExtendDatas() {
        return this.getOFDElements("ExtendData", ST_Loc::getInstance);
    }

    /**
     * 【必选】
     * 增加 扩展复杂属性
     * <p>
     * 使用xs:anyType，用于较复杂的扩展
     *
     * @param data 扩展复杂属性
     * @return this
     */
    public CT_Extension addData(Element data) {
        if (data == null) {
            return this;
        }
        this.add(data);
        return this;
    }

    /**
     * 【必选】
     * 获取 扩展复杂属性序列
     * <p>
     * 使用xs:anyType，用于较复杂的扩展
     *
     * @return 扩展复杂属性序列
     */
    public List<Element> getDatas() {
        return this.elements("Data");
    }
}
