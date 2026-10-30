package org.ofdrw.converter.pdf.write;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.reader.OFDReader;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构源抽取测试：从真实 OFD 样本中取出书签/附件/注释/文档信息
 * <p>
 * 样本取自 {@code src/test/resources/pdf-match/}（入库样本，来源与覆盖特性见同目录 README）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class StructureSourceTest {

    /**
     * 入库样本目录
     */
    private static final Path SAMPLES = Paths.get("src/test/resources/pdf-match");

    @Test
    @DisplayName("书签：testImageNotFound 带的 Outline 被抽取，页对象 ID 映射到导出页码")
    void readsOutlines() throws Exception {
        try (OFDReader reader = new OFDReader(SAMPLES.resolve("testImageNotFound.ofd"))) {
            StructureSource source = StructureSource.load(reader, null);
            assertEquals(reader.getNumberOfPages(), source.getPages().size());
            assertTrue(source.getOutlines().size() >= 2, "样本应带书签，实际：" + source.getOutlines().size());
            assertEquals(Integer.valueOf(0), source.getPageIndexById().get(reader.getPageObjectId(1).toString()));
        }
    }

    @Test
    @DisplayName("附件：AddAttachment 的附件被抽取，文件名取 Attachment/@Name")
    void readsAttachments() throws Exception {
        try (OFDReader reader = new OFDReader(SAMPLES.resolve("AddAttachment.ofd"))) {
            StructureSource source = StructureSource.load(reader, null);
            // 样本自带 1 个附件（Name="Gao"，FileLoc=Res/eg_tulip.jpg），MIME 按扩展名推断
            assertEquals(1, source.getAttachments().size());
            assertEquals("Gao", source.getAttachments().get(0).getAttachmentName());
        }
    }

    @Test
    @DisplayName("注释：AddWatermarkAnnot 的 Watermark 注释按页抽取")
    void readsAnnotations() throws Exception {
        try (OFDReader reader = new OFDReader(SAMPLES.resolve("AddWatermarkAnnot.ofd"))) {
            StructureSource source = StructureSource.load(reader, null);
            assertTrue(source.getAnnotationsByPageId().size() >= 1,
                    "应至少有一页带注释：" + source.getAnnotationsByPageId());
        }
    }

    @Test
    @DisplayName("元数据：helloworld 文档信息被抽取（标题/创建工具）")
    void readsDocInfo() throws Exception {
        try (OFDReader reader = new OFDReader(SAMPLES.resolve("helloworld.ofd"))) {
            StructureSource source = StructureSource.load(reader, null);
            assertNotNull(source.getDocInfo());
            assertNotNull(source.getDocInfo().getCreator());
        }
    }

    @Test
    @DisplayName("页面子集导出：只保留被导出的页，页码按导出顺序重排")
    void exportedSubsetOnly() throws Exception {
        try (OFDReader reader = new OFDReader(SAMPLES.resolve("AddWatermarkAnnot.ofd"))) {
            String secondPageId = reader.getPageObjectId(2).toString();
            StructureSource source = StructureSource.load(reader, java.util.Collections.singletonList(secondPageId));
            assertEquals(1, source.getPages().size());
            assertEquals(Integer.valueOf(0), source.getPageIndexById().get(secondPageId));
        }
    }

    @Test
    @DisplayName("空读取器：返回空快照，不抛异常")
    void nullReader() {
        StructureSource source = StructureSource.load(null, null);
        assertTrue(source.getPages().isEmpty());
        assertTrue(source.getOutlines().isEmpty());
        assertEquals(210, source.pageBox(0).getWidth(), 1e-6);
    }
}
