#!/usr/bin/env bash
#
# 起「第二个后端实例」来验证代码改动 —— **不动共享实例、不重建 fat jar**。
#
# ## 为什么需要它
#
# 本仓常年有实例在 5655 跑着，而且是多个会话共用 devdb。要验一个后端改动，常规做法
# `mvn package` + 重启 5655 有三个代价：① 会打断别人的验证；② 覆盖运行中的 fat jar
# 会导致随机 ClassNotFoundException（另见记忆「运行中覆盖 fat jar」）；③ 全量重建很慢。
#
# 本脚本用 Spring Boot 的 `PropertiesLauncher` + `loader.path`，把**新编的 class 目录**
# 挂在 fat jar 之前加载 —— 只覆盖你改的那几个类，其余全走原 jar：
#
#   java -Dloader.path=<dir1>,<dir2> -cp core-api-*-exec.jar \
#        org.springframework.boot.loader.launch.PropertiesLauncher --server.port=5691
#
# ## 两个必踩的坑（本脚本已封装）
#
# 1. **`loader.path` 是【逗号】分隔**。写成分号会被当成"一个不存在的路径"，
#    **静默不生效** —— 你会以为改的代码没起作用，然后去怀疑代码本身（2026-09-26 实踩）。
# 2. **不能把整个模块的 `target/classes` 挂上去**：里面有 `mapper/*.xml`，
#    会和 jar 里同名资源重复加载，启动报
#    `Result Maps collection already contains value for ...BaseResultMap`。
#    ⇒ 只挂**含你要覆盖的那几个 class 的小目录**。
#
# 配套：`compile-one` 用 **javac 直接输出到独立目录**，绕开 IDE/ECJ 覆盖 target/classes
# 的问题（见 `tools/check-stale-classes.py` 里对残类的说明）。
#
# ## 用法
#
#   # 1) 把改动过的单个 Java 文件编到干净目录（绕开 target/classes）
#   tools/run-second-instance.sh compile <SourceFile.java> <OutDir>
#
#   # 2) 起第二实例（可挂多个覆盖目录，逗号分隔由脚本保证）
#   PORT=5691 tools/run-second-instance.sh start <OutDir> [<OutDir2> ...]
#
#   # 3) 用完停掉（只停本脚本起的那个端口，不碰 5655）
#   PORT=5691 tools/run-second-instance.sh stop
#
set -euo pipefail
cd "$(dirname "$0")/.."

JAR=$(ls -t backend/core/api/core-api/target/core-api-*-exec.jar 2>/dev/null | head -1 || true)
[ -n "$JAR" ] || { echo "找不到 core-api 的 exec jar；先构建一次" >&2; exit 1; }
PORT="${PORT:-5691}"

case "${1:-}" in
  compile)
    SRC="${2:?用法: compile <SourceFile.java> <OutDir>}"
    OUT="${3:?用法: compile <SourceFile.java> <OutDir>}"
    CP_TXT=backend/core/api/core-api/target/cp.txt
    [ -f "$CP_TXT" ] || {
      echo "==> 生成依赖 classpath（首次较慢）"
      (cd backend && ./mvnw -o -q -pl core/api/core-api dependency:build-classpath -Dmdep.outputFile=target/cp.txt)
    }
    mkdir -p "$OUT"
    # javac 的 @argfile：路径里的反斜杠会被当转义 ⇒ 统一换成正斜杠；整串 classpath 太长，
    # 直接放命令行会 "Argument list too long"。
    ARG=tool-results/_javac-args.txt
    mkdir -p tool-results
    python - "$SRC" "$OUT" "$CP_TXT" "$ARG" <<'PY'
import sys
src, out, cp_txt, arg = sys.argv[1:5]
cp = open(cp_txt, encoding='utf-8').read().strip().replace('\\', '/')
full = ('%s/target/classes;' % src.rsplit('/src/main/java/', 1)[0]) + cp
lines = ['-nowarn', '-encoding', 'UTF-8', '-d', out, '-cp', '"' + full + '"', src]
open(arg, 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
PY
    JAVAC="${JAVAC:-/c/Program Files/Java/jdk-17/bin/javac}"
    "$JAVAC" "@$ARG"
    echo "==> 已编译到 $OUT"
    echo "    自检（应为 0）: python tools/check-stale-classes.py --dir $OUT"
    ;;

  start)
    shift
    [ "$#" -ge 1 ] || { echo "用法: start <OutDir> [<OutDir2> ...]" >&2; exit 1; }
    # 逗号拼接 —— 这里就是坑 1 的封装点，别改成别的分隔符
    LOADER_PATH=$(IFS=,; echo "$*")
    LOG="logs/second-instance-$PORT-$(date +%Y%m%d-%H%M%S).log"
    mkdir -p logs
    echo "==> 起第二实例 :$PORT"
    echo "    jar         = $JAR"
    echo "    loader.path = $LOADER_PATH"
    echo "    日志        = $LOG"
    nohup java -Dloader.path="$LOADER_PATH" -cp "$JAR" \
      org.springframework.boot.loader.launch.PropertiesLauncher \
      --spring.profiles.active=dev --server.port="$PORT" > "$LOG" 2>&1 &
    echo "    pid=$!  等待就绪（最多 240s）…"
    for _ in $(seq 1 48); do
      if grep -qaE "Started .*Application in" "$LOG" 2>/dev/null; then
        echo "==> 就绪：http://localhost:$PORT/api"
        exit 0
      fi
      if grep -qaE "Application run failed" "$LOG" 2>/dev/null; then
        echo "!! 启动失败：" >&2
        grep -a -A 3 "Application run failed" "$LOG" | head -8 >&2
        grep -a "Caused by" "$LOG" | head -3 >&2
        exit 1
      fi
      sleep 5
    done
    echo "!! 240s 未就绪，看 $LOG" >&2
    exit 1
    ;;

  stop)
    PIDS=$(netstat -ano | grep ":$PORT " | grep LISTENING | awk '{print $NF}' | sort -u || true)
    [ -n "$PIDS" ] || { echo ":$PORT 没有监听进程"; exit 0; }
    for p in $PIDS; do
      echo "==> 停止 :$PORT 的 pid=$p"
      taskkill //PID "$p" //F >/dev/null 2>&1 || true
    done
    ;;

  *)
    sed -n '2,45p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
    ;;
esac
