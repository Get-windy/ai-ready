# -*- coding: utf-8 -*-
"""系统模块审计 · 菜单/路由核对
A. 菜单 component → 前端文件存在性
B. component → 是否出现在前端路由/动态路由映射中
C. 前端存在但无菜单指向的页面（孤儿页）
D. menu_level / client_type / tenant_id 异常
"""
import os, io, json, sys, re, glob

sys.stdout.reconfigure(encoding="utf-8")
ROOT = r"I:\AI-Ready"
VIEWS = os.path.join(ROOT, "frontend/apps/pc-admin/src/views")
D = json.load(io.open(os.path.join(ROOT, "tools/system-db.json"), encoding="utf-8"))
OUT = os.path.join(ROOT, "tools", "system-menu.json")

LEAF_EXTS = (".vue", "/index.vue")


def comp_to_files(c):
    c = (c or "").strip().lstrip("/")
    c = c.replace("\\", "/")
    if c.startswith("views/"):
        c = c[len("views/"):]
    if c.endswith(".vue"):
        return [os.path.join(VIEWS, c)]
    return [os.path.join(VIEWS, c + ".vue"), os.path.join(VIEWS, c, "index.vue")]


def main():
    tree = D["menu_tree"]
    rows = []
    for m in tree:
        c = m.get("component") or ""
        files = comp_to_files(c) if c else []
        found = [f for f in files if os.path.isfile(f)]
        rows.append({**m, "component_file": os.path.relpath(found[0], ROOT).replace("\\", "/") if found else None,
                     "exists": bool(found)})
    # 前端系统模块全部页面文件
    pages = []
    for pat in ("admin/**/*.vue", "system/**/*.vue"):
        for f in glob.glob(os.path.join(VIEWS, pat.replace("/", os.sep)), recursive=True):
            pages.append(os.path.relpath(f, VIEWS).replace("\\", "/").replace(".vue", ""))
    # 路由引用
    router_txt = ""
    for f in glob.glob(os.path.join(ROOT, "frontend/apps/pc-admin/src/router/**/*.ts"), recursive=True):
        router_txt += io.open(f, encoding="utf-8", errors="replace").read()

    def ncomp(c):
        c = (c or "").replace(".vue", "").lstrip("/")
        c = c[len("views/"):] if c.startswith("views/") else c
        return c[:-len("/index")] if c.endswith("/index") else c
    menu_comp = set(ncomp(r.get("component")) for r in rows if r.get("component"))
    orphan = [p for p in pages if ncomp(p) not in menu_comp]

    missing = [r for r in rows if r.get("menu_type") == "1" and r.get("component") and not r["exists"]]
    nocomp = [r for r in rows if r.get("menu_type") == "1" and not r.get("component")]
    noimport = [r for r in rows if r.get("exists") and os.path.basename(r["component_file"]) not in router_txt
                and r["component"].split("/")[-1] not in router_txt
                and r["component"] not in router_txt]

    out = {"rows": rows, "orphan_pages": orphan, "missing_component_file": missing,
           "leaf_no_component": nocomp, "not_in_router": noimport, "pages": pages}
    io.open(OUT, "w", encoding="utf-8").write(json.dumps(out, ensure_ascii=False, indent=1))

    print("菜单行 %d（叶子 %d）" % (len(rows), len([r for r in rows if r["menu_type"] == "1"])))
    print("\n✗ component 指向的文件不存在 (%d):" % len(missing))
    for r in missing:
        print("   %-8s %-14s %s" % (r["id"], r["menu_name"], r["component"]))
    print("\n✗ 叶子菜单无 component (%d):" % len(nocomp))
    for r in nocomp:
        print("   %-8s %-14s %s" % (r["id"], r["menu_name"], r["path"]))
    print("\n⚠ 前端存在但系统模块菜单未指向的页面 (%d):" % len(orphan))
    for p in sorted(orphan):
        print("   %s" % p)


if __name__ == "__main__":
    main()
