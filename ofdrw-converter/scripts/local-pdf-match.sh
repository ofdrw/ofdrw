#!/usr/bin/env bash
#
# OFD → PDF 本地回归（设计文档第 7.2.1 节、决策 22）
#
# 读取不入库的 ofd_pdf_match/（58 组）全量样本，逐样本转换并与基准 PDF 做像素 + 文本比较，
# 输出 target/local-pdf-match/report.md。**不进入 CI、不随 mvn test 执行。**
#
# 用法（任意目录执行均可）：
#   ./scripts/local-pdf-match.sh                    # 默认样本目录 <仓库>/ofd_pdf_match
#   ./scripts/local-pdf-match.sh /path/to/samples   # 指定样本目录
#   OFD_MATCH_DIR=/path/to/samples ./scripts/local-pdf-match.sh
#   ./scripts/local-pdf-match.sh --assert           # 额外断言入库样本达标（等价 -Dofd.regression.assert=true）
#     注：参数与样本目录互斥使用，--assert 时样本目录取 OFD_MATCH_DIR 或默认值
#
#   OFD_M2_REPO=/path/to/m2repo ./scripts/local-pdf-match.sh
#     # 指定 Maven 本地仓库（~/.m2 只读时必须指定，例如 <仓库>/target/m2merge）
#
# 判定：像素差异像素比例 ≤ 0.5% 且 MAE ≤ 2/255（设计决策 5，72dpi 渲染）。
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# 达标断言开关：--assert 时对入库样本断言像素/文本阈值
ASSERT_FLAG=false
if [[ "${1:-}" == "--assert" ]]; then
    ASSERT_FLAG=true
    shift
fi

# 样本目录：命令行参数 > 环境变量 > 仓库根目录下的 ofd_pdf_match/
SAMPLE_DIR="${1:-${OFD_MATCH_DIR:-$(cd "${MODULE_DIR}/.." && pwd)/ofd_pdf_match}}"
if [[ ! -d "${SAMPLE_DIR}" ]]; then
    echo "样本目录不存在: ${SAMPLE_DIR}" >&2
    echo "请先获取 ofd_pdf_match（不入库，可用 -Dofd.match.dir 或参数指定其它目录）" >&2
    exit 2
fi

# 离线优先：本地仓库缺少依赖时再联网解析
MVN_ARGS=(-q)
if mvn -o -v >/dev/null 2>&1; then
    MVN_ARGS+=(-o)
fi

# 本地仓库位置：环境变量 OFD_M2_REPO 显式指定时优先。
# 受限环境下 ~/.m2 可能只读，需改用可写仓库（例如 <仓库>/target/m2merge）。
if [[ -n "${OFD_M2_REPO:-}" ]]; then
    MVN_ARGS+=("-Dmaven.repo.local=${OFD_M2_REPO}")
fi

echo ">> 样本目录: ${SAMPLE_DIR}"
echo ">> 报告输出: ${MODULE_DIR}/target/local-pdf-match/report.md"

cd "${MODULE_DIR}"
# 只选中手动用例：全量报告需显式开启 ofd.match.full=true（默认跳过，避免误跑）
# 说明：surefire 的系统属性必须经 -DargLine 传进 fork 出来的测试 JVM，
# 直接写在 mvn 命令行上的 -D 只会进 Maven 自身；-Djava.awt.headless=true 让栅格化不依赖 X11。
exec mvn "${MVN_ARGS[@]}" test \
    -DskipTests=false \
    -Dtest='ManualPdfMatchTest' \
    -Dofd.match.dir="${SAMPLE_DIR}" \
    -Dofd.match.full=true \
    -Dofd.regression.assert="${ASSERT_FLAG}" \
    -DargLine="-Djava.awt.headless=true -Dofd.match.dir=${SAMPLE_DIR} -Dofd.match.full=true -Dofd.regression.assert=${ASSERT_FLAG}" \
    -Dsurefire.failIfNoSpecifiedTests=false
