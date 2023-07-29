package org.ofdrw.core.customTags;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;

public class TagsTest {
	public static Tags customTagCase() {
		CustomTags customTags = new CustomTags();
		customTags.addCustomTag(new CustomTag().setTypeID("eInvoice").setFileLoc(new ST_Loc("CustomTag.xml")));
		
		Tags tags = new Tags("eInvoice");
		tags.add(new Tags("IssueDate").addObjectRef(new ST_ID(1L)));
		return tags;
	}
	
	@Test
    public void gen() throws Exception {
        TestTool.genXml("CustomTag", customTagCase());
    }
}
