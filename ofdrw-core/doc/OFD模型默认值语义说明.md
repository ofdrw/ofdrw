# OFD 模型默认值语义说明

> 适用模块：`ofdrw-core`（核心模型）、`ofdrw-converter`（PDF 渲染链）
> 依据：GB/T 33190-2016《电子文件存储与交换格式 版式文档》8.2.1（绘制参数就近原则）、8.3.2（颜色通道值）
> 引入版本：2.5.1

## 1. 一句话结论

`ofdrw-core` 中凡是“属性缺失时返回规范默认值”的访问器，**只能用来读一个值，不能用来判断属性是否被显式声明**。
需要判断“是否声明”时，请使用 2.5.1 新增的 `xxxExplicit()` 访问器（缺失返回 `null`）或直接读取属性文本；
规范默认值只允许在渲染链的**最末端兜底一次**。

## 2. 现象

在 `ofdrw-core` 中，大量访问器在属性缺失（`null` 或空串）时直接返回规范默认值，例如：

- `CT_GraphicUnit#getCap()` → `Butt`
- `CT_GraphicUnit#getJoin()` → `Miter`
- `CT_GraphicUnit#getAlpha()` → `255`
- `CT_DrawParam#getLineWidth()` → `0.353`（mm）
- `org.ofdrw.core.image.Border#getLineWidth()` → `0.353`（mm）
- `CT_Color#getAlpha()` → `255`
- `CT_ColorSpace#getBitsPerComponent()` → `BIT_1`

对“读取一个值”的场景这是方便的；但这些默认值同时抹掉了“未定义”这一状态。

### 2.1 与规范冲突的具体例子：`BitsPerComponent`

`BitsPerComponent` 的枚举注释写的是“默认值取值为 **8**”，GB/T 33190 8.3.2 也要求**未声明位宽按 8 位处理**，
但 `getInstance(null)` / `getInstance("")` 实际返回 **`BIT_1`**。三者互不一致：

```java
// 行为事实（2.5.1 实测）：属性缺失时返回 BIT_1
new CT_ColorSpace().getBitsPerComponent()          // → BitsPerComponent.BIT_1
// 对照：显式声明位宽时行为正确
// 影响：若把“未声明”当成 1 位，颜色通道值 "200" 会被归一化为 200/1 再截断为 1.0（全满通道），
//       而按规范应视为 8 位，得到 200/255 ≈ 0.784。
```

> **这是行为事实，不是要求你“顺手修掉”**：`BitsPerComponent.getInstance(null)` 的返回值属于既有公开行为，
> 修改它会破坏向下兼容（本项目的硬要求是既有 public 行为不变）。
> 正确处置是**新增** Explicit 访问器，由渲染侧按规范语义取值——即 2.5.1 的做法。

## 3. 为什么必须区分“未声明”

GB/T 33190 8.2.1 的就近原则规定生效值顺序为：

```
图元显式属性 > 图元 DrawParam > 图层 DrawParam > Relative 继承链（递归，最大深度 32）> 规范默认值
```

若图元上没有写 `Cap`，却从 `getCap()` 读到 `Butt`，这个 `Butt` 会被当成“图元显式声明”，
**覆盖掉 DrawParam 或 Relative 链上父级真正声明的值**，继承链由此被打断——表现形式是“样式莫名回落到默认值”。

因此“未定义字段不得覆盖继承值”是硬性要求，也就必须有 `null` 语义。

## 4. 处置：5 组、7 个 Explicit 访问器（自 2.5.1 起）

| # | 方法 | 既有访问器缺失时返回 | `xxxExplicit()` 缺失时返回 |
|---|---|---|---|
| 1 | `CT_GraphicUnit#getCapExplicit()` | `Butt` | `null` |
| 2 | `CT_GraphicUnit#getJoinExplicit()` | `Miter` | `null` |
| 3 | `CT_GraphicUnit#getAlphaExplicit()` | `255` | `null` |
| 4 | `CT_DrawParam#getLineWidthExplicit()` | `0.353` | `null` |
| 5 | `CT_ColorSpace#getBitsPerComponentExplicit()` | `BIT_1`（见 2.1，与 8.3.2 的 8 位要求不一致） | `null` |
| 6 | `org.ofdrw.core.image.Border#getLineWidthExplicit()` | `0.353` | `null` |
| 7 | `CT_Color#getAlphaExplicit()` | `255` | `null` |

