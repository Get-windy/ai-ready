#!/usr/bin/env bash
#
# 后端构建 + 重启（一步到位）
#
# ⚠️ 为什么固定用 `clean package`、不要图快改成 `package`：
#   Maven **不会删除「已删源文件」在 target/ 里的旧产物**（MNG-5039，2011 年至今的设计行为，
#   官方明确说不是 bug）。后果有两次实踩：
#     ① 残留的 *Mapper.xml 会被打进 fat jar → 启动报
#        `Failed to parse mapping resource: ...DataPermissionMapper.xml`；
#     ② 残留的 @Aspect/@Component `.class` 会被 Spring 组件扫描**继续加载**，
#        让「已删除」变成假象（切点仍指向已删类 → [Xlint:invalidAbsoluteTypeName] 启动失败）。
#   同理：**切换分支后也必须走本脚本**，因为 target/ 被 .gitignore，旧产物会跨分支残留。
#
# 用法：
#   tools/build-backend.sh                  # 停服 → clean package → 启动 → 等就绪
#   tools/build-backend.sh --build-only     # 只做 clean package，不动服务
#   tools/build-backend.sh --with-tests     # 构建时跑测试（默认 -DskipTests）
#   PORT=5656 tools/build-backend.sh        # 指定端口做就绪探测
#
# ⚠️ 版本号（`VERSION`）是**手写常量**，与 backend/pom.xml 的 `<revision>` 必须一致。
#    bump 版本时两处一起改，否则（见下）停服会失效。
#
# ⚠️⚠️ **bump 版本前必须先停掉旧版本实例**：`stop_backend` 是按 jar 名匹配进程的，
#    新版本号一改，**正在跑的旧版本实例就匹配不到了** ⇒ 不会被 kill ⇒ 端口仍被占 ⇒
#    clean 删不掉旧 jar、新实例也起不来。顺序：先 `taskkill` 旧实例，再改版本号并构建。
#    （2026-09-21 bump 0.3.22 时实测确认过这一串症状。）
#
# ⚠️⚠️⚠️ **bump 版本后必须做一次【全仓】构建，不能只跑本脚本**：
#    所有模块的版本都来自 `${revision}`，版本号一变，本地 Maven 仓库里那批
#    `xxx:0.3.21` 的 jar 就**全部对不上** ⇒ 只构建核心模块时，core-api 会因为
#    「找不到 cn.aiedge:core-agent:jar:0.3.22 等 20 个依赖」直接失败。
#    （2026-09-21 bump 0.3.22 实测：确实报了 20 个 absent。）
#    正确顺序：
#      cd backend && ./mvnw clean install -DskipTests -B     # 全仓（不写 -pl）
#      bash tools/build-backend.sh                            # 之后本脚本即可正常工作
#
set -euo pipefail

cd "$(dirname "$0")/.."

PORT="${PORT:-5655}"
VERSION="0.3.23"
JAR="backend/core/api/core-api/target/core-api-${VERSION}-exec.jar"
# 默认只构建「核心模块 + 聚合模块」，其余业务模块从本地仓库取，避免全仓重建。
# ⚠️ 改了别的业务模块（如 erp/erp-finance）时必须显式带上它，否则 core-api 打包会从
#    本地仓库取到**旧版**那个模块的 jar：
#      MODULES="erp/erp-finance,core/base/core-base,core/platform/core-platform,core/api/core-api" \
#        tools/build-backend.sh
MODULES="${MODULES:-core/base/core-base,core/platform/core-platform,core/api/core-api}"
SKIP_TESTS="-DskipTests"
BUILD_ONLY=0

for arg in "$@"; do
  case "$arg" in
    --build-only) BUILD_ONLY=1 ;;
    --with-tests) SKIP_TESTS="" ;;
    *) echo "未知参数: $arg" >&2; exit 2 ;;
  esac
done

