# OFD Reader & Writer 文档转换

OFDRW 转换模块提供各类文档或媒体格式与OFD文档互相转换功能。

> **提示：本模块用于格式迁移，不建议用于预览。**
>
> - 预览 OFD：使用 OFDRW 系列中的 [ofdrw-viewer](https://github.com/ofdrw/ofdrw-viewer)，按需选用其阅读器组件、渲染引擎或 CLI，无保真损失。
> - 仅需图片：使用 [ofdrw-viewer-cli](https://github.com/ofdrw/ofdrw-viewer/tree/main/packages/ofdrw-viewer-cli)，支持 PNG / JPEG / WebP，无需引入本模块的三方依赖。
> - 转换有损：字体替换、复杂图形、电子签章与印章、加密文档均可能失真或失败。
> - 适用场景：PDF/文本/图片转 OFD 归档、OFD 导出 PDF 用于交换存档。

以OFD文档文档角度出发转换可分为：

- **导出**：OFD文档导出为其它类型文档或媒体文件格式，如OFD导出为图片、文本、PDF等。
- **转换**：它类型文档或媒体文件格式转换为OFD文档，如PDF转换OFD、文本转换OFD等。

请您根据您的需要阅读相关文档：

- [《转换OFD》](./doc/CONVERTER.md)
- [《OFD导出》](./doc/EXPORTER.md)

