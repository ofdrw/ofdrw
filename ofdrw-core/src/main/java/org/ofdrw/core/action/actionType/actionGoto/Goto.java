package org.ofdrw.core.action.actionType.actionGoto;

import org.dom4j.Element;
import org.ofdrw.core.action.actionType.OFDAction;
import org.ofdrw.core.OFDElement;

import java.util.List;

/**
 * 跳转动作表明同一个文档内的跳转，包括一个目的区域
 * 或书签位置
 * <p>
 * 图 74 跳转动作结构
 *
 * @author 权观宇
 * @since 2019-10-05 08:33:55
 */
public class Goto extends OFDElement implements OFDAction  {
    public Goto(Element proxy) {
        super(proxy);
    }

    public Goto() {
        super("Goto");
    }

    public Goto(CT_Dest dest) {
        this();
        this.setDest(dest);
    }

    public Goto(String bookmarkName) {
        this();
        this.setBookmark(bookmarkName);
    }

    /**
     * 【必选】
     * 设置 跳转的目的区域
     *
     * @param dest 跳转的目的区域
     * @return this
     */
    public Goto setDest(CT_Dest dest) {
        this.removeOFDElemByNames("Bookmark", "Dest");
        this.add(dest);
        return this;
    }


    /**
     * 【必选】
     * 设置 跳转的目标书签
     *
     * @param name 跳转的目标书签
     * @return this
     */
    public Goto setBookmark(String name) {
        this.removeOFDElemByNames("Bookmark", "Dest");
        this.add(new Bookmark(name));
        return this;
    }

    /**
     * 【必选】
     * 获取 跳转动作的目标
     * <p>
     * 可能是 CT_Dest 或 Bookmark，可以使用<code>instanceof</code>判断类型并转换
     *
     * @return 跳转动作的目标
     */
    public OFDGotoTarget getTarget() {
        List<Element> elements = this.elements();
        if (elements.size() != 1) {
            throw new IllegalArgumentException("Goto 中含有多个目标位置无法确定");
        }
        Element e = elements.get(0);
        return OFDGotoTarget.getInstance(e);
    }


}