# ── 1. 找出正在跑本 jar 的进程（只认这一个 jar；别动 tool-results 里其它会话的副本）──
# 注意返回**全部** PID：历史上出现过两个实例同时持有同一 jar 的情况，只 kill 第一个会导致 clean 失败。
#
# ⚠️ 2026-09-21 修：末尾必须 `|| true`。
#   本脚本是 `set -euo pipefail`，而"没有实例在跑"时 `grep -E '^[0-9]+$'` 无匹配返回 1，
#     pipeline 随之返回 1 ⇒ 赋值语句失败 ⇒ `set -e` **立刻退出**，且因为退出发生在
#     任何 echo 之前，现场表现是**完全静默地 exit 1**（零输出），极易被误判成"构建卡住/环境坏了"。
#     实测触发路径很普通：上一次启动失败（迁移报错）之后本来就没有实例在跑，
#     于是再跑本脚本就什么都看不到。加 `|| true` 后，空结果会走到下面的
#     "==> 没有正在运行的后端实例" 分支。
find_backend_pids() {
  powershell -NoProfile -Command \
    "(Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*target/core-api-${VERSION}-exec.jar*' }).ProcessId" \
    2>/dev/null | tr -d '\r' | grep -E '^[0-9]+$' || true
}

stop_backend() {
  local pids
  pids="$(find_backend_pids)"
  if [ -n "$pids" ]; then
    for pid in $pids; do
      echo "==> 停止后端实例 PID=$pid"
      taskkill //F //PID "$pid" >/dev/null 2>&1 || true
    done
    # 等端口真正释放，避免下一步启动撞端口
    for _ in $(seq 1 15); do
      if ! netstat -ano 2>/dev/null | grep -q "LISTENING.*:$PORT "; then
        break
      fi
      sleep 1
    done
    # ⚠️ Windows 上进程退出后**文件句柄不会立刻释放**，此处的等待必须有：
    #    否则 clean 会直接失败 "Failed to delete ...core-api-${VERSION}-exec.jar"。
    # ⚠️⚠️ 更隐蔽的是：这个循环**绝对不能**写成 `[ ! -f "$JAR" ] && break` ——
    #    条件为假时整条语句返回非零，在 `set -e` 下会让脚本**静默退出**（本脚本踩过两次，
    #    表现是"只打印了'停止后端实例'就没下文了"）。必须用 if 写法。
    for _ in $(seq 1 30); do
      if [ ! -f "$JAR" ] || rm -f "$JAR" 2>/dev/null; then
        break
      fi
      sleep 1
    done
    # 仍占用就**快速失败**：带着占用去 clean 只会换来一个难懂的 Maven 报错
    if [ -f "$JAR" ]; then
      echo "!! $JAR 仍被其它进程占用（可能是另一个会话启动的实例）。" >&2
      echo "   请先结束占用进程再重试；当前已中止，未做任何构建。" >&2
      exit 1
    fi
  else
    echo "==> 没有正在运行的后端实例"
  fi
}

# ── 2. 构建（必须 clean）──
build() {
  echo "==> clean install（$MODULES）"
  # shellcheck disable=SC2086
  # 用 install 而非 package：被改动的模块要装进本地仓库，否则 core-api 打包时
  # 从仓库取到的仍是旧 jar（多模块改动的经典坑）。
  # mvnw 在 backend/ 下；构建完回到仓库根，保证启动时 cwd 与生产一致。
  (cd backend && ./mvnw -pl "$MODULES" clean install $SKIP_TESTS -B)
  [ -f "$JAR" ] || { echo "构建结束但找不到 $JAR" >&2; exit 1; }
  echo "==> 产物: $JAR ($(date -r "$JAR" '+%Y-%m-%d %H:%M:%S'))"
}

# ── 3. 启动并等就绪 ──
start_backend() {
  local log="logs/backend-$(date +%Y%m%d-%H%M%S).log"
  mkdir -p logs
  echo "==> 启动（日志: $log）"
  nohup java -jar "$JAR" > "$log" 2>&1 &
  echo "    等待就绪（最多 240s）…"
  for _ in $(seq 1 48); do
    if grep -qaE "Started .*Application in" "$log" 2>/dev/null; then
      echo "==> 启动成功"
      grep -aE "Started .*Application in|数据权限拦截器已挂载" "$log" | tail -2
      return 0
    fi
    if grep -qaE "Application run failed" "$log" 2>/dev/null; then
      echo "!! 启动失败，原因：" >&2
      grep -a -A 3 "Application run failed" "$log" | head -6 >&2
      grep -a "Caused by" "$log" | head -3 >&2
      return 1
    fi
    sleep 5
  done
  echo "!! 240s 内未就绪，请查看 $log" >&2
  return 1
}

if [ "$BUILD_ONLY" -eq 1 ]; then
  build
  exit 0
fi

stop_backend
build
start_backend
