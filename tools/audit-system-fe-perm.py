# -*- coding: utf-8 -*-
"""系统模块审计 · 前端权限码对账
提取 views/{admin,system} 下的 v-permission / hasPermission 权限码（含页内 PERM 常量），
与 sys_permission 库比对，并列出库中不存在的码。
输出 tools/system-fe-perm.json
"""
import os, re, io, json, sys, glob

sys.stdout.reconfigure(encoding="utf-8")
ROOT = r"I:\AI-Ready"
VIEWS = os.path.join(ROOT, "frontend/apps/pc-admin/src/views")
DB = json.load(io.open(os.path.join(ROOT, "tools/system-db.json"), encoding="utf-8"))
OUT = os.path.join(ROOT, "tools/system-fe-perm.json")

ATTR = re.compile(r"""v-permission\s*=\s*(["'])([\s\S]{0,100}?)\1""")
EXPR1 = re.compile(r"""^\s*['"]?('?[A-Za-z0-9_:\-]+'?)['"]?\s*$""")
HAS = re.compile(r"""hasPermission\s*\(\s*['"]([^'"]+)['"]""")
CONST = re.compile(r"""const\s+PERM\s*=\s*\{(.*?)\}""", re.S)
KV = re.compile(r"""(\w+)\s*:\s*['"]([^'"]+)['"]""")


def main():
    db_codes = set(r["permission_code"] for r in DB["perm_all"])
    rows = []
    for pat in ("admin/**/*.vue", "system/**/*.vue"):
        for f in glob.glob(os.path.join(VIEWS, pat.replace("/", os.sep)), recursive=True):
            t = io.open(f, encoding="utf-8", errors="replace").read()
            rel = os.path.relpath(f, ROOT).replace("\\", "/")
            mm = CONST.search(t)
            perms = dict(KV.findall(mm.group(1))) if mm else {}
            codes = []
            for m in ATTR.finditer(t):
                raw = m.group(2).strip().strip("'\"")
                ln = t[:m.start()].count("\n") + 1
                if "." in raw and ":" not in raw:          # PERM.CREATE 形式 → 查页内常量
                    key = raw.split(".")[-1]
                    if key in perms:
                        codes.append((perms[key], ln, "PERM." + key))
                elif ":" in raw:                            # 字面权限码
                    codes.append((raw, ln, "literal"))
            for m in HAS.finditer(t):
                codes.append((m.group(1), t[:m.start()].count("\n") + 1, "hasPermission"))
            for c, ln, src in codes:
                rows.append({"file": rel, "line": ln, "code": c, "src": src, "in_db": c in db_codes})
    io.open(OUT, "w", encoding="utf-8").write(json.dumps(rows, ensure_ascii=False, indent=1))

    import collections
    by = collections.defaultdict(list)
    for r in rows:
        by[r["file"]].append(r)
    print("前端权限码引用点 %d 处，涉及 %d 个文件" % (len(rows), len(by)))
    uniq = sorted(set(r["code"] for r in rows))
    print("去重后 %d 个码；库中不存在 %d 个" % (len(uniq), len([c for c in uniq if c not in db_codes])))
    miss = [c for c in uniq if c not in db_codes]
    if miss:
        print("\n❌ 前端引用但库中不存在（该按钮对非超管永不显示/永远 403）:")
        for c in miss:
            files = set(r["file"].split("/src/")[-1] for r in rows if r["code"] == c)
            print("   %-42s %s" % (c, ", ".join(sorted(files))[:90]))
    print("\n各文件的权限码数:")
    for f, rs in sorted(by.items(), key=lambda x: -len(x[1])):
        print("   %-46s %d" % (f.split("/src/")[-1], len(rs)))


if __name__ == "__main__":
    main()
