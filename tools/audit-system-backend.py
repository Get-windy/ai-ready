# -*- coding: utf-8 -*-
"""系统模块审计 · 后端侧
输入 tools/system-contract.json
输出：
  A. 系统模块后端端点清单（按包） + 是否被前端调用（method+path）
  B. 未调用端点清单（冗余/死接口候选）
  C. 无任何鉴权注解的端点
  D. 引用的权限码集合 + 引用了但库中不存在的码
"""
import os, re, io, json, sys, collections

sys.stdout.reconfigure(encoding="utf-8")
ROOT = r"I:\AI-Ready"
C = json.load(io.open(os.path.join(ROOT, "tools/system-contract.json"), encoding="utf-8"))
DB = json.load(io.open(os.path.join(ROOT, "tools/system-db.json"), encoding="utf-8"))
OUT = os.path.join(ROOT, "tools", "system-backend.json")

PKG_PREFIX = (
    "cn.aiedge.module", "cn.aiedge.monitor", "cn.aiedge.platform", "cn.aiedge.datasource",
    "cn.aiedge.devtool", "cn.aiedge.export", "cn.aiedge.base", "cn.aiedge.tenant",
    "cn.aiedge.permission", "cn.aiedge.audit", "cn.aiedge.scheduler", "cn.aiedge.cache",
    "cn.aiedge.dict", "cn.aiedge.config", "cn.aiedge.department", "cn.aiedge.position",
    "cn.aiedge.user", "cn.aiedge.storage", "cn.aiedge.agreement",
)
# 明确不属于系统模块菜单的（避免把设置模块/协议业务页算进来）
PKG_EXCLUDE_NAME = ()


def key(p):
    if p.startswith("/api/"):
        p = p[4:]
    elif p == "/api":
        p = "/"
    return p or "/"


def inscope(e):
    return any(e["pkg"] == p or e["pkg"].startswith(p + ".") for p in PKG_PREFIX)


def main():
    eps = [e for e in C["endpoints"] if inscope(e)]
    fe = set()
    for c in C["calls"]:
        m = c["method"]
        if m in ("STR",):
            continue
        fe.add((m if m != "ANY" else "ANY", key(c["path"])))
    fe_any = set(p for _, p in fe)

    def used(e):
        return (e["method"], key(e["path"])) in fe or key(e["path"]) in fe_any

    rows = []
    for e in eps:
        perm = []
        roles = []
        for a in e["auth"]:
            if a.startswith("SaCheckPermission"):
                perm += re.findall(r'"([^"]+)"', a)
            elif a.startswith("SaCheckRole"):
                roles += re.findall(r'"([^"]+)"', a)
        rows.append({**e, "k": key(e["path"]), "used": used(e),
                     "perms": perm, "roles": roles, "has_auth": bool(e["auth"])})

    # 权限码：代码引用 vs 库
    db_codes = set(r["permission_code"] for r in DB["perm_all"])
    ref_codes = collections.Counter()
    for r in rows:
        for p in r["perms"]:
            ref_codes[p] += 1
    # 全仓（不只系统模块）引用
    all_ref = collections.Counter()
    for e in C["endpoints"]:
        for a in e["auth"]:
            if a.startswith("SaCheckPermission"):
                for p in re.findall(r'"([^"]+)"', a):
                    all_ref[p] += 1

    missing = {p: n for p, n in ref_codes.items() if p not in db_codes}
    missing_all = {p: n for p, n in all_ref.items() if p not in db_codes}

    bypkg = collections.Counter(r["pkg"] for r in rows)
    unused = [r for r in rows if not r["used"]]
    noauth = [r for r in rows if not r["has_auth"]]

    out = {"sys_endpoints": rows, "unused": unused, "noauth": noauth,
           "pkg_stat": dict(bypkg), "ref_codes": dict(ref_codes), "missing_codes": missing,
           "missing_codes_all": missing_all,
           "db_perm_sys_count": len(DB["perm_sys"])}
    io.open(OUT, "w", encoding="utf-8").write(json.dumps(out, ensure_ascii=False, indent=1))

    print("系统模块包内端点: %d 个（路径 %d）" % (len(rows), len(set(r["k"] for r in rows))))
    print("  未被前端调用: %d" % len(unused))
    print("  无鉴权注解  : %d" % len(noauth))
    print("  引用权限码  : %d 种；库中缺失 %d 种" % (len(ref_codes), len(missing)))
    print("\n按包:")
    for p, n in bypkg.most_common():
        u = len([r for r in unused if r["pkg"] == p])
        a = len([r for r in noauth if r["pkg"] == p])
        print("  %-45s %3d 端点  未用 %2d  无鉴权 %2d" % (p, n, u, a))
    if missing:
        print("\n缺失权限码:", json.dumps(missing, ensure_ascii=False))
    print("\n全仓缺失权限码 %d 种" % len(missing_all))


if __name__ == "__main__":
    main()
