package org.ofdrw.core.customTags;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.OFDSimpleTypeElement;
import org.ofdrw.core.basicType.ST_ID;

/**
 * 标引内容
 * <p>
 * 16 图 82 表 63
 *
 * @author iunet@outlook.com
 * @since 2023-07-29 09:15:00
 */
public class Tags extends OFDElement {
	public Tags(Element proxy) {
		super(proxy);
	}

	/**
	 * 添加自定义标引
	 * @param name
	 */
	public Tags(String name) {
		super(name);
	}

	/**
	 * 添加标引指向的内容
	 * @param 内容id
	 * @return
	 */
	public Tags addObjectRef(ST_ID id) {
		OFDSimpleTypeElement ObjectRef = new OFDSimpleTypeElement("ObjectRef", id.getId());
		ObjectRef.addAttribute("PageRef", "1");
		this.add(ObjectRef);
		return this;
	}
}