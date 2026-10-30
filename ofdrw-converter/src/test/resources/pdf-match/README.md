# OFD → PDF 回归样本

本目录是入库的**小体量回归样本**，随 `mvn test` 自动执行（设计文档第 7.1、7.2.1 节，决策 6、22）。
每个样本由两份同名文件组成：

| 文件 | 含义 |
|---|---|
| `<样本名>.ofd` | 输入文档 |
| `<样本名>.pdf` | **基准 PDF**，即“正确转换结果”的参照，用于像素级与文本级比较 |

> 基准 PDF 是既有 OFD 阅读器/转换器输出的配套 PDF，**不是本库生成的结果**；
> 本库的输出与基准之间存在差异属于正常现象，差异大小用指标衡量，阈值见下文。

更大的样本集（`ofd_pdf_match/`，58 组）**不入库**，仅作为本地手动回归的数据源，见
[`scripts/README.md`](../../../../../../scripts/README.md)。

---

## 执行与断言口径

- 测试类：`org.ofdrw.converter.export.PDFRegressionTest`
- 渲染分辨率：**72 dpi**（`ImageType.RGB`）
- 指标与阈值（决策 5、决策 22）：

| 指标 | 阈值 | 说明 |
|---|---|---|
| 页数 | 与基准一致 | 结构级最低要求 |
| 差异像素比例 | **≤ 0.5%** | 任一 RGB 通道差值 > 16 的像素 / 总像素 |
| 平均绝对误差 MAE | **≤ 2/255** | RGB 三通道差值绝对值的平均，按 255 归一 |
| 文本相似度 | **≥ 0.99** | `PDFTextStripper` 提取文本，NFKC 归一并去空白后按 UTF-8 字节 LCS 计算 |

- 失败时在 `target/pdf-regression/<样本名>/` 下输出 `*-expected.png`、`*-actual.png`、`*-diff.png`
  （差异像素标红），便于目视定位。
- 指标实现见 `src/test/java/org/ofdrw/converter/regression/`：
  `ImageDiff`（像素）、`TextComparison`（文本）、`PdfStructure`（页数/书签/附件/注释）、
  `PdfRegressionRunner`（批量执行与 Markdown 报告）。

---

## 样本清单

