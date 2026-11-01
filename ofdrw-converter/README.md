# OFD Reader & Writer 文档转换

OFDRW 转换模块提供各类文档或媒体格式与OFD文档互相转换功能。

> **提示：本模块用于格式迁移，不建议用于预览。**
>
> - 预览 OFD：使用 OFDRW 系列中的 [ofdrw-viewer](https://github.com/ofdrw/ofdrw-viewer)，按需选用其阅读器组件、渲染引擎或 CLI，无保真损失。
> - 仅需图片：使用 [ofdrw-viewer-cli](https://github.com/ofdrw/ofdrw-viewer/tree/main/packages/ofdrw-viewer-cli)，支持 PNG / JPEG / WebP，无需引入本模块的三方依赖。
> - 转换有损：字体替换、复杂图形、电子签章与印章、加密文档均可能失真或失败。
> - 适用场景：PDF/文本/图片转 OFD 归档、OFD 导出 PDF 用于交换存档。
> - **不建议导出 PDF**：除交换存档等特殊需求外无需转换，仅需查看内容请直接用 ofdrw-viewer，详见[《OFD导出》](./doc/EXPORTER.md)。
> - 提示**只写在文档与代码注释中**，转换运行时不会输出这条提示（运行日志只记录降级/资源缺失等 warning）。

以OFD文档文档角度出发转换可分为：

- **导出**：OFD文档导出为其它类型文档或媒体文件格式，如OFD导出为图片、文本、PDF等。
- **转换**：它类型文档或媒体文件格式转换为OFD文档，如PDF转换OFD、文本转换OFD等。

请您根据您的需要阅读相关文档：

- [《转换OFD》](./doc/CONVERTER.md)
- [《OFD导出》](./doc/EXPORTER.md)

## PDF 转 OFD 的两种实现

PDF 转 OFD 提供两套并行实现，按是否需要"文本可检索"来选择：

| 实现类 | 转换方式 | 文本可检索 | 体积（密集文字样本） | 适用场景 |
|:--|:--|:--:|--:|:--|
| `PDFConverter` | 文字对象化：生成 `TextObject` + 内嵌字体 | 支持 | 约 0.3 MB | **推荐**，需要检索、归档、二次编辑的正式文档 |
| `PDFGraphicsConverter` | 图形化：把 PDF 当作图片渲染，字形全部转为路径 | 不支持 | 约 9 MB | 只要"看起来一样"的可视化归档；字形无法嵌入时 |

```java
// 推荐：文字对象化（文字可搜索、体积小）
try (PDFConverter converter = new PDFConverter(dst)) {
    converter.convert(src);
}

// 可选：图形化（视觉保真、实现简单，但不支持文本检索、体积约为前者的 30 倍）
try (PDFGraphicsConverter converter = new PDFGraphicsConverter(dst)) {
    converter.convert(src);
}
```

详见 [《转换OFD》](./doc/CONVERTER.md) 的「PDF转换OFD」与「图形化转换（把PDF当作图片）」章节。

## PDF 导出（2.5.1 起）

PDF 导出统一使用 `org.ofdrw.converter.export.PDFExporter`：

```java
try (OFDExporter exporter = new PDFExporter(ofdPath, pdfPath)) {
    exporter.export();      // 不传参数导出全部页面，页码从 0 起
}
```

- 基于 PDFBox 3.x 的新渲染管线（`org.ofdrw.converter.pdf`，内部实现，不承诺 API 稳定）。
- 历史入口 `PDFExporterIText`、`PDFExporterPDFBox`、`ItextMaker`、`PdfboxMaker` 已废弃，
  签名与可见性保持不变，内部委托 `PDFExporter`，**不再推荐使用**。
- **`export()` 只渲染到内存，PDF 在 `close()` 时才写出**——务必用 try-with-resources 或显式 `close()`，否则得到 0 字节文件。
- 本地回归请用脚本（不要裸跑 `mvn -Dtest=ManualPdfMatchTest`，surefire 的系统属性需经 `argLine` 进 fork JVM）：
  `OFD_M2_REPO=<仓库>/target/m2merge ./scripts/local-pdf-match.sh --assert`，
  详见 [scripts/README.md](./scripts/README.md)；重构设计见[《PDF 转换重构设计》](./doc/PDF转换重构设计.md)。
- 已知限制（含“像素一致但文本不可提取”的样本清单）见 [《OFD导出》](./doc/EXPORTER.md#已知限制251)。

