# OFD → PDF 转换重构设计

> 状态：**已实施**（2.5.1；方案与决策见第 9 节，新增公开能力见第 11 节，验证方法见第 12 节）
> 依据：GB/T 33190-2016《电子文件存储与交换格式 版式文档》、PDF 1.7（ISO 32000-1）
> 基线：`fb0cc476`（iText 已移除，重构前实现为 PDFBox 2.0.27）

---

## 1. 目标与非目标

### 1.1 目标

1. **向后兼容（决策 A，单一实现）**：现有公开接口——`ItextMaker`、`PdfboxMaker`、`PDFExporterIText`、`PDFExporterPDFBox`、`ConvertHelper#ofd2pdf/toPdf`、`OFDExporter`——签名与行为保持不变，老代码升级无感。
2. **另起炉灶**：在 `ofdrw-converter` 内新增独立包 `org.ofdrw.converter.pdf` 承载新管线；老接口保留签名并**内部委托**新实现，全库只保留一种实现。
3. **基于 PDFBox 3.0.8**（当前最新稳定版），不再受 2.x 限制。
4. **质量优先级**（决策，按此顺序）：视觉一致性 → 文字可选中可提取 → 保留书签/附件/注释/签名 → 性能与内存。
5. **可回归**：以 `ofd_pdf_match` 为基准建立像素级 + 文本级回归，关键小样本入库。

### 1.2 非目标

- PDF → OFD（另立项）。
- 公共渲染层（AWT/Graphics2D）重构：本次不改变其行为。
- 音视频动作（OFD 第 12 章 Sound / Movie）：**不转换播放动作**，图片图元本身仍正常绘制。

---

## 2. 现状与问题

现有 `ItextMaker` 与 `PdfboxMaker` 是两套并行的手写转换逻辑，均存在规范覆盖不足的问题（以下为调研结论，非推测）：

| 规范要求（GB/T 33190-2016） | 现有实现 | 影响 |
|---|---|---|
| 文字位置由 X/Y 与 **DeltaX/DeltaY 双数组**精确定位（表 46），Delta 队列可带**末位**尾部增量 | 仅处理 `DeltaX`，DeltaY 与尾部增量支持不完整 | 竖排、旋转文本错位 |
| **字形变换 CGTransform** 支持一对一/多对一/一对多/多对多（11.4） | 仅按 GID 或按 Unicode 二选一 | 连字、泰文等复合字形还原错误 |
| 图元 **Alpha 默认值 0**（表 34 备注）；颜色自身也有 Alpha（表 26） | 按 255 处理或忽略层级 | 透明度叠加错误 |
| 多个 **Clip 取交集**，Clip 内多个 **Area 取并集**，且 Area 可为 **Path 或 Text**（8.4、表 33） | 仅路径裁剪；层级语义不清 | 印章/水印裁剪错误 |
| 裁剪区 **随线宽等绘制参数影响**（表 33 DrawParam 备注） | 未处理 | 边界像素差异 |
| **轴向渐变 Extend(0~3)、MapType(Direct/Repeat/Reflect)、MapUnit、Segment Position 缺省均分**（表 29） | 仅支持 Direct、两段颜色 | 渐变图案失真 |
| **径向渐变 Eccentricity、Angle**（椭圆）与 Extend（表 30） | 按正圆近似 | 椭圆渐变失真 |
| **底纹 Pattern**：XStep/YStep 不得小于单元尺寸、ReflectMethod 四种翻转、RelativeTo、CTM 先排列后变换（表 28） | 未实现 | 水印/背景缺失 |
| **圆弧 A**：LargeArc / SweepDirection / 旋转角（表 36、表 42） | 未实现（`Q/B` 有，`A` 无） | 圆角、弧形路径缺失 |
| **图像边框 Border**（线宽、圆角、虚线、颜色，表 43）与 **ImageMask** | 未实现 | 证件/票据边框缺失 |
| 页面 **模板页** 与 **图层 ZOrder**、**PageBlock 嵌套** | 已基本支持，但按外接矩形缩放逻辑有误（曾导致图片偏移） | 版式错位 |

**底纹首版取近似实现（已确认）**：统一按 `ReflectMethod=Normal` 平铺，且只支持 `RelativeTo` 的默认
对象坐标语义；`Column`/`Row`/`RowAndColumn` 三种翻转与页面坐标相对定位在结果中标记为“近似”。
理由：样本中底纹仅出现 1 次，无法充分对照验证；正确实现成本高、风险不可控。

> 结论：这不是“换库”，而是**按标准重写渲染管线**，并补齐规范覆盖。

---

## 2.1 实测基线（58 组 ofd_pdf_match，现有实现 vs 配套 PDF）

方法：现有 `PDFExporterIText` 转换 → PDFBox 3.0.8 渲染 96 dpi → 与基准 PDF 同页比较（第一页，差异阈值单通道 >16）。

| 差异区间 | 样本数 | 代表样本 |
|---|---|---|
| ≤ 0.5%（可接受） | 26 | `helloworld`(0.08%)、`manual`(0.04%)、`chineseDir`(0.08%)、`透明度文字`(0.44%) |
| 0.5%~5% | 16 | `intro-数科`(1.35%)、`1-1`(0.95%)、`关键字`类(0.27%) |
| 5%~20% | 15 | `AddWatermarkAnnot`(17.99%)、`dense-text-perf`(30.9%)、`testPathClip`(11.2%) |
| >20%（严重） | 1 | `pattern类型`(99.4%) |

**三类典型故障（截图已复核）**：

1. **整页空白**：`hello`、`hello-integrity`、`拿来主义_page6` 输出全白——文字内容全部丢失。
2. **模板页/图层内容丢失**：`AddWatermarkAnnot` 仅剩图片，文字全部丢失；`draw_param_ref`、`1`、`path_unstd` 同类问题。
3. **底纹/图案整页变黑**：`pattern类型` 整页渲染为黑色（Pattern 未实现，被当作实心填充）。

**样本特性覆盖**（用于确认回归覆盖面）：TextObject 50、PathObject 28、ImageObject 17、**TemplatePage 15**、DeltaY 13、Annots 11、Attachments 9、CGTransform 7、Alpha 5、Clips 4、Outlines 4、CompositeObject 3、Arc 2、AxialShd/RadialShd 各 1、ImageMask 1、CharDirection 1。

