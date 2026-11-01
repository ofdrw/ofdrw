# PDF → OFD 文字对象化回归样本

本目录是入库的**小体量 PDF 样本**，随 `mvn test` 由
[`PDFConverterTextObjectTest`](../../java/org/ofdrw/converter/pdf/textobj/PDFConverterTextObjectTest.java)
自动执行（设计文档 §7.2～§7.4、§8 P4）。**只放 PDF**，不放转换结果。

| 样本 | 体积 | 覆盖特性 | 断言要点 |
|---|---:|---|---|
| `dense-text-perf.pdf` | 381 KB | 纯文字中文、内嵌 SimSun（TrueType）原样嵌入、密集 `DeltaX` | TextObject ≥ 1000；PathObject ≤ 整页路径基线/10；内嵌字体可被 `TTFParser` 解析；文本相似度 ≥ 0.99；输出 ≤ 0.6 MB 且小于路径基线 |
| `keyword.pdf` | 297 KB | 内嵌字体 + 图片、文档级资源 | 内嵌字体全部可解析；页面墨迹与 PDF 对齐 |
| `chineseDir.pdf` | 3 KB | 中英混排、**无内嵌字体** → 路径回退 | 无 Res 字体、无 TextObject；PathObject 数与整页路径基线一致 |
| `V4RideRight.pdf` | 29 KB | 多页、特殊排版（含竖排转横排） | 5 页；路径内容存在；逐页墨迹与 PDF 对齐（竖排页用宽松口径） |
| `1-1.pdf` | 5 KB | 小票版式（描边/表格） | 3 页；逐行左端与行内像素重心对齐 |

- 体积：单文件 ≤ 1 MB，合计约 700 KB。
- 来源：仓库同级的 `ofd_pdf_match/` 数据集（不入库），按“覆盖关键分支”原则精选。
- 断言口径、基线生成方式（测试内用 `PDFRenderer` 整页渲染生成路径基线）与运行命令见
  [`doc/CONVERTER.md`](../../../../doc/CONVERTER.md) 的“PDF 文字对象化：实现落地与验证结果”一节。
- 完整 63 样本批量验证与 ofdrw-viewer-cli 对照**不入库**，产物在 `target/pdf-textobj/`。
