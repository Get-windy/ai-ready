# -*- coding: utf-8 -*-
"""系统模块审计 · 装配检查
1) 列出所有含 @RestController 的包，判断是否被 AiReadyApplication.scanBasePackages 覆盖
2) 列出所有 @Mapper 所在包（@MapperScan 通配，通常全绿，仅作对照）
输出 tools/system-assembly.json
"""
import os, re, io, json, sys, collections

sys.stdout.reconfigure(encoding="utf-8")
ROOT = r"I:\AI-Ready\backend"
APP = os.path.join(ROOT, "core/api/core-api/src/main/java/cn/aiedge/AiReadyApplication.java")
OUT = r"I:\AI-Ready\tools\system-assembly.json"


def norm(pat):
    return pat.replace("*", "").rstrip(".")


def main():
    src = io.open(APP, encoding="utf-8").read()
    m = re.search(r"scanBasePackages\s*=\s*\{(.*?)\}", src, re.S)
    pkgs = re.findall(r'"([^"]+)"', m.group(1))
    pkgs = [norm(p) for p in pkgs]
    # 去掉行内注释遗留
    pkgs = [p for p in pkgs if p and p.startswith("cn.")]

    ctrls = collections.defaultdict(list)   # pkg -> [(file, class)]
    mappers = collections.defaultdict(list)
    for dp, dn, fn in os.walk(ROOT):
        dn[:] = [d for d in dn if d not in ("target", ".git")]
        for f in fn:
            if not f.endswith(".java"):
                continue
            p = os.path.join(dp, f)
            try:
                t = io.open(p, encoding="utf-8", errors="replace").read()
            except Exception:
                continue
            pkg = re.search(r"package\s+([\w\.]+);", t)
            if not pkg:
                continue
            pkg = pkg.group(1)
            if "@RestController" in t:
                ctrls[pkg].append((os.path.relpath(p, ROOT), f[:-5]))
            if "@Mapper" in t:
                mappers[pkg].append(os.path.relpath(p, ROOT))

    def covered(pkg):
        return any(pkg == p or pkg.startswith(p + ".") for p in pkgs)

    rows = []
    for pkg in sorted(ctrls):
        files = ctrls[pkg]
        rows.append({"pkg": pkg, "covered": covered(pkg), "count": len(files),
                     "classes": [c for _, c in files]})
    un = [r for r in rows if not r["covered"]]
    out = {"scan_pkgs": pkgs, "controllers": rows, "uncovered": un,
           "mapper_pkgs": {k: len(v) for k, v in sorted(mappers.items())}}
    io.open(OUT, "w", encoding="utf-8").write(json.dumps(out, ensure_ascii=False, indent=1))

    print("scanBasePackages: %d 条" % len(pkgs))
    print("有 @RestController 的包: %d，其中未覆盖: %d" % (len(rows), len(un)))
    for r in un:
        print("  ✗ %-50s %d 个: %s" % (r["pkg"], r["count"], ",".join(r["classes"])[:120]))
    print("\n系统模块相关包覆盖情况:")
    for r in rows:
        if re.search(r"(module|monitor|platform|datasource|export|devtool|storage|dict|tenant|permission|audit|scheduler|config)$|system", r["pkg"]):
            print("  %s %-50s %d" % ("✓" if r["covered"] else "✗", r["pkg"], r["count"]))


if __name__ == "__main__":
    main()