> 结论：`TemplatePage`（模板页）与 `CGTransform` 覆盖率很高，却是现有实现的重灾区，应作为新实现 P1/P2 阶段的最高优先级。

---

## 3. 核心差异分析：OFD 与 PDF 语义对齐

### 3.1 坐标系统

| 维度 | OFD | PDF | 转换策略 |
|---|---|---|---|
| 页面原点 | **左上角**，Y 轴向下，单位 **mm** | **左下角**，Y 轴向上，单位 pt（1/72 in） | `pt = mm × 72/25.4`；`y_pdf = (pageH_mm − y_ofd) × k` |
| 页面框 | `PhysicalBox` / `ApplicationBox` | `MediaBox`（`CropBox` 可缺省） | `MediaBox = CropBox = PhysicalBox` |
| 图元局部坐标 | **对象空间**：以外接矩形左上角为原点（8.1.4） | 路径坐标直接处于当前 CTM 空间 | 图元内部路径先在外接矩形内定位，再叠加 CTM |
| CTM 表示 | `[a b c d e f]` 行向量 `[x y 1]·M`（8.1.5） | `[a b c d e f]` 行向量 `[x y 1]·M` | 写法一致，但**必须先做 Y 轴翻转再叠加**，否则旋转/镜像方向相反 |
| 渲染器一致性 | —— | PDFBox 2.x 在 Java 21 下对“页面整体 Y 翻转矩阵”渲染异常 | **不使用页面级翻转 CTM**，改为逐点换算坐标（已在上一轮验证） |

**决策**：采用**逐点换算**——所有图元坐标在写入前换算为 PDF 坐标，页面 CTM 保持恒等。
好处：与渲染器无关，规避翻转矩阵在部分渲染器上的缺陷；代价：每条路径需换算（开销可接受）。

### 3.2 变换与缩放语义（易错点）

- OFD 图元的 CTM 作用于**对象空间**；外接矩形定位是**先平移再叠加 CTM**。
- 现有实现中“模板页/印章缩放”的判定依赖父节点类型，曾导致普通页面图元被误缩放。新实现改为：缩放**只**由“图元所处容器是否与页面框不同”这一显式上下文决定，不再嗅探父节点类型。

### 3.3 绘制参数继承

OFD 采用“就近原则”（8.2.1）：图元属性 > 图元 DrawParam > 图层 DrawParam > 基础 DrawParam（`Relative` 递归）> 默认值。
PDF 是“状态机”：`q/Q` 保存恢复。因此转换时需要**显式计算有效绘制参数**，再一次性写入 PDF 图形状态，避免依赖 PDF 状态残留。

**设计约定**：`Relative` 链按规范递归解析，限制最大深度（32）并记录告警，防止环引用导致栈溢出。

### 3.3.1 核心模型显式属性语义

**实现后发现的关键问题**：`ofdrw-core` 中不少访问器在属性缺失时返回**规范默认值**而非 `null`
（`getCap()`→`Butt`、`getJoin()`→`Miter`、`getAlpha()`→`255`、`CT_DrawParam#getLineWidth()`→`0.353`、
`CT_ColorSpace#getBitsPerComponent()`→`BIT_1`）。这些默认值不能用来判断“属性是否被显式声明”，
否则图元上未写的字段会以“显式默认值”的身份覆盖 `DrawParam`/`Relative` 链上父级声明的值，就近原则被打断。

而且不同类的默认值之间并不自洽：`BitsPerComponent` 缺失返回 `BIT_1`，但 8.3.2 要求未声明位宽按 **8 位**处理。

处置：

1. `ofdrw-core` 新增 **7 个 `xxxExplicit()` 访问器**（缺失返回 `null`，显式写成规范默认值仍算“已声明”）；
2. 规范默认值只在渲染链**最末端兜底一次**（`GraphicsStateResolver`、`ColorResolver`、`ImageRendererImpl`）；
3. **不修改**既有访问器的返回值——那会破坏向下兼容，新增显式访问器是唯一正确处置。

本轮据此修复 4 处缺陷（`GraphicsStateResolver` 的 Cap/Join、`ColorResolver` 的位宽归一化、
`ImageRendererImpl` 的边框线宽、图元 Alpha 与颜色 Alpha 的叠加）。

> 详见 [《OFD 模型默认值语义说明》](../../ofdrw-core/doc/OFD模型默认值语义说明.md)。

### 3.4 颜色

| OFD | PDF | 说明 |
|---|---|---|
| GRAY / RGB / CMYK + BPC | `DeviceGray` / `DeviceRGB` / `DeviceCMYK`；或 ICCBased（有 Profile） | **必须用 `k`/`K` 写 CMYK，不能转 RGB**，否则黑色文字发灰 |
| `#` 十六进制与十进制通道值 | 需归一化到 0~1 | 越界值按默认色处理（规范 8.3.2） |
| 颜色 Alpha 0~255 | `ca` / `CA` | 与图元 Alpha 相乘 |
| 调色板 Index | 查表后取通道值 | BPC<8 时按位宽缩放 |

### 3.5 透明度层级

OFD 在两处表达透明度：**颜色 Alpha**（表 26）与**图元 Alpha**（表 34，默认 0，即完全透明！）。需要注意“缺省不写”与“写成 0”的差别：属性**缺失**时按规范默认 0 不透明，实际实现应视为“不透明”，只有显式写出才取值。这一点现有实现与规范默认值存在张力。

**决策（已实测确认）**：Alpha 缺省视为 **255（不透明）**，仅显式声明才生效。

实测依据（58 组样本 = 25441 个图元）：
- 仅 **126 个图元（0.50%）**显式声明 Alpha，其余 99.5% 依赖缺省值；
- 取值分布：0(3)、51(82)、76(20)、102(10)、127(2)、135(3)、153(3)、225(3)；
- 语义对照实验（对 `Alpha="0"` 的样本注入缺省值后重测）：