约定：

- 显式访问器与既有访问器读取**同一个属性**（`attributeValue`），只是“缺失”时的返回值不同，
  因此**不改变任何既有行为**，属于纯新增、向后兼容。
- **显式写成规范默认值也算“已声明”**：例如 `Cap="Butt"`、`Alpha="255"`、`LineWidth="0.353"`，
  两种访问器都返回该值，Explicit 不会因为“值等于默认值”而返回 `null`。
- 规范默认值只在渲染链**最末端兜底一次**：`ofdrw-converter` 的 `GraphicsStateResolver`、`ColorResolver`、
  `ImageRendererImpl`（边框）在整条继承链解析完毕后，对仍未定义的字段取默认值。

### 4.1 为什么是两个专用场景也需要

- **`Border#getLineWidth`**：边框“线宽为 0 表示不绘制”，若把“未声明”读成 `0.353`，就无法区分
  “未声明（应取默认线宽或按容器继承）”与“显式写 0（明确不画边框）”。
- **`CT_Color#getAlpha`**：渲染侧要计算“图元 Alpha × 颜色 Alpha”。若把“未设置”当成“显式 255”，
  乘法语义就退化为“颜色永远不透明”，无法再叠加别的透明度来源。

## 5. 已实际修复的缺陷（2.5.1 重构期间）

| # | 位置 | 缺陷 | 修复 |
|---|---|---|---|
| 1 | `pdf/render/GraphicsStateResolver`（`capValue` / `joinValue`） | 直接调用 `getInstance(attrText)`，把“属性缺失”误判为显式的 `Butt`/`Miter`，覆盖 Relative 链上父级的值 | 改为先判空，缺失时继续沿继承链查找 |
| 2 | `pdf/render/ColorResolver#maxChannelValue` | 因 `getBitsPerComponent()` 返回 `BIT_1`，把 `"200"` 算成 `200/1` 再截断为 `1.0` | 改用 `getBitsPerComponentExplicit()`，未声明按 8 位（255）归一化 |
| 3 | `pdf/render/ImageRendererImpl`（边框） | “未声明线宽”被当作 `0.353` 已声明，边框该不该绘制判断错误 | 改用 `Border#getLineWidthExplicit()`，只对显式值判断 |
| 4 | `pdf/render/PathRenderer` + `pdf/render/ColorResolver`（透明度） | 用 `getAlpha()`（缺失返回 255）喂给 DrawParam 链，把“未定义”当“显式不透明”，覆盖继承 Alpha；颜色 Alpha 同样失真 | 图元侧改用 `CT_GraphicUnit#getAlphaExplicit()`，颜色侧改用 `CT_Color#getAlphaExplicit()`，再相乘得到有效不透明度 |

## 6. 验证

| 测试 | 覆盖 |
|---|---|
| `ofdrw-core/.../CT_GraphicUnitExplicitTest`（9 项） | 7 个 Explicit 访问器：既有访问器返回默认值、Explicit 返回 `null`、**显式写成默认值仍识别为已定义** |
| `ofdrw-converter/.../GraphicsStatePrecedenceTest`（7 项） | 优先级链：图元属性 > DrawParam > Relative 链（≤32）> 规范默认值；含“图元未写 Cap、DrawParam 未写、Relative 链写 `Round` → 必须为 `Round`”、“显式写 `Butt` 仍算已声明”、“链深 >32 截断不抛异常” |

复跑方式（注意根 `pom.xml` 中 `skipTests` 默认 `true`，必须显式关闭）：

```bash
mvn -o -Dmaven.repo.local=<仓库>/target/m2merge -pl ofdrw-core test -DskipTests=false -Dgpg.skip=true \
    -Dtest='CT_GraphicUnitExplicitTest' -Dsurefire.failIfNoSpecifiedTests=false \
    -DargLine="-Djava.awt.headless=true"
```

## 7. 给后来者的检查清单

1. 新增读取某个 OFD 可选属性的代码时，先问一句：**“我需要区分未声明吗？”**
   - 只是要一个值 → 用既有访问器；
   - 要参与继承/优先级判断 → 用 `xxxExplicit()`，或直接读 `attributeValue`。
2. 发现某访问器的默认值与规范不符时：**不要改既有访问器的返回值**，新增 `xxxExplicit()` 并在渲染侧兜底。
3. 默认值只在一个地方兜底（继承链末端），不要在各渲染器里各写一份。
