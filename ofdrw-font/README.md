# OFD 字体库

提供文档生成过程中基础的字体库，默认使用操作系统内置的几种字体（如：宋体、黑体）。


如果需要使用到一些外部字体，请手动构造`org.ofdrw.font.Font`设置外部字体文件路径，OFD支持的嵌入式字体类型为OpenType，

OpenType字体可以使用 **[Google Noto](https://github.com/googlefonts/noto-fonts) 开源字体库。**

## 字体嵌入能力（自 2.5.1 起）

`org.ofdrw.converter.font` 包在既有解析能力（`TrueTypeFont`、`TrueTypeCollection`）之上，
新增了面向“**整字体嵌入**（不做子集裁剪）”的输出侧能力。转换器只负责“选哪种字体、如何映射到 PDF 对象”；
字体格式识别、度量、嵌入数据与 ToUnicode 映射均在本模块完成。

| 类型 | 职责 |
|---|---|
| `FontDescriptor` | 嵌入元信息：`FontFile2` / `FontFile3` 键名、PostScript 名（含 `ABCDEF+` 前缀重命名）、字族名、字重、`ItalicAngle`、`FontBBox`、`Flags` |
| `FontMetrics` | 统一度量：`unitsPerEm`、`getAdvanceWidth(gid)`、`getAscent()` / `getDescent()` / `getLineGap()`、`getBBox()`，单位均为 1/1000 文本空间单位 |
| `EmbeddedFontData` | 原样导出可嵌入的字体数据；字体集合（TTC）提取指定字体并重建 sfnt（重算 `checkSumAdjustment`） |
| `ToUnicodeCMap` | 由“字形 → Unicode”映射生成标准 ToUnicode CMap 文本，保证 PDF 文字可提取、可搜索 |
| `GlyphIndexProvider` | 统一的“字符 → 字形”访问：显式 GID（CGTransform）优先，其次 cmap 查表，最后回退字形 0 并标记替换 |

字体格式按**文件内容**识别（不依赖扩展名）：

| 输入 | `FontDescriptor.GlyphFormat` | `EmbeddedFontData#getData()` 内容 | PDF 字体字典键 |
|---|---|---|---|
| TTF / OTF（TrueType 轮廓） | `TRUE_TYPE` | 原始 sfnt 数据 | `FontFile2` |
| OTF（CFF 轮廓） | `CFF` | 原始数据 | `FontFile3` |
| 字体集合 TTC | `TRUE_TYPE` 或 `CFF` | 提取指定字体后重建的 sfnt | 按格式 |
| Type1（PFB） | `TYPE_1` | 原始 PFB 数据（`getPfbSegmentLengths()` 给出各段长度） | `FontFile3` |

> 裸 CFF 数据没有 sfnt 表目录，无法直接嵌入 PDF；`EmbeddedFontData.of(...)` 对此抛出 `IOException`，
> 由转换器按“内嵌优先、替换兜底”的策略改用替代字体并输出 warning 日志。

> 设计取舍：整字体嵌入不改变字形编号（GID），因此不会引入字形错乱；代价是字体会整体进入输出文件
> （Noto CJK 等 10~20MB 的字体会完整嵌入）。

### 使用示例

```java
import org.ofdrw.converter.font.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class FontEmbedExample {

    public static void main(String[] args) throws Exception {
        // 字体文件；字体集合（TTC）用 ttcIndex 选择集合内的字体，普通字体传 0
        Path fontPath = Paths.get("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc");
        int ttcIndex = 0;

        // 1. 嵌入元信息：决定嵌入键（FontFile2 / FontFile3）、BaseFont 名称与描述符字段
        FontDescriptor descriptor = FontDescriptor.from(fontPath, ttcIndex);
        System.out.println("BaseFont   = " + descriptor.getRenamedPostScriptName());
        System.out.println("嵌入键     = " + descriptor.getFontFileKey());
        System.out.println("需要重命名 = " + descriptor.isRenamingRequired());
        // 可直接灌入 PDF FontDescriptor 字典的字段
        Map<String, Object> entries = descriptor.toDescriptorEntries();

        // 2. 可嵌入数据：整字体原样导出；TTC 会自动提取指定字体并重建 sfnt
        EmbeddedFontData embedded = EmbeddedFontData.of(fontPath, ttcIndex);
        byte[] fontData = embedded.getData();
        System.out.println("字体数据 " + embedded.getLength() + " 字节，提取自集合 = "
                + embedded.isExtractedFromCollection());
        // Length1 = fontData.length（未压缩长度），PDF 侧按需压缩

        // 3. 字体度量：字宽用于 TJ 位移，上行/下行用于基准线与行距
        FontMetrics metrics = FontMetrics.of(fontPath, ttcIndex);
        System.out.println("unitsPerEm = " + metrics.getUnitsPerEm()
                + ", ascent = " + metrics.getAscent()
                + ", descent = " + metrics.getDescent());
        float advance = metrics.getAdvanceWidth(469);
        float textWidth = advance / 1000f * 12f;   // 12 号字下该字形的宽度（单位：pt）

        // 4. 字形选择：显式 GID（OFD 的 CGTransform）优先，其次 cmap，最后回退字形 0
        TrueTypeFont font = new TrueTypeFont().parse(fontData);
        GlyphIndexProvider glyphs = new GlyphIndexProvider(font);
        GlyphIndexProvider.GlyphSelection selection = glyphs.resolve(469, 'A');
        if (selection.isSubstituted()) {
            System.out.println("警告：字符被替换为字形 0");
        }
        int gid = selection.getGlyphId(0);

        // 5. ToUnicode：把写入 PDF 的字形编号映射回 Unicode（保证复制/搜索可用）
        ToUnicodeCMap toUnicode = new ToUnicodeCMap(descriptor.getRenamedPostScriptName(), metrics);
        toUnicode.add(gid, 'A');                   // 单个字符
        toUnicode.add(1234, new int[]{'f', 'i'});  // 连字：一个字形对应多个字符
        byte[] cmapBytes = toUnicode.toCMapBytes();

        // 也可以由字体 cmap 表批量生成（按 Unicode 编码写字的场景）
        ToUnicodeCMap fromFont = ToUnicodeCMap.fromFont(fontPath, ttcIndex);
        System.out.println("ToUnicode 映射 " + fromFont.size() + " 条");
    }
}
```

### 说明

- `FontMetrics` 的宽度、上行/下行高度与包围盒均为 **1/1000 文本空间单位**（PDF 字体字典的约定），
  换算关系：`1/1000 文本空间单位 = 字体设计单位 × 1000 / unitsPerEm`。
- `FontDescriptor` 对已带子集前缀（名称含 `+`）的字体重新加 `ABCDEF+` 前缀，前缀按名称哈希稳定生成，
  同一字体多次导出名称一致；名称不含 `+` 时按原样写入。
- `GlyphIndexProvider` 在发生字形替换（回退字形 0、显式 GID 越界、cmap 查不到字符）时输出 **warning 日志**，
  不抛异常、不阻断转换（设计决策：降级不阻断）。
- 本模块只提供能力，不封装输出文件格式；输出后端按上述 API 取数据即可。

## 谁在使用这些能力

| 使用方 | 用途 |
|---|---|
| `ofdrw-converter` 的 PDF 导出（`PDFExporter`，2.5.1 起） | 字体选择后以**整字体嵌入**（不做子集裁剪）写入 PDF；用 `ToUnicodeCMap` 保证文字可提取；字体缺失时按字族名匹配替代字体并输出 warning |

`org.ofdrw.converter.font` 的包名保持不变（历史原因），代码实现位于本模块 `ofdrw-font/src/main/java/org/ofdrw/converter/font/`。

> 提示：OFD 转 PDF 仅用于交换存档等场景，**不建议**作为查看手段；预览 OFD 请使用
> [ofdrw-viewer](https://github.com/ofdrw/ofdrw-viewer)。该提示只出现在文档与代码注释中，不会写入运行日志。