| 样本 | 缺省=不透明 | 缺省=全透明 |
|---|---|---|
| `testPathClip` | diffPix **11.21%** | diffPix **57.54%** |
| `透明度文字` / `1-1` / `ano` / `AddWatermarkAnnot` / `intro-数科` | 与基准一致 | 与基准一致 |

> 结论：严格语义在含大量无 Alpha 图元的样本上会造成严重退化；采用实用语义，并在文档中记录该偏离。

### 3.6 文字与字体（最复杂）

- OFD 文字以**字形原点**逐字定位（X/Y + Delta 队列），PDF 用 `Tm`/`TJ`。转换应在 `Tm` 中写入仿射矩阵，`TJ` 的位移按**字体宽度**修正。
- **CGTransform** 是 OFD 独有的“字符↔字形”映射，PDF 无直接对应。方案：
  - 保留 `ToUnicode` CMap 保证**可提取文本**（质量目标 2）；
  - 渲染走**字形索引**（GID），保证视觉一致（质量目标 1）。
  - 这与 iText 时代的 `FontWrapper#useSimulatedGlyphIndex` 思路一致，但要在机构上做对。
- **字体嵌入**：OFD 内嵌 OpenType 时**整字体嵌入**（不做子集裁剪，见 3.9）；缺失或裁剪时，按 FamilyName/Charset/Serif/Bold/Italic 匹配替代字体，并输出 **warning 日志**。
- **垂直文本**：`ReadDirection=90/180/270` 与 `CharDirection` 组合，需要按表 47 换算；必要时应生成 CMap 走 `WMode=1`。

**决策**：**字形优先 + ToUnicode**——能取到字形索引且字体可用时按 GID 输出保证视觉一致，
同时挂 `ToUnicode` CMap 保证文本可提取、可搜索。

### 3.7 图像

- **保真优先（已确认）**：JPEG 原样嵌入不重编码（避免二次损失）；PNG 等转为无损编码，透明通道走 Alpha/Soft Mask；不做有损压缩换体积。
- `ImageMask` → PDF 的 `/SMask`；`Substitution` → 高分辨率替换（PDF 侧仅保留主图）。
- **Border** 需要额外绘制路径（圆角矩形 + 虚线），不能依赖图像 XObject。

### 3.8 结构与元数据

| OFD | PDF | 策略 |
|---|---|---|
| 文档大纲 `Outlines` | `/Outlines`（书签树） | **仅映射标题与目标** `Dest`（XYZ/Fit/FitH/FitV/FitR，表 54）；不做大纲样式 |
| 附件 `Attachments` | `/Names /EmbeddedFiles` | 保留文件名、创建时间、MIME |
| 注释 `Annots`（Link/Path/Highlight/Stamp/Watermark） | 页面 `/Annots` | 视觉用 Appearance 重绘，交互用对应注释类型 |
| 数字签名与安全签章 | `/AcroForm` 签名字域 + Sig 字典 | **只转换外观**，无法保留原签名有效性（需明确声明） |
| 页面动作 PO/DO/CLICK | `/OpenAction`、链接注释 | 支持 Goto/URI/GotoA；**Sound/Movie 跳过并记录告警** |
| 文档属性 | `/Info`、`/Metadata`（XMP） | 写入 Creator/Producer/日期 |

---

### 3.9 字体能力扩展（在 ofdrw-font 中实现）

**原则**：字体解析、字形度量、ToUnicode、嵌入所需的字体数据**生成**统一放在 `ofdrw-font`，
`ofdrw-converter` 只负责“选哪种字体、如何映射到 PDF 对象”，不在 converter 内自行解析字体结构。

**决策：全部整字体嵌入，不做子集裁剪。**

#### 3.9.1 为什么不子集

| 维度 | 子集裁剪 | 整字体嵌入（选定） |
|---|---|---|
| 复杂度 | TTF 需重写 head/maxp/loca/glyf 并处理复合字形；CFF 需重写 INDEX/DICT/FDArray，调试成本 2~3 倍 | 低：只做格式识别与正确包装 |
| 正确性风险 | 字形编号重排需同步 CGTransform 与 ToUnicode，出错表现为字形错乱而非异常 | 低：字体数据原样嵌入，不会引入字形错误 |
| 体积 | 小 | 大（Noto CJK 等 10~20MB 字体会整体进入 PDF） |
| 适用场景 | 网络传输、大量文档 | 交换存档（本模块定位） |

结论：本模块定位是“交换存档”，优先保证正确性；体积开销可接受。若后续确实需要压缩体积，再作为独立特性追加。

#### 3.9.2 现状与缺口

`ofdrw-font` 已具备：TTF/TTC/CFF/Type1 解析（`TrueTypeFont`、`TrueTypeCollection`）、`cmap` 正反查
（`CmapSubtable#getGlyphId` / `#getCharCodes`）、`hmtx`/`hhea` 度量、`glyf` 轮廓查询（`getPath(int gid)`）、
字体集合（TTC）内单字体的提取。

面向“整字体嵌入”仍缺：**字体类型识别**（TTF/OTF-CFF/Type1）、**嵌入所需的度量汇总**、
**ToUnicode 映射生成**、**字体数据原样导出**（含 TTC 内单字体提取后的 sfnt 重建）。

#### 3.9.3 计划新增的 public API（新增类，不改动已有类签名）

| 新增类型 | 职责 |
|---|---|
| `FontDescriptor` | 嵌入元信息：字体类型（`FontFile2` / `FontFile3`）、PostScript 名、字族名、`ItalicAngle`、`FontBBox`、`Flags`、是否需要 `ABCDEF+` 重命名 |
| `FontMetrics` | 统一度量：`unitsPerEm`、`getAdvanceWidth(gid)`、`getAscent/Descent`、`getBBox()` |
| `EmbeddedFontData` | 原样导出可嵌入的字体数据（TTC 时提取指定字体并重建 sfnt），不做子集 |
| `ToUnicodeCMap` | 由 `glyph → unicode` 映射生成标准 ToUnicode CMap 文本（保证文字可提取） |
| `GlyphIndexProvider` | 统一“字符 → 字形”访问：优先 `CGTransform` 显式 GID，其次 `cmap` 查表，最后回退字形 0，并标记是否发生替换 |

#### 3.9.4 为什么放在 font 模块

