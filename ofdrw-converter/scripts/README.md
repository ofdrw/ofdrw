# OFD → PDF 本地手动回归脚本

本目录存放**本地手动执行**的回归脚本，不属于构建流程，也不进入 CI（设计文档第 7.2.1 节、决策 22）。

## local-pdf-match.sh

读取不入库的 `ofd_pdf_match/`（58 组 `<样本名>.ofd` + 基准 `<样本名>.pdf`），
逐样本转换为 PDF 后比较像素与文本，输出 Markdown 报告。

```bash
# 在 ofdrw-converter 模块下
./scripts/local-pdf-match.sh

# 指定样本目录
./scripts/local-pdf-match.sh /data/ofd_pdf_match
OFD_MATCH_DIR=/data/ofd_pdf_match ./scripts/local-pdf-match.sh
```

输出位置：`ofdrw-converter/target/local-pdf-match/`

| 路径 | 内容 |
|---|---|
| `report.md` | 逐样本指标表：页数、差异像素比例、MAE、文本相似度、结论 |
| `render/<样本名>/*.png` | 基准图 `-expected`、实际图 `-actual`、差异图 `-diff`（差异像素标红） |
| `render/<样本名>/*.pdf` | 本库的转换结果，便于单独复核 |

指标口径与入库样本测试完全一致（72 dpi，差异像素 ≤ 0.5% 且 MAE ≤ 2/255，文本相似度 ≥ 0.99），
实现见 `ofdrw-converter/src/test/java/org/ofdrw/converter/regression/`。

### 说明

- 脚本本质是用 Maven 选中 `org.ofdrw.converter.regression.ManualPdfMatchTest` 运行，并带上
  `-Dofd.match.full=true`；`ManualPdfMatchTest` 在 `mvn test` 中**默认跳过**全量报告（`Assumptions`），
  即使被 `-Dtest` 意外选中也只跑轻量的框架冒烟用例。
- 入库样本的达标断言默认关闭（`-Dofd.regression.assert=false`），本地验收追加 `--assert` 或
  `-Dofd.regression.assert=true` 开启；新渲染管线接通后改为默认开启作为 CI 门禁。
- `ofd_pdf_match/` 与 `src_material/` 已在 `.gitignore` 中，禁止提交。
- 2.5.1 起脚本与入库测试均以 `PDFExporter`（新渲染管线）作为被验证实现。
- 脚本内部已把 `-Djava.awt.headless=true` 与 `-Dofd.match.*` 等系统属性通过 `argLine` 传入 surefire 的 fork JVM
  （直接写在 `mvn` 命令行的 `-D` 属性不会自动传到 fork 进程）。
- `PDFExporter` 的 `export()` 只渲染到内存，**PDF 在 `close()` 时才写出**；自定义回归代码必须 `try (…) { export(); }`，
  否则会得到 0 字节文件并报 `End-of-File, expected line at offset 0`。
- 本机 `~/.m2` 只读或缺少依赖时，用环境变量指定 Maven 本地仓库：

```bash
OFD_M2_REPO=<仓库>/target/m2merge ./ofdrw-converter/scripts/local-pdf-match.sh --assert
```
