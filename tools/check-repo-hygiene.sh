#!/usr/bin/env bash
# 仓库卫生门禁：禁止「运行产物」进入版本控制。
#
# 为什么需要：本项目历史上出现过 7.2GB 的 backend_new.log、3.8MB 的 pnpm 安装包、
# 299MB 的一堆 *.log、34 个 Python 字节码、Playwright 报告、*.bak 备份文件被提交。
# 这类东西一旦入库就会永久留在 git 历史里（删掉也还在），而且会持续污染 git status。
#
# 用法：bash tools/check-repo-hygiene.sh
# 本地可在提交前手动跑，CI 在 .github/workflows/ci-optimized.yml 的 repo-hygiene 作业里强制跑。
#
# 注：只检查「已被 git 跟踪」的文件。工作区里未跟踪的日志不在管辖范围（它们至少不会进历史）。

set -uo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || {
  echo "❌ 当前目录不在 git 仓库内，无法检查"
  exit 2
}
cd "$ROOT"

# 运行产物/构建产物：命中即违规
PATTERN='\.(log|bak|tmp|pyc|pyo|swp|orig|rej)$|(^|/)__pycache__/|\.tsbuildinfo$|vite\.config\.ts\.timestamp-|(^|/)node_modules/|(^|/)dist/|(^|/)playwright-report/|(^|/)test-results/|\.flattened-pom\.xml$|\.DS_Store$'

# 单个文件大小上限（MB）。当前仓库最大值是 4.43MB 的测试基准数据，留出余量。
MAX_MB=5

fail=0

violations="$(git ls-files | grep -E "$PATTERN" || true)"
if [ -n "$violations" ]; then
  echo "❌ 以下运行产物已被 git 跟踪："
  echo "$violations" | sed 's/^/   /'
  fail=1
fi

# 大文件检查读的是已提交内容（git ls-tree），不做全盘 stat，秒级返回
big="$(git ls-tree -r -l HEAD 2>/dev/null \
  | awk -v limit=$((MAX_MB * 1048576)) '$4 > limit {printf "   %.2f MB  %s\n", $4 / 1048576, $5}')"
if [ -n "$big" ]; then
  echo "❌ 以下已提交文件超过 ${MAX_MB}MB："
  echo "$big"
  fail=1
fi

if [ "$fail" -ne 0 ]; then
  echo
  echo "处理方式："
  echo "  · 产物不入库 → 加 .gitignore 规则 + git rm --cached <path>（保留工作区文件）"
  echo "  · 确需入库的大文件 → 先把「为什么必须入库」写进 CLEANUP_AUDIT_REPORT.md，再调整本脚本阈值"
  exit 1
fi

echo "✅ 仓库卫生检查通过：无运行产物入库、无超过 ${MAX_MB}MB 的大文件"