- `ofdrw-layout`、`ofdrw-converter` 及其它输出后端都需要同样的度量与嵌入数据能力，可复用；
- 现有 `ofdrw-font` 已裁剪自 PDFBox，sfnt 表结构读取可直接沿用（`TTFDataStream`）；
- 保持 converter 与字体二进制格式解耦，便于单独回归（`ApiCompatibilityTest` 已覆盖该包公开类型）。

#### 3.9.5 兼容约束

- **不修改** `TrueTypeFont` 等已有公开签名；新能力以新增类的方式提供；
- 新类标注 `@since 2.5.1`，并在 `ofdrw-font/README.md` 增加使用示例；
- 字体缺失或无法解析时，按既定策略“内嵌优先、替换兜底”，发生替换时输出 **warning 日志**。

#### 3.9.6 实施记录（2.5.1）

- 已新增 `FontDescriptor`、`FontMetrics`、`EmbeddedFontData`、`ToUnicodeCMap`、`GlyphIndexProvider` 五个类（`org.ofdrw.converter.font`）。
- **整字体嵌入按要求实现**：写入侧以整字体数据建 `PDType0Font`（`embedSubset=false`），不做子集裁剪（决策 16）。
- 实施中修复了 `ToUnicodeCMap` 两处真实缺陷：单元素映射丢失；`bfrange` 条目带字宽信息被判为非法 CMap，
  表现为文本提取退化为乱码（形如 `fƑ`）。修复后 `PDFTextStripper` 提取结果与基准一致。

---

## 4. 目标架构

### 4.0 export 包新增入口

`export` 包统一入口命名为 **`PDFExporter`**（与其他导出器 `*Exporter` 保持一致）。

**契约**：实现 `OFDExporter` 接口——`export(int... indexes)` 导出指定页（空参表示全部）、`close()` 写出文档级信息（附件、书签、元数据）。

**构造器**：

| 构造器 | 说明 |
|---|---|
| `PDFExporter(Path ofdFilePath, Path pdfFilePath)` | 文件 → 文件 |
| `PDFExporter(InputStream ofdInStream, OutputStream pdfOutStream)` | 流 → 流 |
| `PDFExporter(OFDReader ofdReader, Path pdfFilePath)` | 复用解析器 → 文件 |
| `PDFExporter(OFDReader ofdReader, OutputStream pdfOutStream)` | 复用解析器 → 流 |

**专用扩展方法**：在接口契约之外，允许 `PDFExporter` 提供少量专用方法补充能力（例如页面元数据覆盖、按页输出到独立流），
命名清晰、不修改接口本身。首版不预置；确需时再按需添加，避免提前设计用不上的 API。

**定位**：`PDFExporter` 是新 API 的唯一载体；`org.ofdrw.converter.pdf` 是其内部实现细节，不对外承诺稳定性。
老接口（`PDFExporterIText`、`PDFExporterPDFBox`、`ItextMaker`、`PdfboxMaker`）**直接修改内部实现**调用同一套新代码，
不新增 `compat/` 之类的适配层。

### 4.1 包结构（新增，老包不动）

```
org.ofdrw.converter.export
├── PDFExporter                          对外入口（唯一新 API 载体）：OFD 文件/流 → PDF 文件/流
└── PdfExportOptions                     配置（字体策略、文字模式；如需调整由专用方法传入，首版不预置）

（不提供结果类型：字体替换、资源缺失与未实现特性统一输出 warning 日志）

org.ofdrw.converter.pdf                  内部实现包（package-private 为主，不对外承诺稳定性）
├── model/                               中间模型（与库无关）
│   ├── PdfPageContent                   页内容指令序列
│   └── ...                              路径/文本/图像/渐变/裁剪 指令对象
├── render/                              渲染管线
│   ├── PageRenderer                     页面级（页框、模板页、图层 ZOrder）
│   ├── GraphicsStateResolver            绘制参数就近原则 + DrawParam 继承链
│   ├── PathRenderer                     S/M/L/Q/B/A/C，填充规则
│   ├── TextRenderer                     定位、Delta、CGTransform、方向
│   ├── ImageRenderer                    编码选择、SMask、Border
│   ├── ShadingRenderer                  轴向/径向/高洛德 → PDFShading
│   ├── PatternRenderer                  底纹 → Tiling Pattern
│   └── ClipApplier                      并集/交集、路径与文字裁剪
├── font/                                字体选择与 PDF 对象映射（解析/嵌入数据能力来自 ofdrw-font）
└── structure/                           大纲、附件、注释、动作、元数据
```

**为什么要中间模型**：转换指令与 PDFBox 解耦，才能对同一份指令做“渲染器无关”的单元测试（例如断言生成的 `TJ` 位移序列），也让未来换渲染后端不用重写。

### 4.2 转换流水线

```
OFDReader ──解析──> 页面/资源/注释/签名模型 ──> PageRenderer ──指令流──> PdfPageContent
                                                        │
                                    ┌───────────────────┴────────────────────┐
                                    ▼                                        ▼
                          PdfBox3Writer（默认）                     测试用 DumpWriter
                                    ▼
                             PDDocument(3.0.8) ──> PDF 文件/流
```

### 4.3 关键设计约束

1. **不依赖 PDAbstractContentStream 的隐式状态**：所有 `q/Q` 成对、状态显式写入，避免“一次异常导致后续全部错位”（上一轮已实证该问题）。
2. **所有坐标换算集中于一处**：`CoordSystem` 工具类，禁用零散 `×72/25.4`。
3. **容错分层**：单图元异常与未实现特性（如视频动作）**跳过并打印 warning**，不得中断整页；页面级异常输出空白页并记录；
   **加密（密码保护）文档直接抛出异常**，不静默输出空白页。
4. **可观测**：字体替换、资源缺失、未实现特性统一通过 **warning 日志**输出（不提供结果对象），回归测试通过日志断言。

---

## 5. PDFBox 3.0.8 适配要点（已验证）

以下 API 在 3.x 中**移除或弃用**，新实现必须规避（本地已编译验证）：

