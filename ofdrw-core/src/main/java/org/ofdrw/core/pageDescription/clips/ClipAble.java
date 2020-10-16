package org.ofdrw.core.pageDescription.clips;

import org.dom4j.Element;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.text.text.CT_Text;

/**
 * 可裁剪对象
 * <p>
 * 实现该接口代表能够作为裁剪区域进行裁剪操作
 * <p>
 * 可裁剪对象为： CT_Path、CT_Text
 * <p>
 * 8.5 图 44 表 33
 */
public interface ClipAble extends Element {

    /**
     * 获取可裁剪对象实例
     *
     * @param e 元素对象
     * @return 可裁剪对象
     */
    static ClipAble getInstance(Element e) {
        if (e == null) {
            return null;
        }
        String qName = e.getQualifiedName();
        ClipAble res = null;
        switch (qName) {
            case "ofd:Path":
                res = new CT_Path(e);
                break;
            case "ofd:Text":
                res = new CT_Text(e);
                break;
            default:
                throw new IllegalArgumentException("不支持裁剪对象类型：" + qName);
        }
        return res;
    }
}
