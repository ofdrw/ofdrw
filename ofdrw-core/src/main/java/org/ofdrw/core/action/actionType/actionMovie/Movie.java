package org.ofdrw.core.action.actionType.actionMovie;

import org.dom4j.Element;
import org.ofdrw.core.action.actionType.OFDAction;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_RefID;

/**
 * Movie 动作用于播放视频。
 * <p>
 * 图 79 播放视频动作属性
 *
 * @author 权观宇
 * @since 2019-10-06 05:51:40
 */
public class Movie extends OFDElement implements OFDAction {
    public Movie(Element proxy) {
        super(proxy);
    }

    public Movie() {
        super("Movie");
    }

    public Movie(String resourceId) {
        this();
        setResourceID(ST_RefID.getInstance(resourceId));
    }

    public Movie(ST_RefID resourceId) {
        this();
        setResourceID(resourceId);
    }

    /**
     * 【必选 属性】
     * 设置 引用资源文件中定义的视频资源标识
     *
     * @param resourceId 引用资源文件中定义的视频资源标识
     * @return this
     */
    public Movie setResourceID(ST_RefID resourceId) {
        this.addAttribute("ResourceID", resourceId.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 引用资源文件中定义的视频资源标识
     *
     * @return 引用资源文件中定义的视频资源标识
     */
    public ST_RefID getResourceID() {
        return ST_RefID.getInstance(this.attributeValue("ResourceID"));
    }

    /**
     * 【可选 属性】
     * 设置 放映参数
     * <p>
     * 默认值为 Play
     *
     * @param operator 放映参数，参见{@link PlayType}
     * @return this
     */
    public Movie setOperator(PlayType operator) {
        if (operator == null) {
            this.removeAttr("Operator");
            return this;
        }

        this.addAttribute("Operator", operator.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 放映参数
     * <p>
     * 默认值为 Play
     *
     * @return 放映参数，参见{@link PlayType}
     */
    public PlayType getOperator() {
        return PlayType.getInstance(this.attributeValue("Operator"));
    }
}