| 2.x 写法 | 3.x 替代 |
|---|---|
| `PDDocument.load(File/byte[])` | `Loader.loadPDF(...)` |
| `PDType1Font.HELVETICA` 等常量 | `new PDType1Font(Standard14Fonts.FontName.HELVETICA)` |
| `TTFParser.parse(InputStream)` | `parse(RandomAccessRead)`，如 `new RandomAccessReadBuffer(bytes)` |
| `CFFParser.parse(byte[])` | `parse(byte[], ByteSource)` |
| `PDPageContentStream.setTextMatrix(6 参数)` | `setTextMatrix(Matrix)` |
| `drawXObject(XObject, AffineTransform)` | `drawForm(PDFormXObject)` + `transform(Matrix)` |
| `new PDFormXObject(PDStream)` | `new PDFormXObject(PDDocument)` |
| `PDPageContentStream(doc, form, stream)` | 已移除，改用 `PDFormXObject(PDDocument)` + 内容流写入 |

依赖变化：`pdfbox:3.0.8`、`fontbox:3.0.8`、新增传递依赖 `pdfbox-io:3.0.8`；`jbig2-imageio:3.0.5`（reader 模块）。

---

## 6. 向后兼容方案（决策 A）

1. 老类、老方法、老签名**全部保留**；不做删除、不改包名。
2. 老类的**内部实现直接改为调用新 API**（`PDFExporter` / 内部管线），不新增适配层、不保留双实现。
3. 老接口标记 `@Deprecated` 并在 Javadoc 指向新入口。
   - 涉及：`PDFExporterIText`、`PDFExporterPDFBox`、`ItextMaker`、`PdfboxMaker`；
   - 类注释统一写入提示：**不建议导出为 PDF，仅需查看请使用 ofdrw-viewer**（不在运行日志中输出提示）；
   - 相关文档**隐藏旧实现**（`EXPORTER.md` 不再展开旧类用法），只保留一句指向新实现的说明。
4. 版本策略：本次为新功能，按语义化版本走 **minor**；若必须引入不兼容，另起 major。

**决策（已确认）**：全库只保留**一种实现**——老接口签名、可见性与行为契约不变，内部整体替换为新管线；
标注 `@Deprecated` 并指向新入口；不存在“新旧结果不一致”的双份维护成本。

**开发期策略（已确认）**：
1. 依赖直接升级到 PDFBox **3.0.8**，新旧代码统一使用 3.0.8（不保留 2.0.27）；
2. 开发期间，只被旧实现使用、且与新管线无关的旧代码先**注释或暂时摘除**，让工程只以新实现可编译；
3. 新管线完成后，把旧类的内部实现直接替换为对新 API 的调用，恢复对外契约后统一提交。

---

## 7. 测试与回归

### 7.1 样本策略

- `ofd_pdf_match/`（58 组）**不入库**，作为**本地手动回归**脚本的数据源；`src_material/` 同样不入库，均已在 `.gitignore` 中。
- 挑选**小体积 + 无敏感信息 + 覆盖关键特性**的样本入库至 `ofdrw-converter/src/test/resources/pdf-match/`，逐样本附 `README`（来源、覆盖特性）：
  - 基础：`helloworld`、`helloworld_with_pageblock`、`namespace_no_std`
  - 文字：`multiKeywordInTextCode`、`chineseDir`
  - 图形：`path_unstd`、`1-1`、`1`
  - 结构：`AddAttachment`、`AddWatermarkAnnot`、`DigitalSign`
  - 资源：`containsJPEG`、`testImageNotFound`（异常容错）

### 7.2 回归断言

1. **像素级**：PDFBox 渲染 72/144 dpi，与基准 PDF 逐像素比较；指标用 **平均绝对误差（MAE）+ 差异像素比例**，避免抗锯齿抖动误判。
2. **文本级**：`PDFTextStripper` 提取文本与基准 PDF 比较（保证质量目标 2）。
3. **结构级**：页数、书签层级、附件数量与名称、注释数量。
4. **告警级**：日志中无“未支持特性/资源缺失”的 warning（白名单除外）。

**决策**：72 dpi 下**差异像素比例 ≤ 0.5% 且 MAE ≤ 2/255** 为通过；超出即回归。
阈值以基准样本实测校准（现有实现在 26 个样本上已满足该阈值，可作为框架自检基准）。

### 7.2.1 执行方式（已确认）

| 范围 | 触发 | 说明 |
|---|---|---|
| 入库小样本 | `mvn test` 自动 | 随构建执行，断言像素与文本指标 |
| 58 组大样本 | 本地手动脚本 | 数据源为 `ofd_pdf_match/`，输出逐样本指标报告，不进入 CI |

### 7.3 性能基线

`dense-text-perf.ofd`、`intro-数科.ofd` 记录转换耗时与峰值内存，作为 CI 非阻断基线。

---

## 8. 实施计划（分阶段，每阶段可独立评审）

| 阶段 | 内容 | 验收 |
|---|---|---|
| P0 | 工程化：升级 3.0.8 并修兼容、包骨架、`PDFExporter` 入口、旧类废弃与文档更新、开发期旧实现摘除、样本入库与回归框架 | 全量测试在 3.0.8 下通过；回归框架可输出像素/文本指标 |
| P1 | 坐标系统、绘制参数、路径与裁剪（含圆弧 A、填充规则） | 图形类样本像素达标 |
| P2 | 文字（定位/Delta/方向/CGTransform）+ `ofdrw-font` 扩展（`FontDescriptor`/`FontMetrics`/`EmbeddedFontData`/`ToUnicodeCMap`/`GlyphIndexProvider`）+ 整字体嵌入与替换 | 文本类样本像素 + 文本提取达标 |
| P3 | 颜色（CMYK/调色板/ICC）、透明度、渐变（轴向/径向）、底纹（Normal 近似） | 颜色类样本达标 |
| P4 | 图像（编码、SMask、Border）、模板页/图层、复合对象 | 版式类样本达标 |
| P5 | 结构：书签、附件、注释、动作（音视频跳过）、元数据、签名外观 | 结构级断言通过 |
| P6 | 旧类内部改为调用新 API（单一实现）、文档定稿、性能优化 | 老接口全量测试通过 + 指标不退化 |

实施状态（2.5.1）：

