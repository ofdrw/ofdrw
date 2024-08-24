package org.ofdrw.reader;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 问题用例
 */
public class IssueCase {

    /**
     * 页面名称非 Page_N 格式
     * <p>
     * https://github.com/ofdrw/ofdrw/issues/293
     */
    @Test
    public void github_293() throws Exception {
        Path src = Paths.get("src/test/resources/path_unstd.ofd");
        try (OFDReader reader = new OFDReader(src)) {
            int size = reader.getPageList().size();
            System.out.println(size);
        }
    }
}
