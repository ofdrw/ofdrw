package org.ofdrw.reader;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.reader.model.TemplatePageEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模板页路径大小写不匹配的容错解析测试
 * <p>
 * 测试资源 template_case_mismatch.ofd 中：Document.xml 的
 * {@code <ofd:TemplatePage BaseLoc="TPLS/Tpl_0/Content.xml"/>} 使用大写 TPLS，
 * 而压缩包内实际条目为 {@code Doc_0/Tpls/Tpl_0/Content.xml}（小写 Tpls）。
 * 在大小写敏感的文件系统上按原路径精确查找必然失败，需要按“精确匹配优先、
 * 大小写不敏感兜底”的策略定位模板。
 */
class TemplateCaseMismatchTest {

    /**
     * 模板页路径大小写与实际条目不一致的测试文档
     */
    private static final Path SRC = Paths.get("src/test/resources/template_case_mismatch.ofd");

    /**
     * 普通文档（模板页路径大小写正确），用于确认正常文档行为不变
     */
    private static final Path NORMAL = Paths.get("src/test/resources/path_unstd.ofd");

    @Test
    void shouldOpenAndLoadTemplateWhenCaseMismatch() throws IOException {
        try (OFDReader reader = new OFDReader(SRC)) {
            assertEquals(1, reader.getNumberOfPages(), "文档应包含 1 页");

            final PageInfo pageInfo = reader.getPageList().get(0);
            assertNotNull(pageInfo.getObj(), "页面内容应可解析");
            // 模板 + 页面本身，共 2 个图层来源
            assertEquals(2, pageInfo.getOrderRelatedPageList().size(), "页面应包含模板与页面本身两部分");

            final TemplatePageEntity template = reader.getTemplate("2");
            assertNotNull(template, "大小写不一致时仍应能加载模板页");
            assertNotNull(template.getPage(), "模板页内容应可解析");
        }
    }

    @Test
    void shouldOpenAndLoadTemplateFromStreamWhenCaseMismatch() throws IOException {
        try (OFDReader reader = new OFDReader(new ByteArrayInputStream(Files.readAllBytes(SRC)))) {
            assertEquals(1, reader.getNumberOfPages(), "输入流方式打开页数应为 1");
            final TemplatePageEntity template = reader.getTemplate("2");
            assertNotNull(template, "输入流方式打开时仍应能加载模板页");
        }
    }

    @Test
    void shouldKeepTemplateLoadingForNormalDocument() throws IOException {
        // 回归：模板路径大小写正确的文档解析行为不变
        try (OFDReader reader = new OFDReader(NORMAL)) {
            assertEquals(2, reader.getNumberOfPages(), "普通文档页数应保持不变");
            final List<PageInfo> pages = reader.getPageList();
            assertEquals(2, pages.size());
            for (PageInfo pageInfo : pages) {
                assertNotNull(pageInfo.getObj(), "普通文档页面内容应可解析");
                assertFalse(pageInfo.getOrderRelatedPageList().isEmpty(), "普通文档页面图层不应为空");
            }
            final Page page = reader.getPage(1);
            assertNotNull(page, "普通文档第 1 页应可解析");
        }
    }

    @Test
    void shouldNotResolveContentWithDifferentCaseWhenFileMissing() throws Exception {
        // 兜底仅用于定位已存在的资源：完全不存在的内容仍保持原有异常行为
        try (OFDReader reader = new OFDReader(SRC)) {
            final ResourceLocator rl = reader.getResourceLocator();
            rl.save();
            try {
                rl.cd("/Doc_0");
                assertThrows(org.ofdrw.reader.ErrorPathException.class,
                        () -> rl.get("NotExist/Content.xml", Page::new),
                        "不存在的资源仍应抛出路径不存在异常");
            } finally {
                rl.restore();
            }
        }
    }
}