| 阶段 | 状态 | 验证 |
|---|---|---|
| P0 工程化与骨架 | 已完成 | PDFBox 3.0.8 全仓升级；`org.ofdrw.converter.pdf` 包骨架与 `PDFExporter` 入口 |
| P1 坐标/路径/裁剪 | 已完成 | 图形相关 63 项单测全绿 |
| P2 文字与字体 | 已完成 | 文字相关 29 项单测全绿；`ofdrw-font` 5 个新类 |
| P3 颜色/透明度/渐变/底纹 | 已完成 | P3 相关 75 项单测全绿 |
| P4 图像与版式 | 已完成 | 图像与页面 58 项单测全绿 |
| P5 结构映射 | 已完成 | 结构映射与写入单测、`PDFStructureIntegrationTest` |
| P6 旧类替换与文档定稿 | 已完成 | 旧类与 `PDFExporter` 导出内容流算子序列逐字一致；全量测试见 12.1 |

---

## 9. 已确定的决策

| # | 事项 | 决议 |
|---|---|---|
| 1 | 坐标系实现 | **逐点换算**：图元坐标在写入前换算为 PDF 坐标，页面 CTM 保持恒等 |
| 2 | Alpha 缺省语义 | **缺省不透明（255）**，经实测确认（见 3.5） |
| 3 | 文字输出模式 | **字形优先 + ToUnicode**，保证视觉一致与文本可提取 |
| 4 | 老接口行为 | **单一实现**：老接口内部调用新实现，无回退开关 |
| 5 | 像素回归阈值 | **差异像素 ≤0.5% 且 MAE ≤2/255**（72 dpi），以实测校准 |
| 6 | 入库样本 | 按第 7.1 节清单提交，含基准 PDF 与 README |
| 7 | 字体替换 | 输出 warning 日志，**不阻断** CI |
| 8 | 严格执行模式 | **统一容错**（不做严格/宽松双模式）：降级跳过并输出 warning 日志 |
| 9 | 忽略文件 | `ofd_pdf_match/`、`src_material/` 加入 `.gitignore` |
| 10 | PDFBox 版本 | **3.0.8**（当前最新稳定版） |
| 11 | 新实现位置 | `ofdrw-converter` 内新建独立包 `org.ofdrw.converter.pdf` |
| 12 | 范围 | 仅 OFD → PDF；公共渲染层不改 |
| 13 | export 包入口 | 新增 `PDFExporter`，`ItextMaker`/`PdfboxMaker` 等旧实现隐藏（废弃 + 文档不展开） |
| 14 | 提示文案 | “不建议导出为 PDF，预览请用 ofdrw-viewer”统一写入**代码注释/文档/Javadoc**；该宣传性提示**不写入运行日志**（与第 8 条降级 warning 是两回事） |
| 15 | 字体能力 | 优先扩展 `ofdrw-font`（度量/整字体嵌入数据/ToUnicode/嵌入元信息），converter 不自行解析字体结构 |
| 16 | 字体子集 | **不做子集裁剪，全部整字体嵌入**（正确性优先，体积开销可接受） |
| 17 | 底纹 | 首版按 `ReflectMethod=Normal` 近似，其余翻转与页面相对定位标记“近似” |
| 18 | 音视频 | Sound / Movie 动作**跳过**并输出 warning，图片图元正常绘制 |
| 19 | API 暴露面 | `PDFExporter` 实现 `OFDExporter` 接口，另允许少量专用扩展方法；**不提供 Result/Options 结果对象** |
| 20 | 旧接口切换时机 | 新管线完成后再把旧类内部替换为新实现 |
| 21 | 依赖升级策略 | 直接升到 3.0.8；开发期旧实现可临时注释摘除，统一用 3.0.8 开发 |
| 22 | 回归执行 | 入库小样本随 Maven 测试自动跑；58 组大样本作为本地手动脚本 |
| 23 | 图像嵌入 | 保真优先：JPEG 不重编码，其余无损 + Alpha |
| 24 | 加密文档 | 检测到密码保护**直接抛异常**，不输出空白页 |
| 25 | 大纲映射 | 仅映射标题与目标页/位置，不做大纲样式 |
| 26 | 页面框 | `MediaBox = CropBox = PhysicalBox`，不做内缩与旋转 |

---

## 10. 复杂度评估

### 10.1 客观基数

| 维度 | 量级 | 说明 |
|---|---|---|
| 标准正文 | 12.7 万字，60 张属性表 | 渲染相关 15 张（颜色/渐变/底纹/裁剪/图元/图形/文字/注释/目标区域等） |
| 现有待替代代码 | 约 4200 行（`ItextMaker` 1992 + `PdfboxMaker` 1107 + `PointUtil` 660 + `CommonUtil` 427） | 且这些代码仍覆盖不足 |
| 样本特性覆盖 | 58 组样本：文字 53、路径 36、区域/裁剪 29/8、图像 24、注释 19、CGTransform 11、附件 9、复合对象 5、大纲 4、轴向/径向渐变 2/1、底纹 1 | 覆盖广且分散，回归面大 |

### 10.2 分模块难度

| 模块 | 难度 | 主要原因 | 风险 |
|---|---|---|---|
| 坐标与变换 | 中 | OFD 对象空间 + CTM + Y 翻转三者叠加，易错但可穷举验证 | 低（可单测） |
| 路径与裁剪 | 中高 | 圆弧 `A`（LargeArc/Sweep）、二次贝塞尔转换、Area 并集/Clip 交集、Text 裁剪 | 中 |
| 绘制参数 | 中 | 就近原则 + `Relative` 继承链，需逐图元计算有效值 | 低 |
| 颜色 | 中 | GRAY/RGB/CMYK/BPC/调色板/ICC，CMYK 不可转 RGB | 低 |
| 渐变 | **高** | MapType(Direct/Repeat/Reflect) + Extend(0~3) + MapUnit + Segment 位置缺省均分；径向还有 Eccentricity/Angle 椭圆 | 高（PDF Shading 只能部分表达，需要分片） |
| 底纹 Pattern | 中（Normal 近似） | 首版按 Normal 平铺；完整翻转与页面相对定位暂不实现 | 中（已确认降级） |
| 文字 | **高** | 定位(DeltaX/DeltaY/尾增量)、CharDirection×ReadDirection 组合、CGTransform 四种映射、字形优先与可提取并存 | 高 |
| 字体 | 中高 | 整字体嵌入降低了难度；剩余工作是格式识别、TTC 提取重建、ToUnicode、替换后度量一致性 | 中 |
| 图像 | 中 | 编码选择、SMask、Border（圆角/虚线） | 中 |
| 结构（大纲/附件/注释/签名外观） | 中 | PDF 对象映射直接，但注释类型多、签名只能转外观 | 中 |
| PDFBox 3.x 适配 | 中 | API 已定位，但需全量回归 | 低 |