| 样本 | 体积（ofd/pdf） | 来源 | 覆盖特性 | 说明 |
|---|---|---|---|---|
| `helloworld` | 1.6 KB / 3.0 KB | OFD R&W 生成（`OFDDoc` + 段落布局） | TextObject、`DeltaX` 字距、中英混排、字体名替换（宋体未内嵌） | 最基础的冒烟样本 |
| `helloworld_with_pageblock` | 1.9 KB / 3.0 KB | 同上，文字外包一层 `PageBlock` | PageBlock 嵌套、PageBlock 与页面框的关系 | 校验“嵌套容器不误缩放” |
| `namespace_no_std` | 1.9 KB / 3.0 KB | 同上，命名空间改为 `http://www.ofdspec.org`（非 2016 标准 URI） | 非标准 namespace 容错、TextObject、DeltaX | 校验解析器宽松匹配 |
| `multiKeywordInTextCode` | 1.9 KB / 2.6 KB | 同上 | 单段 `TextCode` 多关键字、跨关键字逐字 `DeltaX` | 定位“按关键字分段排布”的对齐 |
| `chineseDir` | 2.1 KB / 3.0 KB | 同上 | 中文/标点连字度量差异、全角标点、字宽不一致的 `DeltaX` | 见下文“已知差异” |
| `path_unstd` | 8.2 KB / 7.8 KB | 历史缺陷样本（路径坐标不规范） | PathObject、Clip 裁剪、TemplatePage、TextObject、ImageObject | 旧实现的典型错位样本之一 |
| `1-1` | 3.6 KB / 5.1 KB | 业务文档裁剪样 | TextObject ×2、PathObject（描边线）、PageBlock、**Annotations（Watermark 水印注释）** | 曾用于 Alpha 语义实测（差异 0.95%） |
| `1` | 16 KB / 31 KB | 增值税电子发票样例（票面为公开样例模板） | **TemplatePage（Background）**、TextObject 十余处、`DeltaX` 含 `g` 进位、「首字不设增量」写法、PathObject、ImageObject、ColorSpace、附件（`original_invoice.xml`）、**Annotations（Stamp/签名章）**、Signatures | 模板页 + 签章外观的综合样本；页面为横向（595.28×396.85 pt） |
| `AddAttachment` | 39 KB / 40 KB | 附件功能样例 | Attachment ×2（含相对路径）、TextObject | 结构级：附件数量与名称 |
| `AddWatermarkAnnot` | 49 KB / 50 KB | 水印注释样例 | **Annotations（Watermark ×3）**、TextObject ×6（鲁迅《记念刘和珍君》节选）、ImageObject ×3 | 旧实现曾丢失全部文字（17.99% 差异） |
| `DigitalSign` | 3.0 KB / 3.1 KB | 数字签名样例 | Signatures ×2、TextObject（与 `helloworld` 内容相同） | 签名只转外观，验签有效性不在回归范围 |
| `containsJPEG` | 898 KB / 962 KB | 图像样例（**体积最大**） | JPEG 原样嵌入（决策 23）、ImageObject ×2、`DOC_0` 大写目录名容错 | 用于确认 JPEG 不重编码；媒介框 595.17×841.71 pt；体积最大，若需压缩入库体积可优先替换 |
| `testImageNotFound` | 174 KB / 1.13 MB | 异常容错样例 | 资源缺失容错、CGTransform ×2、TemplatePage ×2、Outline（书签）、Annotations（Watermark）、Signatures | 转换**不得抛异常**，缺图仅告警（决策 8、18） |

体积以文件实际大小计（KB 取整）。全部样本均为仓库原有的公开测试素材（由 OFD R&W 生成或公开样例），**不含真实业务数据或个人信息**。

---

## 已知差异（基准 PDF 与本库输出）

回归只按指标判定，不做逐像素或逐句豁免。入库前已用基准数据核对过的**基准自身特性**如下，
出现相关差异时优先归因为这些原因，而不是转换缺陷：

| 样本 | 基准 PDF 的固有特性 | 对本库指标的影响 |
|---|---|---|
| `chineseDir`、`helloworld` 系列 | 中文字体（宋体）未内嵌，显示依赖阅读器字体替换；不同渲染器的替换字体度量不同 | 出现在像素指标里，阈值 0.5% 已为此留出余量；文本提取不受影响 |
| `chineseDir` | 基准 PDF 把中英之间的间距也作为字形输出，OFD 中无对应字符 | 仅影响原始文本比对；本项目文本指标先去空白再比较，因此一致 |
| `containsJPEG` | 基准 PDF 中的图像编码参数与本库输出不同（画布尺寸一致） | 差异集中在图像边缘的极少数像素 |
| `testImageNotFound` | 基准 PDF 中缺失的图片区域为空白 | 与设计决策 8/18 的“降级跳过 + 告警”一致 |
| `testImageNotFound` | 基准 PDF 带有 Outline（书签），且签名字段与 OFD 结构不完全对应 | 结构级（书签/附件/注释）断言留到 P5 阶段，本阶段只比较像素与文本 |

> 如需新增样本：放入 `.ofd` 与基准 `.pdf` 后**必须**在上表补一行（来源、覆盖特性），
> `PDFRegressionTest#everySampleDocumented` 会检查 README 是否覆盖了目录中的每个样本。

---

## 后续阶段接入说明

P0 阶段 `PDFExporter` 尚无渲染实现，`PDFRegressionTest` 暂以 `PDFExporterPDFBox`（已废弃）作为
被验证的导出实现，用于证明“框架可跑通、指标可复现”。新渲染管线接通后，只需把
`PDFRegressionTest#runRegression` 中的导出器工厂换成 `PDFExporter`，样本与阈值不变。
