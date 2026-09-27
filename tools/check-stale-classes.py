# -*- coding: utf-8 -*-
"""嵌套 fat jar / target-classes 里的 **ECJ 残缺类** 扫描闸门（只读）。

## 为什么要它

本仓反复踩「`mvn compile` 报成功、跑起来 NoClassDefFoundError / No qualifying bean」：
IDE（Eclipse/ECJ）会把**带编译错误的 .class** 写进 `target/classes` —— 方法签名退化成 `()LResult;`、
常量池里塞 `Unresolved compilation problem: ...`。这些 class 的时间戳比源码新，
Maven 增量判定认为"没变化"⇒ 跳过重编 ⇒ **错误被静默打进 fat jar**。
症状还可能是 `NoClassDefFoundError: Cell`（ECJ 的错误支撑类）、`No qualifying bean of type`（注解读不出）。

记忆里只有「怎么事后判残类」的口诀，没有闸门 —— 本脚本把它变成**构建失败**。

## 用法

    python tools/check-stale-classes.py                      # 自动找 core-api 的 exec jar
    python tools/check-stale-classes.py <jar 路径>
    python tools/check-stale-classes.py --dir <classes 目录> # 只扫某个 target/classes

命中即 **退出码 1**，并打印「哪个嵌套 jar 的哪个 class 是残类」。
处置：`cd backend && ./mvnw -pl <模块> clean install -DskipTests`（clean 是关键），
或直接 `javac` 输出到独立目录（`target/classes` 已被 IDE 覆盖时不可信）。
"""
import argparse
import glob
import os
import sys
import zipfile

# ⚠️ 2026-09-26 修复：Windows 控制台默认 GBK，而本脚本的输出含 emoji（✅ / ❌ / ⚠️），
#    print 时会抛 UnicodeEncodeError 并以**非 0 退出** —— 调用方（tools/build-backend.sh）
#    据此误判成「产物里有 ECJ 残缺类，拒绝启动」，即使实际检查是**通过**的
#    （实测：成功分支的 `print("✅ 未发现 ECJ 残缺类")` 崩在 GBK 编码上）。
#    这里强制 UTF-8 输出，让退出码只反映检查结果本身。
try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

# ECJ 残类的标志串（常量池里必有其一）
MARKERS = (b"Unresolved compilation", b"org/eclipse/jdt/internal/compiler/problem")

MAX_REPORT = 40


def scan_bytes(name, data, hits):
    for m in MARKERS:
        if m in data:
            hits.append(name)
            return True
    return False


def scan_dir(root, hits, prefix=""):
    n = 0
    for dirpath, _dirnames, filenames in os.walk(root):
        for fn in filenames:
            if not fn.endswith(".class"):
                continue
            p = os.path.join(dirpath, fn)
            n += 1
            with open(p, "rb") as f:
                scan_bytes(prefix + os.path.relpath(p, root).replace(os.sep, "/"), f.read(), hits)
    return n


def scan_jar(path, hits):
    """扫 fat jar：BOOT-INF/classes/** 与每个 BOOT-INF/lib/*.jar 里的 *.class。

    返回 (扫描数, 错误信息)。错误信息非空表示 jar 读不了 —— 最常见的原因是
    **另一个会话正在重建它**（写一半），此时读到 EOF / BadZipFile。
    """
    import io
    scanned = 0
    try:
        with zipfile.ZipFile(path) as z:
            for e in [n for n in z.namelist() if n.endswith(".class")]:
                scanned += 1
                scan_bytes(e, z.read(e), hits)
            libs = [n for n in z.namelist() if n.startswith("BOOT-INF/lib/") and n.endswith(".jar")]
            for lib in libs:
                try:
                    with zipfile.ZipFile(io.BytesIO(z.read(lib))) as nz:
                        for e in nz.namelist():
                            if e.endswith(".class"):
                                scanned += 1
                                scan_bytes(lib.replace("BOOT-INF/lib/", "") + "!" + e, nz.read(e), hits)
                except (zipfile.BadZipFile, EOFError, OSError):
                    hits.append(lib + " (嵌套 jar 已损坏/正在被写入，无法读取)")
    except (zipfile.BadZipFile, EOFError, OSError) as ex:
        return scanned, f"{type(ex).__name__}: {ex}"
    return scanned, None


def default_jar():
    cands = glob.glob("backend/core/api/core-api/target/core-api-*-exec.jar")
    return max(cands, key=os.path.getmtime) if cands else None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("jar", nargs="?", help="exec fat jar 路径")
    ap.add_argument("--dir", action="append", default=[], help="要扫描的 classes 目录（可多次）")
    ap.add_argument("--no-jar", action="store_true",
                    help="只扫 --dir（给「验一个刚编出来的目录」这种定点检查用，省掉 10 万级 class 的全量扫描）")
    args = ap.parse_args()

    hits = []
    total = 0

    for d in args.dir:
        if os.path.isdir(d):
            n = scan_dir(d, hits, prefix=d.replace(os.sep, "/") + "!/")
            total += n
            print(f"[扫目录] {d}  ({n} 个 class)")
        else:
            print(f"⚠️ 目录不存在，跳过: {d}")

    # 指定了 --dir 且没显式传 jar ⇒ 当作定点检查，不去扫 fat jar
    #（否则每次自检都要解压 10 万级 class，且并行会话正在重建 jar 时会读失败）
    want_jar = not args.no_jar and not (args.dir and not args.jar)
    jar = args.jar or (default_jar() if want_jar else None)
    if jar:
        if not os.path.isfile(jar):
            print(f"❌ 找不到 jar: {jar}")
            return 2
        print(f"[扫 fat jar] {jar}  ({os.path.getsize(jar) / 1048576:.0f} MB)")
        n, err = scan_jar(jar, hits)
        total += n
        if err:
            print(f"❌ fat jar 读不了（多半是**另一个会话正在重建它**）：{err}")
            print("   等构建结束再跑一次；本次结论不可信。")
            return 2
    elif want_jar:
        print("⚠️ 没找到 core-api 的 exec jar（跳过）。传路径或先构建。")

    print(f"共扫 {total} 个 .class")
    if hits:
        print(f"\n❌ 发现 {len(hits)} 个 ECJ 残缺类 —— 这份产物**不可用于启动**：")
        for h in hits[:MAX_REPORT]:
            print("   -", h)
        if len(hits) > MAX_REPORT:
            print(f"   ...（还有 {len(hits) - MAX_REPORT} 个）")
        print("\n处置：cd backend && ./mvnw -pl <所属模块> clean install -DskipTests")
        print("      （clean 是关键：IDE 写的残类时间戳更新，增量编译不会重编它）")
        return 1
    print("✅ 未发现 ECJ 残缺类")
    return 0


if __name__ == "__main__":
    sys.exit(main())