### 10.3 结论

- **总量**：约为一个“中型渲染器”的规模，预计新代码 6000~9000 行（含测试），明显大于当前 4200 行的两套旧实现。
- **真正难的不是接口，而是两类语义**：渐变（轴向/径向）的映射模型差异、文字与字形的可提取性；字体因整字体嵌入而难度下降。
- **最大不确定项**：底纹（`Pattern`）只有 1 个样本，无法充分验证；**已确认首版按 Normal 翻转近似**，并在结果中标记。径向渐变样本 2 个，按完整实现（含 Eccentricity/Angle）推进。
- **可按阶段交付**：P1（坐标/路径/裁剪）+ P2（文字/字体）能覆盖 53+36 个样本；P3、P4 为渐进增强，不阻塞主干。

### 10.4 范围裁剪（已确认）

**主干全量实现**，仅两项降级：

- **底纹**按 `ReflectMethod=Normal` 近似，其余翻转与页面相对定位标记“近似”；
- **音视频动作**跳过，图片图元正常绘制。

两项均输出 warning 日志。全量（含底纹完整翻转）与最小集不采用。

---

## 11. 变更清单与新增公开 API

> 本次重构**未删除、未修改任何既有 public 方法签名或可见性**；新增能力全部以新增类型/新增方法提供；
> 受影响的旧实现类（`PDFExporterIText`、`PDFExporterPDFBox`、`ItextMaker`、`PdfboxMaker`）保留签名与可见性，
> 内部改为委托新实现，并标注 `@Deprecated` 说明替代位置。

核对方式：对 5 个被改造类做“HEAD 与当前版本逐行 public 签名 diff + `javap -public` 类型级比对”，
结果均为**无删除、无修改**（`ItextMaker` 3、`PdfboxMaker` 5、`PDFExporterIText` 4、`PDFExporterPDFBox` 4、
`ConvertHelper` 15 个公开成员逐行一致）；`org.ofdrw.converter.point`（`PathPoint`、`TextCodePoint`、`Tuple2`）
保持原样，`git status` 无改动。

### 11.1 新增公开 API（均为新增，向后兼容）

| 模块 | 新增内容 | 说明 |
|---|---|---|
| `ofdrw-core` | `CT_GraphicUnit#getCapExplicit()`、`#getJoinExplicit()`、`#getAlphaExplicit()`；`CT_DrawParam#getLineWidthExplicit()`；`CT_ColorSpace#getBitsPerComponentExplicit()`；`org.ofdrw.core.image.Border#getLineWidthExplicit()`；`CT_Color#getAlphaExplicit()` | 7 个显式属性访问器，缺失返回 `null`；语义见 [《OFD 模型默认值语义说明》](../../ofdrw-core/doc/OFD模型默认值语义说明.md) |
| `ofdrw-font` | `FontDescriptor`、`FontMetrics`、`EmbeddedFontData`、`ToUnicodeCMap`、`GlyphIndexProvider` | 整字体嵌入所需的格式识别、度量、嵌入数据、ToUnicode 与字形选择能力（`org.ofdrw.converter.font`） |
| `ofdrw-converter` | 新包 `org.ofdrw.converter.pdf`（`model` / `render` / `font` / `structure` / `write`） | 新的渲染管线实现，**内部实现细节，不对外承诺稳定性**；包内 `README.md` 记录契约 |
| `ofdrw-converter` | `org.ofdrw.converter.export.PDFExporter` | 对外唯一新入口，实现 `OFDExporter`，提供 4 个构造器 |
| `ofdrw-converter` | `pdf/font/TrueTypePdfFont.Registry#lookupByResourceName(String)`、`pdf/render/PageRendererImpl#create(...)` 等渲染链装配 API | 支撑写入侧按资源名取字体、一次装配整条渲染链 |

### 11.2 行为变更（对外可见，均为修正或决策既定降级）

- 旧类内部统一委托 `PDFExporter`（决策 4）：`PDFExporterIText`、`PDFExporterPDFBox`、`ConvertHelper#ofd2pdf/toPdf`
  与 `PDFExporter` 输出一致。

#### 11.2.1 `makePage` / `addAttachments` 改为显式失败（有意的兼容性取舍）

| 类 | 方法 | 2.5.0 行为 | 2.5.1 行为 |
|---|---|---|---|
| `ItextMaker` | `makePage(PageInfo)` | 逐页渲染并返回 `PDPage` | 抛 `UnsupportedOperationException`，提示改用 `PDFExporter#export(int...)` |
| `ItextMaker` | `addAttachments(OFDReader)` | 向 `PDDocument` 写入附件 | 抛 `UnsupportedOperationException`，提示改用 `PDFExporter#close()` |
| `PdfboxMaker` | 同上两个方法 | 同上 | 同上 |

**这是有意的取舍，而非疏漏**：

- 这两个方法的语义是“把结果写进调用方持有的 `PDDocument`”，而新管线由 `PDFExporter` 独占文档生命周期、
  在 `close()` 时统一写出附件与书签；继续提供“可调用”的同名方法，只会让调用方得到一个
  **缺少书签/附件/元数据的半个 PDF**，且与 `PDFExporter` 的输出口径不一致。
- 仓库内这两个方法**除 `ConvertHelper` 外无调用方**，而 `ConvertHelper` 已改走 `PDFExporter`。
- 公开签名与可见性**保持不变**（`javap -public` 逐行一致），仅行为由“可调用”变为“显式失败 + 明确替代位置”，
  使问题在开发期暴露而不是产生静默的错误产物。
