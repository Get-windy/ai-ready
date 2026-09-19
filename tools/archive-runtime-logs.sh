#!/usr/bin/env bash
# 运行产物归档：把工作区里那些「不该入库、又不舍得直接删」的产物打包到仓库外。
#
# 为什么不在仓库内建 archive 目录：那样只是换个地方脏，还会被下一次 git add 卷进去。
# 为什么默认 dry-run：这些日志可能正在被排查使用（历史上 backend/*.log 累计 299MB），
# 先看清楚要动什么，再决定。
#
# 用法：
#   bash tools/archive-runtime-logs.sh                 # 只打印清单与体积（默认，不动文件）
#   bash tools/archive-runtime-logs.sh --apply         # 打包到 $HOME 下，校验可解压后删除原文件
#   bash tools/archive-runtime-logs.sh --apply --keep   # 打包但保留原文件
#
# 归档位置：$HOME/ai-ready-artifacts/<时间戳>.tar.gz（仓库外，不会被 git 看见）

set -uo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "❌ 不在 git 仓库内"; exit 2; }
cd "$ROOT"

APPLY=0
KEEP=0
for arg in "$@"; do
  case "$arg" in
    --apply) APPLY=1 ;;
    --keep) KEEP=1 ;;
    -h|--help) sed -n '2,16p' "$0"; exit 0 ;;
    *) echo "未知参数：$arg"; exit 2 ;;
  esac
done

# 待归档清单：只收「未跟踪 / 已忽略」的运行产物，已跟踪的一律不碰（那属于版本库决策）
# 注意 node_modules/、dist/、uploads/ 这类可再生目录**不在**归档范围（列表里也不匹配），
# 它们要么可重装，要么是业务数据，不该混进日志归档里。
collect() {
  # stderr 丢掉：pnpm 的 node_modules 里存在一批坏软链，git 会刷屏式告警，与本脚本无关
  git status --porcelain --ignored 2>/dev/null | awk '$1=="??" || $1=="!!" {sub(/^[^ ]+ +/, ""); print}' | while read -r p; do
    # 明确排除：
    #   node_modules / dist / uploads —— 可再生或属于业务数据
    #   tool-results/               —— 对标文档把它当**证据**引用（如 tool-results/ql361/pages/*.json），
    #                                  删除会打断文档引用，故不进归档删除范围
    case "$p" in
      *node_modules*|*"/dist/"*|*"/uploads/"*|tool-results*|*"/tool-results/"*) continue ;;
      *.log|*.err|*.tmp|*.bak|*__pycache__*|*atcode*|playwright-report/*|test-results/*|.._tool-results_*)
        [ -e "$p" ] && printf '%s\n' "$p" ;;
    esac
  done
}

mapfile -t ITEMS < <(collect | sort -u)

if [ "${#ITEMS[@]}" -eq 0 ]; then
  echo "✅ 没有需要归档的运行产物"
  exit 0
fi

echo "待归档 ${#ITEMS[@]} 项，体积合计："
du -ch "${ITEMS[@]}" 2>/dev/null | tail -1 | sed 's/^/   /'
echo "清单（前 30 项）："
printf '   %s\n' "${ITEMS[@]:0:30}"
[ "${#ITEMS[@]}" -gt 30 ] && echo "   ... 其余 $(( ${#ITEMS[@]} - 30 )) 项"

if [ "$APPLY" -ne 1 ]; then
  echo
  echo "（当前是 dry-run，未改动任何文件。加 --apply 才会打包并删除）"
  exit 0
fi

DEST="$HOME/ai-ready-artifacts"
mkdir -p "$DEST"
STAMP="$(date +%Y%m%d-%H%M%S)"
ARCHIVE="$DEST/artifacts-$STAMP.tar.gz"

if [ -e "$ARCHIVE" ]; then
  echo "❌ 归档文件已存在，拒绝覆盖：$ARCHIVE"
  exit 1
fi

echo
echo "打包 → $ARCHIVE"
tar -czf "$ARCHIVE" "${ITEMS[@]}" || { echo "❌ 打包失败，未删除任何文件"; exit 1; }

# 校验：能列出内容且非空，才算归档成功
if ! tar -tzf "$ARCHIVE" >/dev/null 2>&1; then
  echo "❌ 归档校验失败（无法解压），未删除任何文件：$ARCHIVE"
  exit 1
fi
echo "✅ 归档校验通过：$(du -h "$ARCHIVE" | cut -f1)"

if [ "$KEEP" -eq 1 ]; then
  echo "（--keep：保留原文件，仅完成归档）"
  exit 0
fi

printf '%s\n' "${ITEMS[@]}" | while read -r p; do rm -rf -- "$p"; done
# 清理因此变空的目录
find . -type d -empty -not -path './.git/*' -delete 2>/dev/null || true
echo "✅ 已删除归档过的原文件；如需恢复：tar -xzf $ARCHIVE"