- 若调用方确有“逐页构造自有 `PDDocument`”的需求，应直接使用内部包 `org.ofdrw.converter.pdf`
  的 `PageRendererImpl#create(...)` + `PdfCommandReplayer`（内部实现，不承诺 API 稳定）。
- 历史实现特有的错误换算口径不再保留（例如把 mm 线宽直接当作 px、把 Foxit 像素坐标按 300dpi 猜成 pt），
  统一按 GB/T 33190 的 mm→pt 与 Y 翻转换算；`LegacyPathExportTest` 相应改为断言正确换算。
- 决策 17（底纹按 `Normal` 近似、其余翻转标记“近似”）与决策 18（Sound/Movie 动作跳过）会输出 warning 日志。

## 12. 验证方法

### 12.1 构建与测试命令

```bash
# 根 pom.xml 中 skipTests 默认为 true，跑测试必须显式关闭，否则只会打印 "Tests are skipped." 后 BUILD SUCCESS
mvn -o -Dmaven.repo.local=<仓库>/target/m2merge -pl ofdrw-full -am test \
    -DskipTests=false -Dgpg.skip=true -DargLine="-Djava.awt.headless=true"

# 只跑某模块/某测试类
mvn -o -Dmaven.repo.local=<仓库>/target/m2merge -pl ofdrw-converter -am test \
    -DskipTests=false -Dgpg.skip=true -Dtest='org.ofdrw.converter.pdf.**' \
    -Dsurefire.failIfNoSpecifiedTests=false -DargLine="-Djava.awt.headless=true"
```

- **判定标准**：日志中必须出现 `Tests run: N`；只出现 `Tests are skipped.` 说明测试没有执行。
- 本机 `~/.m2` 可能只读或缺少依赖，需用 `-Dmaven.repo.local=<仓库>/target/m2merge`；
  `mvn install` 时追加 `-Dmaven.javadoc.skip=true`，否则 javadoc 插件报 `JAVA_HOME` 错误且**静默不更新 jar**。

### 12.2 并行开发期的日志/报告陷阱

多人同时修改 `ofdrw-converter` 的 main 与 test 源码时，`test-compile` 失败会**清空 `target/test-classes`**，
表现有两种：

1. 先前跑过的测试类仍显示旧报告（0 failures）——**假绿**；
2. 运行时报 `NoClassDefFoundError: xxxTest$1`（匿名内部类找不到）或“类文件读一半”——**假失败**。

判定：**只采信自己那次运行时间点之后生成的 surefire 报告**；并发期间单次出现大量
`NoClassDefFoundError` 时先复跑，不要据此修改实现。实践验证：14:47 并发构建出现 172 个
`NoClassDefFoundError`，14:49 无并发时只剩 1 个。

另有一个同类问题：**增量编译的时间戳错配**——源码已还原但 `target` 里仍是调试版字节码。
改过又还原的文件请先 `touch` 再编译。

### 12.3 回归

- 入库小样本随 `mvn test` 自动执行（`PDFRegressionTest`），阈值：差异像素 ≤ 0.5% 且 MAE ≤ 2/255，文本相似度 ≥ 0.99。
- 58 组大样本用本地脚本手动执行（不进入 CI）：

```bash
OFD_M2_REPO=<仓库>/target/m2merge ./ofdrw-converter/scripts/local-pdf-match.sh --assert
```

`OFD_M2_REPO` 用于指定 Maven 本地仓库（`~/.m2` 只读时必须指定），报告输出到
`ofdrw-converter/target/local-pdf-match/report.md`。

### 12.4 实施后的已知限制（2.5.1 实测基线）

本地 59 组有效样本全量对照（`-Dofd.match.full=true`，脚本 `scripts/local-pdf-match.sh`）：
**通过 12 / 不通过 47**。通过与全量数字随渲染能力迭代变化，**不作为版本承诺**，仅用于定位问题域。

**已修复的根因（多页文档文本提取）**：字体对象在首次使用时创建 ToUnicode，后续页新增字形无法补充；
已改为在保存前统一刷新全部字体的 ToUnicode。连带修正 `codespacerange` 与 `codeByteLength`
“按最大 GID 判断 1/2 字节”的错误——Identity-H 恒为双字节 CID。修复后 `page1`/`page2`/`page3`
（此前像素达标但文本相似度为 0）全部转为通过。

**当前文本提取最差样本**（像素已基本达标，问题集中在文本可提取性）：

| 样本 | 文本相似度 |
|---|---|
| `hello_watermark` | 0.053 |
| `ano` | 0.129 |
| `透明度文字` | 0.184 |
| `999` | 0.245 |

其余不通过项集中在路径还原（`path_unstd`、`1`）、中文字形（`AddWatermarkAnnot`、`testImageNotFound`）、
注释外观与附件等场景。**纯像素门禁对文字错位不敏感**（例如文字整体偏移约 89pt 时，文字面积占比仅 0.18%，
像素差异仍在阈值内），因此除像素门禁外必须补“文字包围盒”断言（比较提取文本的 xMin/yMin 与基准的偏差），
详见 12.5。

另有一类**必须由调用方遵守的契约**：`PDFExporter#export()` 只渲染到内存，**PDF 在 `close()` 时写出**；
漏调 `close()` 会得到 0 字节文件（回归环境中曾因 `Exporter` 工厂只调 `export()` 导致全部样本导出为空，
读取时报 `End-of-File, expected line at offset 0`）。已写入 `EXPORTER.md` 与 `scripts/README.md`。

### 12.5 文字包围盒断言（像素门禁的必要补充）

评审实测：某样本文字整体偏移约 89pt（约 31.7mm），但因文字面积仅占页面 0.18%，像素差异落在 0.5% 阈值内而“通过”。
因此**回归门禁不能只看像素**，必须同时断言文字的位置：

- 用 `PDFTextStripper` 提取实际 PDF 与基准 PDF 的文本，分别取所有字符的包围盒并集；
- 断言 `|xMin_actual − xMin_expected|`、`|yMin_actual − yMin_expected|` 均不超过阈值（建议 2pt，按实测校准）；
- 该断言与像素门禁、文本相似度三者同时满足才算通过。

实现要求：文字位置偏差属于“结构性错误”，一旦超出阈值应直接判定失败，不做白名单豁免。
